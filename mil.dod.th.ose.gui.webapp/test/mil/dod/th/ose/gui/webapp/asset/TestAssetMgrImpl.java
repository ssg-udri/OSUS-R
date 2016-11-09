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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.LocationModel;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigPropModelImpl;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModelFactory;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.AssetExecuteErrorHandler;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.EventHelperAssetDirNamespace;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.EventHelperAssetNamespace;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.EventHelperControllerEvent;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.EventHelperEventAdminNamespace;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.RemoteCreateAssetHandler;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.RemoteSetAssetNameHandler;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.SummaryStatusEnumConverter;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen.Response;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionCommandGen.SetPositionCommand;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.observation.types.StatusGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.Version;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.AzimuthDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.BankDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.ElevationDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HaeMeters;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HeadingDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Orientation;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.OrientationOffset;
import mil.dod.th.remote.lexicon.types.status.StatusTypesGen.OperatingStatus;

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
 * Test class for the asset manager implementation.
 * Note that the actual interface methods are tested indirectly.
 * 
 * @author callen
 */
public class TestAssetMgrImpl 
{
    private AssetMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private GrowlMessageUtil m_GrowlUtil;
    private BundleContextUtil m_BundleUtil;
    private EventHelperEventAdminNamespace m_EventHelper;
    private EventHelperAssetNamespace m_AssetHelper;
    private EventHelperAssetDirNamespace m_AssetDirHelper;
    private EventHelperControllerEvent m_ControllerEvent;
    private BundleContext m_BundleContext;
    private EventAdmin m_EventAdmin;
    private ConfigurationWrapper m_ConfigWrapper;
    private AssetTypesMgr m_AssetTypesMgr;
    private MessageWrapper m_MessageWrapper;
    private AssetImage m_AssetImageInterface;
    private JaxbProtoObjectConverter m_Converter;
    
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);
    
    //these fields are at this level so they can be used in multiple messages
    //system ids
    private int systemId1 = 123;
    private int systemId2 = 1234;
    //pids
    private String pid1 = "PITTER";
    private String pid2 = "PATTER";
    //UUIDs
    private UUID uuid1 = UUID.randomUUID();
    private UUID uuid2 = UUID.randomUUID();
    
    @SuppressWarnings({ "unchecked" })//because of the use of the dictionary for the event helper
    @Before
    public void setUp()
    {
        //mock services
        m_AssetTypesMgr = mock(AssetTypesMgr.class);
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_BundleContext = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_AssetImageInterface = mock(AssetImage.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        
        //create asset manager
        m_SUT = new AssetMgrImpl();
        
        //set dependencies
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        m_SUT.setAssetTypesMgr(m_AssetTypesMgr);
        m_SUT.setAssetImageInterface(m_AssetImageInterface);
        m_SUT.setConverter(m_Converter);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createAssetMessage(Mockito.any(AssetMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createAssetDirectoryServiceMessage(Mockito.any(AssetDirectoryServiceMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //register helper
        m_SUT.registerEventHelpers();
        
        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(4)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(4)).getBundleContext();
        
        //enter classes
        m_EventHelper = (EventHelperEventAdminNamespace) captor.getAllValues().get(0);
        m_AssetHelper = (EventHelperAssetNamespace) captor.getAllValues().get(1);
        m_AssetDirHelper = (EventHelperAssetDirNamespace) captor.getAllValues().get(2);
        m_ControllerEvent = (EventHelperControllerEvent) captor.getAllValues().get(3);
    }
    
    /**
     * Test that each Event Helper has correctly registered for the appropriate topics and
     * filters.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testTopicAndFilterRegistration()
    {
        ArgumentCaptor<Dictionary> dictCaptor = ArgumentCaptor.forClass(Dictionary.class);
        
        verify(m_BundleContext, times(4)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                dictCaptor.capture());

        //get dictionaries
        Dictionary eventHelperDict = dictCaptor.getAllValues().get(0);
        Dictionary assetHelperDict = dictCaptor.getAllValues().get(1);
        Dictionary assetDirHelperDict = dictCaptor.getAllValues().get(2);
        
        //Test Asset EventHelper
        Object dictObj = assetHelperDict.get(EventConstants.EVENT_TOPIC);
        verifyMessageReceivedEventRegistration(dictObj);

        dictObj = assetHelperDict.get(EventConstants.EVENT_FILTER);
        String filter = String.format("(%s=%s)", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        assertThat(dictObj.equals(filter), is(true));
        
        //Test AssetDir EventHelper
        dictObj = assetDirHelperDict.get(EventConstants.EVENT_TOPIC);
        verifyMessageReceivedEventRegistration(dictObj);

        dictObj = assetDirHelperDict.get(EventConstants.EVENT_FILTER);
        filter = String.format("(%s=%s)", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        assertThat(dictObj.equals(filter), is(true));
        
        //Test EventAdmin EventHelper
        dictObj = eventHelperDict.get(EventConstants.EVENT_TOPIC);
        
        String[] topics = (String[]) dictObj;
        assertThat(topics[0].equals(Asset.TOPIC_DATA_CAPTURED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[1].equals(Asset.TOPIC_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[2].equals(Asset.TOPIC_WILL_BE_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[3].equals(Asset.TOPIC_ACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[4].equals(Asset.TOPIC_ACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[5].equals(Asset.TOPIC_WILL_BE_DEACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[6].equals(Asset.TOPIC_DEACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[7].equals(Asset.TOPIC_DEACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX), is(true));
        assertThat(topics[8].equals(FactoryDescriptor.TOPIC_PREFIX + "*"), is(true));
        
        dictObj = eventHelperDict.get(EventConstants.EVENT_FILTER);
        filter = String.format("(%s=*)", RemoteConstants.REMOTE_EVENT_PROP);
        assertThat(dictObj.equals(filter), is(true));
    }
    
    /***
     * Generic test to verify the Event Helper is registered to receive "Message Received" events
     */
    private void verifyMessageReceivedEventRegistration(Object dictObj)
    {
        assertThat(dictObj.toString(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));    
    }
    
    /**
     * Test the predestroy unregistering of event handlers.
     * Verify that all 6 are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterHelpers();

        //verify listeners are unregistered, there are 5
        verify(m_HandlerReg, times(4)).unregister();
    }
    
    /**
     * Test the handling of remote response handler.
     * Verify the handler send unreg requests.
     */
    @Test
    public void testResponseHandler()
    {
        RemoteEventRegistrationHandler handler = new RemoteEventRegistrationHandler(m_MessageFactory);
        
        //response messages to process
        EventRegistrationResponseData response1 = EventRegistrationResponseData.newBuilder().
            setId(1).build();
        EventRegistrationResponseData response2 = EventRegistrationResponseData.newBuilder().
            setId(2).build();

        EventAdminNamespace nameResponse1 = EventAdminNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(EventAdminMessageType.EventRegistrationResponse).build();
        EventAdminNamespace nameResponse2 = EventAdminNamespace.newBuilder().
            setData(response2.toByteString()).
            setType(EventAdminMessageType.EventRegistrationResponse).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.EventAdmin).
            setNamespaceMessage(response1.toByteString()).build();
        TerraHarvestPayload payload2 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.EventAdmin).
                setNamespaceMessage(response2.toByteString()).build();    
            
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.EventAdmin, 123, nameResponse1);

        TerraHarvestMessage thMessage2 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId2, 0, 
            Namespace.EventAdmin, 321, nameResponse2);
        
        //this will load the registration ids into the response handler
        handler.handleResponse(thMessage1, payload1, nameResponse1, response1);
        handler.handleResponse(thMessage2, payload2, nameResponse2, response2);
        
        //verify that handler will send unregister requests
        handler.unregisterRegistrations();
        verify(m_MessageFactory, times(2)).createEventAdminMessage(eq(EventAdminMessageType.UnregisterEventRequest),
            Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(systemId1), (ResponseHandler) eq(null));
        verify(m_MessageWrapper).queue(eq(systemId2), (ResponseHandler) eq(null));
    }

    /**
     * Test the handling of remote response handler.
     * Verify the handler for remote events can send unregister requests (these are for remote events) for just
     * a specific controller.
     */
    @Test
    public void testResponseHandlerRemovalOfSingleController()
    {
        RemoteEventRegistrationHandler handler = new RemoteEventRegistrationHandler(m_MessageFactory);
        
        //response messages to process
        EventRegistrationResponseData response1 = EventRegistrationResponseData.newBuilder().
            setId(1).build();
        EventRegistrationResponseData response2 = EventRegistrationResponseData.newBuilder().
            setId(2).build();

        EventAdminNamespace nameResponse1 = EventAdminNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(EventAdminMessageType.EventRegistrationResponse).build();
        EventAdminNamespace nameResponse2 = EventAdminNamespace.newBuilder().
            setData(response2.toByteString()).
            setType(EventAdminMessageType.EventRegistrationResponse).build();
        
        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.EventAdmin).
                setNamespaceMessage(response1.toByteString()).build();
        TerraHarvestPayload payload2 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.EventAdmin).
                     setNamespaceMessage(response2.toByteString()).build();    

        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.EventAdmin, 123, nameResponse1);

        TerraHarvestMessage thMessage2 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId2, 0, 
            Namespace.EventAdmin, 321, nameResponse2);
        
        //this will load the registration ids into the response handler
        handler.handleResponse(thMessage1, payload1, nameResponse1, response1);
        handler.handleResponse(thMessage2, payload2, nameResponse2, response2);
    }
    
    /**
     * Test the handling of remote object created event message.
     * Verify the get assets request is sent.
     */
    @Test
    public void testHandleRemoteEventCreatedObject()
    {
        //mock event that a factory object was created for this new system
        Event objectCreated = mockEventAssetCreated(systemId2);
        
        m_EventHelper.handleEvent(objectCreated);
        
        //verify request for assets
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.GetAssetsRequest),Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId2), (ResponseHandler) eq(null));
        
        //verify request for asset info
        verify(m_MessageFactory).createAssetMessage(
                eq(AssetMessageType.GetActiveStatusRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId2), (ResponseHandler) eq(null));
        verify(m_MessageFactory).createAssetMessage(
                eq(AssetMessageType.GetNameRequest),
                Mockito.any(Message.class));
        verify(m_MessageFactory,times(1)).createAssetMessage(eq(AssetMessageType.GetLastStatusRequest), 
               Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId2), (ResponseHandler) eq(null));
    }
    
    /**
     * Test the handling of remote object deleted event message.
     * Verify the appropriate asset is removed.
     */
    @Test
    public void testHandleRemoteEventFactoryObjectDeleted()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        //mock event that a factory object was deleted for this system, should removed asset with uuid2
        Event objectDeleted = mockEventAssetDeleted(systemId1);
        
        m_EventHelper.handleEvent(objectDeleted);
        
        //verify asset removed
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getUuid(), is(uuid1));
        //verify user notification
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset removed.",
             String.format("An asset from controller 0x%08x was removed.", systemId1));
        
        //replay, send message again... nothing should happen
        m_EventHelper.handleEvent(objectDeleted);
        
        //verify asset removed
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getUuid(), is(uuid1));
    }
    
    /**
     * Verify that an object's pid can be updated.
     */
    @Test
    public void testHandleRemoteAssetEventFactoryObjPidCreated()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        //create a status
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PIDGEON");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId1);
        Event event = new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED + 
                RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_EventHelper.handleEvent(event);
        
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        boolean found = false;
        for(AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getPid(), is("PIDGEON"));
                found = true;
            }
        }
        
        assertThat(found, is(true));    
    }
    
    /**
     * Verify that a pid can be set to the empty string when a configuration is 
     * removed.
     */
    @Test
    public void testHandleRemoteAssetEventFactoryObjPidDeleted()
    {
      //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        //create a status
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PIDGEON");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId1);
        Event event = new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED + 
                RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_EventHelper.handleEvent(event);
        
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        boolean found = false;
        for(AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getPid(), is("PIDGEON"));
                found = true;
            }
        }
        
        assertThat(found, is(true));
        
        props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId1);
        event = new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED + 
                RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_EventHelper.handleEvent(event);
        
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        found = false;
        for(AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getPid(), is(""));
                found = true;
            }
        }
        
        assertThat(found, is(true));
    }
    
    /*
     * Verify that nothing is done if the model the event is for is not known.
     */
    @Test
    public void testHandleRemoteAssetEventForAssetThatAssetMgrDoesNotHave()
    {
        //create a status
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, 1);
        Event event = new Event(Asset.TOPIC_ACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_EventHelper.handleEvent(event);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Verify AssetDirectoryService.TOPIC_ASSET_WILL_BE_ACTIVATED produces a push event.
     */
    @Test
    public void testHandleRemoteAssetEventWithWillBeActivatedTopic()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        m_EventHelper.handleEvent(mockEventAssetWillBeActivated(1));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(captor.capture());
        
        Event foundEvent = captor.getValue();
        
        assertThat(foundEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)foundEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
        assertThat((String)foundEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.ACTIVATING.toString()));
    }
    
    /**
     * Verify AssetDirectoryService.TOPIC_ASSET_WILL_BE_DEACTIVATED produces a push event.
     */
    @Test
    public void testHandleRemoteAssetEventWithWillBeDeactivatedTopic()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        m_EventHelper.handleEvent(mockEventAssetWillBeDeactivated(1));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(captor.capture());
        
        Event foundEvent = captor.getValue();
        
        assertThat(foundEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)foundEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
        assertThat((String)foundEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.DEACTIVATING.toString()));
    }
    
    /**
     * Verify request is not sent out when no model exists.
     * Verify set asset property request is sent out for a model that is found.
     */
    @Test
    public void testCreateConfiguration()
    {
      //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(1).size(), is(2));
        
        List<ModifiablePropertyModel> configProps = new ArrayList<ModifiablePropertyModel>();
        
        ConfigPropModelImpl model = ModelFactory.createPropModel("id", "football");
        configProps.add(model);
        AssetModel assetModel = m_SUT.getAssetModelByUuid(uuid2, 1);
        m_SUT.createConfiguration(1, assetModel, configProps);
        
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.SetPropertyRequest), msgCaptor.capture());
        
        SetPropertyRequestData data = (SetPropertyRequestData)msgCaptor.getValue();
        assertThat(data, notNullValue());
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid2)));
        assertThat(data.getPropertiesCount(), is(1));
        
        SimpleTypesMapEntry type = data.getProperties(0);
        
        assertThat(type.getKey(), is("id"));
        assertThat(type.getValue().getStringValue(), is("football"));
    }
    
    /**
     * Test the handling of data captured event.
     * Verify growl message.
     */
    @Test
    public void testHandleRemoteEventDataCaptured()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        //mock event that an asset captured data
        Event objectCapturedData = mockEventCapturedData(systemId1);
        
        m_EventHelper.handleEvent(objectCapturedData);
        
        //verify growl message
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset captured data!",
            String.format("An asset from controller 0x%08x captured data. The observation UUID is %s.", systemId1,
                 uuid2.toString()));
    }
    
    /**
     * Verify that capture data event causes a event to be fired but does not produce a growl message.
     */
    @Test 
    public void testHandleRemoteEventDataCapturedWithoutObservationUUID()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(1).size(), is(2));
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, 1);
        Event mockedCapData = new Event(Asset.TOPIC_DATA_CAPTURED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_EventHelper.handleEvent(mockedCapData);
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        
        assertThat(captor.getValue().getTopic(), is(AssetMgr.TOPIC_ASSET_OBSERVATION_UPDATED));
        assertThat((String)captor.getValue().getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
    }
    
    /**
     * Test the handling of activated asset event.
     * Verify asset status is updated to active.
     */
    @Test
    public void testHandleRemoteEventActivatedAsset()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added, get status, should be default of "Active status not set"
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        for (AssetModel model : models)
        {
            assertThat(model.getActiveStatus(), is(nullValue()));
        }
                
        //mock event that an asset was activated
        Event assetActivated = mockEventAssetActivated(systemId1);
        
        m_EventHelper.handleEvent(assetActivated);
        
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        //variable to hold boolean verifying the value was indeed checked
        boolean checked = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getActiveStatus(), is(AssetActiveStatus.ACTIVATED));
                checked = true;
            }
        }
        assertThat(checked, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event pushEvent = eventCaptor.getValue();
        assertThat(pushEvent, notNullValue());
        assertThat(pushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)pushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.ACTIVATED.toString()));
        assertThat((String)pushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
    }
    
    /**
     * Test the handling of asset deactivated event.
     * Verify asset status is updated.
     */
    @Test
    public void testHandleRemoteEventAssetDeactivated()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added, get status, should be default of deactivated
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        for (AssetModel model : models)
        {
            assertThat(model.getActiveStatus(), is(nullValue()));
        }
        
        //mock event that an asset was activated
        Event assetActivated = mockEventAssetActivated(systemId1);
        
        m_EventHelper.handleEvent(assetActivated);
        
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        //variable to hold boolean verifying the value was indeed checked
        boolean checked = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getActiveStatus(), is(AssetActiveStatus.ACTIVATED));
                checked = true;
            }
        }
        assertThat(checked, is(true));
        
        //mock event that the asset that was just activated is now deactivated
        Event assetDeactivated = mockEventAssetDeactivated(systemId1);
        
        m_EventHelper.handleEvent(assetDeactivated);
        
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        //variable to hold boolean verifying the value was indeed checked
        checked = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));
                checked = true;
            }
        }
        assertThat(checked, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().size(), is(2));
        
        Event firstPushEvent = eventCaptor.getAllValues().get(0);
        Event secondPushEvent = eventCaptor.getAllValues().get(1);
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.ACTIVATED.toString()));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
        
        assertThat(secondPushEvent, notNullValue());
        assertThat(secondPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)secondPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.DEACTIVATED.toString()));
        assertThat((String)secondPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
    }
    
    /**
     * Test the handling of asset activation failed event
     */
    @Test
    public void testHandleRemoteEventActivationFailed()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added, get status, should be default of deactivated
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        //set the asset models activation status to deactivated
        m_EventHelper.handleEvent(mockEventAssetDeactivated(systemId1));

        AssetModel modelWithStatusChange = m_SUT.getAssetModelByUuid(uuid2, systemId1);
        assertThat(modelWithStatusChange.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));
        
        //mock event that an asset has failed to be activated
        m_EventHelper.handleEvent(mockEventActivationFailed(systemId1));
        
        AssetModel modelAfterStatusChange = m_SUT.getAssetModelByUuid(uuid2, systemId1);
        assertThat(modelWithStatusChange.getUuid(), is (modelAfterStatusChange.getUuid()));
        assertThat(modelAfterStatusChange.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().size(), is(2));
        
        Event firstPushEvent = eventCaptor.getAllValues().get(0);
        Event secondPushEvent = eventCaptor.getAllValues().get(1);
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.DEACTIVATED.toString()));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
        
        assertThat(secondPushEvent, notNullValue());
        assertThat(secondPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)secondPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.DEACTIVATED.toString()));
        assertThat((String)secondPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
    }
    
    /**
     * Test the handling of event that is not an asset related event.
     * Verify nothing happens.
     */
    @Test
    public void testHandleRemoteEventNotAssetRelated()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify messages sent for the GetAssetsResponse
        //one asset dir svc to get the assets
        verify(m_MessageFactory, times(1)).createAssetDirectoryServiceMessage(Mockito.any(
            AssetDirectoryServiceMessageType.class), Mockito.any(Message.class));
        //asset messages for name, status, etc.
        verify(m_MessageFactory, times(6)).createAssetMessage(Mockito.any(
                AssetMessageType.class), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(8)).queue(anyInt(), Mockito.any(ResponseHandler.class));
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        //mock event that is not related to assets
        Event assetActivated = mockEventNotAssetRelated(systemId1);
        
        m_EventHelper.handleEvent(assetActivated);
        
        //verify that no messages were, nor growl messages posted
        verify(m_MessageFactory, times(1)).createAssetDirectoryServiceMessage(Mockito.any(
            AssetDirectoryServiceMessageType.class), Mockito.any(Message.class));
        verify(m_MessageFactory, times(6)).createAssetMessage(Mockito.any(
                AssetMessageType.class), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(8)).queue(anyInt(), Mockito.any(ResponseHandler.class));
        
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(Mockito.any(Severity.class), anyString(), anyString());

        //verify that no additional asset models were added
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));

        /* Aside from adding an asset, or updating a configuration value an event message can change an
         * asset's status. Verify that the assets status's are set at the default of null. 
         */
        for (AssetModel model : models)
        {
            assertThat(model.getActiveStatus(), is(nullValue()));
        }
    }
    
    /**
     * Test the handling of get assets event.
     * Verify assets are added.
     * Verify additional requests for information are sent.
     */
    @Test
    public void testHandleGetAssetsEvent()
    {
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify request for asset info
        verify(m_MessageFactory, times(2)).createAssetMessage(
                eq(AssetMessageType.GetActiveStatusRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory, times(2)).createAssetMessage(
                eq(AssetMessageType.GetNameRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));

        //verify assets added to the controller id
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        //verify the integrity of the models
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
            }
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
            }
        }
    }
    
    /**
     * Test the handling of get assets event for multiple controllers.
     * Verify assets are added to appropriate system id.
     * Verify additional requests for information are sent.
     */
    @Test
    public void testHandleGetAssetsEventMoreThanOneController()
    {

        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify request for asset info
        verify(m_MessageFactory, times(2)).createAssetMessage(
                eq(AssetMessageType.GetActiveStatusRequest),
            Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory, times(2)).createAssetMessage(
                eq(AssetMessageType.GetNameRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));

        //verify assets added to the controller id
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        assertThat(models.size(), is(2));
        
        //replay
        getAssetsReponse = mockGetAssetsResponse(systemId2);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify request for asset info
        verify(m_MessageFactory, times(4)).createAssetMessage(
            eq(AssetMessageType.GetActiveStatusRequest), 
            Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId2), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory, times(4)).createAssetMessage(
                eq(AssetMessageType.GetNameRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));

        //verify assets added, remember this will return all assets known
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        assertThat(m_SUT.getAssetsForControllerAsync(systemId2).size(), is(2));

        //verify assets added to the controller id
        models = m_SUT.getAssetsForControllerAsync(systemId2);
        assertThat(models.size(), is(2));
        
        //verify the integrity of the models
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
            }
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
            }
        }
    }
    
    /**
     * Test the handling of controller removed event.
     * Verify that the correct system id is removed.
     */
    @Test
    public void testHandleControllerRemovedEvent()
    {
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //replay
        getAssetsReponse = mockGetAssetsResponse(systemId2);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added, remember this will return all assets known
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        assertThat(m_SUT.getAssetsForControllerAsync(systemId2).size(), is(2));
        
        //get controller removed event
        Event removedController = mockControllerRemovedEvent(systemId1);
        m_ControllerEvent.handleEvent(removedController);
        
        //verify that the system id is no longer known
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).isEmpty(), is(true));
    }
    
    /**
     * Test the handling of object name updated event.
     * Verify that name is updated.
     */
    @Test
    public void testHandleObjectNameUpdated()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);

        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //get the default name
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //should be two models, the model with UUID2 is the model updated
        assertThat(models.size(), is(2));
        String defaultName;
        if (models.get(0).getUuid().equals(uuid2))
        {
            defaultName = models.get(0).getName();
        }
        else
        {
            defaultName = models.get(1).getName();
        }
        
        //handle updated event
        Event objUpdated = mockEventObjectNameUpdated(systemId1);
        
        //this should create a request for updating information
        m_EventHelper.handleEvent(objUpdated);

        //get the asset model that should of been updated
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //should be two models
        assertThat(models.size(), is(2));
        AssetModel model;
        if (models.get(0).getUuid().equals(uuid2))
        {
            model = models.get(0);
        }
        else
        {
            model = models.get(1);
        }
        
        //name should of been updated to "splooosh"
        assertThat(model.getName(), is("splooosh"));
        
        //not pulling any fast ones, verify
        assertThat(defaultName, is(not("splooosh")));
    }
    
    /**
     * Test the handling of object name updated event, where the actual name property is missing.
     * Verify that name is NOT updated.
     */
    @Test
    public void testHandleObjectNameUpdatedNull()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);

        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //get the default name
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //should be two models, the model with UUID2 is the model updated
        assertThat(models.size(), is(2));
        String defaultName;
        if (models.get(0).getUuid().equals(uuid2))
        {
            defaultName = models.get(0).getName();
        }
        else
        {
            defaultName = models.get(1).getName();
        }
        
        //handle updated event
        Event objUpdated = mockEventObjectNameUpdatedNull(systemId1);
        
        //this should create a request for updating information
        m_EventHelper.handleEvent(objUpdated);

        //get the asset model that should of been updated
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //should be two models
        assertThat(models.size(), is(2));
        AssetModel model;
        if (models.get(0).getUuid().equals(uuid2))
        {
            model = models.get(0);
        }
        else
        {
            model = models.get(1);
        }
        
        //name should still be the original name
        assertThat(model.getName(), is(defaultName));
    }
    
    /**
     * Test the handling of get assets event.
     * Verify assets are added.
     * Verify that if an additional response for get assets is received and an asset is no longer
     * associated with the system that it is removed.
     */
    @Test
    public void testHandleGetAssetsEventOneAssetRemoved()
    {
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify request for asset info
        verify(m_MessageFactory, times(2)).createAssetMessage(
                eq(AssetMessageType.GetNameRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory, times(2)).createAssetMessage(
            eq(AssetMessageType.GetActiveStatusRequest), 
            Mockito.any(Message.class));
        verify(m_MessageWrapper, times(7)).queue(eq(systemId1), (ResponseHandler) eq(null));

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //replay, this event will represent a get asset response with one of the
        //previously associated assets no long returned, asset with uuid2 is the one not returned
        getAssetsReponse = mockGetAssetsResponseOneRemovedAsset(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify asset is no longer associated
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(1));
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).get(0).getUuid(), is(uuid1));
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).get(0).getType(), is(Asset.class.getName()));
    }
    
    /**
     * Test the handling of get property request event, this will return the assets name.
     * Verify name is set.
     */
    @Test
    public void testHandleGetPropertyResponse()
    {

        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create config admin appropriate event
        Event getPropertyRequest = mockGetPropertyResponse(systemId1);

        //handle event
        m_AssetHelper.handleEvent(getPropertyRequest);

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);

        //check that the appropriate model was updated, should be asset with pid1
        for (AssetModel model : models)
        {
            if (model.getPid().equals(pid1))
            {
                assertThat(model.getName(), is("Prime"));
            }
        }
    }
    
    /**
     * Test the handling of get status for asset request event.
     * Verify the status is updated. (The mock message only updates the asset with uuid1).
     */
    @Test
    public void testHandleGetAssetStatusResponse()
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset directory appropriate event for the asset status update
        Event getStatusResponse = mockGetAssetStatusResponseActive(systemId1);

        //handle event
        m_AssetHelper.handleEvent(getStatusResponse);

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
         //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getActiveStatus(), is(Asset.AssetActiveStatus.ACTIVATED));
                verified = true;
            }
        }
        assertThat(verified, is(true));
        
        //change status
        getStatusResponse = mockGetAssetStatusResponseDeactived(systemId1);

        //handle event
        m_AssetHelper.handleEvent(getStatusResponse);

        //verify
        models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        verified = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getActiveStatus(), is(Asset.AssetActiveStatus.DEACTIVATED));
                verified = true;
            }
        }
        assertThat(verified, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().size(), is(2));
        
        Event firstPushEvent = eventCaptor.getAllValues().get(0);
        Event secondPushEvent = eventCaptor.getAllValues().get(1);
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.ACTIVATED.toString()));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid1.toString()));
        
        assertThat(secondPushEvent, notNullValue());
        assertThat(secondPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_ACTIVATION_STATUS_UPDATED));
        assertThat((String)secondPushEvent.getProperty(AssetMgr.EVENT_PROP_ACTIVE_STATUS_SUMMARY), 
                is(AssetActiveStatus.DEACTIVATED.toString()));
        assertThat((String)secondPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid1.toString()));
    }
    
    /**
     * Test the handling of get status request event.
     * Verify the status is updated. (The mock message only updates the asset with uuid1).
     */
    @Test
    public void testHandleGetLastStatusResponse() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event getStatusResponse = mockGetStatusResponse(systemId1);

        //status
        Observation obs = mock(Observation.class);
        when(obs.getStatus()).thenReturn(new Status().withSummaryStatus(
            new mil.dod.th.core.types.status.OperatingStatus(SummaryStatusEnum.DEGRADED, "Description")));
        //mock converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(obs);
        
        //handle event
        m_AssetHelper.handleEvent(getStatusResponse);

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        boolean verified2 = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(SummaryStatusEnum.DEGRADED));
                assertThat(model.getSummaryDescription(), is("Description"));
                verified = true;
            }
            //check that the default is unknown, as only asset with uuid1 is updated with the event
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verified2 = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verified, is(true));
        assertThat(verified2, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event firstPushEvent = eventCaptor.getValue();
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY), 
                is(SummaryStatusEnum.DEGRADED.toString()));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION), 
                is("Description"));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid1.toString()));
        
    }
    
    /**
     * Test the handling of get status request event when the asset that the status is about isn't known.
     */
    @Test
    public void testHandleGetLastStatusResponseUnknownAsset() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event getStatusResponse = mockGetStatusResponseDiffUuid(systemId1);

        //handle event
        m_AssetHelper.handleEvent(getStatusResponse);

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that neither asset's status is updated
        boolean verifiedAsset1 = false;
        boolean verifiedAsset2 = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verifiedAsset1 = true;
            }
            //check that other the other asset's status wasn't updated either
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verifiedAsset2 = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verifiedAsset1, is(true));
        assertThat(verifiedAsset2, is(true));
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Verify no event posted for an asset directory service event topic not supported by the WEB GUI.
     */
    @Test
    public void testHandleAssetDirScanEvent()
    {
        //make some assets, just so that we don't exit out of the method because of no model
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //handle event with scan topic
        m_EventHelper.handleEvent(mockEventUnHandledAssetDirectoryEvent(systemId1));

        //verify no update event
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test the handling of get status request response is still handled if the summary description is an empty string. 
     * Verify the status is updated. (The mock message only updates the asset with uuid1).
     */
    @Test
    public void testHandleGetStatusResponseNoSummaryDescription() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event getStatusResponse = mockGetStatusResponseNoStatus(systemId1);

        //handle event
        m_AssetHelper.handleEvent(getStatusResponse);

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                assertThat(model.getSummaryDescription(), is(""));
                verified = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verified, is(true));
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test the handling of perform BIT request event.
     * Verify the status is updated. (The mock message only updates the asset with uuid1).
     */
    @Test
    public void testHandlePerformBitResponse() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event bitUpdate = mockPerformBITResponse(systemId1);

        //status
        Observation obs = mock(Observation.class);
        when(obs.getStatus()).thenReturn(new Status().withSummaryStatus(
            new mil.dod.th.core.types.status.OperatingStatus(SummaryStatusEnum.OFF, "zoom")));
        //mock converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(obs);
        
        //handle event
        m_AssetHelper.handleEvent(bitUpdate);

        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset completed BIT test.", 
            String.format("Asset %s from controller 0x%08x, updated status to %s.", 
                "Unknown (" + uuid1.toString() + ")", systemId1, SummaryStatusEnum.OFF));

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        boolean verified2 = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(SummaryStatusEnum.OFF));
                assertThat(model.getSummaryDescription(), is("zoom"));
                verified = true;
            }
            //check that the default is unknown, as only asset with uuid1 is updated with the event
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verified2 = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verified, is(true));
        assertThat(verified2, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event firstPushEvent = eventCaptor.getValue();
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY), 
                is(SummaryStatusEnum.OFF.toString()));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION), 
                is("zoom"));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid1.toString()));
    }
    
    /**
     * Verify an invalid status observation format is ignored and doesn't cause an exception
     */
    @Test
    public void testHandlePerformBitResponse_InvalidFormat() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event bitUpdate = mockPerformBITResponse_XmlFormat(systemId1);

        //handle event
        m_AssetHelper.handleEvent(bitUpdate);

        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                assertThat(model.getSummaryDescription(), is(""));
                verified = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verified, is(true));
    }
    
    /**
     * Verify not exception is thrown if no model is found for UUID.
     */
    @Test
    public void testHandlePerformBitResponse_NoModel() throws ObjectConverterException
    {
        //create asset appropriate event for the asset status update
        Event bitUpdate = mockPerformBITResponse_XmlFormat(systemId1);

        //handle event, just verify no exception
        m_AssetHelper.handleEvent(bitUpdate);
    }
    
    /**
     * Test the handling of perform BIT request event and the status cannot be converted.
     */
    @Test
    public void testHandlePerformBitResponseException() throws ObjectConverterException
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);

        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));

        //create asset appropriate event for the asset status update
        Event bitUpdate = mockPerformBITResponse(systemId1);

        //mock converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).
            thenThrow(new ObjectConverterException("Exception"));
        
        //handle event
        m_AssetHelper.handleEvent(bitUpdate);

        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), anyString(), anyString());

        //verify
        List<AssetModel> models = m_SUT.getAssetsForControllerAsync(systemId1);
        
        //verify that the asset with uuid1 is updated with appropriate status
        boolean verified = false;
        boolean verified2 = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid1))
            {
                assertThat(model.getPid(), is(pid1));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verified = true;
            }
            //check that the default is unknown, as only asset with uuid1 is updated with the event
            else
            {
                assertThat(model.getUuid(), is(uuid2));
                assertThat(model.getPid(), is(pid2));
                assertThat(model.getSummaryStatus(), is(nullValue()));
                verified2 = true;
            }
        }
        
        //verify both assets were checked
        assertThat(verified, is(true));
        assertThat(verified2, is(true));
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Verify an asset property response generates a message.
     */
    @Test
    public void testHandleSetAssetPropertyResponse()
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, null);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, 1);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.SetPropertyResponse.toString());
        
        //the event
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_AssetHelper.handleEvent(event);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), eq("Properties Accepted:"),
                eq(String.format("Controller 0x%08x has accepted the list of properties.", 1)));
    }
    
    /**
     * Test the handling of an execute command response event.
     * Verify that an asset location updated event is posted to the event admin.
     */
    @Test
    public void testHandleExecuteCommandResponse()
    {
        //make some assets
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
        
        List<AssetModel> assetModels = m_SUT.getAssetsForControllerAsync(systemId1);
        
        AssetModel testModel = null;
        
        for (AssetModel assetModel : assetModels)
        {
            if (assetModel.getUuid().equals(uuid1))
            {
                testModel = assetModel;
                break;
            }
        }
        
        // verify location info empty at first
        assertThat(testModel.getLocation().getLatitude(), is(nullValue()));
        assertThat(testModel.getLocation().getLongitude(), is(nullValue()));
        assertThat(testModel.getLocation().getAltitude(), is(nullValue()));
        assertThat(testModel.getLocation().getBank(), is(nullValue()));
        assertThat(testModel.getLocation().getHeading(), is(nullValue()));
        assertThat(testModel.getLocation().getElevation(), is(nullValue()));
        
        double expectedLongitude = -35.0;
        double expectedHeading = 90.0;
        
        //get position command
        Event getPositionCommand = mockExecuteGetPositionCommandResponse(systemId1, expectedLongitude, 
                1, 2, 3, 4, expectedHeading);
        
        m_AssetHelper.handleEvent(getPositionCommand);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AssetModel.EVENT_PROP_UUID, testModel.getUuid().toString());
        Event assetLocationUpdated = new Event(AssetMgr.TOPIC_ASSET_LOCATION_UPDATED, props);
        
        // just check one field from each type to ensure coord and orientation objects are passed properly
        assertThat(Double.valueOf(testModel.getLocation().getLongitude().toString()), is(expectedLongitude));
        assertThat(Double.valueOf(testModel.getLocation().getHeading().toString()), is(expectedHeading));
        
        //verify EventAdmin posted that the location was updated
        verify(m_EventAdmin).postEvent(assetLocationUpdated);
    }
    
    /**
     * Verify that another command response does not generate any events.
     */
    @Test
    public void testHandleExecuteCommandResponseNotGetPositionResponse()
    {
        Response baseResponse = Response.newBuilder().build();
        
        GetPanTiltResponseGen.GetPanTiltResponse panTiltResponse = 
                GetPanTiltResponseGen.GetPanTiltResponse.newBuilder().
                setBase(baseResponse).
                setPanTilt(OrientationOffset.newBuilder().
                        setAzimuth(AzimuthDegrees.newBuilder().setValue(10.0).build()).
                        setElevation(ElevationDegrees.newBuilder().setValue(10.0).build()).build()).
                build();
        
        ExecuteCommandResponseData response = ExecuteCommandResponseData.newBuilder().
                setResponse(panTiltResponse.toByteString()).
            setResponseType(CommandResponseEnumConverter.convertJavaEnumToProto(
                CommandResponseEnum.GET_PAN_TILT_RESPONSE)).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, 1);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ExecuteCommandResponse.toString());
        //the event
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_AssetHelper.handleEvent(event);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Verify that a get position response from an unknown asset model does not produce
     * any events.
     */
    @Test
    public void testHandleExecuteCommandResponseWithUnknownAssetModel()
    {
        //create all the orientation and coordinates measurements and put them in the response
        LongitudeWgsDegrees newLongitude = LongitudeWgsDegrees.newBuilder().
                setValue(10.0D).
                build();
        
        LatitudeWgsDegrees newLatitude = LatitudeWgsDegrees.newBuilder().
                setValue(10D).
                build();
        
        HaeMeters newHaeMeters = HaeMeters.newBuilder().
                setValue(10D).
                build();
        
        ElevationDegrees newElevation = ElevationDegrees.newBuilder().
                setValue(10D).
                build();
        
        HeadingDegrees newHeading = HeadingDegrees.newBuilder().
                setValue(10D).
                build();
        
        Orientation orien = Orientation.newBuilder().
                setElevation(newElevation).
                setHeading(newHeading).
                build();
        
        Coordinates local = Coordinates.newBuilder().
                setLongitude(newLongitude).
                setAltitude(newHaeMeters).
                setLatitude(newLatitude).
                build();
        
        //Build the base response message required by all response messages.
        Response baseResponse = Response.newBuilder().build();
        
        GetPositionResponseGen.GetPositionResponse getPositionResponse = GetPositionResponseGen.
                GetPositionResponse.newBuilder().
                setBase(baseResponse).
                setOrientation(orien).
                setLocation(local).
                build();
                
        ExecuteCommandResponseData response = ExecuteCommandResponseData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                setResponse(getPositionResponse.toByteString()).
                setResponseType(CommandResponseEnumConverter.convertJavaEnumToProto(
                    CommandResponseEnum.GET_POSITION_RESPONSE)).
                build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, 1);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ExecuteCommandResponse.toString());
        
        //the event
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_AssetHelper.handleEvent(event);
        
        //verify EventAdmin posted that the location was updated
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test the handling of remote response handler for create asset requests.
     * Verify response handler sends set property request.
     * Verify that an asset model was created from the create asset response.
     */
    @Test
    public void testRemoteCreateAssetHandler()
    {
        //verify no assets exist for the system
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(0));

        //get a handler
        RemoteCreateAssetHandler handler = m_SUT.createRemoteAssetHandler();
        
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder().
                setPid("PIDDER").
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID())).
                setProductType(Asset.class.getName()).build();
        
        //response messages to process
        CreateAssetResponseData response1 = CreateAssetResponseData.newBuilder().
            setInfo(info).build();

        AssetDirectoryServiceNamespace nameResponse1 = AssetDirectoryServiceNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(AssetDirectoryServiceMessageType.CreateAssetResponse).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.AssetDirectoryService).
                setNamespaceMessage(nameResponse1.toByteString()).build();
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.
            createTerraHarvestMessage(systemId1, 0, Namespace.AssetDirectoryService, 123, nameResponse1);

        //this will load the registration ids into the response handler
        handler.handleResponse(thMessage1, payload1, nameResponse1, response1);
        
        //verify asset added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(1));
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).get(0).getType(), is(Asset.class.getName()));
    }
    
    /**
     * Test the handling of the remote response handler for asset set name requests.
     * Verify that a growl message is posted if there the message is invalid.
     */
    @Test
    public void testRemoteSetAssetNameHandler()
    {
        //verify no assets exist for the system
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(0));

        //get a handler
        RemoteSetAssetNameHandler handler = m_SUT.createRemoteAssetNameHandler(uuid1);
        
        //error message
        GenericErrorResponseData errorResponse = GenericErrorResponseData.newBuilder().
             setError(ErrorCode.INVALID_VALUE).setErrorDescription("Bad bad name").build();

        BaseNamespace namespace = BaseNamespace.newBuilder().
             setType(BaseMessageType.GenericErrorResponse).
             setData(errorResponse.toByteString()).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.Base).
                setNamespaceMessage(namespace.toByteString()).build();
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.
            createTerraHarvestMessage(systemId1, 0, Namespace.Base, 123, namespace);

        //handle response
        handler.handleResponse(thMessage1, payload1, namespace, errorResponse);

        //verify growl message
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset name change failed", 
                String.format("Asset with UUID " + uuid1.toString() + " from controller 0x%08x failed to " 
                    + "update its name, because: " + ErrorCode.INVALID_VALUE.toString() + " - " 
                        + "Bad bad name", systemId1));
        
        //verify that if namespace is not base then don't do anything
        TerraHarvestMessageHelper.handleAssetMessage(AssetMessageType.ExecuteCommandResponse, 
                ExecuteCommandResponseData.getDefaultInstance(), handler);

        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset name change failed", 
                String.format("Asset with UUID " + uuid1.toString() + " from controller 0x%08x failed to " 
                    + "update its name, because: " + ErrorCode.INVALID_VALUE.toString() + " - " 
                        + "Bad bad name", systemId1));
        
        TerraHarvestMessageHelper.handleBaseMessage(BaseMessageType.GetControllerCapabilitiesResponse,
                GetControllerCapabilitiesResponseData.getDefaultInstance(), handler);

        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset name change failed", 
                String.format("Asset with UUID " + uuid1.toString() + " from controller 0x%08x failed to " 
                    + "update its name, because: " + ErrorCode.INVALID_VALUE.toString() + " - " 
                        + "Bad bad name", systemId1));
    }
    
    /**
     * Verify the asset mgr sync position sends the correct request.
     */
    @Test
    public void testSyncPosition() throws InvalidProtocolBufferException
    {
        AssetModel assetModel = mock(AssetModel.class);
        when(assetModel.getName()).thenReturn("name1");
        when(assetModel.getUuid()).thenReturn(uuid1);
        
        m_SUT.syncPosition(0, assetModel);

        ArgumentCaptor<ExecuteCommandRequestData> messageCap = ArgumentCaptor.forClass(ExecuteCommandRequestData.class);
        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                messageCap.capture());
        verify(m_MessageWrapper).queue(eq(0), Mockito.any(AssetExecuteErrorHandler.class));
        
        //verify message
        ExecuteCommandRequestData data = ExecuteCommandRequestData.parseFrom(messageCap.getValue().toByteArray());
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)));
        assertThat(data.getCommandType(), is(CommandTypesGen.CommandType.Enum.GET_POSITION_COMMAND));
    }
    
    /**
     * Verify the asset mgr location update sends the correct requests to update the location with
     * values in the asset model.
     */
    @Test
    public void testLocationUpdate() throws InvalidProtocolBufferException
    {
        AssetModel assetModel = mock(AssetModel.class);
        when(assetModel.getName()).thenReturn("annie");
        when(assetModel.getUuid()).thenReturn(uuid1);
        LocationModel location = new LocationModel();
        when(assetModel.getLocation()).thenReturn(location);
        location.setLongitude(1.00000001);
        location.setLatitude(2.00000002);
        location.setAltitude(3.00000003);
        location.setBank(4.04);
        location.setElevation(5.05);
        location.setHeading(6.06);
        
        m_SUT.locationUpdate(0, assetModel);
        
        ArgumentCaptor<ExecuteCommandRequestData> requestDataCapture = ArgumentCaptor.
                forClass(ExecuteCommandRequestData.class);
        
        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                requestDataCapture.capture());
        verify(m_MessageWrapper).queue(eq(0), Mockito.any(AssetExecuteErrorHandler.class));
        
        ExecuteCommandRequestData request = requestDataCapture.getValue();
        
        assertThat(request.getCommandType(), is(CommandTypesGen.CommandType.Enum.SET_POSITION_COMMAND));
        
        SetPositionCommand setPositionCommand = SetPositionCommand.parseFrom(request.getCommand());
        Coordinates testLocation = setPositionCommand.getLocation();
        Orientation testOrientation = setPositionCommand.getOrientation();
        
        assertThat(testLocation.getLongitude().getValue(), is(location.getLongitude()));
        assertThat(testLocation.getLatitude().getValue(), is(location.getLatitude()));
        assertThat(testLocation.getAltitude().getValue(), is(location.getAltitude()));
        
        assertThat(testOrientation.getBank().getValue(), is(location.getBank()));
        assertThat(testOrientation.getElevation().getValue(), is(location.getElevation()));
        assertThat(testOrientation.getHeading().getValue(), is(location.getHeading()));
    }
    
    /**
     * Testing the asset error handler response handler class.
     * Verify proper message is handled.
     * Verify message of other namespace doesn't get handled.
     * Verify message of same namespace but different type does not get handled.
     */
    @Test
    public void testAssetErrorHandler()
    {
        AssetExecuteErrorHandler assetErrorHandler = m_SUT.new AssetExecuteErrorHandler("name");
        
        GenericErrorResponseData errorResponse = GenericErrorResponseData.newBuilder().
                setError(ErrorCode.ASSET_ERROR).
                setErrorDescription("wow what an error").
                build();
        
        BaseNamespace nameResponse1 = BaseNamespace.newBuilder().
                setData(errorResponse.toByteString()).
                setType(BaseMessageType.GenericErrorResponse).build();
        
        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(nameResponse1.toByteString()).
                build();
        
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
                Namespace.Asset, 123, nameResponse1);
        
        assetErrorHandler.handleResponse(thMessage1, payload1, nameResponse1, errorResponse);
        
        //verify growl message
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Asset Error Occurred", 
                String.format(
                        "Performing action ExecuteCommand for asset %s "
                        + "has resulted in the following error %n %s - %s", 
                        "name", ErrorCode.ASSET_ERROR, 
                        "wow what an error"));
        
        //verify that if namespace is not base then don't do anything
        TerraHarvestMessageHelper.handleAssetMessage(AssetMessageType.ExecuteCommandResponse, 
                ExecuteCommandResponseData.getDefaultInstance(), assetErrorHandler);
        
        //verify growl message
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Asset Error Occurred", 
                String.format(
                        "Performing action ExecuteCommand for asset %s "
                        + "has resulted in the following error %n %s - %s", 
                        "name", ErrorCode.ASSET_ERROR, 
                        "wow what an error"));
        
        TerraHarvestMessageHelper.handleBaseMessage(BaseMessageType.GetControllerCapabilitiesResponse,
                GetControllerCapabilitiesResponseData.getDefaultInstance(), assetErrorHandler);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Asset Error Occurred", 
                String.format(
                        "Performing action ExecuteCommand for asset %s "
                        + "has resulted in the following error %n %s - %s", 
                        "name", ErrorCode.ASSET_ERROR, 
                        "wow what an error"));
    }
    
    /**
     * Verify asset model can be retrieved by uuid.
     */
    @Test
    public void testGetAssetModelById()
    {
        AssetModel modelNotFound = m_SUT.getAssetModelByUuid(UUID.randomUUID(), 10);
        
        assertThat(modelNotFound, nullValue());
        
        Event event = mockEventAssetCreated(10);
        
        m_EventHelper.handleEvent(event);
        
        AssetModel modelFound = m_SUT.getAssetModelByUuid(uuid2, 10);
        assertThat(modelFound, notNullValue());
        assertThat(modelFound.getName().contains("Unknown"), is(true));
    }
    
    /**
     * Test the handling of an asset status change with a message.
     */
    @Test
    public void testHandleRemoteEventStatusChanged()
    {
        //load assets for one system
        Event getAssetsReponse = mockGetAssetsResponse(systemId1);
        
        //this should render two assets for the system id
        m_AssetDirHelper.handleEvent(getAssetsReponse);
        
        //verify assets added
        assertThat(m_SUT.getAssetsForControllerAsync(systemId1).size(), is(2));
                
        //mock event that an asset status event for asset with uuid2
        Event assetStatus = mockEventStatusChange(systemId1);
        
        m_EventHelper.handleEvent(assetStatus);
        
        List<AssetModel>models = m_SUT.getAssetsForControllerAsync(systemId1);
        //variable to hold boolean verifying the value was indeed checked
        boolean checked = false;
        for (AssetModel model : models)
        {
            if (model.getUuid().equals(uuid2))
            {
                assertThat(model.getSummaryStatus(), is(SummaryStatusEnum.DEGRADED));
                assertThat(model.getSummaryDescription(), is("message"));
                checked = true;
            }
        }
        assertThat(checked, is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event firstPushEvent = eventCaptor.getValue();
        assertThat(firstPushEvent, notNullValue());
        assertThat(firstPushEvent.getTopic(), is(AssetMgr.TOPIC_ASSET_STATUS_UPDATED));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY), 
                is(SummaryStatusEnum.DEGRADED.toString()));
        assertThat((String)firstPushEvent.getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION), 
                is("message"));
        assertThat((String)firstPushEvent.getProperty(AssetModel.EVENT_PROP_UUID), is(uuid2.toString()));
    }
    
    /////////////////////////////////////////////////////////////////////////
    // All methods below are helpers for various messages handled by this  //
    // class.                                                              //
    /////////////////////////////////////////////////////////////////////////
    
    /**
     * Used to mock a message from a remote controller representing assets which that controller has. 
     */
    private Event mockGetAssetsResponse(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        GetAssetsResponseData.Builder response = GetAssetsResponseData.newBuilder();

        //mock asset information
        FactoryObjectInfo info1 = FactoryObjectInfo.newBuilder().
            setPid(pid1).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setProductType(Asset.class.getName()).build();
        response.addAssetInfo(info1);
        
        //mock asset information
        FactoryObjectInfo info2 = FactoryObjectInfo.newBuilder().
            setPid(pid2).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid2)).
            setProductType(Asset.class.getName()).build();
        response.addAssetInfo(info2);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response.build());
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetDirectoryServiceMessageType.GetAssetsResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's name property. 
     */
    private Event mockGetPropertyResponse(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //message that would be received as a response to the GetPropertyRequest
        GetNameResponseData response = GetNameResponseData.newBuilder().
            setAssetName("Prime").
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetNameResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's active status. Specifically
     * an activated status for an asset.
     */
    private Event mockGetAssetStatusResponseActive(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        AssetMessages.AssetActiveStatus status = 
            AssetMessages.AssetActiveStatus.ACTIVATED;
        GetActiveStatusResponseData response = GetActiveStatusResponseData.newBuilder().
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setStatus(status).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetActiveStatusResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's active status. Specifically
     * a deactivated status for an asset.
     */
    private Event mockGetAssetStatusResponseDeactived(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        AssetMessages.AssetActiveStatus status = 
            AssetMessages.AssetActiveStatus.DEACTIVATED;
        GetActiveStatusResponseData response = GetActiveStatusResponseData.newBuilder().
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setStatus(status).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetActiveStatusResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's summary status.
     */
    private Event mockGetStatusResponse(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        ObservationGen.Observation protoStat = ObservationGen.Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                setAssetName("asset").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("type").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID())).
                setStatus(
                     StatusGen.Status.newBuilder().
                     setSummaryStatus(OperatingStatus.newBuilder()
                         .setSummary(SummaryStatusEnumConverter.convertJavaEnumToProto(SummaryStatusEnum.OFF))
                         .build())).build();
        GetLastStatusResponseData response = GetLastStatusResponseData.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setStatusObservationNative(protoStat).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetLastStatusResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's summary status, where the
     * asset is no known.
     */
    private Event mockGetStatusResponseDiffUuid(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        ObservationGen.Observation protoStat = ObservationGen.Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                setAssetName("asset").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("type").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID())).
                setStatus(
                        StatusGen.Status.newBuilder().
                     setSummaryStatus(
                         OperatingStatus.newBuilder()
                             .setSummary(SummaryStatusEnumConverter.convertJavaEnumToProto(SummaryStatusEnum.OFF))
                             .build()).build()).build();
        GetLastStatusResponseData response = GetLastStatusResponseData.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID())).
            setStatusObservationNative(protoStat).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetLastStatusResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's summary status response when there
     * is no status known for the Asset.
     */
    private Event mockGetStatusResponseNoStatus(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);

        GetLastStatusResponseData response = GetLastStatusResponseData.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.GetLastStatusResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's update to status based
     * off of the results of an asset BIT.
     */
    private Event mockPerformBITResponse(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        ObservationGen.Observation protoStat = ObservationGen.Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                setAssetName("asset").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("type").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID())).
                setStatus(
                     StatusGen.Status.newBuilder().
                     setSummaryStatus(OperatingStatus.newBuilder()
                         .setSummary(SummaryStatusEnumConverter.convertJavaEnumToProto(SummaryStatusEnum.GOOD))
                         .build()).build()).build();
        PerformBitResponseData response = PerformBitResponseData.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setStatusObservationNative(protoStat).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.PerformBitResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    private Event mockPerformBITResponse_XmlFormat(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        PerformBitResponseData response = PerformBitResponseData.newBuilder()
                .setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1))
                .setStatusObservationXml(ByteString.copyFromUtf8("<xml>")).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.PerformBitResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock a message representing an asset get position response based off the results
     * of a get position request.
     */
    private Event mockExecuteGetPositionCommandResponse(final int systemId, double expectedLongitude, 
            double expectedLatitude, double expectedAltitude, double expectedBank, double expectedElevation, 
            double expectedHeading)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create all the orientation and coordinates measurements and put them in the response
        LongitudeWgsDegrees newLongitude = LongitudeWgsDegrees.newBuilder().
                setValue(expectedLongitude).
                build();
        
        LatitudeWgsDegrees newLatitude = LatitudeWgsDegrees.newBuilder().
                setValue(expectedLatitude).
                build();
        
        HaeMeters newHaeMeters = HaeMeters.newBuilder().
                setValue(expectedAltitude).
                build();
        
        //Create new observation object
        BankDegrees newBank = BankDegrees.newBuilder().
                setValue(expectedBank).
                build();
        
        ElevationDegrees newElevation = ElevationDegrees.newBuilder().
                setValue(expectedElevation).
                build();
        
        HeadingDegrees newHeading = HeadingDegrees.newBuilder().
                setValue(expectedHeading).
                build();
        
        Orientation orien = Orientation.newBuilder().
                setBank(newBank).
                setElevation(newElevation).
                setHeading(newHeading).
                build();
        
        Coordinates local = Coordinates.newBuilder().
                setLongitude(newLongitude).
                setAltitude(newHaeMeters).
                setLatitude(newLatitude).
                build();
        
        Response baseRespnse = Response.newBuilder().build();
        
        GetPositionResponseGen.GetPositionResponse getPositionResponse = GetPositionResponseGen.
                GetPositionResponse.newBuilder().
                setBase(baseRespnse).
                setOrientation(orien).
                setLocation(local).
                build();
                
        ExecuteCommandResponseData response = ExecuteCommandResponseData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                setResponse(getPositionResponse.toByteString()).
                setResponseType(
                    CommandResponseEnumConverter.convertJavaEnumToProto(CommandResponseEnum.GET_POSITION_RESPONSE)).
                build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetMessageType.ExecuteCommandResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset was deleted.
     */
    private Event mockEventAssetDeleted(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //mock properties
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing that an asset captured data.
     */
    private Event mockEventCapturedData(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(Asset.EVENT_PROP_ASSET_OBSERVATION_UUID, uuid2);
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(Asset.TOPIC_DATA_CAPTURED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset's capabilities.
     */
    private Event mockEventAssetCreated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset was activated.
     */
    private Event mockEventAssetActivated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(Asset.TOPIC_ACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset will be activated.
     */
    private Event mockEventAssetWillBeActivated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, 1);
        
        return new Event(Asset.TOPIC_WILL_BE_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset will be deactivated.
     */
    private Event mockEventAssetWillBeDeactivated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        //create a status
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, 1);
        
        return new Event(Asset.TOPIC_WILL_BE_DEACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset was deactivated.
     */
    private Event mockEventAssetDeactivated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(Asset.TOPIC_DEACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing assets which that controller has.
     * This message differs from the other message that uses the same message, as this message will only
     * return one asset representing that an asset may have been removed externally. 
     */
    private Event mockGetAssetsResponseOneRemovedAsset(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        GetAssetsResponseData.Builder response = GetAssetsResponseData.newBuilder();

        //mock asset information
        FactoryObjectInfo info1 = FactoryObjectInfo.newBuilder().
            setPid(pid1).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
            setProductType(Asset.class.getName()).build();
        response.addAssetInfo(info1);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response.build());
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetDirectoryServiceMessageType.GetAssetsResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Create an event that mimics a controller being removed.
     * @param systemId
     *     the system id to be used as the removed controller id
     * @return
     *     an event that is of the type and structure expected when a controller is removed
     */
    private Event mockControllerRemovedEvent(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        //the event
        return new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
    }

    /**
     * Used to mock a message from a remote controller representing an asset's name was successfully updated. 
     */
    private Event mockEventObjectNameUpdated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "splooosh");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        //the event
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock a message from a remote controller representing an asset's name was posted as updated, but the
     * name prop is missing. This is unlikely, but the event prop map can return null if a key is missing.
     */
    private Event mockEventObjectNameUpdatedNull(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, Asset.class.getName());
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        //the event
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }

    /**
     * Used to mock an event from a remote controller representing another send event type.
     */
    private Event mockEventNotAssetRelated(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Comms");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
            props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset status change event.
     */
    private Event mockEventStatusChange(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, "PID");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        props.put(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY, SummaryStatusEnum.DEGRADED.toString());
        props.put(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION, "message");
        return new Event(Asset.TOPIC_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
            props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset activation failed event.
     */
    private Event mockEventActivationFailed(final int systemId)
    {
        //Call to add controller to list.
        m_SUT.getAssetsForControllerAsync(systemId);
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(Asset.TOPIC_ACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Used to mock an event from a remote controller representing an asset dir scanning event.
     */
    private Event mockEventUnHandledAssetDirectoryEvent(final int systemId)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        return new Event(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE 
                + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
}
