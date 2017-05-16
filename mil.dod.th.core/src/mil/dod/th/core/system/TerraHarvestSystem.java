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
package mil.dod.th.core.system;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface to work with any Terra Harvest compliant system runtime. This is offered as an OSGi service by the core 
 * which represents the controller the core is running on, but the interface can also be used to represent other 
 * systems. The behavior of a particular controller can be overridden by a controller's implementation of {@link 
 * mil.dod.th.core.controller.TerraHarvestControllerProxy}. The fields are required to be persisted by a system separate
 * from data stores and caches (i.e., this information must be available in the of event of power-cycles, removal of 
 * bundle caches, or database removals).
 * 
 * @author dhumeniuk
 */
@ProviderType
public interface TerraHarvestSystem
{
    /**
     * Get the human-readable name of the software system.
     * 
     * <p>
     * Will return the name persisted by the core or {@link 
     * mil.dod.th.core.controller.TerraHarvestControllerProxy#getName()} if {@link 
     * mil.dod.th.core.controller.capability.ControllerCapabilities#isNameOverridden()} is true.
     * 
     * <p>
     * The platform can synchronize this with an operating system host-name or other identifying value.
     * The method will return "&lt;undefined&gt;" if the value is not set in the th.system.properties file or if the 
     * file was not found.  
     *    
     * @return
     *     String representing the meaningful name of the system.
     */
    String getName();
    
    /**
     * Set the human-readable name of the software system.
     * 
     * <p>
     * Core will persist the name or update will be handled by {@link 
     * mil.dod.th.core.controller.TerraHarvestControllerProxy#setName(String)} if {@link 
     * mil.dod.th.core.controller.capability.ControllerCapabilities#isNameOverridden()} is true.
     * 
     * <p>
     * This field is required to be persisted by the platform separate from datastores and caches.
     * The system's credentials must be available in the of event of power-cycles, removal of bundle caches,
     * or database removals.  
     * 
     * @param name
     *     Sets a String to represent the meaningful name of the system.
     */
    void setName(String name);
    
    /**
     * Get the identification number of the software system.
     * 
     * <p>
     * Will return the ID persisted by the core or {@link 
     * mil.dod.th.core.controller.TerraHarvestControllerProxy#getId()} if {@link 
     * mil.dod.th.core.controller.capability.ControllerCapabilities#isIdOverridden()} is true.
     * 
     * <p>
     * This must be the network unique identifier of the controller. This method will return -1 if the 'ID' value is
     * not set in the th.system.properties file or if the file was not found.
     * 
     * @return
     *     Return 32-bit integer ID
     */
    int getId();
    
    /**
     * Set the identification number of the software system.
     * 
     * <p>
     * Core will persist the ID or update will be handled by {@link 
     * mil.dod.th.core.controller.TerraHarvestControllerProxy#setId(int)} if {@link 
     * mil.dod.th.core.controller.capability.ControllerCapabilities#isIdOverridden()} is true.
     * 
     * <p>
     * This must be the network unique identifier of the controller. This field is required to be persisted by the 
     * platform separate from datastores and caches. The system's credentials must be available in the of event of 
     * power-cycles, removal of bundle caches, or database removals. 
     * 
     * @param id
     *     Sets given integer as the ID
     */
    void setId(int id); //NOPMD: short method name, name is descriptive of what is being set, which is an ID
}
