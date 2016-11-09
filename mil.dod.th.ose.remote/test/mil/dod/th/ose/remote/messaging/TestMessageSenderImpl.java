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
package mil.dod.th.ose.remote.messaging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage.Builder;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.EventChannel;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.encryption.EncryptMessageService;
import mil.dod.th.ose.remote.messaging.MessageSenderImpl.ResponseEventHandler;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen.MissionProgramTemplate;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Test class for the {@link MessageSenderImpl}.
 * @author allenchl
 *
 */
public class TestMessageSenderImpl
{
    private MessageSenderImpl m_SUT;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private TerraHarvestSystem m_TerraHarvestSystem;
    private RemoteChannel m_Channel;
    private int m_RemoteId;
    private int m_LocalId;
    private ResponseHandler m_Handler;
    private ServiceRegistration<EventHandler> m_HandlerReg;
    private ResponseEventHandler m_ResponseHandler;
    private EncryptMessageService m_EncryptService;
    private LoggingService m_Logging;
    private RemoteSystemEncryption m_RemoteSystemEncryption;
    private RemoteSettings m_RemoteSettings;
    private EventAdmin m_EventAdmin;
    private EventChannel m_EventChannel;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new MessageSenderImpl();
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
        m_TerraHarvestSystem = mock(TerraHarvestSystem.class);
        m_SUT.setTerraHarvestSystem(m_TerraHarvestSystem);
        m_EncryptService = mock(EncryptMessageService.class);
        m_SUT.setEncryptMessageService(m_EncryptService);
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        m_RemoteSystemEncryption = mock(RemoteSystemEncryption.class);
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        m_SUT.setRemoteSystemEncryption(m_RemoteSystemEncryption);
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_EventChannel = mock(EventChannel.class);
        
        m_LocalId = 102;
        m_RemoteId = 282;
        
        // mock terra harvest system to use the local id
        when(m_TerraHarvestSystem.getId()).thenReturn(m_LocalId);
        
