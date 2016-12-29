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
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.SetNameRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Utilities for testing and remotely interacting with the asset and asset directory service namespaces.
 * @author callen
 *
 */
public final class AssetNamespaceUtils
{  
    /**
     * If an asset has been created that contains this String in its name, then do not remove it.
     */
    public static final String DO_NOT_REMOVE = "doNotRemove";
      
    private static final int TIMEOUT = 1200;
    
    /**
     * Hidden constructor.
     */
    private AssetNamespaceUtils()
    {
        
    }
    
    /**
     * Helper method for creating a single asset.
     * 
     * @param productType
     *      product type of the asset (usually the fully qualified class name)
     * @param name
     *      name to give to the asset, set to null to use default name
     * @param properties
     *      configuration properties to be set for the asset, set to null if no properties should be set
     * @return
     *      Response data containing the PID and UUID of the created asset. 
     */
    public static CreateAssetResponseData createAsset(Socket socket, String productType, String name, 
            Map<String, Object> properties) throws UnknownHostException, IOException
    {
        MessageListener listener = new MessageListener(socket);
        CreateAssetRequestData.Builder requestBuilder = CreateAssetRequestData.newBuilder().setProductType(productType);
        if (name != null)
        {
            requestBuilder.setName(name);
        }
        if (properties != null)
        {
            requestBuilder.addAllProperties(SharedMessageUtils.convertMapToListSimpleTypesMapEntry(properties));
        }

        TerraHarvestMessage message = createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.CreateAssetRequest, requestBuilder.build());
        message.writeDelimitedTo(socket.getOutputStream());
        
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)listener.waitForMessage(
                Namespace.AssetDirectoryService, AssetDirectoryServiceMessageType.CreateAssetResponse, TIMEOUT);
    
        return CreateAssetResponseData.parseFrom(response.getData());
    }

    /**
     * Change an asset's name.
     * @param socket
     *     the socket connection to use
     * @param uuid
     *     the proto UUID message containing the UUID 
     * @param name
     *     the name to set for the newly created asset
     */
    public static void changeAssetName(final Socket socket, final SharedMessages.UUID uuid, final String name) 
        throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        //request
        SetNameRequestData request = SetNameRequestData.newBuilder().
             setAssetName(name).
             setUuid(uuid).build();
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.SetNameRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        listener.waitForMessage(Namespace.Asset, AssetMessageType.SetNameResponse,
                TIMEOUT);
    }

    /**
     * Get an asset's name.
     * @param socket
     *     the socket connection to use
     * @param uuid
     *     the UUID protocol buffer message
     */
    public static GetNameResponseData getAssetName(final Socket socket, final SharedMessages.UUID uuid) 
        throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        //request to get the name of the previously created transport layer.
        GetNameRequestData request = GetNameRequestData.newBuilder().
                setUuid(uuid).
                build();
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.GetNameRequest, request);
        
        message.writeDelimitedTo(socket.getOutputStream());

        AssetNamespace response = (AssetNamespace)listener.waitForMessage(
                Namespace.Asset, AssetMessageType.GetNameResponse, TIMEOUT);
        
        return GetNameResponseData.parseFrom(response.getData()); 
    }
    
    /**
     * Get info for an asset of the given name.
     * 
     * @return
     *      info for the asset with the given name or null if not found
     */
    public static FactoryObjectInfo tryGetAssetInfoByName(final Socket socket, final String assetName)
    {
        try
        {
            MessageListener listener = new MessageListener(socket); 
            TerraHarvestMessage message = createAssetDirSvcNamespaceMessage(
                    AssetDirectoryServiceMessageType.GetAssetsRequest, null);        
            message.writeDelimitedTo(socket.getOutputStream());
            
            AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)listener.waitForMessage(
                    Namespace.AssetDirectoryService, AssetDirectoryServiceMessageType.GetAssetsResponse, TIMEOUT);    
            
            GetAssetsResponseData dataResponse = GetAssetsResponseData.parseFrom(response.getData());
            
            for (FactoryObjectInfo assetInfo : dataResponse.getAssetInfoList())
            {
                TerraHarvestMessage nameRequestMsg = AssetNamespaceUtils.createAssetMessage(
                        AssetMessageType.GetNameRequest, 
                        GetNameRequestData.newBuilder().setUuid(assetInfo.getUuid()).build());        
                nameRequestMsg.writeDelimitedTo(socket.getOutputStream());
                
                AssetDirectoryServiceNamespace nameResponseMsg = 
                        (AssetDirectoryServiceNamespace)listener.waitForMessage(Namespace.AssetDirectoryService, 
                                AssetMessageType.GetNameResponse, TIMEOUT);
                
                GetNameResponseData nameResponse = GetNameResponseData.parseFrom(nameResponseMsg.getData());
                
                if (nameResponse.getAssetName().equals(assetName))
                {
                    // found the UUID for the desired asset
                    return assetInfo;
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        return null;
    }
    
    /**
     * Request the specified asset to capture data. This is convenience method, which DOES NOT wait for a response.
     * @param socket
     *     the socket connection to use
     * @param uuid
     *     the UUID protocol buffer message
     */
    public static void requestDataCapture(final Socket socket, final SharedMessages.UUID uuid) 
        throws IOException
    {
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(uuid)
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY).build();
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
            AssetMessageType.CaptureDataRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
    }
    
    /**
     * Remove the specified asset.
     *  @param socket
     *     the socket connection to use
     * @param uuid
     *     the UUID protocol buffer message
     */
    public static void removeAsset(final Socket socket, final SharedMessages.UUID uuid) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        DeleteRequestData request = DeleteRequestData.newBuilder().
                setUuid(uuid).build();

        TerraHarvestMessage message = createAssetMessage(AssetMessageType.DeleteRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        listener.waitForMessage(Namespace.Asset, AssetMessageType.DeleteResponse, 
                TIMEOUT);
    }
    
    /**
     * Remove all assets.
     */
    public static void removeAllAssets(Socket socket) throws IOException
    {
        MessageListener listener = new MessageListener(socket); 
        TerraHarvestMessage message = createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetAssetsRequest, null);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)listener.waitForMessage(
                Namespace.AssetDirectoryService, AssetDirectoryServiceMessageType.GetAssetsResponse, TIMEOUT);    
        
        GetAssetsResponseData dataResponse = GetAssetsResponseData.parseFrom(response.getData()); 

        for(FactoryObjectInfo type: dataResponse.getAssetInfoList())
        {
            
            MessageListener assetMsgListener = new MessageListener(socket);
            GetNameRequestData getNameRequest = GetNameRequestData.newBuilder().
                    setUuid(type.getUuid()).build();
                    
            TerraHarvestMessage assetMsg = createAssetMessage(AssetMessageType.GetNameRequest, getNameRequest);
            assetMsg.writeDelimitedTo(socket.getOutputStream());
            
            AssetNamespace assetResponse = (AssetNamespace)assetMsgListener.waitForMessage(
                    Namespace.Asset, AssetMessageType.GetNameResponse, TIMEOUT);
            
            GetNameResponseData nameResponse = GetNameResponseData.parseFrom(assetResponse.getData());
            
            if (!nameResponse.getAssetName().contains(DO_NOT_REMOVE))
            {            
                removeAsset(socket, type.getUuid());
            }
        }
    }
    
    /**
     * Execute a command.
     * @param command
     *      the command to execute
     * @param commandEnum
     *      the enum value of the command being executed
     * @param uuid
     *      the UUID of the asset which will be executing the command
     * @param socket
     *      the socket to use for communication to the controller
     */
    public static ExecuteCommandResponseData executeCommand(final Message command, 
            final CommandTypesGen.CommandType.Enum commandEnum, 
            final SharedMessages.UUID uuid, final Socket socket) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        ExecuteCommandRequestData executeCommandRequest = ExecuteCommandRequestData.newBuilder().
                setUuid(uuid).
                setCommand(command.toByteString()).
                setCommandType(commandEnum).
                build();
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(AssetMessageType.ExecuteCommandRequest, 
                executeCommandRequest);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.ExecuteCommandResponse, TIMEOUT);
        AssetNamespace response = (AssetNamespace)responseRcvd;
        ExecuteCommandResponseData responseData =  ExecuteCommandResponseData.parseFrom(response.getData());
        assertThat(responseData.getResponseType(), is(notNullValue()));
        return responseData;
    }
    
    /**
     * Helper method for creating asset directory service messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createAssetDirSvcNamespaceMessage(final AssetDirectoryServiceMessageType type, 
            final Message message)
    {
        AssetDirectoryServiceNamespace.Builder assetMessageBuilder = AssetDirectoryServiceNamespace.newBuilder().
                setType(type);
        if(message != null)
        {
            assetMessageBuilder.setData(message.toByteString());
        }                
    
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.AssetDirectoryService, assetMessageBuilder);
        return thMessage;
    }

    /**
     * Deactivate an asset with the given UUID, wait for the response.
     */
    public static void activateAsset(Socket socket, SharedMessages.UUID uuid) throws IOException, 
        InvalidProtocolBufferException
    {
        MessageListener listener = new MessageListener(socket);
        
        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, Asset.TOPIC_ACTIVATION_COMPLETE, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, 
                    SharedMessageUtils.convertProtoUUIDtoUUID(uuid).toString()));
        ActivateRequestData request = ActivateRequestData.newBuilder().
                setUuid(uuid).build(); 
            
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.ActivateRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        try
        {
            listener.waitForMessages(TIMEOUT, 
                new BasicMessageMatcher(Namespace.Asset, 
                        AssetMessageType.ActivateResponse), 
                new MessageMatchers.EventMessageMatcher(Asset.TOPIC_ACTIVATION_COMPLETE));
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }

    /**
     * Deactivate an asset with the given UUID, wait for the response.
     */
    public static void deactivateAsset(Socket socket, SharedMessages.UUID uuid) throws IOException,
            InvalidProtocolBufferException
    {
        MessageListener listener = new MessageListener(socket);        
        
        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, Asset.TOPIC_DEACTIVATION_COMPLETE, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, 
                        SharedMessageUtils.convertProtoUUIDtoUUID(uuid).toString()));
        
        DeactivateRequestData request = DeactivateRequestData.newBuilder().
                setUuid(uuid).build(); 
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.DeactivateRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        try
        {
            listener.waitForMessages(TIMEOUT, 
                new BasicMessageMatcher(Namespace.Asset, 
                    AssetMessageType.DeactivateResponse), 
                new MessageMatchers.EventMessageMatcher(
                    Asset.TOPIC_DEACTIVATION_COMPLETE));
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }
    
    /**
     * Request the specified asset to capture data. This is convenience method, which DOES wait for a response.
     * 
     * @param socket
     *     the socket connection to use
     * @param uuid
     *     the UUID protocol buffer message
     */
    public static SharedMessages.UUID requestDataCaptureReturnObsUuid(final Socket socket, 
            final SharedMessages.UUID uuid) throws IOException
    {
        MessageListener listener = new MessageListener(socket);

        //create request for the asset to capture
        CaptureDataRequestData captureData = CaptureDataRequestData.newBuilder()
            .setUuid(uuid)
            .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY).build();

        //terra harvest message to complete the request
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(AssetMessageType.CaptureDataRequest, 
            captureData);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());
        
        AssetNamespace response = (AssetNamespace)
                listener.waitForMessage(Namespace.Asset, AssetMessageType.CaptureDataResponse, 2000);
        
        return CaptureDataResponseData.parseFrom(response.getData()).getObservationUuid();
    }

    /**
     * Helper method for creating asset messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createAssetMessage(final AssetMessageType type, 
            final Message message)
    {
        AssetNamespace.Builder assetMessageBuilder = AssetNamespace.newBuilder().
                setType(type).
                setData(message.toByteString());
    
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.Asset, assetMessageBuilder);
        return thMessage;
    }
}
