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
package mil.dod.th.ose.logging;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.shared.OSGiEventConstants;
import mil.dod.th.ose.test.BundleContextMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * @author Dave Humeniuk
 *
 */
public class TestEventLogger
{
    private EventLogger m_SUT;
    private LoggingService m_Logging;

    @Before
    public void setUp()
    {
        m_SUT = new EventLogger();
        
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
    }
    
    /**
     * Verify that an EventHandler is not registered as the framework property is not set.
     * 
     * Test deactivation if property is disabled.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivationPropertyNotSet()
    {
        BundleContext context = mock(BundleContext.class);
        
        //replay, first with prop not set all
        m_SUT.activate(context);
        
        //verify all events are registered for
        verify(context, never()).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class));
        
        // just make sure deactivate works in this case.
        m_SUT.deactivate();
    }
    
    /**
     * Verify that an EventHandler is not registered as the framework property is set to disabled.
     * 
     * Test deactivation if property is disabled.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivationDisabled()
    {
        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(EventLogger.LOG_EVENTS_ENABLED_PROPERTY)).thenReturn("false");
        
        //replay, first with prop not set all
        m_SUT.activate(context);
        
        //verify all events are registered for
        verify(context, never()).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class));
        
        // just make sure deactivate works in this case.
        m_SUT.deactivate();
    }
    
    /**
     * Verify that an EventHandler is registered for all events.
     */
    @Test
    public void testActivation()
    {
        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(EventLogger.LOG_EVENTS_ENABLED_PROPERTY)).thenReturn("true");
        
        //replay
        m_SUT.activate(context);
        
        //verify all events are registered for
        BundleContextMocker.assertEventHandler(context, "*");
    }

    /**
     * Verify that an EventHandler is unregistered.
     */
    @Test
    public void testDeactivation()
    {
        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(EventLogger.LOG_EVENTS_ENABLED_PROPERTY)).thenReturn("true");
        ServiceRegistration<EventHandler> reg = 
                BundleContextMocker.stubServiceRegistration(context, EventHandler.class);
        
        //replay
        m_SUT.activate(context);
        m_SUT.deactivate();
        
        verify(reg).unregister();
    }
    
    /**
     * Test that passed events are logged.
     * 
     * Test that log events aren't logged or else we will never stop logging.
     */
    @Test
    public void testEventHandler()
    {
        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(EventLogger.LOG_EVENTS_ENABLED_PROPERTY)).thenReturn("true");
        
        //replay
        m_SUT.activate(context);
        
        //verify all events are registered for
        EventHandler handler = BundleContextMocker.assertEventHandler(context, "*");
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("prop1", 1);
        properties.put("other-prop", "string-value");
        Event event = new Event("test-topic", properties);
        handler.handleEvent(event);
        
        ArgumentCaptor<String> strCap = ArgumentCaptor.forClass(String.class);
        verify(m_Logging).debug(strCap.capture());
        
        assertThat(strCap.getValue(), containsString("Event: topic=[test-topic]"));
        assertThat(strCap.getValue(), containsString("prop1=1"));
        assertThat(strCap.getValue(), containsString("other-prop=string-value"));
        
        // try logging a log event to see what happens, should be ignored
        Event logEvent = new Event(OSGiEventConstants.TOPIC_LOG_ERROR, properties);
        handler.handleEvent(logEvent);
        
        // should be still holding at one
        verify(m_Logging, times(1)).debug(strCap.capture());
    }
}
