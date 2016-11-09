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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage.Version;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Test class for the {@link MessageResponseWrapperImpl}.
 * @author allenchl
 *
 */
public class TestMessageResponseWrapperImpl
{
    private MessageResponseWrapperImpl m_SUT;
    private MessageSender m_MessageSender;
    private TerraHarvestPayload m_Payload;
    private TerraHarvestMessage m_RequestMessage;
    
    //system IDs
    private int m_Local = 3;
    private int m_Remote = 3;
    
    @Before
    public void setup()
    {
        m_MessageSender = mock(MessageSender.class);
    }
    
    /**
     * Verify that correct message information is forwarded to the message sender when 'queueResponse'
     * is called.
     */
    @Test
    public void testQueueResponse()
    {
        //mock channel
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //messages
        m_RequestMessage = createTerraHarvestRequest();
        m_Payload = TestMessageWrapperImpl.createPayload();
        
        //create response wrapper
        m_SUT = new MessageResponseWrapperImpl(m_RequestMessage, m_Payload, m_MessageSender);
        
        //set sender behavior
        when(m_MessageSender.queueMessageResponse(m_RequestMessage, m_Payload, channel)).thenReturn(true);
        
        boolean result = m_SUT.queue(channel);
        
        //verify interactions
        verify(m_MessageSender).queueMessageResponse(m_RequestMessage, m_Payload, channel);
        assertThat(result, is(true));
    }
    
    /**
     * Create a {@link TerraHarvestMessage} to be the request message.
     */
    private TerraHarvestMessage createTerraHarvestRequest()
    {
        BaseNamespace namespaceMessage = BaseNamespace.newBuilder().setType(
                BaseMessageType.RequestControllerInfo).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(namespaceMessage.toByteString()).build();
        return TerraHarvestMessage.newBuilder().
            setDestId(m_Local).
            setEncryptType(EncryptType.NONE).
            setSourceId(m_Remote).
            setTerraHarvestPayload(payload.toByteString()).
            setVersion(Version.newBuilder().setMajor(1).setMinor(2).build()).
            setMessageId(2).build();
    }
}
