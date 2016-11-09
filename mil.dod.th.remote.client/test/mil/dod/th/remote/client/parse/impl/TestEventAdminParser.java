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

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

import org.junit.Test;

public class TestEventAdminParser
{
    private EventAdminParser m_SUT;

    @Test
    public void testParse() throws InvalidProtocolBufferException
    {
        m_SUT = new EventAdminParser();

        for(EventAdminNamespace.EventAdminMessageType type : EventAdminNamespace.EventAdminMessageType.values())
        {
            verifyMessageType(type);
        }
    }

    private void verifyMessageType(EventAdminNamespace.EventAdminMessageType type) throws InvalidProtocolBufferException
    {
        ByteString dataMessage;
        switch (type)
        {
            case SendEvent:
                dataMessage = SendEventData.newBuilder().setTopic("test").build().toByteString();
                break;
            case EventRegistrationRequest:
                dataMessage = EventRegistrationRequestData.newBuilder()
                    .setCanQueueEvent(false).setExpirationTimeHours(5).setObjectFormat(LexiconFormat.Enum.NATIVE)
                    .addTopic("test").build().toByteString();
                break;
            case EventRegistrationResponse:
                dataMessage = EventRegistrationResponseData.newBuilder().setId(2).build().toByteString();
                break;
            case UnregisterEventRequest:
                dataMessage = UnregisterEventRequestData.newBuilder().setId(2).build().toByteString();
                break;
            case UnregisterEventResponse:
                dataMessage = ByteString.EMPTY;
                break;
            case CleanupRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case CleanupResponse:
                dataMessage = ByteString.EMPTY;
                break;
            default:
                throw new UnsupportedOperationException("Unknown message type " + type.name());
        }

        EventAdminNamespace namespaceMessage = EventAdminNamespace.newBuilder()
                .setData(dataMessage)
                .setType(type)
                .build();

        TerraHarvestPayload payload = MessageUtils.createPayload(Namespace.EventAdmin, namespaceMessage);
        TerraHarvestMessage thmessage = MessageUtils.createMessage(payload);

        // replay
        RemoteMessage<EventAdminNamespace> remoteMessage = m_SUT.parse(thmessage, payload);

        // verify
        MessageUtils.verifyRemoteMessage(remoteMessage, Namespace.EventAdmin, namespaceMessage, type, dataMessage);
    }
}
