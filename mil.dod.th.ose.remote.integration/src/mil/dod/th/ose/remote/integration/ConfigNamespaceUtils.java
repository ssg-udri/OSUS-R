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
package mil.dod.th.ose.remote.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.Socket;

import com.google.protobuf.Message;

import example.metatype.XML.ExampleClass;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.remote.integration.namespace.TestConfigAdminNamespace;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * This class assists with interactions relating to the {@link ConfigAdminNamespace}.
 * @author allenchl
 *
 */
public final class ConfigNamespaceUtils
{
    /**
     * Hidden constructor.
     */
    private ConfigNamespaceUtils()
    {
    }
    
    /**
     * Set a property remotely.
     * 
     * @param pid
     *      pid of the configuration to update
     * @param key
     *      the key defining the property to set
     * @param value
     *      the value to set
     * @param socket
     *      the socket to use for communication
     */
    public static void setConfigProperty(final String pid, final String key, final Object value, final Socket socket)
    {
        Multitype multitypeValue = SharedMessageUtils.convertObjectToMultitype(value);
        
        try
        {
            MessageListener listener = new MessageListener(socket);
            SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder().setKey(key).setValue(multitypeValue).build();
            SetPropertyRequestData request = 
                    SetPropertyRequestData.newBuilder().setPid(pid).addProperties(prop).build();
            
            //create terra harvest message   
            TerraHarvestMessage message = TestConfigAdminNamespace.createConfigAdminMessage(
                    ConfigAdminMessageType.SetPropertyRequest, request);
            
            // send out message
            message.writeDelimitedTo(socket.getOutputStream());
            
            listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 500);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Get the property value for a particular key.
     * @param key
     *      the key that should be associated with the above value
     * @param socket
     *      the socket to use for communication
     * @return
     *      the {@link GetPropertyResponseData}
     */
    public static GetPropertyResponseData getPropertyValue(final String key, final Socket socket) 
        throws InterruptedException, IOException
    {
        MessageListener listener = new MessageListener(socket);
        GetPropertyRequestData request = GetPropertyRequestData.newBuilder()
                .setPid(ExampleClass.CONFIG_PID)
                .setKey(key).build();
            
        //create terra harvest message   
        TerraHarvestMessage message = TestConfigAdminNamespace.createConfigAdminMessage(
                ConfigAdminMessageType.GetPropertyRequest, request);
                
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        ConfigAdminNamespace messageResponse = (ConfigAdminNamespace)listener.waitForMessage(Namespace.ConfigAdmin, 
                ConfigAdminMessageType.GetPropertyResponse, 500);
        
        return GetPropertyResponseData.parseFrom(messageResponse.getData());
    }
    
    
    /**
     * Sends a delete configuration method for the specified PID. Verifies a response is received.
     * @param pid
     *      PID of the configuration to be deleted.
     * @param socket
     *      Socket used to send the message.
     * @param listener
     *      Listener used to listener for the response message.
     */
    public static void deleteConfig(final String pid, final Socket socket, final MessageListener listener) throws 
        IOException, InterruptedException
    {
        //Create delete message to reset altered configuration values.
        DeleteConfigurationRequestData deletConfigRequest = DeleteConfigurationRequestData.newBuilder().setPid(
                pid).build();
        
        //create terra harvest message
        TerraHarvestMessage thMessage = 
                TestConfigAdminNamespace.createConfigAdminMessage(
                        ConfigAdminMessageType.DeleteConfigurationRequest, deletConfigRequest);
        
        //send out message
        thMessage.writeDelimitedTo(socket.getOutputStream());
        
        //Verify a delete configuration response is received.
        Message message = listener.waitForMessage(Namespace.ConfigAdmin, 
                ConfigAdminMessageType.DeleteConfigurationResponse, 1500);
        
        //verify response has correct PID
        DeleteConfigurationResponseData responseData = 
                DeleteConfigurationResponseData.parseFrom(((ConfigAdminNamespace)message).getData());
        assertThat(responseData.getPid(), is(pid));
    }
}
