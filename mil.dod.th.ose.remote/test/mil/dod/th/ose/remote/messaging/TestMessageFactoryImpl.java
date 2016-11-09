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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.messaging.MessageWrapperAutoEncryption;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.StartRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelResponseData;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen.AssetCapabilities;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen.MissionProgramTemplate;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

/**
 * Test class for the Message factory implementation.
 * @author allenchl
 *
 */
public class TestMessageFactoryImpl
{
    private MessageFactoryImpl m_SUT;
    private MessageSender m_MessageSender;
    
    //system IDs
    private int m_RemoteId;
    private int m_LocalId;
    private LoggingService m_Logging;
    
    @Before
    public void setup()
    {
        //system under test
        m_SUT = new MessageFactoryImpl();
        
        //mock message sender
        m_MessageSender = mock(MessageSender.class);
        
        //set the service
        m_SUT.setMessageSender(m_MessageSender);
       
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        //system Ids
        m_LocalId = 102;
        m_RemoteId = 282;
    }
    
    /**
     * Verify the message response wrapper returned contains the expected error message contents.
     */
    @Test
    public void testQueueBaseErrorCode() throws IOException
    {
        // create the original request message that the response will be based on
        // required to be set Don't care what it is though
        TerraHarvestMessage request =  createRequestFiller();
        
        MessageResponseWrapper response = m_SUT.createBaseErrorMessage(request, 
                ErrorCode.ILLEGAL_STATE, 
                "Illegal State errror");

        //request to send over mock channel
        RemoteChannel channel = mock(RemoteChannel.class);
        response.queue(channel);
        
        //capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender).queueMessageResponse(eq(request), payloadCap.capture(), eq(channel));
        
        //verify Payload
        TerraHarvestPayload payloadCaptured = payloadCap.getValue();
        assertThat(payloadCaptured.getNamespace(), is(Namespace.Base));
        
        //namespace
        BaseNamespace baseResponseMessage = BaseNamespace.parseFrom(payloadCaptured.getNamespaceMessage());
        assertThat(baseResponseMessage.getType(), is(BaseMessageType.GenericErrorResponse));
        
        //specific message
        GenericErrorResponseData errorData = GenericErrorResponseData.parseFrom(baseResponseMessage.getData());
        assertThat(errorData.getError(), is(ErrorCode.ILLEGAL_STATE));
        assertThat(errorData.getErrorDescription(), is("Illegal State errror"));
    }
    
    /**
     * Verify the message response wrapper returned contains the expected error message contents for a generic error.
     * Verify the exception condition is logged too.
     */
    @Test
    public void testQueueGenericExceptionResponse() throws IOException
    {
        // create the original request message that the response will be based on
        // required to be set Don't care what it is though
        TerraHarvestMessage request =  createRequestFiller();
        
        Exception exception = new Exception("blah");
        MessageResponseWrapper response = m_SUT.createBaseErrorResponse(request, exception, "description");

        //request to send over mock channel
        RemoteChannel channel = mock(RemoteChannel.class);
        response.queue(channel);
        
        //capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender).queueMessageResponse(eq(request), payloadCap.capture(), eq(channel));
        
        //verify Payload
        TerraHarvestPayload payloadCaptured = payloadCap.getValue();
        assertThat(payloadCaptured.getNamespace(), is(Namespace.Base));
        
        //namespace
        BaseNamespace baseResponseMessage = BaseNamespace.parseFrom(payloadCaptured.getNamespaceMessage());
        assertThat(baseResponseMessage.getType(), is(BaseMessageType.GenericErrorResponse));
        
        //specific message
        GenericErrorResponseData errorData = GenericErrorResponseData.parseFrom(baseResponseMessage.getData());
        assertThat(errorData.getError(), is(ErrorCode.INTERNAL_ERROR));
        assertThat(errorData.getErrorDescription(), is("description: java.lang.Exception: blah"));
        
        // verify the error condition was logged including the exception as is
        verify(m_Logging).error(exception, "description");
    }
    
