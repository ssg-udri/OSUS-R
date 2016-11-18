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
package mil.dod.th.ose.remote;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class TestQueuedMessageSender
{
    private QueuedMessageSender m_SUT;
    private RemoteChannel m_Channel;

    /**
     * Message index used {@link #testQueuingMultipleMessages()} to keep track of what message to expect next.
     */
    private int m_NextMessageIndex;
    private boolean m_OutOfOrderDetected;
    private LoggingService m_Logging;
    
    @Before
    public void setUp()
    {
        m_SUT = new QueuedMessageSender();
        
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        m_Channel = mock(RemoteChannel.class);
        
        // simulate activation of the component
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(QueuedMessageSender.CHANNEL_PROP_KEY, m_Channel);
        m_SUT.activate(properties);
    }
    
    /**
     * Verify that a queued message will be sent to the channel for an attempt to send.
     * 
     * Verify only attempted once since the first attempt is successful.
     */
    @Test
    public void testQueuingInitialSuccess() throws InterruptedException
    {
        TerraHarvestMessage message = constructMessage(1);
        
        // mock channel to send messages successfully
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(true);
        
        // queue a single message
        boolean result = m_SUT.queue(message);
        assertThat("message is reported as queued", result, is(true));
        
        // wait for the sender thread to attempt sending message
        Thread.sleep(1000);
        
        // verify actually sent to channel and only once
        verify(m_Channel, times(1)).trySendMessage(message);
    }
    
    /**
     * Verify that is the send fails, the component will attempt to send again until successful.
     */
    @Test
    public void testQueueingWithInitialFailure() throws InterruptedException
    {
        TerraHarvestMessage message = constructMessage(1);
        
        // change channel to not send messages temporarily
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        
        boolean result = m_SUT.queue(message);
        assertThat("message is reported as queued", result, is(true));
        
        // wait for the sender thread to attempt sending message
        Thread.sleep(2000);
        
        // verify message was attempted multiple times, retry is 1000, slept 2000, so should be 2-3 times depending on 
        // timing
        verify(m_Channel, atLeast(2)).trySendMessage(message);
        verify(m_Channel, atMost(3)).trySendMessage(message);
    }
    
    /**
     * Verify that a queued message will be sent to the channel for an attempt to send repeatedly as long as successful.
     * 
     * Verify messages are sent in order.
     */
    @Test
    public void testQueuingMultipleMessages() throws InterruptedException
    {
        // create some messages that will be queued
        final TerraHarvestMessage[] messages = new TerraHarvestMessage[10]; 
        for (int i=0; i < 10; i++)
        {
            messages[i] = constructMessage(i);
        }
        
        // mock channel to send messages successfully
        // answer will verify that messages are sent in the order they are queued
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                TerraHarvestMessage message = (TerraHarvestMessage)invocation.getArguments()[0];
                
                if (message.getMessageId() != m_NextMessageIndex)
                {
                    System.err.format("Expecting %d message id and got %d%n", m_NextMessageIndex, 
                            message.getMessageId());
                    m_OutOfOrderDetected = true;
                }
                m_NextMessageIndex++;
                
                // always report message as sent
                return true;
            }
        });

        for (int i=0; i < 10; i++)
        {
            boolean result = m_SUT.queue(messages[i]);
            assertThat("message is reported as queued", result, is(true));
        }
        
        // wait for the sender thread to attempt sending message
        Thread.sleep(1000);
        
        assertThat("answer method didn't find failure", m_OutOfOrderDetected, is(false));
        
        // verify each message is sent exactly once
        for (int i=0; i < 10; i++)
        {
            verify(m_Channel, times(1)).trySendMessage(messages[i]);
        }
    }

    /**
     * Verify message will not be attempted to be sent after deactivation.
     */
    @Test
    public void testDeactivate() throws InterruptedException
    {
        // allow thead to start running before stopping it
        Thread.sleep(500);
        
        m_SUT.deactivate();
        
        TerraHarvestMessage message = constructMessage(1);
        m_SUT.queue(message);
        
        // wait to see if thread calls trySendMessage
        Thread.sleep(2000);
        
        // should never have sent message since queued after deactivation
        verify(m_Channel, never()).trySendMessage(message);
    }
    
    /**
     * Verify the queue comes back as full if limit is reached.
     * 
     * Verify queue count is correct.
     * 
     * Verify the clearing the queue allows messages to be queued again.
     */
    @Test
    public void testFullQueue() throws InterruptedException
    {
        // have send fail to make queue fill up
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        
        assertThat("queue is empty initially", m_SUT.getQueuedMessageCount(), is(0));
        
        boolean result;
        //Add one to the capacity to account for the message that is currently trying to be sent.
        for (int i=1; i <= QueuedMessageSender.QUEUE_CAPACITY+1; i++)
        {
            TerraHarvestMessage message = constructMessage(i);
            result = m_SUT.queue(message);
            assertThat("able to queue up to capacity limit", result, is(true));
            Thread.yield();
            m_Logging.debug("queue:" + m_SUT.getQueuedMessageCount());
            assertThat("queue size equals number of messages still queued", m_SUT.getQueuedMessageCount(), is(i));
        }
        
        TerraHarvestMessage message = constructMessage(1000);
        result = m_SUT.queue(message);
        assertThat("not able to queue since at limit", result, is(false));
        
        //empty the queue.
        m_SUT.clearQueue();
        
        //verify
        assertThat("queue was emptied", m_SUT.getQueuedMessageCount(), is(0));
        
        //try to queue a message
        message = constructMessage(1001);
        m_SUT.queue(message);
        
        assertThat("queue size equals number of messages queued", m_SUT.getQueuedMessageCount(), is(1));
    }
    
    /**
     * Verify the queue can be emptied for a particular channel. 
     * 
     * Verify queue count returns to 0.
     */
    @Test
    public void testEmptyQueue() throws InterruptedException
    {
        // have send fail to make queue fill up
        when(m_Channel.trySendMessage(Mockito.any(TerraHarvestMessage.class))).thenReturn(false);
        
        assertThat("queue is empty initially", m_SUT.getQueuedMessageCount(), is(0));
        
        boolean result;
        for (int i=1; i < 10; i++)
        {
            TerraHarvestMessage message = constructMessage(i);
            result = m_SUT.queue(message);
            assertThat("able to queue up to capacity limit", result, is(true));
            
            assertThat("queue size equals number of messages still queued", m_SUT.getQueuedMessageCount(), is(i));
        }
        
        //empty the queue.
        m_SUT.clearQueue();
        
        //verify
        assertThat("queue was emptied", m_SUT.getQueuedMessageCount(), is(0));
    }

    /**
     * Create a basic message for testing.
     */
    private TerraHarvestMessage constructMessage(int messageId)
    {
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder().setType(BaseMessageType.ControllerInfo).build();
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(1, 2, Namespace.Base, messageId, 
                        baseNamespaceMessage);
        return message;
    }
}
