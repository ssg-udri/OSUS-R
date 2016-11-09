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
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;

/**
 * @author nickmarcucci
 *
 */
public class TestTerraHarvestControllerUtil
{
    private TerraHarvestControllerUtilImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private TerraHarvestController m_TerraHarvestController;
    
    @Before
    public void setUp()
    {
        m_SUT = new TerraHarvestControllerUtilImpl();
        
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_TerraHarvestController = mock(TerraHarvestController.class);
        
        when(m_TerraHarvestController.getName()).thenReturn("NotBacon");
        when(m_TerraHarvestController.getId()).thenReturn(345);
        
        m_SUT.setTerraHarvestController(m_TerraHarvestController);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        m_SUT.setSystemId(123);
        m_SUT.setSystemName("Bacon");
        
        when(m_TerraHarvestController.getVersion()).thenReturn("MostRecentValue");
    }
    
    /**
     * Test the init function of terra harvest system util.
     */
    @Test
    public void testThsInit()
    {
        m_SUT.init();
        
        assertThat(m_SUT.getSystemId(), is(345));
        assertThat(m_SUT.getSystemName(), is("NotBacon"));
    }
    
    /**
     * Verify the system name can be retrieved/changed
     */
    @Test
    public void testGetSystemName()
    {
        assertThat(m_SUT.getSystemName(), is("Bacon"));
        
        m_SUT.setSystemName("Cheese");
        
        assertThat(m_SUT.getSystemName(), is("Cheese"));
    }
    
    /**
     * Verify the system id can retrieved/changed
     */
    @Test 
    public void testGetSystemId()
    {
        assertThat(m_SUT.getSystemId(), is(123));
        
        m_SUT.setSystemId(1);
        
        assertThat(m_SUT.getSystemId(), is(1));
    }
    
    /**
     * Verify the system version can be retrieved
     */
    @Test
    public void testGetSystemVersion()
    {
        assertThat(m_SUT.getSystemVersion(), is("MostRecentValue"));
    }
    
    /**
     * Verify the system build info can be retrieved
     */
    @Test
    public void testGetSystemBuildInfo()
    {
        
        //map of properties for the build info
        Map<String, String> props = new HashMap<String, String>();
        props.put("when.prop", "1492 sailed the ocean blue");
        props.put("why.prop", "new land");
        when(m_TerraHarvestController.getBuildInfo()).thenReturn(props);

        //verify content
        assertThat(m_SUT.getSystemBuildInformation().size(), is(2));
        assertThat(m_SUT.getSystemBuildInformation(), hasEntry("when.prop", "1492 sailed the ocean blue"));
        assertThat(m_SUT.getSystemBuildInformation(), hasEntry("why.prop", "new land"));
        assertThat(m_SUT.getSystemBuildInformationKeys(), hasItems("when.prop", "why.prop"));
    }
    
    /**
     * Verify system name and id can be changed and that a 
     * FacesMessage is invoked
     */
    @Test
    public void testSetSystemNameAndId()
    {
        m_SUT.setSystemId(111);
        m_SUT.setSystemName("Bears");
        
        m_SUT.setSystemNameAndId();
        
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), anyString(), anyString());
        
        assertThat(m_SUT.getSystemId(), is(111));
        assertThat(m_SUT.getSystemName(), is("Bears"));
    }
}
