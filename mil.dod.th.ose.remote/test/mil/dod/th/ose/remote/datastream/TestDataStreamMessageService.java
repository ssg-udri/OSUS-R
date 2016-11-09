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
package mil.dod.th.ose.remote.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DisableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.EnableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.FactoryObjectMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen;
import mil.dod.th.remote.lexicon.datastream.capability.StreamProfileCapabilitiesGen;
import mil.dod.th.remote.lexicon.datastream.types.StreamTypesGen;

/**
 * @author jmiller
 *
 */
public class TestDataStreamMessageService
{
    private DataStreamMessageService m_SUT;
    
    @Mock private EventAdmin m_EventAdmin;
    @Mock private DataStreamService m_DataStreamService;
    @Mock private AssetDirectoryService m_AssetDirectoryService;
    @Mock private MessageFactory m_MessageFactory;
    @Mock private MessageRouterInternal m_MessageRouter;
    @Mock private MessageResponseWrapper m_ResponseWrapper;
    @Mock private JaxbProtoObjectConverter m_Converter;
    private UUID testProfileUuid = UUID.randomUUID();
    
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new DataStreamMessageService();
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setDataStreamService(m_DataStreamService);
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        
        when(m_MessageFactory.createDataStreamServiceResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(DataStreamServiceMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
        
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }
    
    /**
     * Verify the namespace is DataStreamService
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.DataStreamService));
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
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder()
                .setProductType("blah").build();
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder()
                .setType(DataStreamServiceMessageType.GetCapabilitiesRequest)
                .setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);
        
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
                is(dataStreamMessage.getType().toString()));
        assertThat((DataStreamServiceNamespace)postedEvent.
                getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), is(dataStreamMessage));
        assertThat((GetCapabilitiesRequestData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));  
    }
    
    /**
     * Test get stream profiles request/response remote message system for the DataStream service.
     * Specifically, the following behaviors are tested: 
     *      Verify list of stream profiles are returned.
     *      Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public void testGetStreamProfiles() throws IOException, URISyntaxException
    {
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder()
                .setType(DataStreamServiceMessageType.GetStreamProfilesRequest).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);
        
        Asset testAsset = FactoryObjectMocker.mockFactoryObject(Asset.class, "testAssetPid");
        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        when(streamProfile1.getAsset()).thenReturn(testAsset);
        when(streamProfile1.getFormat()).thenReturn("video/mpeg");
        when(streamProfile1.getSensorId()).thenReturn("testSensor");
        when(streamProfile1.getStreamPort()).thenReturn(new URI("rtp://225.1.2.3:10000"));
        StreamProfile streamProfile2 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid2");
        when(streamProfile2.getAsset()).thenReturn(testAsset);
        when(streamProfile2.getFormat()).thenReturn("video/mpeg");
        when(streamProfile2.getSensorId()).thenReturn("testSensor");
        when(streamProfile2.getStreamPort()).thenReturn(new URI("rtp://225.1.2.3:20000"));
        Set<StreamProfile> streamProfileSet = new HashSet<StreamProfile>();
        streamProfileSet.add(streamProfile1);
        streamProfileSet.add(streamProfile2);
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(streamProfileSet);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetStreamProfilesResponseData> messageCaptor = 
                ArgumentCaptor.forClass(GetStreamProfilesResponseData.class);
        verify(m_MessageFactory).createDataStreamServiceResponseMessage(eq(message), 
                eq(DataStreamServiceMessageType.GetStreamProfilesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetStreamProfilesResponseData response = messageCaptor.getValue();
        assertThat(response.getStreamProfileCount(), is(2));
        for (DataStreamServiceMessages.StreamProfile profile : response.getStreamProfileList())
        {
            if (profile.getInfo().getUuid().equals(
                    SharedMessageUtils.convertUUIDToProtoUUID(streamProfile1.getUuid())))
            {
                assertThat(profile.getInfo().getPid().equals("pid1"), is(true));
            }
            else
            {
                assertThat(profile.getInfo().getPid().equals("pid2"), is(true));
                assertThat(profile.getInfo().getUuid().equals(
                        SharedMessageUtils.convertUUIDToProtoUUID(streamProfile2.getUuid())), is(true));
            }
        }
    }
    
    /**
     * Test get stream profile request/response for the case where only those stream profiles associated
     * with a particular asset should be returned.
     */
    @Test
    public void testGetStreamProfilesByAssetUuid() throws IOException, URISyntaxException
    {
        
        Asset testAsset = FactoryObjectMocker.mockFactoryObject(Asset.class, "testAssetPid");
        Asset otherTestAsset = FactoryObjectMocker.mockFactoryObject(Asset.class, "otherAssetPid");
        
        GetStreamProfilesRequestData request = GetStreamProfilesRequestData.newBuilder()
                .setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(testAsset.getUuid())).build();
        
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder()
                .setType(DataStreamServiceMessageType.GetStreamProfilesRequest)
                .setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);

