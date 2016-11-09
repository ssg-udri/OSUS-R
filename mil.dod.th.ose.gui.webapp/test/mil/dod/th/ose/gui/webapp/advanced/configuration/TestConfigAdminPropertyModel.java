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
 * Test class for the {@link ConfigAdminPropertyModel} class.
 * 
 * @author cweisenborn
 */
public class TestConfigAdminPropertyModel
{
    private ConfigAdminPropertyModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new ConfigAdminPropertyModel();
    }
    
    /**
     * Test the get key method.
     * Verify that the correct key is returned.
     */
    @Test
    public void testGetKey()
    {
        m_SUT.setKey("key");
        
        assertThat(m_SUT.getKey(), is("key"));
    }
    
    /**
     * Test the get value method.
     * Verify that the correct value is returned.
     */
    @Test
    public void testGetValue()
    {
        m_SUT.setValue(15);
        
        assertThat(m_SUT.getValue(), is((Object)15));
    }
    
    /**
     * Test the get type method.
     * Verify that the correct type is returned.
     */
    @Test
    public void testGetType()
    {
        m_SUT.setType(Integer.class);
        
        assertThat(m_SUT.getType(), is((Object)Integer.class));
    }
}
