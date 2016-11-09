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
package mil.dod.th.remote.client.generate;

import java.io.IOException;

import aQute.bnd.annotation.ProviderType;

import com.google.protobuf.UninitializedMessageException;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Provides all common actions performed by a message builder. Instances of this are returned by message generator
 * implementations like {@link BaseMessageGenerator} when creating a new message.
 * 
 * @author dlandoll
 */
@ProviderType
public interface MessageBuilder
{
    /**
     * Create the message. The resulting payload can be sent using
     * {@link mil.dod.th.remote.client.MessageSenderService#sendRequest} or
     * {@link mil.dod.th.remote.client.MessageSenderService#sendResponse}.
     * 
     * @return
     *      payload message
     * @throws UninitializedMessageException
     *      if unable to build payload message
     */
    TerraHarvestPayload build() throws UninitializedMessageException;

    /**
     * Send the message, using {@link mil.dod.th.remote.client.MessageSenderService}, to the system given by the
     * destination ID.
     * 
     * @param destId
     *      ID of the destination system
     * @throws IllegalArgumentException
     *      if destId does not have an output stream/channel
     * @throws IllegalStateException
     *      if client ID has not been set on {@link mil.dod.th.remote.client.MessageSenderService}
     * @throws IOException
     *      if there is a message builder error or an I/O error on the associated output stream
     * 
     * @see mil.dod.th.remote.client.MessageSenderService#sendRequest
     * @see mil.dod.th.remote.client.MessageSenderService#sendResponse
     */
    void send(int destId) throws IllegalArgumentException, IllegalStateException, IOException;
}
