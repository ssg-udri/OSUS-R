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
package mil.dod.th.ose.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.datastream.StreamProfileConfigListener.ConfigurationListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * @author cweisenborn
 *
 */
public class TestStreamProfileConfigListener
{
    private StreamProfileConfigListener m_SUT;
    
    @Mock private EventAdmin m_EventAdmin;
    @Mock private BundleContext m_Context;
    @Mock private ServiceRegistration<EventHandler> m_ServiceReg;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceReg);
        
        m_SUT = new StreamProfileConfigListener();
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verify that a configuration listener is registered for the specified factory PID.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testRegisterConfigListener()
    {
        m_SUT.registerConfigListener("some.factory");
        
        verify(m_Context).registerService(eq(EventHandler.class), Mockito.any(ConfigurationListener.class), 
                Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify that a configuration listener is only registered once for a factory.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testRegisterConfigListenerMultipleTimes()
    {
        m_SUT.registerConfigListener("some.factory");
        m_SUT.registerConfigListener("some.factory");
        
        //Verify that multiple listeners are not registered for the same factory.
        verify(m_Context).registerService(eq(EventHandler.class), Mockito.any(ConfigurationListener.class), 
                Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify that the configuration listener for the factory with the specified PID is unregistered.
     */
    @Test
    public void testUnregisterConfigListener()
    {
        String factoryPid = "some.factory";
        m_SUT.registerConfigListener(factoryPid);
        
        m_SUT.unregisterConfigListener(factoryPid);
        
        verify(m_ServiceReg).unregister();
    }
    
    /**
     * Verify that no attempt is made to unregister a listener if none exists for the factory specified.
     */
    @Test
    public void testUnregisterConfigListenerNullListener()
    {
        String factoryPid = "some.factory";
        
        m_SUT.unregisterConfigListener(factoryPid);
        
        verify(m_ServiceReg, never()).unregister();
    }
    
    /**
     * Verify that the configuration listener reposts the event appropriately.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListener()
    {
        m_SUT.registerConfigListener("some.factory");
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        
        verify(m_Context).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getValue();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("propA", 100);
        props.put("propB", "test this");
        props.put("propC", 1337);
        Event configUpdatedEvent = new Event(ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT, props);
        
        configListener.handleEvent(configUpdatedEvent);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED));
        assertThat(event.containsProperty("propA"), is(true));
        assertThat(event.containsProperty("propB"), is(true));
        assertThat(event.containsProperty("propC"), is(true));
        assertThat(event.getProperty("propA"), is(100));
        assertThat(event.getProperty("propB"), is("test this"));
        assertThat(event.getProperty("propC"), is(1337));
    }
}
