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
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;

/**
 * @author frenchpd
 */
public class TestTerraHarvestControllerCommands
{
    private TerraHarvestControllerCommands m_SUT;
    private TerraHarvestController m_TerraHarvestController;
    
    @Before
    public void setup()
    {
        m_SUT = new TerraHarvestControllerCommands();
        
        m_TerraHarvestController = mock(TerraHarvestController.class);
        
        m_SUT.setTerraHarvestController(m_TerraHarvestController);
    }
    
    @Test
    public void testGetCapabilities() throws UnmarshalException
    {
        ControllerCapabilities test = new ControllerCapabilities();
          
        when(m_TerraHarvestController.getCapabilities()).thenReturn(test);
        
        assertThat(m_SUT.getCapabilities(), is(test));
    }
    
    /**
     * Test getting the version info.
     */
    @Test
    public void testGetVersion()
    {
        when(m_TerraHarvestController.getVersion()).thenReturn("1.0");
        
        assertThat(m_SUT.version(), is("1.0"));
    }
    
    /**
     * Test getting the build info
     */
    @Test
    public void testGetBuildInfo()
    {
        //map of props
        Map<String, String> props = new HashMap<String, String>();
        props.put("key.prop1", "info");
        props.put("key.prop2", "infoAlso");
        
        when(m_TerraHarvestController.getBuildInfo()).thenReturn(props);
        
        //verify formatting
        assertThat(m_SUT.buildInfo(), containsString("key.prop2: infoAlso"));
        assertThat(m_SUT.buildInfo(), containsString("key.prop1: info"));
    }
    
    /*
     * Verify that the operation mode of the system can be set using the shell command.
     */
    @Test
    public void testMode()
    {
        final CommandSession testSession = mock(CommandSession.class);
        m_SUT.setMode(testSession, "OperatIOnal");
        verify(m_TerraHarvestController).setOperationMode(OperationMode.OPERATIONAL_MODE);
        
        m_SUT.setMode(testSession, "TEst");   
        verify(m_TerraHarvestController).setOperationMode(OperationMode.TEST_MODE);       
    }
    
    /*
     * Verify that error log is sent when an invalid system mode is set
     */
    @Test
    public void testInvalidMode()
    {
        final CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);
        
        m_SUT.setMode(testSession, "Invalid");
        
        verify(m_TerraHarvestController, never()).setOperationMode(Mockito.any(OperationMode.class));
        verify(testStream).println("Invalid system mode");
    }
    
    /*
     * Verify that the current operation mode of the system can be gotten using the shell command.
     */
    @Test
    public void testGetMode()
    {   
        when(m_TerraHarvestController.getOperationMode()).thenReturn(OperationMode.OPERATIONAL_MODE);
        
        assertThat(m_SUT.getMode(), is("operational"));
        
        when(m_TerraHarvestController.getOperationMode()).thenReturn(OperationMode.TEST_MODE);
        
        assertThat(m_SUT.getMode(), is("test"));
    }
}
