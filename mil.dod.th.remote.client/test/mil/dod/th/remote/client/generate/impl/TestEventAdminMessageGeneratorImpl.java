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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.MessageBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestEventAdminMessageGeneratorImpl
{
    private EventAdminMessageGeneratorImpl m_SUT;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new EventAdminMessageGeneratorImpl();
    }

    @Test
    public void testCreateEventRegRequest()
    {
        MessageBuilder builder = m_SUT.createEventRegRequest();
        assertThat(builder, notNullValue());
    }

    @Test
    public void testCreateEventUnRegRequest()
    {
        MessageBuilder builder = m_SUT.createEventUnRegRequest();
        assertThat(builder, notNullValue());
    }

    @Test
    public void testCreateEventCleanupRequest() throws IOException
    {
        MessageSenderService messageSenderService = mock(MessageSenderService.class);
        m_SUT.setMessageSenderService(messageSenderService);

        MessageBuilder builder = m_SUT.createEventCleanupRequest();
        assertThat(builder, notNullValue());
        assertThat(builder.build(), notNullValue());

        builder.send(1);
        verify(messageSenderService).sendRequest(eq(1), Mockito.any(TerraHarvestPayload.class));
    }
}
