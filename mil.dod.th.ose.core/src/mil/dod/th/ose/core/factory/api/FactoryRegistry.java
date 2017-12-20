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
package mil.dod.th.ose.core.factory.api; 
//of factory objects

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.factory.impl.PendingFactoryObject;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Keeps a listing of objects created by a set of factories.
 * 
 * @param <T>
 *      control interface for the objects the registry maintains, this should be a common base 
 *      type like {@link mil.dod.th.ose.core.impl.asset.AssetInternal} or 
 *      {@link mil.dod.th.ose.core.impl.ccomm.AddressInternal} not actual factory production types
 *      
 * @author dhumeniuk
 *
 */
@Component(factory = FactoryRegistry.COMPONENT_FACTORY_REG_ID) //NOCHECKSTYLE Class fan out.
public class FactoryRegistry<T extends FactoryObjectInternal>  //NOPMD Avoid really long classes.
{ // TD: Should investigate way to break-down the class.
    
    /**
     * ID used to register the FactoryRegistry instance as a component factory.
     */
    public static final String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.factory.api.FactoryRegistry";
    
    /**
     * Map of all control objects; keyed by the object's UUID.
     */
    private final Map<UUID, T> m_ControlObjects = Collections.synchronizedMap(new HashMap<UUID, T>());
    
    /**
     * Map which keeps track of all component instances created and are 
     * identified by the component's UUID.
     */
    private final Map<UUID, ComponentInstance> m_ComponentMap = 
            Collections.synchronizedMap(new HashMap<UUID, ComponentInstance>());
    
    /**
     * Factory service that manages the registry.
     */
    private DirectoryService m_DirectoryService;
    
    /**
     * Service for storing persisted factory object data.
     */
    private FactoryObjectDataManager m_FactoryObjectDataManager;
    
    /**
     * The type of factory object that this registry maintains (e.g., Asset or Address).
     */
    private String m_BaseType;

    /**
     * List of potential dependencies for all created/updated objects.
     */
    private List<RegistryDependency> m_Dependencies;
    
    /**
     * Set of pending objects.
     */
    private final Set<PendingFactoryObject> m_PendingObjects = 
            Collections.synchronizedSet(new HashSet<PendingFactoryObject>());
    
    /**
     * The bundle context that is to be used for this registry.
     */
    private BundleContext m_BundleContext;

    /**
     * The {@link FactoryServiceProxy} that is to be used for the given registry type.
     */
    private FactoryServiceProxy<T> m_ServiceProxy;
    
    /**
     * The {@link ConfigurationAdmin} service to use.
     */
    private ConfigurationAdmin m_ConfigAdmin;
    
    /**
     * The {@link EventAdmin} service to use.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * The {@link FactoryServiceUtils} service to use.
     */
    private FactoryServiceUtils m_FactoryServiceUtils;
    
    /**
     * The {@link PowerManagerInternal} service to use.
     */
    private PowerManagerInternal m_PowerInternal;
    
    /**
     * The callback interface to use.
     */
    private FactoryRegistryCallback<T> m_Callback;
    
    /**
     * Lock object for synchronizing setting names in order to insure uniqueness of names.
     */
    private final Object m_NameSynchLock = new Object();
    
    /**
     * Reference to the class that handles listening for factory object events.
     */
    private FactoryRegistryEventHandler m_RegistryEventHandler;
    
