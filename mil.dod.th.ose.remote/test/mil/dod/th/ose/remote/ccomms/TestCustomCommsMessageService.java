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
package mil.dod.th.ose.remote.ccomms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.UnmarshalException;

import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.SetLayerNameRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.comms.CustomCommsMessageService;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.FactoryObjectMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.link.capability.LinkLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.physical.capability.PhysicalLinkCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.transport.capability.TransportLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * Testing for the custom comms message service, which includes testing to make sure all requests and responses
 * are sending correctly for all custom comms message types.
 * @author matt
 */
public class TestCustomCommsMessageService
{
    private CustomCommsMessageService m_SUT;
    
    private LoggingService m_Logging;
    private mil.dod.th.core.ccomm.CustomCommsService m_CustomCommsService;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private final UUID testUuid = UUID.randomUUID();
    private final UUID testUuid2 = UUID.randomUUID();
    private final String testLinkLayerName = "LinkTest";
    private MessageRouterInternal m_MessageRouter;
    private JaxbProtoObjectConverter m_Converter;
    private MessageResponseWrapper m_ResponseWrapper;
    
    private LinkLayerFactory m_LinkLayerFactory;
    private TransportLayerFactory m_TransportLayerFactory;
    private PhysicalLinkFactory m_PhysicalLinkFactory;
    
    private final Set<LinkLayerFactory> m_LinkLayerFactories = new HashSet<>(); 
    private final Set<TransportLayerFactory> m_TransportLayerFactories =  new HashSet<>(); 
    private final Set<PhysicalLinkFactory> m_PhysicalLinkFactories = new HashSet<>(); 
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new CustomCommsMessageService();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_CustomCommsService = mock(mil.dod.th.core.ccomm.CustomCommsService.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        
        m_LinkLayerFactory = mock(LinkLayerFactory.class);
        doReturn(LinkLayer.class.getName()).when(m_LinkLayerFactory).getProductType();
        m_LinkLayerFactories.add(m_LinkLayerFactory);
        when(m_CustomCommsService.getLinkLayerFactories()).thenReturn(m_LinkLayerFactories);
        
        m_TransportLayerFactory = mock(TransportLayerFactory.class);
        doReturn(TransportLayer.class.getName()).when(m_TransportLayerFactory).getProductType();        
        m_TransportLayerFactories.add(m_TransportLayerFactory); 
        when(m_CustomCommsService.getTransportLayerFactories()).thenReturn(m_TransportLayerFactories);
        
        m_PhysicalLinkFactory = mock(PhysicalLinkFactory.class);
        doReturn(PhysicalLink.class.getName()).when(m_PhysicalLinkFactory).getProductType();
        m_PhysicalLinkFactories.add(m_PhysicalLinkFactory);
        when(m_CustomCommsService.getPhysicalLinkFactories()).thenReturn(m_PhysicalLinkFactories);
        
        when(m_MessageFactory.createCustomCommsResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(CustomCommsMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
        
        m_SUT.activate();
    }
    
    @After
    public void tearDown()
    {
        m_SUT.deactivate();
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {   
        // m_SUT.activate(); <- this is performed during set-up, no need to repeat.
        // verify service is bound. 
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
        
        // reactivate for other tests
        m_SUT.activate();
    }
    
    /**
     * Verify the namespace is CustomComms
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.CustomComms));
    }
    
    /**
     * Verify that the core service is requested to create a physical link, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testCreatePhysicalLink() throws IOException, CCommException
    {
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT)
                .setPhysicalLinkName("trident")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        

        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        PhysicalLink createdPhysicalLink = mock(PhysicalLink.class);
        when(createdPhysicalLink.getUuid()).thenReturn(testUuid);
        when(createdPhysicalLink.getPid()).thenReturn("physPid");
        when(createdPhysicalLink.getFactory()).thenReturn(m_PhysicalLinkFactory);
        
        when(m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, "trident"))
            .thenReturn(createdPhysicalLink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreatePhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreatePhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreatePhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CreatePhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid));
        assertThat(response.getInfo().getPid(), is("physPid"));
    }
    
    /**
     * Verify ability to create GPIO phys link
     */
    @Test
    public void testCreatePhysicalLinkGPIO() throws IOException, CCommException
    {
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.GPIO)
                .setPhysicalLinkName("trident")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        

        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        PhysicalLink createdPhysicalLink = mock(PhysicalLink.class);
        when(createdPhysicalLink.getUuid()).thenReturn(testUuid);
        when(createdPhysicalLink.getPid()).thenReturn("physPid");
        when(createdPhysicalLink.getFactory()).thenReturn(m_PhysicalLinkFactory);
        
        when(m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.GPIO, "trident"))
            .thenReturn(createdPhysicalLink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreatePhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreatePhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreatePhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CreatePhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid));
        assertThat(response.getInfo().getPid(), is("physPid"));
    }
    
    /**
     * Verify ability to create I2C phys link
     */
    @Test
    public void testCreatePhysicalLinkI2C() throws IOException, CCommException
    {
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C)
                .setPhysicalLinkName("trident")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        

        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        PhysicalLink createdPhysicalLink = mock(PhysicalLink.class);
        when(createdPhysicalLink.getUuid()).thenReturn(testUuid);
        when(createdPhysicalLink.getPid()).thenReturn("physPid");
        when(createdPhysicalLink.getFactory()).thenReturn(m_PhysicalLinkFactory);
        
