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
package mil.dod.th.ose.gui.webapp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.types.OperatingSystemTypeEnum;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.api.ControllerEncryptionConstants;
import mil.dod.th.ose.gui.api.EncryptionTypeManager;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.CapabilitiesFakeObjects;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgrImpl.ChannelEventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgrImpl.CleanupEventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgrImpl.EncryptionEventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgrImpl.EventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgrImpl.ControllerModeEventHandler;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;
import mil.dod.th.remote.lexicon.controller.capability.ControllerCapabilitiesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

/**
 * Tests the capabilities of the controller manager.
 * @author callen
 *
 */
public class TestControllerMgrImpl 
{
    //controller IDs
    private static int CONT_ID_1 = 32165;
    private static int CONT_ID_2 = 789654;
    private static int CONT_ID_3 = 14785;
    private static int CONT_ID_4 = 15932;
    private static int CONT_ID_5 = java.lang.Integer.MAX_VALUE;
    
    private ControllerMgrImpl m_SUT;
    private RemoteChannelLookup m_ChannelLookup;
    private EventAdmin m_EventAdmin;
    private GrowlMessageUtil m_GrowlUtil;
    private BundleContextUtil m_BundleUtil;
    private EventHelper m_Helper;
    private ChannelEventHelper m_ChannelHelper;
    private JaxbProtoObjectConverter m_Converter;
    private ControllerModeEventHandler m_ModeEventHandler;
    private BaseMessages.OperationMode m_Mode; //used to test proper updating of operation mode
    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
    private EncryptionEventHelper m_EncryptionEventHelper;
    private CleanupEventHelper m_CleanupEventHelper;
    private EncryptionTypeManager m_EncryptionTypeManager;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);

    @SuppressWarnings("unchecked")//because of the use of the dictionary for the event helper
    @Before
    public void setUp()
    {
        //mock services
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_ChannelLookup = mock(RemoteChannelLookup.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_EncryptionTypeManager = mock(EncryptionTypeManager.class);
        
        //create controller manager
        m_SUT = new ControllerMgrImpl();
        
        //set dependencies
        m_SUT.setRemoteChannelLookup(m_ChannelLookup);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setBundleContextUtility(m_BundleUtil);
        m_SUT.setConverter(m_Converter);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEncryptionTypeManager(m_EncryptionTypeManager);
        
        //mock remote channel lookup to have no channel initially.
        when(m_ChannelLookup.getAllChannels()).thenReturn(new HashMap<Integer, Set<RemoteChannel>>());
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createBaseMessage(Mockito.any(BaseMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEncryptionInfoMessage(Mockito.any(EncryptionInfoMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //register helper
        m_SUT.setup();
        
        //verify event handlers are registered
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(5)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(5)).getBundleContext();
        
        //get captor value and assign to the event helper and the channel event helper
        m_Helper = (EventHelper)captor.getAllValues().get(0);
        m_ChannelHelper = (ChannelEventHelper)captor.getAllValues().get(1);
        m_ModeEventHandler = (ControllerModeEventHandler)captor.getAllValues().get(2);
        m_EncryptionEventHelper = (EncryptionEventHelper)captor.getAllValues().get(3);
        m_CleanupEventHelper = (CleanupEventHelper)captor.getAllValues().get(4);
    }
    
    /**
     * Verify all event helpers are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterEventHelper();
        verify(m_HandlerReg, times(5)).unregister();
    }
    
    /**
     * Verify that a controller model is created if the remote channel lookup contains any channels upon setup.
     */
    @Test
    public void testSetupInitialChannel()
    {
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(SocketChannel.class));
        final String hostname = "12.11.10.9";
        when(((SocketChannel)remoteChannel).getHost()).thenReturn(hostname);
        when(((SocketChannel)remoteChannel).getPort()).thenReturn(5001);
        
        final Map<Integer, Set<RemoteChannel>> channelsMap = new HashMap<>();
        final Set<RemoteChannel> channelsSet = new HashSet<>();
        channelsSet.add(remoteChannel);
        channelsMap.put(100, channelsSet);
        
        //Setup remote channel lookup so that there is one channel initially.
        when(m_ChannelLookup.getAllChannels()).thenReturn(channelsMap);
        
        m_SUT.setup();
        
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        final Event controllerAdded = eventCaptor.getValue();
        assertThat(controllerAdded.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_ADDED));
        assertThat(controllerAdded.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)100));
        
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Controller Info:", 
                String.format("Added controller 0x%08x", 100));
    }

    /**
     * Test getting a controller by its ID.
     * 
     * Test that all controllers are added and retrievable.
     */
    @Test
    public void testGetController()
    {
        //mock remote channel lookup behavior
        RemoteChannel chan = mock(RemoteChannel.class);
        List<RemoteChannel> setChanns = new ArrayList<RemoteChannel>();
        setChanns.add(chan);
        
        //sets up the map of channels by controller ID
        setUpFourChannels(setChanns);
        
        //create 4 channels
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_1));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_2));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_3));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_4));
        
        //get one controller
        assertThat(m_SUT.getController(CONT_ID_4), is(notNullValue()));
        
        //get all controllers, there should be the four just added
        assertThat(m_SUT.getAllControllers().size(), is(4));
        
        assertThat(m_SUT.getController(CONT_ID_1).needsCleanupRequest(), is(true));
        assertThat(m_SUT.getController(CONT_ID_2).needsCleanupRequest(), is(true));
        assertThat(m_SUT.getController(CONT_ID_3).needsCleanupRequest(), is(true));
        assertThat(m_SUT.getController(CONT_ID_4).needsCleanupRequest(), is(true));
    }
    
    /**
     * Test removal of a controller from the controller manager.
     * Verify that removing a controller that doesn't exist posts a 'growl' message.
     */
    @Test
    public void testRemoveController()
    {
        //mocking
        SocketChannel socChannel = mock(SocketChannel.class);
        when(socChannel.getChannelType()).thenReturn(RemoteChannelTypeEnum.SOCKET);
        TransportChannel channel = mock(TransportChannel.class);
        when(channel.getChannelType()).thenReturn(RemoteChannelTypeEnum.TRANSPORT);
        
        List<RemoteChannel> channelsForCont2 = new ArrayList<RemoteChannel>();
        channelsForCont2.add(channel);
        List<RemoteChannel> channelsForCont3 = new ArrayList<RemoteChannel>();
        channelsForCont3.add(socChannel);
        when(m_ChannelLookup.getChannels(CONT_ID_2)).thenReturn(channelsForCont2);
        when(m_ChannelLookup.getChannels(CONT_ID_3)).thenReturn(channelsForCont3);
        
        //mock remote channel lookup behavior
        RemoteChannel chan = mock(RemoteChannel.class);
        List<RemoteChannel> setChanns = new ArrayList<RemoteChannel>();
        setChanns.add(chan);
        when(m_ChannelLookup.getChannels(CONT_ID_1)).thenReturn(setChanns);
        when(m_ChannelLookup.getChannels(CONT_ID_4)).thenReturn(setChanns);
        
        //sets up the map of channels by controller ID
        setUpFourChannels(setChanns);
        
        //add controllers
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_1));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_2));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_3));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_4));
        
        //get all controllers, there should be the four just added
        assertThat(m_SUT.getAllControllers().size(), is(4));
        
        //remove one of the controllers
        m_SUT.removeController(CONT_ID_2);
        verify(m_ChannelLookup).removeChannel(channel);
        when(m_ChannelLookup.getChannels(CONT_ID_2)).thenReturn(new ArrayList<RemoteChannel>());
        
        //verify other three controller are still in lookup
        assertThat(m_SUT.getAllControllers().size(), is(3));
        
        //remove one of the controllers
        m_SUT.removeController(CONT_ID_3);
        verify(m_ChannelLookup).removeChannel(socChannel);
        
        //remove one of the controllers that was already removed should register a 'growl' message
        try
        {
            m_SUT.removeController(CONT_ID_3);
            fail("Expected exception! Controller was already removed.");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), anyString(), anyString());
    }
    
    /**
     * Verify no event is posted for EventHelper when there are no known controllers.
     */
    @Test
    public void testEventHelperInvalidController()
    {
        //mock sys info response for unknown controller
        mockControllerMessage(6666, BaseMessageType.ControllerInfo); 
        
        // verify event is not sent for unknown controller
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, never()).postEvent(eventCaptor.capture());
    }
    
    /**
     * Test event handling.
     * 
     * Verify that controller information is updated.
     */
    @Test
    public void testEventHelperSystemInfo()
    {
        //controllerId
        int contNew = 6431;                    
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));
        
        //mock sys info response for new controller
        mockControllerMessage(contNew, BaseMessageType.ControllerInfo); 

        //get controller
        ControllerModel controller = m_SUT.getController(contNew);
 
        //verify
        assertThat(controller.getName(), is("Bacon"));
        assertThat(controller.getVersion(), is("TheBest"));
        assertThat(controller.getBuildInfo().size(), is(1));
        assertThat(controller.getBuildInfo(), hasEntry("key", "info"));
        assertThat(controller.getBuildInfoKeys().get(0), is("key"));
        
        // verify event is sent for new controller
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().size(), is(2));
        
        Event controllerEvent = eventCaptor.getAllValues().get(0);
        assertThat(controllerEvent.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_ADDED));
        assertThat((Integer)controllerEvent.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(6431));
    
        Event controllerUpdated = eventCaptor.getAllValues().get(1);
        assertThat(controllerUpdated.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_UPDATED));
    }      
        
    /**
     * Test proper handling of GetSystemModeResponse message.
     * Verify Controller is updated with new system mode.  
     */
    @Test
    public void testEventHelperGetSystemMode()
    {
        //controllerId
        int contNew = 6431;        
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));           
        
        //mock sys info response for new controller 
        mockControllerMessage(contNew, BaseMessageType.ControllerInfo); 
        
        m_Mode = BaseMessages.OperationMode.OPERATIONAL_MODE;
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class); //reset event captor
        mockControllerMessage(contNew,  BaseMessageType.GetOperationModeResponse);
        
        //ensure controller mode was updated
        ControllerModel controller = m_SUT.getController(contNew);
        assertThat(controller.getOperatingMode(), is(OperationMode.OPERATIONAL_MODE));
        
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        //verify controller updated event
        Event controllerUpdated2 = eventCaptor.getValue();
        assertThat(controllerUpdated2.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_UPDATED));
        assertThat(controllerUpdated2.getPropertyNames().length, is(1)); //should only have topic property  
        
        //test with different mode for code coverage; ensure controller mode was updated.
        m_Mode = BaseMessages.OperationMode.TEST_MODE;
        mockControllerMessage(contNew,  BaseMessageType.GetOperationModeResponse);
        
        controller = m_SUT.getController(contNew);
        assertThat(controller.getOperatingMode(), is(OperationMode.TEST_MODE));
    }
    
    /**
     * Test proper handling of GetControllerCapabiltiesResponse message.
     * Verify controller capabilities are set. 
     */
    @Test
    public void testEventHelperGetControllerCaps()
    {
        //controller ID
        int contNew = 6431;        
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));  
        //mock sys info response for new controller
        mockControllerMessage(contNew, BaseMessageType.ControllerInfo);
        
        mockControllerMessage(contNew, BaseMessageType.GetControllerCapabilitiesResponse);
       
        //get controller
        ControllerModel controller = m_SUT.getController(contNew);

        assertThat(controller.getCapabilities().getDescription(), is("test description"));
        assertThat(controller.getCapabilities().isBatteryAmpHourReported(), is(false));
        assertThat(controller.getCapabilities().getCpuSpeed(), is(1.20F));
        assertThat(controller.getCapabilities().isLowPowerModeSupported(), is(true));
        assertThat(controller.getCapabilities().getMaxVoltage(), is(new VoltageVolts().withValue(.10F)));
        assertThat(controller.getCapabilities().getMinVoltage(), is(new VoltageVolts().withValue(.01F)));
        assertThat(controller.getCapabilities().getNominalVoltage(), is(new VoltageVolts().withValue(.08F)));
        assertThat(controller.getCapabilities().getOperatingSystemsSupported(), is(OperatingSystemTypeEnum.WIN_7));
        assertThat(controller.getCapabilities().isBatteryAmpHourReported(), is(false));
        assertThat(controller.getCapabilities().isSetPhysicalLink(), is(false));
        assertThat(controller.getCapabilities().getSystemMemory(), is(4));
        assertThat(controller.getCapabilities().isVoltageReported(), is(false));           
    }
    
    /**
     * Verify if the controller caps native field is not set, capabilities are not available.
     */
    @Test
    public void testEventHelperGetControllerCaps_OneofNotSet()
    {
        //controller ID
        int contNew = 6431;        
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));  
        //mock sys info response for new controller
        mockControllerMessage(contNew, BaseMessageType.ControllerInfo);
        
        // clear out capabilities being sent
        GetControllerCapabilitiesResponseData.Builder builder =
                (GetControllerCapabilitiesResponseData.Builder)createMessage(
                        BaseMessageType.GetControllerCapabilitiesResponse);
        builder.clearControllerCapabilities();
        mockControllerMessage(contNew, BaseMessageType.GetControllerCapabilitiesResponse, builder);
       
        //get controller
        ControllerModel controller = m_SUT.getController(contNew);

        assertThat(controller.getCapabilities(), is(nullValue()));
    }
    
    /**
     * Test event handling.
     * 
     * Verify that controller information is updated.
     */
    @Test
    public void testCleanupHelperCleanupResponse()
    {
        //controllerId
        int contNew = 6431;                    
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));
        
        ControllerModel model = m_SUT.getController(contNew);        
        assertThat(model.isReady(), is(false));
        
        //mock cleanup response for new controller
        mockCleanupMessage(contNew, EventAdminMessageType.CleanupResponse); 
        
        //mock cleanup response for unknown controller
        mockCleanupMessage(9999, EventAdminMessageType.CleanupResponse); 
        
        //model is now ready to send and receive messages
        assertThat(model.isReady(), is(true));
        
        //verify 4 request messages are queued (corresponds to known controller)
        //one system info, one get system mode request, one event registration request, one capabilities request
        verify(m_MessageWrapper, times(4)).queue(eq(contNew), Mockito.any(RemoteEventRegistrationHandler.class));
    }
    
    /**
     * Verify that encryption helper functions properly when new controller is added.
     */
    @Test
    public void testEncryptionEventHelper()
    {
        int contId = 123;
        
        RemoteChannel channel = mock(RemoteChannel.class);
        Map<Integer, Set<RemoteChannel>> remoteAns = new HashMap<>();
        Set<RemoteChannel> setRC = new HashSet<>();
        setRC.add(channel);
        
        remoteAns.put(contId, setRC);
        
        when(m_ChannelLookup.getAllChannels()).thenReturn(remoteAns);
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        assertThat(m_SUT.getController(contId), notNullValue());
        
        
        Event eventEncrypt = mockEncryptionUpdateEvent(contId);
        m_EncryptionEventHelper.handleEvent(eventEncrypt);
        
        assertThat(m_SUT.getController(contId).needsCleanupRequest(), is(false));
        verify(m_MessageFactory).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper).queue(contId, null);
    }
    
    /**
     * Verify if encryption update event is sent and controller is not new a send cleanuprequest 
     * is not sent again
     */
    @Test
    public void testEncryptionEventHelperNotNewController()
    {
        int contId = 123;
        
        RemoteChannel channel = mock(RemoteChannel.class);
        Map<Integer, Set<RemoteChannel>> remoteAns = new HashMap<>();
        Set<RemoteChannel> setRC = new HashSet<>();
        setRC.add(channel);
        
        remoteAns.put(contId, setRC);
        
        when(m_ChannelLookup.getAllChannels()).thenReturn(remoteAns);
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        assertThat(m_SUT.getController(contId), notNullValue());
        
        Map<String, Object> props = new HashMap<>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, contId);
        Event eventEncrypt = mockEncryptionUpdateEvent(contId);
        m_EncryptionEventHelper.handleEvent(eventEncrypt);
        
        assertThat(m_SUT.getController(contId).needsCleanupRequest(), is(false));
        
        verify(m_MessageFactory).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper).queue(contId, null);
        
        m_EncryptionEventHelper.handleEvent(eventEncrypt);
        
        //should still only be once. the second handle event should not cause another cleanup to happen
        verify(m_MessageFactory).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper).queue(contId, null);
    }
    
    /**
     * Verify that an encryption event for an unknown controller does not cause anything to happen.
     */
    @Test
    public void testEncrytpionEventHelperUnknownController()
    {
        Event event = mockEncryptionUpdateEvent(CONT_ID_1);
        m_EncryptionEventHelper.handleEvent(event);
        
        verify(m_MessageFactory, never()).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper, never()).queue(CONT_ID_1, null);
    }
    
    /**
     * Verify when a channel added event is handled and that messages to request controller information are sent since
     * encryption type is unknown.
     */
    @Test
    public void testChannelHelperChannelUpdatedUnknownEncryptType()
    {
        int contId = 123;
        
        RemoteChannel channel = mock(RemoteChannel.class);
        Map<Integer, Set<RemoteChannel>> remoteAns = new HashMap<>();
        Set<RemoteChannel> setRC = new HashSet<>();
        setRC.add(channel);
        
        remoteAns.put(contId, setRC);
        
        when(m_ChannelLookup.getAllChannels()).thenReturn(remoteAns);
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_ADDED));
        assertThat((int)eventCaptor.getValue().getProperty(
                SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(contId));
        
        ControllerModel model = m_SUT.getController(contId);
        assertThat(model, notNullValue());
        assertThat(model.needsCleanupRequest(), is(true));

        verify(m_MessageFactory, never()).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper, never()).queue(contId, null);
    }
    
    @Test
    public void testChannelHelperChannelUpdatedKnownEncryptType()
    {
        int contId = 123;
        
        RemoteChannel channel = mock(RemoteChannel.class);
        Map<Integer, Set<RemoteChannel>> remoteAns = new HashMap<>();
        Set<RemoteChannel> setRC = new HashSet<>();
        setRC.add(channel);
        
        remoteAns.put(contId, setRC);
        
        when(m_ChannelLookup.getAllChannels()).thenReturn(remoteAns);
        when(m_EncryptionTypeManager.getEncryptTypeAsnyc(contId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_ADDED));
        assertThat((int)eventCaptor.getValue().getProperty(
                SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(contId));
        
        ControllerModel model = m_SUT.getController(contId);
        assertThat(model, notNullValue());
        assertThat(model.needsCleanupRequest(), is(false));

        verify(m_MessageFactory).createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        verify(m_MessageWrapper).queue(contId, null);
    }
    
    /**
     * Verify when channel removed event is handled that the recorded controller is removed and the controller 
     * removed event is created.
     */
    @Test
    public void testChannelHelperChannelRemoved()
    {
        //add the channel first so that it can be removed 
        int contId = 123;
        RemoteChannel channel = mock(RemoteChannel.class);
        Map<Integer, Set<RemoteChannel>> remoteAns = new HashMap<>();
        Set<RemoteChannel> setRC = new HashSet<>();
        setRC.add(channel);
        
        remoteAns.put(contId, setRC);

        when(m_ChannelLookup.getAllChannels()).thenReturn(remoteAns);
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        
        assertThat(m_SUT.getController(contId), notNullValue());
        
        when(m_ChannelLookup.getChannels(contId)).thenReturn(new ArrayList<RemoteChannel>());
        
        Event eventRemoved = createBasicChannelRemovedEvent();
        m_ChannelHelper.handleEvent(eventRemoved);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().get(1).getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_REMOVED));
    }
    
    /**
     * Verify topic controller updated when system mode event is captured.
     */
    @Test
    public void testModeEventHandler()
    {
        int contId = 9001;
        
        RemoteChannel channel = addChannelMockHelper(contId);//mock channel lookup
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        
        //mock system info response has been received for newly added channel
        mockControllerMessage(contId, BaseMessageType.ControllerInfo);
        
        ControllerModel model = m_SUT.getController(contId);
        assertThat(model, notNullValue());
        
        /*
         * Mock system mode event - update to test mode
         */        
        mockSystemModeEvent(contId, OperationMode.TEST_MODE);
        
        assertThat(m_SUT.getController(contId).getOperatingMode(), is(OperationMode.TEST_MODE));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        //one event for the addition of the controller, one for controller updated, one for event registration
        assertThat(eventCaptor.getAllValues().size(), is(3));
        
        Event controllerUpdated = eventCaptor.getValue(); //most recent value
        assertThat(controllerUpdated.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_UPDATED));
        assertThat(controllerUpdated.getPropertyNames().length, is(1)); //should only contain topic property
        
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(m_GrowlUtil, times(2)).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                anyString(), stringCaptor.capture());
        
        //assert that growl message displays newly updated mode. 
        assertThat(stringCaptor.getValue(), containsString(m_SUT.getController(contId).getOperatingModeDisplayText()));
        
        /*
         * Mock system mode event - update to operational mode
         */
        eventCaptor = ArgumentCaptor.forClass(Event.class); //reset argument captor
        mockSystemModeEvent(contId, OperationMode.OPERATIONAL_MODE);
        
        assertThat(m_SUT.getController(contId).getOperatingMode(), is(OperationMode.OPERATIONAL_MODE));
        
        stringCaptor = ArgumentCaptor.forClass(String.class); //reset string captor
        verify(m_GrowlUtil, times(3)).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                anyString(), stringCaptor.capture());
        
        //assert that growl message displays newly updated mode
        assertThat(stringCaptor.getValue(), containsString(m_SUT.getController(contId).getOperatingModeDisplayText()));
    }
    
    /**
     * Verify that with no known controllers no event or message is output for 
     * the incoming system mode event.
     */
    @Test
    public void testModeEventHandlerWithNoKnownControllers()
    {
        int contId = 9001;
        mockSystemModeEvent(contId, OperationMode.TEST_MODE);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                anyString(), Mockito.anyString());
    }
    
    /**
     * Verify that a growl message is posted when an illegal controller mode type is received.
     */
    @Test
    public void testModeEventHandlerWithIllegalMode()
    {
        int contId = 9001;
        
        RemoteChannel channel = addChannelMockHelper(contId);//mock channel lookup
        
        Event event = createBasicChannelUpdatedEvent(channel, contId);
        
        m_ChannelHelper.handleEvent(event);
        
        //mock system info response has been received for newly added channel
        mockControllerMessage(contId, BaseMessageType.ControllerInfo);
        
        ControllerModel model = m_SUT.getController(contId);
        assertThat(model, notNullValue());
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, contId);
        props.put(TerraHarvestController.EVENT_PROP_SYSTEM_MODE, "IDONTCARE");
        
        final Event modeEvent = createOsgiEvent(TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED 
                + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        m_ModeEventHandler.handleEvent(modeEvent);
        
        //Verify that the correct error growl message is displayed.
        verify(m_GrowlUtil, times(1)).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Controller Status Update Failed"), 
                eq("The status for controller 0x00002329 could not be updated to IDONTCARE."), 
                Mockito.any(IllegalArgumentException.class));
        
        //Verify that no other controller status updated growl messages are displayed.
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Controller Status Updated"), anyString());
    }

    /**
     * Verify flushing of controller list.
     */
    @Test
    public void testFlushControllerList()
    {
        //controllerId
        int contNew = 6431;
        
        RemoteChannel channel = addChannelMockHelper(contNew);//mock channel lookup
        
        //mock addition of controller
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(channel, contNew));
        //mock sys info response for new controller
        mockControllerMessage(contNew, BaseMessageType.ControllerInfo);

        //verify the controller was added
        assertThat(m_SUT.getController(contNew), is(notNullValue()));
        
        //replay with no channels being returned for the controller id
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(contNew, new HashSet<RemoteChannel>());
        when(m_ChannelLookup.getAllChannels()).thenReturn(channels);
 
        //verify that null is returned because the controller has no channels
        assertThat(m_SUT.getController(contNew), nullValue());
        
        //Verify that a controller removed event was posted for the controller.
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        final Event removedEvent = eventCaptor.getValue();
        assertThat(removedEvent.getTopic(), is(ControllerMgr.TOPIC_CONTROLLER_REMOVED));
    }
    
    /**
     * Verifies that a growl message appears only for controllers that do not have the maximum ID set and that there is 
     * no growl message duplication.
     */
    @Test
    public void testGrowlMessageAddingController()
    {
        //create 2 controllers
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_1));
        m_ChannelHelper.handleEvent(createBasicChannelUpdatedEvent(mock(RemoteChannel.class), CONT_ID_5));
        
        //verify that only 1 growl message appeared
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), anyString(), anyString());
    }
    
    /**
     * Mock channels from the Remote Channel Lookup Service getAllChannels class
     * @param setChanns
     *     list of Channels 
     */
    private void setUpFourChannels(List<RemoteChannel> setChanns)
    {
        //channel sets by ID 
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(CONT_ID_1, new HashSet<RemoteChannel>(setChanns));
        channels.put(CONT_ID_2, new HashSet<RemoteChannel>(setChanns));
        channels.put(CONT_ID_3, new HashSet<RemoteChannel>(setChanns));
        channels.put(CONT_ID_4, new HashSet<RemoteChannel>(setChanns));
        when(m_ChannelLookup.getAllChannels()).thenReturn(channels);
    }
    
    /**
     * Used to mock a message from a controller, will create a controller based on system id. 
     */
    private void mockControllerMessage(int systemId, BaseMessageType messageType)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, createMessage(messageType).build());
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Base.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, messageType.toString());
        //the event
        final Event event = createOsgiEvent(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_Helper.handleEvent(event);
    }
    
    /**
     * Used to mock a message from a controller, will create a controller based on system id. 
     */
    private void mockControllerMessage(int systemId, BaseMessageType messageType, Builder<?> dataMessage)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMessage.build());
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Base.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, messageType.toString());
        //the event
        final Event event = createOsgiEvent(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_Helper.handleEvent(event);
    }
    
    /**
     * Used to mock a SystemMode event regarding a mode change for a particular controller. 
     */
    private void mockSystemModeEvent(int systemId, OperationMode mode)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        props.put(TerraHarvestController.EVENT_PROP_SYSTEM_MODE, mode.value());
        
        final Event event = createOsgiEvent(TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED 
                + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        m_ModeEventHandler.handleEvent(event);
    }
    
    /**
     * Used to mock a message from a controller, will create a controller based on system id. 
     */
    private void mockCleanupMessage(int systemId, EventAdminMessageType messageType)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, createMessage(messageType).build());
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.EventAdmin.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, messageType.toString());
        //the event
        final Event event = createOsgiEvent(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_CleanupEventHelper.handleEvent(event);
    }
    
    /**
     * Helper method for testing all response types being handled by the EventHelper class.
     */
    @SuppressWarnings("rawtypes")
    private Builder createMessage(ProtocolMessageEnum type)
    {        
        if (type.equals(BaseMessageType.GetControllerCapabilitiesResponse))
        {
            //mock caps
            BaseCapabilities baseCaps = BaseCapabilities.newBuilder()
                    .setDescription("someDesc")
                    .setProductName("Controller")
                    .build();
            ControllerCapabilitiesGen.ControllerCapabilities capGen = 
                ControllerCapabilitiesGen.ControllerCapabilities.newBuilder()
                    .setBase(baseCaps)
                    .build();
            
            CapabilitiesFakeObjects fake = new CapabilitiesFakeObjects();
            try
            {
                when(m_Converter.convertToJaxb((Message)any())).thenReturn(fake.genControllerCapabilities());
                
                // construct the message
                return GetControllerCapabilitiesResponseData.newBuilder().setControllerCapabilitiesNative(capGen); 
            }
            catch (ObjectConverterException e)
            {
                fail("Cannot convert object!");
                e.printStackTrace();
            }            
            return null;            
        }
        else if (type.equals(BaseMessageType.GetOperationModeResponse))
        {
            return GetOperationModeReponseData.newBuilder().setMode(m_Mode);
        }
        else
        {
            return ControllerInfoData.newBuilder().
                    setName("Bacon").
                    setVersion("TheBest").
                    addBuildInfo(SimpleTypesMapEntry.newBuilder().
                            setKey("key").
                            setValue(Multitype.newBuilder().
                                    setType(Type.STRING).
                                    setStringValue("info").build()).build());
        }
    }
    
    /**
     * Helper method for mocking the channel lookup functionalities for a particular system.  
     * @param systemId
     *      ID of the system
     * @return 
     *      the mocked channel being used in the channel lookup. Usually needed for event properties.
     */
    private RemoteChannel addChannelMockHelper(int systemId)
    {
        //mock 
        RemoteChannel channel = mock(RemoteChannel.class); 
        when(m_ChannelLookup.getChannel(systemId)).thenReturn(channel);
        
        //mock remote channel lookup behavior
        List<RemoteChannel> setChanns = new ArrayList<RemoteChannel>();
        setChanns.add(channel);
        when(m_ChannelLookup.getChannels(systemId)).thenReturn(setChanns);
        
        //mock that controller has a channel once
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(systemId, new HashSet<RemoteChannel>(setChanns));
        when(m_ChannelLookup.getAllChannels()).thenReturn(channels);
        
        return channel;
    }
    
    /**
     * Mock a basic channel updated event.
     * @param channel
     *      mocked channel that is to be updated
     * @param systemId
     *      ID of the corresponding system
     * @return
     *      the channel updated event
     */
    private Event createBasicChannelUpdatedEvent(RemoteChannel channel, int systemId)
    {
        Map<String, Object> props = new HashMap<String, Object>();
        
        //add channel
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, channel);
        
        return createOsgiEvent(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, props);        
    }
    
    /**
     * Creates a RemoteChannelLookup.TOPIC_CHANNEL_REMOVED event.
     * @return
     *  the channel removed event
     */
    private Event createBasicChannelRemovedEvent()
    {
        return createOsgiEvent(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED, new HashMap<String, Object>());
    }
    
    /**
     * Creates an OSGi Event based on the given topic and properties
     * @param topic
     *  the topic of the event being created
     * @param props
     *  the properties that the event should have
     * @return
     *  the event with the given topic and properties
     */
    private Event createOsgiEvent(String topic, Map<String, Object> props)
    {
        return new Event(topic, props);
    }
    
    /**
     * Mock an encryption type updated event for a controller
     * @param systemId
     *      ID of the controller
     * @return
     *      the encryption type updated event
     */
    private Event mockEncryptionUpdateEvent(int systemId)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        return createOsgiEvent(ControllerEncryptionConstants.TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED, props);
    }
}
