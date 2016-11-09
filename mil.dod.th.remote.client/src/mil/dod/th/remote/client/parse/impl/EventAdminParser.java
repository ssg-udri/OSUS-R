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

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.MessageParser;

/**
 * Parsing implementation for {@link EventAdminNamespace} messages.
 * 
 * @author dlandoll
 */
public class EventAdminParser implements MessageParser<EventAdminNamespace>
{
    @Override
    public RemoteMessage<EventAdminNamespace> parse(final TerraHarvestMessage rawMessage,
        final TerraHarvestPayload payload) throws InvalidProtocolBufferException
    {
        final EventAdminNamespace namespaceMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        switch (namespaceMessage.getType())
        {
            case SendEvent:
                dataMessage = SendEventData.parseFrom(namespaceMessage.getData());
                break;
            case EventRegistrationRequest:
                dataMessage = EventRegistrationRequestData.parseFrom(namespaceMessage.getData());
                break;
            case EventRegistrationResponse:
                dataMessage = EventRegistrationResponseData.parseFrom(namespaceMessage.getData());
                break;
            case UnregisterEventRequest:
                dataMessage = UnregisterEventRequestData.parseFrom(namespaceMessage.getData());
                break;
            case UnregisterEventResponse:
                dataMessage = null;
                break;
            case CleanupRequest:
                dataMessage = null;
                break;
            case CleanupResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for EventAdminNamespace.",
                        namespaceMessage.getType()));
        }

        return new EventAdminMessage(rawMessage, payload, namespaceMessage, dataMessage);
    }
}
