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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.comms.PhysicalLinkMessageService;
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
 * Testing for the physical link message service, which includes testing to make sure all requests and responses
 * are sending correctly for all physical link message types.
 * @author matt
 */
public class TestPhysicalLinkMessageService
{
    private PhysicalLinkMessageService m_SUT;
    
    private CustomCommsService m_CustomCommsService;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private UUID testUuid = UUID.randomUUID();
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    private LoggingService m_Logging;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new PhysicalLinkMessageService();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_CustomCommsService = mock(CustomCommsService.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_Logging = LoggingServiceMocker.createMock();
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setLoggingService(m_Logging);
        
        when(m_MessageFactory.createPhysicalLinkResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(PhysicalLinkMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }
    
    /**
     * Verify the namespace is PhysicalLink
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.PhysicalLink));
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
    public void testIsOpen() throws IOException
    {
        IsOpenRequestData request = IsOpenRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.IsOpenRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        TerraHarvestPayload payload = createPayload(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock physical link
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("rawr");
        when(m_CustomCommsService.isPhysicalLinkOpen("rawr")).thenReturn(true);
        
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify response
        ArgumentCaptor<IsOpenResponseData> messageCaptor = ArgumentCaptor.forClass(IsOpenResponseData.class);  
        
        verify(m_MessageFactory).createPhysicalLinkResponseMessage(eq(message), 
                eq(PhysicalLinkMessageType.IsOpenResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsOpenResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsOpen(), is(true));
    }

    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsOpenResponse() throws IOException
    {
        Message response = IsOpenResponseData.newBuilder()
                .setIsOpen(true)
                .build();
        
        PhysicalLinkNamespace physicalMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.IsOpenResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestMessage message = createPhysicalLinkMessage(physicalMessage);
        TerraHarvestPayload payload = createPayload(physicalMessage);

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
     * Verify that the core service is requested to see if a physical link is in use, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testIsPhysicalLinkInUse() throws IOException, CCommException
    {
        IsInUseRequestData request = IsInUseRequestData.newBuilder()
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.IsInUseRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        PhysicalLink plink = mock(PhysicalLink.class);
        when(m_CustomCommsService.requestPhysicalLink(testUuid)).thenReturn(plink);
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        assertThat((IsInUseRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        ArgumentCaptor<IsInUseResponseData> messageCaptor = ArgumentCaptor.forClass(
                IsInUseResponseData.class);  
        
        verify(m_MessageFactory).createPhysicalLinkResponseMessage(eq(message), 
                eq(PhysicalLinkMessageType.IsInUseResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        IsInUseResponseData response = messageCaptor.getValue();
 
        assertThat(response.getIsInUse(), is(notNullValue()));
        assertThat(response.getIsInUse(), is(false));
    }
    
    /**
     * Verify event posted for response message, verify event property key in the posted event is the correct response 
     * for the message that was sent.
     */
    @Test
    public void testIsPhysicalLinkInUseResponse() throws IOException
    {
        Message response = IsInUseResponseData.newBuilder()
                .setIsInUse(true)
                .build();
        
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.IsInUseResponse)
                .setData(response.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        
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
     * Verify that the core service is requested to remove a physical link, verify event posted for request
     * message, verify event property key in the posted event is the correct request for the message that was sent, also
     * capture argument values for the response message and make sure that the data sent is correct.
     */
    @Test
    public void testRemovePhysicalLink() throws IOException, CCommException
    {
        DeleteRequestData request = DeleteRequestData.newBuilder()
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.DeleteRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        when(m_CustomCommsService.getPhysicalLinkName(testUuid)).thenReturn("Finn");
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify remove physical link was called
        verify(m_CustomCommsService).deletePhysicalLink("Finn");
        
        Message testMessage = null;
        
        verify(m_MessageFactory).createPhysicalLinkResponseMessage(eq(message), 
                eq(PhysicalLinkMessageType.DeleteResponse), eq(testMessage));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the remove physical link response message sends an event with null data message.
     */
    @Test
    public void testRemovePhysicalLinkResponse() throws IllegalArgumentException, IOException
    {
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder().
                setType(PhysicalLinkMessageType.DeleteResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        
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
                is(PhysicalLinkMessageType.DeleteResponse.toString()));
    }
    
    /**
     * Verify event posted for request message even after a exception is thrown, verify event property key in the 
     * posted event is the correct request for the message that was sent, as well as verify that the error response 
     * message sent has the correct error code and correct arguments.
     */
    @Test
    public void testRemovePhysicalLinkExceptions() throws IOException, CCommException
    {
        DeleteRequestData request = DeleteRequestData.newBuilder()
                .setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid))
                .build();
        
        PhysicalLinkNamespace ccommMessage = PhysicalLinkNamespace.newBuilder()
                .setType(PhysicalLinkMessageType.DeleteRequest)
                .setData(request.toByteString())
                .build();
        
        TerraHarvestPayload payload = createPayload(ccommMessage);
        TerraHarvestMessage message = createPhysicalLinkMessage(ccommMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // mock the physical link
        PhysicalLink plink = mock(PhysicalLink.class);
        when(m_CustomCommsService.requestPhysicalLink(testUuid)).thenReturn(plink);

        //----Test Custom Comms Exception----
        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW)).when(m_CustomCommsService).deletePhysicalLink(
                Mockito.anyString());
        
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        // verify the event contains the data message
        event = eventCaptor.getValue();
        
        assertThat((DeleteRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture and verify response
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), 
                eq(ErrorCode.CCOMM_ERROR), Mockito.anyString());
        //reused channel
        verify(m_ResponseWrapper).queue(channel);
    }
    
    TerraHarvestMessage createPhysicalLinkMessage(PhysicalLinkNamespace namespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.PhysicalLink, 100, namespaceMessage);
    }
    private TerraHarvestPayload createPayload(PhysicalLinkNamespace namespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.PhysicalLink).
               setNamespaceMessage(namespaceMessage.toByteString()).
               build();
    }
}