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
package mil.dod.th.core.controller;

import java.util.Map;

/**
 * Proxy implemented by a plug-in to override the behavior of the {@link TerraHarvestController} service. The 
 * capabilities of the controller must also state the property is overridden using methods like {@link 
 * mil.dod.th.core.controller.capability.ControllerCapabilities#isVersionOverridden()}. It is not required for a 
 * controller to provide this service when no functionality needs to be overridden or the basic capabilities are 
 * sufficient. The bundle that implements this class must also contain a capabilities XML file with the name 
 * {@link mil.dod.th.core.factory.FactoryDescriptor#CAPABILITIES_XML_FOLDER_NAME}/<{@link Class#getName()}>.xml.
 * 
 * @author dhumeniuk
 *
 */
public interface TerraHarvestControllerProxy
{
    /**
     * Get the version of the entire Terra Harvest controller runtime. This is a free form field and is left up to the 
     * system developer to determine what is returned, but is typically a period delimited string like "1.0.0".
     * 
     * @return
     *      String representing the version
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    String getVersion() throws UnsupportedOperationException;
    
     /**
     * Get the build information for the entire Terra Harvest controller runtime.  The keys are the property names while
     * the values are free form fields that are left up to the controller developer to determine what is returned, but 
     * would include things like the time the software was built.
     * 
     * @return
     *      Map representing the build info
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    Map<String, String> getBuildInfo() throws UnsupportedOperationException;
    
    /**
     * Get the human-readable name of the controller.
     * 
     * <p>
     * The platform can synchronize this with an operating system host-name or other identifying value.
     *    
     * @return
     *      String representing the meaningful name of the system.
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    String getName() throws UnsupportedOperationException;
    
    /**
     * Set the human-readable name of the controller.
     * 
     * <p>
     * This field is required to be persisted by the platform separate from datastores and caches.
     * The system's credentials must be available in the of event of power-cycles, removal of bundle caches,
     * or database removals.  
     * 
     * @param name
     *      sets a String to represent the meaningful name of the system.
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    void setName(String name) throws UnsupportedOperationException;
    
    /**
     * Get the identification number of the controller. This must be the network unique identifier of the controller.
     * 
     * @return
     *      return 32-bit integer ID
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    int getId() throws UnsupportedOperationException;
    
    /**
     * Set the identification number of the controller. This must be the network unique identifier of the controller.
     * 
     * <p>
     * This field is required to be persisted by the platform separate from datastores and caches.
     * The system's credentials must be available in the of event of power-cycles, removal of bundle caches,
     * or database removals.
     * 
     * @param id
     *      sets given integer as the ID
     * @throws UnsupportedOperationException
     *      if the operation is not overridden by the plug-in
     */
    void setId(int id) throws UnsupportedOperationException; // NOPMD: short method name: name is descriptive of 
                                                             // what is being set, which is an ID
}
