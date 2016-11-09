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
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.Socket;

import mil.dod.th.core.remote.proto.TransportLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.remote.integration.namespace.TestCustomCommsNamespace;

/**
 * This class assists with common TransportLayer interactions.
 * @author allenchl
 *
 */
public final class RemoteTransportLayerUtils
{
    /**
     * Timeout for LinkLayer interactions.
     */
    final static int TIME_OUT = 1200;
    
    /**
     * Hidden constructor.
     */
    private RemoteTransportLayerUtils()
    {
        
    }
    
    /**
     * Create a TransportLayer.
     * @param name
     *     the name to use for the new transport layer
     * @param className
     *     the type of transport layer to create
     * @param uuidOfLinkLayer
     *     the UUID of the link layer create this transport layer on top of
     * @param socket
     *     the socket to use for the request
     * @return
     *     the UUID of the created TransportLayer
     */
    public static UUID createTransportLayer(String name, String className, SharedMessages.UUID uuidOfLinkLayer, 
            Socket socket) throws IOException, InterruptedException
    {
        UUID linkUuid = createTransportCommLayer(name, className, uuidOfLinkLayer, socket).getInfo().getUuid();
        
        assertThat(linkUuid, is(notNullValue()));
        return linkUuid;
    }
    
    /**
     * Create a transport comms layer.
     * @param name
     *     the name to use for the new transport layer
     * @param className
     *     the type of transport layer to create
     * @param uuidOfLinkLayer
     *     the UUID of the link layer create this transport layer on top of or null if no link layer
     * @param socket
     *     the socket to use for the request
     * @return
     *     the response message from creating the layer
     */
    public static CreateTransportLayerResponseData createTransportCommLayer(final String name, final String className, 
            final SharedMessages.UUID uuidOfLinkLayer, final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // request data to create a new transport layer
        CreateTransportLayerRequestData.Builder requestMessage = CreateTransportLayerRequestData.newBuilder().
                setTransportLayerName(name).
                setTransportLayerProductType(className);
        
        if (uuidOfLinkLayer != null)
        {
            requestMessage.setLinkLayerUuid(uuidOfLinkLayer);
        }
        
        // create terra harvest message
        TerraHarvestMessage message = TestCustomCommsNamespace.
            createCustomCommsMessage(CustomCommsMessageType.CreateTransportLayerRequest, requestMessage.build());
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        CustomCommsNamespace response = (CustomCommsNamespace)listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.CreateTransportLayerResponse, TIME_OUT);

        //return the response
        return CreateTransportLayerResponseData.parseFrom(response.getData());
    }
    
    /**
     * Remove a TransportLayer, this assumes the layer is not active.
     * @param transUuid
     *      the UUID of the TransportLayer to remove
     * @param socket
     *     the socket to use for the request
     */
    public static void removeTransportLayer(final UUID transUuid, final Socket socket) throws IOException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new transport message type
        DeleteRequestData removeLayer = 
                DeleteRequestData.newBuilder().setTransportLayerUuid(transUuid).build();
        
        TransportLayerNamespace.Builder transMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.DeleteRequest).
                setData(removeLayer.toByteString());

        // create terra harvest message
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());

        listener.waitForMessage(Namespace.TransportLayer, TransportLayerMessageType.DeleteResponse, TIME_OUT);
    }
}
