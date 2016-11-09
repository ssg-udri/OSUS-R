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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;

/**
 * Proto message utilities used for parser unit testing.
 * 
 * @author dlandoll
 */
public class MessageUtils
{
    private static int DEST_ID = 1;
    private static int SRC_ID = 2;
    private static int MSG_ID = 11;

    /**
     * Create a payload message.
     * 
     * @param namespace
     *      type of payload message
     * @param namespaceMessage
     *      message to attach to the payload
     * @return
     *      payload message
     */
    public static TerraHarvestPayload createPayload(Namespace namespace, Message namespaceMessage)
    {
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(namespace)
                .setNamespaceMessage(namespaceMessage.toByteString())
                .build();

        return payload;
    }

    /**
     * Create a final message with the given payload.
     * 
     * @param payload
     *      payload message to attach
     * @return
     *      TerraHarvest message
     */
    public static TerraHarvestMessage createMessage(TerraHarvestPayload payload)
    {
        TerraHarvestMessage thmessage = TerraHarvestMessage.newBuilder()
                .setDestId(DEST_ID)
                .setIsResponse(false)
                .setMessageId(MSG_ID)
                .setSourceId(SRC_ID)
                .setTerraHarvestPayload(payload.toByteString())
                .setVersion(RemoteConstants.SPEC_VERSION)
                .build();

        return thmessage;
    }

    /**
     * Verify the given remote message based on given parameters.
     * 
     * @param remoteMessage
     *      converted message to verify
     * @param namespace
     *      namespace type to compare with
     * @param namespaceMessage
     *      namespace message to compare with
     * @param dataMessageType
     *      data message type to compare with
     * @param dataMessage
     *      data message to compare with
     */
    public static void verifyRemoteMessage(RemoteMessage<?> remoteMessage, Namespace namespace,
        Message namespaceMessage, ProtocolMessageEnum dataMessageType, ByteString dataMessage)
    {
        assertThat(remoteMessage, notNullValue());
        assertThat(remoteMessage.getRawMessage(), notNullValue());
        assertThat(remoteMessage.getNamespace(), is(namespace));
        assertThat(remoteMessage.getNamespaceMessage(), is(namespaceMessage));
        assertThat(remoteMessage.getDataMessageType(), is(dataMessageType));

        if (remoteMessage.getDataMessage() != null)
        {
            assertThat(remoteMessage.getDataMessage().toByteString(), is(dataMessage));
        }

        assertThat(remoteMessage.getDestId(), is(DEST_ID));
        assertThat(remoteMessage.getSrcId(), is(SRC_ID));

        // Verify that these can be called without causing an exception
        remoteMessage.isError();
        remoteMessage.isResponse();
    }
}
