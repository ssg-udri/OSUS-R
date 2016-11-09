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
package mil.dod.th.ose.gui.webapp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for verifying the functionality of the ControllerModel class.
 * @author bachmakm
 *
 */
public class TestControllerModel
{
    private ControllerModel m_SUT;
    private ControllerImage m_ControllerImageInterface;
    
    @Before
    public void setUp()
    {        
        m_ControllerImageInterface = new ControllerImage();
        m_SUT = new ControllerModel(8675309, m_ControllerImageInterface);
    }
    
    /**
     * Verify method returns correct ID.
     */
    @Test
    public void testGetId()
    {
        assertThat(m_SUT.getId(), is(8675309));
    }
    
    /**
     * Verify name is set.
     * Verify correct name is returned.
     */
    @Test
    public void testGetSetName()
    {
        assertThat(m_SUT.getName(), is(nullValue()));
        m_SUT.setName("howard");
        assertThat(m_SUT.getName(), is("howard"));
    }
    
    /**
     * Verify version is set.
     * Verify correct version is returned. 
     */
    @Test
    public void testGetSetVersion()
    {
        assertThat(m_SUT.getVersion(), is(nullValue()));
        m_SUT.setVersion("T1000");
        assertThat(m_SUT.getVersion(), is("T1000"));
    }
    
    /**
     * Verify build info can be set.
     * Verify correct build info is returned. 
     */
    @Test
    public void testGetSetBuildInfo()
    {
        //map of props
        Map<String, String> props = new HashMap<String, String>();
        props.put("build.info", "my build info");
        
        //verify empty list if there are no entries
        assertThat(m_SUT.getBuildInfo().isEmpty(), is(true));
        assertThat(m_SUT.getBuildInfoKeys().isEmpty(), is(true));
        
        //set the props
        m_SUT.setBuildInfo(props);
        
        //get the entry
        assertThat(m_SUT.getBuildInfo(), hasEntry("build.info", "my build info"));
    }
    
    /**
     * Verify correct hex ID is returned. 
     */
    @Test
    public void testGetHexId()
    {
        assertThat(m_SUT.getHexId(), is("0x00845FED"));
    }
    
    /**
     * Verify operational mode is set.
     * Verify correct operational mode is returned. 
     */
    @Test
    public void testGetSetOperatingMode()
    {
        assertThat(m_SUT.getOperatingMode(), is(nullValue()));
        
        m_SUT.setOperatingMode(OperationMode.TEST_MODE);
        assertThat(m_SUT.getOperatingMode(), is(OperationMode.TEST_MODE));
        
        m_SUT.setOperatingMode(OperationMode.OPERATIONAL_MODE);
        assertThat(m_SUT.getOperatingMode(), is(OperationMode.OPERATIONAL_MODE));
    }
    
    /**
     * Verify controller capabilities are set.
     * Verify correct controller capabilities are returned. 
     */
    @Test
    public void testGetSetControllerCaps()
    {
        assertThat(m_SUT.getCapabilities(), is(nullValue()));
        
        ControllerCapabilities caps = new ControllerCapabilities();
        caps.setManufacturer("kbWootWoot");
        m_SUT.setCapabilities(caps);
        
        ControllerCapabilities returnedCaps = m_SUT.getCapabilities();
        assertThat(returnedCaps, is(caps));
        assertThat(returnedCaps.getManufacturer(), is("kbWootWoot"));
    }
    
    /**
     * Verify that the default controller is returned when getting the image from the controller.
     */
    @Test
    public void testGetImage()
    {
        assertThat(m_SUT.getImage(), is("thoseIcons/default/controller.png"));
    }
    
    /**
     * Verify that the proper display text is returned based on the current operating mode.
     */
    @Test
    public void testGetOperatingModeDisplayText()
    {
        m_SUT.setOperatingMode(OperationMode.TEST_MODE);
        assertThat(m_SUT.getOperatingModeDisplayText(), is("TEST MODE"));
        
        m_SUT.setOperatingMode(OperationMode.OPERATIONAL_MODE);
        assertThat(m_SUT.getOperatingModeDisplayText(), is("OPERATIONAL MODE"));
    }
    
    /**
     * Verify that the correct flag is returned based on whether or not the controller
     * is in operational mode. 
     */
    @Test
    public void testIsOperationalMode()
    {
        m_SUT.setOperatingMode(OperationMode.TEST_MODE);
        assertThat(m_SUT.isOperationalMode(), is(false));
        
        m_SUT.setOperatingMode(OperationMode.OPERATIONAL_MODE);
        assertThat(m_SUT.isOperationalMode(), is(true));
    }
}
