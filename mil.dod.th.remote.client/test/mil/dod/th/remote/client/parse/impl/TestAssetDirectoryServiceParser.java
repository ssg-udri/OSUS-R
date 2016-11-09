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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetScannableAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen.AssetCapabilities;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;

import org.junit.Test;

public class TestAssetDirectoryServiceParser
{
    private AssetDirectoryServiceParser m_SUT;

    @Test
    public void testParse() throws InvalidProtocolBufferException
    {
        m_SUT = new AssetDirectoryServiceParser();

        for (AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType type :
            AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType.values())
        {
            verifyMessageType(type);
        }
    }

    private void verifyMessageType(AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType type)
        throws InvalidProtocolBufferException
    {
        ByteString dataMessage;
        switch (type)
        {
            case GetAssetTypesRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case GetAssetTypesResponse:
                dataMessage = GetAssetTypesResponseData.newBuilder()
                    .addProductName("name").addProductType("type").build().toByteString();
                break;
            case ScanForNewAssetsRequest:
                dataMessage = ScanForNewAssetsRequestData.newBuilder()
                    .setProductType("type").build().toByteString();
                break;
            case ScanForNewAssetsResponse:
                dataMessage = ByteString.EMPTY;
                break;
            case CreateAssetRequest:
                dataMessage = CreateAssetRequestData.newBuilder()
                    .setName("name").setProductType("type").build().toByteString();
                break;
            case CreateAssetResponse:
                dataMessage = CreateAssetResponseData.newBuilder()
                    .setInfo(FactoryObjectInfo.newBuilder().setPid("pid").setProductType("type")
                        .setUuid(UUID.newBuilder().setMostSignificantBits(0).setLeastSignificantBits(0)))
                    .build().toByteString();
                break;
            case GetAssetsRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case GetAssetsResponse:
                dataMessage = GetAssetsResponseData.newBuilder()
                    .addAssetInfo(FactoryObjectInfo.newBuilder().setPid("pid").setProductType("type")
                        .setUuid(UUID.newBuilder().setMostSignificantBits(0).setLeastSignificantBits(0)))
                    .build().toByteString();
                break;
            case GetScannableAssetTypesRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case GetScannableAssetTypesResponse:
                dataMessage = GetScannableAssetTypesResponseData.newBuilder()
                    .addScannableAssetType("type").build().toByteString();
                break;
            case GetCapabilitiesRequest:
                dataMessage = GetCapabilitiesRequestData.newBuilder().setProductType("type").build().toByteString();
                break;      
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.newBuilder()
                    .setProductType("type")
                    .setCapabilities(AssetCapabilities.newBuilder()
                            .setBase(BaseCapabilities.newBuilder()
                                    .setProductName("product")
                                    .setDescription("description")
                                    .setManufacturer("company").build())
                    .setMaxFov(1.1))
                .build().toByteString();
                break;
            default:
                throw new UnsupportedOperationException("Unknown message type " + type.name());
        }

        AssetDirectoryServiceNamespace namespaceMessage = AssetDirectoryServiceNamespace.newBuilder()
                .setType(type)
                .setData(dataMessage)
                .build();

        TerraHarvestPayload payload = MessageUtils.createPayload(Namespace.AssetDirectoryService, namespaceMessage);
        TerraHarvestMessage thmessage = MessageUtils.createMessage(payload);

        // replay
        RemoteMessage<AssetDirectoryServiceNamespace> remoteMessage = m_SUT.parse(thmessage, payload);

        // verify
        MessageUtils.verifyRemoteMessage(remoteMessage, Namespace.AssetDirectoryService, namespaceMessage, type,
            dataMessage);
    }
}
