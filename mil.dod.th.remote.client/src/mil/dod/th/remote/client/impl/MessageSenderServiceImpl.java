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
package mil.dod.th.remote.client.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Component;

import com.google.common.base.Preconditions;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.ChannelStateCallback;
import mil.dod.th.remote.client.MessageSenderService;

/**
 * Implementation of the {@link MessageSenderService}.
 * 
 * @author dlandoll
 */
@Component
public class MessageSenderServiceImpl implements MessageSenderService
{
    private static final String CHANNEL_MISSING_ERROR_MSG = "Channel for destination ID does not exist";

    private Integer m_MessageId = 0;
    private Integer m_ClientId;
    private Map<Integer, RemoteChannelInfo> m_ChannelMap = new HashMap<>();

    @Override
    public synchronized void setClientId(final int clientId)
    {
        m_ClientId = clientId;
    }

    @Override
    public synchronized void addRemoteChannel(final int destId, final OutputStream output,
        final ChannelStateCallback callback) throws IllegalArgumentException, IllegalStateException
    {
        Preconditions.checkArgument(output != null, "Channel output stream argument is required");
        Preconditions.checkState(!m_ChannelMap.containsKey(destId), "Channel already exists for destination ID",
            destId);

        m_ChannelMap.put(destId, new RemoteChannelInfo(destId, output, callback));
    }

    @Override
    public synchronized void removeRemoteChannel(final int destId) throws IllegalArgumentException
    {
        final RemoteChannelInfo channelInfo = m_ChannelMap.get(destId);
        Preconditions.checkArgument(channelInfo != null, CHANNEL_MISSING_ERROR_MSG, destId);

        m_ChannelMap.remove(destId);

        final ChannelStateCallback callback = channelInfo.getCallback();
        if (callback != null)
        {
            callback.onChannelRemoved(destId);
        }
    }

    @Override
    public synchronized void sendRequest(final int destId, final TerraHarvestPayload payload)
            throws IllegalArgumentException, IllegalStateException, IOException
    {
        sendMessage(destId, payload, false);
    }

    @Override
    public synchronized void sendResponse(final int destId, final TerraHarvestPayload payload)
            throws IllegalArgumentException, IllegalStateException, IOException
    {
        sendMessage(destId, payload, true);
    }

    /**
     * Helper method used to send messages to the given destination ID.
     * 
     * @param destId
     *      ID of the destination system
     * @param payload
     *      payload message created by a message generator
     * @param isResponse
     *      flag indicating whether message is a response or not
     * @throws IOException
     *      if there is an I/O error on the associated output stream
     */
    private void sendMessage(final int destId, final TerraHarvestPayload payload, final boolean isResponse)
            throws IOException
    {
        Preconditions.checkState(m_ClientId != null, "Client ID is not set");

        final RemoteChannelInfo channelInfo = m_ChannelMap.get(destId);

        Preconditions.checkArgument(channelInfo != null, CHANNEL_MISSING_ERROR_MSG, destId);

        final TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder()
            .setSourceId(m_ClientId)
            .setDestId(destId)
            .setEncryptType(EncryptType.NONE)
            .setIsResponse(isResponse)
            .setMessageId(m_MessageId++)
            .setVersion(RemoteConstants.SPEC_VERSION)
            .setTerraHarvestPayload(payload.toByteString())
            .build();

        try
        {
            thMessage.writeDelimitedTo(channelInfo.getOutStream());
        }
        catch (final IOException ex)
        {
            final ChannelStateCallback callback = channelInfo.getCallback();
            if (callback != null)
            {
                callback.onChannelRemoved(destId, ex);
            }

            throw ex;
        }
    }
}
