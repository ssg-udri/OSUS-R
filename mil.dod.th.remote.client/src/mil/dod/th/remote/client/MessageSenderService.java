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

import java.io.IOException;
import java.io.OutputStream;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * The message sender service handles the generation and sending of
 * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}s using a provided {@link OutputStream} and
 * message parameters found in {@link #sendRequest} or {@link #sendResponse}.
 * <p>
 * Handling of closed or disconnected output streams is up to users of this service. If an output stream becomes
 * invalid, it must be replaced by calling {@link #addRemoteChannel(int, OutputStream, ChannelStateCallback)}. It can
 * also be called at anytime to replace the output stream for a destination ID. Notification of changes to the remote
 * channel state will be made to the {@link ChannelStateCallback} provided.
 * <p>
 * This is an OSGi service and may be obtained by getting an OSGi service reference or using declarative services.
 * 
 * @author dlandoll
 */
@ProviderType
public interface MessageSenderService
{
    /**
     * Set the client ID for the system sending messages through this service. This must be called before any messages
     * can be sent and should be a unique value where no clients or controllers on a network share the same system ID.
     * 
     * @param clientId
     *      unique client system ID
     */
    void setClientId(int clientId);

    /**
     * Add a remote channel output stream to to send messages on.
     * 
     * @param destId
     *      ID of the destination system
     * @param output
     *      output stream to write messages to
     * @param callback
     *      used for notification of channel state updates, can be null if no callbacks are desired
     * @throws IllegalArgumentException
     *      if output stream is null
     * @throws IllegalStateException
     *      if a channel already exists for the given destId
     */
    void addRemoteChannel(int destId, OutputStream output, ChannelStateCallback callback)
            throws IllegalArgumentException, IllegalStateException;

    /**
     * Remove the remote channel.
     * 
     * @param destId
     *      ID of the destination system
     * @throws IllegalArgumentException
     *      if an output stream for the destId is not managed by this service
     */
    void removeRemoteChannel(int destId) throws IllegalArgumentException;

    /**
     * Send the given payload message, as a request, to the system given by destination ID. Only one attempt will be
     * made to send the message. If sending fails, it is up to the user to try sending again.
     * <p>
     * The encryption types specified in the THOSE remote interface are not supported by this service, so the encryption
     * type in all messages is set to {@link mil.dod.th.core.remote.proto.RemoteBase.EncryptType#NONE}. The remote
     * interface version in all messages is set to {@link mil.dod.th.core.remote.RemoteConstants#SPEC_VERSION} and
     * message ID's are also managed set by this service.
     * 
     * @param destId
     *      ID of the destination system
     * @param payload
     *      payload message created by a message generator
     * @throws IllegalArgumentException
     *      if destId does not have an output stream/channel
     * @throws IllegalStateException
     *      if {@link #setClientId(int)} has not been called
     * @throws IOException
     *      if there is an I/O error on the associated output stream
     */
    void sendRequest(int destId, TerraHarvestPayload payload) throws IllegalArgumentException,
            IllegalStateException, IOException;

    /**
     * Send the given payload message, as a response, to the system given by destination ID. Only one attempt will be
     * made to send the message. If sending fails, it is up to the user to try sending again.
     * <p>
     * The encryption types specified in the THOSE remote interface are not supported by this service, so the encryption
     * type in all messages is set to {@link mil.dod.th.core.remote.proto.RemoteBase.EncryptType#NONE}. The remote
     * interface version in all messages is set to {@link mil.dod.th.core.remote.RemoteConstants#SPEC_VERSION} and
     * message ID's are also managed set by this service.
     * 
     * @param destId
     *      ID of the destination system
     * @param payload
     *      payload message created by a message generator
     * @throws IllegalArgumentException
     *      if destId does not have an output stream/channel
     * @throws IllegalStateException
     *      if {@link #setClientId(int)} has not been called
     * @throws IOException
     *      if there is an I/O error on the associated output stream
     */
    void sendResponse(int destId, TerraHarvestPayload payload) throws IllegalArgumentException,
            IllegalStateException, IOException;
}
