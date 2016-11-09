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
package mil.dod.th.ose.gui.webapp.controller;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.ose.gui.webapp.utils.HexConverter;


/**
 * Base type for controllers. Each controller is verified via a 
 * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}, the response to this message 
 * will update the controller's channels. The status of the all channels for the controller is how the controller
 * status is rendered.
 * 
 * @author callen
 *
 */
public class ControllerModel
{
    /**
     * Holds the controllers id. 
     */
    private final int m_Id;
    
    /**
     * Reference to the controller image class.
     */
    private final ControllerImage m_ControllerImage;
    
    /**
     * The controller's name, this is set in response to system information requested from the
     * actual controller via a terra harvest message.   
     */
    private String m_Name;
    
    /**
     * The version of the software that this controller is running. 
     */
    private String m_Version;
    
    /**
     * The build information of the software that this controller is running. 
     */
    private Map<String, String> m_BuildInfo;
    
    /**
     * The current mode the controller is in.
     */
    private OperationMode m_Mode;
    
    /**
     * Holds a reference to the controller capabilities.
     */
    private ControllerCapabilities m_ControllerCaps;
    
    /**
     * Flag denoting if a controller is ready to send and receive messages.  
     */
    private boolean m_IsReady;
    
    /**
     * Flag denoting whether or not this controller needs a cleanup request or not.
     */
    private boolean m_NeedsCleanupRequest;
    
    /**
     * Public constructor for controller base objects.
     * @param controllerId
     *    identification number of the controller
     * @param imgInterface
     *    the image interface to use.
     */
    public ControllerModel(final int controllerId, final ControllerImage imgInterface)
    {
        super();
        m_Id = controllerId;
        m_ControllerImage = imgInterface;
        m_NeedsCleanupRequest = true;
    }
    
    /**
     * Get the id of the controller.
     * @return
     *    integer representation of the controller's id
     */
    public int getId()
    {
        return m_Id;
    }
    
    /**
     * Set the controller's name, this is expected to be a human readable name.
     * @param name
     *    the name to set for the controller
     */
    public void setName(final String name)
    {
        m_Name = name;
    }
    
    /**
     * Get the name of the controller.
     * @return
     *    string representation of the controller's name
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Set the version of software that this(the represented) controller has.
     * @param version
     *     the {@link mil.dod.th.core.system.TerraHarvestSystem} version of the software
     */
    public void setVersion(final String version)
    {
        m_Version = version;
    }
    
    /**
     * Get the version of software that this(the represented) controller has.
     * @return
     *     a string representing the {@link mil.dod.th.core.system.TerraHarvestSystem} version.
     */
    public String getVersion()
    {
        return m_Version;
    }
    
    /**
     * Set the build info for software that this(the represented) controller has.
     * @param buildInfo
     *     the {@link mil.dod.th.core.system.TerraHarvestSystem} build info of the software
     */
    public void setBuildInfo(final Map<String, String> buildInfo)
    {
        m_BuildInfo = buildInfo;
    }
    
    /**
     * Get the build information of software that this(the represented) controller has.
     * @return
     *     list string pairs representing the {@link mil.dod.th.core.system.TerraHarvestSystem} build info.
     */
    public Map<String, String> getBuildInfo()
    {
        if (m_BuildInfo == null)
        {
            return new HashMap<String, String>();
        }
        return new HashMap<String, String>(m_BuildInfo);
    }
    
    /**
     * Get the build information keys of software that this(the represented) controller has.
     * @return
     *     list string keys representing the {@link mil.dod.th.core.system.TerraHarvestSystem} build info.
     */
    public List<String> getBuildInfoKeys()
    {
        if (m_BuildInfo == null)
        {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(m_BuildInfo.keySet());
    }
    
    /**
     * Function to retrieve the controller id as a formatted hex string.
     * @return
     *  the string hex representation of the controller id
     */
    public String getHexId()
    {
        final HexConverter converter = new HexConverter();
        
        return converter.getAsString(null, null, m_Id);
    }
    
    /**
     * Method used to set the current mode of operation for the controller.
     * 
     * @param mode
     *      The {@link OperationMode} to be set.
     */
    public void setOperatingMode(final OperationMode mode)
    {
        m_Mode = mode;
    }
    
    /**
     * Method that returns the current operating mode for the controller.
     * 
     * @return
     *      The current {@link OperationMode} for the controller.
     */
    public OperationMode getOperatingMode()
    {
        return m_Mode;
    }
    
    /**
     * Function used to set the {@link ControllerCapabilities} for this controller model.
     * @param caps
     *  the capabilities object for this model.
     */
    public void setCapabilities(final ControllerCapabilities caps)
    {
        m_ControllerCaps = caps;
    }
    
    /**
     * Function used to get the {@link ControllerCapabilities} for this controller model.
     * @return The Capabilities object.
     */
    public ControllerCapabilities getCapabilities()
    {
        return m_ControllerCaps;
    }
    
    /**
     * Get the image that pertains to the controller.
     * @return
     *      the string URL of the image
     */
    public String getImage()
    {
        return m_ControllerImage.getImage(m_ControllerCaps);
    }
    
    /**
     * Method that returns a textual representation of a controller's operation mode to be
     * displayed to the user.  
     * @return
     *      textual representation of a controller's operation mode
     */
    public String getOperatingModeDisplayText()
    {
        return m_Mode.toString().replace('_', ' ');
    }
    
    /**
     * Method which returns a boolean indicating whether or not the controller
     * is in Operational Mode.
     * @return
     *  <code>true</code> if controller is in Operational Mode.  <code>false</code>
     *  is controller is in Test Mode.  
     */
    public boolean isOperationalMode()
    {
        return m_Mode == OperationMode.OPERATIONAL_MODE;
    }
    
    /**
     * Method that returns the ready state of the controller.
     * @return
     *      <code>true</code> if the controller is ready to send and receive messages.  <code>false</code> if
     *      the controller is not ready to send or receive messages. 
     */
    public boolean isReady()
    {
        return m_IsReady;
    }
    
    /**
     * Method used to set the ready state of the controller. 
     * @param isReady
     *      Set to <code>true</code> if the controller is ready to send and receive messages.  Set to 
     *      <code>false</code> if the controller is not ready to send or receive messages. 
     */
    public void setIsReady(final boolean isReady)
    {
        m_IsReady = isReady;
    }
    
    /**
     * Method used to indicate whether a cleanup request is needed or not.
     * @return
     *  true indicates that the cleanup request needs to be sent; false otherwise
     */
    public boolean needsCleanupRequest()
    {
        return m_NeedsCleanupRequest;
    }
    
    /**
     * Method used to set whether or not a cleanup request needs to be set.
     * @param needsCleanup
     *  indicates whether or not cleanup request needs to be sent
     */
    public void setNeedsCleanupRequest(final boolean needsCleanup)
    {
        m_NeedsCleanupRequest = needsCleanup;
    }
}
