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
import java.util.Map;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;

/**
 * Interface for the application scoped system configuration manager bean. This bean contains a list of all known 
 * controllers and the configurations those controllers contain. The configurations being stored need to be modifiable
 * since all data on a configuration is not retrieved at one time. Also the configuration may be updated and therefore
 * needs be modifiable.
 * 
 * @author cweisenborn
 */
public interface SystemConfigurationMgr
{
    /** Topic used for a remote configuration deleted event. */
    String TOPIC_CONFIGURATION_DELETED_REMOTE = 
            ConfigurationEventConstants.TOPIC_CONFIGURATION_DELETED_EVENT + RemoteConstants.REMOTE_TOPIC_SUFFIX;
    
    /** Topic used for a remote configuration update event. */
    String TOPIC_CONFIGURATION_UPDATED_REMOTE = 
            ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT + RemoteConstants.REMOTE_TOPIC_SUFFIX;
    
    /** Topic used for a remote configuration location change event. */
    String TOPIC_CONFIGURATION_LOCATION_REMOTE = 
            ConfigurationEventConstants.TOPIC_CONFIGURATION_LOCATION_EVENT + RemoteConstants.REMOTE_TOPIC_SUFFIX;
    
    /** Event topics prefix for all system configuration manager events. */
    String TOPIC_SYSTEM_CONFIG_PREFIX = "mil/dod/th/ose/gui/webapp/advanced/configuration/SystemConfigurationMgr/";
    
    /**
     * Topic used to specify information on a remote configuration has been received and the model(s) representing that
     * information has been updated. Contains the following properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the bundle is located on.
     * <li>{@link mil.dod.th.ose.config.event.constants.ConfigurationEventConstants#EVENT_PROP_PID} - PID of the 
     * configuration that was updated. Will be null if more than one configuration was updated.
     * <li>{@link mil.dod.th.ose.config.event.constants.ConfigurationEventConstants#EVENT_PROP_FACTORY_PID} - PID of the
     * factory associated with the configuration. May be null if the configuration did not originate from a factory or
     * if more than one configuration was updated.
     * </ul>
     */
    String TOPIC_CONFIG_MODEL_UPDATED = TOPIC_SYSTEM_CONFIG_PREFIX + "CONFIG_MODEL_UPDATED";
    
    /**
     * Method used to retrieve a map of all configurations associated with a specific controller. The key is the PID
     * of the configuration and the value is a configuration model that represents the configuration. This method may
     * return null if no configurations for the specified controller are found.
     * 
     * @param controllerId
     *          ID of the controller to retrieve a list of configurations for.
     * @return
     *          A map of configurations that pertain to the specified controller. The key is the PID of the
     *          configuration the value is a model that represents the configuration.
     */
    Map<String, ConfigAdminModel> getConfigurationsAsync(int controllerId);

    /**
     * Method that retrieves the configuration model for the configuration with the specified PID. This method may
     * return null if no configuration with the specified PID is found.
     * 
     * @param controllerId
     *          ID of the controller that contains the specified configuration.
     * @param pid
     *          PID of the configuration.
     * @return
     *          {@link ConfigAdminModel} that contains information for the specified configuration. May return null
     *          if the configuration is not found.
     */
    ConfigAdminModel getConfigurationByPidAsync(int controllerId, String pid);

    /**
     * Method that returns a map of all factory configurations for the specified factory. May return null if no factory
     * with the specified factory PID exists. The key is the PID of the factory configuration and the value is a model
     * that represents the factory configuration. This method may return null if no factory configurations for the 
     * specified factory are found.
     * 
     * @param controllerId
     *          ID of the controller that contains the specified factory.
     * @param factoryPid
     *          Factory PID of the factory to retrieve all factory configurations for.
     * @return
     *          A map of {@link ConfigAdminModel}s that contain information for all factory configurations for the
     *          specified factory. May return null if no factory with the specified PID exists. The key is the PID of
     *          the factory configuration and the value is a model that represents the factory configuration.
     */
    Map<String, ConfigAdminModel> getFactoryConfigurationsByFactoryPidAsync(int controllerId, String factoryPid);

    /**
     * Method that returns all properties for the configuration with the specified PID. May return null if no 
     * properties can be found for the specified PID.
     * 
     * @param controllerId
     *          Controller that contains the configuration with properties to be retrieved.
     * @param pid
     *          PID of the configuration to retrieve the properties of.
     * @return
     *          A list of {@link ConfigAdminPropertyModel}s that contain information on properties 
     *          for the configuration with the specified PID. May return null if no properties can be 
     *          found for the specified PID.
     */
    List<ConfigAdminPropertyModel> getPropertiesByPidAsync(int controllerId, String pid);

    /**
     * Method that sets the value for a property of the specified configuration.
     * 
     * @param controllerId
     *          Controller that contains the property to be set.
     * @param pid
     *          PID of the configuration with the property to be set.
     * @param properties
     *          List of {@link ModifiablePropertyModel} that represent the key and value pairs for the configuration 
     *          properties to be set.         
     */
    void setConfigurationValueAsync(int controllerId, String pid, List<ModifiablePropertyModel> properties);

    /**
     * Method used to create a new factory configuration given the factory PID.
     * 
     * @param controllerId
     *          ID of the controller that contains the factory to create a configuration for.
     * @param factoryPid
     *          Factory PID of the factory to create a configuration for.
     */
    void createFactoryConfigurationAsync(int controllerId, String factoryPid);

    /**
     * Method that removes a configuration from a controller.
     * 
     * @param controllerId
     *          ID of the controller that contains the configuration to be removed.
     * @param pid
     *          PID of the configuration to be removed.
     */
    void removeConfigurationAsync(int controllerId, String pid);
}
