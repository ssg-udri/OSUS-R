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

import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.MessageParser;

/**
 * Parsing implementation for {@link DataStreamServiceNamespace} messages.
 * 
 * @author jmiller
 *
 */
public class DataStreamServiceParser implements MessageParser<DataStreamServiceNamespace>
{
    @Override
    public RemoteMessage<DataStreamServiceNamespace> parse(final TerraHarvestMessage rawMessage,
        final TerraHarvestPayload payload) throws InvalidProtocolBufferException
    {
        final DataStreamServiceNamespace namespaceMessage = 
            DataStreamServiceNamespace.parseFrom(payload.getNamespaceMessage());
        
        final Message dataMessage;
        switch (namespaceMessage.getType())
        {
            case GetStreamProfilesRequest:
                dataMessage = null;
                break;
            case GetStreamProfilesResponse:
                dataMessage = GetStreamProfilesResponseData.parseFrom(namespaceMessage.getData());
                break;
            case GetCapabilitiesRequest:
                dataMessage = GetCapabilitiesRequestData.parseFrom(namespaceMessage.getData());
                break;      
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.parseFrom(namespaceMessage.getData());
                break;
            case EnableStreamProfileRequest:
                dataMessage = null;
                break;
            case EnableStreamProfileResponse:
                dataMessage = null;
                break;
            case DisableStreamProfileRequest:
                dataMessage = null;
                break;
            case DisableStreamProfileResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                    String.format("Message type: %s is not a supported type for DataStreamServiceNamespace.",
                    namespaceMessage.getType()));
        }
        
        return new DataStreamServiceMessage(rawMessage, payload, namespaceMessage, dataMessage);

    }

}
