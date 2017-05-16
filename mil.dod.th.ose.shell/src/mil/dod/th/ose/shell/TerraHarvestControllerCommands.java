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
package mil.dod.th.ose.shell;

import java.io.PrintStream;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Terra Harvest Controller commands.
 * 
 * @author frenchpd
 */
@Component(provide = TerraHarvestControllerCommands.class, properties = {"osgi.command.scope=those", 
        "osgi.command.function=version|buildInfo|getCapabilities|mode|getMode|setMode" })
public class TerraHarvestControllerCommands
{    
    /**
     * Reference to the Terra Harvest system service.
     */
    private TerraHarvestController m_TerraHarvestController;
    
    /**
     * Sets the Terra Harvest Controller service.
     * @param terraHarvestController
     *          system to be set 
     */
    @Reference
    public void setTerraHarvestController(final TerraHarvestController terraHarvestController)
    {
        m_TerraHarvestController = terraHarvestController;
    }
    
    /**
     * Returns the capabilities of the Terra Harvest Controller.
     * 
     * @return String
     *           the version of software
     * @throws UnmarshalException 
     *            thrown when xml file cannot be marshaled
     */
    @Descriptor("Displays the capabilities of Terra Harvest Controller.")
    public ControllerCapabilities getCapabilities() throws UnmarshalException
    {
        return m_TerraHarvestController.getCapabilities();
    }
    
    /**
     * Returns the version of the Terra Harvest software.
     * 
     * @return String
     *           the version of software
     */
    @Descriptor("Displays the current version for the Terra Harvest system runtime.")
    public String version()
    {
        return m_TerraHarvestController.getVersion();
    }
    
    /**
     * Returns the build info for the Terra Harvest system runtime.
     * 
     * @return String
     *           the build info for software
     */
    @Descriptor("Displays the build info for the Terra Harvest system runtime.")
    public String buildInfo()
    {
        //line separator
        final String sep = System.getProperty("line.separator");
        
        //build to be returned
        final StringBuilder builder = new StringBuilder();
        //get the build information
        final Map<String, String> props = m_TerraHarvestController.getBuildInfo();
        //iterate over keys and format the data
        for (String key : props.keySet())
        {
            builder.append(key).append(": ").append(props.get(key)).append(sep);
        }
        return builder.toString();
    }
    
    /**
     * Sets the operation mode of the Terra Harvest System.
     * 
     * @param session 
     *            CommandSession for console output
     * @param systemMode
     *            the operation mode of the system to be set to
     */
    @Descriptor("Sets the operation mode of Terra Harvest System.")
    public void setMode(final CommandSession session,
            @Descriptor("System operation mode to be set. Available modes are 'test' and 'operational'.")
            final String systemMode)
    {
        try
        {
            m_TerraHarvestController.setOperationMode(OperationMode.fromValue(systemMode.toLowerCase()));
        }
        catch (final IllegalArgumentException exception)
        {
            final PrintStream out = session.getConsole();
            out.println("Invalid system mode");
        }
    }
    
    /**
     * Gets the current controller operation mode of the Terra Harvest System.
     *
     * @return 
     *      Returns the current operation mode of the system
     */
    @Descriptor("Gets the current operation mode of the Terra Harvest System")
    public String getMode()
    {
        return m_TerraHarvestController.getOperationMode().value();
    }   
}
