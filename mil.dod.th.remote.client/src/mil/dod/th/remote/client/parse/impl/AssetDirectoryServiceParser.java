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
package mil.dod.th.remote.client.parse.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetScannableAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.MessageParser;

/**
 * Parsing implementation for {@link AssetDirectoryServiceNamespace} messages.
 * 
 * @author dlandoll
 */
public class AssetDirectoryServiceParser implements MessageParser<AssetDirectoryServiceNamespace>
{
    @Override // NOCHECKSTYLE: Cyclomatic complexity unavoidable due to number of message types
    public RemoteMessage<AssetDirectoryServiceNamespace> parse(final TerraHarvestMessage rawMessage,
        final TerraHarvestPayload payload) throws InvalidProtocolBufferException
    {
        final AssetDirectoryServiceNamespace namespaceMessage =
            AssetDirectoryServiceNamespace.parseFrom(payload.getNamespaceMessage());

        final Message dataMessage;
        switch (namespaceMessage.getType())
        {
            case GetAssetTypesRequest:
                dataMessage = null;
                break;
            case GetAssetTypesResponse:
                dataMessage = GetAssetTypesResponseData.parseFrom(namespaceMessage.getData());
                break;
            case ScanForNewAssetsRequest:
                dataMessage = ScanForNewAssetsRequestData.parseFrom(namespaceMessage.getData());
                break;
            case ScanForNewAssetsResponse:
                dataMessage = null;
                break;
            case CreateAssetRequest:
                dataMessage = CreateAssetRequestData.parseFrom(namespaceMessage.getData());
                break;
            case CreateAssetResponse:
                dataMessage = CreateAssetResponseData.parseFrom(namespaceMessage.getData());
                break;
            case GetAssetsRequest:
                dataMessage = null;
                break;
            case GetAssetsResponse:
                dataMessage = GetAssetsResponseData.parseFrom(namespaceMessage.getData());
                break;
            case GetScannableAssetTypesRequest:
                dataMessage = null;
                break;
            case GetScannableAssetTypesResponse:
                dataMessage = GetScannableAssetTypesResponseData.parseFrom(namespaceMessage.getData());
                break;
            case GetCapabilitiesRequest:
                dataMessage = GetCapabilitiesRequestData.parseFrom(namespaceMessage.getData());
                break;      
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.parseFrom(namespaceMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                    String.format("Message type: %s is not a supported type for AssetDirectoryServiceNamespace.",
                    namespaceMessage.getType()));
        }

        return new AssetDirectoryServiceMessage(rawMessage, payload, namespaceMessage, dataMessage);
    }
}
