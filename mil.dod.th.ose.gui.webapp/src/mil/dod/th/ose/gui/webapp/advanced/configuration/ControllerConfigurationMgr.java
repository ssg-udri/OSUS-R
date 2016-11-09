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
 * Interface for the request scoped bean that is responsible for displaying data and handling request to and from the 
 * configuration tab of the system configuration XHTML page.
 * 
 * @author cweisenborn
 */
public interface ControllerConfigurationMgr
{
    /** Event topics prefix for all controller configuration manager events. */
    String TOPIC_CONTROLLER_CONFIGAURATION_PREFIX = 
            "mil/dod/th/ose/gui/webapp/advanced/configuration/ControllerConfigurationMgr/";
    
    /** 
     * Topic used to specify that the configuration display models stored by the controller configuration manager have
     * been updated. Contains no properties.
     */
    String TOPIC_CONFIG_DISPLAY_MODELS_UPDATED = 
            TOPIC_CONTROLLER_CONFIGAURATION_PREFIX + "CONFIG_DISPLAY_MODELS_UPDATED";
    
    /**
     * Method that sets the PID of the service that is to be removed.
     * 
     * @param pid
     *          PID to be set.
     */
    void setRemoveConfigPid(String pid);

    /**
     * Method that retrieves the PID of the service to be removed.
     * 
     * @return
     *          PID of the service to be removed.
     */
    String getRemoveConfigPid();

    /**
     * Method that retrieves factory configurations for the specified factory PID from the system configuration
     * manager.
     * 
     * @param controllerId
     *          ID of controller to retrieve the factory configuration from.
     * @param factoryPid
     *          PID of the factory to retrieve factory configurations for.
     * @return
     *          List of {@link ConfigAdminModel}s that represent the factory configurations for the specified 
     *          factory PID.
     */
    List<ConfigAdminModel> getFactoryConfigurationsAsync(int controllerId, String factoryPid);

    /**
     * Method used to set a configuration's values.
     * 
     * @param controllerId
     *          ID of the controller where the configuration is stored.
     * @param model
     *          The {@link ModifiableConfigMetatypeModel} that contains the values to be set.
     */
    void setConfigurationValuesAsync(int controllerId, ModifiableConfigMetatypeModel model);

    /**
     * Method that retrieves a configuration display model for the specified configuration.
     * 
     * @param controllerId
     *          ID of the controller the configuration display model pertains to.
     * @param pid
     *          PID of the configuration to retrieves a display model for.
     * @return
     *          {@link ModifiableConfigMetatypeModel} that represents the configuration with the specified PID.
     */
    ModifiableConfigMetatypeModel getConfigModelByPidAsync(int controllerId, String pid);

    /**
     * Method used to retrieve the bundle location for a configuration.
     * 
     * @param controllerId
     *          ID of the controller the configuration is located.
     * @param pid
     *          PID of the configuration to retrieve the bundle location.
     * @return
     *          String the represents the bundle location of the configuration. Will return null if no bundle location
     *          for the specified PID can be found.
     */
    String getConfigBundleLocationAsync(int controllerId, String pid);
}
