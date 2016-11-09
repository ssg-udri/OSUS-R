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
package mil.dod.th.core.remote;

import aQute.bnd.annotation.ConsumerType;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Interface for handling remote message events. This interface is meant to assist with messages in which the 
 * response message requires unique handling. Examples of messages that require this type of handling would be 
 * configuration admin responses, asset responses, or generic(Base namespace) error messages.
 * @author callen
 *
 */
@ConsumerType
public interface ResponseHandler 
{
    /**
     * Response handling for a specific message.
     * @param message
     *     the message that contains the entire response to handle
     * @param payload
     *     Payload contains the namespace information and the data message information
     * @param namespaceMessage
     *     the namespace response message
     * @param dataMessage
     *     the data message of the namespace response, will be null if no data is associated with the
     *     namespace message.
     */
    void handleResponse(TerraHarvestMessage message, TerraHarvestPayload payload, Message namespaceMessage, 
        Message dataMessage);
}
