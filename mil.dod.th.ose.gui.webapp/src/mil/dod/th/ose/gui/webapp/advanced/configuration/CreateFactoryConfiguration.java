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
 * Interface for holding the selected configuration property setters for creating a new factory configuration.
 * @author matt
 *
 */
public interface CreateFactoryConfiguration
{
    /**
     * Get the currently selected list of modifiable properties.
     * @return
     *      the list of modifiable properties for creating a new factory configuration.
     */
    List<ModifiablePropertyModel> getPropertiesList();
    
    /**
     * Set the selected configuration properties to be used for creating a new factory configuration.
     * 
     * @param config
     *      the unmodifiable model which contains config + metatype values that are to be used
     * @param pid
     *      the pid of the factory object to create
     * @param controllerId
     *      the controller to send the create configuration request to in the 
     *      event the user wishes to create configuration
     */
    void setPropertiesList(UnmodifiableConfigMetatypeModel config, String pid, int controllerId);
    
    /**
     * Create the configuration using the properties that the user set.
     */
    void createConfiguration();
   
}
