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
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.LinkLayerMessages;
import mil.dod.th.core.remote.proto.LinkLayerMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.comms.LinkLayerMessageService;
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
 * Testing for the link layer message service, which includes testing to make sure all requests and responses
 * are sending correctly for all link layer message types.
 * @author matt
 */
public class TestLinkLayerMessageService
{
    private LinkLayerMessageService m_SUT;
    
    private LoggingService m_Logging;
    private mil.dod.th.core.ccomm.CustomCommsService m_CustomCommsService;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private AddressManagerService m_AddressManagerService;
    private UUID testUuid = UUID.randomUUID();
    private List<LinkLayer> m_LinkLayerList;
    private LinkLayer testLayer;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new LinkLayerMessageService();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_CustomCommsService = mock(mil.dod.th.core.ccomm.CustomCommsService.class);
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
        
        testLayer = mock(LinkLayer.class);
        m_LinkLayerList = new ArrayList<LinkLayer>();
        m_LinkLayerList.add(testLayer);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(m_LinkLayerList);
        when(testLayer.getUuid()).thenReturn(testUuid);
        when(testLayer.performBit()).thenReturn(LinkStatus.LOST);
        
        when(m_MessageFactory.createLinkLayerResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(LinkLayerMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
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
     * Verify event posted for request message, verify event property key in the posted event is the correct request 
     * for the message that was sent, also capture argument values for the response message and make sure that the 
     * data sent is correct.
     */
    @Test
    public void testGetPhysicalLink() throws IOException
    {
        // build the terra harvest message
        GetPhysicalLinkRequestData request = GetPhysicalLinkRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetPhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock a physical link
        PhysicalLink plink = mock(PhysicalLink.class);
        when(plink.getUuid()).thenReturn(testUuid);
        
        // when link layer's physical link is requested return a mocked physical link
        when(testLayer.getPhysicalLink()).thenReturn(plink);
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetPhysicalLinkRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetPhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetPhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.GetPhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetPhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getPhysicalLinkUuid()), is(testUuid));
    }
    
    /**
     * Verify returned physical link UUID is null when the link layer does not have a physical link.
     */
    @Test
    public void testGetPhysicalLink_NoPhysicalLink() throws IOException
    {
        // build the terra harvest message
        GetPhysicalLinkRequestData request = GetPhysicalLinkRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetPhysicalLinkRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // when link layer's physical link is requested return a mocked physical link
        when(testLayer.getPhysicalLink()).thenReturn(null);
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify response
        ArgumentCaptor<GetPhysicalLinkResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetPhysicalLinkResponseData.class);  
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.GetPhysicalLinkResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetPhysicalLinkResponseData response = messageCaptor.getValue();
 
        assertThat(response.hasPhysicalLinkUuid(), is(false));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetPhysicalLinkResponse() throws IOException
    {
        // build the terra harvest message
        Message response = GetPhysicalLinkResponseData.newBuilder()
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetPhysicalLinkResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
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
     * Verify event posted for request message, verify event property key in the posted event is the correct request 
     * for the message that was sent, also capture argument values for the response message and make sure that the 
     * data sent is correct.
     */
    @Test
    public void testIsAvailable() throws IOException, CCommException
    {
        // build the terra harvest message
        IsAvailableRequestData request = IsAvailableRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setAddress("woof")
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.IsAvailableRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock address
        Address testAddress = mock(Address.class);
        when(m_AddressManagerService.getOrCreateAddress("woof")).thenReturn(testAddress);
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
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.IsAvailableResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsAvailableResponseData response = messageCaptor.getValue();
 
        assertThat(response.getAvailable(), is(true));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Verify event posted for request message even after an exception is thrown, verify event property key in the 
     * posted event is the correct request for the message that was sent, as well as verify that the error response 
     * message sent has the correct error code and correct arguments.
     */
    @Test
    public void testIsAvailableExceptions() throws CCommException, IOException
    {
        // build the terra harvest message
        IsAvailableRequestData request = IsAvailableRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setAddress("woof")
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.IsAvailableRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //----Test Custom Comms Exception----
        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW)).when(m_AddressManagerService).getOrCreateAddress(
                "woof");
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsAvailableRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
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
    public void testIsAvailableResponse() throws IOException
    {
        // build the terra harvest message
        Message response = IsAvailableResponseData.newBuilder()
                .setAvailable(true)
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID()))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.IsAvailableResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
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
     * Verify event posted for request message, verify event property key in the posted event is the correct request 
     * for the message that was sent, also capture argument values for the response message and make sure that the 
     * data sent is correct.
     */
    @Test
    public void testGetMtu() throws IOException
    {
        // build the terra harvest message
        GetMtuRequestData request = GetMtuRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetMtuRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock mtu value returned from test link layer
        when(testLayer.getMtu()).thenReturn(9000);
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetMtuRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<GetMtuResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetMtuResponseData.class);  
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.GetMtuResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetMtuResponseData response = messageCaptor.getValue();
 
