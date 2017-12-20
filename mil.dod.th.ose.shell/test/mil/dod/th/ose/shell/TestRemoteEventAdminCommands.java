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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.remote.api.RemoteEventRegistration;
import mil.dod.th.ose.test.remote.RemoteUtils;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventHandler;

/**
 * Test the remote event admin commands.
 * @author callen
 *
 */
public class TestRemoteEventAdminCommands
{
    private RemoteEventAdmin m_RemoteEventAdmin;
    private RemoteEventAdminCommands m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new RemoteEventAdminCommands();
        m_RemoteEventAdmin = mock(RemoteEventAdmin.class);
        
        //set the remote event admin service
        m_SUT.setRemoteEventAdmin(m_RemoteEventAdmin);
    }
    
    /**
     * Verify the ability to get the registrations known for a particular system Id.
     */
    @Test
    public void testGetEventRegistrations()
    {
        //system id to use
        int sysId1 = 1;
        int sysId2 = 2;
        
        //dummy remote event messages
        EventRegistrationRequestData.Builder request1 = EventRegistrationRequestData.newBuilder().addTopic("TOPIC-A");
        // add required fields
        request1 = RemoteUtils.appendRequiredFields(request1, false);
        RemoteEventRegistration reg1 = new RemoteEventRegistration(sysId1, (ServiceRegistration<EventHandler>)null, 
                request1.build());

        EventRegistrationRequestData.Builder request2 = EventRegistrationRequestData.newBuilder().addTopic("TOPIC-B");
        // add required fields
        request2 = RemoteUtils.appendRequiredFields(request2, false);
        RemoteEventRegistration reg2 = new RemoteEventRegistration(sysId2, (ServiceRegistration<EventHandler>)null,
                request2.build());
       
        //map of registrations
        Map<Integer, RemoteEventRegistration> registrations = new HashMap<Integer, RemoteEventRegistration>();
        registrations.put(1, reg1);
        registrations.put(2, reg2);
        
        //behavior
        when(m_RemoteEventAdmin.getRemoteEventRegistrations()).thenReturn(registrations);
        when(m_RemoteEventAdmin.getRemoteEventExpirationHours(1)).thenReturn(167L);
        when(m_RemoteEventAdmin.getRemoteEventExpirationHours(2)).thenReturn(5L);
        
        CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);
        
        m_SUT.eventRegs(testSession);
        
        verify(testStream).printf("Registration ID: %d (Expires in %d-%d hours)\n%s", 1,
                (long)RemoteConstants.REMOTE_EVENT_DEFAULT_REG_TIMEOUT_HOURS - 1,
                (long)RemoteConstants.REMOTE_EVENT_DEFAULT_REG_TIMEOUT_HOURS, reg1);
        verify(testStream).printf("Registration ID: %d (Expires in %d-%d hours)\n%s", 2, 5L, 6L, reg2);
    }
}
