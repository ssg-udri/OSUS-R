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
 * Test class for the {@link ConfigAdminModel} class.
 * 
 * @author cweisenborn
 */
public class TestConfigAdminModel
{
    private ConfigAdminModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new ConfigAdminModel();
    }
    
    /**
     * Test the get bundle location method.
     * Verify that the correct location is returned.
     */
    @Test
    public void testGetBundleLocation()
    {
        m_SUT.setBundleLocation("location");
        
        assertThat(m_SUT.getBundleLocation(), is("location"));
    }
    
    /**
     * Test the get pid method.
     * Verify that the correct pid is returned.
     */
    @Test
    public void testGetPid()
    {
        m_SUT.setPid("pid");
        
        assertThat(m_SUT.getPid(), is("pid"));
    }
    
    /**
     * Test the get factory pid method.
     * Verify that the correct factory pid is returned.
     */
    @Test
    public void testGetFactoryPid()
    {
        m_SUT.setFactoryPid("factory pid");
        
        assertThat(m_SUT.getFactoryPid(), is("factory pid"));
    }
    
    /**
     * Test the is factory method.
     * Verify the correct boolean is returned.
     */
    @Test
    public void testIsFactory()
    {
        m_SUT.setIsFactory(true);
        
        assertThat(m_SUT.isFactory(), is(true));
    }
    
    /**
     * Test the get factory configurations method.
     * Verify that correct configuration is in the returned list of configurations.
     */
    @Test
    public void testGetFactoryConfigurations()
    {
        ConfigAdminModel test = new ConfigAdminModel();
        
        m_SUT.getFactoryConfigurations().put("Test", test);
        
        assertThat(m_SUT.getFactoryConfigurations().get("Test"), is(test));
    }
    
    /**
     * Test the get properties method.
     * Verify that the correct property is in the returned list of properties.
     */
    @Test
    public void testGetProperties()
    {
        ConfigAdminPropertyModel test = new ConfigAdminPropertyModel();
        
        m_SUT.getProperties().add(test);
        
        assertThat(m_SUT.getProperties().get(0), is(test));
    }
}
