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
package mil.dod.th.remote.client.parse;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;

/**
 * Message parser used by {@link mil.dod.th.remote.client.MessageListenerService} to create remote messages from raw
 * received messages. Each namespace from the remote interface has its own parser implementation.
 * 
 * @author dlandoll
 * 
 * @param <T>
 *      Namespace message type, e.g. {@link mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace}
 */
public interface MessageParser<T extends Message>
{
    /**
     * Generate a {@link RemoteMessage} from the raw message.
     * 
     * @param rawMessage
     *      raw remote interface message
     * @param payload
     *      payload message contained in the raw message
     * @return
     *      remote message for the namespace message type
     * @throws InvalidProtocolBufferException
     *      if there is an error parsing the protocol buffer message
     */
    RemoteMessage<T> parse(TerraHarvestMessage rawMessage, TerraHarvestPayload payload)
            throws InvalidProtocolBufferException;
}
