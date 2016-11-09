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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;

import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.primefaces.push.PushContext;

/**
 * Unit test class for PushChannelsGenericEventHandler
 * @author nickmarcucci
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class})
public class TestPushChannelsGenericEventHandler
{
    private PushChannelsGenericEventHandler m_SUT;
    private PushContextUtil m_PushUtil;
    private PushContext m_PushContext;
    private ServiceRegistration<?> m_Registration;
    private BundleContextUtil m_BundleUtil;
    private BundleContext m_BundleContext;
    private PushChannelMessageManager m_Manager;
    
    @SuppressWarnings("unchecked")
    @Before
    public void init()
    {
        m_SUT = new PushChannelsGenericEventHandler();
        
        m_Manager = mock(PushChannelMessageManager.class);
        m_PushContext = mock(PushContext.class);
        m_PushUtil = mock(PushContextUtil.class);
        when(m_PushUtil.getPushContext()).thenReturn(m_PushContext);
        
        m_BundleUtil = mock(BundleContextUtil.class);
        m_BundleContext = mock(BundleContext.class);
        m_Registration = mock(ServiceRegistration.class);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).
                thenReturn(m_Registration);
        
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setPushManager(m_Manager);
    }
    
    /**
     * Verify that correct events are registered for
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRegisterAndUnregister()
    {
        m_SUT.setup();
        
        ArgumentCaptor<Dictionary> dictCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_BundleContext, times(1)).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), dictCaptor.capture());
        
        Dictionary<String, Object> props = dictCaptor.getValue();
        
        assertThat(props.size(), is(1));
        assertThat((String)props.get(EventConstants.EVENT_TOPIC), is("mil/dod/th/ose/gui/webapp/*"));
        
        m_SUT.cleanup();
        
        verify(m_Registration, times(1)).unregister();
    }
    
    /**
     * Verify that an event is properly pushed and handled 
     */
    @Test
    public void testHandleEvent()
    {
        Event nonRemovedEvent = PowerMockito.mock(Event.class);
        when(nonRemovedEvent.getTopic()).thenReturn("mil/dod/th/ose/gui/webapp/someEvent");
        when(nonRemovedEvent.getPropertyNames()).thenReturn(new String[] {"propKey1", "propKey2"});
        when(nonRemovedEvent.getProperty("propKey1")).thenReturn("propVal1");
        when(nonRemovedEvent.getProperty("propKey2")).thenReturn("propVal2");
        m_SUT.handleEvent(nonRemovedEvent);
        
        ArgumentCaptor<PushDataMessage> msgCaptor = ArgumentCaptor.forClass(PushDataMessage.class);
        verify(m_Manager).addMessage(msgCaptor.capture());
        
        PushDataMessage pushData = msgCaptor.getValue();
        
        PushEventMessage pushMsg = (PushEventMessage)pushData;
        
        assertThat(pushMsg.getType(), is(PushMessageType.EVENT.toString()));
        assertThat(pushMsg.getTopic(), is("mil/dod/th/ose/gui/webapp/someEvent"));
        assertThat(pushMsg.getProperties().size(), is(2));
        assertThat(pushMsg.getProperties().keySet(), hasItems("propKey1", "propKey2"));
        assertThat(pushMsg.getProperties().values(), hasItems((Object)"propVal1", (Object)"propVal2"));
    }
}
