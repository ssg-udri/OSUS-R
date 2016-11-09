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

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.system.TerraHarvestSystem;

/**
 * Interface that works with any Terra Harvest controller system. This is offered as an OSGi service by the core 
 * which represents the controller the core is running on, but the interface can also be used to represent other 
 * controllers. The behavior of a particular controller can be overridden by a controller's implementation of {@link 
 * TerraHarvestControllerProxy}. The fields are required to be persisted by a system separate
 * from data stores and caches (i.e., this information must be available in the of event of power-cycles, removal of 
 * bundle caches, or database removals).
 */
@ProviderType
public interface TerraHarvestController extends TerraHarvestSystem
{
    /** Event topic prefix to use for all topics in {@link TerraHarvestController}. */
    String TOPIC_PREFIX = "mil/dod/th/core/controller/";
    
    /**
     * Topic used when the operation mode of the controller has been changed.
     * 
     * Contains the following fields:
     * <ul>
     * <li> {@link #EVENT_PROP_SYSTEM_MODE} - the new operation mode of the controller.
     * </ul>
     */
    String TOPIC_CONTROLLER_MODE_CHANGED = TOPIC_PREFIX + "MODE_CHANGED";
    
    /** Event property key for the {@link OperationMode#value()} of the system. */
    String EVENT_PROP_SYSTEM_MODE = "operation.mode";
    
    /**
     * Get the version representing the entire {@link TerraHarvestController} runtime. Will return the version 
     * determined by the core or {@link TerraHarvestControllerProxy#getVersion()} if {@link 
     * ControllerCapabilities#isVersionOverridden()} is true.
     * 
     * @return
     *      String representing the version
     */
    String getVersion();
    
     /**
     * Get the build information for the entire {@link TerraHarvestController} runtime. Will return the info determined 
     * by the core or {@link TerraHarvestControllerProxy#getBuildInfo()} if {@link 
     * ControllerCapabilities#isBuildInfoOverridden()} is true. The keys are the property names while the values are 
     * free form fields that are left up to the system developer to determine what is returned, but would include things
     * like the time the software was built.
     * 
     * @return
     *      Map representing the build info
     */
    Map<String, String> getBuildInfo();
    
    /**
     * Request the controller capabilities. Will return a default capabilities object if no bundle provides the {@link 
     * TerraHarvestControllerProxy} service. If the proxy interface is provided, the bundle must then contain a 
     * capabilities XML file with the name 
     * {@link mil.dod.th.core.factory.FactoryDescriptor#CAPABILITIES_XML_FOLDER_NAME}/<{@link Class#getName()}>.xml for 
     * the class implementing the proxy.
     *       
     * @return the controller capabilities
     */
    ControllerCapabilities getCapabilities();
    
    /**
     * Get the controller's operation mode.
     * 
     * @return
     *     the current operation mode of the controller, will return {@link OperationMode#TEST_MODE} if the value is not
     *     found in the th.system.properties file or the file was not found.
     */
    OperationMode getOperationMode();

    /**
     * Set the controller's operation mode.
     * 
     * @param controllerMode
     *     the operation mode assigned to the controller
     */
    void setOperationMode(OperationMode controllerMode);
    
    /**
     * Enumeration representing the mode of a controller.
     */
    enum OperationMode
    {
        /** Means the controller is in operational mode. */
        OPERATIONAL_MODE("operational"),
        
        /** Means that the controller is in test mode. */
        TEST_MODE("test");
        
        /** String representation of the value. */
        private String m_Value;

        /** 
         * Operation mode enumeration constructor.
         * 
         * @param value
         *      string representation
         */
        OperationMode(final String value)
        {
            m_Value = value;
        }
        
        /**
         * Get the string representation of the mode.
         * 
         * @return
         *      string representation
         */
        public String value()
        {
            return m_Value;
        }
        
        /**
         * Get the enum value given the string representation.
         * 
         * @param value
         *      string representation
         * @return
         *      enum value
         */
        public static OperationMode fromValue(final String value)
        {
            for (OperationMode mode : OperationMode.values())
            {
                if (mode.value().equals(value))
                {
                    return mode;
                }
            }
            
            throw new IllegalArgumentException(value + " not a valid operation mode");
        }
    }
}

