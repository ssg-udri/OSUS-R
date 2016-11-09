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
package mil.dod.th.remote.client;

import aQute.bnd.annotation.ConsumerType;

import com.google.protobuf.Message;

/**
 * Message callback interface used for notification of newly received messages. Instances of this interface are
 * registered with the {@link MessageListenerService} and called when a message is received for the associated
 * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace} type.
 * <p>
 * Possible implementors of this interface include {@link mil.dod.th.remote.client.parse.MessageParser} components or
 * custom, user provided components that handle messages directly.
 *
 * @author dlandoll
 *
 * @param <T>
 *      Namespace message type, e.g. {@link mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace}
 */
@ConsumerType
public interface MessageListenerCallback<T extends Message>
{
    /**
     * Message handling function called for a specific message namespace type when a new message is received.
     * 
     * @param message
     *      newly received remote message
     */
    void handleMessage(RemoteMessage<T> message);
}
