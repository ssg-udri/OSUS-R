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

import static org.mockito.Mockito.*;

import java.io.IOException;

import com.google.protobuf.ByteString;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestMessageBuilderImpl
{
    private MessageBuilderImpl m_SUT1;
    private MessageBuilderImpl m_SUT2;
    private TerraHarvestPayload m_Payload;

    @Mock
    private MessageSenderService m_MessageSenderService;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_Payload = TerraHarvestPayload.newBuilder()
            .setNamespace(Namespace.Base)
            .setNamespaceMessage(ByteString.EMPTY)
            .build();

        m_SUT1 = new MessageBuilderImpl(false, m_MessageSenderService)
        {
            @Override
            public TerraHarvestPayload build()
            {
                return m_Payload;
            }
        };

        m_SUT2 = new MessageBuilderImpl(true, m_MessageSenderService)
        {
            @Override
            public TerraHarvestPayload build()
            {
                return m_Payload;
            }
        };
    }

    /**
     * Verify that messages are sent through {@link MessageSenderService}.
     */
    @Test
    public void testSend() throws IOException
    {
        m_SUT1.send(2);
        verify(m_MessageSenderService).sendRequest(2, m_Payload);

        m_SUT2.send(4);
        verify(m_MessageSenderService).sendResponse(4, m_Payload);
    }
}
