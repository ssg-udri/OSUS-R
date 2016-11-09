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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestPushUpdateOutputText
{
    private PushUpdateOutputText m_SUT;
    
    @Before
    public void setUp()
    {
        m_SUT = Mockito.spy(new PushUpdateOutputText());
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("for", "WooWoo");
        when(m_SUT.getAttributes()).thenReturn(attributes);   
    }
    
    /**
     * Verify that correct id is returned when client id doesn't have colon.
     */
    @Test
    public void testGetForIdClientIdNoColon()
    {
        doReturn("nocolon").when(m_SUT).getClientId();
        
        assertThat(m_SUT.getForId(), is("WooWoo"));
    }
    
    /**
     * Verify that correct id is returned when client id contains colons.
     */
    @Test
    public void testGetForIdClientIdWithColon()
    {
        doReturn("ya:ya:yippe:W").when(m_SUT).getClientId();
        
        assertThat(m_SUT.getForId(), is("ya:ya:yippe:WooWoo"));
    }
}
