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
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.ose.gui.webapp.mp.MissionModel.MissionTemplateLocation;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link MissionModel}.
 * 
 * @author cweisenborn
 */
public class TestMissionModel
{
    private MissionModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new MissionModel();
    }
    
    @Test
    public void testSetName()
    {
        String testName = "test";
        m_SUT.setName(testName);
        assertThat(m_SUT.getName(), is(testName));
    }
    
    @Test
    public void testSetDescription()
    {
        String testDesc = "Some test description!";
        m_SUT.setDescription(testDesc);
        assertThat(m_SUT.getDescription(), is(testDesc));
    }
    
    @Test
    public void testSetSource()
    {
        String testSource = "Some source code.";
        m_SUT.setSource(testSource);
        assertThat(m_SUT.getSource(), is(testSource));
    }
    
    @Test
    public void testSetLocation()
    {
        m_SUT.setLocation(MissionTemplateLocation.LOCAL);
        assertThat(m_SUT.getLocation(), is(MissionTemplateLocation.LOCAL));
    }
    
    @Test
    public void testGetArguments()
    {
        List<MissionArgumentModel> args = new ArrayList<MissionArgumentModel>();
        MissionArgumentModel testArgs  = new MissionArgumentModel();
        
        testArgs.setName("Var 1");
        args.add(testArgs);
        m_SUT.getArguments().add(testArgs);
        
        testArgs.setName("Var 2");
        args.add(testArgs);
        m_SUT.getArguments().add(testArgs);
        
        assertThat(m_SUT.getArguments(), is(args));
    }
    
    @Test
    public void testGetSecondaryImages()
    {
        List<Integer> testMap = new ArrayList<Integer>();
        testMap.add(1);
        m_SUT.getSecondaryImageIds().add(1);
        testMap.add(2);
        m_SUT.getSecondaryImageIds().add(2);
        testMap.add(3);
        m_SUT.getSecondaryImageIds().add(3);
        
        assertThat(m_SUT.getSecondaryImageIds(), is(testMap));
    }
    
    @Test
    public void testSetWithImageCapture()
    {
        m_SUT.setWithImageCapture(true);
        assertThat(m_SUT.isWithImageCapture(), is(true));
    }
    
    @Test
    public void testSetWithInterval()
    {
        m_SUT.setWithInterval(true);
        assertThat(m_SUT.isWithInterval(), is(true));
    }
    
    @Test
    public void testSetWithSensorTrigger()
    {
        m_SUT.setWithSensorTrigger(true);
        assertThat(m_SUT.isWithSensorTrigger(), is(true));
    }
    
    @Test
    public void testSetWithTimerTrigger()
    {
        m_SUT.setWithTimerTrigger(true);
        assertThat(m_SUT.isWithTimerTrigger(), is(true));
    }
}
