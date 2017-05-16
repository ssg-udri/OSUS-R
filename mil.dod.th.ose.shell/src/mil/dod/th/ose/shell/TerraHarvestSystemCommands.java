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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.utils.numbers.Integers;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Terra Harvest system commands.
 * 
 * @author cweisenborn
 */
@Component(provide = TerraHarvestSystemCommands.class, properties = {"osgi.command.scope=those", 
        "osgi.command.function=id|getId|setId|name|getName|setName" })
public class TerraHarvestSystemCommands
{
    /**
     * Reference to the Terra Harvest system service.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;
   
    /**
     * Sets the Terra Harvest system service.
     * 
     * @param terraHarvestSystem
     *           service to be set
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    /**
     * Sets the id of the Terra Harvest System.
     * @param session 
     *           CommandSession for console output
     * @param systemId
     *           string representing the system id (hex or decimal format)
     */
    @Descriptor("Sets the Terra Harvest System ID.")
    public void setId(final CommandSession session,
            @Descriptor("System ID to be set in hexadecimal or decimal format.")
            final String systemId)
    {
        final int intId;
        try
        {
            intId = Integers.parseHexOrDecimal(systemId);
        }
        catch (final NumberFormatException e)
        {
            final PrintStream out = session.getConsole();
            out.println("Invalid system id. System id must be in hexadecimal (and start with 0x) or decimal format.");
            return;
        }
        
        m_TerraHarvestSystem.setId(intId);
    }
    
    /**
     * Returns the id of the Terra Harvest System.
     * 
     * @return String 
     *            the id of the Terra Harvest System in hexadecimal format
     */
    @Descriptor("Returns the Terra Harvest System ID.")
    public String getId()
    {
        final int strId = m_TerraHarvestSystem.getId();
        return String.format("0x%08x", strId);    
    }
    
    /**
     * Sets the name of the Terra Harvest System Software.
     * 
     * @param name
     *            the name of the system to be set
     */
    @Descriptor("Sets the name of the Terra Harvest System.")
    public void setName(
            @Descriptor("The name of the system to be set.")
            final String name)
    {
        m_TerraHarvestSystem.setName(name);
    }
    
    /**
     * Returns the name of the Terra Harvest System.
     * 
     * @return String
     *             the name of the Terra Harvest System.
     */
    @Descriptor("Returns the name of the Terra Harvest System.")
    public String getName()
    {
        return m_TerraHarvestSystem.getName();
    }
}
