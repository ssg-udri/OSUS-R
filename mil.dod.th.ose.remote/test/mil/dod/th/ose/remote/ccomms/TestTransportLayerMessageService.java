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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.TransportLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.ShutdownRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.comms.TransportLayerMessageService;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Testing for the transport layer message service, which includes testing to make sure all requests and responses
 * are sending correctly for all transport message types.
 * @author matt
 */
public class TestTransportLayerMessageService
{
    private TransportLayerMessageService m_SUT;
    
    private LoggingService m_Logging;
    private CustomCommsService m_CustomCommsService;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private AddressManagerService m_AddressManagerService;
    private UUID testUuid = UUID.randomUUID();
    private List<TransportLayer> m_TransportList;
    private TransportLayer testLayer;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new TransportLayerMessageService();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_CustomCommsService = mock(CustomCommsService.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_AddressManagerService = mock(AddressManagerService.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setAddressManagerService(m_AddressManagerService);
        m_SUT.setMessageRouter(m_MessageRouter);
        
        testLayer = mock(TransportLayer.class);
        m_TransportList = new ArrayList<TransportLayer>();
        m_TransportList.add(testLayer);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(m_TransportList);
        when(testLayer.getUuid()).thenReturn(testUuid);
        
        when(m_MessageFactory.createTransportLayerResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(TransportLayerMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
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
     * Verify the namespace is transport layer
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.TransportLayer));
    }
    
    /**
     * Verify shutdown is called on the test transport layer, verify event posted for request message, verify event 
     * property key in the posted event is the correct request for the message that was sent, also capture argument 
     * values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testShutdown() throws IOException, CCommException
    {
        ShutdownRequestData request = ShutdownRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.ShutdownRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((ShutdownRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        // make sure shutdown was called on the test transport layer
        verify(testLayer).shutdown();
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.ShutdownResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the shutdown response message sends an event with null data message.
     */
    @Test
    public void testShutdownResponse() throws IllegalArgumentException, IOException
    {
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.ShutdownResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(transportMessage);
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        
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
                is(TransportLayerMessageType.ShutdownResponse.toString()));
    }
    
    /**
     * Verify we can get the UUID of the link layer associated with a transport layer, verify event posted for 
     * request message, verify event property key in the posted event is the correct request for the message that 
     * was sent, also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testGetLinkLayer() throws IOException
    {
        GetLinkLayerRequestData request = GetLinkLayerRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.GetLinkLayerRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock the link layer
        LinkLayer testLink = mock(LinkLayer.class);
        
        UUID linkUuid = UUID.randomUUID();
        when(testLink.getUuid()).thenReturn(linkUuid);
        when(testLayer.getLinkLayer()).thenReturn(testLink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetLinkLayerRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetLinkLayerResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetLinkLayerResponseData.class);  
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.GetLinkLayerResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLinkLayerResponseData response = messageCaptor.getValue();
 
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(linkUuid)));
    }

    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetLinkLayerResponse() throws IOException
    {
        // build the terra harvest message
        Message response = GetLinkLayerResponseData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.GetLinkLayerResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
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
     * Verify a link layer can see if an address is available and returns the correct value, verify event posted for 
     * request message, verify event property key in the posted event is the correct request for the message that 
     * was sent, also capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testIsAvailable() throws CCommException, IOException
    {
        IsAvailableRequestData request = IsAvailableRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setAddress("finn")
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsAvailableRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock the address
        Address testAddress = mock(Address.class);
        when(m_AddressManagerService.getOrCreateAddress("finn")).thenReturn(testAddress);
        when(testLayer.isAvailable(testAddress)).thenReturn(true);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsAvailableRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<IsAvailableResponseData> messageCaptor = ArgumentCaptor.forClass(
                IsAvailableResponseData.class);  
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.IsAvailableResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsAvailableResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsAvailable(), is(true));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Test is available when a CComException is thrown. 
     * Verify error message.
     */
    @Test
    public void testIsAvailableIllegalException() throws CCommException, IOException
    {
        IsAvailableRequestData request = IsAvailableRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setAddress("finn")
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsAvailableRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(transportMessage);
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock the address
        when(m_AddressManagerService.getOrCreateAddress("finn")).thenThrow(new CCommException(FormatProblem.OTHER));
        
        m_SUT.handleMessage(message, payload, channel);
         
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsAvailableResponse() throws IOException
    {
        // build the terra harvest message
        Message response = IsAvailableResponseData.newBuilder()
                .setIsAvailable(true)
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID()))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsAvailableResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
         
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
     * Verify a link layer's transmitting status can be retrieved, verify event posted for request message, 
     * verify event property key in the posted event is the correct request for the message that was sent, also 
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testIsTransmitting() throws IOException
    {
        IsTransmittingRequestData request = IsTransmittingRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsTransmittingRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        when(testLayer.isTransmitting()).thenReturn(true);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsTransmittingRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<IsTransmittingResponseData> messageCaptor = ArgumentCaptor.forClass(
                IsTransmittingResponseData.class);  
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.IsTransmittingResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsTransmittingResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsTransmitting(), is(true));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsTransmittingResponse() throws IOException
    {
        // build the terra harvest message
        Message response = IsTransmittingResponseData.newBuilder()
                .setIsTransmitting(true)
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsTransmittingResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
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
     * Verify a link layer's receiving status can be retrieved, verify event posted for request message, 
     * verify event property key in the posted event is the correct request for the message that was sent, also 
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testIsReceiving() throws IOException
    {
        IsReceivingRequestData request = IsReceivingRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsReceivingRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        when(testLayer.isReceiving()).thenReturn(true);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsReceivingRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<IsReceivingResponseData> messageCaptor = ArgumentCaptor.forClass(
                IsReceivingResponseData.class);  
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.IsReceivingResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsReceivingResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsReceiving(), is(true));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsReceivingResponse() throws IOException
    {
        // build the terra harvest message
        Message response = IsReceivingResponseData.newBuilder()
                .setIsReceiving(true)
                .build();
        
        TransportLayerNamespace transportMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.IsReceivingResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestMessage message = createTransportLayerMessage(transportMessage);
        TerraHarvestPayload payload = createPayload(transportMessage);
        
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
    
    TerraHarvestMessage createTransportLayerMessage(TransportLayerNamespace namespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.TransportLayer, 100, 
                namespaceMessage);
    }
    private TerraHarvestPayload createPayload(TransportLayerNamespace namespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.TransportLayer).
               setNamespaceMessage(namespaceMessage.toByteString()).
               build();
    }
    
