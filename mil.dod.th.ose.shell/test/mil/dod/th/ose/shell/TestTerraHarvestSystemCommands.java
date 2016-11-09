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

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.system.TerraHarvestSystem;

/**
 * @author cweisenborn
 */
public class TestTerraHarvestSystemCommands
{
    private TerraHarvestSystemCommands m_SUT;
    private TerraHarvestSystem m_TerraHarvestSystem;
    
    @Before
    public void setup()
    {
        m_SUT = new TerraHarvestSystemCommands();  
        m_TerraHarvestSystem = mock(TerraHarvestSystem.class);     
        m_SUT.setTerraHarvestSystem(m_TerraHarvestSystem);
    }
   
    /**
     * Verify that the system id can be set using the shell command as a hex value.
     */
    @Test
    public void testSetId_Hex()
    {
        String hexId = "0x1234abcd";
        final CommandSession testSession = mock(CommandSession.class);
        m_SUT.setId(testSession, hexId);
        verify(m_TerraHarvestSystem).setId(0x1234abcd);
    }
    
    /**
     * Verify that the system id can be set using the shell command as a hex value.
     */
    @Test
    public void testSetId_Decimal()
    {
        String decimalId = "12345689";
        final CommandSession testSession = mock(CommandSession.class);
        m_SUT.setId(testSession, decimalId);
        verify(m_TerraHarvestSystem).setId(12345689);
    }
   
    /**
     * Verify that error log is sent when an invalid system id is set.
     */
    @Test
    public void testSetId_Invalid()
    {
        final CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);
        
        m_SUT.setId(testSession, "pq@");
        verify(testStream, atLeastOnce()).println(
                "Invalid system id. System id must be in hexadecimal (and start with 0x) or decimal format.");
    }
    
    /*
     * Verify that system id can be gotten using the shell command
     * getId
     */  
    @Test
    public void testGetId()
    {
        when(m_TerraHarvestSystem.getId()).thenReturn(0x25);
        
        assertThat(m_SUT.getId(), is("0x00000025"));
    }
    
    @Test
    public void testSetName()
    {
        String test = "name";
        
        m_SUT.setName(test);
        
        verify(m_TerraHarvestSystem).setName(test);
    }
    
    @Test
    public void testGetName()
    {   
        when(m_TerraHarvestSystem.getName()).thenReturn("name");
        
        assertThat(m_SUT.getName(), is("name"));
    }
}
