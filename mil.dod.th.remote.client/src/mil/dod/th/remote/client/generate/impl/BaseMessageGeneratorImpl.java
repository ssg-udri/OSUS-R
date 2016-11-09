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

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.BaseMessageGenerator;
import mil.dod.th.remote.client.generate.MessageBuilder;

/**
 * Implementation of the {@link BaseMessageGenerator}.
 * 
 * @author dlandoll
 */
@Component
public class BaseMessageGeneratorImpl implements BaseMessageGenerator
{
    private MessageSenderService m_MessageSenderService;

    @Reference
    public void setMessageSenderService(final MessageSenderService messageSenderService)
    {
        m_MessageSenderService = messageSenderService;
    }

    @Override
    public MessageBuilder createGetControllerInfoRequest()
    {
        return new MessageBuilderImpl(false, m_MessageSenderService)
        {
            @Override
            public TerraHarvestPayload build()
            {
                final BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
                        .setType(BaseNamespace.BaseMessageType.RequestControllerInfo)
                        .build();

                return createPayload(Namespace.Base, namespaceMessage);
            }
        };
    }
}
