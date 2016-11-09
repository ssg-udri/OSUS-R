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

package mil.dod.th.ose.remote;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.encryption.EncryptMessageService;
import mil.dod.th.ose.remote.encryption.InvalidKeySignatureException;
import mil.dod.th.ose.remote.messaging.TerraHarvestMessageUtil;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author Dave Humeniuk
 */
public class TestMessageRouterImpl
{

    private MessageRouterImpl m_SUT;
    private LoggingService m_Logging;
    private TerraHarvestSystem m_TerraSystem;
    private EncryptMessageService m_EcryptService;
    private RemoteSettings m_RemoteSettings;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private EventAdmin m_EventAdmin;
    
    private int m_SystemId = 1;

    /**
     * Setup message router for testing, bind a logging service.
     */
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new MessageRouterImpl();
        m_TerraSystem = mock(TerraHarvestSystem.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setTerraHarvestSystem(m_TerraSystem);
        m_EcryptService = mock(EncryptMessageService.class);
        m_SUT.setEncryptMessageService(m_EcryptService);
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_SUT.bindRemoteChannelLookup(m_RemoteChannelLookup);
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        
        when(m_TerraSystem.getId()).thenReturn(m_SystemId);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
    }
    
    /**
     * Test the message handler.  Verifies messages are routed to proper MessageService.
     * 
     * Verify message are only routed once bound.
     */
    @Test
    public void testHandleMessage() throws Exception
    {
        // construct a single base namespace message to verify sent to base message service
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(baseNamespaceMessage.toByteString()).
            build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload);
        // replay without a message service registered, should not bomb out
        m_SUT.handleMessage(baseMessage, channel);
        
        // now register a service that will handle the message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        m_SUT.bindMessageService(baseMessageService);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify proper message service is sent the same message
        verify(baseMessageService).handleMessage(baseMessage, payload, channel);
        
        // now register a service that will handle a different namespace
        MessageService assetMessageService = mock(MessageService.class);
        when(assetMessageService.getNamespace()).thenReturn(Namespace.Asset);
        m_SUT.bindMessageService(assetMessageService);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify proper message service is sent the same message, should be 2nd time
        verify(baseMessageService, times(2)).handleMessage(baseMessage, payload, channel);
        
        // set destination ID of message to max value
        TerraHarvestMessage maxIdMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, Integer.MAX_VALUE, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // replay with message that has max destination ID, message should still be handled
        m_SUT.handleMessage(maxIdMessage, channel);
        
        // verify proper message service is sent the message with the max destination ID
        verify(baseMessageService).handleMessage(maxIdMessage, payload, channel);
        
