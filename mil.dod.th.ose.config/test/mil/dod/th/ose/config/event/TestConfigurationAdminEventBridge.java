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
package mil.dod.th.ose.config.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Dictionary;

import mil.dod.th.ose.config.event.ConfigurationAdminEventBridge.ConfigListener;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * Test class for the {@link ConfigurationAdminEventBridge} class.
 * 
 * @author cweisenborn
 */
public class TestConfigurationAdminEventBridge
{
    private ConfigurationAdminEventBridge m_SUT;
    private EventAdmin m_EventAdmin;
    private ServiceRegistration<ConfigurationListener> m_ServiceRegistration;
    private ConfigListener m_ConfigListener;

    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_EventAdmin = mock(EventAdmin.class);
        m_ServiceRegistration = mock(ServiceRegistration.class);
        
        m_SUT = new ConfigurationAdminEventBridge();
        
        m_SUT.setEventAdmin(m_EventAdmin);
    }
    
    /**
     * Test the activate method.
     * Verify that a configuration listener is registered with the bundle context.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivate()
    { 
        BundleContext context = mock(BundleContext.class);

        //Mock the registration return from the bundle context.
        when(context.registerService(eq(ConfigurationListener.class), Mockito.any(ConfigListener.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceRegistration);
        
        //Activate the class.
        m_SUT.activate(context);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        ArgumentCaptor<ConfigListener> listenerCaptor = ArgumentCaptor.forClass(ConfigListener.class);
        //Verify that the bundle context register service method was called and capture the dictionary that
        //was passed to the registration method.
        verify(context).registerService(eq(ConfigurationListener.class), listenerCaptor.capture(), 
                captor.capture());
        
        Dictionary<String, Object> props = captor.getValue();
        m_ConfigListener = listenerCaptor.getValue();
        
        //Verify the contents of the dictionary passed the service registration method of the bundle context.
        assertThat(props.get(EventConstants.EVENT_TOPIC), is((Object)"org/osgi/service/cm/ConfigurationEvent/*"));
    }
    
    /**
     * Test method that handles converting configuration events to standard events.
     * Verify that the event is posted to the event admin and that it is converted appropriately.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationEvent()
    {
        //activate 
        activateSetup();
        
        //Mock the configuration event.
        ConfigurationEvent configEvent = mock(ConfigurationEvent.class);

        ServiceReference<ConfigurationAdmin> serviceReference = mock(ServiceReference.class);
        when(configEvent.getPid()).thenReturn("testPid");
        when(configEvent.getFactoryPid()).thenReturn("testFactoryPid");
        when(configEvent.getReference()).thenReturn(serviceReference);
        when(serviceReference.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID)).thenReturn("service id");
        when(serviceReference.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID)).
            thenReturn("service pid");
        
        //Set the type of configuration event to deleted.
        when(configEvent.getType()).thenReturn(ConfigurationEvent.CM_DELETED);
        //Post the event.
        m_ConfigListener.configurationEvent(configEvent);
        
        //Set the type of configuration event to updated.
        when(configEvent.getType()).thenReturn(ConfigurationEvent.CM_UPDATED);
        //Post the event.
        m_ConfigListener.configurationEvent(configEvent);
        
        //Set the type of configuration event to location changed.
        when(configEvent.getType()).thenReturn(ConfigurationEvent.CM_LOCATION_CHANGED);
        //Post the event.
        m_ConfigListener.configurationEvent(configEvent);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //Verify the event admin service was called for all three events and capture those events.
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        //Verify the values of three configuration events below.
        Event event = eventCaptor.getAllValues().get(0);
        assertThat(event.getTopic(), is(ConfigurationEventConstants.TOPIC_CONFIGURATION_DELETED_EVENT));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"testPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is((Object)"testFactoryPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID), is ((Object)"service id"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID), is((Object)"service pid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_REFERENCE), 
                is((Object)serviceReference));
        
        event = eventCaptor.getAllValues().get(1);
        assertThat(event.getTopic(), is(ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"testPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is((Object)"testFactoryPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID), is ((Object)"service id"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID), is((Object)"service pid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_REFERENCE), 
                is((Object)serviceReference));
        
        event = eventCaptor.getAllValues().get(2);
        assertThat(event.getTopic(), is(ConfigurationEventConstants.TOPIC_CONFIGURATION_LOCATION_EVENT));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID), is((Object)"testPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID), is((Object)"testFactoryPid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID), is ((Object)"service id"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID), is((Object)"service pid"));
        assertThat(event.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_REFERENCE), 
                is((Object)serviceReference));
        
        //Set the type to a non-existent configuration event type.
        when(configEvent.getType()).thenReturn(100);
        
        try
        {
            //Since the configuration event type does not exist an illegal argument exception should be thrown.
            m_ConfigListener.configurationEvent(configEvent);
            fail("Illegal argument exception should be thrown.");
        }
        catch (IllegalArgumentException exception)
        {
            
        }
    }
    
    /**
     * Test the deactivate method.
     * Verify that the configuration listener is unregistered.
     */
    @Test
    public void testDeactivate()
    {
        //activate 
        activateSetup();
        
        m_SUT.deactivate();
        
        verify(m_ServiceRegistration).unregister();
    }
    
    /**
     * Activate the component and set the listener.
     */
    @SuppressWarnings("unchecked")
    private void activateSetup()
    {
        BundleContext context = mock(BundleContext.class);

        //Mock the registration return from the bundle context.
        when(context.registerService(eq(ConfigurationListener.class), Mockito.any(ConfigListener.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceRegistration);
        
        //Activate the class.
        m_SUT.activate(context);
        
        ArgumentCaptor<ConfigListener> listenerCaptor = ArgumentCaptor.forClass(ConfigListener.class);
        //Verify that the bundle context register service method was called and capture the dictionary that
        //was passed to the registration method.
        verify(context).registerService(eq(ConfigurationListener.class), listenerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        m_ConfigListener = listenerCaptor.getValue();
    }
}
