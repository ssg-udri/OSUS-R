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
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.EventUnregistrationBuilder;

/**
 * Implementation of the {@link EventUnregistrationBuilder}.
 * 
 * @author dlandoll
 */
public class EventUnregistrationBuilderImpl extends MessageBuilderImpl implements EventUnregistrationBuilder
{
    private final UnregisterEventRequestData.Builder m_RequestData;

    /**
     * Constructor for the {@link EventUnregistrationBuilder} implementation.
     * 
     * @param messageSenderService
     *      Reference to the message sender
     */
    EventUnregistrationBuilderImpl(final MessageSenderService messageSenderService)
    {
        // Request message
        super(false, messageSenderService);

        m_RequestData = UnregisterEventRequestData.newBuilder();
    }

    @Override
    public TerraHarvestPayload build() throws UninitializedMessageException
    {
        final EventAdminNamespace namespaceMessage = EventAdminNamespace.newBuilder()
            .setType(EventAdminNamespace.EventAdminMessageType.UnregisterEventRequest)
            .setData(m_RequestData.build().toByteString())
            .build();
        return createPayload(Namespace.EventAdmin, namespaceMessage);
    }

    @Override
    public EventUnregistrationBuilder setId(final int regId)
    {
        m_RequestData.setId(regId);
        return this;
    }
}
