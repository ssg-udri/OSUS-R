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
package mil.dod.th.ose.remote.osgi;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminErrorCode;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespaceErrorData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Test that the ConfigAdminMessage service correctly handles messages, and that expected responses are sent.
 * @author callen
 *
 */
public class TestConfigAdminMessageService 
{
    private ConfigAdminMessageService m_SUT;
    private MessageFactory m_MessageFactory;
    private EventAdmin m_EventAdmin;
    private ConfigurationAdmin m_ConfigAdmin;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new ConfigAdminMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_ConfigAdmin = mock(ConfigurationAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        
        m_SUT.setConfigurationAdmin(m_ConfigAdmin);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        when(m_MessageFactory.createConfigAdminResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ConfigAdminMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
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
     * Verify the namespace is ConfigAdmin.
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.ConfigAdmin));
    }
    
    /**
     * Verify generic handling of message, and that events are correctly posted in response to the received message.
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        // construct a single config admin namespace message to verify sent to config admin message service
        ConfigAdminNamespace namespaceMessage = ConfigAdminNamespace.newBuilder()
            .setType(ConfigAdminMessageType.SetPropertyResponse)
            .build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.ConfigAdmin).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, 
            Namespace.ConfigAdmin, 100, namespaceMessage);
        
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
                is(namespaceMessage.getType().toString()));
        assertThat((ConfigAdminNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Test the handling of the request for property keys messages.
     * 
     * Verify that keys are retrieved and placed into response message.
     * 
     * Verify that if there are no keys found that an empty list is in the response.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testGetPropertyKeys() throws IOException
    {
        //mocking         
        Configuration config = mock(Configuration.class);
        when(m_ConfigAdmin.getConfiguration("core-pid", null)).thenReturn(config);
        
        @SuppressWarnings("rawtypes")
        //mock dictionary representing the configuration
        Dictionary dict = new Hashtable();
        dict.put("BundleA", "NARWHAL");
        dict.put("BundleD", "Beluga");
        when(config.getProperties()).thenReturn(dict);
        
        //construct the request message
        GetPropertyKeysRequestData request = GetPropertyKeysRequestData.newBuilder().setPid("core-pid").build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyKeysRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyKeysRequest, request);
            
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat(
                (GetPropertyKeysRequestData)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        // verify the message is passed correctly
        ArgumentCaptor<GetPropertyKeysResponseData> captor = ArgumentCaptor.forClass(GetPropertyKeysResponseData.class);
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetPropertyKeysResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //verify that both keys in the mock configuration are contained in the response message
        GetPropertyKeysResponseData response = captor.getValue();
        assertThat(response.getKeyCount(), is(2));
        assertThat(response.getKeyList(), hasItem("BundleA"));
        assertThat(response.getKeyList(), hasItem("BundleD"));
        assertThat(response.getPid(), is("core-pid"));
        
        //mock dictionary representing the configuration
        dict = null;
        when(config.getProperties()).thenReturn(dict);
        
        // handle the request
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory, times(2)).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetPropertyKeysResponse), captor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        //verify message contains the empty key list
        response = captor.getValue();
        assertThat(response.getKeyCount(), is(0));
    }
    
    /**
     * Test the handling of the request for property keys messages when an IOException is thrown.
     * 
     * Verify error message sent.
     */
    @Test
    public final void testGetPropertyKeysException() throws IOException
    {
        //mocking
        when(m_ConfigAdmin.getConfiguration("core-pid", null)).thenThrow(new IOException());
        
        //construct the request message
        GetPropertyKeysRequestData request = GetPropertyKeysRequestData.newBuilder().setPid("core-pid").build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyKeysRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyKeysRequest, request);
            
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        //verify error message sent
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.ConfigAdminNamespaceError), 
                Mockito.any(ConfigAdminNamespaceErrorData.class));
        verify(m_ResponseWrapper).queue(channel);
        verify(m_MessageFactory, never()).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.GetPropertyKeysResponse), Mockito.any(Message.class));
    }

    /**
     * Test the get property message handling.
     * 
     * Verify that a property is returned from the config admin service and returned in the appropriate config admin
     * type terra harvest message.
     * 
     * Verify that if no property is found with the key or pid given, that the response message contains an empty string
     * as the property value.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testGetProperty() throws IOException
    {
        //mocking         
        Configuration config = mock(Configuration.class);
        when(m_ConfigAdmin.getConfiguration("core-pid", null)).thenReturn(config);
        
        @SuppressWarnings("rawtypes")
        //mock dictionary of properties for the configuration
        Dictionary dict = new Hashtable();
        dict.put("BundleA", "test");
        dict.put("BundleD", "test two");
        when(config.getProperties()).thenReturn(dict);
        
        //construct the request
        GetPropertyRequestData request = GetPropertyRequestData.newBuilder().setPid("core-pid").setKey("BundleA").
            build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyRequest, request);
            
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((GetPropertyRequestData)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture the message
        ArgumentCaptor<GetPropertyResponseData> captor = ArgumentCaptor.forClass(GetPropertyResponseData.class);
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.GetPropertyResponse), 
                captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //parse the response message
        GetPropertyResponseData response = captor.getValue();
        Multitype value = response.getValue();
        
        // verify
        assertThat(value.getType(), is(Type.STRING));
        assertThat(value.getStringValue(), is("test"));
        assertThat(response.getKey(), is("BundleA"));
        
        //test an invalid key
        request = GetPropertyRequestData.newBuilder().setPid("core-pid").setKey("OnANeedToKnowBasis").
            build();
        when(config.getProperties()).thenReturn(dict);
        payload = createPayload(ConfigAdminMessageType.GetPropertyRequest, request);
        message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyRequest, request);
            
        // handle the request, should log a message "The property was not found for the PID:{pid value} from 
        //system {system ID}"
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
            String.format("The key [%s] was not found for configuration with pid %s", "OnANeedToKnowBasis", 
                "core-pid"));
    }
    
    /**
     * Test the handling of the request for a property message when an IOException is thrown.
     * 
     * Verify error message sent.
     */
    @Test
    public final void testGetPropertyException() throws IOException
    {
        //mocking
        when(m_ConfigAdmin.getConfiguration("core-pid", null)).thenThrow(new IOException());
        
        //construct the request
        GetPropertyRequestData request = GetPropertyRequestData.newBuilder().setPid("core-pid").setKey("BundleA").
            build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyRequest, request);
            
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        //verify error message sent
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.ConfigAdminNamespaceError), 
                Mockito.any(ConfigAdminNamespaceErrorData.class));
        verify(m_ResponseWrapper).queue(channel);
        verify(m_MessageFactory, never()).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.GetPropertyResponse), Mockito.any(Message.class));
    }
    
    /**
     * Test the handling of the set property message.
     * 
     * Verify that updating specific properties is registered with the {@link ConfigurationAdmin} service.
     * 
     * Verify that when properties are successfully updated that a response message is sent.
     * 
     * Verify that if the config admin throws an exception that a response is not sent.
     */
    @Test
    public final void testSetProperty() throws IOException
    {
        Configuration config = mock(Configuration.class);
        when(m_ConfigAdmin.getConfiguration("core", null)).thenReturn(config);
        
        // construct first request
        Multitype value1 = Multitype.newBuilder().setType(Type.STRING).setStringValue("some value").build();
        SimpleTypesMapEntry prop1 = SimpleTypesMapEntry.newBuilder().setKey("prop1").setValue(value1).build();
        Multitype value2 = Multitype.newBuilder().setType(Type.STRING).setStringValue("bob").build();
        SimpleTypesMapEntry prop2 = SimpleTypesMapEntry.newBuilder().setKey("prop2").setValue(value2).build();
        Multitype value3 = Multitype.newBuilder().setType(Type.STRING).setStringValue("test").build();
        SimpleTypesMapEntry prop3 = SimpleTypesMapEntry.newBuilder().setKey("prop3").setValue(value3).build();
        SetPropertyRequestData request = 
                SetPropertyRequestData.newBuilder().setPid("core").addProperties(prop1).addProperties(prop2).
                addProperties(prop3).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.SetPropertyRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.SetPropertyRequest, request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((SetPropertyRequestData)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture the message
        verify(m_MessageFactory).createConfigAdminResponseMessage(message, ConfigAdminMessageType.SetPropertyResponse, 
                null);
        verify(m_ResponseWrapper).queue(channel);
        
        // verify
        Dictionary<String, Object> expectedDict = new Hashtable<String, Object>();
        expectedDict.put("prop1", "some value");
        expectedDict.put("prop2", "bob");
        expectedDict.put("prop3", "test");
        verify(config, times(1)).update(expectedDict);
        
        // re-mock to actually return a dictionary
        Dictionary<String, Object> actualDict = new Hashtable<String, Object>();
        actualDict.put("prop1", "some value");
        actualDict.put("prop2", "bob");
        actualDict.put("prop3", "test");
        when(config.getProperties()).thenReturn(actualDict);
        
        expectedDict.put("test", "value");
        
        //reconstruct a request with update dictionary
        value1 = Multitype.newBuilder().setType(Type.STRING).setStringValue("value").build();
        prop1 = SimpleTypesMapEntry.newBuilder().setKey("test").setValue(value1).build();
        request = SetPropertyRequestData.newBuilder().setPid("core").addProperties(prop1).build();
        payload = createPayload(ConfigAdminMessageType.SetPropertyRequest, request);
        message = createNamespaceMessage(ConfigAdminMessageType.SetPropertyRequest, request);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        //verify
        verify(config).update(expectedDict);
        verify(m_MessageFactory).createConfigAdminResponseMessage(message, ConfigAdminMessageType.SetPropertyResponse, 
                null);
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        //update dictionary
        expectedDict.put("prop2", 7);
        
        //reconstruct request
        value1 = SharedMessageUtils.convertObjectToMultitype(7);
        prop1 = SimpleTypesMapEntry.newBuilder().setKey("prop2").setValue(value1).build();
        request = SetPropertyRequestData.newBuilder().setPid("core").addProperties(prop1).build();
        payload = createPayload(ConfigAdminMessageType.SetPropertyRequest, request);
        message = createNamespaceMessage(ConfigAdminMessageType.SetPropertyRequest, request);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        //verify
        verify(config, times(2)).update(expectedDict);
        verify(m_MessageFactory).createConfigAdminResponseMessage(message, 
            ConfigAdminMessageType.SetPropertyResponse, null);
      //reused channel
        verify(m_ResponseWrapper, times(3)).queue(channel);
        
        //remock behavior, have the config admin throw IOException 
        when(m_ConfigAdmin.getConfiguration("core", null)).thenThrow(new IOException());
        
        m_SUT.handleMessage(message, payload, channel);

        //check that there was no further interactions with the message sender given the exact same message request
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.ConfigAdminNamespaceError), 
                Mockito.any(ConfigAdminNamespaceErrorData.class));
        //reused channel
        verify(m_ResponseWrapper, times(4)).queue(channel);
    }
    
    /**
     * Test the get configuration info message handling.
     */
    @Test
    public void testGetConfiguration() throws IOException, InvalidSyntaxException
    {
        //mock configurations
        Configuration configA = mock(Configuration.class);
        when(configA.getBundleLocation()).thenReturn("China");
        when(configA.getFactoryPid()).thenReturn("Factory");
        when(configA.getPid()).thenReturn("PIDS");
        
        Configuration configB = mock(Configuration.class);
        when(configB.getBundleLocation()).thenReturn("Norway");
        when(configB.getFactoryPid()).thenReturn("Factory BOB");
        when(configB.getPid()).thenReturn("PIDS R US");
        
        Configuration configC = mock(Configuration.class);
        when(configC.getBundleLocation()).thenReturn("Africa");
        when(configC.getFactoryPid()).thenReturn("Factory SUE");
        when(configC.getPid()).thenReturn("PIDS R U");
        
        Configuration configD = mock(Configuration.class);
        when(configD.getBundleLocation()).thenReturn("The Stork");
        when(configD.getFactoryPid()).thenReturn("Factory TOM");
        when(configD.getPid()).thenReturn("PIDS R ME");
        
        Configuration configE= mock(Configuration.class);
        when(configE.getBundleLocation()).thenReturn("Sun");
        when(configE.getFactoryPid()).thenReturn(null);
        when(configE.getPid()).thenReturn("SOUP");
        
        //mock behavior
        Configuration[] configs = {configA, configB, configC, configD, configE};
        when(m_ConfigAdmin.listConfigurations(null)).thenReturn(configs);
        
        //construct request
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
            setIncludeProperties(false).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat(
                (GetConfigurationInfoRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture the message
        ArgumentCaptor<GetConfigurationInfoResponseData> captor = 
                ArgumentCaptor.forClass(GetConfigurationInfoResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetConfigurationInfoResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //parse response message
        GetConfigurationInfoResponseData response = captor.getValue();
        //verify that five configurations are in the configuration list
        assertThat(response.getConfigurationsCount(), is(5));
        assertThat(response.getConfigurations(0), is(notNullValue()));
        //verify the contents of the configurations returned
        assertThat(response.getConfigurations(0).getBundleLocation(), is("China"));
        assertThat(response.getConfigurations(0).getFactoryPid(), is("Factory"));
        assertThat(response.getConfigurations(0).getPid(), is("PIDS"));
        
        assertThat(response.getConfigurations(1).getBundleLocation(), is("Norway"));
        assertThat(response.getConfigurations(1).getFactoryPid(), is("Factory BOB"));
        assertThat(response.getConfigurations(1).getPid(), is("PIDS R US"));
        //check the configuration info returned from the configuration without a factory pid
        assertThat(response.getConfigurations(4).getBundleLocation(), is("Sun"));
        assertThat(response.getConfigurations(4).hasFactoryPid(), is(false));
        assertThat(response.getConfigurations(4).getPid(), is("SOUP"));        
        
        //mock that there is not anything returned from filter
        when(m_ConfigAdmin.listConfigurations("bundleC")).thenReturn(null);

        //create new message
        request = GetConfigurationInfoRequestData.newBuilder().
                setFilter("bundleC").
                setIncludeProperties(false).
                build();
        payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoRequest, request);    
        message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, request);

        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetConfigurationInfoResponse), captor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        //parse response message
        response = captor.getValue();
        //verify that the configuration count is 0
        assertThat(response.getConfigurationsCount(), is(0));
    }
    
    /**
     * Test the get configuration info message including properties being returned.
     */
    @Test
    public void testGetConfigurationsWithProperties() throws IOException, InvalidSyntaxException
    {
        //mock configurations
        Configuration configA = mock(Configuration.class);
        when(configA.getBundleLocation()).thenReturn("China");
        when(configA.getFactoryPid()).thenReturn("Factory");
        when(configA.getPid()).thenReturn("PIDS");
        Hashtable<String, Object> props1 = new Hashtable<String, Object>();
        props1.put("prop1", "some value 1");
        props1.put("prop2", "some value 2");
        when(configA.getProperties()).thenReturn(props1);
        
        Configuration configB = mock(Configuration.class);
        when(configB.getBundleLocation()).thenReturn("Norway");
        when(configB.getFactoryPid()).thenReturn("Factory BOB");
        when(configB.getPid()).thenReturn("PIDS R US");
        Hashtable<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("some.prop", "blah");
        props2.put("test", "value 2");
        when(configB.getProperties()).thenReturn(props2);
        
        //mock behavior
        Configuration[] configs = {configA, configB};
        when(m_ConfigAdmin.listConfigurations(null)).thenReturn(configs);
        
        //construct request
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
            setIncludeProperties(true).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        //capture the message
        ArgumentCaptor<GetConfigurationInfoResponseData> captor = 
                ArgumentCaptor.forClass(GetConfigurationInfoResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetConfigurationInfoResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //parse response message
        GetConfigurationInfoResponseData response = captor.getValue();
        //verify that five configurations are in the configuration list
        assertThat(response.getConfigurationsCount(), is(2));
        //verify the contents of the configurations returned
        assertThat(response.getConfigurations(0).getBundleLocation(), is("China"));
        assertThat(response.getConfigurations(0).getFactoryPid(), is("Factory"));
        assertThat(response.getConfigurations(0).getPid(), is("PIDS"));
        List<SimpleTypesMapEntry> properties = response.getConfigurations(0).getPropertiesList();
        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).getKey(), is("prop2"));
        assertThat(properties.get(0).getValue().getStringValue(), is("some value 2"));
        assertThat(properties.get(1).getKey(), is("prop1"));
        assertThat(properties.get(1).getValue().getStringValue(), is("some value 1"));
        
        assertThat(response.getConfigurations(1).getBundleLocation(), is("Norway"));
        assertThat(response.getConfigurations(1).getFactoryPid(), is("Factory BOB"));
        assertThat(response.getConfigurations(1).getPid(), is("PIDS R US"));
        properties = response.getConfigurations(1).getPropertiesList();
        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).getKey(), is("some.prop"));
        assertThat(properties.get(0).getValue().getStringValue(), is("blah"));
        assertThat(properties.get(1).getKey(), is("test"));
        assertThat(properties.get(1).getValue().getStringValue(), is("value 2"));
    }
    
    /**
     * Test the getting configuration information for a configuration which does not have a location set. 
     */
    @Test
    public void testGetConfigurationNoLocation() throws IOException, InvalidSyntaxException
    {
        //mock configurations
        Configuration configA = mock(Configuration.class);
        when(configA.getBundleLocation()).thenReturn(null);
        when(configA.getFactoryPid()).thenReturn("Factory");
        when(configA.getPid()).thenReturn("PIDS");
        
        //mock behavior
        Configuration[] configs = {configA};
        when(m_ConfigAdmin.listConfigurations(null)).thenReturn(configs);
        
        //construct request
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
            setIncludeProperties(false).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat(
                (GetConfigurationInfoRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture the message
        ArgumentCaptor<GetConfigurationInfoResponseData> captor = 
                ArgumentCaptor.forClass(GetConfigurationInfoResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.GetConfigurationInfoResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //parse response message
        GetConfigurationInfoResponseData response = captor.getValue();
        //verify that five configurations are in the configuration list
        assertThat(response.getConfigurationsCount(), is(1));
        assertThat(response.getConfigurations(0), is(notNullValue()));
        //verify the contents of the configurations returned
        assertThat(response.getConfigurations(0).hasBundleLocation(), is(false));
        assertThat(response.getConfigurations(0).getFactoryPid(), is("Factory"));
        assertThat(response.getConfigurations(0).getPid(), is("PIDS"));
    }

    /**
     * Test get configuration request with invalid syntax in the filter.
     * Verify that error response is sent.
     */
    @Test
    public void testGetConfigurationExceptionHandling() throws IOException, InvalidSyntaxException
    {
        when(m_ConfigAdmin.listConfigurations(Mockito.anyString())).thenThrow(
            new InvalidSyntaxException("asdasdfsda", "asdfsdaf"));
        
        //construct request
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
            setIncludeProperties(false).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        //verify error message sent
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                String.format("The syntax of the filter %s is not correct %s", null, "asdasdfsda: asdfsdaf"));
        verify(m_ResponseWrapper).queue(channel);
        verify(m_MessageFactory, never()).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.GetConfigurationInfoResponse), Mockito.any(Message.class));
    }
    
    /**
     * Verify get property keys response message when handled will set the data event property.
     */
    @Test
    public final void testGetPropertyKeysResponse() throws IOException
    {
        // construct a GetPropertyKeysResponse message
        Message responseData = GetPropertyKeysResponseData.newBuilder().addKey("1-key").
            setPid("core-pid").build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyKeysResponse, responseData);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyKeysResponse,
                responseData);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(responseData));
    }
    
    /**
     * Verify get property response message when handled will set the data event property.
     */
    @Test
    public final void testGetPropertyResponse() throws IOException
    {
        // construct a GetPropertyKeysResponse message
        Message responseData = GetPropertyResponseData.newBuilder().setValue(
                Multitype.newBuilder().setType(Type.BOOL)).
                setPid("core-pid").setKey("scooter").build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetPropertyResponse, responseData);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetPropertyResponse, responseData);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(responseData));
    }
    
    /**
     * Verify get config info response message when handled will set the data event property.
     */
    @Test
    public final void testGetConfigInfoResponse() throws IOException
    {
        // construct a GetPropertyKeysResponse message
        Message responseData = GetConfigurationInfoResponseData.getDefaultInstance();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.GetConfigurationInfoResponse, responseData);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.GetConfigurationInfoResponse, 
                responseData);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(responseData));
    }

    /**
     * Test the create factory configuration request handling.
     * Verify that the config admin is request to create factory configuration.
     * Verify response is sent.
     * Verify error message sent if an IOException occurs.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testCreateFactoryConfiguration() throws IOException
    {
        //mock configuration
        Configuration configE = mock(Configuration.class);
        when(configE.getPid()).thenReturn("new pid");
        
        //mock behavior
        when(m_ConfigAdmin.createFactoryConfiguration("PID", null)).thenReturn(configE);
        
        //construct request
        Dictionary<String, Object> propDictionary = new Hashtable<String, Object>();
        propDictionary.put("testKey1", "test1");
        propDictionary.put("testKey2", "test2");
        
        List<SimpleTypesMapEntry> listTest = SharedMessageUtils.convertDictionarytoMap(propDictionary);
        
        CreateFactoryConfigurationRequestData request = CreateFactoryConfigurationRequestData.newBuilder()
            .setFactoryPid("PID")
            .addAllFactoryProperty(listTest).build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.CreateFactoryConfigurationRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.CreateFactoryConfigurationRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());

        Event event = eventCaptor.getValue();
        assertThat(
                (CreateFactoryConfigurationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify the configuration is updated with the specified properties
        ArgumentCaptor<Dictionary> dictionCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(configE).update(dictionCaptor.capture());
        assertThat(dictionCaptor.getValue().get("testKey1").toString(), is("test1"));
        assertThat(dictionCaptor.getValue().get("testKey2").toString(), is("test2"));
        
        //capture the message
        ArgumentCaptor<CreateFactoryConfigurationResponseData> responseCaptor = ArgumentCaptor.
            forClass(CreateFactoryConfigurationResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.CreateFactoryConfigurationResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //check message
        CreateFactoryConfigurationResponseData response = responseCaptor.getValue();
        assertThat(response.getPid(), is("new pid"));
        
        //mock that an IOException is thrown
        when(m_ConfigAdmin.createFactoryConfiguration("PID", null)).thenThrow(new IOException());

        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.ConfigAdminNamespaceError), Mockito.any(ConfigAdminNamespaceErrorData.class));
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        //verify that no other interactions with the message sender took place
        verify(m_MessageFactory, times(1)).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.CreateFactoryConfigurationResponse), Mockito.any(Message.class));
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Test the create factory configuration request handling.
     * Verify error message sent if no factory property is created
     */
    @Test
    public void testMissingPropertyErrorMessage() throws IOException
    {
        //construct request
        CreateFactoryConfigurationRequestData request = CreateFactoryConfigurationRequestData.newBuilder().
            setFactoryPid("new pid").build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.CreateFactoryConfigurationRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.CreateFactoryConfigurationRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);

        // verify correct error message is captured
        ArgumentCaptor<ConfigAdminNamespaceErrorData> eventCaptor = 
                ArgumentCaptor.forClass(ConfigAdminNamespaceErrorData.class);
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.ConfigAdminNamespaceError), eventCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        assertEquals(eventCaptor.getValue().getError(), ConfigAdminErrorCode.MissingPropertyError);
        assertEquals(eventCaptor.getValue().getErrorDescription(), 
                "Missing properties, unable to create a factory configuration");
    }

    /**
     * Test the remove configuration request handling.
     * Verify that the config admin is requested to remove the configuration.
     * Verify response is sent.
     * Verify error message sent if an IOException occurs.
     */
    @Test
    public void testRemoveConfiguration() throws IOException
    {
        //mock configuration
        Configuration configE = mock(Configuration.class);
        
        //mock behavior
        when(m_ConfigAdmin.getConfiguration("PID", null)).thenReturn(configE);
        
        //construct request
        DeleteConfigurationRequestData request = DeleteConfigurationRequestData.newBuilder().
            setPid("PID").
            build();
        TerraHarvestPayload payload = createPayload(ConfigAdminMessageType.DeleteConfigurationRequest, request);
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.DeleteConfigurationRequest, 
            request);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat(
                (DeleteConfigurationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //capture the message
        ArgumentCaptor<DeleteConfigurationResponseData> responseData = ArgumentCaptor.
            forClass(DeleteConfigurationResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
                eq(ConfigAdminMessageType.DeleteConfigurationResponse), responseData.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //check message for pid
        DeleteConfigurationResponseData response = responseData.getValue();
        assertThat(response.getPid(), is("PID"));
        
        //mock that an IOException is thrown
        when(m_ConfigAdmin.getConfiguration("PID", null)).thenThrow(new IOException());

        //handle request
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.ConfigAdminNamespaceError), Mockito.any(ConfigAdminNamespaceErrorData.class));
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        //verify that no other interactions with the message sender took place
        verify(m_MessageFactory, times(1)).createConfigAdminResponseMessage(eq(message), 
            eq(ConfigAdminMessageType.DeleteConfigurationResponse), 
                Mockito.any(DeleteConfigurationResponseData.class));
    }
    
    /**
     * Verify handling of delete configuration response.
     */
    @Test
    public void testDeleteConfigurationResponseHandling() throws IOException
    {
        DeleteConfigurationResponseData data = DeleteConfigurationResponseData.newBuilder().
            setPid("pid").build();
        
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.DeleteConfigurationResponse, data);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
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
                is(ConfigAdminMessageType.DeleteConfigurationResponse.toString()));
        assertThat((ConfigAdminNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage())));
        assertThat((DeleteConfigurationResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(data));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify handling of the configuration error message.
     */
    @Test
    public void testConfigErrorMessageHandling() throws IOException
    {
        ConfigAdminNamespaceErrorData data = ConfigAdminNamespaceErrorData.newBuilder().
            setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
            setErrorDescription("ERROR ERROR").build();
        
        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.ConfigAdminNamespaceError, data);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
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
                is(ConfigAdminMessageType.ConfigAdminNamespaceError.toString()));
        assertThat((ConfigAdminNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage())));
        assertThat((ConfigAdminNamespaceErrorData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(data));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }

    /**
     * Verify handling of the create configuration response message.
     */
    @Test
    public void testCreateConfigHandling() throws IOException
    {
        CreateFactoryConfigurationResponseData data = CreateFactoryConfigurationResponseData.newBuilder().
            setPid("pid").build();

        TerraHarvestMessage message = createNamespaceMessage(ConfigAdminMessageType.CreateFactoryConfigurationResponse, 
            data);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
        
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
                is(ConfigAdminMessageType.CreateFactoryConfigurationResponse.toString()));
        assertThat((ConfigAdminNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage())));
        assertThat((CreateFactoryConfigurationResponseData)postedEvent.getProperty(
                RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(data));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }

    private TerraHarvestMessage createNamespaceMessage(ConfigAdminMessageType type, Message responseData)
    {
        ConfigAdminNamespace namespaceMessage = ConfigAdminNamespace.newBuilder()
            .setType(type)
            .setData(responseData.toByteString())
            .build();
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, 
            Namespace.ConfigAdmin, 100, namespaceMessage);
        return message;
    }
    private TerraHarvestPayload createPayload(ConfigAdminMessageType type, Message responseData)
    {
        ConfigAdminNamespace namespaceMessage = ConfigAdminNamespace.newBuilder()
             .setType(type)
             .setData(responseData.toByteString())
             .build();
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.ConfigAdmin).
               setNamespaceMessage(namespaceMessage.toByteString()).
               build();
    }
}
