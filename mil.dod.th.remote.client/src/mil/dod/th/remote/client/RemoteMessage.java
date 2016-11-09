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

import aQute.bnd.annotation.ProviderType;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;

/**
 * Provides an unparsed remote message including the namespace and data messages.
 * 
 * @author dlandoll
 *
 * @param <T>
 *      Namespace message type, e.g. {@link mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace}
 */
@ProviderType
public interface RemoteMessage<T extends Message>
{
    /**
     * Retrieve the raw message used to generate this remote message.
     * 
     * @return
     *      original raw message
     */
    TerraHarvestMessage getRawMessage();

    /**
     * Returns the namespace type for the message.
     * 
     * @return
     *      namespace type
     */
    Namespace getNamespace();

    /**
     * Returns the namespace message.
     * 
     * @return
     *      namespace message
     */
    T getNamespaceMessage();

    /**
     * Returns the data message type enumeration.
     * 
     * @return
     *      data message type
     */
    ProtocolMessageEnum getDataMessageType();

    /**
     * Returns the data message.
     * 
     * @return
     *      data message
     */
    Message getDataMessage();

    /**
     * Returns whether the message is an error message. This might include a data message of type
     * {@link mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData} or another error message.
     * 
     * @return
     *      true if an error message, false otherwise
     */
    boolean isError();

    /**
     * Returns whether the message is a response message.
     * 
     * @return
     *      true if a response message, false otherwise
     */
    boolean isResponse();

    /**
     * Returns the source ID of the message.
     * 
     * @return
     *      source ID
     */
    int getSrcId();

    /**
     * Returns the destination ID of the message.
     * 
     * @return
     *      destination ID
     */
    int getDestId();
}
