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
package mil.dod.th.ose.remote.api;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.ose.test.remote.RemoteUtils;

import org.junit.Test;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventHandler;

/**
 * @author Dave Humeniuk
 *
 */
public class TestRemoteEventRegistration
{
    /**
     * Verify fields can be retrieved based on values passed to constructor.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFields()
    {
        //construct message
        EventRegistrationRequestData.Builder data = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        EventRegistrationRequestData dataApp = RemoteUtils.appendRequiredFields(data, false).build();
        ServiceRegistration<EventHandler> serviceReg = mock(ServiceRegistration.class);
        RemoteEventRegistration reg = new RemoteEventRegistration(100, serviceReg, dataApp);
        
        assertThat(reg.getServiceRegistration(), is(serviceReg));
        assertThat(reg.getSystemId(), is(100));
        assertThat(reg.getEventRegistrationRequestData(), is(dataApp));
    }
    
    /**
     * Verify toString.
     */
    @Test
    public void testToString()
    {
        EventRegistrationRequestData.Builder data = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        EventRegistrationRequestData dataApp = RemoteUtils.appendRequiredFields(data, false).build();
        @SuppressWarnings("unchecked")
        ServiceRegistration<EventHandler> serviceReg = mock(ServiceRegistration.class);
        RemoteEventRegistration reg = new RemoteEventRegistration(100, serviceReg, dataApp);
        
        assertThat(reg.toString(), is(String.format("SystemId: %d%nRemote Registration Data:%n%s%n", 
                100, dataApp)));
    }
}