        assertThat(response.getMtu(), is(9000));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetMtuResponse() throws IOException
    {
        // build the terra harvest message
        Message response = GetMtuResponseData.newBuilder()
                .setMtu(9000)
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetMtuResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
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
     * Verify event posted for request message, verify event property key in the posted event is the correct request 
     * for the message that was sent, also capture argument values for the response message and make sure that the 
     * data sent is correct.
     */
    @Test
    public void testIsActivated() throws IOException
    {
        // build the terra harvest message
        IsActivatedRequestData request = IsActivatedRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.IsActivatedRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock that the test link layer is activated
        when(testLayer.isActivated()).thenReturn(true);
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsActivatedRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<IsActivatedResponseData> messageCaptor = ArgumentCaptor.forClass(
                IsActivatedResponseData.class);  
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.IsActivatedResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsActivatedResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsActivated(), is(true));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsActivatedResponse() throws IOException
    {
        // build the terra harvest message
        Message response = IsActivatedResponseData.newBuilder()
                .setIsActivated(true)
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.IsActivatedResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
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
     * Verify event posted for request message, verify event property key in the posted event is the correct request 
     * for the message that was sent, also capture argument values for the response message and make sure that the data 
     * sent is correct.
     */
    @Test
    public void testGetStatus() throws IOException
    {
        // build the terra harvest message
        GetStatusRequestData request = GetStatusRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetStatusRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //-----testing ok status-----
        LinkStatus testStatus = LinkStatus.OK;
        
        when(testLayer.getLinkStatus()).thenReturn(testStatus);
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((GetStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<GetStatusResponseData> messageCaptor = ArgumentCaptor.forClass(
                GetStatusResponseData.class);  
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.GetStatusResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetStatusResponseData response = messageCaptor.getValue();
 
        assertThat(response.getLinkStatus(), is(LinkLayerMessages.LinkStatus.OK));
        
        //-----testing lost status-----
        testStatus = LinkStatus.LOST;
        
        when(testLayer.getLinkStatus()).thenReturn(testStatus);
        
        m_SUT.handleMessage(message, payload, channel);

        
        //capture and verify response
        verify(m_MessageFactory, times(2)).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.GetStatusResponse), messageCaptor.capture());
        //reused the channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        response = messageCaptor.getValue();
 
        assertThat(response.getLinkStatus(), is(LinkLayerMessages.LinkStatus.LOST));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testGetStatusResponse() throws IOException
    {
        // build the terra harvest message
        Message response = GetStatusResponseData.newBuilder()
                .setLinkStatus(LinkLayerMessages.LinkStatus.OK)
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID()))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.GetStatusResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
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
     * Verify link layer was requested to deactivate, verify event posted for request message, verify event property 
     * key in the posted event is the correct request for the message that was sent, also capture argument values for 
     * the response message and make sure that the data sent is correct.
     */
    @Test
    public void testDeactivate() throws IOException
    {
        // build the terra harvest message
        DeactivateRequestData request = DeactivateRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.DeactivateRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((DeactivateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        // make sure deactivate was called on the test link layer
        verify(testLayer).deactivateLayer();
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.DeactivateResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the deactivate response message sends an event with null data message.
     */
    @Test
    public void testDeactivateResponse() throws IllegalArgumentException, IOException
    {
        LinkLayerNamespace ccommMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.DeactivateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createLinkLayerMessage(ccommMessage);
        
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
                is(LinkLayerMessageType.DeactivateResponse.toString()));
    }
    
    /**
     * Verify link layer was requested to activate, verify event posted for request message, verify event property key 
     * in the posted event is the correct request for the message that was sent, also capture argument values for the 
     * response message and make sure that the data sent is correct.
     */
    @Test
    public void testActivate() throws IOException
    {
        // build the terra harvest message
        ActivateRequestData request = ActivateRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.ActivateRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((ActivateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        // make sure activate was called on the test link layer
        verify(testLayer).activateLayer();
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.ActivateResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the activate response message sends an event with null data message.
     */
    @Test
    public void testActivateResponse() throws IllegalArgumentException, IOException
    {
        LinkLayerNamespace ccommMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.ActivateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createLinkLayerMessage(ccommMessage);
        
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
                is(LinkLayerMessageType.ActivateResponse.toString()));
    }

    /**
     * Test requesting a link layer to perform its BIT.
     * Verify that a response message is sent.
     */
    @Test
    public void testPerformBITRequest() throws CCommException, IOException
    {
        // create request
        PerformBITRequestData request = PerformBITRequestData.newBuilder()
                .setLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.PerformBITRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);

        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((PerformBITRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        // make sure perform BIT was called on the test link layer
        verify(testLayer).performBit();
        
        ArgumentCaptor<PerformBITResponseData> captor = ArgumentCaptor.forClass(PerformBITResponseData.class);
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.PerformBITResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //verify response
        PerformBITResponseData response = captor.getValue();
        assertThat(response.getLinkUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
        assertThat(response.getPerformBitStatus(), is(LinkLayerMessages.LinkStatus.LOST));
    }

    /**
     * Test requesting a link layer to perform its BIT, and the layer does not support BIT.
     * Verify that a response message is sent.
     */
    @Test
    public void testPerformBITRequestCComException() throws CCommException, IOException
    {
        // create request
        PerformBITRequestData request = PerformBITRequestData.newBuilder()
                .setLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.PerformBITRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);

        //mock exception from the layer
        when(testLayer.performBit()).thenThrow(new CCommException(FormatProblem.OTHER));
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // make sure perform BIT was called on the test link layer
        verify(testLayer).performBit();

        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.CCOMM_ERROR), 
            anyString());
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test Perform BIT response.
     * Verify event.
     */
    @Test
    public void testPerformBITResponse() throws CCommException, IOException
    {
        // create request
        PerformBITResponseData request = PerformBITResponseData.newBuilder()
                .setLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .setPerformBitStatus(LinkLayerMessages.LinkStatus.OK)
                .build();
        
        LinkLayerNamespace llMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.PerformBITResponse)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(llMessage);
        TerraHarvestMessage message = createLinkLayerMessage(llMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);

        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((PerformBITResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
    }
    
    /**
     * Verify that the core service is requested to remove a link layer, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testRemoveLinkLayer() throws IOException, FactoryException
    {
        DeleteRequestData request = DeleteRequestData.newBuilder()
                .setLinkLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        LinkLayerNamespace ccommMessage = LinkLayerNamespace.newBuilder()
                .setType(LinkLayerMessageType.DeleteRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createLinkLayerMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock link layer list
        LinkLayer lLayer = mock(LinkLayer.class);
        when(lLayer.getUuid()).thenReturn(testUuid);
        
        List<LinkLayer> listLayers = new ArrayList<LinkLayer>();
        listLayers.add(lLayer);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(listLayers);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify remove link layer was called
        verify(lLayer).delete();
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createLinkLayerResponseMessage(eq(message), 
                eq(LinkLayerMessageType.DeleteResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the remove link layer response message sends an event with null data message.
     */
    @Test
    public void testRemoveLinkLayerResponse() throws IllegalArgumentException, IOException
    {
        LinkLayerNamespace ccommMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.DeleteResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createLinkLayerMessage(ccommMessage);
        
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
                is(LinkLayerMessageType.DeleteResponse.toString()));
    }
    
    private TerraHarvestMessage createLinkLayerMessage(LinkLayerNamespace namespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.LinkLayer, 100, namespaceMessage);
    }
    
    private TerraHarvestPayload createPayload(LinkLayerNamespace namespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.LinkLayer).
               setNamespaceMessage(namespaceMessage.toByteString()).
               build();
    }
}
