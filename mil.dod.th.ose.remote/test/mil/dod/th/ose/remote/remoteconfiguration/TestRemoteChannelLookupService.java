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
package mil.dod.th.ose.remote.remoteconfiguration;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelRequestData;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelResponseData;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Test the remote channel setup service 
 * @author Pinar French
 * 
 *
 */
public class TestRemoteChannelLookupService
{
    private RemoteChannelLookupMessageService m_SUT;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageRouterInternal m_MessageRouter;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private MessageResponseWrapper m_ResponseWrapper;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new RemoteChannelLookupMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_MessageFactory = mock(MessageFactory.class);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
        
        when(m_MessageFactory.createRemoteChannelLookupResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(RemoteChannelLookupMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
    }

    /**
     * Verify the namespace is RemoteChannel
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.RemoteChannelLookup));
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        m_SUT.activate();
        
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }
    
    /**
     * Verify handling of sync transport layer channel request message
     */
    @Test
    public void testSyncTransportChannelRequestMessage() throws IOException
    {
        // construct a single message containing data within a namespace message
        SyncTransportChannelRequestData transportRequestData = SyncTransportChannelRequestData.newBuilder()
                .setDestSystemAddress("destination").setRemoteSystemAddress("system")
                .setTransportLayerName("transport").setRemoteSystemId(7)
                .build();
        
        RemoteChannelLookupNamespace remoteNamespaceMessage = RemoteChannelLookupNamespace.newBuilder()
                .setType(RemoteChannelLookupMessageType.SyncTransportChannelRequest)
                .setData(transportRequestData.toByteString()).build();
        
        TransportChannel transportChannel = mock(TransportChannel.class);
        when(transportChannel.getTransportLayerName()).thenReturn("transport");
        when(transportChannel.getRemoteMessageAddress()).thenReturn("system");
        when(transportChannel.getLocalMessageAddress()).thenReturn("local");
        when(m_RemoteChannelLookup.syncTransportChannel(
                transportRequestData.getTransportLayerName(),transportRequestData.getDestSystemAddress()
                , transportRequestData.getRemoteSystemAddress(), transportRequestData.getRemoteSystemId()))
                .thenReturn(transportChannel);

        TerraHarvestMessage message = createRemoteChannelMessage(remoteNamespaceMessage);
        TerraHarvestPayload payload = createPayload(remoteNamespaceMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_RemoteChannelLookup).syncTransportChannel("transport", "destination", "system", 7);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(remoteNamespaceMessage.getType().toString()));
        assertThat((RemoteChannelLookupNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(remoteNamespaceMessage));
        assertThat((SyncTransportChannelRequestData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(transportRequestData));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify handling of the response to syncing a transport layer channel.
     */
    @Test
    public void testSyncTransportChannelResponseMessage() throws IOException
    {
        // construct a single message containing data within a namespace message
        SyncTransportChannelResponseData transportRequestData = SyncTransportChannelResponseData.newBuilder()
                .setRemoteSystemAddress("system")
                .setTransportLayerName("transport")
                .setRemoteSystemId(7)
                .setSourceSystemAddress("local")
                .build();
        
        RemoteChannelLookupNamespace remoteNamespaceMessage = RemoteChannelLookupNamespace.newBuilder()
                .setType(RemoteChannelLookupMessageType.SyncTransportChannelResponse)
                .setData(transportRequestData.toByteString()).build();

        TerraHarvestMessage message = createRemoteChannelMessage(remoteNamespaceMessage);
        TerraHarvestPayload payload = createPayload(remoteNamespaceMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(remoteNamespaceMessage.getType().toString()));
        assertThat((RemoteChannelLookupNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(remoteNamespaceMessage));
        assertThat((SyncTransportChannelResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(transportRequestData));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Create remote channel message
     */
    private TerraHarvestMessage createRemoteChannelMessage(RemoteChannelLookupNamespace remoteChannelNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(5, 1, Namespace.RemoteChannelLookup, 100, 
                remoteChannelNamespaceMessage);
    }
    
    /**
     * create the payload for the remote channel message
     */
    private TerraHarvestPayload createPayload(RemoteChannelLookupNamespace remoteChannelNamespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.RemoteChannelLookup).
               setNamespaceMessage(remoteChannelNamespaceMessage.toByteString()).
               build();
    }
}