    private LoggingService m_Logging;
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Method used to set the {@link ConfigurationAdmin} service to use.
     * @param admin
     *  the configuration admin service that is to be used
     */
    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin admin)
    {
        m_ConfigAdmin = admin;
    }
    
    /**
     * Method used to set the {@link FactoryServiceUtils} service to use.
     * @param utils
     *  the factory service utils service that is to be used
     */
    @Reference
    public void setFactoryServiceUtils(final FactoryServiceUtils utils)
    {
        m_FactoryServiceUtils = utils;
    }
    
    /**
     * Method used to set the {@link EventAdmin} service to use.
     * @param eventAdmin
     *  the event admin service that is to be used
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method used to set the {@link PowerManagerInternal} service to use.
     * @param powerMgr
     *  the power manager internal service to use
     */
    @Reference
    public void setPowerManagerInternal(final PowerManagerInternal powerMgr)
    {
        m_PowerInternal = powerMgr;
    }
    
    /**
     * Activation method for this component.
     * @param context
     *  the bundle context that is to be used
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
        
        //register for events
        m_RegistryEventHandler = new FactoryRegistryEventHandler();
        m_RegistryEventHandler.registerEvents();
    }
    
    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate()
    {
        m_RegistryEventHandler.unregsiterEvents();
    }
    
    /**
     * Method used to initialize needed dependencies for a given factory registry. This method
     * must be called before this class can be used.
     * 
     * @param directoryService
     *      directory service that manages the registry 
     * @param serviceProxy
     *      the {@link FactoryServiceProxy} that is to be used for this registry
     * @param callback
     *      reference to callback class that will be used to notify others of objects that have been 
     *      rebased or removed
     */
    public void initialize(final DirectoryService directoryService, final FactoryServiceProxy<T> serviceProxy, 
            final FactoryRegistryCallback<T> callback)
    {
        m_DirectoryService = directoryService;
        m_ServiceProxy = serviceProxy;
        m_FactoryObjectDataManager = serviceProxy.getDataManager();
        m_BaseType = serviceProxy.getBaseType().getSimpleName();
        m_Callback = callback;

        //retrieve deps from callback service
        m_Dependencies = new ArrayList<RegistryDependency>(m_Callback.retrieveRegistryDependencies());
    }

    /**
     * Method to handle post update notifications.
     * @param object
     *      the object that was updated
     * @throws ConfigurationException
     *      if there was an error with the configuration
     * @throws FactoryException
     *      if there is an error while performing the callback
     */
    public void handleUpdated(final T object) throws FactoryException, ConfigurationException
    {
        m_Callback.preObjectUpdated(object);
    }
    
    /**
     * Get an unmodifiable set of all known control objects.
     * 
     * @return 
     *     the set of all objects in the registry
     */
    public Set<T> getObjects()
    {
        return Collections.unmodifiableSet(new HashSet<T>(m_ControlObjects.values()));
    }
    
    /**
     * Get the UUIDs of all the objects in the registry.
     * @return
     *      a list of all the UUIDs
     */
    public List<UUID> getUuids()
    {
        final List<UUID> uuidList = new ArrayList<UUID>();
        
        for (T object : getObjects())
        {
            uuidList.add(object.getUuid());
        }
        
        return uuidList;
    }
    
    /**
     * Adds an object entry to the registry and posts an {@link FactoryDescriptor#TOPIC_FACTORY_OBJ_CREATED} 
     * event.
     * 
     * @param object
     *     object to add
     */
    private void addObject(final T object)
    {
        m_ControlObjects.put(object.getUuid(), object);

        m_DirectoryService.postFactoryObjectEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, object);
        
        m_Logging.info("%s [%s] UUID=%s added to registry", m_BaseType, 
                object.getName(), object.getUuid());
    }
    
    /**
     * Remove an object entry from the registry.
     * 
     * @param uuid
     *      UUID of the object to remove
     */
    public void removeObject(final UUID uuid)
    {
        m_Logging.info("Attempting to remove %s with UUID %s from registry", m_BaseType, uuid);

        final T itemRemoved = m_ControlObjects.remove(uuid);
        if (itemRemoved != null)
        {
            final ComponentInstance compInst = m_ComponentMap.remove(itemRemoved.getUuid());
            if (compInst == null)
            {
                m_Logging.error("Component instance for object with uuid [%s] "
                        + "could not be found, and cannot be disposed of properly.", itemRemoved.getUuid());
            }
            else
            {
                compInst.dispose();
            }
            
            //notify listeners that the object has been removed
            m_Callback.onRemovedObject(itemRemoved);
        }
    }
    
    /**
     * Add a persistent object entry to the data store. Used creating objects.
     * 
     * @param desiredName
     *     the name to persist. This method assumes the name is not blank or null.
     * @param uuid
     *     the UUID to associate with the name
     * @param factory
     *     the factory the object belongs to
     * @param autoRename
     *     if set to true then a duplicate name (typically a default name) should automatically have the UUID appended
     *     to create a new unique name automatically
     * @throws IllegalArgumentException
     *     thrown if the name is the duplicate of a name already in the data store.
     * @throws FactoryObjectInformationException
     *     thrown if a problem occurs saving the object name
     * @return string
     *     the persisted name. If autoRenamed, this will be the final unique name used.
     */
    public synchronized String persistNewObjectData(final String desiredName, final UUID uuid, 
            final FactoryDescriptor factory, final boolean autoRename) throws IllegalArgumentException,
            FactoryObjectInformationException
    {
        synchronized (m_NameSynchLock)
        {
            //check name for uniqueness
            final UUID entryUUID = m_FactoryObjectDataManager.getPersistentUuid(desiredName);
            final String nameEntry = getUniqueName(desiredName, uuid, autoRename);
        
            if (uuid.equals(entryUUID))
            {
                //name is already associated with the given object
                return nameEntry;
            }
        
            //persist the name
            m_FactoryObjectDataManager.persistNewObjectData(uuid, factory, nameEntry);
            return nameEntry;
        }
    }
    
    /**
     * Name a persistent object in the data store.
     * 
     * @param desiredName
     *     the name to persist. This method assumes the name is not blank or null.
     * @param uuid
     *     the UUID to associate with the name
     * @param autoRename
     *     if set to true then a duplicate name (typically a default name) should automatically have the UUID appended
     *     to create a new unique name automatically
     * @throws IllegalArgumentException
     *     thrown if the name is the duplicate of a name already in the data store.
     * @throws FactoryObjectInformationException
     *     thrown if a problem occurs saving the object name
     * @return string
     *     the persisted name. If autoRenamed, this will be the final unique name used.
     */
    public synchronized String setName(final String desiredName, final UUID uuid,
        final boolean autoRename) throws IllegalArgumentException, FactoryObjectInformationException
    {
        Preconditions.checkNotNull(desiredName);
        Preconditions.checkNotNull(uuid);
        
        synchronized (m_NameSynchLock)
        {
            //check name for uniqueness
            final UUID entryUUID = m_FactoryObjectDataManager.getPersistentUuid(desiredName);
            final String nameEntry = getUniqueName(desiredName, uuid, autoRename);
        
            if (uuid.equals(entryUUID))
            {
                //name is already associated with the given object
                return nameEntry;
            }
        
            //persist the name
            m_FactoryObjectDataManager.setName(uuid, nameEntry);
            return nameEntry;
        }
    }
    
    /**
     * Check if a name already exists in the datastore. If a duplicate is found and autoRename is true then
     * this method returns a new, unique name.
     * 
     * @param desiredName
     *     the desired name to check for uniqueness
     * @param uuid
     *     the UUID to associated with the given name
     * @param autoRename
     *     if true and the name is not unique it will be appended with the UUID to create a unique name
     * @return uniqueName
     *     returns the original name or the new unique version of the name created using autoRename
     * @throws IllegalArgumentException
     *     if the name is not unique and not autoRenamed
     */
    private String getUniqueName(final String desiredName, final UUID uuid, final boolean autoRename) 
            throws IllegalArgumentException
    {
        //check that name is not a duplicate
        final UUID entryUuid = m_FactoryObjectDataManager.getPersistentUuid(desiredName);
        String uniqueName = desiredName;
        
        if (entryUuid != null)
        {
            if (entryUuid.equals(uuid))
            {
                //desiredName is already saved in the data store and associated with the given UUID
                return uniqueName;
            }
            else if (autoRename)
            {
                uniqueName = desiredName + uuid;
                m_Logging.info("Name [%s] already exists in the datastore. %s Automatically renamed to [%s]", 
                        desiredName, m_BaseType, uniqueName);
            }
            else
            {
                throw new IllegalArgumentException(
                        String.format("Duplicate name: [%s] is already in use by object with UUID [%s].", 
                                desiredName, entryUuid));
            }
        }
        
        return uniqueName;
    }
    
    /**
     * Set the name of an object in the registry.
     * 
     * @param object
     *     the object to set the name of
     * @param name
     *     the object's new name
     * @throws IllegalArgumentException
     *     thrown if the name parameter is null, empty, or a already the name of a different object.
     * @throws FactoryObjectInformationException
     *     thrown if a problem occurs saving the object name
     */
    public synchronized void setName(final FactoryObjectInternal object, final String name)
            throws IllegalArgumentException, FactoryObjectInformationException
    {       
        //name cannot be blank or null
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("Name cannot be set to null or empty.");
        }

        object.internalSetName(setName(name, object.getUuid(), false));
        
        //name changed post event
        m_DirectoryService.postFactoryObjectEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, object);
    }

    /**
     * Set the name of an object in the registry.
     * 
     * @param uuid
     *     the UUID of the object to set the name of
     * @param name
     *     the object's new name
     * @throws IllegalArgumentException
     *     thrown if the name parameter is null, empty, or a already the name of a different object.
     * @throws FactoryObjectInformationException
     *     thrown if a problem occurs saving the object name
     */
    public synchronized void setName(final UUID uuid, final String name) 
            throws IllegalArgumentException, FactoryObjectInformationException
    {       
        final T obj = getObjectByUuid(uuid);
        
        setName(obj, name);
    }
    
    /**
     * Find the object with the given name.
     * 
     * @param name
     *      Name of the object
     * @return
     *      object with the given name or null if not found
     */
    public T findObjectByName(final String name)
    {
        synchronized (m_ControlObjects)
        {
            for (T object : m_ControlObjects.values())
            {
                if (name.equals(object.getName()))
                {
                    return object;
                }
            }            
        }
        
        return null;
    }
    
    /**
     * Check if the object is in the registry.
     * 
     * @param name
     *      name of the object to find
     * @return
     *      true if object with name is in registry, false otherwise
     */
    public boolean isObjectCreated(final String name) 
    {
        synchronized (m_ControlObjects)
        {
            for (T object : m_ControlObjects.values())
            {
                if (name.equals(object.getName()))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Returns an object with the specified name.
     * 
     * @param name
     *      the name to search for
     * @return 
     *      the object in the registry that corresponds with this name
     * @throws IllegalArgumentException 
     *      object not in registry
     */
    public T getObjectByName(final String name) throws IllegalArgumentException
    {
        final T object = findObjectByName(name);
        
        if (object == null)
        {
            throw new IllegalArgumentException(String.format("%s [%s] not found in lookup.", 
                    m_BaseType, name));
        }
        else
        {
            return object;
        }
    }
    
    /**
     * Get the object by its unique persistent identifier.
     * 
     * @param pid
     *      unique persistent identifier
     * @return
     *      object with the unique identifier
     */
    public T getObjectByPid(final String pid)
    {
        //iterate over all known objects checking the PID
        for (T obj : getObjects())
        {
            final String objPid = obj.getPid();

            if (objPid != null && objPid.equals(pid))
            { 
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Get the object by its unique persistent identifier.
     * 
     * @param uuid
     *      unique persistent identifier
     * @return
     *      object with the given UUID
     * @throws IllegalArgumentException
     *      if the UUID is not for a known object
     */
    public T getObjectByUuid(final UUID uuid) throws IllegalArgumentException
    {
        if (uuid == null)
        {
            //exception thrown if UUID is null
            throw new IllegalArgumentException("UUID cannot be null");
        }
        else
        {
            final T objToReturn = m_ControlObjects.get(uuid);
            if (objToReturn != null)
            {
                return objToReturn;
            }
            //exception thrown if UUID is not null but not associated with an object
            throw new IllegalArgumentException(String.format("Not a valid UUID [%s]", uuid.toString()));
        }
    }
    
    /**
     * Get a set of objects in the registry with the given product type.
     * 
     * @param productType
     *      type to find as returned by {@link FactoryDescriptor#getProductType()}
     * @return
     *      set of objects matching the given type, an empty set means that no objects of the specified type are known
     */
    public Set<T> getObjectsByProductType(final String productType)
    {
        final Set<T> objects = new HashSet<T>();
        for (T object : getObjects())
        {
            if (object.getFactory().getProductType().equals(productType))
            {
                objects.add(object);
            }
        }
        return objects;
    }
    
    /**
     * Get a {@link List} of the logical names of all objects currently in this registry.
     * 
     * @return 
     *      the List of object names
     */
    public List<String> getObjectNames()
    {
        final List<String> names = new ArrayList<String>();
        for (T object : getObjects())
        {
            names.add(object.getName());
        }
        
        return names;
    }

    /**
     * Removes the factory object's information from the persistent store and 
     * {@link org.osgi.service.cm.ConfigurationAdmin} service.
     * Posts an {@link FactoryDescriptor#TOPIC_FACTORY_OBJ_DELETED} event once deletion is successful.
     * @param object
     *     the object to remove
     */
    public void delete(final FactoryObjectInternal object)
    {
        final UUID uuid = object.getUuid();
        removeObject(uuid);
       
        object.getFactory().dispose(object.getProxy());
        
        tryDeleteConfiguration(uuid);

        m_FactoryObjectDataManager.tryRemove(uuid);

        m_DirectoryService.postFactoryObjectEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED, object);
    }
    
    /**
     * Delete the {@link org.osgi.service.cm.ConfigurationAdmin} configuration for the given PID.
     * 
     * @param uuid
     *      UUID of the object to have its configuration deleted
     */
    protected void tryDeleteConfiguration(final UUID uuid)
    {
        m_Logging.debug("Attempting to delete configuration for UUID [%s]", uuid);
        
        try
        {
            final String pid = m_FactoryObjectDataManager.getPid(uuid);
            
            final Configuration config = FactoryServiceUtils.getFactoryConfiguration(m_ConfigAdmin, pid);
            if (config != null)
            {
                config.delete();
            }
        }
        catch (final Exception e)
        {
            m_Logging.warning(e, "Unable to delete configuration during cleanup for object with UUID [%s]", uuid);
        }
    }
    
    /**
     * Default implementation to create a factory object.
     * 
     * @param factory
     *      factory used to create the object
     * @param name
     *      the name to assign to the newly created object or null to use a default
     * @param properties
     *      properties for the object
     * @return
     *      the object that was created
     * @throws FactoryException
     *      if there was an error with the creation of the object or its configuration
     * @throws IllegalArgumentException
     *      if the name is a duplicate of a name already in the data store
     * @throws FactoryObjectInformationException
     *      if a problem occurs saving the object's data
     */
    public T createNewObject(final FactoryInternal factory, final String name, final Map<String, Object> properties) 
            throws IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        Preconditions.checkNotNull(properties);
        
        final UUID uuid = createAndPersistObject(factory, name);
        
        try
        {
            // create a configuration if needed
            if (!properties.isEmpty()) // only create if there are non-standard properties
            {
                final String factoryPid = factory.getPid();
                final Configuration config = createConfiguration(uuid, factoryPid, null);
                try
                {
                    config.update(new Hashtable<>(properties));
                }
                catch (final IOException e)
                {
                    throw new FactoryException(
                            String.format("Error updating configuration for object with UUID: %s ", uuid), e);
                }
            }
            
            try
            {
                //create instance, will check if object is satisfied
                return createOrRestoreObject(factory, uuid);
            }
            catch (final Exception e)
            {
                tryDeleteConfiguration(uuid);
                throw e;
            }
        }
        catch (final Exception e)
        {
            if (!isPendingObject(uuid))
            {
                m_FactoryObjectDataManager.tryRemove(uuid);
            }

            throw e;
        }
    }
    
    /**
     * Implementation that creates a factory object that is associated with a specific configuration.
     * 
     * @param factory
     *      factory used to create the object
     * @param name
     *      the name to assign to the newly created object or null to use a default
     * @param configPid
     *      PID of the configuration the factory object should be associated with
     * @return
     *      the object that was created
     * @throws IllegalArgumentException
     *      thrown if no configuration with the specified PID can be found, the configuration specified is already 
     *      associated with a factory object, or the name is a duplicate of a name already in the data store
     * @throws FactoryException
     *      thrown if there was an error with the creation of the object
     * @throws FactoryObjectInformationException
     *      thrown if a problem occurs saving the object's data
     * @throws IOException
     *      thrown if an error occurs attempting to verify the existence of the configuration with the specified PID
     * @throws InvalidSyntaxException
     *      thrown if an error occurs attempting to verify the existence of the configuration with the specified PID
     */
    public T createNewObjectForConfig(final FactoryInternal factory, final String name, final String configPid) 
            throws IllegalArgumentException, FactoryException, FactoryObjectInformationException, IOException, 
                InvalidSyntaxException
    {
        Preconditions.checkNotNull(configPid);
     
        final Configuration config = FactoryServiceUtils.getFactoryConfiguration(m_ConfigAdmin, configPid);
        if (config == null)
        {
            throw new IllegalArgumentException(String.format("No configuration exists with PID: %s ", configPid));
        }
        
        if (getObjectByPid(configPid) != null)
        {
            throw new IllegalArgumentException(
                    String.format("A factory object is already associated to configuration with PID: %s", configPid));
        }
        
        final UUID uuid = createAndPersistObject(factory, name);
        
        try
        {
            m_FactoryObjectDataManager.setPid(uuid, configPid);

            //create instance, will check if object is satisfied
            return createOrRestoreObject(factory, uuid);
        }
        catch (final Exception e)
        {
            m_FactoryObjectDataManager.tryRemove(uuid);
            throw e;
        }
    }
    
    /**
     * Creates and persists an instance of an object for the specified factory.
     * 
     * @param factory
     *     factory used to create the object
     * @param name
     *      the name to assign to the newly created object or null to use a default
     * @return
     *      UUID of the created factory object.
     * @throws IllegalArgumentException
     *      if the name is a duplicate of a name already in the data store
     * @throws FactoryObjectInformationException
     *      thrown if a problem occurs saving the object's data
     */
    private UUID createAndPersistObject(final FactoryInternal factory, final String name) 
            throws IllegalArgumentException, FactoryObjectInformationException
    {
        Preconditions.checkNotNull(factory);
        
        final UUID uuid = UUID.randomUUID();
        
        String objectName = name;
        Boolean autoRename = false;
        //if provided name is blank or null use default name instead.
        if (name == null || name.trim().isEmpty())
        {
            //default name
            objectName = Iterables.getLast(Splitter.on(".").split(factory.getProductType()), factory.getProductType());
            autoRename = true;
        }
        //persist the objects data
        persistNewObjectData(objectName, uuid, factory, autoRename);
        
        return uuid;
    }

    /**
     * Default implementation to create or restore a factory object.
     * 
     * @param factory
     *      factory used to create the object
     * @param uuid
     *      the UUID for the newly created object, the same UUID should be associated with the object's name, PID, etc.
     * @return
     *      the object that was created
     * @throws FactoryException
     *      if there was an error with the creation of the object or its configuration
     * @throws IllegalArgumentException
     *      if the name is the duplicate of a name already in the data store
     * @throws FactoryObjectInformationException
     *      if a problem occurs saving the object's data
     */
    @SuppressWarnings("unchecked")
    protected T createOrRestoreObject(final FactoryInternal factory, final UUID uuid) 
            throws IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        //get the object name 
        final String objectName = m_FactoryObjectDataManager.getName(uuid);
        
        //check if there is a PID (and hence a configuration) already known for the given UUID
        final String pid = m_FactoryObjectDataManager.getPid(uuid);
        
        //fetch properties from the config admin
        final Configuration config = FactoryServiceUtils.getFactoryConfiguration(m_ConfigAdmin, pid);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        if (config != null)
        {
            properties = config.getProperties();
        }
        
        //the properties will be checked, to see if the object is satisfied
        final Dictionary<String, Object> withDefltProperties = getAllDeps(properties, factory);
        //evaluate if the object's deps are satisfied
        final DependencyState state = isSatisfied(uuid, withDefltProperties);
        if (state == DependencyState.INVALID)
        {
            // dependencies are invalid, will not create object ever, so do clean up
            m_FactoryObjectDataManager.tryRemove(uuid);
            tryDeleteConfiguration(uuid);
            throw new FactoryException(String.format("Unable to create/restore %s {%s}, unknown dependency.", 
                    m_BaseType, uuid));
        }
        else if (state == DependencyState.UNSATISFIED)
        {
            // missing dependency, don't create the object now, but add to the list of pending objects
            final PendingFactoryObject pendingObject =  new PendingFactoryObject(uuid, properties, factory); 
            m_PendingObjects.add(pendingObject);
            throw new FactoryException(String.format("Unable to create/restore %s {%s}, unsatisfied dependency.", 
                    m_BaseType, uuid));
        }
        
        final ComponentInstance compInst = m_ServiceProxy.createFactoryObjectInternal(factory);
        final T object = (T)compInst.getInstance();
        final FactoryObjectProxy proxy = factory.create();
        
        //proxy has not been initialized and should not be used yet, just sending for later use
        object.initialize(this, proxy, factory, m_ConfigAdmin, m_EventAdmin, m_PowerInternal, uuid,
                objectName, pid, m_BaseType);
        
        //this callback is used to allow lookup services to initialize any accessory
        //items that are needed before the object can be released into circulation
        try
        {
            m_Callback.preObjectInitialize(object);
            m_ServiceProxy.initializeProxy(object, proxy, ConfigurationUtils.convertDictionaryPropsToMap(properties));
            m_Callback.postObjectInitialize(object);
        }
        catch (final ConfigurationException ex)
        {
            m_Callback.onRemovedObject(object);
            throw new FactoryException(
                    String.format("Unable to update %s object", m_BaseType), ex);
        }
        
        // add factory object instance 
        addObject(object);
        
        //add component instance to map
        m_ComponentMap.put(object.getUuid(), compInst);

        // Call method on object to notify that creation is complete (e.g. then post asset status, etc.)
        object.postCreation();

        // return the object
        return object;
    }

    /**
     * Restore all objects associated with the given factory.
     * @param factory
     *      the factory to restore objects for 
     */
    public void restoreAllObjects(final FactoryInternal factory)
    {
        //collect all entries
        final Collection<PersistentData> objectDataCollection = m_FactoryObjectDataManager.getAllObjectData(factory);
        
        for (PersistentData data : objectDataCollection)
        {
            final UUID objUuid = data.getUUID();
            try
            {
                createOrRestoreObject(factory, objUuid);
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Failed to restore %s with UUID: %s", m_BaseType, 
                        objUuid.toString());
            }
        }
    }
    
    /**
     * Clear the PID from the object's persisted data.
     * @param object
     *      the object to remove the PID from
     * @throws IllegalArgumentException
     *      if the object is not known
     * @throws FactoryObjectInformationException
     *      if a problem occurs saving the object's data
     */
    public void unAssignPidForObj(final FactoryObjectInternal object) throws IllegalArgumentException, 
            FactoryObjectInformationException
    {
        object.setPid(null);
        m_FactoryObjectDataManager.clearPid(object.getUuid());
        
        //notify that the pid has been unassigned for an object.
        m_DirectoryService.postFactoryObjectEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED, 
                object);
    }
    
    /**
     * Create a configuration and assign the new PID to the object.
     * @param objUuid
     *      the UUID of the for which the PID is being persisted
     * @param factoryPid
     *      the PID to persist
     * @param object
     *      the object to assign the new configuration's PID to
     * @return
     *      the created configuration
     * @throws IllegalArgumentException
     *      if the UUID for the object cannot be found in the data store
     * @throws FactoryObjectInformationException
     *      if a problem occurs saving the object's data
     * @throws FactoryException
     *      if there was an error with the creation of the object or its configuration
     */
    
    public Configuration createConfiguration(final UUID objUuid, final String factoryPid, 
            final FactoryObjectInternal object) 
            throws IllegalArgumentException, FactoryObjectInformationException, FactoryException
    {
        final String pid;
        final Configuration config;
        try
        {
            //Create the configuration, put null as the location so that the configuration does not 
            //bind to the core bundle.
            config = m_ConfigAdmin.createFactoryConfiguration(factoryPid, null);
        }
        catch (final IOException e)
        {
            //clean up
            throw new FactoryException(String.format("Error creating configuration for %s with UUID [%s].",
                    m_BaseType, objUuid), e);
        }
        pid = config.getPid();
        
        //assign the PID if the object is not null
        if (object != null)
        {
            object.setPid(pid);
            
            //send out an event that notifies that the pid has been set for an object.
            m_DirectoryService.postFactoryObjectEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED, 
                    object);
        }
        m_FactoryObjectDataManager.setPid(objUuid, pid);
        return config;
    }
    
    /**
     * Create a dictionary of all possible dependencies that an object may have.
     * 
     * @param properties
     *      the properties to use for the object, this {@link Dictionary} will NOT be modified
     * @param factory
     *      the factory that creates instances of the desired object type
     * @return 
     *      dictionary containing all possible dependencies and properties for the object
     */
    private Dictionary<String, Object> getAllDeps(final Dictionary<String, Object> properties, 
            final FactoryDescriptor factory)
    {
        //clone the properties
        final Dictionary<String, Object> withDefltProperties = ConfigurationUtils.cloneDictionary(properties);
        
        //fetch factory default properties
        final Dictionary<String, Object> deflts = m_FactoryServiceUtils.getMetaTypeDefaults(factory);
        
        // Collect all default property keys
        final Enumeration<String> keys = deflts.keys();
        while (keys.hasMoreElements())
        {
            final String key = keys.nextElement();
            //don't replace a property if it is already there
            if (properties.get(key) == null)
            {
                //grab from default props
                withDefltProperties.put(key, deflts.get(key));
            }
        }
        return withDefltProperties;
    }
    
    /**
     * Check if an object ID is pending creation.
     * 
     * @param uuid
     *      UUID of the object to check for the pending state
     * @return
     *      <code>true</code> if object is pending, otherwise <code>false</code>
     */
    private boolean isPendingObject(final UUID uuid)
    {
        for (PendingFactoryObject pendingObj : m_PendingObjects)
        {
            if (pendingObj.getUuid().equals(uuid))
            {
                return true;
            }
        }

        return false;
    }

    ///////////////////////////////////////////////////
    //      Factory object dependency handling.      //
    ///////////////////////////////////////////////////
    
    /**
     * Check if all dependencies are satisfied for a given property set.
     * 
     * @param uuid
     *      UUID of the object to check its dependencies
     * @param properties
     *      properties for the object containing all standard properties
     * @return
     *      <code>true</code> if dependencies are satisfied, otherwise <code>false</code>
     */
    public DependencyState isSatisfied(final UUID uuid, final Dictionary<String, Object> properties)
    {
        for (RegistryDependency dep : m_Dependencies)
        {
            final String objectName = (String)properties.get(dep.getObjectNameProperty());
            if (Strings.isNullOrEmpty(objectName))
            {
                if (dep.isRequired()) // no prop value is only problem if dep is required
                {
                    m_Logging.debug("[%s] property is required and not set so ignoring dependency for [%s] {%s}.", 
                        dep.getObjectNameProperty(), m_BaseType, uuid);
                    return DependencyState.INVALID;
                }
            }
            else
            {
                final Object object = dep.findDependency(objectName);
                if (object == null)
                {
                    m_Logging.info("Missing dependency [%s] for %s", objectName, m_BaseType);
                    return DependencyState.UNSATISFIED;
                }
            }
        }
        // if we get here all deps have been check and are available
        return DependencyState.SATISFIED;
    }
    
    /**
     * Class to handle registering, handling, and unregistering for factory registry related events.
     * 
     * @author nickmarcucci
     *
     */
    class FactoryRegistryEventHandler implements EventHandler
    {
        /**
         * The service registration reference for this handler class.
         */
        private ServiceRegistration<EventHandler> m_ServiceRegistration;
        
        /**
         * Method used to register this handler class for factory events.
         */
        public void registerEvents()
        {
            final String[] topics = {FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, 
                FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED};
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EventConstants.EVENT_TOPIC, topics);
            m_ServiceRegistration = m_BundleContext.registerService(EventHandler.class, this, properties);
        }
        
        /**
         * Method used to unregister this handler class.
         */
        public void unregsiterEvents()
        {
            m_ServiceRegistration.unregister();
        }
        
        /**
         * Handle event when an object is created that may satisfy a dependency for another object.
         * 
         * @param event
         *      the event that contains the information about a created object
         */
        @Override
        public void handleEvent(final Event event)
        {
            synchronized (m_PendingObjects)
            {
                final Iterator<PendingFactoryObject> pendingIterator = m_PendingObjects.iterator();
                while (pendingIterator.hasNext())
                {
                    final PendingFactoryObject pendingObject = pendingIterator.next();
                    m_Logging.debug("checking pending %s with UUID=%s", m_BaseType,
                            pendingObject.getUuid());
                    
                    final Dictionary<String, Object> properties = pendingObject.getProperties();
                    final DependencyState state = isSatisfied(pendingObject.getUuid(), properties);
                    
                    if (state == DependencyState.SATISFIED)
                    {
                        pendingIterator.remove();  // remove iterator first so not checked again, 
                                                   // creation below will cause another check of 
                                                   // remaining pending objects
                        final FactoryInternal factory = pendingObject.getFactory();
                        try
                        {
                            createOrRestoreObject(factory, pendingObject.getUuid());
                        }
                        catch (final FactoryException e)
                        {
                            m_Logging.error(e, "Unable to create a pending %s [%s]", 
                                    m_BaseType, pendingObject);
                        }
                        catch (final FactoryObjectInformationException e)
                        {
                            m_Logging.error(e, "Persisted data for pending %s [%s] is unreadable.", 
                                m_BaseType, pendingObject);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Possible states for an objects dependencies.
     */
    public enum DependencyState
    {
        /** Dependency has an invalid value and can never be satisfied. */
        INVALID,
        
        /** Dependency is known, but is not currently available. */
        UNSATISFIED,
        
        /** Dependency is known and available. */
        SATISFIED
    }
}
