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
package mil.dod.th.ose.remote.integration.namespace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigurationInfoType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.integration.ConfigNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageMatchers.EventMessageMatcher;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;

import example.metatype.XML.ExampleClass;
import example.metatype.XML.ExampleClassConfig;

/**
 * Tests routing of messages to the {@link ConfigAdminNamespace}.
 * @author callen
 *
 */
public class TestConfigAdminNamespace 
{
    private Socket socket;
    
    /**
     * Creates an example asset to be used for each of the unit tests.
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
    }
    
    /**
     * Removes the event registrations and closes the socket.
     */
    @After
    public void teardown() throws UnknownHostException, IOException, InterruptedException
    {     
        try 
        {
            MessageListener.unregisterEvent(socket);            
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * This test has to be run first to create the OSGi type configuration.
     * Sets the configuration with an initial value.
     * 
     * Verify the updating of the configuration properties.
     * 
     * Verify setting different primitive types as values for property.
     */
    @Test
    public void testSetProperty() throws IOException, InterruptedException
    {
        //try a string
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", "golden egg", socket);
        
        //try setting integer property
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", 32000, socket);
        
        //try setting float property
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", 1.234f, socket);
    }
    
    /**
     * Test that the get property request is properly handled.
     * 
     * Verify that the expected value set in {@link TestConfigAdminNamespace#testSetProperty()} is returned.
     */
    @Test
    public void testGetProperty() throws IOException, InterruptedException
    {
        //set the property
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", "golden egg", socket);
        
        //request to get a property key value
        //verify values returned
        GetPropertyResponseData propData = ConfigNamespaceUtils.getPropertyValue("name", socket);
        assertThat(propData.getValue(), is(notNullValue()));
        assertThat(propData.getPid(), is(ExampleClass.CONFIG_PID));
        assertThat(propData.getKey(), is("name"));
        
        assertThat(SharedMessageUtils.convertMultitypeToObject(propData.getValue()), is((Object)"golden egg"));
    }
    
    /**
     * Test the get configuration info request. 
     * 
     * Verify that the configuration list is returned and contains the PID value previously set.
     */
    @Test
    public void testGetConfigurationInfo() throws IOException, InterruptedException
    {
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", "golden egg", socket);
        
        //allow config admin to post property set
        Thread.sleep(1000);
        
        MessageListener listener = new MessageListener(socket);
        
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
            setFilter("(name=golden egg)").setIncludeProperties(false).build();
        
        //create terra harvest message   
        TerraHarvestMessage message = createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
                
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        Message messageRcvd = 
            listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.GetConfigurationInfoResponse, 1000);

        //parse specific message type
        ConfigAdminNamespace namespaceResponse = (ConfigAdminNamespace) messageRcvd;
        assertThat(namespaceResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoResponse));
        //verify
        GetConfigurationInfoResponseData responseData = GetConfigurationInfoResponseData.
            parseFrom(namespaceResponse.getData());
        List<ConfigurationInfoType> data = responseData.getConfigurationsList();
        assertThat(data.size(), is(1));
        assertThat(data.get(0).getPid(), is(ExampleClass.CONFIG_PID));
        assertThat(data.get(0).hasFactoryPid(), is(false));
        assertThat(data.get(0).getBundleLocation(), is(notNullValue()));
    }
    
    /**
     * Test the 'get property keys request'.
     * 
     * Verify that keys are returned. 
     * 
     * Verify that is there is no properties from the requested PID that the 'property key list' is empty and not
     * null.
     */
    @Test
    public void testGetPropertyKeys() throws IOException, InterruptedException
    {   
        ConfigNamespaceUtils.setConfigProperty(ExampleClass.CONFIG_PID, "name", "golden egg", socket);
        
        //allow config admin to post property set
        Thread.sleep(1000);
        
        MessageListener listener = new MessageListener(socket);
        
        GetPropertyKeysRequestData request = GetPropertyKeysRequestData.newBuilder().
                setPid(ExampleClass.CONFIG_PID).build();
                
        //create terra harvest message   
        TerraHarvestMessage message = createConfigAdminMessage(ConfigAdminMessageType.GetPropertyKeysRequest, request);
                
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for response
        ConfigAdminNamespace namespaceResponse = (ConfigAdminNamespace)listener.waitForMessage(
                Namespace.ConfigAdmin, ConfigAdminMessageType.GetPropertyKeysResponse, 500);
        
        //verify
        GetPropertyKeysResponseData keysData = GetPropertyKeysResponseData.parseFrom(namespaceResponse.getData());
        assertThat(keysData.getKeyList(), is(notNullValue()));
        assertThat(keysData.getKeyCount(), greaterThan(0));
        assertThat(keysData.getKeyList(), containsInAnyOrder("service.pid", "name"));
        assertThat(keysData.getPid(), is(ExampleClass.CONFIG_PID));
                            
        //create message with a pid that is not known
        request = GetPropertyKeysRequestData.newBuilder().setPid("Look ma no hands").build();
                    
        //create terra harvest message   
        message = createConfigAdminMessage(ConfigAdminMessageType.GetPropertyKeysRequest, request);
                    
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
         
        // read in response
        namespaceResponse = (ConfigAdminNamespace)listener.waitForMessage(
                Namespace.ConfigAdmin, ConfigAdminMessageType.GetPropertyKeysResponse, 500);
        //verify
        keysData = GetPropertyKeysResponseData.parseFrom(namespaceResponse.getData());
        assertThat(keysData.getKeyList(), is(notNullValue()));
        assertThat(keysData.getKeyList().isEmpty(), is(true));
        assertThat(keysData.getPid(), is("Look ma no hands"));       
    }