        when(m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C, "trident"))
            .thenReturn(createdPhysicalLink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreatePhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreatePhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreatePhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CreatePhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid));
        assertThat(response.getInfo().getPid(), is("physPid"));
    }
    
    /**
     * Verify response message is still sent even if PID is not set for the physical link created.
     */
    @Test
    public void testCreatePhysicalLinkNoPID() throws IOException, CCommException
    {
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.GPIO)
                .setPhysicalLinkName("PhysicallyImpossible")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        

        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        final PhysicalLink createdPhysicalLink = mock(PhysicalLink.class);
        when(createdPhysicalLink.getUuid()).thenReturn(testUuid);
        when(createdPhysicalLink.getPid()).thenReturn(null);
        when(createdPhysicalLink.getFactory()).thenReturn(m_PhysicalLinkFactory);
        
        when(m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.GPIO, "PhysicallyImpossible")).
            thenReturn(createdPhysicalLink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreatePhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreatePhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreatePhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CreatePhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid));
        assertThat(response.getInfo().hasPid(), is(false));
    }
    
    /**
     * Verify event posted for request message even after a exception is thrown, verify event property key in the 
     * posted event is the correct request for the message that was sent, as well as verify that the error response 
     * message sent has the correct error code and correct arguments.
     */
    @Test
    public void testCreatePhysicalLinkExceptions() throws IOException, CCommException
    {
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder()
                .setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SPI)
                .setPhysicalLinkName("grrrr")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        final PhysicalLink createdPhysicalLink = mock(PhysicalLink.class);
        when(createdPhysicalLink.getUuid()).thenReturn(testUuid);
        when(createdPhysicalLink.getPid()).thenReturn("physPid");
        
        //----Test Custom Comms Exception----
        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW)).when(m_CustomCommsService).createPhysicalLink(
                Mockito.any(PhysicalLinkTypeEnum.class), eq("grrrr"));
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        //----Test Persistence Failed Exception----
        doThrow(new CCommException(FormatProblem.OTHER)).when(m_CustomCommsService).createPhysicalLink(
                Mockito.any(PhysicalLinkTypeEnum.class), eq("grrrr"));
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        event = eventCaptor.getValue();
        
        assertThat((CreatePhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory, times(2)).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testCreatePhysicalLinkResponse() throws IOException
    {
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setPid("physPid")
                .setProductType("testPhysicalLayer")
                .build();
        
        Message response = CreatePhysicalLinkResponseData.newBuilder()
                .setInfo(info)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreatePhysicalLinkResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Verify that the core service is requested to create a link layer, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testCreateLinkLayer() throws IOException, CCommException
    {
        CreateLinkLayerRequestData request = CreateLinkLayerRequestData.newBuilder()
                .setLinkLayerProductType("meow")
                .setLinkLayerName("woof")
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateLinkLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        LinkLayer createdLinkLayer = mock(LinkLayer.class);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, "chirp");
        when(m_CustomCommsService.createLinkLayer("meow", "woof", properties)).thenReturn(
                createdLinkLayer);
        when(createdLinkLayer.getUuid()).thenReturn(testUuid2);
        when(createdLinkLayer.getPid()).thenReturn("linkPid");
        when(createdLinkLayer.getFactory()).thenReturn(m_LinkLayerFactory);
        
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("chirp");
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreateLinkLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreateLinkLayerResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreateLinkLayerResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreateLinkLayerResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CreateLinkLayerResponseData response = messageCaptor.getValue();
 
        assertThat(response.getInfo().getUuid(), is(notNullValue()));
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid2));
        assertThat(response.getInfo().getPid(), is("linkPid"));
    }
    
    /**
     * Verify if no physical link UUID is provided, then the property is not passed to the custom comm service.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateLinkLayer_NoPhysicalLink() throws IOException, CCommException
    {
        CreateLinkLayerRequestData request = CreateLinkLayerRequestData.newBuilder()
                .setLinkLayerProductType("meow")
                .setLinkLayerName("woof")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateLinkLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        LinkLayer createdLinkLayer = mock(LinkLayer.class);
        when(m_CustomCommsService.createLinkLayer(anyString(), anyString(), Mockito.any(Map.class))).thenReturn(
                createdLinkLayer);
        when(createdLinkLayer.getUuid()).thenReturn(testUuid2);
        when(createdLinkLayer.getPid()).thenReturn("linkPid");
        when(createdLinkLayer.getFactory()).thenReturn(m_LinkLayerFactory);
        
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("chirp");
        
        m_SUT.handleMessage(message, payload, channel);
        
        Map<String, Object> properties = new HashMap<String, Object>();
        verify(m_CustomCommsService).createLinkLayer("meow", "woof", properties);
    }
    
    /**
     * Verify event posted for request message even after a exception is thrown, verify event property key in the 
     * posted event is the correct request for the message that was sent, as well as verify that the error response 
     * message sent has the correct error code and correct arguments. 
     */
    @Test
    public void testCreateLinkLayerExceptions() throws IllegalArgumentException, IllegalStateException, CCommException, 
        IOException, FactoryException
    {
        CreateLinkLayerRequestData request = CreateLinkLayerRequestData.newBuilder()
                .setLinkLayerProductType("meow")
                .setLinkLayerName("hiss")
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateLinkLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("righto");
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, "righto");

        //----Test Custom Comms Exception----
        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW)).when(m_CustomCommsService).createLinkLayer(
                eq("meow"), Mockito.anyString(), eq(properties));
        
        m_SUT.handleMessage(message, payload, channel);
        
        //verify the event contains the data message*/
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreateLinkLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testCreateLinkLayerResponse() throws IOException
    {
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setPid("linkPid")
                .setProductType("testLinkLayer")
                .build();
        
        Message response = CreateLinkLayerResponseData.newBuilder()
                .setInfo(info)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateLinkLayerResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Verify that the core service is requested to create a transport layer, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testCreateTransportLayer() throws IOException, CCommException
    {
        CreateTransportLayerRequestData request = CreateTransportLayerRequestData.newBuilder()
                .setTransportLayerProductType("rawr")
                .setTransportLayerName("chirp")
                .setLinkLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateTransportLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        final TransportLayer createdTransportLayer = mock(TransportLayer.class);
        when(createdTransportLayer.getUuid()).thenReturn(testUuid);
        when(createdTransportLayer.getPid()).thenReturn("transPid");
        when(createdTransportLayer.getFactory()).thenReturn(m_TransportLayerFactory);
        
        LinkLayer testLinkLayer = mock(LinkLayer.class);
        when(testLinkLayer.getName()).thenReturn(testLinkLayerName);
        when(testLinkLayer.getUuid()).thenReturn(testUuid);
        when(testLinkLayer.getFactory()).thenReturn(m_LinkLayerFactory);
        
        List<LinkLayer> linkList = new ArrayList<LinkLayer>();
        linkList.add(testLinkLayer);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(linkList);
        when(m_CustomCommsService.createTransportLayer("rawr", "chirp",
                        testLinkLayer.getName())).thenReturn(createdTransportLayer);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreateTransportLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<CreateTransportLayerResponseData> messageCaptor = ArgumentCaptor.forClass(
                CreateTransportLayerResponseData.class);  

        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.CreateTransportLayerResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CreateTransportLayerResponseData response = messageCaptor.getValue();
 
        assertThat(response.getInfo().getUuid(), is(notNullValue()));
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getInfo().getUuid()), is(testUuid));
        assertThat(response.getInfo().getPid(), is("transPid"));
    }
    
    /**
     * Verify if the link layer UUID field is empty, the transport layer is still created.
     */
    @Test
    public void testCreateTransportLayer_NoLinkLayer() throws IOException, CCommException
    {
        CreateTransportLayerRequestData request = CreateTransportLayerRequestData.newBuilder()
                .setTransportLayerProductType("rawr")
                .setTransportLayerName("chirp")
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateTransportLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        final TransportLayer createdTransportLayer = mock(TransportLayer.class);
        when(createdTransportLayer.getUuid()).thenReturn(testUuid);
        when(createdTransportLayer.getPid()).thenReturn("transPid");
        when(createdTransportLayer.getFactory()).thenReturn(m_TransportLayerFactory);
        
        when(m_CustomCommsService.createTransportLayer(anyString(), anyString(), anyString()))
            .thenReturn(createdTransportLayer);
        
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_CustomCommsService).createTransportLayer("rawr", "chirp", (String)null);
    }
    
    /**
     * Verify event posted for request message even after a exception is thrown, verify event property key in the 
     * posted event is the correct request for the message that was sent, as well as verify that the error response 
     * message sent has the correct error code and correct arguments.
     */
    @Test
    public void testCreateTransportLayerException() throws IOException, CCommException
    {
        CreateTransportLayerRequestData request = CreateTransportLayerRequestData.newBuilder()
                .setTransportLayerProductType("rawr")
                .setTransportLayerName("boo")
                .setLinkLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateTransportLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        LinkLayer testLinkLayer = mock(LinkLayer.class);
        when(testLinkLayer.getUuid()).thenReturn(testUuid);
        when(testLinkLayer.getName()).thenReturn(testLinkLayerName);
        
        List<LinkLayer> linkList = new ArrayList<LinkLayer>();
        linkList.add(testLinkLayer);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(linkList);
        
        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW)).when(m_CustomCommsService).createTransportLayer(
                eq("rawr"), anyString(), anyString());
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((CreateTransportLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        doThrow(new CCommException(FormatProblem.OTHER)).when(m_CustomCommsService).
            createTransportLayer(anyString(), anyString(), anyString());
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        event = eventCaptor.getValue();
        
        assertThat((CreateTransportLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory, times(2)).createBaseErrorMessage(eq(message),
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testCreateTransportLayerResponse() throws IOException
    {
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setPid("transPid")
                .setProductType("testTransportLayer")
                .build();
        
        Message response = CreateTransportLayerResponseData.newBuilder()
                .setInfo(info)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.CreateTransportLayerResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }

    /**
     * Verify that the core service is requested to get available comms types from a specified comm type, verify event 
     * posted for request message, verify event property key in the posted event is the correct request for the message 
     * that was sent, also capture argument values for the response message and make sure that the data sent is correct.
     * Make sure that each comm type is unique.
     */
    @Test
    public void testGetAvailableCommTypesLinkLayer() throws IOException, CCommException
    {
        GetAvailableCommTypesRequestData request = GetAvailableCommTypesRequestData.newBuilder()
                .setCommType(CommType.Linklayer)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetAvailableCommTypesRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock device factory behavior
        LinkLayerFactory factory = mock(LinkLayerFactory.class);
        when(factory.getProductName()).thenReturn("name");
        doReturn(LinkLayer.class.getName()).when(factory).getProductType();
        
        Set<LinkLayerFactory> factorySet = new HashSet<>();
        factorySet.add(factory);

        when(m_CustomCommsService.getLinkLayerFactories()).thenReturn(factorySet);

        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetAvailableCommTypesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetAvailableCommTypesResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetAvailableCommTypesResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetAvailableCommTypesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetAvailableCommTypesResponseData response = messageCaptor.getValue();
 
        //make sure that only 1 was added to the list, duplicates are not allowed
        assertThat(response.getProductTypeList().size(), is(1));
        assertThat(response.getProductTypeList(), hasItem(LinkLayer.class.getName()));
        assertThat(response.getCommType(), is(CommType.Linklayer));
    }
    
    /**
     * Verify that the core service is requested to get available comms types from a specified comm type, verify event 
     * posted for request message, verify event property key in the posted event is the correct request for the message 
     * that was sent, also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testGetAvailableCommTypesPhysicalLinks() throws IOException, CCommException
    {
        GetAvailableCommTypesRequestData request = GetAvailableCommTypesRequestData.newBuilder()
                .setCommType(CommType.PhysicalLink)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetAvailableCommTypesRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock device factory behavior
        PhysicalLinkFactory factory = mock(PhysicalLinkFactory.class);
        when(factory.getProductName()).thenReturn("name");
        doReturn(PhysicalLink.class.getName()).when(factory).getProductType();
        
        Set<PhysicalLinkFactory> factorySet = new HashSet<>();
        factorySet.add(factory);

        when(m_CustomCommsService.getPhysicalLinkFactories()).thenReturn(factorySet);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetAvailableCommTypesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetAvailableCommTypesResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetAvailableCommTypesResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetAvailableCommTypesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetAvailableCommTypesResponseData response = messageCaptor.getValue();
 
        assertThat(response.getProductTypeList().size(), is(1));
        assertThat(response.getProductTypeList().contains(PhysicalLink.class.getName()), is(true));
        assertThat(response.getCommType(), is(CommType.PhysicalLink));
    }
    
    /**
     * Verify that the core service is requested to get available comms types from a specified comm type, verify event 
     * posted for request message, verify event property key in the posted event is the correct request for the message 
     * that was sent, also capture argument values for the response message and make sure that the data sent is correct.
     * Make sure that each comm type is unique.
     */
    @Test
    public void testGetAvailableCommTypesTransportLayers() throws IOException, CCommException
    {
        GetAvailableCommTypesRequestData request = GetAvailableCommTypesRequestData.newBuilder()
                .setCommType(CommType.TransportLayer)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetAvailableCommTypesRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //mock device factory behavior
        TransportLayerFactory factory = mock(TransportLayerFactory.class);
        when(factory.getProductName()).thenReturn("name");
        doReturn(TransportLayer.class.getName()).when(factory).getProductType();
        
        Set<TransportLayerFactory> factorySet = new HashSet<>();
        factorySet.add(factory);

        when(m_CustomCommsService.getTransportLayerFactories()).thenReturn(factorySet);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetAvailableCommTypesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetAvailableCommTypesResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetAvailableCommTypesResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetAvailableCommTypesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetAvailableCommTypesResponseData response = messageCaptor.getValue();
 
        //make sure that only 1 was added to the list, duplicates are not allowed
        assertThat(response.getProductTypeList().size(), is(1));
        assertThat(response.getProductTypeList().contains(TransportLayer.class.getName()), 
                is(true));
        assertThat(response.getCommType(), is(CommType.TransportLayer));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetAvailableCommTypesResponse() throws IOException
    {
        Message response = GetAvailableCommTypesResponseData.newBuilder()
                .addProductType("rawr")
                .setCommType(CommType.Linklayer)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetAvailableCommTypesResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Verify that you can get all the layers known to the link layer comm type, verify event posted for request 
     * message, verify event property key in the posted event is the correct request for the message that was sent, 
     * also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testGetLayersRequestLinkLayer() throws IOException, CCommException
    {
        GetLayersRequestData request = GetLayersRequestData.newBuilder()
                .setCommType(CommType.Linklayer)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetLayersRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        LinkLayer mockLink = FactoryObjectMocker.mockFactoryObject(LinkLayer.class, "pid1");
        LinkLayer mockLink2 = FactoryObjectMocker.mockFactoryObject(LinkLayer.class, "pid2");
        
        List<LinkLayer> linkLayerList = new ArrayList<LinkLayer>();
        linkLayerList.add(mockLink);
        linkLayerList.add(mockLink2);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(linkLayerList);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetLayersRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetLayersResponseData> messageCaptor = ArgumentCaptor.forClass(GetLayersResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetLayersResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayersResponseData response = messageCaptor.getValue();
 
        assertThat(response.getLayerInfoCount(), is(2));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(mockLink.getUuid()))
                .setPid("pid1")
                .setProductType(mockLink.getFactory().getProductType())
                .build()));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(mockLink2.getUuid()))
                .setPid("pid2")
                .setProductType(mockLink2.getFactory().getProductType())
                .build()));
    }
    
    /**
     * Verify that you can get all the layers known to the physical link comm type, verify event posted for request 
     * message, verify event property key in the posted event is the correct request for the message that was sent, 
     * also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testGetLayersRequestPhysicalLinks() throws IOException, CCommException
    {
        GetLayersRequestData request = GetLayersRequestData.newBuilder()
                .setCommType(CommType.PhysicalLink)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetLayersRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        PhysicalLink plink1 = FactoryObjectMocker.mockFactoryObject(PhysicalLink.class, "pid1");
        PhysicalLink plink2 = FactoryObjectMocker.mockFactoryObject(PhysicalLink.class, "pid2");
        
        List<UUID> pLinkLayerList = new ArrayList<UUID>();
        pLinkLayerList.add(plink1.getUuid());
        pLinkLayerList.add(plink2.getUuid());
        when(m_CustomCommsService.getPhysicalLinkUuids()).thenReturn(pLinkLayerList);
        
        when(m_CustomCommsService.getPhysicalLinkPid(plink1.getUuid())).thenReturn("pid1");
        when(m_CustomCommsService.getPhysicalLinkPid(plink2.getUuid())).thenReturn("pid2");
        PhysicalLinkFactory factory1 = plink1.getFactory();
        PhysicalLinkFactory factory2 = plink2.getFactory();
        when(m_CustomCommsService.getPhysicalLinkFactory(plink1.getUuid())).thenReturn(factory1);
        when(m_CustomCommsService.getPhysicalLinkFactory(plink2.getUuid())).thenReturn(factory2);
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetLayersRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetLayersResponseData> messageCaptor = ArgumentCaptor.forClass(GetLayersResponseData.class);  

        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetLayersResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayersResponseData response = messageCaptor.getValue();
 
        assertThat(response.getLayerInfoCount(), is(2));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(plink1.getUuid()))
                .setPid("pid1")
                .setProductType(plink1.getFactory().getProductType())
                .build()));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(plink2.getUuid()))
                .setPid("pid2")
                .setProductType(plink2.getFactory().getProductType())
                .build()));
    }
    
    /**
     * Verify that you can get all the layers known to the transport layer comm type, verify event posted for request 
     * message, verify event property key in the posted event is the correct request for the message that was sent, 
     * also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testGetLayersRequestTransportLayer() throws IOException, CCommException
    {
        GetLayersRequestData request = GetLayersRequestData.newBuilder()
                .setCommType(CommType.TransportLayer)
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetLayersRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        TransportLayer mockLink = FactoryObjectMocker.mockFactoryObject(TransportLayer.class, "pid1");
        TransportLayer mockLink2 = FactoryObjectMocker.mockFactoryObject(TransportLayer.class, "pid2");
        
        List<TransportLayer> linkLayerList = new ArrayList<TransportLayer>();
        linkLayerList.add(mockLink);
        linkLayerList.add(mockLink2);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(linkLayerList);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetLayersRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetLayersResponseData> messageCaptor = ArgumentCaptor.forClass(GetLayersResponseData.class);  
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
                eq(CustomCommsMessageType.GetLayersResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayersResponseData response = messageCaptor.getValue();
 
        assertThat(response.getLayerInfoCount(), is(2));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(mockLink.getUuid()))
                .setPid("pid1")
                .setProductType(mockLink.getFactory().getProductType())
                .build()));
        assertThat(response.getLayerInfoList(), hasItem(FactoryObjectInfo.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(mockLink2.getUuid()))
                .setPid("pid2")
                .setProductType(mockLink2.getFactory().getProductType())
                .build()));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetLayersResponse() throws IOException
    {
        Message response = GetLayersResponseData.newBuilder()
                .setCommType(CommType.Linklayer)
                .addLayerInfo(
                        FactoryObjectInfo.newBuilder()
                            .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                            .setPid("test-pid")
                            .setProductType(LinkLayer.class.getName())
                            .build())
                .build();
        
        CustomCommsNamespace ccommMessage = CustomCommsNamespace.newBuilder()
                .setType(CustomCommsMessageType.GetLayersResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createCustomCommsMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }

    /**
     * Test get name for transport layers.
     * Verify that expected name is returned.
     */
    @Test
    public void testTransLayersGetLayerName() throws IOException
    {
        //mock behavior
        TransportLayer trans = mock(TransportLayer.class);
        when(trans.getName()).thenReturn("name");
        when(trans.getUuid()).thenReturn(testUuid);

        //list returned
        List<TransportLayer> layers = new ArrayList<TransportLayer>();
        layers.add(trans);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(layers);

        //request to get the name of a transport layer
        GetLayerNameRequestData request = GetLayerNameRequestData.newBuilder().
            setCommType(CommType.TransportLayer).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.GetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((GetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetLayerNameResponseData> response = ArgumentCaptor.forClass(GetLayerNameResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.GetLayerNameResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayerNameResponseData data = response.getValue();
        assertThat(data.getCommType(), is(CommType.TransportLayer));
        assertThat(data.getLayerName(), is("name"));
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }

    /**
     * Test get name for link layers.
     * Verify that expected name is returned.
     */
    @Test
    public void testLinkLayersGetLayerName() throws IOException
    {
        //mock behavior
        LinkLayer link = mock(LinkLayer.class);
        when(link.getName()).thenReturn("name");
        when(link.getUuid()).thenReturn(testUuid);

        //list returned
        List<LinkLayer> layers = new ArrayList<LinkLayer>();
        layers.add(link);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(layers);

        //request to get the name of a link layer
        GetLayerNameRequestData request = GetLayerNameRequestData.newBuilder().
            setCommType(CommType.Linklayer).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.GetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((GetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetLayerNameResponseData> response = ArgumentCaptor.forClass(GetLayerNameResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.GetLayerNameResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayerNameResponseData data = response.getValue();
        assertThat(data.getCommType(), is(CommType.Linklayer));
        assertThat(data.getLayerName(), is("name"));
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Test get name for physical link layers.
     * Verify that expected name is returned.
     */
    @Test
    public void testPhysLayersGetLayerName() throws IOException
    {
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("name");

        //request to get the name of a physical link layer
        GetLayerNameRequestData request = GetLayerNameRequestData.newBuilder().
            setCommType(CommType.PhysicalLink).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.GetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((GetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetLayerNameResponseData> response = ArgumentCaptor.forClass(GetLayerNameResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.GetLayerNameResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLayerNameResponseData data = response.getValue();
        assertThat(data.getCommType(), is(CommType.PhysicalLink));
        assertThat(data.getLayerName(), is("name"));
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }

    /**
     * Test set name for transport layers.
     * Verify that expected name is set.
     */
    @Test
    public void testTransLayerssetLayerName() throws IOException, IllegalArgumentException, FactoryException
    {
        //mock behavior
        TransportLayer trans = mock(TransportLayer.class);
        when(trans.getUuid()).thenReturn(testUuid);

        //list returned
        List<TransportLayer> layers = new ArrayList<TransportLayer>();
        layers.add(trans);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(layers);

        //request to set the name of a transport layer
        SetLayerNameRequestData request = SetLayerNameRequestData.newBuilder().
            setCommType(CommType.TransportLayer).
            setLayerName("name").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.SetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify name set
        verify(trans).setName("name");
        
        //verify
        assertThat((SetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.SetLayerNameResponse), eq((Message)null));
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test set name for link layers.
     * Verify that expected name set to the layer.
     */
    @Test
    public void testLinkLayersSetLayerName() throws IOException, IllegalArgumentException, FactoryException
    {
        //mock behavior
        LinkLayer link = mock(LinkLayer.class);
        when(link.getUuid()).thenReturn(testUuid);

        //list returned
        List<LinkLayer> layers = new ArrayList<LinkLayer>();
        layers.add(link);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(layers);

        //request to set the name of a link layer
        SetLayerNameRequestData request = SetLayerNameRequestData.newBuilder().
            setCommType(CommType.Linklayer).
            setLayerName("name").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.SetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify name set
        verify(link).setName("name");
        
        //verify
        assertThat((SetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.SetLayerNameResponse), (Message)eq(null));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test set name for physical link layers.
     * Verify that expected name is set.
     */
    @Test
    public void testPhysLayersSetLayerName() throws IOException, IllegalArgumentException, CCommException
    {
        //request to set the name of a physical link layer
        SetLayerNameRequestData request = SetLayerNameRequestData.newBuilder().
            setCommType(CommType.PhysicalLink).
            setLayerName("name").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.SetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((SetLayerNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify name set
        verify(m_CustomCommsService).setPhysicalLinkName(testUuid, "name");
        
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message), 
            eq(CustomCommsMessageType.SetLayerNameResponse), (Message)eq(null));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test set name for layers with persistence failed.
     * Verify that expected error message is sent.
     */
    @Test
    public void testSetLayerNamePersist() throws IOException, IllegalArgumentException, CCommException
    {
        doThrow(new CCommException("Exception", FormatProblem.OTHER)).
            when(m_CustomCommsService).setPhysicalLinkName(testUuid, "name");

        //request to set the name of a physical link layer
        SetLayerNameRequestData request = SetLayerNameRequestData.newBuilder().
            setCommType(CommType.PhysicalLink).
            setLayerName("name").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(request.toByteString()).
                setType(CustomCommsMessageType.SetLayerNameRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CCOMM_ERROR, 
            "Unable to set the name for layer PhysicalLink with UUID " + testUuid.toString() + ". Exception:OTHER");
        //reused channel
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test get name for layer response.
     * Verify that expected event is posted.
     */
    @Test
    public void testGetLayerNameResponse() throws IOException, IllegalArgumentException
    {
        GetLayerNameResponseData response = GetLayerNameResponseData.newBuilder().
            setCommType(CommType.Linklayer).
            setLayerName("name").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setData(response.toByteString()).
                setType(CustomCommsMessageType.GetLayerNameResponse).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((CustomCommsNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(namespace));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(CustomCommsMessageType.GetLayerNameResponse.toString()));
        assertThat((GetLayerNameResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test set name for layer response.
     * Verify that expected event is posted.
     */
    @Test
    public void testSetLayerNameResponse() throws IOException, IllegalArgumentException
    {
        CustomCommsNamespace namespace = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.SetLayerNameResponse).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage message = createCustomCommsMessage(namespace);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((CustomCommsNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(namespace));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(CustomCommsMessageType.SetLayerNameResponse.toString()));
    }
    
    /**
     * Test the get capabilities request/response message system for link layers.
     * Specifically, the following behaviors are tested:
     *     Verify incoming request message is posted to the event admin.
     *     Verify capabilities are gotten from the appropriate link layer factory.
     *     Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public final void testGetLinkLayerCapabilities() throws UnmarshalException, ObjectConverterException, IOException
    { 
        CustomCommsNamespace linkLayerMessage = 
                createGetCapsRequestMessage(CommType.Linklayer, LinkLayer.class.getName());
        TerraHarvestPayload payload = createPayload(linkLayerMessage);
        TerraHarvestMessage message = createCustomCommsMessage(linkLayerMessage);
        
        Message capsGen = LinkLayerCapabilitiesGen.LinkLayerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("Super Cool Link Layers Inc").
                    setDescription("The best link layer.").
                    setProductName("CoolLinkLayer")).
                setPhysicalLinkRequired(false).
                setPerformBITSupported(false).
                setModality(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT).
                setStaticMtu(false).
                setSupportsAddressing(false).build();
        
        //mock necessary objects/actions
        LinkLayerCapabilities capabilities = mock(LinkLayerCapabilities.class);
        when(m_LinkLayerFactory.getLinkLayerCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).thenReturn(capsGen);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // reply
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetCapabilitiesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(GetCapabilitiesRequestData.newBuilder().
                        setCommType(CommType.Linklayer).setProductType(LinkLayer.class.getName()).build()));
        
        //capture and verify response
        ArgumentCaptor<GetCapabilitiesResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetCapabilitiesResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message),
                eq(CustomCommsMessageType.GetCapabilitiesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetCapabilitiesResponseData response = messageCaptor.getValue();
        
        //ensure all necessary fields are set
        assertThat(response.hasLinkCapabilities(), is(true));
        assertThat(response.getLinkCapabilities(), is(capsGen));
        assertThat(response.getProductType(), is(LinkLayer.class.getName()));
    }
    
    /**
     * Test the get capabilities request/response message system for transport layers.
     * Specifically, the following behaviors are tested:
     *     Verify incoming request message is posted to the event admin.
     *     Verify capabilities are gotten from the appropriate transport factory.
     *     Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public final void testGetTransportLayerCapabilities() throws UnmarshalException, ObjectConverterException, 
        IOException
    { 
        CustomCommsNamespace transportLayerMessage = 
                createGetCapsRequestMessage(CommType.TransportLayer, TransportLayer.class.getName());
        TerraHarvestPayload payload = createPayload(transportLayerMessage);
        TerraHarvestMessage message = createCustomCommsMessage(transportLayerMessage);
        
        Message capsGen = TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("Super Cool Transport Layers Inc").
                    setDescription("The best transport layer.").
                    setProductName("CoolTransportLayer")).
                build();
        
        //mock necessary objects/actions
        TransportLayerCapabilities capabilities = mock(TransportLayerCapabilities.class);
        when(m_TransportLayerFactory.getTransportLayerCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).thenReturn(capsGen);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // reply
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetCapabilitiesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
            is(GetCapabilitiesRequestData.newBuilder().
                    setCommType(CommType.TransportLayer).setProductType(TransportLayer.class.getName()).build()));
        
        //capture and verify response
        ArgumentCaptor<GetCapabilitiesResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetCapabilitiesResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message),
                eq(CustomCommsMessageType.GetCapabilitiesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetCapabilitiesResponseData response = messageCaptor.getValue();
        
        //ensure all necessary fields are set
        assertThat(response.hasTransportCapabilities(), is(true));
        assertThat(response.getTransportCapabilities(), is(capsGen));
        assertThat(response.getProductType(), is(TransportLayer.class.getName()));
        m_SUT.deactivate();
    }
    
    /**
     * Test the get capabilities request/response message system for physical link.
     * Specifically, the following behaviors are tested:
     *     Verify incoming request message is posted to the event admin.
     *     Verify capabilities are gotten from the appropriate physical factory.
     *     Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public final void testGetPhysicalLinkCapabilities() throws UnmarshalException, ObjectConverterException, IOException
    {  
        CustomCommsNamespace physicalLinkMessage = 
                this.createGetCapsRequestMessage(CommType.PhysicalLink, PhysicalLink.class.getName());
        TerraHarvestPayload payload = createPayload(physicalLinkMessage);
        TerraHarvestMessage message = createCustomCommsMessage(physicalLinkMessage);
        
        Message capsGen = PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("Super Cool Physical Links Inc").
                    setDescription("The best physical link.").
                    setProductName("CoolPhysicalLink")).
                setLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT).build();
        
        //mock necessary objects/actions
        PhysicalLinkCapabilities capabilities = mock(PhysicalLinkCapabilities.class);
        when(m_PhysicalLinkFactory.getPhysicalLinkCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).thenReturn(capsGen);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // reply
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetCapabilitiesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(GetCapabilitiesRequestData.newBuilder().
                        setCommType(CommType.PhysicalLink).setProductType(PhysicalLink.class.getName()).build()));
        
        //capture and verify response
        ArgumentCaptor<GetCapabilitiesResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetCapabilitiesResponseData.class);
        verify(m_MessageFactory).createCustomCommsResponseMessage(eq(message),
                eq(CustomCommsMessageType.GetCapabilitiesResponse), messageCaptor.capture());
        
        GetCapabilitiesResponseData response = messageCaptor.getValue();
        
        //ensure all necessary fields are set
        assertThat(response.hasPhysicalCapabilities(), is(true));
        assertThat(response.getPhysicalCapabilities(), is(capsGen));
        assertThat(response.getProductType(), is(PhysicalLink.class.getName()));
        m_SUT.deactivate();
    }
    
    /**
     * Verify get capabilities response message will set the data event property correctly when handled
     * for link layers.
     */
    @Test
    public final void testGetLinkLayerCapabilitiesResponse() throws IOException
    {
        Message capsGen = LinkLayerCapabilitiesGen.LinkLayerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("LinkCaps Response Makers").
                    setDescription("A responsive link layer.").
                    setProductName("CapsResponseLinkLayer")).
                setPhysicalLinkRequired(false).
                setPerformBITSupported(false).
                setModality(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT).
                setStaticMtu(false).
                setSupportsAddressing(false).build();
        Message response = GetCapabilitiesResponseData.newBuilder().
                setLinkCapabilities((LinkLayerCapabilitiesGen.LinkLayerCapabilities) capsGen).
                setCommType(CommType.Linklayer).
                setProductType(LinkLayer.class.getName()).build();
        CustomCommsNamespace linkLayerMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.GetCapabilitiesResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(linkLayerMessage);
        TerraHarvestMessage message = createCustomCommsMessage(linkLayerMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);
        
        //verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
    
    /**
     * Verify get capabilities response message will set the data event property correctly when handled
     * for transport layers.
     */
    @Test
    public final void testGetTransportLayerCapabilitiesResponse() throws IOException
    {
        Message capsGen = TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("TransportCaps Response Makers").
                    setDescription("A responsive transport layer.").
                    setProductName("CapsResponseTransportLayer")).
                build();
        Message response = GetCapabilitiesResponseData.newBuilder().
                setTransportCapabilities((TransportLayerCapabilitiesGen.TransportLayerCapabilities) capsGen).
                setCommType(CommType.TransportLayer).
                setProductType(TransportLayer.class.getName()).build();
        CustomCommsNamespace transportLayerMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.GetCapabilitiesResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(transportLayerMessage);
        TerraHarvestMessage message = createCustomCommsMessage(transportLayerMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);
        
        //verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
    
    /**
     * Verify get capabilities response message will set the data event property correctly when handled
     * for physical links.
     */
    @Test
    public final void testGetPhysicalLinkCapabilitiesResponse() throws IOException
    {
        Message capsGen = PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("PhysicalCaps Response Makers").
                    setDescription("A responsive physical link.").
                    setProductName("CapsResponsePhysicalLink")).
                setLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT).build();
        Message response = GetCapabilitiesResponseData.newBuilder().
                setPhysicalCapabilities((PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities) capsGen).
                setCommType(CommType.PhysicalLink).
                setProductType(PhysicalLink.class.getName()).build();
        CustomCommsNamespace physicalLinkMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.GetCapabilitiesResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(physicalLinkMessage);
        TerraHarvestMessage message = createCustomCommsMessage(physicalLinkMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);
        
        //verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
    
    /**
     * Test that an invalid fqcn value is correctly handled for all comms layer types
     * during the get capabilities operation. This error occurs if the fqcn included in the message
     * does not correspond to a factory of the given type in the custom comms service registry.
     */
    @Test
    public final void testInvalidFactoryError() throws IOException
    {
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // test LinkLayerCapabilities invalid value:
        CustomCommsNamespace commsMessage = 
                createGetCapsRequestMessage(CommType.Linklayer, "bad.link.layer.name");
        TerraHarvestPayload payload = createPayload(commsMessage);
        TerraHarvestMessage message = createCustomCommsMessage(commsMessage);
        
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE,
                "Cannot complete request. Link layer factory not found.");
        verify(m_ResponseWrapper).queue(channel);
        
        // test TransportLayerCapabilities invalid value:
        // use LinkLayer.class as the product type this time, error should still be thrown because
        // there are no TransportLayer factories corresponding to LinkLayer.class
        commsMessage = createGetCapsRequestMessage(CommType.TransportLayer, LinkLayer.class.getName());
        payload = createPayload(commsMessage);
        message = createCustomCommsMessage(commsMessage);
        
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE,
                "Cannot complete request. Transport layer factory not found.");
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    
        // test PhysicalLinkCapabilities invalid value:
        commsMessage = createGetCapsRequestMessage(CommType.PhysicalLink, "bad.physcial.link.name");
        payload = createPayload(commsMessage);
        message = createCustomCommsMessage(commsMessage);
        
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE,
                "Cannot complete request. Physical link factory not found.");
        //reused channel
        verify(m_ResponseWrapper, times(3)).queue(channel);
    }
    
    /**
     * Test that the object converter is correctly handled when an error occurs converting the capabilities
     * to google protocol buffer format.
     */
    @Test
    public final void testObjectConverter() throws UnmarshalException, IOException, ObjectConverterException
    {
        // test LinkLayerCapabilities objectConverter exception:
        CustomCommsNamespace commsMessage = 
                createGetCapsRequestMessage(CommType.Linklayer, LinkLayer.class.getName());
        TerraHarvestPayload payload = createPayload(commsMessage);
        TerraHarvestMessage message = createCustomCommsMessage(commsMessage);
    
        //mock necessary objects/actions;
        LinkLayerCapabilities linkCaps = mock(LinkLayerCapabilities.class);
        when(m_LinkLayerFactory.getLinkLayerCapabilities()).thenReturn(linkCaps);
        when(m_Converter.convertToProto(linkCaps)).
                thenThrow(new ObjectConverterException("cannot convert"));
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        //reply
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Cannot complete request. cannot convert");
        verify(m_ResponseWrapper).queue(channel);
        
        // test TransportLayerCapabilities unmarshal exception:
        commsMessage = createGetCapsRequestMessage(CommType.TransportLayer, TransportLayer.class.getName());
        payload = createPayload(commsMessage);
        message = createCustomCommsMessage(commsMessage);
    
        //mock necessary objects/actions;
        TransportLayerCapabilities transportCaps = mock(TransportLayerCapabilities.class);
        when(m_TransportLayerFactory.getTransportLayerCapabilities()).thenReturn(transportCaps);
        when(m_Converter.convertToProto(transportCaps)).
                thenThrow(new ObjectConverterException("cannot convert"));
        
        //reply
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Cannot complete request. cannot convert");
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        // test PhysicalLinkCapabilities unmarshal exception: 
        commsMessage = createGetCapsRequestMessage(CommType.PhysicalLink, PhysicalLink.class.getName());
        payload = createPayload(commsMessage);
        message = createCustomCommsMessage(commsMessage);
    
        //mock necessary objects/actions;
        PhysicalLinkCapabilities physicalCaps = mock(PhysicalLinkCapabilities.class);
        when(m_PhysicalLinkFactory.getPhysicalLinkCapabilities()).thenReturn(physicalCaps);
        when(m_Converter.convertToProto(physicalCaps)).
                thenThrow(new ObjectConverterException("cannot convert"));
        
        //reply
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Cannot complete request. cannot convert");
        //reused channel
        verify(m_ResponseWrapper, times(3)).queue(channel);
    }
    
    /**
     * Helper method that constructs a valid terra harvest message with the comms namespace.
     */
    TerraHarvestMessage createCustomCommsMessage(CustomCommsNamespace customCommsNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.CustomComms, 100, 
                customCommsNamespaceMessage);
    }
    private TerraHarvestPayload createPayload(CustomCommsNamespace customCommsNamespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.CustomComms).
               setNamespaceMessage(customCommsNamespaceMessage.toByteString()).
               build();
    }
    
    /**
     * Helper method to construct getCapabilites request message
     */
    private CustomCommsNamespace createGetCapsRequestMessage(final CommType commType, final String fqcn)
    {
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
                setCommType(commType).setProductType(fqcn).build();
        CustomCommsNamespace capsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.GetCapabilitiesRequest).
                setData(request.toByteString()).build();
        return capsMessage;
    }
}