        m_Channel = mock(RemoteChannel.class);
        List<RemoteChannel> channels = new ArrayList<RemoteChannel>();
        channels.add(m_Channel);
        when(m_RemoteChannelLookup.getChannels(m_RemoteId)).thenReturn(channels);
        when(m_RemoteChannelLookup.getChannel(m_RemoteId)).thenReturn(m_Channel);
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
    }
    
    /**
     * Verify that the send base message method will pass the correct message based on the destination id.
     */
    @Test
    public void testSendBaseMessage() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        BaseNamespace baseNamespace = BaseNamespace.newBuilder().
                setType(BaseMessageType.ControllerInfo).
                setData(systemInfoData.toByteString()).build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, baseNamespace);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        boolean sent = m_SUT.trySendMessage(m_RemoteId, payload, EncryptType.NONE);
        assertThat("message reported as sent", sent, is(true));
        
        // verify message is sent to the associated channel with proper fields set
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(m_Channel).trySendMessage(messageCaptor.capture());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
                getTerraHarvestPayload());
        assertThat(messageCaptor.getValue().getSourceId(), is(m_LocalId));
        assertThat(messageCaptor.getValue().getDestId(), is(m_RemoteId));
        assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
        
        // verify inner namespace message
        BaseNamespace baseMessageResponse = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(baseMessageResponse.getType(), is(BaseMessageType.ControllerInfo));
        assertThat(baseMessageResponse.getData(), is(systemInfoData.toByteString()));
    }
    
    /**
     * Verify that when an invalid destination id is passed, the send fails.
     */
    @Test
    public void testSendBaseMessageException() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        BaseNamespace baseNamespace = BaseNamespace.newBuilder().
                setType(BaseMessageType.ControllerInfo).
                setData(systemInfoData.toByteString()).build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, 0xdeadbeef, 0 , Namespace.Base, baseNamespace);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
                
        // try sending to bad id, should throw exception
        try
        {
            m_SUT.trySendMessage(0xdeadbeef, payload, EncryptType.NONE);
            fail("Expecting exception, invalid destination id");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event sendEvent = eventCaptor.getValue();
        assertThat(sendEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST));
        assertThat(sendEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(0xdeadbeef));
        assertThat(sendEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(m_LocalId));
        assertThat(sendEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(request));
    }
    
    /**
     * Verifies that the queue remote base message method will pass the correct message.  
     *  
     * Verifies message is sent to channel and queued if could not be successfully sent.
     */
    @Test
    public void testQueueBaseMessage() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, systemInfoData);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        boolean result = m_SUT.queueMessage(m_RemoteId, payload, request.getEncryptType(), null);
        assertThat("message is queued/sent as reported by channel", result, is(true));
        
        // verify message is sent to the associated channel with correct namespace
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(m_Channel).trySendMessage(messageCaptor.capture());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
                getTerraHarvestPayload());
        assertThat(payLoadTest.getNamespace(), is(Namespace.Base));

        // verify not queued as message was sent out
        verify(m_Channel, never()).queueMessage(Mockito.any(TerraHarvestMessage.class));
        
        // make channel return false now to see that message will be queued, but report queuing as successful
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);

        BaseNamespace baseNamespace = BaseNamespace.newBuilder().
                setType(BaseMessageType.ControllerInfo).
                setData(systemInfoData.toByteString()).build();
        
        request = createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, baseNamespace);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        // replay
        result = m_SUT.queueMessage(m_RemoteId, payload, request.getEncryptType(), null);
        assertThat("message is queued/sent as reported by channel", result, is(true));
        
        // verify queued since message could not be sent out
        verify(m_Channel).queueMessage(messageCaptor.capture());
        payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
                getTerraHarvestPayload());
        assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
        // verify inner namespace message
        BaseNamespace baseMessageResponse = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(baseMessageResponse.getType(), is(BaseMessageType.ControllerInfo));
        assertThat(baseMessageResponse.getData(), is(systemInfoData.toByteString()));
        
        // make channel return false now to see that message will be reported as not queue
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);

        // send message again with no data
        result = m_SUT.queueMessage(m_RemoteId, payload, request.getEncryptType(), null);
        assertThat("message is not queued as reported by channel", result, is(false));
    }
    
    /**
     * Verifies that the queue remote message response method will pass the correct message based on the request.
     *  
     * Verify the channel used is the one requested and not from the lookup.
     * 
     * Verify the message is passed to the channel queue.
     */
    @Test
    public void testQueueResponse() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;
        
        // construct fake request message that should be used by service to construct the response
        MissionProgrammingNamespace requestMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_RemoteId, m_LocalId, 0 , Namespace.MissionProgramming, requestMessage);
        
        // construct data message
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MissionProgrammingNamespace responseMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesResponse)
                .setData(getTemplateData.toByteString()).build();
        
        TerraHarvestMessage response = 
                createTerraHarvestMessage(messageId, Namespace.MissionProgramming, responseMessage, true);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        
        boolean result = m_SUT.queueMessageResponse(request, payload, m_Channel);
        assertThat("message is queued as reported by channel", result, is(true));
        
        // verify message is sent to the associated channel with proper fields set
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(m_Channel).queueMessage(messageCaptor.capture());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
                getTerraHarvestPayload());
        assertThat(messageCaptor.getValue().getSourceId(), is(m_LocalId));
        assertThat(messageCaptor.getValue().getDestId(), is(m_RemoteId));
        assertThat(messageCaptor.getValue().getMessageId(), is(messageId));
        assertThat(messageCaptor.getValue().getIsResponse(), is(true));
        assertThat(payLoadTest.getNamespace(), is(Namespace.MissionProgramming));
        assertThat(messageCaptor.getValue().getMessageId(), is(messageId));
         
        // verify inner namespace message
        MissionProgrammingNamespace messageResponse = 
                MissionProgrammingNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(messageResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesResponse));
        assertThat(messageResponse.getData(), is(getTemplateData.toByteString()));
    }
    
    /**
     * Verify that messages are submitted to the encryption service.
     */
    @Test
    public void testEncryptionServiceSubmitting() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;  
        //first build a TerraHarvestPayload message
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(ByteString.EMPTY).
            build();
        // create the original request message that the response will be based on
        // required to be set Don't care what it is though
        TerraHarvestMessage request =  TerraHarvestMessageUtil.getPartialMessage()
                .setTerraHarvestPayload(payload.toByteString())
                .setSourceId(m_RemoteId)
                .setDestId(m_LocalId)
                .setMessageId(messageId).build();
        
        GenericErrorResponseData errorData = GenericErrorResponseData.newBuilder().
                setError(ErrorCode.ILLEGAL_STATE).
                setErrorDescription("Illegal State errror").build();
        BaseNamespace baseResponseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(errorData.toByteString()).build();
        TerraHarvestMessage response = 
                createTerraHarvestMessage(100 , Namespace.Base, baseResponseMessage, true);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        
        m_SUT.queueMessageResponse(request, TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload()), 
            m_Channel);
        
        // messages submitted to the encryption service
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(m_Channel).queueMessage(messageCaptor.capture());
        ArgumentCaptor<TerraHarvestPayload> payloadCaptor = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService).encryptMessage(builderCaptor.capture(), payloadCaptor.capture());
        
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
             getTerraHarvestPayload());
        TerraHarvestPayload payloadFromCaptor = payloadCaptor.getValue();
        assertThat(payloadFromCaptor, is(payLoadTest));

        //verify that fields are correct in the builder message before the built th message is returned
        assertThat(builderCaptor.getValue().getSourceId(), is(messageCaptor.getValue().getSourceId()));
        assertThat(messageCaptor.getValue().getSourceId(), is(m_LocalId));
        assertThat(builderCaptor.getValue().getDestId(), is(messageCaptor.getValue().getDestId()));
        assertThat(messageCaptor.getValue().getDestId(), is(m_RemoteId));
        assertThat(builderCaptor.getValue().getMessageId(), is(messageCaptor.getValue().getMessageId()));
        assertThat(messageCaptor.getValue().getMessageId(), is(messageId));
    }
    
    /**
     * Verify that when a message is a response that the source information from the request is correctly transferred
     * to be the destination.
     */
    @Test
    public void testResponseSystemIds() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;  
        //first build a TerraHarvestPayload message
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(ByteString.EMPTY).
            build();
        // create the original request message that the response will be based on
        // required to be set Don't care what it is though
        TerraHarvestMessage request =  TerraHarvestMessageUtil.getPartialMessage()
                .setTerraHarvestPayload(payload.toByteString())
                .setSourceId(m_RemoteId)
                .setDestId(m_LocalId)
                .setMessageId(messageId).build();
        
        GenericErrorResponseData errorData = GenericErrorResponseData.newBuilder().
                setError(ErrorCode.ILLEGAL_STATE).
                setErrorDescription("Illegal State errror").build();
        BaseNamespace baseResponseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(errorData.toByteString()).build();
        TerraHarvestMessage response = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 100 , Namespace.Base, baseResponseMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        
        m_SUT.queueMessageResponse(request, TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload()), 
            m_Channel);
        
        // messages submitted to the encryption service
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(m_Channel).queueMessage(messageCaptor.capture());
        ArgumentCaptor<TerraHarvestPayload> payloadCaptor = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService).encryptMessage(builderCaptor.capture(), payloadCaptor.capture());
        
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(messageCaptor.getValue().
             getTerraHarvestPayload());
        TerraHarvestPayload payloadFromCaptor = payloadCaptor.getValue();
        assertThat(payloadFromCaptor, is(payLoadTest));

        //verify that fields are correct in the builder message before the built th message is returned
        //The source id should now be what was the dest id since this is a response message and vice a versa for
        //dest id, it should now be the source
        assertThat(builderCaptor.getValue().getSourceId(), is(messageCaptor.getValue().getSourceId()));
        assertThat(messageCaptor.getValue().getSourceId(), is(m_LocalId));
        assertThat(builderCaptor.getValue().getDestId(), is(messageCaptor.getValue().getDestId()));
        assertThat(messageCaptor.getValue().getDestId(), is(m_RemoteId));
        assertThat(builderCaptor.getValue().getMessageId(), is(messageCaptor.getValue().getMessageId()));
        assertThat(messageCaptor.getValue().getMessageId(), is(messageId));
    }
    
    /**
     * Setup the service registration and activate the component.
     * Verify that the service is registered.
     */
    @SuppressWarnings("unchecked")
    private void activateSetUp()
    {
        //mocks for registration
        BundleContext context = mock(BundleContext.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        
        //activate component
        when(context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        m_SUT.activate(context);
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> captorDict = ArgumentCaptor.forClass(Dictionary.class);
        verify(context).registerService(eq(EventHandler.class), captor.capture(), 
            captorDict.capture());
        
        assertThat((String)captorDict.getValue().get(EventConstants.EVENT_FILTER), is("(message.response=true)"));
        
        //enter mocks for the inner classes
        m_ResponseHandler = (ResponseEventHandler) captor.getAllValues().get(0);
        m_Handler = mock(ResponseHandler.class);
    }
    
    /**
     * Verify that the event handler will call the response handler {@link ResponseHandler#handleResponse} method.
     * Verify activation registers the event handler.
     * Verify that deactivation unregisters the listener.
     */
    @Test
    public void testQueueMessageWithHandler() throws IOException
    {
        //setup and activate component
        activateSetUp();

        // construct data message to send, should reg handler
        ActivateRequestData messageData = ActivateRequestData.newBuilder().
                setUuid(UUID.newBuilder().setLeastSignificantBits(0L).setMostSignificantBits(1L).build()).build();
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(messageData.toByteString()).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0, Namespace.Asset, nameMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        m_SUT.queueMessage(100, TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload()), EncryptType.NONE, 
            m_Handler);
        
        //message as response
        nameMessage = AssetNamespace.newBuilder().
                 setType(AssetMessageType.ActivateResponse).build();
        request = createTerraHarvestMessage(0, Namespace.Asset, nameMessage, true);
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
                
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, request);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());

        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);

        m_ResponseHandler.handleEvent(event);
        verify(m_Handler).handleResponse(request,payLoadTest, nameMessage, null);
        
        //post the event again and verify the handler was not called again
        m_ResponseHandler.handleEvent(event);
        verify(m_Handler, only()).handleResponse(request, payLoadTest,nameMessage, null);
        
        m_SUT.deactivate();
        verify(m_HandlerReg).unregister();
    }
    
    /**
     * Verify that the event handler will not call a handler if no handler is associated with the message.
     */
    @Test
    public void testQueueMessageWithoutHandlerReg() throws IOException
    {
        //setup and activate component
        activateSetUp();

        //message as response
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                 setType(AssetMessageType.ActivateResponse).build();
        TerraHarvestMessage request = createTerraHarvestMessage(0, Namespace.Asset, nameMessage, false);
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
            
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, request);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());

        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);

        m_ResponseHandler.handleEvent(event);
        //verify
        verify(m_Handler, never()).handleResponse(Mockito.any(TerraHarvestMessage.class),
            Mockito.any(TerraHarvestPayload.class), Mockito.any(Message.class),Mockito.any(Message.class));
    }
    
    /**
     * Verify that the correct handler is called.
     */
    @Test
    public void testQueueMessageWithMultipleHandlers() throws IOException
    {
        //setup and activate component
        activateSetUp();

        //create another handler
        ResponseHandler handler2 = mock(ResponseHandler.class);

        // construct data message to send, should reg handler
        ActivateRequestData messageData = ActivateRequestData.newBuilder().
                setUuid(UUID.newBuilder().setLeastSignificantBits(0L).setMostSignificantBits(1L).build()).build();
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(messageData.toByteString()).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0, Namespace.Asset, nameMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        m_SUT.queueMessage(m_RemoteId, TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload()), 
                EncryptType.NONE, m_Handler);
        
        //use other handler
        TerraHarvestMessage requestSecond = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 1, Namespace.Asset, nameMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(requestSecond);
        m_SUT.queueMessage(m_RemoteId, TerraHarvestPayload.parseFrom(requestSecond.getTerraHarvestPayload()), 
                EncryptType.NONE, handler2);
        
        //verify
        assertThat(m_SUT.getResponseHandleRegCount(), is(2));
        
        //message as response
        nameMessage = AssetNamespace.newBuilder().
                 setType(AssetMessageType.ActivateResponse).build();
        request = createTerraHarvestMessage(0, Namespace.Asset, nameMessage, true);
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, request);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());

        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);

        //handle event first handle should be called
        m_ResponseHandler.handleEvent(event);
        verify(m_Handler).handleResponse(request,payLoadTest, nameMessage, null);
        verify(handler2, never()).handleResponse(request, payLoadTest, nameMessage, null);
        
        //create another terra harvest message, increase message id by one
        request = createTerraHarvestMessage(1, Namespace.Asset, nameMessage, true);
        payLoadTest = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
                
        // properties for the event
        final Map<String, Object> props2 = new HashMap<String, Object>();
        props2.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props2.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props2.put(RemoteConstants.EVENT_PROP_MESSAGE, request);
        props2.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props2.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props2.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props2.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props2.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());

        Event event2 = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props2);

        //verify second handler is called, and not the original
        m_ResponseHandler.handleEvent(event2);
        
        verify(handler2).handleResponse(request, payLoadTest, nameMessage, null);
        verify(m_Handler, never()).handleResponse(request, payLoadTest, nameMessage, null);
    }
    
    /**
     * Verify that the correct handler is called.
     * Verify that the message id is correct for the handler.
     */
    @Test
    public void testQueueMessageWithMultipleHandlersFailTrySend() throws IOException
    {
        //setup and activate component
        activateSetUp();

        //create another handler
        ResponseHandler handler2 = mock(ResponseHandler.class);

        // construct data message to send, should reg handler
        ActivateRequestData messageData = ActivateRequestData.newBuilder().
                setUuid(UUID.newBuilder().setLeastSignificantBits(0L).setMostSignificantBits(1L).build()).build();
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(messageData.toByteString()).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0, Namespace.Asset, nameMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        // make channel return false see that the message will fail to be queued or sent
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        //request to send
        m_SUT.queueMessage(m_RemoteId, TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload()), 
                EncryptType.NONE, m_Handler);

        //verify that if queue and try send fail that handler is not registered
        assertThat(m_SUT.getResponseHandleRegCount(), is(0));
        
        // make channel return true for queued
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);

        //now try queuing message again with queue successful
        m_SUT.queueMessage(m_RemoteId, TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload()), 
                EncryptType.NONE, m_Handler);
        
        //verify added
        assertThat(m_SUT.getResponseHandleRegCount(), is(1));
        
        //use other handler
        TerraHarvestMessage requestSecond = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 1, Namespace.Asset, nameMessage);
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(requestSecond);
        m_SUT.queueMessage(m_RemoteId, TerraHarvestPayload.parseFrom(requestSecond.getTerraHarvestPayload()), 
            EncryptType.NONE, handler2);
        
        //verify
        assertThat(m_SUT.getResponseHandleRegCount(), is(2));
        
        //message as response
        nameMessage = AssetNamespace.newBuilder().
                 setType(AssetMessageType.ActivateResponse).build();
        //message id is 3 because of the failed attempts at registering a listener.
        TerraHarvestMessage response = createTerraHarvestMessage(3, Namespace.Asset, nameMessage, true);
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
                
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE, true);

        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);

        //handle event first handle should be called
        m_ResponseHandler.handleEvent(event);
        verify(m_Handler).handleResponse(response, payLoadTest, nameMessage, null);
        verify(handler2, never()).handleResponse(response, payLoadTest, nameMessage, null);
        
        //create another terra harvest message, increase message id by one
        //Increasing by one because the message id will increase with every attempt to send
        //the message.
        response = createTerraHarvestMessage(5, Namespace.Asset, nameMessage, true);
        payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
                
        // properties for the event
        final Map<String, Object> props2 = new HashMap<String, Object>();
        props2.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props2.put(RemoteConstants.EVENT_PROP_PAYLOAD, payLoadTest);
        props2.put(RemoteConstants.EVENT_PROP_MESSAGE, response);
        props2.put(RemoteConstants.EVENT_PROP_SOURCE_ID, m_RemoteId);
        props2.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props2.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, nameMessage);
        props2.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props2.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ActivateResponse.toString());
        props2.put(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE, true);

        Event event2 = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props2);

        //verify second handler is called, and not the original
        m_ResponseHandler.handleEvent(event2);
        
        verify(handler2).handleResponse(response, payLoadTest, nameMessage, null);
        verify(m_Handler, never()).handleResponse(response, payLoadTest, nameMessage, null);
    }

    /**
     * Verify that the send base message method will pass the correct message based on the destination id.
     * Verify that given no encryption level that the appropriate level is obtained from the service.
     */
    @Test
    public void testSendBaseMessageRemoteSystemEncryptionService() throws IOException
    {
        //set the service
        m_SUT.setRemoteSystemEncryption(m_RemoteSystemEncryption);
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        BaseNamespace baseNamespace = BaseNamespace.newBuilder().
                setType(BaseMessageType.ControllerInfo).
                setData(systemInfoData.toByteString()).build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, baseNamespace);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        m_SUT.trySendMessage(m_RemoteId, payload);
        
        //capture the message being sent to the encryption service
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService).encryptMessage(builderCaptor.capture(), eq(payload));
        assertThat(builderCaptor.getValue().getEncryptType(), is(EncryptType.AES_ECDH_ECDSA));
    }
    
    /**
     * Verifies that the queue remote base message method will pass the correct message.  
     *  
     * Verifies message is properly encrypted based upon data returned from the remote encryption service.
     */
    @Test
    public void testQueueBaseMessageRemoteEncryptionService() throws IOException
    {
      //set the service
        m_SUT.setRemoteSystemEncryption(m_RemoteSystemEncryption);
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);

        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, systemInfoData);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        m_SUT.queueMessage(m_RemoteId, payload, null);
        
        //capture the message being sent to the encryption service
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService).encryptMessage(builderCaptor.capture(), eq(payload));
        assertThat(builderCaptor.getValue().getEncryptType(), is(EncryptType.AES_ECDH_ECDSA));
    }
    
    /**
     * Verify if the encryption type is not known for a system, an exception is thrown
     */
    @Test
    public void testUnknownEncryptionType() throws IOException
    {
        //set the service
        m_SUT.setRemoteSystemEncryption(m_RemoteSystemEncryption);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);

        // construct data message to send
        ControllerInfoData controllerInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, controllerInfoData);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        try
        {
            m_SUT.queueMessage(m_RemoteId, payload, null);
            fail("Expecting exception as remote id is not known by remote encryption service");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Verify that an exception is thrown in the event that no remote encryption service is set and the
     * send method without the encryption parameter is called.
     */
    @Test
    public void testSendBaseMessageRemoteSystemEncryptionServiceException() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        BaseNamespace baseNamespace = BaseNamespace.newBuilder().
                setType(BaseMessageType.ControllerInfo).
                setData(systemInfoData.toByteString()).build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, baseNamespace);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        try
        {
            m_SUT.setRemoteSystemEncryption(null);
            m_SUT.trySendMessage(m_RemoteId, payload);
            fail("Expected exception as the remote encryption service is not available.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that an exception is thrown in the event that no remote encryption service is set and the
     * queue method without the encryption parameter is called.
     */
    @Test
    public void testQueueBaseMessageRemoteEncryptionServiceException() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, systemInfoData);
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        try
        {
            m_SUT.setRemoteSystemEncryption(null);
            m_SUT.queueMessage(m_RemoteId, payload, null);
            fail("Expected exception as the remote encryption service is not available.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that the message encryption level is set to that of the remote system's encryption level when the local
     * system's encryption level is lower.
     */
    @Test
    public void testEncryptionLevelCheck() throws InvalidProtocolBufferException
    {
        // construct test data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, systemInfoData);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        //Test remote system has higher encryption level than local system.
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        m_SUT.queueMessage(m_RemoteId, payload, null);
        
        //Test local system has higher encryption level than remote system.
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.NONE);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        m_SUT.queueMessage(m_RemoteId, payload, null);
        
        //Test both local and remote having no encryption
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.NONE);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        m_SUT.queueMessage(m_RemoteId, payload, null);
        
        //capture the message being sent to the encryption service
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService, times(3)).encryptMessage(builderCaptor.capture(), eq(payload));
        assertThat(builderCaptor.getAllValues().get(0).getEncryptType(), is(EncryptType.AES_ECDH_ECDSA));
        assertThat(builderCaptor.getAllValues().get(1).getEncryptType(), is(EncryptType.AES_ECDH_ECDSA));
        assertThat(builderCaptor.getAllValues().get(2).getEncryptType(), is(EncryptType.NONE));
        
        // construct encryption info namespace message.
        GetEncryptionTypeResponseData encryptResponse = GetEncryptionTypeResponseData.newBuilder().
                setType(EncryptType.AES_ECDH_ECDSA).build();
        request = createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.EncryptionInfo, encryptResponse);
        payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        //Test send encryption info namespace message. Encryption level should be none.
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        m_SUT.queueMessage(m_RemoteId, payload, null);
        
        builderCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService, times(1)).encryptMessage(builderCaptor.capture(), eq(payload));
        assertThat(builderCaptor.getAllValues().get(0).getEncryptType(), is(EncryptType.NONE));
    }
    
    /**
     * Verify that a message can be queued for a specific remote channel.
     */
    @Test
    public void testQueueMessageByChannel() throws InvalidProtocolBufferException
    {
        ResponseHandler handler = mock(ResponseHandler.class);
        
        // construct test data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0 , Namespace.Base, systemInfoData);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        //Mock behavior.
        when(m_RemoteChannelLookup.getChannelSystemId(m_Channel)).thenReturn(m_RemoteId);
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        when(m_RemoteSystemEncryption.getEncryptType(m_RemoteId)).thenReturn(EncryptType.NONE);
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        
        //Verify true is return if the message is successfully queue to the channel. No handler so list should be empty.
        assertThat(m_SUT.queueMessage(m_Channel, payload, null), is(true));
        assertThat(m_SUT.getResponseHandleRegCount(), is(0));
        
        //Verify true is return if the message is successfully queue to the channel. Also verify that the response 
        //handler was added.
        assertThat(m_SUT.queueMessage(m_Channel, payload, handler), is(true));
        assertThat(m_SUT.getResponseHandleRegCount(), is(1));
        
        //Mock failing to queue to channel.
        when(m_Channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        
        //Verify false is returned if the message is not successfully queue to the channel. Also verify no that the
        //response handler count remains at one.
        assertThat(m_SUT.queueMessage(m_Channel, payload, handler), is(false));
        assertThat(m_SUT.getResponseHandleRegCount(), is(1));
    }
    
    /**
     * Verify that the event channel queue message method is called.
     */
    @Test
    public void testQueueMessageWithEventChannel() throws IOException
    {
        //setup and activate component
        activateSetUp();

        // construct data message to send, should reg handler
        ActivateRequestData messageData = ActivateRequestData.newBuilder().
                setUuid(UUID.newBuilder().setLeastSignificantBits(0L).setMostSignificantBits(1L).build()).build();
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(messageData.toByteString()).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0, Namespace.Asset, nameMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        when(m_EventChannel.getRemoteSystemId()).thenReturn(m_RemoteId);
        when(m_EventChannel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        when(m_RemoteChannelLookup.getChannels(m_RemoteId)).thenReturn(new ArrayList<RemoteChannel>());
        m_SUT.queueMessage(m_EventChannel, payload, EncryptType.NONE, null);
        
        verify(m_EventChannel).queueMessage(request);
    }
    
    /**
     * Verify that the queue message method will check whether there is an available remote channel to the destination
     * if an event channel is passed to the method.
     */
    @Test
    public void testQueueMessageWithConvertToRemoteChannel() throws IOException
    {
        //setup and activate component
        activateSetUp();

        // construct data message to send, should reg handler
        ActivateRequestData messageData = ActivateRequestData.newBuilder().
                setUuid(UUID.newBuilder().setLeastSignificantBits(0L).setMostSignificantBits(1L).build()).build();
        AssetNamespace  nameMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(messageData.toByteString()).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_LocalId, m_RemoteId, 0, Namespace.Asset, nameMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(request.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(request);
        when(m_EventChannel.getRemoteSystemId()).thenReturn(m_RemoteId);
        when(m_EventChannel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        final List<RemoteChannel> channelList = new ArrayList<RemoteChannel>();
        channelList.add(m_Channel);
        when(m_RemoteChannelLookup.getChannels(m_RemoteId)).thenReturn(channelList);
        
        m_SUT.queueMessage(m_EventChannel, payload, EncryptType.NONE, null);
        
        verify(m_Channel).queueMessage(request);
    }
    
    /**
     * Verifies that the event channel is used to queue the response message.
     */
    @Test
    public void testQueueResponseWithEventChannel() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;
        
        // construct fake request message that should be used by service to construct the response
        MissionProgrammingNamespace requestMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_RemoteId, m_LocalId, 0 , Namespace.MissionProgramming, requestMessage);
        
        // construct data message
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MissionProgrammingNamespace responseMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesResponse)
                .setData(getTemplateData.toByteString()).build();
        
        TerraHarvestMessage response = 
                createTerraHarvestMessage(messageId, Namespace.MissionProgramming, responseMessage, true);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        when(m_EventChannel.getRemoteSystemId()).thenReturn(m_RemoteId);
        when(m_EventChannel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        when(m_RemoteChannelLookup.getChannels(m_RemoteId)).thenReturn(new ArrayList<RemoteChannel>());
        
        boolean result = m_SUT.queueMessageResponse(request, payload, m_EventChannel);
        assertThat("message is queued as reported by channel", result, is(true));
        
        // verify message is sent to the associated channel with proper fields set
        verify(m_EventChannel).queueMessage(response);
    }
    
    @Test
    public void testQueueResponseWithConvertToRemoteChannel() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;
        
        // construct fake request message that should be used by service to construct the response
        MissionProgrammingNamespace requestMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(m_RemoteId, m_LocalId, 0 , Namespace.MissionProgramming, requestMessage);
        
        // construct data message
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MissionProgrammingNamespace responseMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesResponse)
                .setData(getTemplateData.toByteString()).build();
        
        TerraHarvestMessage response = 
                createTerraHarvestMessage(messageId, Namespace.MissionProgramming, responseMessage, true);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        when(m_EventChannel.getRemoteSystemId()).thenReturn(m_RemoteId);
        when(m_EventChannel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        final List<RemoteChannel> channelList = new ArrayList<RemoteChannel>();
        channelList.add(m_Channel);
        when(m_RemoteChannelLookup.getChannels(m_RemoteId)).thenReturn(channelList);
        
        boolean result = m_SUT.queueMessageResponse(request, payload, m_EventChannel);
        assertThat("message is queued as reported by channel", result, is(true));
        
        // verify message is sent to the associated channel with proper fields set
        verify(m_Channel).queueMessage(response);
    }
    
    /**
     * Verify that the response message uses source ID of request message since the remote channel system ID does not
     * match that of the requests source ID.
     */
    @Test
    public void testQueueResponseWithVaryingId() throws IOException
    {
        // id to use for the request and response
        int messageId = 100;
        
        // id for the remote channel as returned by the remote channel lookup.
        int sourceId = 50;
        
        // construct fake request message that should be used by service to construct the response
        MissionProgrammingNamespace requestMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestMessage request = 
                createMessageWithDestSource(sourceId, m_LocalId, 0 , Namespace.MissionProgramming, requestMessage);
        
        // construct data message
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MissionProgrammingNamespace responseMessage = MissionProgrammingNamespace.newBuilder()
                .setType(MissionProgrammingMessageType.GetTemplatesResponse)
                .setData(getTemplateData.toByteString()).build();
        
        TerraHarvestMessage response = 
                createTerraHarvestMessage(messageId, Namespace.MissionProgramming, responseMessage, true);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        when(m_EncryptService.encryptMessage(Mockito.any(Builder.class), Mockito.any(TerraHarvestPayload.class))).
            thenReturn(response);
        when(m_RemoteChannelLookup.getChannelSystemId(m_Channel)).thenReturn(m_RemoteId);
        
        boolean result = m_SUT.queueMessageResponse(request, payload, m_Channel);
        assertThat("message is queued as reported by channel", result, is(true));
        
        // verify message is sent to the associated channel
        verify(m_Channel).queueMessage(response);
        
        ArgumentCaptor<TerraHarvestMessage.Builder> builderCaptor = 
                ArgumentCaptor.forClass(TerraHarvestMessage.Builder.class);
        verify(m_EncryptService).encryptMessage(builderCaptor.capture(), eq(payload));
        TerraHarvestMessage.Builder builder = builderCaptor.getValue();
        
        //Verify source ID for the request is used as the destination ID of the response and not the channel's remote
        //system ID.
        assertThat(builder.getDestId(), is(sourceId));
    }
    
    /**
     * Inner helper class that construct an valid TerraHarvestMessage.
     * @param localId
     *     the id of the sending system
     * @param remoteId
     *     the id of the receiving system
     * @param messageId
     *     the desired message id
     * @param namespace
     *     the namespace from which the namespace message represents
     * @param namespaceMessage
     *      the namespace specific message
     * @return
     *     a valid TerraHarvestMessage
     */
    private TerraHarvestMessage createMessageWithDestSource(int localId, int remoteId, int messageId, 
            Namespace namespace, Message namespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(localId, remoteId, namespace, messageId, 
                namespaceMessage);
    }
    
    /**
     * Inner helper class constructs an valid TerraHarvestMessage which can be identified as a response.
     * @param messageId
     *     the desired message id
     * @param namespace
     *     the namespace from which the namespace message represents
     * @param namespaceMessage
     *      the namespace specific message
     * @param isResponse
     *      denotes that this message is a response
     * @return
     *     a valid TerraHarvestMessage
     */
    private TerraHarvestMessage createTerraHarvestMessage(int messageId, Namespace namespace, Message namespaceMessage, 
        boolean isResponse)
    {
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(m_LocalId, m_RemoteId, 
            namespace, messageId, namespaceMessage);
        if (isResponse)
        {
            TerraHarvestMessage.Builder builder = thMessage.toBuilder();
            return builder.setIsResponse(true).build();
        }
        
        return thMessage;
    }
}
