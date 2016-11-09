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

import java.util.HashMap;

import javax.faces.application.FacesMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.primefaces.push.PushContext;

/**
 * Tests the PushChannelMessageManager class.
 * @author nickmarcucci
 *
 */
public class TestPushChannelMessageManager
{
    private PushChannelMessageManager m_SUT;
    private PushContextUtil m_PushUtil;
    private PushContext m_PushContext;
    
    @Before
    public void init()
    {
        m_PushContext = mock(PushContext.class);
        m_PushUtil = mock(PushContextUtil.class);
        
        when(m_PushUtil.getPushContext()).thenReturn(m_PushContext);
        when(m_PushContext.push(eq(PushChannelConstants.PUSH_CHANNEL_THOSE_MESSAGES),
                Mockito.any(PushDataMessage.class))).thenReturn(null);
        
        m_SUT = new PushChannelMessageManager();
        
        m_SUT.setPushContextUtil(m_PushUtil);
    }
    
    @After
    public void tearDown()
    {
        m_SUT.preDestory();
    }
    
    /**
     * Tests pushing of a message.
     */
    @Test
    public void testPushMessage() throws InterruptedException
    {
        //activate the component.
        m_SUT.postConstruct();
        
        PushGrowlMessage msg = new PushGrowlMessage(FacesMessage.SEVERITY_INFO, "summary", "description", false);
        
        //push message
        m_SUT.addMessage(msg);
        
        PushEventMessage msg2 = new PushEventMessage("some event", new HashMap<String, Object>());
        msg2.setType(PushMessageType.EVENT);
        
        //push message
        m_SUT.addMessage(msg2);
        
        //verify that both messages were pushed
        ArgumentCaptor<PushDataMessage> captor = ArgumentCaptor.forClass(PushDataMessage.class);
        verify(m_PushContext, timeout(2000).times(2)).push(eq(PushChannelConstants.PUSH_CHANNEL_THOSE_MESSAGES), 
                captor.capture());
        
        assertThat(captor.getAllValues().size(), is(2));
        
        PushGrowlMessage rcvMsg = (PushGrowlMessage)captor.getAllValues().get(0);
        assertThat(rcvMsg, notNullValue());
        assertThat(rcvMsg.getSummary(), is("summary"));
        assertThat(rcvMsg.getDetail(), is("description"));
        assertThat(rcvMsg.getSticky(), is(false));
        assertThat(rcvMsg.getSeverity(), is("info"));
        
        PushEventMessage rcvEvent = (PushEventMessage)captor.getAllValues().get(1);
        assertThat(rcvEvent, notNullValue());
        assertThat(rcvEvent.getTopic(), is("some event"));
        assertThat(rcvEvent.getProperties().size(), is(0));
    }
}
