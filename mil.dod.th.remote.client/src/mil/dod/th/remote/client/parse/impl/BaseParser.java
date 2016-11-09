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

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.MessageParser;

/**
 * Parsing implementation for {@link BaseNamespace} messages.
 * 
 * @author dlandoll
 */
public class BaseParser implements MessageParser<BaseNamespace>
{
    @Override
    public RemoteMessage<BaseNamespace> parse(final TerraHarvestMessage rawMessage, final TerraHarvestPayload payload)
            throws InvalidProtocolBufferException
    {
        final BaseNamespace namespaceMessage = BaseNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        switch (namespaceMessage.getType())
        {
            case RequestControllerInfo:
                dataMessage = null;
                break;
            case ControllerInfo:
                dataMessage = ControllerInfoData.parseFrom(namespaceMessage.getData());
                break;
            case GenericErrorResponse:
                dataMessage = GenericErrorResponseData.parseFrom(namespaceMessage.getData());
                break;
            case ShutdownSystem:
                dataMessage = null;
                break;
            case ReceivedShutdownRequest:
                dataMessage = null;
                break;
            case SetOperationModeRequest:
                dataMessage = SetOperationModeRequestData.parseFrom(namespaceMessage.getData());
                break;
            case SetOperationModeResponse:
                dataMessage = null;
                break;
            case GetOperationModeRequest:
                dataMessage = null;
                break;
            case GetOperationModeResponse:
                dataMessage = GetOperationModeReponseData.parseFrom(namespaceMessage.getData());
                break;
            case GetControllerCapabilitiesRequest:
                dataMessage = null;
                break;      
            case GetControllerCapabilitiesResponse:
                dataMessage = GetControllerCapabilitiesResponseData.parseFrom(namespaceMessage.getData());
                break; 
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for BaseNamespace.",
                        namespaceMessage.getType()));
        }

        return new BaseMessage(rawMessage, payload, namespaceMessage, dataMessage);
    }
}
