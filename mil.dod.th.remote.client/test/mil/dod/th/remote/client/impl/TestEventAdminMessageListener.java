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
package mil.dod.th.remote.client.impl;

import static org.mockito.Mockito.*;

import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.RemoteMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestEventAdminMessageListener
{
    private EventAdminMessageListener m_SUT;

    @Mock private EventAdmin m_EventAdmin;
    @Mock private Logging m_Logging;
    @Mock private MessageListenerService m_MessageListenerService;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new EventAdminMessageListener();
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLogging(m_Logging);
        m_SUT.setMessageListenerService(m_MessageListenerService);
    }

    @Test
    public void testActivateDeactivate()
    {
        // replay
        m_SUT.activate();

        // verify
        verify(m_MessageListenerService).registerCallback(eq(Namespace.EventAdmin),
            Mockito.any(MessageListenerCallback.class));

        // replay
        m_SUT.deactivate();

        // verify
        verify(m_MessageListenerService).unregisterCallback(Mockito.any(MessageListenerCallback.class));
    }

    /**
     * Verify that event messages received are posted to EventAdmin.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testPostEvent()
    {
        m_SUT.activate();

        // mock
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<MessageListenerCallback> callbackCaptor = ArgumentCaptor.forClass(MessageListenerCallback.class);
        verify(m_MessageListenerService).registerCallback(eq(Namespace.EventAdmin), callbackCaptor.capture());

        @SuppressWarnings("rawtypes")
        MessageListenerCallback callback = callbackCaptor.getValue();
        @SuppressWarnings("rawtypes")
        RemoteMessage remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getDataMessageType()).thenReturn(EventAdminMessageType.SendEvent);
        SendEventData sendEventData = SendEventData.newBuilder()
            .setTopic("testEvent1")
            .build();
        when(remoteMessage.getDataMessage()).thenReturn(sendEventData);

        // replay
        callback.handleMessage(remoteMessage);
        
        sendEventData = SendEventData.newBuilder()
                .setTopic("testEvent2")
                .addProperty(ComplexTypesMapEntry.newBuilder()
                    .setKey("testkey")
                    .setProgramStatus(MissionStatus.EXECUTING))
                .build();
        when(remoteMessage.getDataMessage()).thenReturn(sendEventData);

        callback.handleMessage(remoteMessage);

        // verify
        verify(m_EventAdmin, times(2)).postEvent(any(Event.class));
    }
}