    /**
     * Test delete configuration request.
     * 
     * Verify configuration is removed.
     */
    @Test
    public void testDeleteConfiguration() throws UnknownHostException, IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        UUID someUUID = UUID.randomUUID();
        CreateFactoryConfigurationRequestData request = CreateFactoryConfigurationRequestData.newBuilder().
                setFactoryPid("example.asset.ExampleAssetConfig").
                addFactoryProperty(SimpleTypesMapEntry.newBuilder().setKey("someKey").
                        setValue(Multitype.newBuilder().setType(Type.STRING).
                                setStringValue(someUUID.toString())).build()).build();
            
        //create terra harvest message   
        TerraHarvestMessage message = createConfigAdminMessage(
            ConfigAdminMessageType.CreateFactoryConfigurationRequest, request);
                    
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
 
        // read in response
        Message messageRcvd = 
                listener.waitForMessage(Namespace.ConfigAdmin, 
                        ConfigAdminMessageType.CreateFactoryConfigurationResponse, 1500);
            
        ConfigAdminNamespace nameMessage = (ConfigAdminNamespace) messageRcvd;
        CreateFactoryConfigurationResponseData response = CreateFactoryConfigurationResponseData.
            parseFrom(nameMessage.getData());
        assertThat(response.hasPid(), is(true));
        String newPid = response.getPid();

