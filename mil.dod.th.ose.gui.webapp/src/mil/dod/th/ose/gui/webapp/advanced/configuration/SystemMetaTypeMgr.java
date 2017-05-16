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
 * Interface for the system meta type manager bean. This bean contains a list of known controllers and all meta type
 * information stored on those controllers.
 * 
 * @author cweisenborn
 */
public interface SystemMetaTypeMgr
{
    /** Event topics prefix for all system meta type manager events. */
    String TOPIC_SYSTEM_METATYPE_PREFIX = "mil/dod/th/ose/gui/webapp/advanced/configuration/SystemMetaTypeMgr/";
    
    /**
     * Topic used to specify remote meta type information has been received and that the model(s) representing that 
     * information has been updated. Contains the following properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the information is located on.
     * </ul>
     */
    String TOPIC_METATYPE_MODEL_UPDATED = TOPIC_SYSTEM_METATYPE_PREFIX + "METATYPE_MODEL_UPDATED";
    
    /**
     * Method that returns a list of meta type information for all configurations on the specified controller.
     * 
     * @param controllerId
     *          ID of the controller to retrieve a meta type information for.
     * @return
     *          List of {@link MetaTypeModel}s that contains meta type information for all possible configurations 
     *          on the specified controller.
     */
    List<MetaTypeModel> getConfigurationsListAsync(int controllerId);

    /**
     * Method that returns a list of meta type information for all factories on the specified controller.
     * 
     * @param controllerId
     *          ID of the controller to retrieve a meta type information for.
     * @return
     *          List of {@link MetaTypeModel}s that contains all meta type information for factories on the specified 
     *          controller.
     */
    List<MetaTypeModel> getFactoriesListAsync(int controllerId);

    /**
     * Method that returns the meta type information for a single configuration with the specified PID. May return
     * null if the meta type model with the specified PID does not exist.
     * 
     * @param controllerId
     *          ID of the controller where the meta type information is stored.
     * @param pid
     *          PID of the service to retrieve meta type information for.
     * @return
     *          {@link MetaTypeModel} that represents the meta type information for the specified configuration. May
     *          return null with the meta type model with the specified PID does not exist.
     */
    MetaTypeModel getConfigInformationAsync(int controllerId, String pid);
    
    /**
     * Method that returns meta type information for a single factory with the specified factory PID. May return
     * null if the meta type model with the specified factory PID does not exist.
     * 
     * @param controllerId
     *          ID of the controller where the meta type information is stored.
     * @param factoryPid
     *          PID of the factory to retrieve meta type information for.
     * @return
     *          {@link MetaTypeModel} that represents the meta type information for the specified factory. May return
     *          null if the meta type model with the specified factory PID does not exist.
     */
    MetaTypeModel getFactoryInformationAsync(int controllerId, String factoryPid);
}