        // create a new message of a different type
        TerraHarvestMessage assetMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Asset, 100, 
                        baseNamespaceMessage);
        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Asset).
                setNamespaceMessage(baseNamespaceMessage.toByteString()).
                build();
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload1);
        
        // replay
        m_SUT.handleMessage(assetMessage, channel);
        
        // verify proper message service is sent the same message, base should still be at 3, asset at 1
        verify(baseMessageService, times(2)).handleMessage(baseMessage, payload, channel);
        verify(baseMessageService, times(1)).handleMessage(maxIdMessage, payload, channel);
        verify(assetMessageService, times(1)).handleMessage(assetMessage, payload1, channel);
    }
    
    /**
     * Verify that an event channel is used if no channel is specified when handling a message.
     */
    @Test
    public void testHandleMessageNoChannel() throws UnmarshalException, IOException, ObjectConverterException, 
        InvalidKeySignatureException
    {
        // construct a single base namespace message to verify sent to base message service
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(baseNamespaceMessage.toByteString()).
            build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload);
        
        // now register a service that will handle the message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        m_SUT.bindMessageService(baseMessageService);
        
        // replay
        m_SUT.handleMessage(baseMessage);
        
        ArgumentCaptor<EventChannel> channelCaptor = ArgumentCaptor.forClass(EventChannel.class);
        
        // verify proper message service is sent the same message
        verify(baseMessageService).handleMessage(eq(baseMessage), eq(payload), channelCaptor.capture());
        
        EventChannel channel = channelCaptor.getValue();
        assertThat(channel.getRemoteSystemId(), is(0));
    }
    
    @Test
    public void testHandleMessageRouteToDest()
    {
        // construct a single base namespace message to verify sent to base message service
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 500, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(baseMessage)).thenReturn(true);
        List<RemoteChannel> channels = new ArrayList<RemoteChannel>();
        channels.add(channel);
        
        when(m_RemoteChannelLookup.getChannels(500)).thenReturn(channels);
        
        m_SUT.handleMessage(baseMessage, null);
        
        verify(channel).queueMessage(baseMessage);
    }
    
    @Test
    public void testHandleMessageInvalidDest()
    {
        // construct a single base namespace message to verify sent to base message service
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 500, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        List<RemoteChannel> channels = new ArrayList<RemoteChannel>();
        when(m_RemoteChannelLookup.getChannels(500)).thenReturn(channels);
        
        m_SUT.handleMessage(baseMessage, null);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event invalidDestEvent = eventCaptor.getValue();
        assertThat(invalidDestEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(500));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(baseMessage));
    }
    
    /**
     * Verify message handler handles an invalid protocol exception. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_InvalidProtoBufferException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        //---Verify protocol buffer exception (will throw an IO exception since it extends it)---
        InvalidProtocolBufferException exception = new InvalidProtocolBufferException("exception-message");
        doThrow(exception).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class), eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay, should not bomb
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.INVALID_REQUEST, 
                "Failed IO operation when handling [Base] namespace message: exception-message");
    }
    
    /**
     * Verify message handler handles an illegal argument exception. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_IllegalArgException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        //---Verify illegal arg exception---
        IllegalArgumentException illegalException = new IllegalArgumentException("exception-message");
        doThrow(illegalException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.INVALID_VALUE, 
                "Invalid value when handling [Base] namespace message: exception-message");
        
        // test with empty exception message
        illegalException = new IllegalArgumentException();
        doThrow(illegalException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        verifyBaseErrorResponse(channel, ErrorCode.INVALID_VALUE, 
                "Invalid value when handling [Base] namespace message: null"); 
    }
    
    /**
     * Verify message handler handles an illegal state exception. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_IllegalStateException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        //---Verify illegal state exception---
        IllegalStateException illegalStateException = new IllegalStateException("exception-message");
        doThrow(illegalStateException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.ILLEGAL_STATE, 
                "Illegal state when handling [Base] namespace message: exception-message");
        
        // test with empty exception message
        illegalStateException = new IllegalStateException();
        doThrow(illegalStateException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        verifyBaseErrorResponse(channel, ErrorCode.ILLEGAL_STATE, 
                "Illegal state when handling [Base] namespace message: null");  
    }
    
    /**
     * Verify message handler handles an {@link UnmarshalException}. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_UnarshalException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        UnmarshalException exception = new UnmarshalException("exception-message");
        doThrow(exception).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.JAXB_ERROR, 
                "JAXB error when handling [Base] namespace message: exception-message");
    }
    
    /**
     * Verify message handler handles an {@link ObjectConverterException}. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_ObjectConverterException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        ObjectConverterException exception = new ObjectConverterException("exception-message");
        doThrow(exception).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.CONVERTER_ERROR, 
                "Proto converter error when handling [Base] namespace message: exception-message");
    }
    
    /**
     * Verify message handler handles a runtime exception. Verify the channel the message
     * was sent on queues an error response message for the exception.
     */
    @Test
    public void testHandleMessage_RuntimeException() throws Exception
    {
        // construct a single base namespace message just to satisfy input
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // register a service that will throw exception when handling message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        
        //---Verify random exception---
        RuntimeException randomException = new RuntimeException("exception-message");
        doThrow(randomException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        m_SUT.bindMessageService(baseMessageService);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify error response is queued from the channel
        verifyBaseErrorResponse(channel, ErrorCode.INTERNAL_ERROR, 
                "RuntimeException when handling [Base] namespace message: exception-message");
        
        // test with empty exception message
        randomException = new RuntimeException();
        doThrow(randomException).when(baseMessageService).handleMessage(Mockito.any(TerraHarvestMessage.class), 
            Mockito.any(TerraHarvestPayload.class),eq(channel));
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        verifyBaseErrorResponse(channel, ErrorCode.INTERNAL_ERROR, 
                "RuntimeException when handling [Base] namespace message: null");        
    }
    
    /**
     * Verify if a message service is unbound it will no longer be used to handle messages.
     */
    @Test
    public void testUnbindMessageService() throws Exception
    {
        // construct a single base namespace message to verify sent to base message service
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseNamespaceMessage.toByteString()).
                build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, Namespace.Base, 100, 
                        baseNamespaceMessage);
         
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload);
        
        // now register a service that will handle the message
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        m_SUT.bindMessageService(baseMessageService);
        
        // now unbind
        m_SUT.unbindMessageService(baseMessageService);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify proper message service is sent the same message
        verify(baseMessageService, never()).handleMessage(baseMessage, payload, channel);
    }
    
    /**
     * Verify if a message service is not set that an error response is queued to the channel that was
     * attempting to handle a message.
     *  
     */
    @Test
    public void testInvalidRemoteMessageService() throws InvalidProtocolBufferException, InvalidKeySignatureException
    {
        TerraHarvestMessage baseMessage = TerraHarvestMessageHelper.createBaseMessage();
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload()) ;
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        //decryption call
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).
            thenReturn(payLoadTest);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        TerraHarvestMessage testMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(baseMessage, 
                ErrorCode.NO_MESSAGE_SERVICE, "No remote message service found to handle the message: " + 
                payLoadTest.getNamespace());
        
        // verify error response is queued from the channel
        verify(channel).queueMessage(testMessage);
    }
    
    /**
     * Verify if a message is received and the encryption type is none, but the system is set to a level 
     * that an error response is sent.
     *  
     */
    @Test
    public void testUnsupportedEncryption()
    {
        TerraHarvestMessage baseMessage = TerraHarvestMessageHelper.createBaseMessage();
        
        //change the encryption type
        TerraHarvestMessage.Builder builder = baseMessage.toBuilder();
        baseMessage = builder.setEncryptType(EncryptType.NONE).setIsResponse(true).build();
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        TerraHarvestMessage testMessage = TerraHarvestMessageUtil.buildEncryptionErrorResponseMessage(baseMessage, 
            EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL, 
            String.format("System %s does not support type NONE encryption.", m_SystemId), 
            EncryptionMode.AES_ECDH_ECDSA);
        
        // verify error response is queued from the channel
        verify(channel).queueMessage(testMessage);
    }
    
    /**
     * Verify if a message is received and the encryption type is ECDH, but the system is set to a NONE 
     * that the message is attempted to be handled.
     *  
     */
    @Test
    public void testAdaptiveEncryption() throws Exception
    {
        TerraHarvestMessage baseMessage = TerraHarvestMessageHelper.createBaseMessage();
        
        //change the encryption type
        TerraHarvestMessage.Builder builder = baseMessage.toBuilder();
        baseMessage = builder.setEncryptType(EncryptType.AES_ECDH_ECDSA).setIsResponse(true).build();
        
        //payload
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(baseMessage.getTerraHarvestPayload());
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        //encryption service behavior
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload);
        
        MessageService baseMessageService = mock(MessageService.class);
        when(baseMessageService.getNamespace()).thenReturn(Namespace.Base);
        m_SUT.bindMessageService(baseMessageService);
        
        // replay
        m_SUT.handleMessage(baseMessage, channel);
        
        // verify proper message service is sent the same message
        verify(baseMessageService).handleMessage(baseMessage, payload, channel);
        
        // verify no error response is queued from the channel
        verify(channel, never()).queueMessage(Mockito.any(TerraHarvestMessage.class));
    }
    
    /**
     * Verify that the Encryption information message is always acknowledged and handled appropriately even when the 
     * system has an encryption level higher than none.
     */
    @Test
    public void testHandleEncryptionInfoMessage() throws Exception
    {
        EncryptionInfoNamespace namepsaceMessage = EncryptionInfoNamespace.newBuilder().setType(
                EncryptionInfoMessageType.GetEncryptionTypeRequest).build();
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(0, m_SystemId, 
                Namespace.EncryptionInfo, 225, namepsaceMessage, EncryptType.NONE);
        
        //payload
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        //encryption service behavior
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(payload);
        
        MessageService encryptInfoMessageService = mock(MessageService.class);
        when(encryptInfoMessageService.getNamespace()).thenReturn(Namespace.EncryptionInfo);
        m_SUT.bindMessageService(encryptInfoMessageService);
        
        // replay
        m_SUT.handleMessage(thMessage, channel);
        
        // verify proper message service is sent the same message
        verify(encryptInfoMessageService).handleMessage(thMessage, payload, channel);
        
        // verify no error response is queued from the channel
        verify(channel, never()).queueMessage(Mockito.any(TerraHarvestMessage.class));
    }
    
    /**
     * Verify that the invalid protocol exception is handled correctly when calling the handle message function.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testHandlePayloadInvalidProtocolException() throws InvalidProtocolBufferException, 
        InvalidKeySignatureException
    {
        TerraHarvestMessage baseMessage = TerraHarvestMessageHelper.createBaseMessage();
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        //encryption service behavior force throw invalid protocol exception.
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenThrow(
                InvalidProtocolBufferException.class);
        
        m_SUT.handleMessage(baseMessage, channel);
        
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(channel).queueMessage(messageCaptor.capture());
        
        //Verify invalid protocol message.
        TerraHarvestMessage errorMessage = messageCaptor.getAllValues().get(0);
        TerraHarvestPayload errorPayload = TerraHarvestPayload.parseFrom(errorMessage.getTerraHarvestPayload());
        BaseNamespace baseNamespaceMessage = BaseNamespace.parseFrom(errorPayload.getNamespaceMessage());
        GenericErrorResponseData errorResponse = GenericErrorResponseData.parseFrom(baseNamespaceMessage.getData());
        
        assertThat(errorResponse.getError(), is(ErrorCode.INVALID_REQUEST));
        assertThat(errorResponse.getErrorDescription(), is("Invalid payload exception when handling a message"));
    }
    
    /**
     * Verify that the invalid key signature exception is handled correctly when calling the handle message function.
     */
    @Test
    public void testHandlePayloadInvalidKeySignatureException() throws InvalidProtocolBufferException, 
        InvalidKeySignatureException
    {
        TerraHarvestMessage baseMessage = TerraHarvestMessageHelper.createBaseMessage();
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        when(channel.queueMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        //encryption service behavior force throw invalid protocol exception.
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.NONE);
        when(m_EcryptService.decryptRemoteMessage(Mockito.any(TerraHarvestMessage.class))).thenThrow(
                new InvalidKeySignatureException("test failure!"));
        
        m_SUT.handleMessage(baseMessage, channel);
        
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(channel).queueMessage(messageCaptor.capture());
        
        //Verify invalid key signature method.
        TerraHarvestMessage errorMessage = messageCaptor.getAllValues().get(0);
        TerraHarvestPayload errorPayload = TerraHarvestPayload.parseFrom(errorMessage.getTerraHarvestPayload());
        EncryptionInfoNamespace namespaceMessage = 
                EncryptionInfoNamespace.parseFrom(errorPayload.getNamespaceMessage());
        EncryptionInfoErrorResponseData errorResponse = 
                EncryptionInfoErrorResponseData.parseFrom(namespaceMessage.getData());
        
        assertThat(errorResponse.getError(), is(EncryptionErrorCode.INVALID_SIGNATURE_KEY));
        assertThat(errorResponse.getErrorDescription(), 
                is("test failure!"));
    }
    
    /**
     * Verify that the remote channel lookup is unbound appropriately.
     */
    @Test
    public void testUnbindRemoteChannelLookup()
    {
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage baseMessage = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 500, Namespace.Base, 100, 
                        baseNamespaceMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        List<RemoteChannel> channels = new ArrayList<RemoteChannel>();
        channels.add(channel);
        when(m_RemoteChannelLookup.getChannels(500)).thenReturn(channels);
        
        m_SUT.unbindRemoteChannelLookup(m_RemoteChannelLookup);
        
        m_SUT.handleMessage(baseMessage);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event invalidDestEvent = eventCaptor.getValue();
        assertThat(invalidDestEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(500));
        assertThat(invalidDestEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(baseMessage));
    }
    
    public void verifyBaseErrorResponse(RemoteChannel channel, ErrorCode errorCode, String errorDescription) 
        throws InvalidProtocolBufferException
    {
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(channel, atLeastOnce()).queueMessage(messageCaptor.capture());
        TerraHarvestMessage message = messageCaptor.getValue();
        
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
        assertThat(payload.getNamespace(), is(Namespace.Base));
        
        BaseNamespace baseMessage = BaseNamespace.parseFrom(payload.getNamespaceMessage());
        assertThat(baseMessage.getType(), is(BaseMessageType.GenericErrorResponse));
        
        GenericErrorResponseData data = GenericErrorResponseData.parseFrom(baseMessage.getData());
        assertThat(data.getError(), is(errorCode));
        assertThat(data.getErrorDescription(), is(errorDescription));
    }
}