        StreamProfile streamProfile1 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid1");
        StreamProfile streamProfile2 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid2");
        StreamProfile streamProfile3 = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, "pid3");
        
        when(streamProfile1.getAsset()).thenReturn(testAsset);
        when(streamProfile1.getFormat()).thenReturn("video/mpeg");
        when(streamProfile1.getSensorId()).thenReturn("testSensor");
        when(streamProfile1.getStreamPort()).thenReturn(new URI("rtp://225.1.2.3:10000"));
        
        when(streamProfile2.getAsset()).thenReturn(testAsset);
        when(streamProfile2.getFormat()).thenReturn("video/mpeg");
        when(streamProfile2.getSensorId()).thenReturn("testSensor");
        when(streamProfile2.getStreamPort()).thenReturn(new URI("rtp://225.1.2.3:20000"));
        
        when(streamProfile3.getAsset()).thenReturn(otherTestAsset);
        when(streamProfile3.getFormat()).thenReturn("video/mpeg");
        when(streamProfile3.getSensorId()).thenReturn("testSensor");
        when(streamProfile3.getStreamPort()).thenReturn(new URI("rtp://225.1.2.3:30000"));
        
        Set<StreamProfile> allStreamProfileSet = new HashSet<StreamProfile>();
        allStreamProfileSet.add(streamProfile1);
        allStreamProfileSet.add(streamProfile2);
        allStreamProfileSet.add(streamProfile3);
        
        Set<StreamProfile> testAssetStreamProfileSet = new HashSet<StreamProfile>();
        testAssetStreamProfileSet.add(streamProfile1);
        testAssetStreamProfileSet.add(streamProfile2);
        
        when(m_AssetDirectoryService.getAssetByUuid(testAsset.getUuid())).thenReturn(testAsset);
        when(m_DataStreamService.getStreamProfiles()).thenReturn(allStreamProfileSet);
        when(m_DataStreamService.getStreamProfiles(testAsset)).thenReturn(testAssetStreamProfileSet);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetStreamProfilesResponseData> messageCaptor = 
                ArgumentCaptor.forClass(GetStreamProfilesResponseData.class);
        verify(m_MessageFactory).createDataStreamServiceResponseMessage(eq(message), 
                eq(DataStreamServiceMessageType.GetStreamProfilesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetStreamProfilesResponseData response = messageCaptor.getValue();
        assertThat(response.getStreamProfileCount(), is(2));
        for (DataStreamServiceMessages.StreamProfile profile : response.getStreamProfileList())
        {
            // Assert that each stream profile returned is associated with testAsset
            assertThat(profile.getAssetUuid(), 
                    is(SharedMessageUtils.convertUUIDToProtoUUID(testAsset.getUuid())));            
        }
    }
    
    /**
     * Test the get capabilities request/response message system for stream profiles.  
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify capabilities are gotten from the stream profile factory.
     *      Verify response (containing correct data) is sent after completing request. 
     */
    @Test
    public void testGetCapabilities() throws IOException, ObjectConverterException
    {
        m_SUT.activate();
        
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
                setProductType(StreamProfileProxy.class.getName()).build();
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.GetCapabilitiesRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);

        Message capsGen = StreamProfileCapabilitiesGen.StreamProfileCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                        setManufacturer("Worldwide").
                        setDescription("Streams").
                        setProductName("The Stream Profile").build()).
                addFormat(StreamTypesGen.StreamFormat.newBuilder().
                        setMimeFormat("video/mpeg").build()).
                setMinBitrateKbps(1.0).
                setMaxBitrateKbps(1000.0).build();
        
        StreamProfileFactory factory = mock(StreamProfileFactory.class);
        StreamProfileCapabilities capabilities = mock(StreamProfileCapabilities.class);
        Set<StreamProfileFactory> factories = new HashSet<>();
        factories.add(factory);
        doReturn(StreamProfileProxy.class.getName()).when(factory).getProductType();
        when(m_DataStreamService.getStreamProfileFactories()).thenReturn(factories);
        when(factory.getStreamProfileCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).thenReturn(capsGen);
                        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);        
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetCapabilitiesRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
                
        //capture and verify response
        ArgumentCaptor<GetCapabilitiesResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetCapabilitiesResponseData.class);
        verify(m_MessageFactory).createDataStreamServiceResponseMessage(eq(message),
                eq(DataStreamServiceMessageType.GetCapabilitiesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetCapabilitiesResponseData response = messageCaptor.getValue();
        
        assertThat(response.hasCapabilities(), is(true));
        assertThat(response.getCapabilities(), is(capsGen));
        assertThat(response.getProductType(), is(StreamProfileProxy.class.getName()));        
    }
    
    /**
     * Test the enable stream profile request/response remote message system for the datastream service.
     * Specifically, the following behaviors are tested: 
     * Verify incoming request message is posted to event admin.
     * Verify stream profile is enabled on request.
     * Verify response is sent after completing request to enable stream profile. 
     */
    @Test
    public void testEnableStreamProfile() throws IOException
    {       
        EnableStreamProfileRequestData request = EnableStreamProfileRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testProfileUuid)).build();
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.EnableStreamProfileRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);
        
        StreamProfile profile = mock(StreamProfile.class);
        
        when(m_DataStreamService.getStreamProfile(testProfileUuid)).thenReturn(profile);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();        
        assertThat((EnableStreamProfileRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(profile).setEnabled(true);
        verify(m_MessageFactory).createDataStreamServiceResponseMessage(eq(message),
                eq(DataStreamServiceMessageType.EnableStreamProfileResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the enable stream profile response message sends an event with null data message.
     */
    @Test
    public void testEnableStreamProfileResponse() throws IOException
    {
        DataStreamServiceNamespace datastreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.EnableStreamProfileResponse).build();
        
        TerraHarvestPayload payload = createPayload(datastreamMessage);
        TerraHarvestMessage message = createMessage(datastreamMessage);
        
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
                is(DataStreamServiceMessageType.EnableStreamProfileResponse.toString()));
    }
    
    /**
     * Test the disable stream profile request/response remote message system for the datastream service.
     * Specifically, the following behaviors are tested: 
     * Verify incoming request message is posted to event admin.
     * Verify stream profile is disabled on request.
     * Verify response is sent after completing request to enable stream profile. 
     */
    @Test
    public void testDisableStreamProfile() throws IOException
    {
        DisableStreamProfileRequestData request = DisableStreamProfileRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testProfileUuid)).build();
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.DisableStreamProfileRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);
        
        StreamProfile profile = mock(StreamProfile.class);
        
        when(m_DataStreamService.getStreamProfile(testProfileUuid)).thenReturn(profile);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();        
        assertThat((DisableStreamProfileRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(profile).setEnabled(false);
        verify(m_MessageFactory).createDataStreamServiceResponseMessage(eq(message),
                eq(DataStreamServiceMessageType.DisableStreamProfileResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the disable stream profile response message sends an event with null data message.
     */
    @Test
    public void testDisableStreamProfileResponse() throws IOException
    {
        DataStreamServiceNamespace datastreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.DisableStreamProfileResponse).build();
        
        TerraHarvestPayload payload = createPayload(datastreamMessage);
        TerraHarvestMessage message = createMessage(datastreamMessage);
        
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
                is(DataStreamServiceMessageType.DisableStreamProfileResponse.toString()));
    }
    
    /**
     * Test and verify that illegal argument exception is handled properly and that the error message is queued.
     */
    @Test
    public final void testIllegalArgumentException() throws IOException
    {
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
                setProductType("bad.factory").build();
        DataStreamServiceNamespace dataStreamMessage = DataStreamServiceNamespace.newBuilder().
                setType(DataStreamServiceMessageType.GetCapabilitiesRequest).
                setData(request.toByteString()).build();
        
        TerraHarvestPayload payload = createPayload(dataStreamMessage);
        TerraHarvestMessage message = createMessage(dataStreamMessage);
        
        RemoteChannel channel =  mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE,
                "Cannot complete request. Factory not found.");
        
        verify(m_ResponseWrapper).queue(channel);
    }
    
    private TerraHarvestMessage createMessage(DataStreamServiceNamespace dataStreamMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.DataStreamService, 3, 
                dataStreamMessage);
    }
    private TerraHarvestPayload createPayload(DataStreamServiceNamespace dataStreamMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.DataStreamService).
               setNamespaceMessage(dataStreamMessage.toByteString()).
               build();
    }
}
