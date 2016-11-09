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

import com.google.protobuf.UninitializedMessageException;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.EventRegistrationBuilder;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

/**
 * Implementation of the {@link EventRegistrationBuilder}.
 * 
 * @author dlandoll
 */
public class EventRegistrationBuilderImpl extends MessageBuilderImpl implements EventRegistrationBuilder
{
    private final EventRegistrationRequestData.Builder m_RequestData;

    /**
     * Constructor for the {@link EventRegistrationBuilder} implementation.
     * 
     * @param messageSenderService
     *      Reference to the message sender
     */
    public EventRegistrationBuilderImpl(final MessageSenderService messageSenderService)
    {
        // Request message
        super(false, messageSenderService);

        m_RequestData = EventRegistrationRequestData.newBuilder();
    }

    @Override
    public TerraHarvestPayload build() throws UninitializedMessageException
    {
        final EventAdminNamespace namespaceMessage = EventAdminNamespace.newBuilder()
            .setType(EventAdminNamespace.EventAdminMessageType.EventRegistrationRequest)
            .setData(m_RequestData.build().toByteString())
            .build();
        return createPayload(Namespace.EventAdmin, namespaceMessage);
    }

    @Override
    public EventRegistrationBuilder setTopics(final String... topics)
    {
        for (String topic : topics)
        {
            m_RequestData.addTopic(topic);
        }

        return this;
    }

    @Override
    public EventRegistrationBuilder setFilter(final String filter)
    {
        m_RequestData.setFilter(filter);
        return this;
    }

    @Override
    public EventRegistrationBuilder setExpirationTimeHours(final int expirationTimeHours)
    {
        m_RequestData.setExpirationTimeHours(expirationTimeHours);
        return this;
    }

    @Override
    public EventRegistrationBuilder setCanQueueEvent(final boolean canQueueEvent)
    {
        m_RequestData.setCanQueueEvent(canQueueEvent);
        return this;
    }

    @Override
    public EventRegistrationBuilder setObjectFormat(final LexiconFormat.Enum objectFormat)
    {
        m_RequestData.setObjectFormat(objectFormat);
        return this;
    }
}