        //remove the configuration
        ConfigNamespaceUtils.deleteConfig(newPid, socket, listener);
    }
    
    /**
     * Test setting a configuration value for a configuration that is not a factory.
     * Verify value is set.
     */
    @Test
    public void testSetPropertyNonFactory() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //register to listen for magic number event that will be posted when the configuration value is 25
        RemoteEventRegistration.regRemoteEventMessages(socket, "TOPIC_MAGICAL_UPDATE_EXAMPLECLASS");
        
        Multitype value = Multitype.newBuilder().setType(Type.INT32).setInt32Value(25).build();
        SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder()
                .setKey(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE).setValue(value).build();
        SetPropertyRequestData requestSet = SetPropertyRequestData.newBuilder().
            setPid(ExampleClass.CONFIG_PID).addProperties(prop).build();
        
        //create terra harvest message   
        TerraHarvestMessage thMessage = createConfigAdminMessage(ConfigAdminMessageType.SetPropertyRequest, requestSet);
                
        // send out message
        thMessage.writeDelimitedTo(socket.getOutputStream());
        
        //collect responses
        listener.waitForMessages(4500, 
                new BasicMessageMatcher(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse),
                new EventMessageMatcher("TOPIC_MAGICAL_UPDATE_EXAMPLECLASS"));
        
        GetPropertyRequestData requestGet = GetPropertyRequestData.newBuilder().
                setPid(ExampleClass.CONFIG_PID).
                setKey(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE).build();
            
        //create terra harvest message   
        thMessage = createConfigAdminMessage(ConfigAdminMessageType.GetPropertyRequest, requestGet);

        // send out message
        thMessage.writeDelimitedTo(socket.getOutputStream());
        
        //wait
        ConfigAdminNamespace namespaceResponse = (ConfigAdminNamespace)listener.waitForMessage(
                Namespace.ConfigAdmin, ConfigAdminMessageType.GetPropertyResponse, 500);
        
        GetPropertyResponseData propData = GetPropertyResponseData.parseFrom(namespaceResponse.getData());
        assertThat(propData.getValue().getInt32Value(), is(25));
        assertThat(propData.getPid(), is(ExampleClass.CONFIG_PID));
        assertThat(propData.getKey(), is(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE));
    }
    
    /**
     * Test setting multiple configuration properties with one message and receiving multiple configuration properties
     * with one message.
     */
    @Test
    public void testGetSetMultipleConfigProperties() throws UnknownHostException, IOException, InterruptedException
    {   
        MessageListener listener = new MessageListener(socket);
        
        //Delete configuration to insure properties are set to default values.
        ConfigNamespaceUtils.deleteConfig(ExampleClass.CONFIG_PID, socket, listener);
        
        SetPropertyRequestData.Builder requestSet = SetPropertyRequestData.newBuilder().setPid(
                ExampleClass.CONFIG_PID);
        //Set thread pool size for the event admin.
        Multitype value1 = Multitype.newBuilder().setType(Type.INT32).setInt32Value(25).build();
        SimpleTypesMapEntry prop1 = SimpleTypesMapEntry.newBuilder()
                .setKey(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE).setValue(value1).build();
        requestSet.addProperties(prop1);
        //Set timeout
        Multitype value2 = Multitype.newBuilder().setType(Type.STRING).setStringValue("something").build();
        SimpleTypesMapEntry prop2 = 
                SimpleTypesMapEntry.newBuilder()
                .setKey(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING).setValue(value2).build();
        requestSet.addProperties(prop2);
        //Set require topic
        Multitype value3 = Multitype.newBuilder().setType(Type.STRING).setStringValue("somewhere").build();
        SimpleTypesMapEntry prop3 = 
                SimpleTypesMapEntry.newBuilder()
                .setKey(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN).setValue(value3).build();
        requestSet.addProperties(prop3);
        
        //create terra harvest message   
        TerraHarvestMessage thMessage = createConfigAdminMessage(ConfigAdminMessageType.SetPropertyRequest, 
                requestSet.build());
        
        //send out message
        thMessage.writeDelimitedTo(socket.getOutputStream());
        
        //Verify a set property response is received.
        Message messageRcvd = 
            listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 2000);
        
        //Create message to retrieve configuration and associated properties.
        GetConfigurationInfoRequestData request = GetConfigurationInfoRequestData.newBuilder().
                setFilter("(service.pid=" + ExampleClass.CONFIG_PID +")").setIncludeProperties(true).build();
        
        //create terra harvest message   
        TerraHarvestMessage message = createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
            request);
                
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());

        messageRcvd = listener.waitForMessage(Namespace.ConfigAdmin, 
                ConfigAdminMessageType.GetConfigurationInfoResponse, 1000);

        //parse specific message type
        ConfigAdminNamespace namespaceResponse = (ConfigAdminNamespace) messageRcvd;
        assertThat(namespaceResponse.getType(), is(ConfigAdminMessageType.GetConfigurationInfoResponse));
        //verify
        GetConfigurationInfoResponseData responseData = GetConfigurationInfoResponseData.
            parseFrom(namespaceResponse.getData());
        List<ConfigurationInfoType> data = responseData.getConfigurationsList();
        assertThat(data.size(), is(1));
        assertThat(data.get(0).getPid(), is(ExampleClass.CONFIG_PID));
        assertThat(data.get(0).hasFactoryPid(), is(false));
        assertThat(data.get(0).getBundleLocation(), is(notNullValue()));
        assertThat(data.get(0).getPropertiesCount(), is(4));
        Map<String, SimpleTypesMapEntry> properties = Maps.uniqueIndex(data.get(0).getPropertiesList(), 
            new Function<SimpleTypesMapEntry, String>()
            {
                @Override
                public String apply(SimpleTypesMapEntry item)
                {
                    return item.getKey();
                }
            });
        assertThat(properties.get("service.pid").getValue().getStringValue(), is(ExampleClass.CONFIG_PID));
        assertThat(properties.get(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN)
                .getValue().getStringValue(), is("somewhere"));
        assertThat(properties.get(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE)
                .getValue().getInt32Value(), is(25));
        assertThat(properties.get(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING)
                .getValue().getStringValue(), is("something"));
        
        ConfigNamespaceUtils.deleteConfig(ExampleClass.CONFIG_PID, socket, listener);
    }
    
    /**
     * Create a configuration admin protocol message of the specified type.
     * 
     * @param type
     *      {@link ConfigAdminMessageType}
     * @param message
     *      Data message.
     * @return
     *      {@link TerraHarvestMessage}
     */
    public static TerraHarvestMessage createConfigAdminMessage(final ConfigAdminMessageType type, 
        final Message message)
    {
        ConfigAdminNamespace.Builder configMessageBuilder =  ConfigAdminNamespace.newBuilder();
        
        if(message != null)
        {
            configMessageBuilder.setData(message.toByteString());
        }
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.ConfigAdmin,
            configMessageBuilder.setType(type));
        
        return thMessage;
    }
}
