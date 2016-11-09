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
package mil.dod.th.ose.gui.webapp.utils.push;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;


/**
 * Manager class which provides a central object to which other objects can post push messages via. Messages
 * will be pushed out to clients in a FIFO manner.
 * @author nickmarcucci
 *
 */
@Singleton
@Startup
public class PushChannelMessageManager
{
    /**
     * {@link BlockingQueue} which holds the messages to be pushed.
     */
    private final BlockingQueue<PushDataMessage> m_PushMessageQueue = new LinkedBlockingQueue<PushDataMessage>();
    
    /**
     * Future object which is used to stop the message thread processing.
     */
    private Future<?> m_MsgThreadFuture;
    
    /**
     * Utility service that is used to get the push context.
     */
    @Inject
    private PushContextUtil m_PushContextUtil;
    
    /**
     * Set the push context utility to use.
     * 
     * @param pushContextUtil
     *     the {@link PushContextUtil} to set.
     */
    public void setPushContextUtil(final PushContextUtil pushContextUtil)
    {
        m_PushContextUtil = pushContextUtil;
    }
    
    /**
     * PostConstruct method which will initialize thread execution.
     */
    @PostConstruct
    public void postConstruct()
    {
        final MessagePusherThread pusherThread = new MessagePusherThread();
        
        final ExecutorService pool = Executors.newFixedThreadPool(1);
        
        try
        {
            m_MsgThreadFuture = pool.submit(pusherThread);
        }
        catch (final Exception exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, 
                    "An error has occurred trying to activate service component %s. " 
                    + "Trying to initiate message pushing thread has failed.", this.getClass());
        }
    }
    
    /**
     * PreDestroy function which will cancel thread execution.
     */
    @PreDestroy
    public void preDestory()
    {
        //needs to be interrupted
        m_MsgThreadFuture.cancel(true);
    }
    
    /**
     * Function adds a message to be pushed at some time.
     * @param message
     *  the message that is to be pushed.
     */
    public void addMessage(final PushDataMessage message)
    {
        m_PushMessageQueue.add(message);
    }
    
    /**
     * Class which constantly checks a message queue and pushes messages if the queue contains any.
     * 
     * @author nickmarcucci
     *
     */
    private class MessagePusherThread implements Runnable
    {
        /**
         * Error message beginning constant.
         */
        private static final String ERROR_MSG = "The following wait for the future object to complete has failed ";
        
        @Override
        public void run()
        {
            while (true)
            {
                final PushDataMessage message;
                try
                {
                    //grab message from queue but block if empty
                    message = m_PushMessageQueue.take();
                    
                }
                catch (final InterruptedException exception)
                {
                    Logging.log(LogService.LOG_INFO, exception, 
                            ERROR_MSG + "has been interrupted with the exception %s", exception.getMessage());
                    break;
                }
                
                try
                {
                    Logging.log(LogService.LOG_DEBUG, "Pushing message [%s]", message.toString());
                    m_PushContextUtil.getPushContext().push(
                            PushChannelConstants.PUSH_CHANNEL_THOSE_MESSAGES, message);
                }
                catch (final Throwable exception) //NOPMD: Call may result in a StackOverflowException if 
                {                                 //JSONifier is unable to properly convert the given message.
                    Logging.log(LogService.LOG_ERROR, exception, "Failed to push message. [%s]", message.toString());
                }
                
            }
        }
        
    }
}
