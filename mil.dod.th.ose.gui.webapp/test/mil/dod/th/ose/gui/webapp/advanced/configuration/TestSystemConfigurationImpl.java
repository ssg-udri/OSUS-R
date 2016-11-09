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

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigurationInfoType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemConfigurationMgrImpl.ConfigurationAdminEventHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemConfigurationMgrImpl.ConfigurationEventHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemConfigurationMgrImpl.ConfigurationResponseHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemConfigurationMgrImpl.ControllerEventHandler;
import mil.dod.th.ose.gui.webapp.advanced.configuration.SystemConfigurationMgrImpl.RegisterEventsResponseHandler;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * Test class for the {@link SystemConfigurationMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestSystemConfigurationImpl
{
    private SystemConfigurationMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private BundleContextUtil m_BundleContextUtil;
    private GrowlMessageUtil m_GrowlUtil;
    private ConfigurationEventHandler m_ConfigEventHandler;
    private ControllerEventHandler m_ControllerEventHandler;
    private ConfigurationAdminEventHandler m_ConfigAdminHandler;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_HandlerReg;
    private EventAdmin m_EventAdmin;
    private MessageWrapper m_MessageWrapper;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_MessageFactory = mock(MessageFactory.class);
        m_BundleContextUtil = mock(BundleContextUtil.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_SUT = new SystemConfigurationMgrImpl();
        
        //mock behavior for event listener
        m_SUT.setBundleContextUtil(m_BundleContextUtil);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEventAdmin(m_EventAdmin);
        
        when(m_BundleContextUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createConfigAdminMessage(Mockito.any(ConfigAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        m_SUT.setup();
        
        //Verify and capture the event handlers being registered.
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(3)).registerService(eq(EventHandler.class), eventHandlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        //Set the event handlers.
        m_ConfigEventHandler = (ConfigurationEventHandler)eventHandlerCaptor.getAllValues().get(0);
        m_ControllerEventHandler = (ControllerEventHandler)eventHandlerCaptor.getAllValues().get(1);
        m_ConfigAdminHandler = (ConfigurationAdminEventHandler)eventHandlerCaptor.getAllValues().get(2);
    }
    
    /**
     * Test the cleanup method.
     * Verify that all listeners are unregistered.
     * Verify that any unregister message is sent for all remote events being listened for.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCleanup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        //Gain access to the map that stored event registration IDs so that fake ones can be added.
        Field map = m_SUT.getClass().getDeclaredField("m_EventRegistraionIds");
        map.setAccessible(true);
        Map<Integer, Integer> registrationIds = (Map<Integer, Integer>)map.get(m_SUT);
        
        //Add registered events to the registration map.
        registrationIds.put(25, 1);
        registrationIds.put(50, 30);
        registrationIds.put(7, 125);
        
        //Replay
        m_SUT.cleanup();
        
        //Verify that three handlers are unregistered.
        verify(m_HandlerReg, times(3)).unregister();
        
        ArgumentCaptor<UnregisterEventRequestData> messageCaptor = 
                ArgumentCaptor.forClass(UnregisterEventRequestData.class);
        
        //Verify that three unregister event messages are sent.
        verify(m_MessageFactory, times(3)).createEventAdminMessage(eq(EventAdminMessageType.UnregisterEventRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), eq((ResponseHandler)null));
        verify(m_MessageWrapper).queue(eq(50), eq((ResponseHandler)null));
        verify(m_MessageWrapper).queue(eq(7), eq((ResponseHandler)null));
        
        //Verify the contents of the unregister event messages.
        UnregisterEventRequestData message = messageCaptor.getAllValues().get(0);
        assertThat(message.getId(), is(30));
        message = messageCaptor.getAllValues().get(1);
        assertThat(message.getId(), is(125));
        message = messageCaptor.getAllValues().get(2);
        assertThat(message.getId(), is(1));
    }
    
    /**
     * Test the get configurations method.
     * Verify the calls made to the remote message sender if the controller is not known.
     */
    @Test
    public void testGetConfigurations()
    {
        assertThat(m_SUT.getConfigurationsAsync(25), is(notNullValue()));
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        verify(m_MessageFactory).createConfigAdminMessage(
            eq(ConfigAdminMessageType.GetConfigurationInfoRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(25), Mockito.any(ResponseHandler.class));
        
        verify(m_MessageFactory).createEventAdminMessage(eq(EventAdminMessageType.EventRegistrationRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(25), 
            Mockito.any(RegisterEventsResponseHandler.class));
        
        GetConfigurationInfoRequestData configInfoRequest = 
                (GetConfigurationInfoRequestData)messageCaptor.getAllValues().get(0);
        
        EventRegistrationRequestData registrationRequest = 
                (EventRegistrationRequestData)messageCaptor.getAllValues().get(1);
        
        assertThat(configInfoRequest.hasFilter(), is(false));
        
        assertThat(registrationRequest.getTopic(0), is(ConfigurationEventConstants.TOPIC_ALL_CONFIGURATION_EVENTS));
    }
    
    /**
     * Test getting a configuration by pid.
     * Verify that the correct configuration is returned.
     * Verify that the correct factory configuration is returned.
     * Verify null is returned if the configuration with the specified pid cannot be found.
     */
    @Test
    public void testGetConfigurationByPid() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {
        setupConfigurations();
        
        //Verify that a standard configuration is returned.
        ConfigAdminModel returned = m_SUT.getConfigurationByPidAsync(25, "some PID");
        assertThat(returned.getBundleLocation(), is("another location"));
        assertThat(returned.getPid(), is("some PID"));
        assertThat(returned.getFactoryPid(), is(nullValue()));
        
        //Verify that the correct factory configuration is returned.
        returned = m_SUT.getConfigurationByPidAsync(25, "factory config 1");
        assertThat(returned.getBundleLocation(), is("location"));
        assertThat(returned.getPid(), is("factory config 1"));
        assertThat(returned.getFactoryPid(), is("factory PID"));
        
        //Verify null is returned if the pid does not exist within the list.
        assertThat(m_SUT.getConfigurationByPidAsync(25, "non-existent PID"), is(nullValue()));
    }
    
    /**
     * Test getting a list of all factory configuration for the specified factory.
     * Verify that the list returned contains the appropriate factory configurations.
     * Verify that null is returned if the factory configuration does not exist.
     */
    @Test
    public void testGetFactoryConfigurationsByFactoryPid() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        setupConfigurations();
        
        Map<String, ConfigAdminModel> factoryConfigs = 
                m_SUT.getFactoryConfigurationsByFactoryPidAsync(25, "factory PID");
        
        assertThat(factoryConfigs.size(), is(2));
        ConfigAdminModel config = factoryConfigs.get("factory config 1");
        assertThat(config.getBundleLocation(), is("location"));
        assertThat(config.getPid(), is("factory config 1"));
        assertThat(config.getFactoryPid(), is("factory PID"));
        
        config = factoryConfigs.get("factory config 2");
        assertThat(config.getBundleLocation(), is("location"));
        assertThat(config.getPid(), is("factory config 2"));
        assertThat(config.getFactoryPid(), is("factory PID"));
        
        //Verify that null is returned if no factory configuration with the specified PID exists.
        assertThat(m_SUT.getFactoryConfigurationsByFactoryPidAsync(25, "nada"), is(nullValue()));
    }
    
    /**
     * Test the get properties by pid method.
     * Verify that the list returned contains all properties for the specified configuration.
     */
    @Test
    public void testGetPropertiesByPid() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        setupConfigurations();
        
        List<ConfigAdminPropertyModel> props = m_SUT.getPropertiesByPidAsync(25, "factory config 2");
        
        assertThat(props.size(), is(2));
        
        ConfigAdminPropertyModel property = props.get(0);
        assertThat(property.getKey(), is("key1"));
        assertThat(property.getValue(), is((Object)"value1"));
        
        property = props.get(1);
        assertThat(property.getKey(), is("key2"));
        assertThat(property.getValue(), is((Object)1500));
        
        //Verify that null is returned if no properties for the specified PID can be found.
        assertThat(m_SUT.getPropertiesByPidAsync(25, "nada"), is(nullValue()));
    }
    
    /**
     * Method used to add fake configurations to the system configuration manager.
     */
    @SuppressWarnings("unchecked")
    private void setupConfigurations() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        ConfigAdminModel testConfig = new ConfigAdminModel();
        testConfig.setBundleLocation("another location");
        testConfig.setPid("some PID");
        
        ConfigAdminModel testFactoryConfig1 = new ConfigAdminModel();
        testFactoryConfig1.setBundleLocation("location");
        testFactoryConfig1.setPid("factory config 1");
        testFactoryConfig1.setFactoryPid("factory PID");
        
        ConfigAdminModel testFactoryConfig2 = new ConfigAdminModel();
        testFactoryConfig2.setBundleLocation("location");
        testFactoryConfig2.setPid("factory config 2");
        testFactoryConfig2.setFactoryPid("factory PID");
        
        ConfigAdminModel testFactory = new ConfigAdminModel();
        testFactory.setIsFactory(true);
        testFactory.setPid("factory PID");
        testFactory.setBundleLocation("location");
        testFactory.getFactoryConfigurations().put("factory config 1", testFactoryConfig1);
        testFactory.getFactoryConfigurations().put("factory config 2", testFactoryConfig2);
        
        ConfigAdminPropertyModel props1 = new ConfigAdminPropertyModel();
        props1.setKey("key1");
        props1.setValue("value1");
        props1.setType(String.class);
        
        ConfigAdminPropertyModel props2 = new ConfigAdminPropertyModel();
        props2.setKey("key2");
        props2.setValue(1500);
        props2.setType(Integer.class);
        
        testConfig.getProperties().add(props1);
        testConfig.getProperties().add(props2);
        testFactoryConfig2.getProperties().add(props1);
        testFactoryConfig2.getProperties().add(props2);
        
        Field map = m_SUT.getClass().getDeclaredField("m_ControllerConfigList");
        map.setAccessible(true);
        Map<Integer, Map<String, ConfigAdminModel>> configList = 
                (Map<Integer, Map<String, ConfigAdminModel>>)map.get(m_SUT);
        
        Map<String, ConfigAdminModel> configurations = new HashMap<String, ConfigAdminModel>();
        configurations.put("some PID", testConfig);
        configurations.put("factory PID", testFactory);
        configList.put(25, configurations);
    }
    
    /**
     * Test for the configuration event handler.
     * Verify that the configuration event handler handles all events appropriately. 
     */
    @Test
    public void testConfigurationEventHandler() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        setupConfigurations();
        
        //create configuration event where controller does not exist in config list
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, "factory config 3");
        props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, "factory PID 1");
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 99);
        Event nonExistingConfigEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIGURATION_UPDATED_REMOTE, props);
        
        m_ConfigEventHandler.handleEvent(nonExistingConfigEvent);
        
        ArgumentCaptor<EventRegistrationRequestData> messageNonExistingCaptor = 
                ArgumentCaptor.forClass(EventRegistrationRequestData.class);
        
        verify(m_MessageFactory).createEventAdminMessage(eq(EventAdminMessageType.EventRegistrationRequest), 
                messageNonExistingCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(99), 
                Mockito.any(RegisterEventsResponseHandler.class));
        
        EventRegistrationRequestData data = messageNonExistingCaptor.getValue();
        assertThat(data.getTopicCount(), is(1));
        assertThat(data.getTopic(0), is(ConfigurationEventConstants.TOPIC_ALL_CONFIGURATION_EVENTS));
        
        //create the configuration event.
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, "factory config 2");
        props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, "factory PID");
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        Event configEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIGURATION_UPDATED_REMOTE, props);
        
        //Replay the configuration updated event.
        m_ConfigEventHandler.handleEvent(configEvent);
        
        //Replay the configuration location changed event.
        configEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIGURATION_LOCATION_REMOTE, props);
        m_ConfigEventHandler.handleEvent(configEvent);
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        //Verify that the remote message sender was called two times.
        verify(m_MessageFactory, times(3)).createConfigAdminMessage(
                eq(ConfigAdminMessageType.GetConfigurationInfoRequest), messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(25), Mockito.any(ResponseHandler.class));
        
        GetConfigurationInfoRequestData configInfoRequest = 
                (GetConfigurationInfoRequestData)messageCaptor.getAllValues().get(1);
        assertThat(configInfoRequest.getFilter(), is("(service.pid=factory config 2)"));
        
        configInfoRequest = (GetConfigurationInfoRequestData)messageCaptor.getAllValues().get(1);
        assertThat(configInfoRequest.getFilter(), is("(service.pid=factory config 2)"));
        
        //Create the config deleted event.
        configEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIGURATION_DELETED_REMOTE, props);
        
        assertThat(m_SUT.getConfigurationByPidAsync(25, "factory config 2"), is(notNullValue()));
        //Replay factory configuration deleted event.
        m_ConfigEventHandler.handleEvent(configEvent);
        assertThat(m_SUT.getConfigurationByPidAsync(25, "factory config 2"), is(nullValue()));
        
        //create configuration deleted event.
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, "some PID");
        props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, null);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        configEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIGURATION_DELETED_REMOTE, props);
        
        assertThat(m_SUT.getConfigurationByPidAsync(25, "some PID"), is(notNullValue()));
        //Replay configuration deleted event.
        m_ConfigEventHandler.handleEvent(configEvent);
        assertThat(m_SUT.getConfigurationByPidAsync(25, "some PID"), is(nullValue()));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getAllValues().get(0);
        assertThat(event.getTopic(), is(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED));
        assertThat(event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)25));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"factory config 2"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is((Object)"factory PID"));
        
        event = eventCaptor.getAllValues().get(1);
        assertThat(event.getTopic(), is(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED));
        assertThat(event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is((Object)25));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"some PID"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is(nullValue()));
    }
    
    /**
     * Test the controller event handler.
     * Verify that all information is removed for the specified controller in the controller removed event.
     */
    @Test
    public void testHandleRemoveController() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {   
        setupConfigurations();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        Event event = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
        
        assertThat(m_SUT.getConfigurationsAsync(25).size(), is(2));
        
        m_ControllerEventHandler.handleEvent(event);
        
        assertThat(m_SUT.getConfigurationsAsync(25).size(), is(0));
    }
    
    /**
     * Test the configuration response handler.
     * Verify that the configuration information response is handled correctly.
     * Verify that the configuration updated event is posted with the correct information.
     */
    @Test
    public void testConfigurationResponesHanlder() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException, InvalidProtocolBufferException
    {
        setupConfigurations();
        
        final SimpleTypesMapEntry intProp = SimpleTypesMapEntry.newBuilder().setKey("intProp").setValue(
                Multitype.newBuilder().setInt32Value(5).setType(Type.INT32).build()).build();
        final SimpleTypesMapEntry stringProp = SimpleTypesMapEntry.newBuilder().setKey("stringProp").setValue(
                Multitype.newBuilder().setStringValue("some string").setType(Type.STRING).build()).build();
        final List<SimpleTypesMapEntry> propList = new ArrayList<SimpleTypesMapEntry>();
        propList.add(intProp);
        propList.add(stringProp);
        final ConfigurationInfoType config1 = ConfigurationInfoType.newBuilder().setPid("some.pid1").setBundleLocation(
                "bundle location 1").addAllProperties(propList).build();
        final ConfigurationInfoType config2 = ConfigurationInfoType.newBuilder().setPid("some.pid2").setFactoryPid(
                "some.factory1").setBundleLocation("bundle location 2").addAllProperties(propList).build();
        final ConfigurationInfoType config3 = ConfigurationInfoType.newBuilder().setPid("some.pid3").setFactoryPid(
                "factory PID").setBundleLocation("updated location").addAllProperties(propList).build();
        
        final SimpleTypesMapEntry updatedIntProp = SimpleTypesMapEntry.newBuilder().setKey("key2").setValue(
                Multitype.newBuilder().setInt32Value(5).setType(Type.INT32).build()).build();
        final SimpleTypesMapEntry updatedStringProp = SimpleTypesMapEntry.newBuilder().setKey("key1").setValue(
                Multitype.newBuilder().setStringValue("updated string").setType(Type.STRING).build()).build();
        final List<SimpleTypesMapEntry> updatedPropsList = new ArrayList<SimpleTypesMapEntry>();
        updatedPropsList.add(updatedStringProp);
        updatedPropsList.add(updatedIntProp);
        final ConfigurationInfoType updatedConfig1= ConfigurationInfoType.newBuilder().setPid("factory config 2").
                setFactoryPid("factory PID").setBundleLocation("updated location").addAllProperties(
                        updatedPropsList).build();
        final ConfigurationInfoType updatedConfig2 = ConfigurationInfoType.newBuilder().setPid("some PID").
                setBundleLocation("updated location").addAllProperties(updatedPropsList).build();
        
        List<ConfigurationInfoType> configList = new ArrayList<ConfigurationInfoType>();
        configList.add(config1);
        configList.add(config2);
        configList.add(config3);
        configList.add(updatedConfig1);
        configList.add(updatedConfig2);
        
        GetConfigurationInfoResponseData response = 
                GetConfigurationInfoResponseData.newBuilder().addAllConfigurations(configList).build();
        ConfigAdminNamespace namespace = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.GetConfigurationInfoResponse).setData(response.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, 
                Namespace.ConfigAdmin, 5000, namespace);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        final ConfigurationResponseHandler responseHandler = m_SUT.new ConfigurationResponseHandler();
        responseHandler.handleResponse(thMessage, payload, namespace, response);
        
        ConfigAdminModel config = m_SUT.getConfigurationByPidAsync(25, "some.pid1");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("some.pid1"));
        assertThat(config.getFactoryPid(), is(nullValue()));
        assertThat(config.getBundleLocation(), is("bundle location 1"));
        List<ConfigAdminPropertyModel> props = config.getProperties();
        assertThat(props.size(), is(2));
        assertThat(props.get(0).getKey(), is("intProp"));
        assertThat(props.get(0).getType(), is((Object)Integer.class));
        assertThat(props.get(0).getValue(), is((Object)5));
        assertThat(props.get(1).getKey(), is("stringProp"));
        assertThat(props.get(1).getType(), is((Object)String.class));
        assertThat(props.get(1).getValue(), is((Object)"some string"));
        
        config = m_SUT.getConfigurationByPidAsync(25, "some.pid2");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("some.pid2"));
        assertThat(config.getFactoryPid(), is("some.factory1"));
        assertThat(config.getBundleLocation(), is("bundle location 2"));
        props = config.getProperties();
        assertThat(props.size(), is(2));
        assertThat(props.get(0).getKey(), is("intProp"));
        assertThat(props.get(0).getType(), is((Object)Integer.class));
        assertThat(props.get(0).getValue(), is((Object)5));
        assertThat(props.get(1).getKey(), is("stringProp"));
        assertThat(props.get(1).getType(), is((Object)String.class));
        assertThat(props.get(1).getValue(), is((Object)"some string"));

        config = m_SUT.getConfigurationByPidAsync(25, "some.pid3");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("some.pid3"));
        assertThat(config.getFactoryPid(), is("factory PID"));
        assertThat(config.getBundleLocation(), is("updated location"));
        props = config.getProperties();
        assertThat(props.size(), is(2));
        assertThat(props.get(0).getKey(), is("intProp"));
        assertThat(props.get(0).getType(), is((Object)Integer.class));
        assertThat(props.get(0).getValue(), is((Object)5));
        assertThat(props.get(1).getKey(), is("stringProp"));
        assertThat(props.get(1).getType(), is((Object)String.class));
        assertThat(props.get(1).getValue(), is((Object)"some string"));
        
        config = m_SUT.getConfigurationByPidAsync(25, "some.factory1");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("some.factory1"));
        assertThat(config.getFactoryPid(), is(nullValue()));
        assertThat(config.getBundleLocation(), is("bundle location 2"));
        assertThat(config.getFactoryConfigurations().size(), is(1));
        
        config = m_SUT.getConfigurationByPidAsync(25, "factory config 2");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("factory config 2"));
        assertThat(config.getFactoryPid(), is("factory PID"));
        assertThat(config.getBundleLocation(), is("updated location"));
        props = config.getProperties();
        assertThat(props.size(), is(2));
        assertThat(props.get(0).getKey(), is("key1"));
        assertThat(props.get(0).getType(), is((Object)String.class));
        assertThat(props.get(0).getValue(), is((Object)"updated string"));
        assertThat(props.get(1).getKey(), is("key2"));
        assertThat(props.get(1).getType(), is((Object)Integer.class));
        assertThat(props.get(1).getValue(), is((Object)5));
        
        config = m_SUT.getConfigurationByPidAsync(25, "some PID");
        assertThat(config, is(notNullValue()));
        assertThat(config.getPid(), is("some PID"));
        assertThat(config.getFactoryPid(), is(nullValue()));
        assertThat(config.getBundleLocation(), is("updated location"));
        props = config.getProperties();
        assertThat(props.size(), is(2));
        assertThat(props.get(0).getKey(), is("key1"));
        assertThat(props.get(0).getType(), is((Object)String.class));
        assertThat(props.get(0).getValue(), is((Object)"updated string"));
        assertThat(props.get(1).getKey(), is("key2"));
        assertThat(props.get(1).getType(), is((Object)Integer.class));
        assertThat(props.get(1).getValue(), is((Object)5));
        
        //Create a message with a single configuration to verify event is posted correctly.
        configList = new ArrayList<ConfigurationInfoType>();
        configList.add(config2);
        
        response = 
                GetConfigurationInfoResponseData.newBuilder().addAllConfigurations(configList).build();
        namespace = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.GetConfigurationInfoResponse).setData(response.toByteString()).build();
        thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, 
                Namespace.ConfigAdmin, 5000, namespace);
        payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        responseHandler.handleResponse(thMessage, payload, namespace, response);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        Event configUpdated = eventCaptor.getAllValues().get(0);
        assertThat(configUpdated.getTopic(), is(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED));
        assertThat(configUpdated.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is(nullValue()));
        assertThat(configUpdated.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is(nullValue()));
        configUpdated = eventCaptor.getAllValues().get(1);
        assertThat(configUpdated.getTopic(), is(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED));
        assertThat(configUpdated.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"some.pid2"));
        assertThat(configUpdated.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), 
                is((Object)"some.factory1"));
    }
    
    /**
     * Verify that a growl message is posted when a set property response message is received.
     */
    @Test
    public void testHandleSetPropertyResponse()
    {
        ConfigurationResponseHandler responseHandler = m_SUT.new ConfigurationResponseHandler();
        
        ConfigAdminNamespace namespaceMessage = 
                ConfigAdminNamespace.newBuilder().setType(ConfigAdminMessageType.SetPropertyResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.ConfigAdmin).
                setNamespaceMessage(namespaceMessage.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 1, 
                Namespace.ConfigAdmin, 5000, namespaceMessage);
        
        responseHandler.handleResponse(thMessage, payload, namespaceMessage, null);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), eq("Properties Accepted:"), 
                eq("Controller 0x00000019 has accepted the list of updated properties."));
    }
    
    /**
     * Test the register event handler.
     * Verify that the event registration ID is added to the list of registration event IDs.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterEventHandler() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException, InvalidProtocolBufferException
    {
        EventRegistrationResponseData regResponse = EventRegistrationResponseData.newBuilder().setId(5).build();
        EventAdminNamespace namepsace = EventAdminNamespace.newBuilder().setData(regResponse.toByteString()).setType(
                EventAdminMessageType.EventRegistrationResponse).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, 
                Namespace.EventAdmin, 5000, namepsace);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        RegisterEventsResponseHandler handler = m_SUT.new RegisterEventsResponseHandler();
        
        handler.handleResponse(thMessage, payload, null, regResponse);
        
        Field field = m_SUT.getClass().getDeclaredField("m_EventRegistraionIds");
        field.setAccessible(true);
        
        Map<Integer, Integer> regIdMap = (Map<Integer, Integer>)field.get(m_SUT);
        
        assertThat(regIdMap.containsKey(25), is(true));
        assertThat(regIdMap.get(25), is(5));
    }
    
    /**
     * Test the set configuration value method.
     * Verify that the remote message sender is called with the appropriate values.
     */
    @Test
    public void setConfigurationValue()
    {
        ModifiablePropertyModel propModel = mock(ModifiablePropertyModel.class);
        List<ModifiablePropertyModel> listProps = new ArrayList<ModifiablePropertyModel>();
        listProps.add(propModel);
        
        when(propModel.getKey()).thenReturn("test");
        when(propModel.getValue()).thenReturn("some value");
        m_SUT.setConfigurationValueAsync(25, "test-pid", listProps);
        
        ArgumentCaptor<SetPropertyRequestData> messageCaptor = ArgumentCaptor.forClass(SetPropertyRequestData.class);
        
        verify(m_MessageFactory).createConfigAdminMessage(eq(ConfigAdminMessageType.SetPropertyRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), Mockito.any(
                SystemConfigurationMgrImpl.ConfigurationResponseHandler.class));
        
        SetPropertyRequestData message = messageCaptor.getValue();
        SimpleTypesMapEntry prop = message.getProperties(0);
        assertThat(message.getPid(), is("test-pid"));
        assertThat(prop.getKey(), is("test"));
        assertThat(prop.getValue().getStringValue(), is("some value"));
    }
    
    /**
     * Verify that multiple properties can be updated at once.
     */
    @Test
    public void setMultipleConfigurationValues()
    {
        ModifiablePropertyModel propModel1 = mock(ModifiablePropertyModel.class);
        ModifiablePropertyModel propModel2 = mock(ModifiablePropertyModel.class);
        ModifiablePropertyModel propModel3 = mock(ModifiablePropertyModel.class);
        List<ModifiablePropertyModel> listProps = new ArrayList<ModifiablePropertyModel>();
        listProps.add(propModel1);
        listProps.add(propModel2);
        listProps.add(propModel3);
        
        when(propModel1.getKey()).thenReturn("test1");
        when(propModel1.getValue()).thenReturn("value1");
        when(propModel2.getKey()).thenReturn("test2");
        when(propModel2.getValue()).thenReturn("value2");
        when(propModel3.getKey()).thenReturn("test3");
        when(propModel3.getValue()).thenReturn("value3");
        
        m_SUT.setConfigurationValueAsync(25, "test-pid", listProps);
        
        ArgumentCaptor<SetPropertyRequestData> messageCaptor = ArgumentCaptor.forClass(SetPropertyRequestData.class);
        
        verify(m_MessageFactory).createConfigAdminMessage(eq(ConfigAdminMessageType.SetPropertyRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), Mockito.any(
                SystemConfigurationMgrImpl.ConfigurationResponseHandler.class));
        
        SetPropertyRequestData message = messageCaptor.getValue();
        List<SimpleTypesMapEntry> properties = message.getPropertiesList();
        assertThat(properties.size(), is(3));
        SimpleTypesMapEntry prop = properties.get(0);
        assertThat(prop.getKey(), is("test1"));
        assertThat(prop.getValue().getStringValue(), is((Object)"value1"));
        prop = properties.get(1);
        assertThat(prop.getKey(), is("test2"));
        assertThat(prop.getValue().getStringValue(), is((Object)"value2"));
        prop = properties.get(2);
        assertThat(prop.getKey(), is("test3"));
        assertThat(prop.getValue().getStringValue(), is((Object)"value3"));
    }
    
    /**
     * Test the create factory configuration method.
     * Verify that the remote message sender is called with the appropriate values.
     */
    @Test
    public void testCreateFactoryConfiguration()
    {
        m_SUT.createFactoryConfigurationAsync(25, "factory pid");
        
        ArgumentCaptor<CreateFactoryConfigurationRequestData> messageCaptor = 
                ArgumentCaptor.forClass(CreateFactoryConfigurationRequestData.class);
        
        verify(m_MessageFactory).createConfigAdminMessage(
                eq(ConfigAdminMessageType.CreateFactoryConfigurationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), eq((ResponseHandler)null));
        
        CreateFactoryConfigurationRequestData message = messageCaptor.getValue();
        assertThat(message.getFactoryPid(), is("factory pid"));
    }
    
    /**
     * Test the remove configuration method.
     * Verify that the remote message sender is called with the appropriate values.
     */
    @Test
    public void testRemoveConfiguration()
    {
        m_SUT.removeConfigurationAsync(25, "test pid");
        
        ArgumentCaptor<DeleteConfigurationRequestData> messageCaptor = 
                ArgumentCaptor.forClass(DeleteConfigurationRequestData.class);
        
        verify(m_MessageFactory).createConfigAdminMessage(eq(ConfigAdminMessageType.DeleteConfigurationRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), eq((ResponseHandler)null));
        
        DeleteConfigurationRequestData message = messageCaptor.getValue();
        assertThat(message.getPid(), is("test pid"));
    }
    
    /**
     * Test the configuration admin event handlers handle event method.
     * Verify that the handler handles creating factory configurations and removing configuration appropriately.
     */
    @Test
    public void testConfigurationAdminEventHandler() throws SecurityException, IllegalArgumentException, 
        NoSuchFieldException, IllegalAccessException
    {
        setupConfigurations();
        
        CreateFactoryConfigurationResponseData responseCreate = 
                CreateFactoryConfigurationResponseData.newBuilder().setPid("some-pid").build();
        ConfigAdminNamespace namepsace = 
                ConfigAdminNamespace.newBuilder().setType(ConfigAdminMessageType.CreateFactoryConfigurationResponse).
                setData(responseCreate.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(25, 0, 
                Namespace.EventAdmin, 500, namepsace);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, thMessage);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, responseCreate);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                ConfigAdminMessageType.CreateFactoryConfigurationResponse.toString());
        Event event = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        //Replay factory config created message received.
        m_ConfigAdminHandler.handleEvent(event);
        
        //Argument captor used to capture configuration info request.
        ArgumentCaptor<GetConfigurationInfoRequestData> infoRequestCaptor = 
                ArgumentCaptor.forClass(GetConfigurationInfoRequestData.class);
        
        //Verify the remote message sender was called.
        verify(m_MessageFactory).createConfigAdminMessage(eq(ConfigAdminMessageType.GetConfigurationInfoRequest), 
                infoRequestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(25), Mockito.any(ResponseHandler.class));
        
        //Verify the filter in the configuration info request message.
        assertThat(infoRequestCaptor.getValue().getFilter(), is("(service.pid=some-pid)"));
    }
}
