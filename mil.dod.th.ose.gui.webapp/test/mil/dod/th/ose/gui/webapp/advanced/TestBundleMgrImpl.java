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
package mil.dod.th.ose.gui.webapp.advanced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;  // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.BundleMessages.BundleErrorCode;
import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespaceErrorData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoResponseData;
import mil.dod.th.core.remote.proto.EventMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.advanced.BundleMgrImpl.BundleMessageEventHelper;
import mil.dod.th.ose.gui.webapp.advanced.BundleMgrImpl.ControllerEventHelper;
import mil.dod.th.ose.gui.webapp.advanced.BundleMgrImpl.RemoteBundleEventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.shared.OSGiEventConstants;

/**
 *Test class for the {@link BundleMgrImpl} class.
 */
public class TestBundleMgrImpl
{
    private BundleMgrImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private MessageFactory m_MessageFactory;
    private BundleContextUtil m_BundleUtil;
    private BundleMessageEventHelper m_BundleHelper;
    private ControllerEventHelper m_ControllerHelper;
    private RemoteBundleEventHelper m_RemoteBundleHelper;
    private EventAdmin m_EventAdmin;
    private Event m_Event;
    private MessageWrapper m_MessageWrapper;
    private BundleContext m_BundleContext;
    
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_BundleUtil = mock(BundleContextUtil.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_BundleContext = mock(BundleContext.class);
        m_Event = mock(Event.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        
        m_SUT = new BundleMgrImpl();
        
        m_SUT.setBundleUtil(m_BundleUtil);
        m_SUT.setGrowlUtil(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEventAdmin(m_EventAdmin);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        m_SUT.setup();
        
        //Verify and capture the event handlers being registered.
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(3)).registerService(eq(EventHandler.class), eventHandlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(3)).getBundleContext();
        
        when(m_MessageFactory.createBundleMessage(Mockito.any(BundleMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        m_BundleHelper = (BundleMessageEventHelper)eventHandlerCaptor.getAllValues().get(0);
        m_ControllerHelper = (ControllerEventHelper)eventHandlerCaptor.getAllValues().get(1);
        m_RemoteBundleHelper = (RemoteBundleEventHelper)eventHandlerCaptor.getAllValues().get(2);
    }
    
    /**
     * Test the pre-destroy method.
     * Verify that all event handlers are unregistered.
     */
    @Test
    public void testCleanup()
    {
        m_SUT.unregisterEventHelper();
        
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
        Dictionary remoteBundleHandlerDict = dictCaptor.getAllValues().get(2);
        
        //Test bundler handler
        String topic = (String)bundleHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topic, is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        String filterString = String.format("(&(%s=%s)(|(%s=%s)(%s=%s)))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Bundle.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.GetBundleInfoResponse.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.BundleNamespaceError.toString());
        String filter = (String)bundleHandlerDict.get(EventConstants.EVENT_FILTER);
        assertThat(filter, is (filterString));
        
        //Test controller handler
        topic = (String)controllerHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topic, is(ControllerMgr.TOPIC_CONTROLLER_REMOVED));
        
        //Test remote bundle handler
        List<String> topicList = (List<String>)remoteBundleHandlerDict.get(EventConstants.EVENT_TOPIC);
        assertThat(topicList.size(), is(5));
        assertThat(topicList.get(0), 
                is(OSGiEventConstants.TOPIC_BUNDLE_STARTED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat(topicList.get(1), 
                is(OSGiEventConstants.TOPIC_BUNDLE_STOPPED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat(topicList.get(2), 
                is(OSGiEventConstants.TOPIC_BUNDLE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat(topicList.get(3), 
                is(OSGiEventConstants.TOPIC_BUNDLE_INSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat(topicList.get(4), 
                is(OSGiEventConstants.TOPIC_BUNDLE_UNINSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
    }
    
    /**
     * Test the get bundles method.
     * Verify that the the proper list is returned and that controller is added to the list if it is not already known.
     * Verify the correct requests are sent out.
     */
    @Test
    public void testGetBundles()
    {
        ArgumentCaptor<EventMessages.EventRegistrationRequestData> registrationMsg = 
                ArgumentCaptor.forClass(EventMessages.EventRegistrationRequestData.class);
        
        //Should be true since the controller is known by the controller list and therefore has no known bundles
        //until the bundles request message is responded to by the controller.
        assertThat(m_SUT.getBundlesAsync(100).isEmpty(), is(true));
        
        assertThat(m_SUT.getBundlesAsync(100).isEmpty(), is(true));
        
        ArgumentCaptor<GetBundleInfoRequestData> msgCaptor = ArgumentCaptor.forClass(GetBundleInfoRequestData.class);
        
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.GetBundleInfoRequest), msgCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(100), Mockito.any(RemoteEventRegistrationHandler.class));
        verify(m_MessageFactory).createEventAdminMessage(eq(EventAdminMessageType.EventRegistrationRequest), 
                registrationMsg.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(100), Mockito.any(RemoteEventRegistrationHandler.class));
        
        GetBundleInfoRequestData capValue = msgCaptor.getValue();
        
        assertThat(capValue, notNullValue());
        
        assertThat(capValue.hasBundleId(), is(false));
        assertThat(capValue.hasBundleDescription(), is(true));
        assertThat(capValue.getBundleDescription(), is(true));
        
        assertThat(capValue.hasBundleLastModified(), is(true));
        assertThat(capValue.getBundleLastModified(), is(true));
        
        assertThat(capValue.hasBundleLocation(), is(true));
        assertThat(capValue.getBundleLocation(), is(true));
        
        assertThat(capValue.hasBundleName(), is(true));
        assertThat(capValue.getBundleName(), is(true));
        
        assertThat(capValue.hasBundleState(), is(true));
        assertThat(capValue.getBundleState(), is(true));
        
        assertThat(capValue.hasBundleSymbolicName(), is(true));
        assertThat(capValue.getBundleSymbolicName(), is(true));
        
        assertThat(capValue.hasBundleVendor(), is(true));
        assertThat(capValue.getBundleVendor(), is(true));
        
        assertThat(capValue.hasBundleVersion(), is(true));
        assertThat(capValue.getBundleVersion(), is(true));
        
        assertThat(capValue.hasPackageExports(), is(true));
        assertThat(capValue.getPackageExports(), is(true));
        
        assertThat(capValue.hasPackageImports(), is(true));
        assertThat(capValue.getPackageImports(), is(true));
        
        List<String> topics = registrationMsg.getValue().getTopicList();
        assertThat(topics.contains("org/osgi/framework/BundleEvent/*"), is(true));
    }
    
    /**
     * Test the retrieve bundle by location method.
     * Verify that the correct bundle model is returned.
     */
    @Test
    public void testRetrieveBundleByLocation() throws SecurityException, NoSuchFieldException, IllegalArgumentException,
        IllegalAccessException
    {
        BundleModel bundle = addBundleToMap(Bundle.ACTIVE);
        
        //Verify that the bundle model is retrieved when the proper controller ID and bundle ID are specified.
        assertThat(m_SUT.retrieveBundleByLocationAsync(25, "Location"), is(bundle));    
    }
    
    /**
     * Test the local event handler handling bundle info response messages.
     * Verify that the handler takes appropriate actions to handle a bundle info response message.
     */
    @Test
    public void testHandleEventBundleInfo()
    {
        m_SUT.getBundlesAsync(25);
        
        GetBundleInfoRequestData req = GetBundleInfoRequestData.newBuilder().setBundleId(0).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, Namespace.Bundle, 0,
                req);

        GetBundleInfoResponseData.Builder infoBuilder = GetBundleInfoResponseData.newBuilder();

        BundleInfoType bundleID1 = BundleInfoType.newBuilder().setBundleName("Test Bundle 1").setBundleId(1L).
                setBundleLocation("location").addPackageExport("").addPackageImport("").build();
        
        BundleInfoType bundleID2 = BundleInfoType.newBuilder().setBundleName("Test Bundle 2").setBundleId(2L).
                setBundleLocation("location 2").addPackageExport("").addPackageImport("").build();
        
        BundleInfoType bundleID3 = BundleInfoType.newBuilder().setBundleName("Test Bundle 3").setBundleId(3L).
                setBundleLocation("location 3").addPackageExport("").addPackageImport("").build();
        
        List<BundleInfoType> bundleInfo = new ArrayList<BundleInfoType>();
        bundleInfo.add(bundleID1);
        bundleInfo.add(bundleID2);
        bundleInfo.add(bundleID3);
        infoBuilder.addAllInfoData(bundleInfo);
        
        Map<String, Object> props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.GetBundleInfoResponse.toString());
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, infoBuilder.build());
        m_Event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED, props);
       
        m_BundleHelper.handleEvent(m_Event);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED));
        assertThat((Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(25));
        assertThat((Long)event.getProperty(EventConstants.BUNDLE_ID), is(nullValue()));
        
        List<BundleModel> bundles = m_SUT.getBundlesAsync(25);
        assertThat(bundles.size(), is(3));
        assertThat(bundles.get(0).getBundleName(), is("Test Bundle 1"));
        assertThat(bundles.get(1).getBundleName(), is("Test Bundle 2"));
        assertThat(bundles.get(2).getBundleName(), is("Test Bundle 3"));
        
        BundleInfoType typeModified = BundleInfoType.newBuilder().setBundleName("Altered").setBundleId(1L).
                addPackageExport("").addPackageImport("").build();
        
        bundleInfo.clear();
        bundleInfo.add(typeModified);
        infoBuilder.clearInfoData();
        infoBuilder.addAllInfoData(bundleInfo);
        
        props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.GetBundleInfoResponse.toString());
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, infoBuilder.build());
        m_Event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED, props);
        
        m_BundleHelper.handleEvent(m_Event);
        
        bundles = m_SUT.getBundlesAsync(25);
        assertThat(bundles.size(), is(3));
        assertThat(bundles.get(0).getBundleName(), is("Altered"));
        assertThat(bundles.get(1).getBundleName(), is("Test Bundle 2"));
        assertThat(bundles.get(2).getBundleName(), is("Test Bundle 3"));
        
        ArgumentCaptor<Event> eventCaptorAlt = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptorAlt.capture());
        
        Event eventAlt = eventCaptorAlt.getAllValues().get(1);
        assertThat(eventAlt.getTopic(), is(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED));
        assertThat((Integer)eventAlt.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(25));
        assertThat((Long)eventAlt.getProperty(EventConstants.BUNDLE_ID), is(1L));
        assertThat((String)eventAlt.getProperty(BundleMgr.EVENT_PROP_BUNDLE_LOCATION), is(""));
    }
    
    /**
     * Test the local event handler handling a bundle error message.
     * Verify that a growl message with the error information is displayed when an error message is received.
     */
    @Test
    public void testBundleEventError()
    {
        GetBundleInfoRequestData req = GetBundleInfoRequestData.newBuilder().setBundleId(0).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, Namespace.Bundle, 0,
                req);
        BundleNamespaceErrorData errorMsg = BundleNamespaceErrorData.newBuilder().setError(
                BundleErrorCode.OSGiBundleException).setErrorDescription("Error Description!").build();

        Map<String, Object> props = new HashMap<>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.BundleNamespaceError.toString());
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, errorMsg);
        m_Event = new Event(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED, props);

        m_BundleHelper.handleEvent(m_Event);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), Mockito.anyString(), 
                Mockito.anyString());
    }
    
    /**
     * Test the bundle event handler handling an update bundle event.
     * Verify that a message is sent to the control to retrieve the updated bundle information.
     */
    @Test
    public void testHandleBundleEventUpdate()
    {
        m_SUT.getBundlesAsync(25);
        
        setupBundleEvent("UPDATED_REMOTE");
        
        ArgumentCaptor<GetBundleInfoRequestData> msgCaptor = ArgumentCaptor.forClass(GetBundleInfoRequestData.class);
        
        m_RemoteBundleHelper.handleEvent(m_Event);
        
        verify(m_MessageFactory, times(2)).createBundleMessage(
                eq(BundleMessageType.GetBundleInfoRequest), msgCaptor.capture());
        verify(m_MessageWrapper, times(3)).queue(eq(25), Mockito.any(ResponseHandler.class));
    }
    
    /**
     * Test the bundle event handler handling a start bundle event.
     * Verify that the bundle's state is changed to active.
     */
    @Test
    public void testHandleBundleEventStart() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        m_SUT.getBundlesAsync(25);
        
        setupBundleEvent("STARTED_REMOTE");
        
        addBundleToMap(Bundle.INSTALLED);
        
        assertThat(m_SUT.getBundlesAsync(25).get(0).getState(), is("Installed"));
        
        m_RemoteBundleHelper.handleEvent(m_Event);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(BundleMgr.TOPIC_BUNDLE_STATUS_UPDATED));
        assertThat(event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)25));
        assertThat(event.getProperty(EventConstants.BUNDLE_ID), is((Object)5L));
        assertThat(event.getProperty(BundleMgr.EVENT_PROP_BUNDLE_LOCATION), is((Object)"Location"));
        
        assertThat(m_SUT.getBundlesAsync(25).get(0).getState(), is("Active"));
    }
    
    /**
     * Test the bundle event handler handling a stop bundle event.
     * Verify that the bundle's state is changed to resolved.
     */
    @Test
    public void testHandleSendEventStop() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {
        m_SUT.getBundlesAsync(25);
        
        setupBundleEvent("STOPPED_REMOTE");
        
        addBundleToMap(Bundle.ACTIVE);
        
        assertThat(m_SUT.getBundlesAsync(25).get(0).getState(), is("Active"));
        
        m_RemoteBundleHelper.handleEvent(m_Event);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(BundleMgr.TOPIC_BUNDLE_STATUS_UPDATED));
        assertThat(event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)25));
        assertThat(event.getProperty(EventConstants.BUNDLE_ID), is((Object)5L));
        assertThat(event.getProperty(BundleMgr.EVENT_PROP_BUNDLE_LOCATION), is((Object)"Location"));
        
        assertThat(m_SUT.getBundlesAsync(25).get(0).getState(), is("Resolved"));
    }
    
    /**
     * Test the bundle event handler handling an uninstall bundle event.
     * Verify that the bundle has been removed from the controller's list of installed bundles.
     */
    @Test
    public void testHandleBundleEventUninstall() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        m_SUT.getBundlesAsync(25);
        
        setupBundleEvent("UNINSTALLED_REMOTE");
        
        addBundleToMap(Bundle.ACTIVE);
        
        assertThat(m_SUT.getBundlesAsync(25).get(0).getState(), is("Active"));
        
        m_RemoteBundleHelper.handleEvent(m_Event);

        assertThat(m_SUT.getBundlesAsync(25).isEmpty(), is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        
        assertThat(event.getTopic(), is(BundleMgr.TOPIC_BUNDLE_INFO_REMOVED));
        assertThat(event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)25));
        assertThat(event.getProperty(EventConstants.BUNDLE_ID), is((Object)5L));
        assertThat(event.getProperty(BundleMgr.EVENT_PROP_BUNDLE_LOCATION), is((Object)"Location"));
    }
    
    /**
     * Test the bundle event handler handling an install bundle event.
     * Verify that a message is sent to the controller to retrieve information about the installed bundle.
     */
    @Test
    public void testHandleBundleEventInstall()
    {
        m_SUT.getBundlesAsync(25);
        
        setupBundleEvent("INSTALLED_REMOTE");
        
        m_RemoteBundleHelper.handleEvent(m_Event);
        
        ArgumentCaptor<GetBundleInfoRequestData> captor = ArgumentCaptor.forClass(GetBundleInfoRequestData.class);
        verify(m_MessageFactory, times(2)).
            createBundleMessage(eq(BundleMessageType.GetBundleInfoRequest), captor.capture());
        verify(m_MessageWrapper, times(3)).queue(eq(25), Mockito.any(ResponseHandler.class));
        
        assertThat(captor.getValue().getBundleId(), is(5L));
    }
    
    /**
     * Test the controller event handler handling a controller removed event.
     * Verify that controller was removed from the local list of controllers.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testHandleRemoveController() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        m_SUT.getBundlesAsync(25);
        
        Map<String, Object> props = new HashMap<>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        Event event = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);

        Field controllerMapField = m_SUT.getClass().getDeclaredField("m_ControllerBundles");
        controllerMapField.setAccessible(true);
        
        Map<Integer, List<BundleModel>> controllerList = (Map<Integer, List<BundleModel>>)controllerMapField.get(m_SUT);
        assertThat(controllerList.keySet().size(), is(1));
        
        m_ControllerHelper.handleEvent(event);
        
        controllerList = (Map<Integer, List<BundleModel>>)controllerMapField.get(m_SUT);
        assertThat(controllerList.keySet().size(), is(0));
    }
    
    /**
     * Method used in testing to set up a send event message.
     */
    private void setupBundleEvent(String topic)
    {
        Map<String, Object> props = new HashMap<>();
        props.put("bundle.id", 5L);
        props.put("controller.id", 25);
        m_Event = new Event("org/osgi/framework/BundleEvent/" + topic, props);
    }
    
    /**
     * Adds a bundle to the bundle manager map.
     */
    @SuppressWarnings("unchecked")
    private BundleModel addBundleToMap(int bundleState) throws SecurityException, NoSuchFieldException, 
        IllegalArgumentException, IllegalAccessException
    {
      //Create the bundle model to be retrieved.
        BundleModel bundle = new BundleModel(createBundleInfo(bundleState));
        
        //Place the bundle model in the map contained within the bundle manager.
        Field map = m_SUT.getClass().getDeclaredField("m_ControllerBundles");
        map.setAccessible(true);
        Map<Integer, Map<Long, BundleModel>> controllerBundles = (Map<Integer, Map<Long, BundleModel>>)map.get(m_SUT);
        Map<Long, BundleModel> bundleList = new HashMap<Long, BundleModel>();
        bundleList.put(bundle.getBundleId(), bundle);
        controllerBundles.put(25, bundleList);
        
        return bundle;
    }
    
    /**
     * Method used in testing to create a bundle info response message that can be used to create a bundle model.
     */
    private BundleInfoType createBundleInfo(int bundleState)
    {
        BundleInfoType bundleInfo = BundleInfoType.newBuilder().
                setBundleDescription("Descrip").setBundleName("SomeBundle").setBundleId(Long.valueOf(5)).
                setBundleLocation("Location").setBundleSymbolicName("SymbolicName").setBundleVendor("SomeVendor").
                setBundleVersion("1.0.1").setBundleLastModified(Long.valueOf(10)).setBundleState(bundleState).
                addPackageExport("").addPackageImport("").build();
        
        return bundleInfo;
    }
}
