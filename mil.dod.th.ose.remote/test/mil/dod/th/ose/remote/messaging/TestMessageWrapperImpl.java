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
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Test class for the {@link MessageWrapperImpl}.
 * @author allenchl
 *
 */
public class TestMessageWrapperImpl
{
    private MessageWrapperImpl m_SUT;
    private MessageSender m_MessageSender;
    private TerraHarvestPayload m_Payload;
    private RemoteChannel m_Channel;
    
    //controller ID
    private int sysId = 9;
    
    @Before
    public void setup()
    {
        //message sender service
        m_MessageSender =  mock(MessageSender.class);
        m_Channel = mock(RemoteChannel.class);
        
        //create Payload to send
        m_Payload = createPayload();
        m_SUT = new MessageWrapperImpl(m_Payload, m_MessageSender);
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'trySend' is called.
     */
    @Test
    public void testTrySend()
    {
        //sender behavior
        when(m_MessageSender.trySendMessage(sysId, m_Payload, EncryptType.NONE)).thenReturn(true);
        
        //call 'trySend'
        boolean result = m_SUT.trySend(sysId, EncryptType.NONE);
        
        //verify
        verify(m_MessageSender).trySendMessage(sysId, m_Payload, EncryptType.NONE);
        assertThat(result, is(true));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'queue' is called.
     * This test is without a {@link ResponseHandler}.
     */
    @Test
    public void testQueue()
    {
        //sender behavior
        when(m_MessageSender.queueMessage(sysId, m_Payload, EncryptType.AES_ECDH_ECDSA, null)).thenReturn(false);
        
        //call 'queue'
        boolean result = m_SUT.queue(sysId, EncryptType.AES_ECDH_ECDSA, null);
        
        //verify
        verify(m_MessageSender).queueMessage(sysId, m_Payload, EncryptType.AES_ECDH_ECDSA, null);
        assertThat(result, is(false));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'queue' is called.
     * This test is with a {@link ResponseHandler}.
     */
    @Test
    public void testQueueWithHandler()
    {
        //Response handler
        ResponseHandler handler = mock(ResponseHandler.class);
        
        //sender behavior
        when(m_MessageSender.queueMessage(sysId, m_Payload, EncryptType.AES_ECDH_ECDSA, handler)).thenReturn(false);
        
        //call 'queue'
        boolean result = m_SUT.queue(sysId, EncryptType.AES_ECDH_ECDSA, handler);
        
        //verify
        verify(m_MessageSender).queueMessage(sysId, m_Payload, EncryptType.AES_ECDH_ECDSA, handler);
        assertThat(result, is(false));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'trySend' is called.
     * Verify that the encryption type not being present does not cause an error.
     */
    @Test
    public void testTrySendNoEncryptType()
    {
        //sender behavior
        when(m_MessageSender.trySendMessage(sysId, m_Payload)).thenReturn(true);
        
        //call 'trySend'
        boolean result = m_SUT.trySend(sysId);
        
        //verify
        verify(m_MessageSender).trySendMessage(sysId, m_Payload);
        assertThat(result, is(true));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'queue' is called.
     * Verify that the encryption type not being present does not cause an error.
     */
    @Test
    public void testQueueNoEncryptType()
    {
        //sender behavior
        when(m_MessageSender.queueMessage(sysId, m_Payload, null)).thenReturn(false);
        
        //call 'queue'
        boolean result = m_SUT.queue(sysId, null);
        
        //verify
        verify(m_MessageSender).queueMessage(sysId, m_Payload, null);
        assertThat(result, is(false));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'queue' is called.
     * Verify that the encryption type not being present does not cause an error.
     */
    @Test
    public void testQueueChannelNoEncryptType()
    {  
        //sender behavior
        when(m_MessageSender.queueMessage(m_Channel, m_Payload, null)).thenReturn(false);
        
        boolean result = m_SUT.queue(m_Channel, null);
        
        //verify
        verify(m_MessageSender).queueMessage(m_Channel, m_Payload, null);
        assertThat(result, is(false));
    }
    
    /**
     * Verify that correct information is sent to the message sender service when 'queue' is called.
     */
    @Test
    public void testQueueChannelEncryptType()
    {  
        //sender behavior
        when(m_MessageSender.queueMessage(m_Channel, m_Payload, EncryptType.AES_ECDH_ECDSA, null)).thenReturn(false);
        
        boolean result = m_SUT.queue(m_Channel, EncryptType.AES_ECDH_ECDSA, null);
        
        //verify
        verify(m_MessageSender).queueMessage(m_Channel, m_Payload, EncryptType.AES_ECDH_ECDSA, null);
        assertThat(result, is(false));
    }
    
    /**
     * Construct a payload message.
     */
    public static TerraHarvestPayload createPayload()
    {
        ControllerInfoData data = ControllerInfoData.newBuilder().setName("name").build();
        BaseNamespace namespace = BaseNamespace.newBuilder().
            setType(BaseMessageType.ControllerInfo).
            setData(data.toByteString()).build();
        return TerraHarvestPayload.newBuilder().
            setNamespace(Namespace.Base).
            setNamespaceMessage(namespace.toByteString()).build();
    }
}
