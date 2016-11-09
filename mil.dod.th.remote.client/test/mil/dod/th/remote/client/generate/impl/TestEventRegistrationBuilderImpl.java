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
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestEventRegistrationBuilderImpl
{
    private EventRegistrationBuilderImpl m_SUT;

    @Mock
    private MessageSenderService m_MessageSenderService;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new EventRegistrationBuilderImpl(m_MessageSenderService);
    }

    /**
     * Verify that a valid payload message can be generated.
     */
    @Test
    public void testBuild() throws InvalidProtocolBufferException
    {
        // replay
        TerraHarvestPayload payload = m_SUT.setCanQueueEvent(false)
            .setExpirationTimeHours(200)
            .setTopics("testtopic1", "testtopic2")
            .setObjectFormat(LexiconFormat.Enum.XML)
            .build();

        // verify
        EventAdminNamespace namespaceMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        EventRegistrationRequestData data = EventRegistrationRequestData.parseFrom(namespaceMessage.getData());
        assertThat(payload.getNamespace(), is(Namespace.EventAdmin));
        assertThat(data.getCanQueueEvent(), is(false));
        assertThat(data.getExpirationTimeHours(), is(200));
        assertThat(data.getTopic(0), is("testtopic1"));
        assertThat(data.getTopic(1), is("testtopic2"));
        assertThat(data.getObjectFormat(), is(LexiconFormat.Enum.XML));
    }

    /**
     * Verify that the default lexicon format is used when its not set.
     */
    @Test
    public void testBuildWithDefaultFormat() throws InvalidProtocolBufferException
    {
        // replay
        TerraHarvestPayload payload = m_SUT.setCanQueueEvent(false)
            .setExpirationTimeHours(200)
            .setTopics("testtopic1", "testtopic2")
            .build();

        // verify
        EventAdminNamespace namespaceMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        EventRegistrationRequestData data = EventRegistrationRequestData.parseFrom(namespaceMessage.getData());
        assertThat(payload.getNamespace(), is(Namespace.EventAdmin));
        assertThat(data.getCanQueueEvent(), is(false));
        assertThat(data.getExpirationTimeHours(), is(200));
        assertThat(data.getTopic(0), is("testtopic1"));
        assertThat(data.getTopic(1), is("testtopic2"));
        assertThat(data.getObjectFormat(), is(LexiconFormat.Enum.NATIVE));
    }

    /**
     * Verify that a valid payload message can be generated with filter field included.
     */
    @Test
    public void testBuildWithFilter() throws InvalidProtocolBufferException
    {
        // replay
        TerraHarvestPayload payload = m_SUT.setCanQueueEvent(false)
            .setExpirationTimeHours(200)
            .setTopics("testtopic1", "testtopic2")
            .setFilter("filter")
            .build();

        // verify
        EventAdminNamespace namespaceMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        EventRegistrationRequestData data = EventRegistrationRequestData.parseFrom(namespaceMessage.getData());
        assertThat(payload.getNamespace(), is(Namespace.EventAdmin));
        assertThat(data.getCanQueueEvent(), is(false));
        assertThat(data.getExpirationTimeHours(), is(200));
        assertThat(data.getTopic(0), is("testtopic1"));
        assertThat(data.getTopic(1), is("testtopic2"));
        assertThat(data.getFilter(), is("filter"));
    }

    /**
     * Verify that exception is thrown if {@link EventRegistrationBuilderImpl#setCanQueueEvent(boolean)} is not called.
     */
    @Test
    public void testBuildNoCanQueueEvent()
    {
        m_SUT.setExpirationTimeHours(200).setTopics("testtopic1", "testtopic2");

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

    /**
     * Verify that exception is thrown if {@link EventRegistrationBuilderImpl#setExpirationTimeHours(int)} is not
     * called.
     */
    @Test
    public void testBuildNoExpTimeHours()
    {
        m_SUT.setCanQueueEvent(false).setTopics("testtopic1", "testtopic2");

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
