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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.EventAdminMessageGenerator;
import mil.dod.th.remote.client.generate.EventRegistrationBuilder;
import mil.dod.th.remote.client.generate.EventUnregistrationBuilder;
import mil.dod.th.remote.client.generate.MessageBuilder;

/**
 * Implementation of the {@link EventAdminMessageGenerator}.
 * 
 * @author dlandoll
 */
@Component
public class EventAdminMessageGeneratorImpl implements EventAdminMessageGenerator
{
    private MessageSenderService m_MessageSenderService;

    @Reference
    public void setMessageSenderService(final MessageSenderService messageSenderService)
    {
        m_MessageSenderService = messageSenderService;
    }

    @Override
    public EventRegistrationBuilder createEventRegRequest()
    {
        return new EventRegistrationBuilderImpl(m_MessageSenderService);
    }

    @Override
    public EventUnregistrationBuilder createEventUnRegRequest()
    {
        return new EventUnregistrationBuilderImpl(m_MessageSenderService);
    }

    @Override
    public MessageBuilder createEventCleanupRequest()
    {
        return new MessageBuilderImpl(false, m_MessageSenderService)
        {
            @Override
            public TerraHarvestPayload build()
            {
                final EventAdminNamespace namespaceMessage = EventAdminNamespace.newBuilder()
                    .setType(EventAdminNamespace.EventAdminMessageType.CleanupRequest)
                    .build();

                return createPayload(Namespace.EventAdmin, namespaceMessage);
            }
        };
    }
}
