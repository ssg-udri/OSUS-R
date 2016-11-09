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
import static org.junit.Assert.fail;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UninitializedMessageException;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestEventUnregistrationBuilderImpl
{
    private EventUnregistrationBuilderImpl m_SUT;

    @Mock
    private MessageSenderService m_MessageSenderService;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new EventUnregistrationBuilderImpl(m_MessageSenderService);
    }

    /**
     * Verify that a valid payload message can be generated.
     */
    @Test
    public void testBuild() throws UninitializedMessageException, InvalidProtocolBufferException
    {
        // replay
        TerraHarvestPayload payload = m_SUT.setId(100).build();

        // verify
        EventAdminNamespace namespaceMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        UnregisterEventRequestData data = UnregisterEventRequestData.parseFrom(namespaceMessage.getData());
        assertThat(payload.getNamespace(), is(Namespace.EventAdmin));
        assertThat(data.getId(), is(100));
    }

    /**
     * Verify that exception is thrown if {@link EventUnregistrationBuilderImpl#setId(int)} is not called.
     */
    @Test
    public void testBuildNoId()
    {
        try
        {
            m_SUT.build();
            fail("build should throw UninitializedMessageException");
        }
        catch (UninitializedMessageException e)
        {
            // Expected
        }
    }
}
