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

package mil.dod.th.ose.remote.osgi; 

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.RemoteEventRegistration;
import mil.dod.th.ose.remote.osgi.EventAdminMessageService.EventHandlerImpl;
import mil.dod.th.ose.remote.proto.PersistEventRegistration.PersistentEventRegistrationMessage;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.remote.util.RemotePropertyConverter;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.remote.RemoteUtils;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.Message;

/**
 * Test class for testing functions within the {@link EventAdminMessageService} service.
 * @author bachmakm
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TestEventAdminMessageService
{
    private EventAdminMessageService m_SUT;
    private BundleContext m_Context;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageRouterInternal m_MessageRouter;
    private RemotePropertyConverter m_ConversionService;
    private PersistentDataStore m_DataStore;
    private LoggingService m_Logger;
    private MessageResponseWrapper m_ResponseWrapper;
    private MessageWrapper m_MessageWrapper;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new EventAdminMessageService();
        m_Context = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_ConversionService =  mock(RemotePropertyConverter.class);
        m_DataStore = mock(PersistentDataStore.class);
        m_Logger = LoggingServiceMocker.createMock();
        
        m_SUT.setLoggingService(m_Logger);        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setRemotePropertyConverter(m_ConversionService);
        m_SUT.setPersistentDataStore(m_DataStore);
        
        when(m_MessageFactory.createEventAdminResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(EventAdminMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        verify(m_DataStore).query(EventAdminMessageService.class);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }
    
    /**
     * Verify persisted event registrations are pulled out of the datastore.
     */
    @Test
    public void testActivateDatastorePull()
    {
        //persistent reg messages.
        List<PersistentData> datas = new ArrayList<PersistentData>();
        PersistentEventRegistrationMessage message1 = createPersistedRegMessage();
        PersistentEventRegistrationMessage message2 = createPersistedRegMessage();
        
        //mock persistent data
        PersistentData data1 = mock(PersistentData.class);
        when(data1.getEntity()).thenReturn(message1.toByteArray());
        when(data1.getDescription()).thenReturn("1");
        PersistentData data2 = mock(PersistentData.class);
        when(data2.getEntity()).thenReturn(message2.toByteArray());
        when(data2.getDescription()).thenReturn("2");
        
        //add the mocks to the collection
        datas.add(data1);
        datas.add(data2);
        Collection<PersistentData> perData = datas;
        when(m_DataStore.query(EventAdminMessageService.class)).thenReturn(perData);
        
        //when the message are pulled the events will be re registered for
        ServiceRegistration<EventHandler> handler1 = mock(ServiceRegistration.class);
        ServiceRegistration<EventHandler> handler2 = mock(ServiceRegistration.class);
        
        //service registreation mocks
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(handler1, handler2);
        
        //activate
        m_SUT.activate(m_Context);
        
        verify(m_DataStore, times(2)).query(EventAdminMessageService.class);
        
        m_SUT.deactivate();
        
        verify(handler1).unregister();
        verify(handler2).unregister();
    }
    
    /**
     * Verify persisted event registrations are pulled out of the datastore.
     * Verify that the registration ids are increased and that when the largest id is found while pulling the data
     * that that id is then made the 'last reg ID' so that the next ID is one higher. 
     */
    @Test
    public void testActivateDatastorePullIncreaseRegId() throws Exception
    {
        //persistent reg messages.
        List<PersistentData> datas = new ArrayList<PersistentData>();
        PersistentEventRegistrationMessage message1 = createPersistedRegMessage();
        PersistentEventRegistrationMessage message2 = createPersistedRegMessage();
        
        //mock persistent data
        PersistentData data1 = mock(PersistentData.class);
        when(data1.getEntity()).thenReturn(message1.toByteArray());
        when(data1.getDescription()).thenReturn("5");
        PersistentData data2 = mock(PersistentData.class);
        when(data2.getEntity()).thenReturn(message2.toByteArray());
        when(data2.getDescription()).thenReturn("6");
        
        //add the mocks to the collection
        datas.add(data1);
        datas.add(data2);
        Collection<PersistentData> perData = datas;
        when(m_DataStore.query(EventAdminMessageService.class)).thenReturn(perData);
        
        //when the message are pulled the events will be re registered for
        ServiceRegistration<EventHandler> handler1 = mock(ServiceRegistration.class);
        ServiceRegistration<EventHandler> handler2 = mock(ServiceRegistration.class);
        
        //service registration mocks
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(handler1, handler2);
        
        //activate
        m_SUT.activate(m_Context);
        
        verify(m_DataStore, times(2)).query(EventAdminMessageService.class);
        
        //verify the increase in the registration ids
      //construct message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
               
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((EventRegistrationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(requestMessage.build()));

        ArgumentCaptor<EventRegistrationResponseData> messageCaptor = 
                ArgumentCaptor.forClass(EventRegistrationResponseData.class);
        ArgumentCaptor<Dictionary> properties = ArgumentCaptor.forClass(Dictionary.class);

        // verify that the event is being registered with the proper properties
        verify(m_Context, times(3)).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), properties.capture());
        // verify response message is sent back to source requester (id=0)
        verify(m_MessageFactory).createEventAdminResponseMessage(eq(message), 
                eq(EventAdminMessageType.EventRegistrationResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        EventRegistrationResponseData receivedMessage = messageCaptor.getValue();
        //ensure that registered event has unique ID
        assertThat(receivedMessage.getId(), is(7));
    }

    /**
     * Verify the namespace is EventAdmin
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.EventAdmin));
    }

    /**
     * Verify generic handling of message, that it posts an event locally
     */
    @Test
    public void testGenericHandleMessage() throws Exception
    {
        EventRegistrationResponseData data = EventRegistrationResponseData.newBuilder().setId(1).build();
        
        // construct a single event admin namespace message to verify that it is sent to event handler service
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationResponse)
                .setData(data.toByteString())
                .build();

        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

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
                is(eventAdminMessage.getType().toString()));
        assertThat((EventAdminNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(eventAdminMessage));
        assertThat((EventRegistrationResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(data));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }

    /**
     * Test if remote event is properly sending/receiving event registration 
     * requests and responses. 
     */
    @Test
    public void testRegisterForEvent_Basic() throws Exception, IllegalArgumentException, PersistenceFailedException 
    {
        //construct message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((EventRegistrationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(requestMessage.build()));

        ArgumentCaptor<EventRegistrationResponseData> messageCaptor = 
                ArgumentCaptor.forClass(EventRegistrationResponseData.class);
        ArgumentCaptor<Dictionary> properties = ArgumentCaptor.forClass(Dictionary.class);

        // verify that the event is being registered with the proper properties
        verify(m_Context).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), properties.capture());
        // verify response message is sent back to source requester (id=0)
        verify(m_MessageFactory).createEventAdminResponseMessage(eq(message), 
                eq(EventAdminMessageType.EventRegistrationResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        EventRegistrationResponseData receivedMessage = messageCaptor.getValue();
        //ensure that registered event has unique ID
        assertThat(receivedMessage.getId(), is(1));
        
        ArgumentCaptor<byte[]> thCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_DataStore).persist(
                eq(EventAdminMessageService.class), Mockito.any(UUID.class), anyString(), thCaptor.capture());
        byte[] bytes = thCaptor.getValue();
        PersistentEventRegistrationMessage persistMessage = PersistentEventRegistrationMessage.parseFrom(bytes);
        //verify encryption type is set
        assertThat(persistMessage.getEncryptionType(), is(EncryptType.AES_ECDH_ECDSA));
        //get reg information
        EventRegistrationRequestData persistedData = persistMessage.getRegMessage();
        assertThat(persistedData, is(equalTo(requestMessage.build())));

        // verify service registration properties are being set properly
        assertThat((String[])properties.getValue().get(EventConstants.EVENT_TOPIC), is(new String[]{"test-topic"}));
        assertThat((String)properties.getValue().get(EventConstants.EVENT_FILTER), 
                is(String.format("(!(%s=*))", RemoteConstants.REMOTE_EVENT_PROP)));

        // Create new event registration request - has multiple topics
        requestMessage = EventRegistrationRequestData.newBuilder().addTopic("test-topic")
                .addTopic("other-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        payload = createPayload(eventAdminMessage);
        message = createEventAdminMessage(eventAdminMessage);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify that the event is being registered with the proper properties
        verify(m_Context, times(2)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class),
                properties.capture());
        // verify response message is sent back to source requester (id=0)
        verify(m_MessageFactory).createEventAdminResponseMessage(eq(message), 
                eq(EventAdminMessageType.EventRegistrationResponse), messageCaptor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);

        // verify content of message
        receivedMessage = messageCaptor.getValue();
        //ensure new registered event has unique ID
        assertThat(receivedMessage.getId(), is(2));

        // verify service registration properties are being set properly
        assertThat((String[])properties.getValue().get(EventConstants.EVENT_TOPIC), 
                is(new String[]{"test-topic", "other-topic"}));
        assertThat((String)properties.getValue().get(EventConstants.EVENT_FILTER), 
                is(String.format("(!(%s=*))", RemoteConstants.REMOTE_EVENT_PROP)));


        // replay - construct new event registration message with set filter 
        requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("new-topic")
                .setFilter("(x=y)");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        payload = createPayload(eventAdminMessage);
        message = createEventAdminMessage(eventAdminMessage);
        //handle new message
        m_SUT.handleMessage(message, payload, channel);

        // verify that the event is being registered with the proper properties
        verify(m_Context, times(3)).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), properties.capture());
        // verify response message is sent back to source requester (id=0)
        verify(m_MessageFactory).createEventAdminResponseMessage(eq(message), 
                eq(EventAdminMessageType.EventRegistrationResponse), messageCaptor.capture());
        verify(m_ResponseWrapper, times(3)).queue(channel);

        // verify content of message
        receivedMessage = messageCaptor.getValue();
        //ensure that new registered event has unique ID
        assertThat(receivedMessage.getId(), is(3));

        // verify service registration properties are being set properly
        assertThat((String[])properties.getValue().get(EventConstants.EVENT_TOPIC), is(new String[]{"new-topic"}));
        assertThat((String)properties.getValue().get(EventConstants.EVENT_FILTER), 
                is(String.format("(&(!(%s=*))(x=y))", RemoteConstants.REMOTE_EVENT_PROP)));
    }
    
    /**
     * Verify parameters are passed to event handler properly.
     */
    @Test
    public void testRegisterForEvent_EventHandlerParams() throws Exception 
    {
        //construct message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic")
                .setObjectFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY);
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<EventHandler> eventHandler = ArgumentCaptor.forClass(EventHandler.class);

        // verify that the event is being registered with the proper event handler
        verify(m_Context).registerService(eq(EventHandler.class), eventHandler.capture(), (Dictionary)any());
        
        Map<String, Object> props = new HashMap<>();
        Event event = new Event("some-topic", props);
        eventHandler.getValue().handleEvent(event);
        
        verify(m_ConversionService, timeout(1000)).mapToComplexTypesMap(props, 
                RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY);
        verify(m_MessageFactory, timeout(1000)).createEventAdminMessage(eq(EventAdminMessageType.SendEvent), 
                (Message)any());
        verify(m_MessageWrapper, timeout(1000)).trySend(200, EncryptType.AES_ECDH_ECDSA);
    }
    
    /**
     * Test if remote event is unable to be persisted that an Exception message is sent.
     * 
     */
    @Test
    public void testRegisterForEvent_PersistFail() throws Exception
    {
        //mock datastore exception
        when(m_DataStore.persist(eq(EventAdminMessageService.class), Mockito.any(UUID.class), anyString(), 
                Mockito.any(Serializable.class))).thenThrow(new PersistenceFailedException("failed"));
        //construct message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
                
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify exception
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.PERSIST_ERROR, 
                "Failed persisting the remote registration, the registration will not be known if the system restarts." 
                + "failed");
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test if remote event is being properly unregistered via sending/receiving unregister event
     * requests and responses.  
     */
    @Test
    public void testUnregister() throws Exception
    {
        // mock
        ServiceRegistration reg = mock(ServiceRegistration.class);

        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(reg);

        // Register Event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // create unregister event request - unique id of registered event (above) should be 1
        UnregisterEventRequestData unregisterRequestMessage = UnregisterEventRequestData.newBuilder()
                .setId(1).build();
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.UnregisterEventRequest)
                .setData(unregisterRequestMessage.toByteString())
                .build();
        payload = createPayload(eventAdminMessage);
        message = createEventAdminMessage(eventAdminMessage);
        m_SUT.handleMessage(message, payload, channel);
        // verify that the registered service has been unregistered
        verify(reg).unregister();
        verify(m_DataStore).removeMatching(EventAdminMessageService.class, "1");
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((UnregisterEventRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(unregisterRequestMessage));
        
        verify(m_MessageFactory).createEventAdminResponseMessage(message, 
                EventAdminMessageType.UnregisterEventResponse, null);
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Verify the unregister response message sends an event with null data message.
     */
    @Test
    public void testUnregisterResponse() throws Exception
    {
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder().
                setType(EventAdminMessageType.UnregisterEventResponse).
                build();
        
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(nullValue()));
    }

    /**
     * Test if registered remote events are properly unregistered via cleanup requests/responses.
     * 
     * Verify that only registrations from the request of the cleanup are unregistered.
     * 
     * Verify that unregister won't be called again on subsequent calls.
     */
    @Test
    public final void testCleanupPostChannels() throws Exception
    {
        // mock service registrations for source 1 and 2
        ServiceRegistration reg1 = mock(ServiceRegistration.class);
        ServiceRegistration reg2 = mock(ServiceRegistration.class);

        when(m_Context.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(reg1, reg2);

        // Register Event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);       
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(1, eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // Register another event
        requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
                
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        message = createEventAdminMessage(2, eventAdminMessage);
        m_SUT.handleMessage(message, payload, channel);

        // Create cleanup request from source 1
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.CleanupRequest)
                .build();
        payload = createPayload(eventAdminMessage);
        message = createEventAdminMessage(1, eventAdminMessage);
        m_SUT.handleMessage(message, payload, channel);

        //verify that the registration from source 1 is unregistered, but not the one from source 2
        verify(reg1, times(1)).unregister();
        verify(reg2, never()).unregister();
        
        verify(m_DataStore, times(1)).removeMatching(eq(EventAdminMessageService.class), anyString());
        
        verify(m_MessageFactory).createEventAdminResponseMessage(message, 
                EventAdminMessageType.CleanupResponse, null);
        //reused channel
        verify(m_ResponseWrapper, times(3)).queue(channel);
        
        // send message again, verify unregister not called again
        m_SUT.handleMessage(message, payload, channel);
        // should still be at the same number of calls
        verify(reg1, times(1)).unregister();
        verify(reg2, never()).unregister();
        
        verify(m_DataStore, times(1)).removeMatching(eq(EventAdminMessageService.class), anyString());
    }
    
    /**
     * Verify the cleanup response message sends an event with null data message.
     */
    @Test
    public void testCleanupPostResponse() throws Exception
    {
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder().
                setType(EventAdminMessageType.CleanupResponse).
                build();
        
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(nullValue()));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(EventAdminMessageType.CleanupResponse.toString()));
    }

    /**
     * Test if registered remote events are properly posted locally when a sendEvent
     * message is received.
     */
    @Test
    public void testSendEvent() throws Exception
    {
        //Send event message
        SendEventData requestMessage = SendEventData.newBuilder()
                .setTopic("new-topic").build();

        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.SendEvent)
                .setData(requestMessage.toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        //mock converted props
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "test");
        props.put("prop2", true);
        when(m_ConversionService.complexTypesMapToMap(Mockito.anyList())).thenReturn(props);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());//once in send event and again in handle message

        /* The sendEvent message is posted locally before the handle message method posts 
         * the general event info locally. Therefore, the first values captured contain the 
         * sendEvent message contents and are used to verify correct outputs.     
         */
        assertThat(eventCaptor.getAllValues().get(0).getTopic(), 
            is(RemoteInterfaceUtilities.getRemoteEventTopic("new-topic")));
        assertThat((String)eventCaptor.getAllValues().get(0).getProperty("prop1"), is("test"));
        assertThat((Boolean)eventCaptor.getAllValues().get(0).getProperty("prop2"), is(true));
        assertThat((Integer)eventCaptor.getAllValues().get(0).getProperty("controller.id"), is(200));
        
        // now verify the second event, the handle message event contains the data message
        Event event = eventCaptor.getValue();
        assertThat((SendEventData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(requestMessage));
    }

    /**
     * Test event handling logic for locally posted events.
     */
    @Test
    public void testEventHandlerImpl() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        Event event = new Event("FAKE_EVENT", properties);

        EventRegistrationRequestData requestData = EventRegistrationRequestData.newBuilder()
                .setCanQueueEvent(false)
                .setExpirationTimeHours(-1)
                .setObjectFormat(RemoteTypesGen.LexiconFormat.Enum.XML)
                .setFilter("(something.id=Gold)")
                .addTopic("Silver")
                .build();        
        
        //conversion behavior
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        ComplexTypesMapEntry entry = ComplexTypesMapEntry.newBuilder().
            setKey("fakeKey").
            setMulti(Multitype.newBuilder().
                setType(Type.STRING).
                setStringValue("fakeValue").build()).build();
        props.add(entry);
        when(m_ConversionService. mapToComplexTypesMap(anyMap(), 
                (RemoteTypesGen.LexiconFormat.Enum)any())).thenReturn(props);

        m_SUT.new EventHandlerImpl(200, EncryptType.AES_ECDH_ECDSA, 
                requestData).handleEvent(event);
        
        // verify format is passed properly
        verify(m_ConversionService, timeout(1000)).mapToComplexTypesMap(anyMap(), 
                eq(RemoteTypesGen.LexiconFormat.Enum.XML));

        ArgumentCaptor<SendEventData> messageCaptor = ArgumentCaptor.forClass(SendEventData.class);
        verify(m_MessageFactory, timeout(1000)).createEventAdminMessage(eq(EventAdminMessageType.SendEvent), 
                messageCaptor.capture());
        verify(m_MessageWrapper, timeout(1000)).trySend(200, EncryptType.AES_ECDH_ECDSA);

        // verify content of message
        SendEventData eventMessage = messageCaptor.getValue();
        assertThat(eventMessage.getTopic(), is("FAKE_EVENT"));

        ComplexTypesMapEntry entry1 = eventMessage.getProperty(0);
        assertThat(entry1.getKey(), is("fakeKey"));
        assertThat(entry1.getMulti().getStringValue(), is("fakeValue"));
        ComplexTypesMapEntry entry2 = eventMessage.getProperty(1);
        assertThat(entry2.getKey(), is(RemoteConstants.REMOTE_EVENT_PROP));
        assertThat(entry2.getMulti().getBoolValue(), is(true));
    }
    
    /**
     * Test that if an exception is experienced when trying to create a remote event that the event is not sent.
     */
    @Test
    public void testEventHandlerImpl_ConversionException() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        Event event = new Event("FAKE_EVENT", properties);
        EventRegistrationRequestData request = EventRegistrationRequestData.newBuilder()
                .setCanQueueEvent(true)
                .setExpirationTimeHours(-1)
                .setObjectFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY)
                .setFilter("(something.id=Red)")
                .addTopic("Blue")
                .build();
        //conversion behavior
        when(m_ConversionService.mapToComplexTypesMap(anyMap(), (RemoteTypesGen.LexiconFormat.Enum)any())).
            thenThrow(new ObjectConverterException("Exception"));

        m_SUT.new EventHandlerImpl(200, EncryptType.NONE, request).handleEvent(event);

        // wait to give thread time to start and try to read message
        Thread.sleep(500);

        ArgumentCaptor<SendEventData> messageCaptor = ArgumentCaptor.forClass(SendEventData.class);
        verify(m_MessageFactory, never()).createEventAdminMessage(eq(EventAdminMessageType.SendEvent), 
                messageCaptor.capture());
        verify(m_MessageWrapper, never()).trySend(200, EncryptType.NONE);
    }

    /**
     * Test if a channel to the remote system is not available that an error is thrown only three times.
     * Verify reset if successful.
     */
    @Test
    public void testEventHandlerImpl_IllegalArgExceptionReset() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        Event event = new Event("EVENT", properties);
        
        EventRegistrationRequestData requestData = EventRegistrationRequestData.newBuilder()
                .setCanQueueEvent(false)
                .setExpirationTimeHours(-1)
                .setObjectFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY)
                .setFilter("(something.id=Ruby)")
                .addTopic("Sapphire")
                .build();   
        
        //conversion behavior
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        when(m_ConversionService. mapToComplexTypesMap(anyMap(), 
                (RemoteTypesGen.LexiconFormat.Enum)any())).thenReturn(props);

        //exception behavior
        doThrow(new IllegalArgumentException("Exception!")).when(m_MessageWrapper).trySend(anyInt(), 
                Mockito.any(EncryptType.class));
        
        EventHandlerImpl handler = m_SUT.new EventHandlerImpl(200, EncryptType.AES_ECDH_ECDSA, requestData);
        //handle event 4 times, the fourth time should not throw exception
        int tries = 0;
        while (tries < 5)
        {
            handler.handleEvent(event);
            Thread.sleep(300);
            tries++;
            
            //verify
            assertThat("Failed attempts matches attempts.", handler.getFailedAttemptCount(), is(tries));
        }
        //verify exception logging only three times
        verify(m_Logger, times(3)).warning(anyString(), anyObject());
        
        //mock success
        doReturn(true).when(m_MessageWrapper).trySend(anyInt(), eq(EncryptType.AES_ECDH_ECDSA));
        
        //handle even again
        handler.handleEvent(event);
        Thread.sleep(300);
        
        //verify
        assertThat("Failed attempts does NOT match attempts.", handler.getFailedAttemptCount(), is(0));
    }
    
    /**
     * Test if registered remote events are properly unregistered when the service is deactivated.
     */
    @Test
    public final void testDeactivation() throws Exception
    {
        // mock
        ServiceRegistration reg = mock(ServiceRegistration.class);

        when(m_Context.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(reg);

        // Register Event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("ANOTHER-TOPIC");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);        
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // Register another event
        requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("G,g,g,g,g U-N-I-T");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
                
        eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        message = createEventAdminMessage(eventAdminMessage);
        m_SUT.handleMessage(message, payload, channel);
        
        m_SUT.deactivate();

        //verify that both registered services have been unregistered
        verify(reg, times(2)).unregister();
    }
    
    /**
     * Verify the ability to get the registrations known for a particular system Id.
     */
    @Test
    public void testGetEventRegistrations() throws UnmarshalException, IOException, ObjectConverterException
    {
        int systemId = 200;
        
        //construct message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        EventAdminNamespace eventAdminMessage = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage.build().toByteString())
                .build();
        
        TerraHarvestMessage message = createEventAdminMessage(eventAdminMessage);
        TerraHarvestPayload payload = createPayload(eventAdminMessage);

      //construct message
        EventRegistrationRequestData.Builder requestMessage2 = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic2");
        // add required fields
        requestMessage2 = RemoteUtils.appendRequiredFields(requestMessage2, true);
        EventAdminNamespace eventAdminMessage2 = EventAdminNamespace.newBuilder()
                .setType(EventAdminMessageType.EventRegistrationRequest)
                .setData(requestMessage2.build().toByteString())
                .build();
        
        TerraHarvestMessage message2 = createEventAdminMessage(eventAdminMessage2);
        TerraHarvestPayload payload2 = createPayload(eventAdminMessage2);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //mocks
        ServiceRegistration<EventHandler> handler1 = mock(ServiceRegistration.class);
        ServiceRegistration<EventHandler> handler2 = mock(ServiceRegistration.class);
        
        //service registration mocks
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(handler1, handler2);
        
        //handle messages
        m_SUT.handleMessage(message, payload, channel);
        m_SUT.handleMessage(message2, payload2, channel);

        Map<Integer, RemoteEventRegistration> regsMap = m_SUT.getRemoteEventRegistrations();
        assertThat(regsMap.size(), is(2));
        for (RemoteEventRegistration reg: regsMap.values())
        {
            assertThat(reg.getSystemId(), is(systemId));
            if(reg.getServiceRegistration().equals(handler1))
            {
                assertThat(reg.getEventRegistrationRequestData(), is(requestMessage.build()));
            }
            else
            {
                assertThat(reg.getEventRegistrationRequestData(), is(requestMessage2.build()));
            }
        }
    }

    /**
     * Verify adding remote event not using the message service directly.
     * Verify they are not persisted.
     */
    @Test
    public void testAddEventRegRemoteEventAdmin()
    {
        ServiceRegistration<EventHandler> handler1 = mock(ServiceRegistration.class);
        
        //service registration mocks
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(handler1);
        
        EventRegistrationRequestData request = EventRegistrationRequestData.newBuilder()
                .setCanQueueEvent(true)
                .setExpirationTimeHours(-1)
                .setFilter("(something.id=bob)")
                .addTopic("BURGERS")
                .build();
        
        m_SUT.addRemoteEventRegistration(1, request);
        
        verify(m_DataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), anyString(), anyByte());

        ArgumentCaptor<Dictionary> dictionaryCap = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Context, times(1)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                dictionaryCap.capture());
        
        //verify dictionary catpured
        Dictionary<String, Object> dicOfProps = dictionaryCap.getValue();
        assertThat((String[])dicOfProps.get(EventConstants.EVENT_TOPIC), is(new String[]{"BURGERS"}));
        assertThat((String)dicOfProps.get(EventConstants.EVENT_FILTER), is("(&(!(remote=*))(something.id=bob))"));
        
        m_SUT.deactivate();
        
        verify(handler1).unregister();
    }
    
    TerraHarvestMessage createEventAdminMessage(EventAdminNamespace eventAdminNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(200, 1, Namespace.EventAdmin, 100, 
                eventAdminNamespaceMessage).toBuilder().setEncryptType(EncryptType.AES_ECDH_ECDSA).build();
    }
    
    /**
     * Create a valid terra harvest message with the event admin namespace.
     */
    TerraHarvestMessage createEventAdminMessage(int sourceId, EventAdminNamespace eventAdminNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.EventAdmin, 100, 
                eventAdminNamespaceMessage);
    }
    private TerraHarvestPayload createPayload(EventAdminNamespace eventAdminNamespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.EventAdmin).
               setNamespaceMessage(eventAdminNamespaceMessage.toByteString()).
               build();
    }
    
    /**
     * Create a persisted event registration message.
     */
    private PersistentEventRegistrationMessage createPersistedRegMessage()
    {
        EventRegistrationRequestData.Builder data = EventRegistrationRequestData.newBuilder().
                addTopic("Persisted Topic").
                setFilter("Persisted Filter");
        // add required fields
        data = RemoteUtils.appendRequiredFields(data, false);
                
        return PersistentEventRegistrationMessage.newBuilder().
                setRegMessage(data).
                setSystemId(0).
                setEncryptionType(EncryptType.AES_ECDH_ECDSA).build();
    }
    @Test
    public void testQueueingMessage() throws MarshalException, ObjectConverterException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        Event event = new Event("Jhoto", properties);
        
        EventRegistrationRequestData requestData = EventRegistrationRequestData.newBuilder()
                .setCanQueueEvent(true)
                .setExpirationTimeHours(-1)
                .setObjectFormat(RemoteTypesGen.LexiconFormat.Enum.XML)
                .setFilter("(something.id=Pearl)")
                .addTopic("Diamond")
                .build();   
        
        //Conversion behavior
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        ComplexTypesMapEntry entry = ComplexTypesMapEntry.newBuilder().
            setKey("fakeKey").
            setMulti(Multitype.newBuilder().
                setType(Type.STRING).
                setStringValue("fakeValue").build()).build();
        props.add(entry);
        when(m_ConversionService. mapToComplexTypesMap(anyMap(), 
                (RemoteTypesGen.LexiconFormat.Enum)any())).thenReturn(props);
        
        EventHandlerImpl handler = m_SUT.new EventHandlerImpl(200, EncryptType.AES_ECDH_ECDSA, 
                requestData);
        handler.handleEvent(event);
        
        // verify format is passed properly
        verify(m_ConversionService, timeout(1000)).mapToComplexTypesMap(anyMap(), 
                eq(RemoteTypesGen.LexiconFormat.Enum.XML));

        ArgumentCaptor<SendEventData> messageCaptor = ArgumentCaptor.forClass(SendEventData.class);
        verify(m_MessageFactory, timeout(1000)).createEventAdminMessage(eq(EventAdminMessageType.SendEvent), 
                messageCaptor.capture());
        verify(m_MessageWrapper, timeout(1000)).queue(200, EncryptType.AES_ECDH_ECDSA,null);

        // verify content of message
        SendEventData eventMessage = messageCaptor.getValue();
        assertThat(eventMessage.getTopic(), is("Jhoto"));

        ComplexTypesMapEntry entry1 = eventMessage.getProperty(0);
        assertThat(entry1.getKey(), is("fakeKey"));
        assertThat(entry1.getMulti().getStringValue(), is("fakeValue"));
        ComplexTypesMapEntry entry2 = eventMessage.getProperty(1);
        assertThat(entry2.getKey(), is(RemoteConstants.REMOTE_EVENT_PROP));
        assertThat(entry2.getMulti().getBoolValue(), is(true));
    }
}
