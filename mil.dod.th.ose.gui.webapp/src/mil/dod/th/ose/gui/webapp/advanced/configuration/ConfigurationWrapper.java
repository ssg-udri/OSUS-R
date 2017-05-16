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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.List;

/**
 * Interface for the application scoped bean that is used to retrieve an intermediary model that contains both meta type
 * and configuration information for a PID. Can also be used to set the value of a property for a configuration.
 * 
 * @author cweisenborn
 */
public interface ConfigurationWrapper
{
    /**
     * Method used to retrieve the intermediary model that represents meta type and configuration information for a
     * specified PID. If an empty PID is passed in, no model can be retrieved and null will be returned.
     * 
     * @param controllerId
     *          ID of the controller where the configuration with the specified PID is located.
     * @param pid
     *          PID of the service that an intermediary model is to be retrieved for.
     * @return
     *          {@link UnmodifiableConfigMetatypeModel} that represents the meta type 
     *          and configuration information for the specified PID.
     */
    UnmodifiableConfigMetatypeModel getConfigurationByPidAsync(int controllerId, String pid);
    
    /**
     * Method that retrieves the factory metatype defaults for a given factory PID.
     * @param controllerId
     *  ID of the controller where the configuration with the specified factory PID is located
     * @param factoryPid
     *  PID of the service that an intermediary model is to be retrieved for
     * @return
     *  {@link UnmodifiableConfigMetatypeModel} that represents the metatype information for the given PID 
     */
    UnmodifiableConfigMetatypeModel getConfigurationDefaultsByFactoryPidAsync(int controllerId, String factoryPid);
    
    /**
     * Method that retrieves the intermediary model that represents meta type and configuration information for a
     * specified property of a configuration. If an empty PID is passed in, no model can be retrieved and null will
     * be returned.
     * 
     * @param controllerId
     *          ID of the controller where the configuration with the specified PID is located.
     * @param pid
     *          PID of the service that contains the property to be retrieved.
     * @param key
     *          Key of the property to be retrieved.
     * @return
     *          {@link UnmodifiablePropertyModel} that represents the property 
     *          information or null if the property is not found
     */
    UnmodifiablePropertyModel getConfigurationPropertyAsync(int controllerId, 
            String pid, String key);
    
    /**
     * Method used to set the value of properties for a configuration.
     * 
     * @param controllerId
     *          ID of the controller that contains the configuration.
     * @param pid
     *          PID of the configuration with the property to be set.
     * @param properties
     *          List of {@link ModifiablePropertyModel} that contains the key and value pairs for the configuration
     *          properties to be set.
     */
    void setConfigurationValueAsync(int controllerId, String pid, List<ModifiablePropertyModel> properties);
    
    /**
     * Method that identifies the changed properties based on the controller id and the passed in PID.
     * @param controllerId
     *  the ID of the controller on which the configuration information resides
     * @param pid
     *  the PID of that is being used to retrieve configuration/meta type information
     * @param properties
     *  the properties of a configuration property model
     * @return
     *  the list of changed properties; list will be empty if no properties have been changed
     */
    List<ModifiablePropertyModel> findChangedPropertiesAsync(int controllerId, String pid, 
            List<ModifiablePropertyModel> properties);
    
}
