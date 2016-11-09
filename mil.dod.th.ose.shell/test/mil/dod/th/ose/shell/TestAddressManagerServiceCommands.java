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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;

/**
 * @author cweisenborn
 */
public class TestAddressManagerServiceCommands
{
    private AddressManagerServiceCommands m_SUT;
    private AddressManagerService m_AddressManagerService;
   
    @Before
    public void setup() throws Exception
    {
        m_SUT = new AddressManagerServiceCommands();
        
        m_AddressManagerService = mock(AddressManagerService.class);
        
        m_SUT.setAddressManagerService(m_AddressManagerService);
    }
    
    @Test
    public void testGetOrCreateAddress() throws CCommException
    {
        Address testAddress = mock(Address.class);
       
        when(m_AddressManagerService.getOrCreateAddress(anyString())).thenReturn(testAddress);
        
        assertThat(m_SUT.getOrCreateAddress("test"), equalTo(testAddress));
    }
    
    @Test
    public void testPrintDeepSession()
    {
        PrintStream printStream = mock(PrintStream.class);
        CommandSession session = mock(CommandSession.class);
        
        when(session.getConsole()).thenReturn(printStream);
        
        m_SUT.printDeep(session);
        
        verify(m_AddressManagerService).printDeep(printStream);
    }
}
