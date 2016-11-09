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
package mil.dod.th.ose.gui.webapp.channel;

import java.util.Dictionary;

import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for ChannelEventWrapper
 * @author nickmarcucci
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class})
public class TestChannelEventWrapper
{
    private ChannelEventWrapper m_SUT;
    private BundleContextUtil m_BundleUtil;
    private BundleContext m_BundleContext;
    private EventAdmin m_EventAdmin;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_ServiceRegistration;
    
    @SuppressWarnings("unchecked")
    @Before
    public void init()
    {
        m_SUT = new ChannelEventWrapper();
        
        m_BundleUtil = mock(BundleContextUtil.class);
        m_BundleContext = mock(BundleContext.class);
        m_ServiceRegistration = mock(ServiceRegistration.class);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        
        when(m_BundleContext.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).
                thenReturn(m_ServiceRegistration);
        
        m_EventAdmin = mock(EventAdmin.class);
        
        m_SUT.setBundleContextUtility(m_BundleUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
    }
    
    /**
     * Verify init and cleanup register and unregister for event admin service.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testInitAndCleanup()
    {
        ArgumentCaptor<Dictionary> propCaptor = ArgumentCaptor.forClass(Dictionary.class);
        
        m_SUT.init();
        
        verify(m_BundleContext).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), propCaptor.capture());
        
        Dictionary<String, Object> props = propCaptor.getValue();
        
        assertThat(props.size(), is(1));
        
        String[] array = (String[])props.get(EventConstants.EVENT_TOPIC);
        assertThat(array.length, is(2));
        
        assertThat(array[0], is(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED));
        assertThat(array[1], is(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED));
        
        m_SUT.cleanup();
        
        verify(m_ServiceRegistration).unregister();
        
    }
    
    /**
     * Verify RemoteChannelLookup channel updated and removed events are properly
     * translated into local gui events.
     */
    @Test
    public void testHandleEvent()
    {
        Event nonRemovedEvent = PowerMockito.mock(Event.class);
        when(nonRemovedEvent.getTopic()).thenReturn(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED);
        
        Event removedEvent = PowerMockito.mock(Event.class);
        when(removedEvent.getTopic()).thenReturn(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED);
        
        m_SUT.handleEvent(nonRemovedEvent);
        m_SUT.handleEvent(removedEvent);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event channelUpdated = eventCaptor.getAllValues().get(0);
        assertThat(channelUpdated.getTopic(), is(ChannelEventWrapper.TOPIC_CHANNEL_UPDATED));
       
        Event channelRemoved = eventCaptor.getAllValues().get(1);
        assertThat(channelRemoved.getTopic(), is(ChannelEventWrapper.TOPIC_CHANNEL_REMOVED));
    }
    
    /**
     * Verify that incorrect topic does not cause the event admin to post an event.
     */
    @Test
    public void testHandleEventWithNotRightTopic()
    {
        Event badEventTopic = PowerMockito.mock(Event.class);
        when(badEventTopic.getTopic()).thenReturn("bad topic");
        
        m_SUT.handleEvent(badEventTopic);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(0)).postEvent(eventCaptor.capture());
    }
}
