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

import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;

/**
 * Class will send messages that have been queued.  If the message can't be sent, the message will stay in queue for a
 * later attempt.  The queue has a capacity of {@value #QUEUE_CAPACITY} messages and will retry messages every {@value 
 * #RETRY_INTERVAL_MS} milliseconds.
 */
@Component(factory = QueuedMessageSender.FACTORY_NAME)
public class QueuedMessageSender
{
    /**
     * Name of the factory.
     */
    public static final String FACTORY_NAME = "mil.dod.th.ose.remote.QueuedMessageSender";
    
    /**
     * Size of queue for sending messages.
     */
    public static final int QUEUE_CAPACITY = 500;
    
    /**
     * How often to attempt re-sending.
     */
    public static final int RETRY_INTERVAL_MS = 1000; 

    /**
     * Property key for the component containing the {@link RemoteChannel} used for sending messages.
     */
    public static final String CHANNEL_PROP_KEY = "channel";

    /**
     * Queue used for sending messages.
     */
    private final BlockingDeque<TerraHarvestMessage> m_SendMessageQueue = 
            new LinkedBlockingDeque<TerraHarvestMessage>(QUEUE_CAPACITY);

    /**
     * Channel used to send queued data.
     */
    private RemoteChannel m_Channel;

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Whether the sender is enabled to be running.  Set to false to stop thread.
     */
    private boolean m_EnableRunning = true;

    /**
     * Thread continuously attempts to send messages as new ones are queued.
     */
    private Thread m_Thread;
    
   /**
    * Queued message that is trying to be sent.
    */
    private TerraHarvestMessage m_QueueMessage;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Activate this component.
     * 
     * @param properties
     *      properties of the component, namely {@value #CHANNEL_PROP_KEY}
     */
    @Activate
    public void activate(final Map<String, Object> properties)
    {
        m_Channel = (RemoteChannel)properties.get(CHANNEL_PROP_KEY);
        
        m_Thread = new Thread(new Runner());
        m_Thread.setName(m_Channel.toString() + "MessageSender");
        m_Thread.start();
    }
    
    /**
     * Deactivate the component by stopping the send thread.  Any messages on the queue will be lost.
     * 
     * @throws InterruptedException
     *      if interrupted while waiting for thread to stop 
     */
    @Deactivate
    public void deactivate() throws InterruptedException
    {
        m_EnableRunning = false;
        
        m_Thread.interrupt(); // interrupt the thread if it is waiting
        final int ThreadWaitMs = 1000;
        m_Thread.join(ThreadWaitMs);
        assert !m_Thread.isAlive();
    }
    
    /**
     * Queue the message to be sent.  The message will attempted to be sent until successful.  Will attempt to send 
     * message every {@value #RETRY_INTERVAL_MS} milliseconds.
     * 
     * @param message
     *      message to send
     * @return
     *      true if message was queued, false if queue is full
     */
    public boolean queue(final TerraHarvestMessage message)
    {
        return m_SendMessageQueue.offer(message);
    }
    
    /**
     * Get the count of queued messages.  The queue count is different than how many messages are left to be sent.  A 
     * single message may be in the process of being sent and no longer on the queue.
     * 
     * @return
     *      number of queued messages
     */
    public int getQueuedMessageCount()
    {
        if (m_QueueMessage == null)
        {
            return m_SendMessageQueue.size();
        }
        else
        {
            //Account for the message that is trying to be sent
            return m_SendMessageQueue.size() + 1;
        }
    }

    /**
     * Clear the queue of messages for this channel. Any data in the queue will be permanently lost.  If a message is 
     * already in the process of being sent, it will still be sent.
     */
    public void clearQueue()
    {
        m_SendMessageQueue.clear();
        m_QueueMessage = null; //NOPMD: null assignment, makes getQueuedMessages return correct number.
    }
    
    /**
     * Runner class that reads messages from the queue and try to send them out.
     * 
     * @author Dave Humeniuk
     *
     */
    class Runner implements Runnable
    {
        @Override
        public void run()
        {
            while (m_EnableRunning)
            {
                if (m_QueueMessage == null)
                {
                    // no message to send yet, wait for one
                    try
                    {
                        m_QueueMessage = m_SendMessageQueue.takeFirst();
                    }
                    catch (final InterruptedException e)
                    {
                        // thread is interrupted so go ahead and exit out
                        m_Logging.debug("QueuedMessageSender thread for [%s] interrupted while waiting for message" 
                                + ", possibly due to channel deactivation", m_Channel);
                        return;
                    }
                }
                else
                {
                    // message send, try to do it
                    if (m_Channel.trySendMessage(m_QueueMessage))
                    {
                        // message sent, no longer needed
                        m_QueueMessage = null; //NOPMD: null assignment, used to keep track of state
                    }
                    else
                    {
                        // failed to send message, wait before trying again
                        try
                        {
                            Thread.sleep(RETRY_INTERVAL_MS);
                        }
                        catch (final InterruptedException e)
                        {
                            // thread is interrupted so go ahead and exit out
                            m_Logging.warning(
                                    "QueuedMessageSender thread for [%s] interrupted while waiting to retry sending", 
                                    m_Channel);
                            return;
                        }
                    }
                }
            }
        }
    }
}
