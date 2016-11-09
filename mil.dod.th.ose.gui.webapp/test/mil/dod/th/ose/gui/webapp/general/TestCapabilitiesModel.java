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
package mil.dod.th.ose.gui.webapp.general;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;

/**
 * @author jgold
 *
 */
public class TestCapabilitiesModel 
{
    private CapabilityModel m_SUT;
    
    @Before
    public void setUp()
    {
        m_SUT = new CapabilityModel();
    }

    /**
     * Test basic getters and setters.
     */
    @Test
    public void testGettersAndSetters() 
    {
        m_SUT.setName("myname");
        m_SUT.setValue("myvalue");
        
        assertThat(m_SUT.getName(), is("myname"));
        assertThat(m_SUT.getValue(), is("myvalue"));
        
        assertThat(m_SUT.toString(), is("myname / myvalue"));
    }

}
