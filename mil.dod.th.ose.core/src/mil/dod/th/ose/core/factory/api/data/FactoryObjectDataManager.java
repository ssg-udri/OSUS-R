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

import java.util.Collection;
import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.persistence.PersistentData;


/**
 * This interface assists with the management and control of factory object information. Most notably this interface
 * is used to persistently store information about factory objects. All service that create factory objects need to 
 * use either the {@link BaseFactoryObjectDataManager} or a specific implementation for the type of factory object that
 * extends this interface.
 * @author callen
 *
 */
public interface FactoryObjectDataManager
{
    /**
     * Get the name of a factory object.
     * @param uuid
     *     the UUID of the object
     * @return
     *     the name of the object with the given UUID 
     * @throws FactoryObjectInformationException
     *     if the information is unable to be parsed
     * @throws IllegalArgumentException
     *     if there is no object data associated with the given UUID
     */
    String getName(UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Set the name of a factory object. This should be called when factory object data already exists
     * for the object being named.
     * @param uuid
     *     the UUID of the object
     * @param name
     *     the desired name to be assigned
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the setting of the name to be persisted
     * @throws IllegalArgumentException
     *     if there is no object data associated with the given UUID
     */
    void setName(UUID uuid, String name) throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Persist the basic data of a factory object, including the name and factoryPid.
     * @param uuid
     *     the UUID of the object
     * @param factory
     *     the factory that created or will create the object
     * @param name
     *     the desired name to be assigned
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the setting of the name to be persisted
     * @throws IllegalArgumentException
     *     if the UUID is already being used by another object
     */
    void persistNewObjectData(UUID uuid, FactoryDescriptor factory, String name) 
            throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Return the UUID of a persistent data store entry for the given object name.
     * @param name
     *     the name to search for
     * @return UUID
     *     the unique identifier of the data store entry, will return null if no entry exists for the given name
     */
    UUID getPersistentUuid(String name);
    
    /**
     * Return the configuration PID of the factory object.
     * @param uuid
     *     the UUID of the object
     * @return
     *     the configuration PID of the factory object, returns null if there is no PID
     * @throws FactoryObjectInformationException
     *     if the information is unable to be parsed 
     * @throws IllegalArgumentException
     *     if the UUID is not associate with an object
     */
    String getPid(UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Set the configuration PID of a factory object.
     * @param uuid
     *     the UUID of the object
     * @param pid
     *     the configuration PID to be assigned
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents setting of the PID
     * @throws IllegalArgumentException
     *     if the UUID is not associated with an object, or the PID is null
     */
    void setPid(UUID uuid, String pid) throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Remove the configuration PID from the factory object. This method will be called when the configuration
     * of an object is deleted.
     * @param uuid
     *     the UUID of the object
     * @throws FactoryObjectInformationException
     *     if the information is unable to be parsed or if there is an error 
     *     merging information into the persistent data store
     * @throws IllegalArgumentException
     *     if the UUID is not associated with an object
     */
    void clearPid(UUID uuid) throws FactoryObjectInformationException, IllegalArgumentException;
    
    /**
     * Remove the entry for an object.
     * @param uuid
     *     the UUID of the object
     */
    void tryRemove(UUID uuid);
    
    /**
     * Get all {@link PersistentData} objects associated with a factory.
     * 
     * @param factory
     *      factory to limit the list of objects
     * @return
     *      collection of data entries associated with the passed factory PID
     */
    Collection<PersistentData> getAllObjectData(FactoryDescriptor factory);
}