    /**
     * Verify the message response wrapper returned contains the expected error message contents for specific 
     * exceptions.
     */
    @Test
    public void testQueueSpecificExceptionResponse() throws IOException
    {
        TerraHarvestMessage request =  createRequestFiller();
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.createBaseErrorResponse(request, new AssetException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new CCommException(FormatProblem.ADDRESS_MISMATCH),
                "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new ObjectConverterException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new IllegalStateException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new IllegalArgumentException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new MarshalException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new UnmarshalException("blah"), "description").queue(channel);
        m_SUT.createBaseErrorResponse(request, new PersistenceFailedException("blah"), "description").queue(channel);

        //capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender, times(8)).queueMessageResponse(eq(request), payloadCap.capture(), eq(channel));

        List<ErrorCode> errorCodes = new ArrayList<>();
        for (TerraHarvestPayload payload : payloadCap.getAllValues())
        {
            BaseNamespace baseResponseMessage = BaseNamespace.parseFrom(payload.getNamespaceMessage());
            GenericErrorResponseData errorData = GenericErrorResponseData.parseFrom(baseResponseMessage.getData());
            errorCodes.add(errorData.getError());
        }
        assertThat(errorCodes.get(0), is(ErrorCode.ASSET_ERROR));
        assertThat(errorCodes.get(1), is(ErrorCode.CCOMM_ERROR));
        assertThat(errorCodes.get(2), is(ErrorCode.CONVERTER_ERROR));
        assertThat(errorCodes.get(3), is(ErrorCode.ILLEGAL_STATE));
        assertThat(errorCodes.get(4), is(ErrorCode.INVALID_VALUE));
        assertThat(errorCodes.get(5), is(ErrorCode.JAXB_ERROR));
        assertThat(errorCodes.get(6), is(ErrorCode.JAXB_ERROR));
        assertThat(errorCodes.get(7), is(ErrorCode.PERSIST_ERROR));
    }
    
    /**
     *  Verify the message wrapper returned contains the expected base message contents.
     */
    @Test
    public void testSendBaseMessage() throws IOException
    {
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        MessageWrapper request = m_SUT.createBaseMessage(BaseMessageType.ControllerInfo, systemInfoData);
        
        //request to send and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.Base, 1);
        
        // verify namespace message
        BaseNamespace baseMessageResponse = BaseNamespace.parseFrom(namespace);
        assertThat(baseMessageResponse.getType(), is(BaseMessageType.ControllerInfo));
        
        //verify data message
        assertThat(baseMessageResponse.getData(), is(systemInfoData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createBaseMessage(BaseMessageType.ControllerInfo, null);
        
        //request to send and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.Base, 2);
        
        // verify namespace message
        baseMessageResponse = BaseNamespace.parseFrom(namespace);
        assertThat(baseMessageResponse.getType(), is(BaseMessageType.ControllerInfo));
        
        //verify data message
        assertThat(baseMessageResponse.hasData(), is(false));
    }
    
    /**
     *  Verify the message response wrapper returned contains the expected base message contents.
     */
    @Test
    public void testQueueBaseMessageResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder().setName("blah").build();
        
        MessageResponseWrapper response = m_SUT.createBaseMessageResponse(request, BaseMessageType.ControllerInfo, 
                systemInfoData);

        //request to send over mock channel
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.Base);
        
        //namespace
        BaseNamespace baseResponseMessage = BaseNamespace.parseFrom(namespace);
        assertThat(baseResponseMessage.getType(), is(BaseMessageType.ControllerInfo));
        
        //specific message
        assertThat(baseResponseMessage.getData(), is(systemInfoData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createBaseMessageResponse(request, BaseMessageType.ControllerInfo, null);

        //request to send over mock channel
        namespace = confirmResponseWrapperQueue(response, Namespace.Base);
        
        //namespace
        baseResponseMessage = BaseNamespace.parseFrom(namespace);
        assertThat(baseResponseMessage.getType(), is(BaseMessageType.ControllerInfo));
        
        //specific message
        assertThat(baseResponseMessage.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected mission programming contents.
     */
    @Test
    public void testSendMissionNamespace() throws IOException
    {
        // construct data message to send
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MessageWrapper request = 
            m_SUT.createMissionProgrammingMessage(MissionProgrammingMessageType.GetTemplatesRequest, getTemplateData);
        
        //request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.MissionProgramming, 1);
        
        // verify namespace message
        MissionProgrammingNamespace mpMessageResponse = 
                MissionProgrammingNamespace.parseFrom(namespace);
        assertThat(mpMessageResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesRequest));
        
        //verify data in the message
        GetTemplatesResponseData mpDataResponse = GetTemplatesResponseData.parseFrom(mpMessageResponse.getData());
        MissionProgramTemplate mpTemplateData = mpDataResponse.getTemplate(0);
        assertThat(mpTemplateData, is(template));
        
        //--send again with null for the data message--//
        request = m_SUT.createMissionProgrammingMessage(MissionProgrammingMessageType.GetTemplatesRequest, null);

        // request sending
        namespace = confirmWrapperTrySend(request, Namespace.MissionProgramming, 2);
       
        // verify namespace message
        mpMessageResponse = MissionProgrammingNamespace.parseFrom(namespace);
        assertThat(mpMessageResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesRequest));
        
        assertThat(mpMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected mission programming contents.
     */
    @Test
    public void testQueueMissionResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
                
        // construct data message to send
        MissionProgramTemplate template = MissionProgramTemplate.newBuilder().setName("test").setSource("src").build();
        GetTemplatesResponseData getTemplateData = GetTemplatesResponseData.newBuilder().addTemplate(template).build();
        
        MessageResponseWrapper response = 
            m_SUT.createMissionProgrammingResponseMessage(request, MissionProgrammingMessageType.GetTemplatesResponse, 
                    getTemplateData);

        //request to send over mock channel
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.MissionProgramming);
        
        // verify namespace message
        MissionProgrammingNamespace messageResponse = MissionProgrammingNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesResponse));

        //verify data message
        assertThat(messageResponse.getData(), is(getTemplateData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createMissionProgrammingResponseMessage(request,
                MissionProgrammingMessageType.GetTemplatesResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.MissionProgramming);
        
        // verify namespace message
        messageResponse = MissionProgrammingNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesResponse));

        // verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected event admin contents.
     */
    @Test
    public void testSendEventNamespace() throws IOException
    {
        // construct data message to send
        SendEventData sendEventData = SendEventData.newBuilder().setTopic("test").build();
        
        MessageWrapper request = 
                m_SUT.createEventAdminMessage(EventAdminMessageType.SendEvent, sendEventData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.EventAdmin, 1);

        // verify namespace message
        EventAdminNamespace eventAdminMessageResponse = EventAdminNamespace.parseFrom(namespace);
        assertThat(eventAdminMessageResponse.getType(), is(EventAdminMessageType.SendEvent));
        
        //verify data message
        assertThat(eventAdminMessageResponse.getData(), is(sendEventData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createEventAdminMessage(EventAdminMessageType.SendEvent, null);
            
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.EventAdmin, 2);

        // verify namespace message
        eventAdminMessageResponse = EventAdminNamespace.parseFrom(namespace);
        assertThat(eventAdminMessageResponse.getType(), is(EventAdminMessageType.SendEvent));
        
        //verify data message
        assertThat(eventAdminMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected event admin contents.
     */
    @Test
    public void testQueueEventResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        SendEventData messageData = SendEventData.newBuilder().setTopic("test").build();
        
        MessageResponseWrapper response = 
                m_SUT.createEventAdminResponseMessage(request, EventAdminMessageType.SendEvent, messageData);

        // request to send over mock channel
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.EventAdmin);
        
        // verify namespace message
        EventAdminNamespace eventAdminMessageResponse = EventAdminNamespace.parseFrom(namespace);
        assertThat(eventAdminMessageResponse.getType(), is(EventAdminMessageType.SendEvent));
        
        //verify data message
        assertThat(eventAdminMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createEventAdminResponseMessage(request, EventAdminMessageType.SendEvent, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.EventAdmin);
        
        // verify namespace message
        eventAdminMessageResponse = EventAdminNamespace.parseFrom(namespace);
        assertThat(eventAdminMessageResponse.getType(), is(EventAdminMessageType.SendEvent));
        
        //verify data message
        assertThat(eventAdminMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected config admin contents.
     */
    @Test
    public void testSendConfigAdminNamespace() throws IOException
    {
        // construct data message to send
        GetConfigurationInfoRequestData getConfigInfoData = 
                GetConfigurationInfoRequestData.newBuilder().setFilter("test").setIncludeProperties(false).build();
        
        MessageWrapper request = 
                m_SUT.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, getConfigInfoData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.ConfigAdmin, 1);

        // verify namespace message
        ConfigAdminNamespace configAdminMessageResponse = 
                ConfigAdminNamespace.parseFrom(namespace);
        assertThat(configAdminMessageResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoRequest));
        
        //verify data message
        assertThat(configAdminMessageResponse.getData(), is(getConfigInfoData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, null);
            
        // request sending
        namespace = confirmWrapperTrySend(request, Namespace.ConfigAdmin, 2);

        // verify namespace message
        configAdminMessageResponse = ConfigAdminNamespace.parseFrom(namespace);
        assertThat(configAdminMessageResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoRequest));
        
        //verify data message
        assertThat(configAdminMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected config admin contents.
     */
    @Test
    public void testQueueConfigAdminResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        GetConfigurationInfoRequestData messageData = 
                GetConfigurationInfoRequestData.newBuilder().setFilter("test").setIncludeProperties(false).build();
        
        MessageResponseWrapper response = 
                m_SUT.createConfigAdminResponseMessage(request, ConfigAdminMessageType.GetConfigurationInfoRequest, 
                        messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.ConfigAdmin);
        
        // verify namespace message
        ConfigAdminNamespace messageResponse = ConfigAdminNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoRequest));
        
        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createConfigAdminResponseMessage(request, ConfigAdminMessageType.GetConfigurationInfoRequest, 
                        null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.ConfigAdmin);
      
        // verify namespace message
        messageResponse = ConfigAdminNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoRequest));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected metatype contents.
     */
    @Test
    public void testSendMetatypeNamespace() throws IOException
    {
        // construct data message to send
        GetAttributeDefinitionRequestData getAttrDefData = 
                GetAttributeDefinitionRequestData.newBuilder().setBundleId(1).setPid("test-pid").build();
        
        MessageWrapper request = 
                m_SUT.createMetaTypeMessage(MetaTypeMessageType.GetAttributeDefinitionRequest, getAttrDefData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.MetaType, 1);

        // verify namespace message
        MetaTypeNamespace metaTypeMessageResponse = MetaTypeNamespace.parseFrom(namespace);
        assertThat(metaTypeMessageResponse.getType(), is(MetaTypeMessageType.GetAttributeDefinitionRequest));
        
        //verify data message
        assertThat(metaTypeMessageResponse.getData(), is(getAttrDefData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createMetaTypeMessage(MetaTypeMessageType.GetAttributeDefinitionRequest, null);
            
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.MetaType, 2);

        // verify namespace message
        metaTypeMessageResponse = MetaTypeNamespace.parseFrom(namespace);
        assertThat(metaTypeMessageResponse.getType(), is(MetaTypeMessageType.GetAttributeDefinitionRequest));
        
        //verify data message
        assertThat(metaTypeMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected metatype contents.
     */
    @Test
    public void testQueueMetaTypeResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        GetAttributeDefinitionRequestData messageData = 
                GetAttributeDefinitionRequestData.newBuilder().setBundleId(1).setPid("test-pid").build();
        
        MessageResponseWrapper response = 
                m_SUT.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetAttributeDefinitionResponse, 
                        messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.MetaType);
        
        // verify namespace message
        MetaTypeNamespace messageResponse = MetaTypeNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(MetaTypeMessageType.GetAttributeDefinitionResponse));
        
        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetAttributeDefinitionResponse, 
                        null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.MetaType);

        // verify namespace message
        messageResponse = MetaTypeNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(MetaTypeMessageType.GetAttributeDefinitionResponse));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected asset namespace contents.
     */
    @Test
    public void testSendAssetNamespace() throws IOException
    {
        // construct data message to send
        UUID uuid = UUID.newBuilder().setLeastSignificantBits(1L).setMostSignificantBits(2L).build();
        CaptureDataRequestData messageData = 
                CaptureDataRequestData.newBuilder().setUuid(uuid).build();
        
        MessageWrapper request = 
                m_SUT.createAssetMessage(AssetMessageType.CaptureDataResponse, messageData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.Asset, 1);

        // verify namespace message
        AssetNamespace messageResponse = AssetNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetMessageType.CaptureDataResponse));
        
        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createAssetMessage(AssetMessageType.CaptureDataResponse, null);
            
