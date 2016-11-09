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

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests PushGrowlMessage data structure functions properly.
 * @author nickmarcucci
 *
 */
public class TestPushGrowlMessage
{
    private PushGrowlMessage m_SUT;
    
    @Before
    public void init()
    {
        m_SUT = new PushGrowlMessage(FacesMessage.SEVERITY_INFO, "This is summary", "This is detail", true);
    }
    
    /**
     * Verify summary of message can be retrieved.
     */
    @Test
    public void testGetSummary()
    {
        assertThat(m_SUT.getSummary(), is("This is summary"));
        
        m_SUT.setSummary("Hockey is Back!!!");
        
        assertThat(m_SUT.getSummary(), is("Hockey is Back!!!")); 
    }
    
    /**
     * Verify detail of message can be retrieved.
     */
    @Test
    public void testGetDetail()
    {
        assertThat(m_SUT.getDetail(), is("This is detail"));
        
        m_SUT.setDetail("Hawks.");
        
        assertThat(m_SUT.getDetail(), is("Hawks."));
    }
    
    /**
     * Verify severity can be retrieved/set.
     */
    @Test
    public void testGetSeverity()
    {
        assertThat(m_SUT.getSeverity(), is("info"));
        
        m_SUT.setSeverity(FacesMessage.SEVERITY_FATAL);
        
        assertThat(m_SUT.getSeverity(), is("fatal"));
        
        m_SUT.setSeverity(FacesMessage.SEVERITY_WARN);
        
        assertThat(m_SUT.getSeverity(), is("warn"));
        
        m_SUT.setSeverity(FacesMessage.SEVERITY_ERROR);
        
        assertThat(m_SUT.getSeverity(), is("error"));
    }
    
    /**
     * Verify status for sticky can be set/retrieved.
     */
    @Test
    public void testGetSticky()
    {
        assertThat(m_SUT.getSticky(), is(true));
        
        m_SUT.setSticky(false);
        
        assertThat(m_SUT.getSticky(), is(false));
    }
    
    /**
     * Verify toString method properly prints out values.
     */
    @Test
    public void testToString()
    {
        String toString = m_SUT.toString();
        assertThat(toString, is("MessageType: GROWL_MESSAGE " 
                +"{ summary: 'This is summary' detail: 'This is detail' severity: 'info' sticky: 'true' }"));
    }
}
