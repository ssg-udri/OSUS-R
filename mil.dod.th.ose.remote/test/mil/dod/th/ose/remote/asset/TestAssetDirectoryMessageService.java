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
package mil.dod.th.ose.remote.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetScannableAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.FactoryObjectMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * @author Admin
 *
 */
public class TestAssetDirectoryMessageService
{
    private AssetDirectoryMessageService m_SUT;
    private UUID testUuid = UUID.randomUUID();
    private AssetDirectoryService m_AssetDirectoryService;
    private LoggingService m_Logging;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageRouterInternal m_MessageRouter;
    private FactoryObjectInfo testInfo;
    private MessageResponseWrapper m_ResponseWrapper;
    private JaxbProtoObjectConverter m_Converter;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new AssetDirectoryMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_AssetDirectoryService = mock(AssetDirectoryService.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_Converter =  mock(JaxbProtoObjectConverter.class);
        
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);  
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        
        testInfo = FactoryObjectInfo.newBuilder().
                setProductType(Asset.class.getName()).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).
                setPid("PID").build();

        when(m_MessageFactory.createAssetDirectoryServiceResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(AssetDirectoryServiceMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }
    
    /**
     * Verify the namespace is AssetDirectoryService
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.AssetDirectoryService));
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
     * Verify generic handling of message, that it posts an event locally.
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
               setProductType("a").build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = mock(Asset.class);
        when(this.m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        when(asset.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        
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
                is(assetDirMessage.getType().toString()));
        assertThat((AssetDirectoryServiceNamespace)postedEvent.
                getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), is(assetDirMessage));
        assertThat((GetCapabilitiesRequestData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(request));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));      
    }
    
    /**
     * Test the get asset types request/response remote message system for the asset directory service.  
     * Specifically, the following behaviors are tested: 
     *      Verify asset name and type are returned.
     *      Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public void testGetAssetTypes() throws IOException
    {
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetAssetTypesRequest).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        AssetFactory factory = mock(AssetFactory.class);
        doReturn(AssetProxy.class.getName()).when(factory).getProductType();
        when(factory.getProductName()).thenReturn("Asset");
        Set<AssetFactory> factorySet = new HashSet<>();
        factorySet.add(factory);
        when(this.m_AssetDirectoryService.getAssetFactories()).thenReturn(factorySet);
               
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetAssetTypesResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetAssetTypesResponseData.class); 
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.GetAssetTypesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        GetAssetTypesResponseData response = messageCaptor.getValue();
        
        assertThat(response.getProductNameCount(), is(1));
        assertThat(response.getProductTypeCount(), is(1));
        assertThat(response.getProductName(0), is(Asset.class.getSimpleName()));
        assertThat(response.getProductType(0), is(AssetProxy.class.getName()));
    }

    /**
     * Test the scan for new assets request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify new assets are scanned for upon request (with and without specified asset type).
     *      Verify response is sent after request.
     */
    @Test
    public void testScanForNewAssets() throws IOException
    {
        ScanForNewAssetsRequestData request = ScanForNewAssetsRequestData.newBuilder().build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
    
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ScanForNewAssetsRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(m_AssetDirectoryService).scanForNewAssets();
        
        //capture and verify response
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
        
        //New request with set product type
        AssetFactory factory = mock(AssetFactory.class);
        Set<AssetFactory> factorySet = new HashSet<>();
        factorySet.add(factory);
        
        when(m_AssetDirectoryService.getAssetFactories()).thenReturn(factorySet);
        doReturn(AssetProxy.class.getName()).when(factory).getProductType();
        
        request = ScanForNewAssetsRequestData.newBuilder().setProductType(AssetProxy.class.getName()).build();
        assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest).
                setData(request.toByteString()).build();
        payload = createPayload(assetDirMessage);
        message = createMessage(assetDirMessage); 
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, Mockito.times(2)).postEvent(eventCaptor.capture());
        event = eventCaptor.getValue();
        assertThat((ScanForNewAssetsRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(m_AssetDirectoryService).scanForNewAssets(AssetProxy.class.getName());
        
        //capture and verify response
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse), Mockito.any(Message.class));
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Verify the scan for new assets response message sends an event with null data message.
     */
    @Test
    public void testScanForNewAssetsResponse() throws IOException
    {
        AssetDirectoryServiceNamespace assetDirectoryMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(assetDirectoryMessage);
        TerraHarvestMessage message = createMessage(assetDirectoryMessage);
        
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
                is(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse.toString()));
    }
    
    /**
     * Test the create asset request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset is created upon request.
     *      Verify response is sent after completing request.
     */
    @Test
    public void testCreateAsset() throws IllegalArgumentException, AssetException, IOException
    {
        CreateAssetRequestData request = CreateAssetRequestData.newBuilder().
                setProductType("my.FQ.className").build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = FactoryObjectMocker.mockFactoryObject(Asset.class, "PID");
        when(this.m_AssetDirectoryService.createAsset(Mockito.anyString(), Mockito.anyString())).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CreateAssetRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<CreateAssetResponseData> messageCaptor = ArgumentCaptor.forClass(CreateAssetResponseData.class); 
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.CreateAssetResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CreateAssetResponseData response = messageCaptor.getValue();
        
        //assert that uuid in response matches expected uuid
        assertThat(response.getInfo().getUuid().getLeastSignificantBits(), 
                is(asset.getUuid().getLeastSignificantBits()));
        assertThat(response.getInfo().getUuid().getMostSignificantBits(), 
                is(asset.getUuid().getMostSignificantBits()));
        assertThat(response.getInfo().getPid(), is("PID"));  
    }
    
    /**
     * Test the create asset request/response remote message system for the asset directory service.
     * Specifically, this test verifies that the asset name is set and passed to the core service correctly
     * when provided in the message.
     */
    @Test
    public void testCreateAssetWithName() throws IllegalArgumentException, AssetException, IOException
    {
        CreateAssetRequestData request = CreateAssetRequestData.newBuilder().
                setProductType("my.FQ.className").setName("TheBestName").build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = FactoryObjectMocker.mockFactoryObject(Asset.class, "PID");
        when(this.m_AssetDirectoryService.createAsset(Mockito.anyString(), Mockito.anyString())).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CreateAssetRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<CreateAssetResponseData> messageCaptor = ArgumentCaptor.forClass(CreateAssetResponseData.class); 
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.CreateAssetResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CreateAssetResponseData response = messageCaptor.getValue();
        
        //assert that uuid in response matches expected uuid
        assertThat(response.getInfo().getUuid().getLeastSignificantBits(), 
                is(asset.getUuid().getLeastSignificantBits()));
        assertThat(response.getInfo().getUuid().getMostSignificantBits(), is(asset.getUuid().getMostSignificantBits()));
        assertThat(response.getInfo().getPid(), is("PID"));  
    }
    
    /**
     * Test the create asset request/response remote message system for the asset directory service.
     * Specifically, this test verifies that the asset properties specified are passed to the core service correctly
     * when provided in the message.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCreateAssetWithProperties() throws IllegalArgumentException, AssetException, IOException
    {
        SimpleTypesMapEntry property = SimpleTypesMapEntry.newBuilder().setKey("propA").setValue(
                Multitype.newBuilder().setType(Type.BOOL).setBoolValue(true).build()).build();
        CreateAssetRequestData request = CreateAssetRequestData.newBuilder().
                setProductType("my.FQ.className").setName("test").addProperties(property).build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = FactoryObjectMocker.mockFactoryObject(Asset.class, "PID");
        when(m_AssetDirectoryService.createAsset(Mockito.anyString(), Mockito.anyString(), 
                Mockito.anyMap())).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify correct create asset method was called with appropriate properties
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(m_AssetDirectoryService).createAsset(eq("my.FQ.className"), eq("test"), mapCaptor.capture());
        Map<String, Object> properties = mapCaptor.getValue();
        assertThat(properties.containsKey("propA"), is(true));
        assertThat((boolean)properties.get("propA"), is(true));

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CreateAssetRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<CreateAssetResponseData> messageCaptor = ArgumentCaptor.forClass(CreateAssetResponseData.class); 
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.CreateAssetResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CreateAssetResponseData response = messageCaptor.getValue();
        
        //assert that uuid in response matches expected uuid
        assertThat(response.getInfo().getUuid().getLeastSignificantBits(), 
                is(asset.getUuid().getLeastSignificantBits()));
        assertThat(response.getInfo().getUuid().getMostSignificantBits(), 
                is(asset.getUuid().getMostSignificantBits()));
        assertThat(response.getInfo().getPid(), is("PID"));  
    }
    
    /**
     * Test get assets request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     *      Verify list of assets are returned.
     *      Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public void testGetAssets() throws IOException
    {
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetAssetsRequest).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset1 = FactoryObjectMocker.mockFactoryObject(Asset.class, "IamApid");
        Asset asset2 = FactoryObjectMocker.mockFactoryObject(Asset.class, "IamApidTOOO");
        Set<Asset> assetSet = new HashSet<Asset>();
        assetSet.add(asset1);
        assetSet.add(asset2);
        
        when(m_AssetDirectoryService.getAssets()).thenReturn(assetSet);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        ArgumentCaptor<GetAssetsResponseData> messageCaptor = ArgumentCaptor.forClass(GetAssetsResponseData.class); 
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.GetAssetsResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetAssetsResponseData response = messageCaptor.getValue();
        assertThat(response.getAssetInfoCount(), is(2));
        for (FactoryObjectInfo info : response.getAssetInfoList())
        {
            if (info.getUuid().equals(SharedMessageUtils.convertUUIDToProtoUUID(asset1.getUuid())))
            {
                assertThat(info.getPid().equals("IamApid"), is(true));
            }
            else
            {
                assertThat(info.getPid().equals("IamApidTOOO"), is(true));
                assertThat(info.getUuid().equals(SharedMessageUtils.convertUUIDToProtoUUID(asset2.getUuid())), 
                        is(true));
            }
        }
    }
    
    /**
     * Verify create asset response message when handled will set the data event property.
     */
    @Test
    public void testCreateAssetResponse() throws IOException
    {
        Message response = CreateAssetResponseData.newBuilder().
                setInfo(testInfo).build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
    
    /**
     * Verify get capabilities response message when handled will set the data event property.
     */
    @Test
    public final void testGetCapabilitiesResponse() throws IOException
    {
        Message capsGen = AssetCapabilitiesGen.AssetCapabilities.newBuilder().
                setNominalFov(22.0).
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("teamAwesome").
                    setDescription("does stuff").
                    setProductName("Bill")).
                build();
        Message response = GetCapabilitiesResponseData.newBuilder().
                setCapabilities((AssetCapabilitiesGen.AssetCapabilities)capsGen).
                setProductType(AssetProxy.class.getName()).build();
        AssetDirectoryServiceNamespace assetMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);


        RemoteChannel channel = mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
   
    /**
     * Verify get assets response message when handled will set the data event property. 
     */
    @Test
    public void testGetAssetsResponse() throws IOException
    {
        Message response = GetAssetsResponseData.newBuilder().
                addAssetInfo(testInfo).build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetAssetsResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));        
    }
    
    /**
     * Verify get asset types response message when handled will set the data event property. 
     */
    @Test
    public void testGetAssetTypesResponse() throws IOException
    {
        Message response = GetAssetTypesResponseData.newBuilder().
                addProductName("oatmeal").addProductType("cookies").build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetAssetTypesResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));       
    }
    
    /**
     * Test and verify that asset exception is caught and error message is queued.
     */
    @Test
    public void testAssetException() throws IllegalArgumentException, FactoryException, IOException
    {
        ScanForNewAssetsRequestData request = ScanForNewAssetsRequestData.newBuilder().
                setProductType("myProductType").build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);

        doThrow(new IllegalArgumentException("Factory not found for given class name"))
            .when(m_AssetDirectoryService).scanForNewAssets("myProductType");

        RemoteChannel channel = mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);    
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.ASSET_ERROR, 
                "Cannot complete request. Factory not found for given class name");
        verify(m_ResponseWrapper).queue(channel);
        
        //Create asset request exception
        CreateAssetRequestData request2 = CreateAssetRequestData.newBuilder().
                setProductType("my.FQ.className").build();
        assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request2.toByteString()).build();
        payload = createPayload(assetDirMessage);
        message = createMessage(assetDirMessage);
        
        when(m_AssetDirectoryService.createAsset(Mockito.anyString(), Mockito.anyString())).
            thenThrow(new AssetException("create asset error"));
        
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.ASSET_ERROR, 
                "Cannot complete request. create asset error");
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }

    /**
     * Verify handling of request for scannable asset types.
     * Verify event posted when message is received.
     * Verify response with list of product types.
     */
    @Test
    public void testGetScannableAssetTypes() throws IOException
    {
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetScannableAssetTypesRequest).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //mock out the asset dir svc response
        Set<String> clazzes = new HashSet<>();
        clazzes.add(AssetProxy.class.getName());
        when(m_AssetDirectoryService.getScannableAssetTypes()).thenReturn(clazzes);
    
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetDirectoryServiceMessageType.GetScannableAssetTypesRequest.toString()));
        verify(m_AssetDirectoryService).getScannableAssetTypes();
        
        //capture and verify response
        ArgumentCaptor<GetScannableAssetTypesResponseData> messageCap = ArgumentCaptor.
                forClass(GetScannableAssetTypesResponseData.class);
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.GetScannableAssetTypesResponse), messageCap.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetScannableAssetTypesResponseData responseData = messageCap.getValue();
        List<String> classes = responseData.getScannableAssetTypeList();
        assertThat(classes, hasItem(AssetProxy.class.getName()));
    }
    
    /**
     * Verify event posted with scannable asset types response is received.
     */
    @Test
    public void testGetScannableAssetTypesResponse() throws IOException
    {
        GetScannableAssetTypesResponseData data = GetScannableAssetTypesResponseData.newBuilder().
                addScannableAssetType(AssetProxy.class.getName()).build();
        AssetDirectoryServiceNamespace assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetScannableAssetTypesResponse).
                setData(data.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetDirectoryServiceMessageType.GetScannableAssetTypesResponse.toString()));
        assertThat((GetScannableAssetTypesResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(data));
    }
    /**
     * Test and verify that object converter exception is caught and error message is queued
     */
    @Test
    public final void testCatchObjectConverterException() throws IOException, ObjectConverterException
    {   
        //set up converter
        m_SUT.activate();
        
        //mock channel
        RemoteChannel channel = mock(RemoteChannel.class);

        // build request
        GetCapabilitiesRequestData request3 = GetCapabilitiesRequestData.newBuilder().
                setProductType(AssetProxy.class.getName()).build();     
        AssetDirectoryServiceNamespace assetMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesRequest).
                setData(request3.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        AssetCapabilities capabilities = mock(AssetCapabilities.class);
        AssetFactory factory = mock(AssetFactory.class);
        Set<AssetFactory> factories = new HashSet<>();
        factories.add(factory);
        doReturn(AssetProxy.class.getName()).when(factory).getProductType();
        when(m_AssetDirectoryService.getAssetFactories()).thenReturn(factories);
        when(factory.getCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(Mockito.anyObject())).
            thenThrow(new ObjectConverterException("convert error"));
        
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Cannot complete request. convert error");
        //reused channel
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test and verify that illegal argument exception is handled properly and that the error message is queued.
     */
    @Test
    public final void testIllegalArgumentException() throws IOException
    {
        //build request
        GetCapabilitiesRequestData request6 = GetCapabilitiesRequestData.newBuilder().
                setProductType("bad.factory").build();
        AssetDirectoryServiceNamespace assetMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesRequest).
                setData(request6.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        RemoteChannel channel =  mock(RemoteChannel.class);
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE,
                "Cannot complete request. Factory not found.");
        //reused channel
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test the get capabilities request/response message system for assets.  
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify capabilities are gotten from the asset factory.
     *      Verify response (containing correct data) is sent after completing request. 
     */
    @Test
    public final void testGetCapabilities() throws IOException, ObjectConverterException
    {
        //set up converter
        m_SUT.activate();
        
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
                setProductType(AssetProxy.class.getName()).build();
        AssetDirectoryServiceNamespace assetMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);


        Message capsGen = AssetCapabilitiesGen.AssetCapabilities.newBuilder().
                setNominalFov(22.0).
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setManufacturer("teamAwesome").
                    setDescription("does stuff").
                    setProductName("Bill")).
                build();

        //mock necessary objects/actions
        AssetFactory factory = mock(AssetFactory.class);
        AssetCapabilities capabilities = mock(AssetCapabilities.class);
        Set<AssetFactory> factories = new HashSet<>();
        factories.add(factory);
        doReturn(AssetProxy.class.getName()).when(factory).getProductType();
        when(m_AssetDirectoryService.getAssetFactories()).thenReturn(factories);
        when(factory.getAssetCapabilities()).thenReturn(capabilities);
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
        verify(m_MessageFactory).createAssetDirectoryServiceResponseMessage(eq(message), 
                eq(AssetDirectoryServiceMessageType.GetCapabilitiesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetCapabilitiesResponseData response = messageCaptor.getValue();

        // ensure all necessary fields are set
        assertThat(response.hasCapabilities(), is(true));
        assertThat(response.getCapabilities(), is(capsGen));
        assertThat(response.getProductType(), is(AssetProxy.class.getName()));
    }
    
    private TerraHarvestMessage createMessage(AssetDirectoryServiceNamespace assetDirMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.AssetDirectoryService, 3, 
                assetDirMessage);
    }
    private TerraHarvestPayload createPayload(AssetDirectoryServiceNamespace assetDirMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.AssetDirectoryService).
               setNamespaceMessage(assetDirMessage.toByteString()).
               build();
    }
}
