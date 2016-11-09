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
package mil.dod.th.ose.gui.webapp.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import java.util.Map;

import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.webapp.general.StatusCapable;

import org.junit.Before;
import org.junit.Test;

/**
 * @author allenchl
 *
 */
public class TestStatusComponent
{
    private StatusComponent m_SUT = new StatusComponent();
    private StatusCapable m_CapableObj;
    
    @Before
    public void setup()
    {
        //get the attribute list.. the map is live
        Map<String, Object> attrs = m_SUT.getAttributes();
        
        //mock status giving object
        m_CapableObj = mock(StatusCapable.class);
        
        //add to attrs
        attrs.put("statusCapableObject", m_CapableObj);
    }
    
    /**
     * Verify correct string is returned for good status.
     */
    @Test
    public void testGoodStatus()
    {
        when(m_CapableObj.getSummaryStatus()).thenReturn(SummaryStatusEnum.GOOD);
        
        assertThat(m_SUT.getStatusStyle(), is("led-GOOD"));
    }
    
    /**
     * Verify correct string is returned for bad status.
     */
    @Test
    public void testBadStatus()
    {
        when(m_CapableObj.getSummaryStatus()).thenReturn(SummaryStatusEnum.BAD);
        
        assertThat(m_SUT.getStatusStyle(), is("led-BAD"));
    }
    
    /**
     * Verify correct string is returned for degraded status.
     */
    @Test
    public void testDegradedStatus()
    {
        when(m_CapableObj.getSummaryStatus()).thenReturn(SummaryStatusEnum.DEGRADED);
        
        assertThat(m_SUT.getStatusStyle(), is("led-DEGRADED"));
    }
    
    /**
     * Verify correct string is returned for UNKNONWN and null statuses.
     */
    @Test
    public void testUnknownStatus()
    {
        String expectedStyle = "led-UNKNOWN ui-icon-help ui-icon";
        when(m_CapableObj.getSummaryStatus()).thenReturn(SummaryStatusEnum.UNKNOWN);
        
        assertThat(m_SUT.getStatusStyle(), is(expectedStyle));
        
        when(m_CapableObj.getSummaryStatus()).thenReturn(null);
        
        assertThat(m_SUT.getStatusStyle(), is(expectedStyle));
    }
}
