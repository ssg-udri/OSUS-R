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

import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.StreamProfile;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;
import mil.dod.th.remote.lexicon.datastream.capability.StreamProfileCapabilitiesGen.StreamProfileCapabilities;

import org.junit.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author jmiller
 *
 */
public class TestDataStreamServiceParser
{
    private DataStreamServiceParser m_SUT;
    
    @Test
    public void testParse() throws InvalidProtocolBufferException
    {
        m_SUT = new DataStreamServiceParser();
        for (DataStreamServiceNamespace.DataStreamServiceMessageType type :
            DataStreamServiceNamespace.DataStreamServiceMessageType.values())
        {
            verifyMessageType(type);
        }
    }
    
    private void verifyMessageType(DataStreamServiceNamespace.DataStreamServiceMessageType type)
        throws InvalidProtocolBufferException
    {
        ByteString dataMessage;
        switch (type)
        {
            case GetStreamProfilesRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case GetStreamProfilesResponse:
                dataMessage = GetStreamProfilesResponseData.newBuilder()
                .addStreamProfile(StreamProfile.newBuilder()
                        .setBitrateKbps(100.0)
                        .setAssetUuid(UUID.newBuilder().setMostSignificantBits(1).setLeastSignificantBits(1).build())
                        .setStreamPort("rtp://226.5.6.7:12000")
                        .setIsEnabled(true)
                        .setFormat("video/mp4")
                        .setInfo(FactoryObjectInfo.newBuilder().setPid("pid").setProductType("type")
                        .setUuid(UUID.newBuilder().setMostSignificantBits(0).setLeastSignificantBits(0))).build())
                    .build().toByteString();
                break;
            case GetCapabilitiesRequest:
                dataMessage = GetCapabilitiesRequestData.newBuilder().setProductType("type").build().toByteString();
                break;      
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.newBuilder()
                    .setProductType("type")
                    .setCapabilities(StreamProfileCapabilities.newBuilder()
                            .setBase(BaseCapabilities.newBuilder()
                                    .setProductName("product")
                                    .setDescription("description")
                                    .setManufacturer("company").build())
                    .setMaxBitrateKbps(1000.0).build()).build().toByteString();
                break;
            case EnableStreamProfileRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case EnableStreamProfileResponse:
                dataMessage = ByteString.EMPTY;
                break;
            case DisableStreamProfileRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case DisableStreamProfileResponse:
                dataMessage = ByteString.EMPTY;
                break;
            default:
                throw new UnsupportedOperationException("Unknown message type " + type.name());               
        }
        
        DataStreamServiceNamespace namespaceMessage = DataStreamServiceNamespace.newBuilder()
                .setType(type)
                .setData(dataMessage).build();
        
        TerraHarvestPayload payload = MessageUtils.createPayload(Namespace.DataStreamService, namespaceMessage);
        TerraHarvestMessage thmessage = MessageUtils.createMessage(payload);
        
        //replay
        RemoteMessage<DataStreamServiceNamespace> remoteMessage = m_SUT.parse(thmessage, payload);
        
        //verify
        MessageUtils.verifyRemoteMessage(remoteMessage, Namespace.DataStreamService, namespaceMessage, type,
                dataMessage);             
    }
}
