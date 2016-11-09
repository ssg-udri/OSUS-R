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
package mil.dod.th.ose.remote.messaging;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Implementation of {@link MessageWrapper}.
 * @author allenchl
 *
 */
public class MessageWrapperImpl implements MessageWrapper
{
    /**
     * Service used to send {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}s.
     */
    private final MessageSender m_MessageSender;
    
    /**
     * The message information that will be stored and manipulated.
     */
    private final TerraHarvestPayload m_Payload;

    /**
     * Constructor that accepts the {@link TerraHarvestPayload} to send, and the 
     * service used to send messages.
     * @param payload
     *     the message information
     * @param messageSender
     *     the service used to send messages
     */
    public MessageWrapperImpl(final TerraHarvestPayload payload, final MessageSender messageSender)
    {
        m_Payload = payload;
        m_MessageSender = messageSender;
    }
    
    @Override
    public boolean trySend(final int destId, final EncryptType encryptType) throws IllegalArgumentException
    {
        return m_MessageSender.trySendMessage(destId, m_Payload, encryptType);
    }

    @Override
    public boolean trySend(final int destId) throws IllegalArgumentException, IllegalStateException
    {
        return m_MessageSender.trySendMessage(destId, m_Payload);
    }

    @Override
    public boolean queue(final int destId, final EncryptType encryptType, final ResponseHandler handler) 
            throws IllegalArgumentException
    {
        return m_MessageSender.queueMessage(destId, m_Payload, encryptType, handler);
    }

    @Override
    public boolean queue(final int destId, final ResponseHandler handler) throws IllegalArgumentException,
            IllegalStateException
    {
        return m_MessageSender.queueMessage(destId, m_Payload, handler);
    }

    @Override
    public boolean queue(final RemoteChannel channel, final ResponseHandler handler)
    {
        return m_MessageSender.queueMessage(channel, m_Payload, handler);
    }

    @Override
    public boolean queue(final RemoteChannel channel, final EncryptType encryptType, final ResponseHandler handler)
    {
        return m_MessageSender.queueMessage(channel, m_Payload, encryptType, handler);
    }
}