        // request sending
        namespace = confirmWrapperTrySend(request, Namespace.Asset, 2);

        // verify namespace message
        messageResponse = AssetNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetMessageType.CaptureDataResponse));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected asset namespace contents.
     */
    @Test
    public void testQueueAssetResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        UUID uuid = UUID.newBuilder().setLeastSignificantBits(1L).setMostSignificantBits(2L).build();
        CaptureDataRequestData messageData = 
                CaptureDataRequestData.newBuilder().setUuid(uuid).build();
        
        MessageResponseWrapper response = 
                m_SUT.createAssetResponseMessage(request, AssetMessageType.CaptureDataResponse, messageData);

        // request to send over mock channel
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.Asset);
       
        // verify namespace message
        AssetNamespace messageResponse = AssetNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetMessageType.CaptureDataResponse));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createAssetResponseMessage(request, AssetMessageType.CaptureDataResponse, null);

        // request to send over mock channel
        namespace = confirmResponseWrapperQueue(response, Namespace.Asset);

        // verify namespace message
        messageResponse = AssetNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetMessageType.CaptureDataResponse));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected custom comms contents.
     */
    @Test
    public void testSendCustomCommsServiceNamespace() throws IOException
    {
        // construct data message to send
        CreatePhysicalLinkRequestData messageData = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT)
                .setPhysicalLinkName("space")
                .build();
        
        MessageWrapper request = 
                m_SUT.createCustomCommsMessage(CustomCommsMessageType.CreatePhysicalLinkRequest, messageData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.CustomComms, 1);

        // verify namespace message
        CustomCommsNamespace messageResponse = CustomCommsNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(CustomCommsMessageType.CreatePhysicalLinkRequest));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createCustomCommsMessage(CustomCommsMessageType.CreatePhysicalLinkRequest, null);
            
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.CustomComms, 2);

        // verify namespace message
        messageResponse = CustomCommsNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(CustomCommsMessageType.CreatePhysicalLinkRequest));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected custom comms contents.
     */
    @Test
    public void testQueueCustomCommsServiceResponse() throws InvalidProtocolBufferException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder()
                .setPid("aba")
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(java.util.UUID.randomUUID()))
                .setProductType("testPhysicalLink")
                .build();
        
        // construct data message to send
        CreatePhysicalLinkResponseData messageData = CreatePhysicalLinkResponseData.newBuilder()
                .setInfo(info)
                .build();

        MessageResponseWrapper response = 
                m_SUT.createCustomCommsResponseMessage(request, CustomCommsMessageType.CreatePhysicalLinkResponse, 
                        messageData);

        // request to send over mock channel
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.CustomComms);

        // verify namespace message
        CustomCommsNamespace commsResponse = CustomCommsNamespace.parseFrom(namespace);
        assertThat(commsResponse.getType(), is(CustomCommsMessageType.CreatePhysicalLinkResponse));

        //verify data message
        assertThat(commsResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createCustomCommsResponseMessage(request, CustomCommsMessageType.CreatePhysicalLinkResponse, 
                        null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.CustomComms);
      
        // verify namespace message
        commsResponse = CustomCommsNamespace.parseFrom(namespace);
        assertThat(commsResponse.getType(), is(CustomCommsMessageType.CreatePhysicalLinkResponse));

        //verify data message
        assertThat(commsResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected link layer contents.
     */
    @Test
    public void testSendLinkLayerNamespace() throws IOException
    {
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        
        // construct data message to send
        IsActivatedRequestData messageData = IsActivatedRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        MessageWrapper request = m_SUT.createLinkLayerMessage(LinkLayerMessageType.IsActivatedRequest, messageData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.LinkLayer, 1);

        // verify namespace message
        LinkLayerNamespace messageResponse = LinkLayerNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(LinkLayerMessageType.IsActivatedRequest));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createLinkLayerMessage(LinkLayerMessageType.IsActivatedRequest, null);
            
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.LinkLayer, 2);

        // verify namespace message
        messageResponse = LinkLayerNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(LinkLayerMessageType.IsActivatedRequest));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected link layer contents.
     */
    @Test
    public void testQueueLinkLayerMessageResponse() throws InvalidProtocolBufferException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        
        // construct data message to send
        IsActivatedRequestData messageData = IsActivatedRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        MessageResponseWrapper response = m_SUT.createLinkLayerResponseMessage(request, 
                LinkLayerMessageType.IsActivatedResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.LinkLayer);

        // verify namespace message
        LinkLayerNamespace linkResponse = LinkLayerNamespace.parseFrom(namespace);
        assertThat(linkResponse.getType(), is(LinkLayerMessageType.IsActivatedResponse));

        //verify data message
        assertThat(linkResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createLinkLayerResponseMessage(request, LinkLayerMessageType.IsActivatedResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.LinkLayer);
        // verify namespace message
        linkResponse = LinkLayerNamespace.parseFrom(namespace);
        assertThat(linkResponse.getType(), is(LinkLayerMessageType.IsActivatedResponse));
        
        //verify data message
        assertThat(linkResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected physical link contents.
     */
    @Test
    public void testSendPhysicalLinkNamespace() throws IOException
    {
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        
        // construct data message to send
        IsOpenRequestData messageData = IsOpenRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        MessageWrapper request = m_SUT.createPhysicalLinkMessage(PhysicalLinkMessageType.IsOpenRequest, messageData);
            
        // request sending and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.PhysicalLink, 1);

        // verify namespace message
        PhysicalLinkNamespace messageResponse = PhysicalLinkNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(PhysicalLinkMessageType.IsOpenRequest));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createPhysicalLinkMessage(PhysicalLinkMessageType.IsOpenRequest, null);
        
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.PhysicalLink, 2);

        // verify namespace message
        messageResponse = PhysicalLinkNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(PhysicalLinkMessageType.IsOpenRequest));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected physical link contents.
     */
    @Test
    public void testQueuePhysicalLinkMessageResponse() throws InvalidProtocolBufferException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        IsOpenResponseData messageData = IsOpenResponseData.newBuilder().setIsOpen(true).build();
        
        MessageResponseWrapper response = m_SUT.createPhysicalLinkResponseMessage(request, 
                PhysicalLinkMessageType.IsOpenResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.PhysicalLink);

        // verify namespace message
        PhysicalLinkNamespace physicalResponse = PhysicalLinkNamespace.parseFrom(namespace);
        assertThat(physicalResponse.getType(), is(PhysicalLinkMessageType.IsOpenResponse));

        //verify data message
        assertThat(physicalResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createPhysicalLinkResponseMessage(request, PhysicalLinkMessageType.IsOpenResponse, 
                null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.PhysicalLink);

        // verify namespace message
        physicalResponse = PhysicalLinkNamespace.parseFrom(namespace);
        assertThat(physicalResponse.getType(), is(PhysicalLinkMessageType.IsOpenResponse));
        
        //verify data message
        assertThat(physicalResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected transport layer contents.
     */
    @Test
    public void testSendTransportLayerNamespace() throws IOException
    {
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        
        // construct data message to send
        IsReceivingRequestData messageData = IsReceivingRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        MessageWrapper request = m_SUT.createTransportLayerMessage(TransportLayerMessageType.IsReceivingRequest, 
                messageData);
        
        // request sending
        ByteString namespace = confirmWrapperTrySend(request, Namespace.TransportLayer, 1);

        // verify namespace message
        TransportLayerNamespace messageResponse = 
                TransportLayerNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(TransportLayerMessageType.IsReceivingRequest));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createTransportLayerMessage(TransportLayerMessageType.IsReceivingRequest, null);
        
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.TransportLayer, 2);

        // verify namespace message
        messageResponse = TransportLayerNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(TransportLayerMessageType.IsReceivingRequest));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected transport layer contents.
     */
    @Test
    public void testQueueTransportLayerMessageResponse() throws InvalidProtocolBufferException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        
        // construct data message to send
        IsReceivingRequestData messageData = IsReceivingRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        MessageResponseWrapper response = m_SUT.createTransportLayerResponseMessage(request, 
                TransportLayerMessageType.IsReceivingResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.TransportLayer);

        // verify namespace message
        TransportLayerNamespace transportResponse = TransportLayerNamespace.parseFrom(namespace);
        assertThat(transportResponse.getType(), is(TransportLayerMessageType.IsReceivingResponse));

        //verify data message
        assertThat(transportResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createTransportLayerResponseMessage(request,TransportLayerMessageType.IsReceivingResponse, 
                null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.TransportLayer);

        // verify namespace message
        transportResponse = TransportLayerNamespace.parseFrom(namespace);
        assertThat(transportResponse.getType(), is(TransportLayerMessageType.IsReceivingResponse));
        
        //verify data message
        assertThat(transportResponse.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected asset directory service contents.
     */
    @Test
    public void testSendAssetDirectoryServiceNamespace() throws IOException
    {
        // construct data message to send
        GetCapabilitiesRequestData messageData = GetCapabilitiesRequestData.newBuilder().setProductType("blah blah").
                build();
        
        MessageWrapper request = m_SUT.createAssetDirectoryServiceMessage(
                AssetDirectoryServiceMessageType.GetCapabilitiesRequest, 
                messageData);
        
        // request sending, and verify namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.AssetDirectoryService, 1);

        // verify namespace message
        AssetDirectoryServiceNamespace adsMessageResponse = AssetDirectoryServiceNamespace.parseFrom(namespace);
        assertThat(adsMessageResponse.getType(), is(AssetDirectoryServiceMessageType.GetCapabilitiesRequest));

        //verify data message
        assertThat(adsMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetCapabilitiesRequest, 
                null);
        
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.AssetDirectoryService, 2);

        // verify namespace message
        adsMessageResponse = AssetDirectoryServiceNamespace.parseFrom(namespace);
        assertThat(adsMessageResponse.getType(), is(AssetDirectoryServiceMessageType.GetCapabilitiesRequest));
        
        //verify data message
        assertThat(adsMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected asset directory service contents.
     */
    @Test
    public void testQueueAssetDirectoryServiceResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        GetCapabilitiesResponseData messageData = GetCapabilitiesResponseData.newBuilder().setProductType("a")
                .setCapabilities(AssetCapabilities.newBuilder()
                        .setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder()
                            .setProductName("name")
                            .setDescription("desc"))
                        .build())
                .build();
        
        MessageResponseWrapper response = m_SUT.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetCapabilitiesResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.AssetDirectoryService);

        // verify namespace message
        AssetDirectoryServiceNamespace messageResponse = AssetDirectoryServiceNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetDirectoryServiceMessageType.GetCapabilitiesResponse));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetCapabilitiesResponse, null);

        // request to send over mock channel
        namespace = confirmResponseWrapperQueue(response, Namespace.AssetDirectoryService);

        // verify namespace message
        messageResponse = AssetDirectoryServiceNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(AssetDirectoryServiceMessageType.GetCapabilitiesResponse));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected bundle contents.
     */
    @Test
    public void testSendBundleMessage() throws IOException
    {
        // construct data message to send
        StartRequestData messageData = StartRequestData.newBuilder().setBundleId(1L).build();
        
        MessageWrapper request = m_SUT.createBundleMessage(BundleMessageType.StartRequest, messageData);
        
        // request sending
        ByteString namespace = confirmWrapperTrySend(request, Namespace.Bundle, 1);

        // verify namespace message
        BundleNamespace bunRequest = BundleNamespace.parseFrom(namespace);
        assertThat(bunRequest.getType(), is(BundleMessageType.StartRequest));

        //verify data message
        assertThat(bunRequest.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createBundleMessage(BundleMessageType.StartRequest, null);
        
        // request sending
        namespace = confirmWrapperTrySend(request, Namespace.Bundle, 2);

        // verify namespace message
        bunRequest = BundleNamespace.parseFrom(namespace);
        assertThat(bunRequest.getType(), is(BundleMessageType.StartRequest));
        
        //verify data message
        assertThat(bunRequest.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected bundle contents.
     */
    @Test
    public void testQueueBundleResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        GetBundleInfoRequestData messageData = GetBundleInfoRequestData.newBuilder().setBundleId(1L).build();

        MessageResponseWrapper response = m_SUT.createBundleResponseMessage(request, 
                BundleMessageType.GetBundleInfoResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.Bundle);

        // verify namespace message
        BundleNamespace messageResponse = BundleNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(BundleMessageType.GetBundleInfoResponse));

        //verify data message
        assertThat(messageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createBundleResponseMessage(request, 
                BundleMessageType.GetBundleInfoResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.Bundle);

        // verify namespace message
        messageResponse = BundleNamespace.parseFrom(namespace);
        assertThat(messageResponse.getType(), is(BundleMessageType.GetBundleInfoResponse));
        
        //verify data message
        assertThat(messageResponse.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected observation store contents.
     */
    @Test
    public void testTrySendObservationStoreMessage() throws IOException
    {
        // construct data message to send
        java.util.UUID uuid =java.util.UUID.randomUUID();
        FindObservationByUUIDRequestData messageData = FindObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        
        MessageWrapper request = m_SUT.createObservationStoreMessage(
                ObservationStoreMessageType.FindObservationByUUIDRequest, messageData);
        
        // request sending, and confirm namespace
        ByteString namespace = confirmWrapperTrySend(request, Namespace.ObservationStore, 1);

        // verify namespace message
        ObservationStoreNamespace obsMessageResponse = ObservationStoreNamespace.parseFrom(namespace);
        assertThat(obsMessageResponse.getType(), is(ObservationStoreMessageType.FindObservationByUUIDRequest));

        //verify data message
        assertThat(obsMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createObservationStoreMessage(ObservationStoreMessageType.FindObservationByUUIDRequest, null);
        
        // request sending and verify namespace
        namespace = confirmWrapperTrySend(request, Namespace.ObservationStore, 2);

        // verify namespace message
        obsMessageResponse = ObservationStoreNamespace.parseFrom(namespace);
        assertThat(obsMessageResponse.getType(), is(ObservationStoreMessageType.FindObservationByUUIDRequest));
        
        //verify data message
        assertThat(obsMessageResponse.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected observation store contents.
     */
    @Test
    public void testQueueObservationStoreMessageResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();

        // construct data message to send
        GetBundleInfoRequestData messageData = GetBundleInfoRequestData.newBuilder().setBundleId(1L).build();

        MessageResponseWrapper response = m_SUT.createObservationStoreResponseMessage(request, 
                ObservationStoreMessageType.FindObservationByUUIDResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.ObservationStore);

        // verify namespace message
        ObservationStoreNamespace obsMessageResponse = ObservationStoreNamespace.parseFrom(namespace);
        assertThat(obsMessageResponse.getType(), is(ObservationStoreMessageType.FindObservationByUUIDResponse));

        //verify data message
        assertThat(obsMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createObservationStoreResponseMessage(request, 
                ObservationStoreMessageType.FindObservationByUUIDResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.ObservationStore);

        // verify namespace message
        obsMessageResponse = ObservationStoreNamespace.parseFrom(namespace);
        assertThat(obsMessageResponse.getType(), is(ObservationStoreMessageType.FindObservationByUUIDResponse));
        
        //verify data message is not set
        assertThat(obsMessageResponse.hasData(), is(false));
    }

    /**
     * Verify the message wrapper returned contains the expected remote channel lookup contents.
     */
    @Test
    public void testSendRemoteChannelLookupMessage() throws IOException
    {
        // construct data message to send
        SyncTransportChannelResponseData messageData = SyncTransportChannelResponseData.newBuilder().
                setRemoteSystemAddress("remote").
                setRemoteSystemId(99).
                setSourceSystemAddress("dest").
                setTransportLayerName("trans-name").build();
        
        MessageWrapper request = m_SUT.createRemoteChannelLookupMessage(
                RemoteChannelLookupMessageType.SyncTransportChannelResponse, messageData);
        
        // request sending, and verify namespace
        ByteString namespaceMessage = confirmWrapperTrySend(request, Namespace.RemoteChannelLookup, 1);
        
        // verify namespace message
        RemoteChannelLookupNamespace nameMessageResponse = RemoteChannelLookupNamespace.parseFrom(namespaceMessage);
        assertThat(nameMessageResponse.getType(), is(RemoteChannelLookupMessageType.SyncTransportChannelResponse));

        //verify data message
        assertThat(nameMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createRemoteChannelLookupMessage(RemoteChannelLookupMessageType.SyncTransportChannelResponse, 
                null);
        
        // request sending and verify namespace
        namespaceMessage = confirmWrapperTrySend(request, Namespace.RemoteChannelLookup, 2);

        // verify namespace message
        nameMessageResponse = RemoteChannelLookupNamespace.parseFrom(namespaceMessage);
        assertThat(nameMessageResponse.getType(), is(RemoteChannelLookupMessageType.SyncTransportChannelResponse));
        
        //verify data message
        assertThat(nameMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected remote channel lookup contents.
     */
    @Test
    public void testQueueRemoteChannelLookupMessageResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        // construct data message to send
        SyncTransportChannelResponseData messageData = SyncTransportChannelResponseData.newBuilder().
                setRemoteSystemAddress("remote").
                setRemoteSystemId(99).
                setSourceSystemAddress("dest").
                setTransportLayerName("trans-name").build();

        MessageResponseWrapper response = m_SUT.createRemoteChannelLookupResponseMessage(request, 
                RemoteChannelLookupMessageType.SyncTransportChannelResponse, messageData);

        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.RemoteChannelLookup);
        
        // verify namespace message
        RemoteChannelLookupNamespace nameMessageResponse = 
                RemoteChannelLookupNamespace.parseFrom(namespace);
        assertThat(nameMessageResponse.getType(), is(RemoteChannelLookupMessageType.SyncTransportChannelResponse));

        //verify data message
        assertThat(nameMessageResponse.getData(), is(messageData.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createRemoteChannelLookupResponseMessage(request, 
                RemoteChannelLookupMessageType.SyncTransportChannelResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.RemoteChannelLookup);

        // verify namespace message
        nameMessageResponse = 
                RemoteChannelLookupNamespace.parseFrom(namespace);
        assertThat(nameMessageResponse.getType(), is(RemoteChannelLookupMessageType.SyncTransportChannelResponse));
        
        //verify data message is not set
        assertThat(nameMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected encryption information contents.
     */
    @Test
    public void testQueueEncryptionInfoMessage() throws IOException
    {
        byte[] fakeBytes = {10, 10, 20, 30};
        ByteString fakeByteString = ByteString.copyFrom(fakeBytes);
        Message fakeMessage = mock(Message.class);
        
        when(fakeMessage.toByteString()).thenReturn(fakeByteString);
        
        MessageWrapperAutoEncryption request = m_SUT.createEncryptionInfoMessage(
                EncryptionInfoMessageType.GetEncryptionTypeRequest, fakeMessage);
        
        // request sending and verify namespace
        ByteString namespaceMessage = confirmWrapperNoEncryptionTrySend(request, Namespace.EncryptionInfo, 1);
        
        // verify namespace message
        EncryptionInfoNamespace nameMessageResponse = EncryptionInfoNamespace.parseFrom(namespaceMessage);
        assertThat(nameMessageResponse.getType(), is(EncryptionInfoMessageType.GetEncryptionTypeRequest));
        
        //verify data message
        assertThat(nameMessageResponse.getData(), is(fakeMessage.toByteString()));
        
        //--send again with null for the data message--//
        request = m_SUT.createEncryptionInfoMessage(EncryptionInfoMessageType.GetEncryptionTypeRequest, 
                null);
        
        // request sending and verify namespace
        namespaceMessage = confirmWrapperNoEncryptionTrySend(request, Namespace.EncryptionInfo, 2);

        // verify namespace message
        nameMessageResponse = EncryptionInfoNamespace.parseFrom(namespaceMessage);
        assertThat(nameMessageResponse.getType(), is(EncryptionInfoMessageType.GetEncryptionTypeRequest));
        
        //verify data message
        assertThat(nameMessageResponse.hasData(), is(false));
    }
    
    /**
     * Verify the message wrapper returned contains the expected encryption information contents.
     */
    @Test
    public void testQueueEncryptionInfoMessageResponse() throws IOException
    {
        // construct fake request message
        TerraHarvestMessage request = createRequestFiller();
        
        GetEncryptionTypeResponseData dataMessage = 
                GetEncryptionTypeResponseData.newBuilder().setType(EncryptType.AES_ECDH_ECDSA).build();
        
        MessageResponseWrapper response = m_SUT.createEncryptionInfoResponseMessage(request, 
                EncryptionInfoMessageType.GetEncryptionTypeResponse, dataMessage);
        
        // request to send over mock channel and verify namespace
        ByteString namespace = confirmResponseWrapperQueue(response, Namespace.EncryptionInfo);
        
        // verify namespace message
        EncryptionInfoNamespace nameMessageResponse = EncryptionInfoNamespace.parseFrom(namespace);
        assertThat(nameMessageResponse.getType(), is(EncryptionInfoMessageType.GetEncryptionTypeResponse));

        //verify data message
        assertThat(nameMessageResponse.getData(), is(dataMessage.toByteString()));
        
        //--send again with no data message--//
        response = m_SUT.createEncryptionInfoResponseMessage(request, 
                EncryptionInfoMessageType.GetEncryptionTypeResponse, null);

        // request to send over mock channel and verify namespace
        namespace = confirmResponseWrapperQueue(response, Namespace.EncryptionInfo);

        // verify namespace message
        nameMessageResponse = 
                EncryptionInfoNamespace.parseFrom(namespace);
        assertThat(nameMessageResponse.getType(), is(EncryptionInfoMessageType.GetEncryptionTypeResponse));
        
        //verify data message is not set
        assertThat(nameMessageResponse.hasData(), is(false));
    }
    
    /**
     * Create a Terra Harvest Message to use for the request message for responses.
     */
    private TerraHarvestMessage createRequestFiller()
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
        return TerraHarvestMessageUtil.getPartialMessage()
                .setTerraHarvestPayload(payload.toByteString())
                .setSourceId(m_RemoteId)
                .setDestId(m_LocalId)
                .setMessageId(messageId).build();
    }
    
    /**
     * Confirm that the wrapper passed in can 'trySend' a message. Verifies the expected namespace of the intercepted
     * payload message.
     * @param wrapper
     *     the message wrapper that contains the payload to send
     * @param namespace
     *     the expected namespace
     * @param times
     *     the number of times the message sender will have executed the 'trySend' call for the single test method
     * @return
     *     the bytestring data containing the namespace message
     */
    private ByteString confirmWrapperTrySend(final MessageWrapper wrapper, final Namespace namespace, final int times)
    {
        // request sending
        wrapper.trySend(m_RemoteId, EncryptType.NONE);

        // capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender, times(times)).trySendMessage(eq(m_RemoteId), payloadCap.capture(), 
                eq(EncryptType.NONE));

        // verify Payload
        TerraHarvestPayload payloadCaptured = payloadCap.getValue();
        assertThat(payloadCaptured.getNamespace(), is(namespace));
        
        return payloadCaptured.getNamespaceMessage();
    }
    
    /**
     * Confirm that the wrapper no encryption passed in can 'trySend' a message. Verifies the expected namespace of the 
     * intercepted payload message.
     * @param wrapper
     *     the message wrapper that contains the payload to send
     * @param namespace
     *     the expected namespace
     * @param times
     *     the number of times the message sender will have executed the 'trySend' call for the single test method
     * @return
     *     the bytestring data containing the namespace message
     */
    private ByteString confirmWrapperNoEncryptionTrySend(final MessageWrapperAutoEncryption wrapper, 
            final Namespace namespace, final int times)
    {
        // request sending
        wrapper.trySend(m_RemoteId);

        // capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender, times(times)).trySendMessage(eq(m_RemoteId), payloadCap.capture());

        // verify Payload
        TerraHarvestPayload payloadCaptured = payloadCap.getValue();
        assertThat(payloadCaptured.getNamespace(), is(namespace));
        
        return payloadCaptured.getNamespaceMessage();
    }
    
    /**
     * Confirm that the response wrapper passed in can 'queue' a message. Verifies the expected namespace of the 
     * intercepted payload message.
     * @param wrapper
     *     the message wrapper that contains the payload to send
     * @param namespace
     *     the expected namespace
     * @return
     *     the bytestring data containing the namespace message
     */
    private ByteString confirmResponseWrapperQueue(final MessageResponseWrapper wrapper, final Namespace namespace)
    {
        // request to send over a mock channel
        RemoteChannel channel = mock(RemoteChannel.class);
        wrapper.queue(channel);

        // capture the payload constructed in the message factory
        ArgumentCaptor<TerraHarvestPayload> payloadCap = ArgumentCaptor.forClass(TerraHarvestPayload.class);
        verify(m_MessageSender).queueMessageResponse(eq(createRequestFiller()), payloadCap.capture(), 
                eq(channel));

        // verify Payload
        TerraHarvestPayload payloadCaptured = payloadCap.getValue();
        assertThat(payloadCaptured.getNamespace(), is(namespace));
        
        return payloadCaptured.getNamespaceMessage();
    }
}
