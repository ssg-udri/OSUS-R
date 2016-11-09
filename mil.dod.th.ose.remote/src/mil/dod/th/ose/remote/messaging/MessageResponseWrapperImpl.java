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
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Implementation of the {@link MessageResponseWrapperImpl}.
 * @author allenchl
 *
 */
public class MessageResponseWrapperImpl implements MessageResponseWrapper
{
    /**
     * Service used to send {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}s.
     */
    private final MessageSender m_MessageSender;
    
    /**
     * The original request message.
     */
    private final TerraHarvestMessage m_RequestMessage;
    
    /**
     * The message information that will be stored and manipulated.
     */
    private final TerraHarvestPayload m_Payload;

    /**
     * Constructor that accepts the original request, the response {@link TerraHarvestPayload}, and the 
     * service used to send messages.
     * @param request
     *     the original request message.
     * @param payload
     *     the message information
     * @param messageSender
     *     the service used to send messages
     */
    public MessageResponseWrapperImpl(final TerraHarvestMessage request,
        final TerraHarvestPayload payload, final MessageSender messageSender)
    {
        m_RequestMessage = request;
        m_Payload = payload;
        m_MessageSender = messageSender;
    }

    @Override
    public boolean queue(final RemoteChannel channel)
    {
        return m_MessageSender.queueMessageResponse(m_RequestMessage, m_Payload, channel);
    }
}