    /**
     * Verify that the core service is requested to remove a transport layer, verify event posted for request
     * message, verify event property request in the posted event is the correct key for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testRemoveTransportLayer() throws IOException, FactoryException
    {
        DeleteRequestData request = DeleteRequestData.newBuilder()
                .setTransportLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        TransportLayerNamespace ccommMessage = TransportLayerNamespace.newBuilder()
                .setType(TransportLayerMessageType.DeleteRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createTransportLayerMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock transport layer
        TransportLayer transportLayer = mock(TransportLayer.class);
        when(transportLayer.getUuid()).thenReturn(testUuid);
        
        List<TransportLayer> transportList = new ArrayList<TransportLayer>();
        transportList.add(transportLayer);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(transportList);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify remove transport layer was called
        verify(transportLayer).delete();
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.DeleteResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the remove transport layer response message sends an event with null data message.
     */
    @Test
    public void testRemoveTransportLayerResponse() throws IllegalArgumentException, IOException
    {
        TransportLayerNamespace ccommMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.DeleteResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createTransportLayerMessage(ccommMessage);
        
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
                is(TransportLayerMessageType.DeleteResponse.toString()));
    }

    /**
     * Verify that a property request is properly handled
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testSetPropertyRequest() throws IOException, 
        IllegalArgumentException, IllegalStateException, FactoryException
    {
        UUID uuid = UUID.randomUUID();
        // build request
        Multitype multi = Multitype.newBuilder().setStringValue("valueA").setType(Type.STRING).build();
        SimpleTypesMapEntry type = SimpleTypesMapEntry.newBuilder().setKey("AKey").setValue(multi).build();
        SetPropertyRequestData request = SetPropertyRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).addProperties(type).build();
        TransportLayerNamespace transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.SetPropertyRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(transportLayerMessage);
        TerraHarvestMessage message = createTransportLayerMessage(transportLayerMessage);
        
        //mock necessary objects/actions
        TransportLayer transportLayer = mock(TransportLayer.class);
        when(transportLayer.getUuid()).thenReturn(uuid);
        
        Map<String, Object> propsToSet = new HashMap<>();
        propsToSet.put("AKey", "");
        
        when(transportLayer.getProperties()).thenReturn(propsToSet);
        
        List<TransportLayer> transportLayerList = new ArrayList<TransportLayer>();
        transportLayerList.add(transportLayer);
        when(m_CustomCommsService.getTransportLayers()).thenReturn(transportLayerList);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory, times(1)).createTransportLayerResponseMessage(eq(message), 
                eq(TransportLayerMessageType.SetPropertyResponse), eq((Message)null));
        
        ArgumentCaptor<Map> dictCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transportLayer, times(1)).setProperties(dictCaptor.capture());
        
        Map<String, Object> capDict = dictCaptor.getValue();
        assertThat(capDict.size(), is(1));
        
        assertThat((String)capDict.get("AKey"), is("valueA"));
    }

    /**
     * Verify set property response message when handled will set the data event property.
     */
    @Test
    public void testSetPropertyResponse() throws IOException
    {
        Message response = null;
        TransportLayerNamespace transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.SetPropertyResponse).build();
        TerraHarvestPayload payload = createPayload(transportLayerMessage);
        TerraHarvestMessage message = createTransportLayerMessage(transportLayerMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
}
