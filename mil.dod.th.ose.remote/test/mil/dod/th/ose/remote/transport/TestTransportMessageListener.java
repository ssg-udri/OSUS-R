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
package mil.dod.th.ose.remote.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.test.EventAdminSyncer;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.ByteString;

/**
 * @author bachmakm
 *
 */
public class TestTransportMessageListener
{
    private TransportMessageListener m_SUT;
    private MessageRouter m_MessageRouter;
    private ServiceRegistration<EventHandler> m_ServiceRegistration;
    private BundleContext  m_Context;
    private TransportChannel m_Channel;
    private EventAdmin m_EventAdmin;
    private RemoteSettings m_RemoteSettings;
    private LoggingService m_Logging;

    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws SecurityException
    {
        //mock necessary objects and OSGi services
        m_SUT = new TransportMessageListener();
        m_MessageRouter = mock(MessageRouter.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_ServiceRegistration = mock(ServiceRegistration.class);
        m_Context = mock(BundleContext.class);
        m_Channel = mock(TransportChannel.class);
        when(m_Channel.getTransportLayerName()).thenReturn("transportBob");
        when(m_Channel.getLocalMessageAddress()).thenReturn("localBill");
        when(m_Channel.getRemoteMessageAddress()).thenReturn("remoteBurt");

        //mock initialization of services
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        // set max message size limit
        when(m_RemoteSettings.getMaxMessageSize()).thenReturn(1024L);
        // set logging enabled
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(true);
       
        //mock for event handler service registration  
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(TransportMessageListener.class), 
                Mockito.any(Dictionary.class))).thenReturn(this.m_ServiceRegistration);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TransportMessageListener.CHANNEL_PROP_KEY, m_Channel);
        
        // add message size limit
        m_SUT.activate(props, m_Context);      
    }

    /**
     * Verify the factory name matches the class name
     */
    @Test
    public void testFactoryName()
    {
        assertThat(TransportMessageListener.FACTORY_NAME, is(m_SUT.getClass().getName()));
    }
    
    /**
     * Method tests that a) configuration properties (messageAddress and transportName) are being properly set
     * and b) the Event Handler is being registered and has the appropriate event topic and filter.  
     * 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivate()
    {

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> propertiesCap = ArgumentCaptor.forClass(Dictionary.class);
        
        //verify that the service is being registered
        verify(m_Context).registerService(eq(EventHandler.class), eq(m_SUT), propertiesCap.capture());
        
        //ensure that the event topic is set
        assertThat((String)propertiesCap.getValue().get("event.topics"), is(TransportLayer.TOPIC_PACKET_RECEIVED));
        
        //ensure that property values are set and are being reflected in event filter
        assertThat((String)propertiesCap.getValue().get("event.filter"), 
                is("(&(obj.name=transportBob)(source.addr.desc=remoteBurt)(dest.addr.desc=localBill))"));     
    }
    
    /**
     * Verify run method exits when deactivate is called.
     */
    @Test(timeout = 1000)
    public void testDeactivate() throws InterruptedException
    {
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // should cause thread to stop
        m_SUT.deactivate();
        
        // wait for thread to stop or just timeout
        thread.join(100);
        
      //verify that event handler service is being unregistered
        verify(this.m_ServiceRegistration).unregister();
        // verify thread has stopped in case of timeout
        assertThat(thread.isAlive(), is(false));
    }
    
    /**
     * Method verifies correct behavior of events in handleEvent method.
     * 
     * Verify new/changed id event is only sent when id changes
     */
    @Test
    public void testHandleEvent() throws IOException
    {  
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        int sourceId = 100;
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(Integer.MAX_VALUE, 1, 
                Namespace.Base, 100, baseMessage);
        
        //mimics message that would be sent through TransportChannel
        Map<String, Object> properties = new HashMap<String, Object>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        //mimics type of event that would be caught by listener
        TransportPacket packet = mock(TransportPacket.class);
        when(packet.getPayload()).thenReturn(ByteBuffer.wrap(output.toByteArray()));
        properties.put(TransportLayer.EVENT_PROP_PACKET, packet);
        Event event = new Event(TransportLayer.TOPIC_PACKET_RECEIVED, properties);
        
        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID);
        
        m_SUT.handleEvent(event);
        
        // event is posted on a separate thread, wait for it
        syncer.waitFor(1, TimeUnit.SECONDS);
        
        // the listener should read in the message and parse as the same message
        verify(m_MessageRouter).handleMessage(message, m_Channel);
        
        // wait for event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        List<Event> postedEvents = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, 
                RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, 1);
        
        assertThat((TransportChannel)postedEvents.get(0).getProperty(RemoteConstants.EVENT_PROP_CHANNEL), 
                is(m_Channel));
        assertThat((Integer)postedEvents.get(0).getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(Integer.MAX_VALUE));
        
        // send out different message (with different source)
        TerraHarvestMessage message2 = TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 
                100, baseMessage);
        
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        message2.writeDelimitedTo(output2);
        when(packet.getPayload()).thenReturn(ByteBuffer.wrap(output2.toByteArray()));
        properties.put(TransportLayer.EVENT_PROP_PACKET, packet);
        Event event2 = new Event(TransportLayer.TOPIC_PACKET_RECEIVED, properties);
        
        // wait for the event again
        syncer = new EventAdminSyncer(m_EventAdmin, RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID);
        
        m_SUT.handleEvent(event2);
        
        // wait again for separate thread
        syncer.waitFor(1, TimeUnit.SECONDS);
        
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvents = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, 
                RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, 2); // should now be a 2nd event
        assertThat((Integer)postedEvents.get(1).getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(sourceId));
        
        //verify logging enabled
        //multiple messages are sent so logging will occur more than once
        verify(m_Logging, atLeast(1)).debug(anyString(), anyVararg());
        
        // wait for the event again, but not expecting a new event posted (0 times)
        syncer = new EventAdminSyncer(m_EventAdmin, RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, 0);
        
        // send same event again, should not see another changed id event
        m_SUT.handleEvent(event);
        
        // wait again for separate thread, event should not be found
        syncer.waitFor(1, TimeUnit.SECONDS);
        
        postedEvents = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, 
                RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, 2);  // should still be 2
    }
    
    /**
     * Test with a message that exceeds the max allowed size.
     * @throws IOException on error writing output to TerraHarvestMessage
     */
    @Test
    public void testHandleEventLargeMsg() throws IOException
    {
        // attempt to send message that is over the max limit
        // setup the message to be received
        byte[] tooBig = new byte[1025]; // a byte array that is just 1 byte too big
        for (int i = 0; i < tooBig.length; i++)
        {
            tooBig[i] = 0x01;
        }
        
        BaseNamespace bigMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                setData(ByteString.copyFrom(tooBig)).
                build();

        final int sourceId = 100;

        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMessage(sourceId, 1, Namespace.Base, 100, bigMessage);        
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        TransportPacket packet = mock(TransportPacket.class);
        when(packet.getPayload()).thenReturn(ByteBuffer.wrap(output.toByteArray()));
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(TransportLayer.EVENT_PROP_PACKET, packet);
        Event event = new Event(TransportLayer.TOPIC_PACKET_RECEIVED, properties);
        
        // wait for the event again
        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, RemoteConstants.TOPIC_REMOVE_CHANNEL);
        
        m_SUT.handleEvent(event);
        
        // wait again for separate thread
        syncer.waitFor(1, TimeUnit.SECONDS);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        List<Event> postedEvents = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, 
                RemoteConstants.TOPIC_REMOVE_CHANNEL, 1);
        assertThat((TransportChannel)postedEvents.get(0).getProperty(RemoteConstants.EVENT_PROP_CHANNEL), 
                    is(m_Channel));
    }
 
    /**
     * Method verifies correct behavior of events in handleEvent method.
     * 
     * Verify new/changed id event is only sent when id changesInterruptedException
     */
    @Test
    public void testHandleEventLoggingDisabled() throws IOException
    {      
        //disable logging
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(false);
        Thread thread = new Thread(m_SUT);
        thread.start();
    
        int sourceId = 100;
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
    
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 
                100, baseMessage);
    
        //mimics message that would be sent through TransportChannel
        Map<String, Object> properties = new HashMap<String, Object>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
    
        //mimics type of event that would be caught by listener
        TransportPacket packet = mock(TransportPacket.class);
        when(packet.getPayload()).thenReturn(ByteBuffer.wrap(output.toByteArray()));
        properties.put(TransportLayer.EVENT_PROP_PACKET, packet);
        Event event = new Event(TransportLayer.TOPIC_PACKET_RECEIVED, properties);
    
        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID);
    
        m_SUT.handleEvent(event);
    
        // event is posted on a separate thread, wait for it
        syncer.waitFor(1, TimeUnit.SECONDS);
    
        // the listener should read in the message and parse as the same message
        verify(m_MessageRouter).handleMessage(message, m_Channel);
    
        // verify logging is disabled
        // debug logging for id should still be posted, no other debug messages should be logged (exactly 1)
        verify(m_Logging, times(1)).debug(anyString(), anyVararg());
    }
}
