//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.core.factory.api.data;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ExtensionRegistry;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;

import org.osgi.service.log.LogService;

/**
 * Base abstract implementation of the factory object data manager. This service needs to be extended for all types
 * of factory objects.
 * @author callen
 *
 */
public abstract class BaseFactoryObjectDataManager implements FactoryObjectDataManager
{   
    /**
     * Bound service for persistent datastore.
     */
    private PersistentDataStore m_PersistentDataStore;
    
    /**
     * Extension registry for link layer specific information.
     */
    private final ExtensionRegistry m_Registry = ExtensionRegistry.newInstance();
    
    /**
     * Bind a service for persistent data store. Needs to be called  by all implementors.
     * 
     * @param persistentDataStore
     *      service to bind for persistent data store
     */
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }

    @Override
    public String getName(final UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException
    {
        return getMessage(uuid).getName();
    }
    
    @Override
    public void setName(final UUID uuid, final String name) throws FactoryObjectInformationException, 
            IllegalArgumentException
    {
        //check the persistent data store for the object's entry
        final FactoryObjectData factoryInformation = getMessage(uuid);
    
        //merge with old entity
        final FactoryObjectData.Builder updatedFactInfo = factoryInformation.toBuilder().setName(name);
        //update entry found
        mergeEntity(uuid, updatedFactInfo);
    }

    @Override
    public void persistNewObjectData(final UUID uuid, final FactoryDescriptor factory, final String name) 
            throws FactoryObjectInformationException, IllegalArgumentException
    {
        //check the persistent data store for the object's entry
        FactoryObjectData factoryInformation = tryGetMessage(uuid);
        
        if (factoryInformation == null)
        {
            factoryInformation = FactoryObjectData.newBuilder().setName(name).build();
            //set the object name and persist new entry
            try
            {
                m_PersistentDataStore.persist(getServiceObjectType(), uuid, factory.getProductType(), 
                        factoryInformation.toByteArray());
            }
            catch (final PersistenceFailedException ex)
            {
                throw new FactoryObjectInformationException(ex); //NOPMD not losing stacktrace the parent error is 
                //expected if the entry is new
            }
            //completed persisting
            return;
        }
        else
        {
            throw new IllegalArgumentException(String.format("New persistent entry not created. UUID [%s] is already"
                    +  "being used by another object entry", uuid));
        }
    }

    @Override //not calling get message because we need to iterate through all the entries we don't want to error
    public UUID getPersistentUuid(final String name) //out if entry is un-readable
    {
        final Collection<PersistentData> allFactData = m_PersistentDataStore.query(getServiceObjectType());
        for (PersistentData factObjData : allFactData)
        {
            final byte[] byteData = (byte[])factObjData.getEntity();
            final FactoryObjectData factDataMessage;
            
            try
            {
                factDataMessage = FactoryObjectData.parseFrom(byteData);
            }
            catch (final IOException e)
            {
                //at this point we don't want to throw an error because this entity may not contain the data being
                //looked for
                Logging.log(LogService.LOG_ERROR, e, "Unable to parse factory data of a(n) [%s] with UUID [%s].", 
                        getServiceObjectType().getSimpleName(), factObjData.getUUID());
                continue;
            }
            
            if (factDataMessage.getName().equals(name))
            {
                return factObjData.getUUID();
            }
        }
        return null;
    }
    
    @Override
    public String getPid(final UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException
    {
        final FactoryObjectData factDataMessage = getMessage(uuid);
        
        if (factDataMessage.hasPid())
        {
            return factDataMessage.getPid();
        }
        
        return null;
    }

    @Override
    public void setPid(final UUID uuid, final String pid) 
            throws FactoryObjectInformationException, IllegalArgumentException
    {
        if (pid == null)
        {
            throw new IllegalArgumentException("The PID for an object cannot be stored as null.");
        }
        //check the persistent data store for the object's entry
        final FactoryObjectData factoryInformation = getMessage(uuid);
        
        //merge old entity
        final FactoryObjectData.Builder updatedFactInfo = factoryInformation.toBuilder().setPid(pid);
        
        //update entry found
        mergeEntity(uuid, updatedFactInfo);
    }
    
    @Override
    public void clearPid(final UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException
    {
        //check the persistent data store for the object's entry
        final FactoryObjectData factoryInformation = getMessage(uuid);
        
        if (factoryInformation.hasPid())
        {
            //merge old entity
            final FactoryObjectData.Builder updatedFactInfo = factoryInformation.toBuilder().clearPid();
        
            //update entry found
            mergeEntity(uuid, updatedFactInfo);
        }
    }
    
    @Override
    public void tryRemove(final UUID uuid)
    {
        Preconditions.checkNotNull(uuid);
        
        try
        {
            m_PersistentDataStore.remove(uuid);
        }
        catch (final IllegalArgumentException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Failed to remove %s data store entry with UUID [%s]",  
                    getServiceObjectType().getSimpleName(), uuid);
        }
    }

    @Override
    public Collection<PersistentData> getAllObjectData(final FactoryDescriptor factory)
    {
        return m_PersistentDataStore.query(getServiceObjectType(), factory.getProductType());
    }
    
    /**
     * Get the type of object that the implementation is supporting. Should be the base type, ie Asset, LinkLayer, etc.
     * @return
     *     the type of factory object
     */
    protected abstract Class<? extends FactoryObject> getServiceObjectType();
    
    /**
     * Get the registry of extensions needed to parse messages.
     * @return
     *     the registry needed to parse messages
     */
    public ExtensionRegistry getRegistry()
    {
        return m_Registry;
    }
    
    /**
     * Get the factory object information message from a persistent entity.
     * The message is parsed with the applicable extension registry.
     * @param uuid
     *     the UUID of entity
     * @return 
     *     the factory object information message, will return null if there is not a message associated with the
     *     presented UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the message from being parsed
     */
    protected FactoryObjectData tryGetMessage(final UUID uuid) throws FactoryObjectInformationException
    {
        final PersistentData factData = m_PersistentDataStore.find(uuid);
        if (factData != null)
        {
            final byte[] byteData = (byte[])factData.getEntity();
            try
            {
                return FactoryObjectData.parseFrom(byteData, getRegistry());
            }
            catch (final IOException e)
            {
                throw new FactoryObjectInformationException(
                        String.format("Factory information for a %s with UUID [%s] was not parseable.", 
                                getServiceObjectType().getSimpleName(), uuid), e);
            }
        }
        return null;
    }
    
    /**
     * Get the factory object information message using {@link #tryGetMessage}, if data is
     * null, throw an exception instead. This method should be used by methods that do not
     * handle a case where no data entry exists for a given UUID.
     * @param uuid
     *     the UUID of the entity
     * @return
     *     the factory object information message, if there is not a message associated with the presented UUID
     *     an exception will be thrown
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the message from being parsed
     * @throws IllegalArgumentException
     *     if there is not a message associated with the given uuid
     */
    protected FactoryObjectData getMessage(final UUID uuid) throws FactoryObjectInformationException, 
            IllegalArgumentException
    {
        final FactoryObjectData factData = tryGetMessage(uuid);
        
        if (factData == null)
        {
            throw new IllegalArgumentException(String.format("No factory object data exists for the given UUID [%s]",
                uuid.toString()));
        }
        
        return factData;
    }
    
    /**
     * Merge updated information to an existent entity.
     * @param uuid
     *     the UUID of entity
     * @param factInformationBuilder
     *     the message builder to persist, this method will build the message
     * @throws IllegalArgumentException
     *     if a data message cannot be found for the given UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the data from being persisted
     */
    protected void mergeEntity(final UUID uuid, final FactoryObjectData.Builder factInformationBuilder) throws 
            IllegalArgumentException, FactoryObjectInformationException
    {
        final PersistentData factData = m_PersistentDataStore.find(uuid);
        factData.setEntity(factInformationBuilder.build().toByteArray());
        
        try
        {
            m_PersistentDataStore.merge(factData);
        }
        catch (final PersistenceFailedException | ValidationFailedException e)
        {
            throw new FactoryObjectInformationException(
                String.format("Unable to update location for asset with uuid %s, because the factory object"
                    + " information could not be merged.", uuid.toString()), e);
        }
    }
}
