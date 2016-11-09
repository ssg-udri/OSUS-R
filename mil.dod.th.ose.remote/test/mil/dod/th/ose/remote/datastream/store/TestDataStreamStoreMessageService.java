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
package mil.dod.th.ose.remote.datastream.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.core.datastream.store.DateRange;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.ClientAckData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DisableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.EnableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsResponseData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivedDataRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivedDataResponseData;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace.DataStreamStoreMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.test.FactoryObjectMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.protobuf.Message;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(DataStreamStoreMessageService.class)
public class TestDataStreamStoreMessageService
{
    private DataStreamStoreMessageService m_SUT;
    
    @Mock private EventAdmin m_EventAdmin;
    @Mock private DataStreamService m_DataStreamService;
    @Mock private DataStreamStore m_DataStreamStore;
    @Mock private MessageFactory m_MessageFactory;
    @Mock private MessageRouterInternal m_MessageRouter;
    @Mock private MessageResponseWrapper m_ResponseWrapper;
    private UUID testProfileUuid = UUID.randomUUID();
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new DataStreamStoreMessageService();
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setDataStreamService(m_DataStreamService);
        m_SUT.setDataStreamStore(m_DataStreamStore);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        
        when(m_MessageFactory.createDataStreamStoreResponseMessage(Mockito.any(TerraHarvestMessage.class),
                Mockito.any(DataStreamStoreMessageType.class), Mockito.any(Message.class))).
                thenReturn(m_ResponseWrapper);
        
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class),
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);       
    }
    
    /**
     * Verify the namespace is DataStreamStore
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.DataStreamStore));
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
     * Verify generic handling of message (posts an event locally)
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        GetArchivePeriodsRequestData request = GetArchivePeriodsRequestData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build()).build();
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.GetArchivePeriodsRequest)
                .setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
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
                is(dataStreamStoreMessage.getType().toString()));
        assertThat((DataStreamStoreNamespace)postedEvent.
                getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), is(dataStreamStoreMessage));
        assertThat((GetArchivePeriodsRequestData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));   
    }
    
    @Test
    public void testEnableArchiving() throws IOException
    {
        EnableArchivingRequestData request = EnableArchivingRequestData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build())
                .setUseSourceBitrate(true)
                .setHeartbeatPeriod(5)
                .setDelay(0).build();
        
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.EnableArchivingRequest)
                .setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        Set<StreamProfile> streamProfileSet = new HashSet<>();
        streamProfileSet.add(streamProfile1);
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(streamProfileSet);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();   
        assertThat((EnableArchivingRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(m_MessageFactory).createDataStreamStoreResponseMessage(eq(message),
                eq(DataStreamStoreMessageType.EnableArchivingResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    @Test
    public void testDisableArchiving() throws IOException
    {
        DisableArchivingRequestData request = DisableArchivingRequestData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build()).build();
        
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.DisableArchivingRequest)
                .setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        Set<StreamProfile> streamProfileSet = new HashSet<>();
        streamProfileSet.add(streamProfile1);
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(streamProfileSet);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();   
        assertThat((DisableArchivingRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(m_MessageFactory).createDataStreamStoreResponseMessage(eq(message),
                eq(DataStreamStoreMessageType.DisableArchivingResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    @Test
    public void testClientAck() throws IOException
    {
        ClientAckData request = ClientAckData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build()).build();
        
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.ClientAck)
                .setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        
        when(m_DataStreamService.getStreamProfile(testProfileUuid)).thenReturn(streamProfile1);        
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_DataStreamStore).clientAck(streamProfile1);
    }
    
    @Test
    public void testGetArchivePeriods() throws IOException
    {
        GetArchivePeriodsRequestData request = GetArchivePeriodsRequestData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build()).build();
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.GetArchivePeriodsRequest)
                .setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        
        DateRange dr1 = new DateRange(new Date(1000000), new Date(1000100));
        DateRange dr2 = new DateRange(new Date(2000000), new Date(2000200));
        DateRange dr3 = new DateRange(new Date(3000000), new Date(3000300));
        
        List<DateRange> dateRanges = new ArrayList<>();
        dateRanges.add(dr1);
        dateRanges.add(dr2);
        dateRanges.add(dr3);
        
        when(m_DataStreamService.getStreamProfile(testProfileUuid)).thenReturn(streamProfile1);
        when(m_DataStreamStore.getArchivePeriods(streamProfile1)).thenReturn(dateRanges);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetArchivePeriodsResponseData> messageCaptor = 
                ArgumentCaptor.forClass(GetArchivePeriodsResponseData.class);
        verify(m_MessageFactory).createDataStreamStoreResponseMessage(eq(message), 
                eq(DataStreamStoreMessageType.GetArchivePeriodsResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetArchivePeriodsResponseData response = messageCaptor.getValue();
        assertThat(response.getDateRangeCount(), is(3));
    }
    
    @Test
    public void testGetArchivedData() throws Exception
    {
        GetArchivedDataRequestData request = GetArchivedDataRequestData.newBuilder()
                .setStreamProfileUuid(SharedMessages.UUID.newBuilder()
                        .setLeastSignificantBits(testProfileUuid.getLeastSignificantBits())
                        .setMostSignificantBits(testProfileUuid.getMostSignificantBits()).build())
                .setDateRange(mil.dod.th.core.remote.proto.DataStreamStoreMessages.DateRange.newBuilder()
                        .setStartTime(1000000)
                        .setStopTime(1000100).build()).build();
        
        DataStreamStoreNamespace dataStreamStoreMessage = DataStreamStoreNamespace.newBuilder()
                .setType(DataStreamStoreMessageType.GetArchivedDataRequest)
                .setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamStoreMessage);
        TerraHarvestMessage message = createMessage(dataStreamStoreMessage);
        
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        when(m_DataStreamService.getStreamProfile(testProfileUuid)).thenReturn(streamProfile1);
        
        int dataBlockSize = 1024*1024;
        InputStream in = mock(InputStream.class);
        BufferedInputStream buffIn = mock(BufferedInputStream.class);
        when(m_DataStreamStore.getArchiveStream(eq(streamProfile1), Mockito.any(DateRange.class))).thenReturn(in);
        
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(buffIn);
        
        //Read 1 MB of data the first time, half that the second time, and then read 0 bytes
        when(buffIn.read(Mockito.any(byte[].class))).thenReturn(dataBlockSize, dataBlockSize/2, 0); 
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetArchivedDataResponseData> messageCaptor =
                ArgumentCaptor.forClass(GetArchivedDataResponseData.class);
        
        verify(m_MessageFactory, times(3)).createDataStreamStoreResponseMessage(eq(message),
                eq(DataStreamStoreMessageType.GetArchivedDataResponse), messageCaptor.capture());
        
        verify(m_ResponseWrapper, times(3)).queue(channel);
        
        List<GetArchivedDataResponseData> responses = messageCaptor.getAllValues();
        
        assertThat(responses.size(), is(3));
        
        //First block
        assertThat(responses.get(0).getDataBlock().toByteArray().length, is(dataBlockSize));
        assertThat(responses.get(0).getIsLastResponse(), is(false));
        assertThat(responses.get(0).getSequenceNum(), is(0L));
        
        //Second block
        assertThat(responses.get(1).getDataBlock().toByteArray().length, is(dataBlockSize/2));
        assertThat(responses.get(1).getIsLastResponse(), is(false));
        assertThat(responses.get(1).getSequenceNum(), is(1L));
        
        //Third block
        assertThat(responses.get(2).getDataBlock().toByteArray().length, is(0));
        assertThat(responses.get(2).getIsLastResponse(), is(true));
        assertThat(responses.get(2).getSequenceNum(), is(2L));
    }
    
    private TerraHarvestMessage createMessage(DataStreamStoreNamespace dataStreamStoreMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.DataStreamStore, 3, 
                dataStreamStoreMessage);
    }
    
    private TerraHarvestPayload createPayload(DataStreamStoreNamespace dataStreamStoreMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.DataStreamStore).
               setNamespaceMessage(dataStreamStoreMessage.toByteString()).
               build();
    }
}
