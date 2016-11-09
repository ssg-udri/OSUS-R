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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link UnmodifiableConfigMetatypeModel} class.
 * 
 * @author cweisenborn
 */
public class TestUnmodifiableConfigMetatypeModel
{
    private UnmodifiableConfigMetatypeModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new UnmodifiableConfigMetatypeModel("some pid");
    }
    
    /**
     * Test the get pid method.
     * Verify that the correct pid is returned.
     */
    @Test
    public void testGetPid()
    {
        assertThat(m_SUT.getPid(), is("some pid"));
    }
    
    /**
     * Test the get properties method.
     * Verify that the method returns a list with correct properties within.
     */
    @Test
    public void  testGetProperties()
    {
        UnmodifiablePropertyModel property = ModelFactory.createPropModel("key", "value");
        
        m_SUT.getProperties().add(property);
        
        assertThat(m_SUT.getProperties().size(), is(1));
        assertThat(m_SUT.getProperties().get(0), is(property));
    }
}
