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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field; // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.osgi.service.metatype.AttributeDefinition;

import com.google.protobuf.Message;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.MetaTypeMessages;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeInfoType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.ObjectClassDefinitionType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.advanced.BundleMgr;
import mil.dod.th.ose.gui.webapp.advanced.BundleModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemMetaTypeMgrImpl.BundleEventHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemMetaTypeMgrImpl.ControllerEventHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemMetaTypeMgrImpl.MetaTypeEventHandler;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

/**
 * Test class for the {@link SystemMetaTypeMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestSystemMetatypeMgrImpl
{
    private static final int CONTROLLERID = 25;
    private static final long BUNDLEID = 5L;
    private static final long BUNDLEID2 = 3L;
    private static final long BUNDLEID3 = 6L;
    
    private SystemMetaTypeMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private BundleContextUtil m_BundleContextUtil;
    private BundleEventHandler m_BundleEventHandler;
    private ControllerEventHandler m_ControllerEventHandler;
    private MetaTypeEventHandler m_MetaTypeEventHandler;
    private EventAdmin m_EventAdmin;
    private MessageWrapper m_MessageWrapper;
    private BundleContext m_BundleContext;
    
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg;

    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_MessageFactory = mock(MessageFactory.class);
        m_BundleContextUtil = mock(BundleContextUtil.class);
        m_BundleContext = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        m_MessageWrapper = mock(MessageWrapper.class);

        m_SUT = new SystemMetaTypeMgrImpl();

        m_SUT.setBundleContextUtil(m_BundleContextUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEventAdmin(m_EventAdmin);
        when(m_BundleContextUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createMetaTypeMessage(Mockito.any(MetaTypeMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);

        m_SUT.setup();

        // Verify and capture the event handlers being registered.
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(3)).registerService(eq(EventHandler.class), eventHandlerCaptor.capture(),
                Mockito.any(Dictionary.class));

        m_BundleEventHandler = (BundleEventHandler)eventHandlerCaptor.getAllValues().get(0);
        m_ControllerEventHandler = (ControllerEventHandler)eventHandlerCaptor.getAllValues().get(1);
        m_MetaTypeEventHandler = (MetaTypeEventHandler)eventHandlerCaptor.getAllValues().get(2);
    }
    
    /**
     * Test the pre-destroy method.
     * Verify that all event handlers are unregistered.
     */
    @Test
    public void testCleanup()
    {
        m_SUT.cleanup();
        
        //Verify that each event handler called the unregister method.
        verify(m_HandlerReg, times(3)).unregister();
    }
    
    /**
     * Test that each Event Helper has correctly registered for the appropriate topics and
     * filters.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testTopicAndFilterRegistration()
    {
        ArgumentCaptor<Dictionary> dictCaptor = ArgumentCaptor.forClass(Dictionary.class);
        
        verify(m_BundleContext, times(3)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                dictCaptor.capture());
        
        Dictionary bundleHandlerDict = dictCaptor.getAllValues().get(0);
        Dictionary controllerHandlerDict = dictCaptor.getAllValues().get(1);
        Dictionary metaTypeHandlerDict = dictCaptor.getAllValues().get(2);
        
        //Test bundle event handler
        String[] topics = (String[])bundleHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topics[0], 
                is(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat(topics[1], is(BundleMgr.TOPIC_BUNDLE_INFO_REMOVED));
        assertThat(topics[2], is(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED));
        
        //Test controller event handler
        String topic = (String)controllerHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topic, is(ControllerMgr.TOPIC_CONTROLLER_REMOVED));
        
        //Test metatype event handler
        topic = (String)metaTypeHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topic, is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.MetaType.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, MetaTypeMessageType.GetMetaTypeInfoResponse.toString());
        String filter = (String)metaTypeHandlerDict.get(EventConstants.EVENT_FILTER);
        assertThat(filter, is(filterString));
    }   

    /**
     * Test the controller event handler. Verify that all information for a given controller is removed when a
     * controller removed event is received.
     */
    @Test
    public void testHandleRemoveController()
    {
        setupMetatypeList();

        assertThat(m_SUT.getConfigurationsListAsync(CONTROLLERID), is(notNullValue()));
        assertThat(m_SUT.getFactoriesListAsync(CONTROLLERID), is(notNullValue()));

        Map<String, Object> props = new HashMap<>();
        props.put(EventConstants.BUNDLE_ID, BUNDLEID);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, CONTROLLERID);
        Event event = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);

        m_ControllerEventHandler.handleEvent(event);

        assertThat(m_SUT.getConfigurationsListAsync(CONTROLLERID).isEmpty(), is(true));
        assertThat(m_SUT.getFactoriesListAsync(CONTROLLERID).isEmpty(), is(true));
    }

    /**
     * Test the bundle event handler. Verify that all events are handled appropriately.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBundleEventHandler() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        Map<String, Object> props = new HashMap<>();
        props.put(EventConstants.BUNDLE_ID, BUNDLEID);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, CONTROLLERID);
        Event event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED, props);

        m_BundleEventHandler.handleEvent(event);

        props = new HashMap<>();
        props.put(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID, BUNDLEID3);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, CONTROLLERID);
        event = new Event(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE 
                + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);

        m_BundleEventHandler.handleEvent(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(m_MessageFactory, times(2)).createMetaTypeMessage(eq(MetaTypeMessageType.GetMetaTypeInfoRequest),
                messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(CONTROLLERID), eq((ResponseHandler)null));

        GetMetaTypeInfoRequestData infoRequest = (GetMetaTypeInfoRequestData)messageCaptor.getAllValues().get(0);
        assertThat(infoRequest.getBundleId(), is(BUNDLEID));
        infoRequest = (GetMetaTypeInfoRequestData)messageCaptor.getAllValues().get(1);
        assertThat(infoRequest.getBundleId(), is(BUNDLEID3));

        Field metaMapField = m_SUT.getClass().getDeclaredField("m_MetatypeListConfigs");
        metaMapField.setAccessible(true);
        Map<Integer, List<MetaTypeModel>> metaMap = (Map<Integer, List<MetaTypeModel>>) metaMapField.get(m_SUT);
        List<MetaTypeModel> listModels = new ArrayList<MetaTypeModel>();
        listModels.add(new MetaTypeModel("some pid", BUNDLEID));
        listModels.add(new MetaTypeModel("pid 2", BUNDLEID2));
        metaMap.put(CONTROLLERID, listModels);
        
        props = new HashMap<>();
        props.put(EventConstants.BUNDLE_ID, BUNDLEID);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, CONTROLLERID);
        event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_REMOVED, props);

        assertThat(m_SUT.getConfigurationsListAsync(CONTROLLERID).size(), is(2));
        m_BundleEventHandler.handleEvent(event);
        assertThat(m_SUT.getConfigurationsListAsync(CONTROLLERID).size(), is(1));
    }

    private void setupMetatypeList()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(EventConstants.BUNDLE_ID, BUNDLEID);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, CONTROLLERID);
        Event event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED, props);

        m_BundleEventHandler.handleEvent(event);
    }

    /**
     * Test the get config information method. 
     * Verify that null is returned if the meta type information cannot be found for the PID. 
     * Verify that the meta type information is returned for the specified PID.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetConfigInformation() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        //Mock bundle manager information.
        BundleModel testBundle = mock(BundleModel.class);
        List<BundleModel> bundleList = new ArrayList<BundleModel>();
        bundleList.add(testBundle);
        when(testBundle.getBundleId()).thenReturn(BUNDLEID);
        
        assertThat(m_SUT.getConfigInformationAsync(CONTROLLERID, "some PID"), is(nullValue()));
        
        //Added controller and meta type model to map for meta type information.
        Field configMap = m_SUT.getClass().getDeclaredField("m_MetatypeListConfigs");
        configMap.setAccessible(true);
        Map<Integer, List<MetaTypeModel>> map = (Map<Integer, List<MetaTypeModel>>)configMap.get(m_SUT);
        MetaTypeModel model = new MetaTypeModel("some PID", BUNDLEID);
        List<MetaTypeModel> modelList = new ArrayList<MetaTypeModel>();
        modelList.add(model);
        map.put(CONTROLLERID, modelList);
        
        assertThat(m_SUT.getConfigInformationAsync(CONTROLLERID, "some PID"), is(model));
        
        //Verify message sent to retrieve meta info.
        ArgumentCaptor<GetMetaTypeInfoRequestData> requestCaptor = 
                ArgumentCaptor.forClass(GetMetaTypeInfoRequestData.class);
        verify(m_MessageFactory).createMetaTypeMessage(eq(MetaTypeMessageType.GetMetaTypeInfoRequest), 
                requestCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(CONTROLLERID), 
                Mockito.any(ResponseHandler.class));
    }
    
    /**
     * Test the get factory information method. 
     * Verify that null is returned if the meta type information cannot be found for the factory PID. 
     * Verify that the meta type information is returned for the specified factory PID.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetFactoryInformation() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        assertThat(m_SUT.getFactoryInformationAsync(CONTROLLERID, "factory PID"), is(nullValue()));
        
        //Added controller and meta type model to map for meta type information.
        Field configMap = m_SUT.getClass().getDeclaredField("m_MetatypeListFactories");
        configMap.setAccessible(true);
        Map<Integer, List<MetaTypeModel>> map = (Map<Integer, List<MetaTypeModel>>)configMap.get(m_SUT);
        MetaTypeModel model = new MetaTypeModel("factory PID", BUNDLEID);
        List<MetaTypeModel> modelList = new ArrayList<MetaTypeModel>();
        modelList.add(model);
        map.put(CONTROLLERID, modelList);
        
        assertThat(m_SUT.getFactoryInformationAsync(CONTROLLERID, "factory PID"), is(model));
    }
    
    /**
     * Test the metatype event handler.
     * Verify that the event handler handles all the get meta type information response correctly.
     */
    @Test
    public void testMetaTypeEventHandler() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {
        setupMetaTypeInfo();
        
        String ocdName = "name";
        String ocdId = "ID";
        String description = "desc";
        
        ObjectClassDefinitionType ocdMes1 = ObjectClassDefinitionType.newBuilder()
                .setName(ocdName)
                .setId(ocdId)
                .setDescription(description).build();
        //just to verify that if there is no description things don't explode
        ObjectClassDefinitionType ocdMes2 = ObjectClassDefinitionType.newBuilder()
                .setName(ocdName)
                .setId(ocdId).build();
        AttributeDefinitionType attrInt = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.INTEGER).setCardinality(0).setDescription("some description int").setId("intField").
                setName("Int Field").build();
        AttributeDefinitionType attrString = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.STRING).setCardinality(0).setDescription("some description string").
                setId("stringField").setName("String Field").build();
        AttributeDefinitionType attrBool = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.BOOLEAN).setCardinality(0).setDescription("some description bool").
                setId("boolField").setName("Bool Field").build();
        List<AttributeDefinitionType> attributeList = new ArrayList<MetaTypeMessages.AttributeDefinitionType>();
        attributeList.add(attrInt);
        attributeList.add(attrString);
        attributeList.add(attrBool);
                
        MetaTypeInfoType meta1 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).setPid("some.factory").
                setIsFactory(true).addAllAttributes(attributeList).setOcd(ocdMes1).build();
        MetaTypeInfoType meta2 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID2).setPid("some.service.bundle3").
                setIsFactory(false).addAllAttributes(attributeList).setOcd(ocdMes2).build();
        MetaTypeInfoType meta3 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).setPid("some.service.bundle5").
                setIsFactory(false).addAllAttributes(attributeList).setOcd(ocdMes1).build();
        MetaTypeInfoType meta4 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID3).setPid("some.service.bundle6").
                setIsFactory(false).addAllAttributes(attributeList).setOcd(ocdMes2).build();
        MetaTypeInfoType metaExists1 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID2).
                setPid("already.existent.factory").setIsFactory(true).setOcd(ocdMes1)
                .addAllAttributes(attributeList).build();
        MetaTypeInfoType metaExists2 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).
                setPid("already.existent.service").setIsFactory(false).setOcd(ocdMes1)
                .addAllAttributes(attributeList).build();
        List<MetaTypeInfoType> metaInfoList = new ArrayList<MetaTypeInfoType>();
        metaInfoList.add(meta1);
        metaInfoList.add(meta2);
        metaInfoList.add(meta3);
        metaInfoList.add(meta4);
        metaInfoList.add(metaExists1);
        metaInfoList.add(metaExists2);
        
        GetMetaTypeInfoResponseData responseMessage = 
                GetMetaTypeInfoResponseData.newBuilder().addAllMetaType(metaInfoList).build();
        MetaTypeNamespace namespace = MetaTypeNamespace.newBuilder().setType(
                MetaTypeMessageType.GetMetaTypeInfoResponse).setData(responseMessage.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLERID, 0, 
                Namespace.MetaType, 5000, namespace);
        
        //Mock event
        Map<String, Object> props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, responseMessage);
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_MetaTypeEventHandler.handleEvent(event);
        
        List<MetaTypeModel> modelList = m_SUT.getConfigurationsListAsync(CONTROLLERID);
        assertThat(modelList.size(), is(4));
        List<String> metaPidList = new ArrayList<String>();
        for (MetaTypeModel model: modelList)
        {
            metaPidList.add(model.getPid());
            verifyAttributes(model.getAttributes());
        }
        assertThat(metaPidList, hasItems("some.service.bundle3", "some.service.bundle5", "some.service.bundle6", 
                "already.existent.service"));
        
        List<MetaTypeModel> factoryModelList = m_SUT.getFactoriesListAsync(CONTROLLERID);
        assertThat(factoryModelList.size(), is(2));
        metaPidList = new ArrayList<String>();
        for (MetaTypeModel model: factoryModelList)
        {
            metaPidList.add(model.getPid());
            verifyAttributes(model.getAttributes());
        }
        assertThat(metaPidList, hasItems("some.factory", "already.existent.factory"));
    }
    
    /**
     * Test the metatype event handler.
     * Verify that the event handler handles all the get meta type information response correctly.
     * And will ignore the attributes if the OCD is a partial.
     */
    @Test
    public void testMetaTypeEventHandlerNoAddPartial() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        setupMetaTypeInfo();
        
        String ocdName = "name";
        String ocdId = "ID";
        String description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION;
        
        ObjectClassDefinitionType ocdMes1 = ObjectClassDefinitionType.newBuilder()
                .setName(ocdName)
                .setId(ocdId)
                .setDescription(description).build();
        ObjectClassDefinitionType ocdMes2 = ObjectClassDefinitionType.newBuilder()
                .setName(ocdName)
                .setId(ocdId).build();
        AttributeDefinitionType attrInt = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.INTEGER).setCardinality(0).setDescription("some description int").setId("intField").
                setName("Int Field").build();
        AttributeDefinitionType attrString = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.STRING).setCardinality(0).setDescription("some description string").
                setId("stringField").setName("String Field").build();
        AttributeDefinitionType attrBool = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.BOOLEAN).setCardinality(0).setDescription("some description bool").
                setId("boolField").setName("Bool Field").build();
        List<AttributeDefinitionType> attributeList = new ArrayList<MetaTypeMessages.AttributeDefinitionType>();
        attributeList.add(attrInt);
        attributeList.add(attrString);
        attributeList.add(attrBool);
                
        MetaTypeInfoType meta1 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).setPid("some.factory").
                setIsFactory(true).addAllAttributes(attributeList).setOcd(ocdMes2).build();
        MetaTypeInfoType meta2 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID2).setPid("some.service.bundle3").
                setIsFactory(false).addAllAttributes(attributeList).setOcd(ocdMes2).build();
        MetaTypeInfoType meta3 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID2).
                setPid("something.different").setIsFactory(true).setOcd(ocdMes1)
                .addAllAttributes(attributeList).build();
        MetaTypeInfoType meta4 = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).
                setPid("blah.blah").setIsFactory(false).setOcd(ocdMes1)
                .addAllAttributes(attributeList).build();
        List<MetaTypeInfoType> metaInfoList = new ArrayList<MetaTypeInfoType>();
        metaInfoList.add(meta1);
        metaInfoList.add(meta2);
        metaInfoList.add(meta3);
        metaInfoList.add(meta4);
        
        GetMetaTypeInfoResponseData responseMessage = 
                GetMetaTypeInfoResponseData.newBuilder().addAllMetaType(metaInfoList).build();
        MetaTypeNamespace namespace = MetaTypeNamespace.newBuilder().setType(
                MetaTypeMessageType.GetMetaTypeInfoResponse).setData(responseMessage.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLERID, 0, 
                Namespace.MetaType, 5000, namespace);
        
        //Mock event
        Map<String, Object> props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, responseMessage);
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_MetaTypeEventHandler.handleEvent(event);
        
        List<MetaTypeModel> modelList = m_SUT.getConfigurationsListAsync(CONTROLLERID);
        assertThat(modelList.size(), is(2));
        List<String> metaPidList = new ArrayList<String>();
        for (MetaTypeModel model: modelList)
        {
            metaPidList.add(model.getPid());
        }
        assertThat(metaPidList, hasItems("some.service.bundle3", "already.existent.service"));
        assertThat(metaPidList, not(hasItem("blah.blah")));
        
        List<MetaTypeModel> factoryModelList = m_SUT.getFactoriesListAsync(CONTROLLERID);
        assertThat(factoryModelList.size(), is(2));
        metaPidList = new ArrayList<String>();
        for (MetaTypeModel model: factoryModelList)
        {
            metaPidList.add(model.getPid());
        }
        assertThat(metaPidList, hasItems("some.factory", "already.existent.factory"));
        assertThat(metaPidList, not(hasItem("something.different")));
    }
    
    /**
     * Verify if a bundle is updated and an attribute has been removed then the local model reflects that change.
     */
    @Test
    public void testAttributeRemoved() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {
        setupMetaTypeInfo();
        
        //Verify model before update.
        MetaTypeModel model = m_SUT.getConfigInformationAsync(CONTROLLERID, "already.existent.service");
        assertThat(model.getBundleId(), is(BUNDLEID));
        assertThat(model.getPid(), is("already.existent.service"));
        List<AttributeModel> attributes = model.getAttributes();
        assertThat(attributes.size(), is(3));
        assertThat(attributes.get(0).getId(), is("intField"));
        assertThat(attributes.get(1).getId(), is("stringField"));
        assertThat(attributes.get(2).getId(), is("boolField"));
        
        String ocdName = "name";
        String ocdId = "ID";
        String description = "desc";
        
        ObjectClassDefinitionType ocdMes1 = ObjectClassDefinitionType.newBuilder()
                .setName(ocdName)
                .setId(ocdId)
                .setDescription(description).build();
        
        AttributeDefinitionType attrInt = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.INTEGER).setCardinality(0).setDescription("some description int").setId("intField").
                setName("Int Field").build();
        AttributeDefinitionType attrString = AttributeDefinitionType.newBuilder().setAttributeType(
                AttributeDefinition.STRING).setCardinality(0).setDescription("some description string").
                setId("stringField").setName("String Field").build();
        List<AttributeDefinitionType> attributeList = new ArrayList<MetaTypeMessages.AttributeDefinitionType>();
        attributeList.add(attrInt);
        attributeList.add(attrString);
        
        MetaTypeInfoType metaUpdated = MetaTypeInfoType.newBuilder().setBundleId(BUNDLEID).
                setPid("already.existent.service").setIsFactory(false).
                setOcd(ocdMes1).addAllAttributes(attributeList).build();
        List<MetaTypeInfoType> metaInfoList = new ArrayList<MetaTypeInfoType>();
        metaInfoList.add(metaUpdated);
        
        GetMetaTypeInfoResponseData responseMessage = 
                GetMetaTypeInfoResponseData.newBuilder().addAllMetaType(metaInfoList).build();
        MetaTypeNamespace namespace = MetaTypeNamespace.newBuilder().setType(
                MetaTypeMessageType.GetMetaTypeInfoResponse).setData(responseMessage.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLERID, 0, 
                Namespace.MetaType, 5000, namespace);
        
        //Mock event
        Map<String, Object> props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, responseMessage);
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_MetaTypeEventHandler.handleEvent(event);
        
        //Verify model after update.
        model = m_SUT.getConfigInformationAsync(CONTROLLERID, "already.existent.service");
        assertThat(model.getBundleId(), is(BUNDLEID));
        assertThat(model.getPid(), is("already.existent.service"));
        attributes = model.getAttributes();
        assertThat(attributes.size(), is(2));
        assertThat(attributes.get(0).getId(), is("intField"));
        assertThat(attributes.get(1).getId(), is("stringField"));
    }
    
    /**
     * Verify list of attributes contained in a model.
     */
    private void verifyAttributes(List<AttributeModel> list)
    {
        assertThat(list.size(), is(3));
        List<String> idList = new ArrayList<String>();
        for (AttributeModel attribute: list)
        {
            idList.add(attribute.getId());
        }
        assertThat(idList, hasItems("intField", "stringField", "boolField"));
        AttributeModel attribute = list.get(idList.indexOf("intField"));
        assertThat(attribute.getName(), is("Int Field"));
        assertThat(attribute.getType(), is(AttributeDefinition.INTEGER));
        attribute = list.get(idList.indexOf("stringField"));
        assertThat(attribute.getName(), is("String Field"));
        assertThat(attribute.getType(), is(AttributeDefinition.STRING));
        attribute = list.get(idList.indexOf("boolField"));
        assertThat(attribute.getName(), is("Bool Field"));
        assertThat(attribute.getType(), is(AttributeDefinition.BOOLEAN));
    }
    
    /**
     * Create meta type information for testing.
     */
    @SuppressWarnings("unchecked")
    private void setupMetaTypeInfo() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        AttributeModel attrInt = ModelFactory.createAttributeModel("intField", AttributeDefinition.INTEGER);
        AttributeModel attrString = ModelFactory.createAttributeModel("stringField", AttributeDefinition.STRING);
        AttributeModel attrBool = ModelFactory.createAttributeModel("boolField", AttributeDefinition.BOOLEAN);
        
        Field metaMapField = m_SUT.getClass().getDeclaredField("m_MetatypeListConfigs");
        metaMapField.setAccessible(true);
        Map<Integer, List<MetaTypeModel>> metaMap = (Map<Integer, List<MetaTypeModel>>)metaMapField.get(m_SUT);
        MetaTypeModel model = new MetaTypeModel("already.existent.service", BUNDLEID);
        model.getAttributes().add(attrInt);
        model.getAttributes().add(attrString);
        model.getAttributes().add(attrBool);
        List<MetaTypeModel> listModels = new ArrayList<MetaTypeModel>();
        listModels.add(model);
        metaMap.put(CONTROLLERID, listModels);
        
        metaMapField = m_SUT.getClass().getDeclaredField("m_MetatypeListFactories");
        metaMapField.setAccessible(true);
        Map<Integer, List<MetaTypeModel>> metaMapFactories = (Map<Integer, List<MetaTypeModel>>)metaMapField.get(m_SUT);
        MetaTypeModel factoryModel = new MetaTypeModel("already.existent.factory", BUNDLEID2);
        factoryModel.getAttributes().add(attrInt);
        factoryModel.getAttributes().add(attrString);
        factoryModel.getAttributes().add(attrBool);
        List<MetaTypeModel> listFactoryModels = new ArrayList<MetaTypeModel>();
        listFactoryModels.add(factoryModel);
        metaMapFactories.put(CONTROLLERID, listFactoryModels);
    }
}
