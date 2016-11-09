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
import static org.mockito.Mockito.*;

import java.util.Date;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.event.DateSelectEvent;
import org.primefaces.event.FlowEvent;

import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * Test for the {@link MissionSetUpMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestMissionSetUpMgrImpl 
{
    private MissionSetUpMgrImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    
    @Before
    public void setup() throws Exception
    {
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        
        m_SUT = new MissionSetUpMgrImpl();
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
    } 
    
    @Test
    public void testGetMission()
    {
        MissionModel testMission = new MissionModel();
        m_SUT.setMission(testMission);
        assertThat(m_SUT.getMission(), equalTo(testMission));
    }
    
    @Test
    public void testGetImmediate()
    {
        m_SUT.setImmediate(true);
        assertThat(m_SUT.isImmediate(), equalTo(true));
    }
    
    @Test
    public void testGetIndefinite()
    {
        m_SUT.setIndefinate(false);
        assertThat(m_SUT.isIndefinate(), equalTo(false));
    }
    
    @Test
    public void testAjaxStartInterval()
    {
        DateSelectEvent event = mock(DateSelectEvent.class);
        Date date = new Date();
        when(event.getDate()).thenReturn(date);
        m_SUT.ajaxDateSelectStartInterval(event);
        assertThat(m_SUT.getStartInterval(), equalTo(date));
    }
    
    @Test
    public void testAjaxStopInterval()
    {
        DateSelectEvent event = mock(DateSelectEvent.class);
        Date date = new Date();
        when(event.getDate()).thenReturn(date);
        m_SUT.ajaxDateSelectStopInterval(event);
        assertThat(m_SUT.getStopInterval(), equalTo(date));
    }
    
    @Test
    public void testGetStartInterval()
    {
        Date date = new Date();
        m_SUT.setStartInterval(date);
        assertThat(m_SUT.getStartInterval(), equalTo(date));
    }
    
    @Test
    public void testGetStopInterval()
    {
        Date date = new Date();
        m_SUT.setStopInterval(date);
        assertThat(m_SUT.getStopInterval(), equalTo(date));
    }
    
    @Test
    public void testOnFlowProcess() throws Exception
    {
        //Mock event and faces message.
        FlowEvent testEvent = mock(FlowEvent.class);
        
        //Test flow process for no mission being selected on the choose a mission tab.
        m_SUT.setMission(null);
        when(testEvent.getOldStep()).thenReturn("mission");
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("mission"));
        
        //Test flow process for mission selected returns the parameters tab.
        MissionModel testModel = mock(MissionModel.class);
        when(testEvent.getNewStep()).thenReturn("variables");
        m_SUT.setMission(testModel);
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("variables"));
        
        //Test to make sure flow process allows moving back a tab even if date time is not set appropriately set on
        //the schedule tab.
        when(testEvent.getOldStep()).thenReturn("schedule");
        when(testEvent.getNewStep()).thenReturn("variables");
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("variables"));
        
        //Test to make sure flow process does not allow advancing to the final when start time is before the current 
        //system time.
        when(testEvent.getNewStep()).thenReturn("end");
        m_SUT.setImmediate(false);
        m_SUT.setIndefinate(true);
        m_SUT.setStartInterval(new Date(System.currentTimeMillis() - 25000));
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("schedule"));
        
        //Test to make sure flow process does not allow advancing to the final tab when stop time is before current 
        //system time.
        m_SUT.setImmediate(true);
        m_SUT.setIndefinate(false);
        m_SUT.setStopInterval(new Date(System.currentTimeMillis() - 25000));
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("schedule"));
        
        //Test to make sure flow process does not allow advancing to the final tab when stop time is before start time.
        m_SUT.setImmediate(false);
        m_SUT.setIndefinate(false);
        m_SUT.setStartInterval(new Date(System.currentTimeMillis() - 25000));
        m_SUT.setStopInterval(new Date(System.currentTimeMillis() - 35000));
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("schedule"));
        
        //Test to make sure flow process does not allow advancing to the final tab when start time and stop time are
        //equivalent.
        Date testDate = new Date(System.currentTimeMillis() + 25000);
        m_SUT.setImmediate(false);
        m_SUT.setIndefinate(false);
        m_SUT.setStartInterval(testDate);
        m_SUT.setStopInterval(testDate);
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("schedule"));
        
        //Test to make sure flow process allows advancing when start and stop times are appropriately set.
        m_SUT.setStartInterval(new Date(System.currentTimeMillis() + 25000));
        m_SUT.setStopInterval(new Date(System.currentTimeMillis() + 45000));
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("end"));
        
        //Test to make sure that the flow process allows returning to the mission selection tab from the variables tab
        //without validating parameters.
        when(testEvent.getOldStep()).thenReturn("variables");
        when(testEvent.getNewStep()).thenReturn("mission");
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("mission"));
        
        //Test to make sure flow process does not allow advancing to the next tab when a variable is null on the
        //parameter tab.
        when(testEvent.getNewStep()).thenReturn("schedule");
        MissionModel mission = new MissionModel();
        MissionArgumentModel argument = new MissionArgumentModel();
        mission.getArguments().add(argument);
        m_SUT.setMission(mission);
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("variables"));
        
        verify(m_GrowlUtil, times(6)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), anyString(), 
                anyString());
        
        //Test to make sure that the schedule tab is returned when values on the parameter tab are set.
        argument.setType(MissionVariableTypesEnum.STRING);
        argument.setCurrentValue("Some Value");
        assertThat(m_SUT.onFlowProcess(testEvent), equalTo("schedule"));
    }

    /**
     * Test manipulating the mission program's name.
     * 
     * Verify that the name is initialized to an empty string.
     * 
     * Verify the name is correctly set.
     */
    @Test
    public void testSetName()
    {
        //check default
        assertThat(m_SUT.getProgramName().isEmpty(), is(true));

        //set the program name
        m_SUT.setProgramName("Charlie");

        //verify
        assertThat(m_SUT.getProgramName(), is("Charlie"));

        //replay
        m_SUT.setProgramName("Charles");
        assertThat(m_SUT.getProgramName(), is("Charles"));
    }
}
