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
import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.SetLayerNameRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.integration.namespace.TestCustomCommsNamespace;

import com.google.protobuf.Message;

/**
 * Utilities for testing and remote interacting with the custom comms namespace.
 * @author callen
 *
 */
public class CustomCommsNamespaceUtils
{
    /**
     * Time out value, used when waiting for messages.
     */
    private static final int TIME_OUT = 800;

    /**
     * Hidden constructor.
     */
    private CustomCommsNamespaceUtils()
    {
        
    }
    
    /**
     * Get a comm layer's name.
     * @param socket
     *     the socket to use for the request
     * @param uuid
     *     the proto UUID message
     * @param type
     *     the type of the layer
     */
    public static GetLayerNameResponseData getCommLayerName(final Socket socket, final SharedMessages.UUID uuid, 
         final CommType type) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //request to get the name of the previously created transport layer.
        GetLayerNameRequestData request = GetLayerNameRequestData.newBuilder().
                setCommType(type).
                setUuid(uuid).
                build();
        
        CustomCommsNamespace.Builder customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.GetLayerNameRequest).
                setData(request.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());

        //check responses
        Message messageRcvd = listener.waitForMessage(Namespace.CustomComms, 
            CustomCommsMessageType.GetLayerNameResponse, TIME_OUT);

        CustomCommsNamespace namespace = (CustomCommsNamespace) messageRcvd;
        return GetLayerNameResponseData.parseFrom(namespace.getData());
    }
    
    /**
     * Set a comm layer's name.
     * @param socket
     *     the socket to use for the request
     * @param uuid
     *     the proto UUID message
     * @param type
     *     the type of the layer
     * @param name
     *     the name to set for the layer
     */
    public static void setCommLayerName(final Socket socket, final SharedMessages.UUID uuid, final CommType type, 
        final String name) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //request to get the name of the previously created transport layer.
        SetLayerNameRequestData request = SetLayerNameRequestData.newBuilder().
                setCommType(type).
                setLayerName(name).
                setUuid(uuid).
                build();
        
        CustomCommsNamespace.Builder customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.SetLayerNameRequest).
                setData(request.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //check responses
        listener.waitForMessage(Namespace.CustomComms, CustomCommsMessageType.SetLayerNameResponse, TIME_OUT);
    }
       
    /**
     * Helper method for getting layer UUIDs known to the service of a certain comm type. 
     * @param commSocket
     *      socket to use for getting the information 
     * @param type
     *      type of the layers to get
     * @return
     *      UUID of the layers found
     */
    public static List<SharedMessages.UUID> getLayerUuidsByType(final Socket commSocket, final CommType type) 
        throws IOException, InterruptedException
    {
        List<FactoryObjectInfo> layerInfoList = getLayersByType(commSocket, type);
        List<SharedMessages.UUID> uuidList = new ArrayList<SharedMessages.UUID>();
        
        for (FactoryObjectInfo info : layerInfoList)
        {
            uuidList.add(info.getUuid());
        }
        
        return uuidList;
    }
    
    /**
     * Helper method for getting layers known to the service of a certain comm type.
     * @param commSocket
     *      socket to use for getting the information 
     * @param type
     *      type of the layers to get
     * @return
     *      info for the layers found
     */
    public static List<FactoryObjectInfo> getLayersByType(final Socket commSocket, final CommType type)
        throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(commSocket);
        
        // construct new custom comms message type
        GetLayersRequestData requestMessage = GetLayersRequestData.newBuilder().
                setCommType(type).
                build();
        
        // create terra harvest message
        TerraHarvestMessage message = TestCustomCommsNamespace.createCustomCommsMessage(
            CustomCommsMessageType.GetLayersRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(commSocket.getOutputStream());
        
        // listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.GetLayersResponse, 1000);

        CustomCommsNamespace response = (CustomCommsNamespace)responseRcvd;
        GetLayersResponseData dataResponse = GetLayersResponseData.parseFrom(
                response.getData());
        
        return dataResponse.getLayerInfoList();
    }
    
    /**
     * Helper method for getting available types of a certain comm type. 
     * @param type
     *      comm type of the types to get
     * @param commSocket
     *      socket to use for getting the information 
     * @return
     *      fully qualified class names of the types of the comms available
     */
    public static List<String> getAvailableCommTypes(final CommType type, final Socket commSocket) throws IOException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(commSocket);
        
        // construct new custom comms message type
        GetAvailableCommTypesRequestData requestMessage = GetAvailableCommTypesRequestData.newBuilder().
                setCommType(type).
                build();
        
        // create terra harvest message
        TerraHarvestMessage message = TestCustomCommsNamespace.createCustomCommsMessage(
                CustomCommsMessageType.GetAvailableCommTypesRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(commSocket.getOutputStream());
        
        // listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.GetAvailableCommTypesResponse, 800);

        CustomCommsNamespace response = (CustomCommsNamespace)responseRcvd;
        GetAvailableCommTypesResponseData dataResponse = GetAvailableCommTypesResponseData.parseFrom(
                response.getData());
        assertThat(dataResponse.getCommType(), is(type));
        
        return dataResponse.getProductTypeList();
    }
}
