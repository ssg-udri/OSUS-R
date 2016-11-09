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
package mil.dod.th.core.factory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

/**
 * Provides the default interface to interact with all factory core objects.
 */
@ProviderType
public interface FactoryObject
{
    /** Prefix used to denote a Terra Harvest property. This includes both configuration and component properties. */
    String TH_PROP_PREFIX = "th";

    /**
     * Get the universally unique identifier of the object. 
     * 
     * @return Universally unique identifier
     */
    UUID getUuid();
    
    /**
     * Get the unique persistence ID of the object. The PID is used to reference the configuration in
     * {@link org.osgi.service.cm.ConfigurationAdmin}. If a PID is null, then no configuration exists
     * for this object.
     * 
     * @return Persistence ID
     */
    String getPid();
    
    /**
     * Retrieve the cached name of the factory object.
     *
     * @return The name of the object
     */
    String getName();

    /**
     * Set the given properties of the factory object via {@link org.osgi.service.cm.ConfigurationAdmin}. Properties
     * previously set will be unset if not included in the map. Must call {@link #getProperties()} if wanting to add
     * onto existing properties.
     * 
     * This method will block until the configuration has been updated.  While the configuration is being updated
     * the object's {@link FactoryObjectProxy#updated(java.util.Map)} method will be 
     * called and completed before this method returns.  
     * Do not call from {@link FactoryObjectProxy#updated(java.util.Map)}.
     * 
     * @param properties
     *            map of updated properties for the factory object
     * @throws FactoryException
     *             if there is an error in setting the properties
     * @throws IllegalArgumentException
     *             if key or value parameter is null
     * @throws IllegalStateException
     *             if object is not activated or ConfigurationAdmin is not available
     */
    void setProperties(Map<String, Object> properties) throws IllegalArgumentException, IllegalStateException, 
            FactoryException;

    /**
     * Get a copy of the map of currently set properties. This does not include default values for unset properties. Use
     * with an attribute interface like {@link mil.dod.th.core.asset.AssetAttributes} to retrieve default values or call
     * {@link mil.dod.th.core.asset.Asset#getConfig()} directly to get the current value or the default if not set.
     * 
     * @return A set of all property keys
     */
    Map<String, Object> getProperties();
    
    /**
     * Accessor method for the factory that created the object.
     * 
     * @return
     *      the factory that created the object 
     */
    FactoryDescriptor getFactory(); 
    
    /**
     * Get an {@link Extension} object. The extension type passed must also be available by calling, 
     * {@link #getExtensionTypes()}.
     * @param <T>
     *      the extension type being requested
     * @param type
     *      the class type of the extension object to get
     * @return
     *      the extension object of the passed class type
     * @throws IllegalArgumentException
     *      if the given class is not a known extension type
     */
    <T> T getExtension(Class<T> type) throws IllegalArgumentException;
    
    /** 
     * Get the types of {@link Extension}s supported. The class types returned can be passed to 
     * {@link #getExtension(Class)} to acquire that actual extension object.
     * @return
     *      the set of all supported extensions, may be empty if no extensions are supported 
     */
    Set<Class<?>> getExtensionTypes(); 
    
    /**
     * Remove the factory object from the directory service that is responsible for managing it. The factory object's 
     * configuration and any other associated resources will be cleaned up.
     * 
     * @throws IllegalStateException
     *      If the factory object is currently in a state that doesn't allow for removal.
     */
    void delete() throws IllegalStateException; 
    
    /**
     * Set the persisted name of the factory object.
     * 
     * @param name
     *     The new name of the factory object
     * @throws FactoryException
     *     Thrown if an error occurs saving the factory object name
     * @throws IllegalArgumentException
     *     Thrown if name is null, blank, or the duplicate of a name of another factory object of the same type.
     */
    void setName(String name) throws IllegalArgumentException, FactoryException;
    
    /**
     * Determine if the object is currently being managed by its respective directory service.
     * 
     * @return 
     *      true if the object is being managed by the directory service, false if not
     */
    boolean isManaged();
}
