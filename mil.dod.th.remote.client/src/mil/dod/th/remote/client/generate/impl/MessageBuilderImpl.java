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
package mil.dod.th.remote.client.generate.impl;

import java.io.IOException;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.MessageBuilder;

/**
 * Implementation of the base {@link MessageBuilder}.
 * 
 * @author dlandoll
 */
public abstract class MessageBuilderImpl implements MessageBuilder
{
    private final MessageSenderService m_MessageSenderService;
    private final boolean m_IsResponse;

    /**
     * Constructor for the base {@link MessageBuilder} implementation.
     * 
     * @param isResponse
     *      indicates whether building a response message or not
     * @param messageSenderService
     *      reference to service used for sending
     */
    protected MessageBuilderImpl(final boolean isResponse, final MessageSenderService messageSenderService)
    {
        m_IsResponse = isResponse;
        m_MessageSenderService = messageSenderService;
    }

    @Override
    public void send(final int destId) throws IllegalArgumentException, IllegalStateException, IOException
    {
        final TerraHarvestPayload payload = build();
        if (isResponse())
        {
            m_MessageSenderService.sendResponse(destId, payload);
        }
        else
        {
            m_MessageSenderService.sendRequest(destId, payload);
        }
    }

    /**
     * Returns whether the {@link MessageBuilder} creates a response message or not.
     * 
     * @return
     *      true if response message, false otherwise
     */
    protected boolean isResponse()
    {
        return m_IsResponse;
    }

    /**
     * Create a {@link TerraHarvestPayload} instance.
     * 
     * @param namespace
     *     the namespace type
     * @param namespaceMessage
     *     the namespace defined message
     * @return
     *     the completed payload
     */
    protected TerraHarvestPayload createPayload(final Namespace namespace, final Message namespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
            setNamespace(namespace).
            setNamespaceMessage(namespaceMessage.toByteString()).build();
    }
}
