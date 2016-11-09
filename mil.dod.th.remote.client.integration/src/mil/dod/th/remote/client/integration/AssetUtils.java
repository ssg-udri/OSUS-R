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

package mil.dod.th.remote.client.integration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages
    .AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.AssetDirectoryMessageGenerator;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;

/**
 * General utilities for the core integration tests.
 * 
 * @author dhumeniuk
 *
 */
public class AssetUtils
{
    private static MessageSenderService getMsgSenderService(BundleContext context)
    {
        return ServiceUtils.getService(context, MessageSenderService.class);
    }
    
    private static AssetDirectoryMessageGenerator getAssetDirMsgGen(BundleContext context) 
    {
        return ServiceUtils.getService(context, AssetDirectoryMessageGenerator.class);
    }
    
    /**
     * Create an asset through the remote interface.
     */
    public static UUID createAsset(BundleContext context, String productType, String assetName) throws IOException
    {
        MessageListener listener = new MessageListener(context);
        
        CreateAssetRequestData createAssetRequest = CreateAssetRequestData.newBuilder()
                .setProductType(productType)
                .setName(assetName)
                .build();
        AssetDirectoryServiceNamespace namespace = AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.CreateAssetRequest)
                .setData(createAssetRequest.toByteString())
                .build();
        getMsgSenderService(context).sendRequest(SystemConstants.REMOTE_SYS_ID, TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.AssetDirectoryService)
                .setNamespaceMessage(namespace.toByteString()).build());
        
        CreateAssetResponseData response = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.CreateAssetResponse, 5, TimeUnit.SECONDS);
        return response.getInfo().getUuid();
    }
    
    /**
     * Delete an asset given its UUID.
     */
    public static void deleteAsset(BundleContext context, UUID assetUuid) throws IOException
    {
        DeleteRequestData deleteAssetRequest = DeleteRequestData.newBuilder()
                .setUuid(assetUuid)
                .build();
        AssetNamespace namespace = AssetNamespace.newBuilder()
                .setType(AssetMessageType.DeleteRequest)
                .setData(deleteAssetRequest.toByteString())
                .build();
        getMsgSenderService(context).sendRequest(SystemConstants.REMOTE_SYS_ID, TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Asset)
                .setNamespaceMessage(namespace.toByteString()).build());
    }
    
    /**
     * Request asset (with the given UUID) to capture data.
     */
    public static void captureData(BundleContext context, UUID uuid) throws IOException, TimeoutException
    {
        //MessageListener listener = new MessageListener(context);
        
        CaptureDataRequestData captureDataRequest = CaptureDataRequestData.newBuilder()
                .setUuid(uuid)
                .build();
        AssetNamespace namespace = AssetNamespace.newBuilder()
                .setType(AssetMessageType.CaptureDataRequest)
                .setData(captureDataRequest.toByteString())
                .build();
        getMsgSenderService(context).sendRequest(SystemConstants.REMOTE_SYS_ID, TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Asset)
                .setNamespaceMessage(namespace.toByteString()).build());
        
        // TD: listener service doesn't support this namespace yet
        //listener.waitForMessage(CaptureDataResponseData.class, 5, TimeUnit.SECONDS);
    }

    /**
     * Delete all assets that exists on the remote system.
     */
    public static void deleteAllAsset(BundleContext context) throws IOException
    {
        MessageListener listener = new MessageListener(context);
        
        getAssetDirMsgGen(context).createGetAssetsRequest().send(SystemConstants.REMOTE_SYS_ID);
        GetAssetsResponseData response = listener.waitForMessage(Namespace.AssetDirectoryService,
                AssetDirectoryServiceMessageType.GetAssetsResponse, 5, TimeUnit.SECONDS);
        
        for (FactoryObjectInfo info : response.getAssetInfoList())
        {
            deleteAsset(context, info.getUuid());
        }
    }
}
