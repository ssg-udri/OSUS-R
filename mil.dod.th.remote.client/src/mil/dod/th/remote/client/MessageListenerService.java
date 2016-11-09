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

import java.io.InputStream;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;

/**
 * The message listener service handles the reception of messages received via {@link InputStream}s and performs initial
 * processing of the message to create a {@link RemoteMessage} (which is then forwarded to an associated callback, if
 * one is registered).
 * <p>
 * Listens for new messages using an {@link InputStream} provided by the user. When
 * {@link #addRemoteChannel(int, InputStream, ChannelStateCallback)} is called, a background thread is created to wait
 * for messages to be received. New messages are parsed/received as a
 * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} and converted to a {@link RemoteMessage}. Message
 * handler callbacks are issued when a response is received.
 * <p>
 * Handling of closed or disconnected input streams is up to users of this service. When the input stream throws an
 * exception the background receiving thread stops executing, a call is made to
 * {@link ChannelStateCallback#onChannelRemoved(int, Exception)} and the user must add another input stream using
 * {@link #addRemoteChannel(int, InputStream, ChannelStateCallback)}.
 * <p>
 * This is an OSGi service and may be obtained by getting an OSGi service reference or using declarative services.
 * 
 * @author dlandoll
 */
@ProviderType
public interface MessageListenerService
{
    /**
     * Add a remote channel input stream to listen for messages on. A background thread is created to wait for messages
     * from the input stream.
     * 
     * @param srcId
     *      ID of the source system
     * @param input
     *      input stream to read messages from
     * @param callback
     *      used for notification of channel state updates, can be null if no callbacks are desired
     * @throws IllegalArgumentException
     *      if input stream is null
     * @throws IllegalStateException
     *      if a channel already exists for the given srcId
     */
    void addRemoteChannel(int srcId, InputStream input, ChannelStateCallback callback) throws IllegalArgumentException,
            IllegalStateException;

    /**
     * Remove a remote channel and stop its background receiving thread.
     * 
     * @param srcId
     *      ID of the source system
     * @throws IllegalArgumentException
     *      if the input stream is not managed by this service
     */
    void removeRemoteChannel(int srcId) throws IllegalArgumentException;

    /**
     * Registers message handlers that are called when messages from any namespace are received.
     * 
     * @param callback
     *      callback implementation
     * @throws IllegalArgumentException
     *      if callback is null
     */
    void registerCallback(MessageListenerCallback<?> callback) throws IllegalArgumentException;

    /**
     * Registers message handlers that are called when a specific namespace type is received.
     * 
     * @param type
     *      namespace type handled by the callback
     * @param callback
     *      callback implementation
     * @throws IllegalArgumentException
     *      if namespace type is not supported or callback is null
     */
    void registerCallback(Namespace type, MessageListenerCallback<?> callback) throws IllegalArgumentException;

    /**
     * Unregisters the given message handler from all namespace types.
     * 
     * @param callback
     *      callback reference
     * @throws IllegalArgumentException
     *      if callback is null
     */
    void unregisterCallback(MessageListenerCallback<?> callback) throws IllegalArgumentException;
}
