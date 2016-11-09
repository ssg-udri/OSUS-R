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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the functionality of the CurrentMissionModel class.
 * 
 * @author nickmarcucci
 *
 */
public class TestCurrentMissionModel
{
    private CurrentMissionModel m_SUT;
    
    @Before
    public void init()
    {
        m_SUT = new CurrentMissionModel("Mission 1", 
                ProgramStatus.EXECUTED, "Template 1", (long)22, (long)0);
    }
    
    /*
     * Verify model with initialized values returns the correct values.
     */
    @Test
    public void testCurrentMissionModel()
    {
        //mission name
        assertThat(m_SUT.getMissionName(), is("Mission 1"));
        
        //mission template name
        assertThat(m_SUT.getTemplateName(), is("Template 1"));
        
        //mission status
        assertThat(m_SUT.getMissionStatus().toString(), is(ProgramStatus.EXECUTED.toString()));
        
        //mission start interval
        assertThat(m_SUT.getStartInterval(), is((long)22));
        
        //mission stop interval
        assertThat(m_SUT.getStopInterval(), is((long)0));
    }
    
    /**
     * Verify that hasStartInterval returns true when set and false otherwise.
     */
    @Test
    public void testHasStartInterval()
    {
        //assert that expected initial values return expected results
        assertThat(m_SUT.hasStartInterval(), is(true));
        
        //reverse and test again
        m_SUT = new CurrentMissionModel("Mission 1", 
                ProgramStatus.EXECUTED, "Template 1", (long)0, (long)22);
        
        assertThat(m_SUT.hasStartInterval(), is(false));
    }
    
    /**
     * Verify that hasStopInterval returns true when set and false otherwise.
     */
    @Test
    public void testHasStopInterval()
    {
        //assert that expected initial values return expected results
        assertThat(m_SUT.hasStopInterval(), is(false));
        
        //reverse and test again
        m_SUT = new CurrentMissionModel("Mission 1", 
                ProgramStatus.EXECUTED, "Template 1", (long)0, (long)22);
        
        assertThat(m_SUT.hasStopInterval(), is(true));
    }
    
    /**
     * Verify LastTestResult is initially null and returns correct value when set.
     */
    @Test
    public void testLastTestResult()
    {
        //initialized to null should get it back
        assertThat(m_SUT.getLastTestResult(), nullValue());
        
        m_SUT.setLastTestResult(TestResult.PASSED);
        
        //should get test result value
        assertThat(m_SUT.getLastTestResult(), is(TestResult.PASSED));
    }
}
