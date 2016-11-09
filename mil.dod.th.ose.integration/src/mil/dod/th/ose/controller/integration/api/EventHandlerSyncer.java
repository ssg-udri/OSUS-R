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
package mil.dod.th.ose.controller.integration.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class EventHandlerSyncer implements EventHandler
{
    private Semaphore m_WaitSem;
    private ServiceRegistration<EventHandler> m_Registration;
    private List<Event> m_FoundEvents = new ArrayList<>();
    private ExpectedEvent m_ExpectedEvent;
    
    /** Optional handler. */
    private EventHandler m_Handler;
    
    public EventHandlerSyncer(final String topic)
    {
        this(new String[] { topic }, null, null, null);
    }
    
    public EventHandlerSyncer(final String topic, final ExpectedEvent expected)
    {
        this(new String[] {topic}, null, null, expected);
    }
    
    public EventHandlerSyncer(final String[] topics)
    {
        this(topics, null, null, null);
    }
    
    public EventHandlerSyncer(final String topic, final String filter)
    {
        this(new String[] { topic }, filter, null, null);
    }
    
    public EventHandlerSyncer(final String topic, final String filter, final ExpectedEvent expected)
    {
        this(new String[] { topic }, filter, null, expected);
    }
    
    public EventHandlerSyncer(final String[] topics, final String filter)
    {
        this(topics, filter, null, null);
    }
    
    public EventHandlerSyncer(final String topic, final String filter, final EventHandler handler)
    {
        this(new String[] { topic }, filter, handler, null);
    }
    
    public EventHandlerSyncer(final String topic, final String filter, 
            final EventHandler handler, final ExpectedEvent expected)
    {
        this(new String[] { topic }, filter, handler, expected);
    }
    
    public EventHandlerSyncer(final String[] topics, final String filter, 
            final EventHandler handler)
    {
       this(topics, filter, handler, null);
    }
    
    public EventHandlerSyncer(final String[] topics, final String filter, 
            final EventHandler handler, final ExpectedEvent expectedEvent)
    {
        m_Handler = handler;
        m_WaitSem = new Semaphore(0);
        
        m_ExpectedEvent = expectedEvent;
        
        final Dictionary<String, Object> d = new Hashtable<String, Object>();
        d.put(EventConstants.EVENT_TOPIC, topics);
        if (filter != null)
        {
            d.put(EventConstants.EVENT_FILTER, filter);
        }
        m_Registration = IntegrationTestRunner.getBundleContext().registerService(EventHandler.class, this, d);
        
        //log the creation of this syncer
        StringBuilder builder = new StringBuilder();
        builder.append("EventHandlerSyncer has been registered with topic(s) ");
        for(String topic : topics)
        {
            builder.append(topic + "; ");
        }
        
        if (filter != null)
        {
            builder.append("and filter " + filter + " ");
        }
        
        if (expectedEvent != null)
        {
            builder.append("and includes an ExpectedEvent matcher ");
        }
        
        Logging.log(LogService.LOG_INFO, builder.toString()); 
    }
    
    @Override
    public void handleEvent(final Event event)
    {
        if (m_Handler != null)
        {
            // call optional handler
            m_Handler.handleEvent(event);
        }
        m_FoundEvents.add(event);
        
        if (m_ExpectedEvent != null)
        {
            if (m_ExpectedEvent.isExpectedEvent(event))
            {
                m_WaitSem.release();
            }
        }
        else
        {
            m_WaitSem.release();
        }
    }
    
    /**
     * Wait for the event to occur.
     * 
     * @param timeout
     *      how long in seconds to wait
     * @param times
     *      how many times to wait for the event, exactly, if the event happens more in the timeout period, assert will
     *      occur
     * @param extraTimeout
     *      how long to wait for additional events if looking for an exact amount
     * @throws InterruptedException
     *      if thread is interrupted while waiting
     */
    public List<Event> waitForEvent(int timeout, int times, int extraTimeout) throws InterruptedException
    {
        return waitForEvent(timeout, true, -times, extraTimeout);
    }
    
    /**
     * Wait for the event to occur.
     * 
     * @param timeout
     *      how long in seconds to wait
     */
    public Event waitForEvent(int timeout)
    {
        return waitForEvent(timeout, true, 1, 0).get(0);
    }
    
    
    /**
     * Wait for the event to occur.
     * 
     * @param timeout
     *      how long in seconds to wait
     * @param doAssert  
     *      whether to assert the failure
     * @param times
     *      how many times to wait for the event, a zero or negative means wait for exactly that many events (* -1), if 
     *      the event happens more in the timeout period, assert will occur
     * @param extraTimeout
     *      how long to wait for additional events if looking for an exact amount
     * @return
     *      list of events that were found while waiting, more or may not equal the desired amount
     */
    public List<Event> waitForEvent(int timeout, boolean doAssert, int times, int extraTimeout)
    {
        final ServiceReference<?> reference = m_Registration.getReference();
        final String eventStr = arrayToString((String[])reference.getProperty(EventConstants.EVENT_TOPIC), ",") 
            + " filter=" + reference.getProperty(EventConstants.EVENT_FILTER);
        
        try
        {
            if (times > 0)
            {
                final boolean acquired = m_WaitSem.tryAcquire(times, timeout, TimeUnit.SECONDS);
                if (doAssert)
                {
                    assertThat("Timeout waiting for event: " + eventStr, acquired, is(true));
                }
            }
            else
            {
                final boolean acquired = m_WaitSem.tryAcquire(-times, timeout, TimeUnit.SECONDS);
                if (doAssert)
                {
                    assertThat("Timeout waiting for event: " + eventStr, acquired, is(true));
                }
                
                if (!acquired) {return m_FoundEvents;}
                
                final boolean extraAcquired = m_WaitSem.tryAcquire(1, extraTimeout, TimeUnit.SECONDS);
                if (doAssert)
                {
                    assertThat("Received too many of event: " + eventStr, extraAcquired, is(false));
                }
            }
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        finally
        {
            m_Registration.unregister();
        }
        
        return m_FoundEvents;
    }

    private String arrayToString(String[] strings, String separator)
    {
        StringBuffer result = new StringBuffer();
        if (strings.length > 0)
        {
            result.append(strings[0]);
            for (int i=1; i < strings.length; i++)
            {
                result.append(separator);
                result.append(strings[i]);
            }
        }
        return result.toString();
    }
    
    /**
     * Capture events for later inspection. 
     * @author callen
     *
     */
    public static class LastEventSavingHandler implements EventHandler
    {
        private Event m_LastEvent;
        
        public Event getLastEvent()
        {
            return m_LastEvent;
        }
        
        @Override
        public void handleEvent(Event event)
        {
            m_LastEvent = event;
        }
    }
}
