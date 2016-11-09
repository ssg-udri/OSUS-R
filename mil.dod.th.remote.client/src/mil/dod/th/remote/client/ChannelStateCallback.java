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

/**
 * Channel state callback interface used for notification channel related events. Register this callback using
 * {@link MessageListenerService#addRemoteChannel(int, java.io.InputStream, ChannelStateCallback)} or
 * {@link MessageSenderService#addRemoteChannel(int, java.io.OutputStream, ChannelStateCallback)}.
 * 
 * @author dlandoll
 */
@ConsumerType
public interface ChannelStateCallback
{
    /**
     * Called when a channel stream is removed by the user.
     * 
     * @param channelId
     *      remote channel ID (e.g. destination system ID)
     */
    void onChannelRemoved(int channelId);

    /**
     * Called when a channel stream is automatically removed due to an error.
     * 
     * @param channelId
     *      remote channel ID (e.g. destination system ID)
     * @param exception
     *      exception that caused the channel to be removed
     */
    void onChannelRemoved(int channelId, Exception exception);
}
