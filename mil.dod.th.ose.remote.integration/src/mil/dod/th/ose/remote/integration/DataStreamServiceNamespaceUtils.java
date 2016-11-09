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

import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.protobuf.Message;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DisableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.StreamProfile;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.EnableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.datastream.capability.StreamProfileCapabilitiesGen.StreamProfileCapabilities;

/**
 * @author jmiller
 *
 */
public final class DataStreamServiceNamespaceUtils
{    
    private static final int TIMEOUT = 5000;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private DataStreamServiceNamespaceUtils()
    {
        
    }
    
    /**
     * Enable a stream profile using the given UUID and wait for the response.
     */
    public static void enableStreamProfile(Socket socket, SharedMessages.UUID uuid) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        int regId = RemoteEventRegistration.regRemoteEventMessages(socket,
                DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED, String.format("(%s=%s)",
                        FactoryDescriptor.EVENT_PROP_OBJ_UUID, 
                        SharedMessageUtils.convertProtoUUIDtoUUID(uuid)));
        
        EnableStreamProfileRequestData request = EnableStreamProfileRequestData.newBuilder().
                setUuid(uuid).build();
        TerraHarvestMessage message = DataStreamServiceNamespaceUtils.createDataStreamServiceNamespaceMessage(
                DataStreamServiceMessageType.EnableStreamProfileRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        try
        {
            listener.waitForMessages(TIMEOUT,
                    new BasicMessageMatcher(Namespace.DataStreamService, 
                            DataStreamServiceMessageType.EnableStreamProfileResponse),
                    new MessageMatchers.EventMessageMatcher(DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED));
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }
    
    /**
     * Disable a stream profile using the given UUID and wait for the response.
     */
    public static void disableStreamProfile(Socket socket, SharedMessages.UUID uuid) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        DisableStreamProfileRequestData request = DisableStreamProfileRequestData.newBuilder().
                setUuid(uuid).build();
        TerraHarvestMessage message = DataStreamServiceNamespaceUtils.createDataStreamServiceNamespaceMessage(
                DataStreamServiceMessageType.DisableStreamProfileRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        try
        {
            listener.waitForMessages(TIMEOUT,
                    new BasicMessageMatcher(Namespace.DataStreamService, 
                            DataStreamServiceMessageType.DisableStreamProfileResponse));                   
        }
        finally
        {
            
        }
    }
    
    /**
     * Get all stream profiles.
     */
    public static Set<StreamProfile> getStreamProfiles(Socket socket) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        GetStreamProfilesRequestData request = GetStreamProfilesRequestData.newBuilder().build();
        TerraHarvestMessage message = DataStreamServiceNamespaceUtils.createDataStreamServiceNamespaceMessage(
                DataStreamServiceMessageType.GetStreamProfilesRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        DataStreamServiceNamespace response = (DataStreamServiceNamespace)listener.waitForMessage(
                Namespace.DataStreamService, DataStreamServiceMessageType.GetStreamProfilesResponse, TIMEOUT);
        
        GetStreamProfilesResponseData dataResponse = GetStreamProfilesResponseData.parseFrom(response.getData());
        
        return Sets.newHashSet(dataResponse.getStreamProfileList());      
    }
    
    /**
     * Get stream profile capabilities.
     */
    public static StreamProfileCapabilities getStreamProfileCapabilities(Socket socket, String productType) 
            throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder()
                .setProductType(productType).build();
        TerraHarvestMessage message = DataStreamServiceNamespaceUtils.createDataStreamServiceNamespaceMessage(
                DataStreamServiceMessageType.GetCapabilitiesRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        DataStreamServiceNamespace response = (DataStreamServiceNamespace)listener.waitForMessage(
                Namespace.DataStreamService, DataStreamServiceMessageType.GetCapabilitiesResponse, TIMEOUT);
        
        GetCapabilitiesResponseData dataResponse = GetCapabilitiesResponseData.parseFrom(response.getData());
        
        return dataResponse.getCapabilities();
    }
       
    /**
     * Helper method for creating datastream service messages to send to the controller.
     * 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createDataStreamServiceNamespaceMessage(final DataStreamServiceMessageType type,
            final Message message)
    {
        DataStreamServiceNamespace.Builder dataStreamMessageBuilder = DataStreamServiceNamespace.newBuilder().
                setType(type);
        
        if (message != null)
        {
            dataStreamMessageBuilder.setData(message.toByteString());
        }
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(
                Namespace.DataStreamService, dataStreamMessageBuilder);
        
        return thMessage;
    }
}
