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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.mp.Program.ProgramStatus;


import org.junit.Before;
import org.junit.Test;

/**
 * Tests the functionality of the CurrentMissionRequestImpl class.
 * @author nickmarcucci
 *
 */
public class TestCurrentMissionRequestImpl
{
    private CurrentMissionRequestImpl m_SUT;
    private CurrentMissionMgr m_CurrentMgr;
    
    @Before
    public void init()
    {
        m_CurrentMgr = mock(CurrentMissionMgrImpl.class);
        
        m_SUT = new CurrentMissionRequestImpl();
        
        m_SUT.setCurrentMissionMgr(m_CurrentMgr);
        
        when(m_CurrentMgr.getCurrentMissionsForControllerAsync(123)).thenReturn(new ArrayList<CurrentMissionModel>());
    }
    
    /*
     * Verify that mission filters contains all the program status lists.
     */
    @Test
    public void testGetMissionFilters()
    {
        List<ProgramStatus> statuses = m_SUT.getMissionFilters();
        assertThat(statuses.size(), is(ProgramStatus.values().length));
        assertThat(statuses, contains(ProgramStatus.values()));
    }
    
    /*
     * Test that correct missions are returned based on the current filters
     * that are stored by the CurrentMissionRequest object.
     * 
     * 1. No missions so none returned
     * 2. All missions returned because filter contains all types
     * 3. Subset of missions based on filters
     */
    @Test
    public void testGetCurrentFilteredMissions()
    {
        //test that empty list is returned when no missions exist
        assertThat(m_SUT.getCurrentFilteredMissions(123).size(), is(0));
        
        CurrentMissionModel model1 = new CurrentMissionModel("Mission 1", 
                ProgramStatus.CANCELED, "Template 1", (long)0, (long)0);
        CurrentMissionModel model2 = new CurrentMissionModel("Mission 2", 
                ProgramStatus.EXECUTED, "Template 2", (long)0, (long)0);
        CurrentMissionModel model3 = new CurrentMissionModel("Mission 3", 
                ProgramStatus.EXECUTING, "Template 3", (long)0, (long)0);
        CurrentMissionModel model4 = new CurrentMissionModel("Mission 4", 
                ProgramStatus.EXECUTING_TEST, "Template 4", (long)0, (long)0);
        CurrentMissionModel model5 = new CurrentMissionModel("Mission 5", 
                ProgramStatus.INITIALIZATION_ERROR, "Template 5", (long)0, (long)0);
        CurrentMissionModel model6 = new CurrentMissionModel("Mission 6", 
                ProgramStatus.SCHEDULED, "Template 6", (long)0, (long)0);
        CurrentMissionModel model7 = new CurrentMissionModel("Mission 7", 
                ProgramStatus.SCRIPT_ERROR, "Template 7", (long)0, (long)0);
        CurrentMissionModel model8 = new CurrentMissionModel("Mission 8", 
                ProgramStatus.SHUTDOWN, "Template 8", (long)0, (long)0);
        CurrentMissionModel model9 = new CurrentMissionModel("Mission 9", 
                ProgramStatus.SHUTTING_DOWN, "Template 9", (long)0, (long)0);
        CurrentMissionModel model10 = new CurrentMissionModel("Mission 10", 
                ProgramStatus.UNSATISFIED, "Template 10", (long)0, (long)0);
        CurrentMissionModel model11 = new CurrentMissionModel("Mission 11", 
                ProgramStatus.VARIABLE_ERROR, "Template 11", (long)0, (long)0);
        CurrentMissionModel model12 = new CurrentMissionModel("Mission 12", 
                ProgramStatus.WAITING_INITIALIZED, "Template 12", (long)0, (long)0);
        CurrentMissionModel model13 = new CurrentMissionModel("Mission 13", 
                ProgramStatus.WAITING_UNINITIALIZED, "Template 13", (long)0, (long)0);
        
        List<CurrentMissionModel> models = new ArrayList<CurrentMissionModel>();
        models.add(model1);
        models.add(model2);
        models.add(model3);
        models.add(model4);
        models.add(model5);
        models.add(model6);
        models.add(model7);
        models.add(model8);
        models.add(model9);
        models.add(model10);
        models.add(model11);
        models.add(model12);
        models.add(model13);
        
        when(m_CurrentMgr.getCurrentMissionsForControllerAsync(123)).thenReturn(models);
        
        //test that all missions are returned
        assertThat(m_SUT.getCurrentFilteredMissions(123).size(), is(13));
        
        //setFilters and makes sure only a subset are returned 
        List<ProgramStatus> statuses = new ArrayList<ProgramStatus>();
        statuses.add(ProgramStatus.EXECUTING);
        statuses.add(ProgramStatus.SHUTDOWN);
        statuses.add(ProgramStatus.UNSATISFIED);
        
        m_SUT.setMissionFilters(statuses);
        
        //test that only the three missions are returned
        List<CurrentMissionModel> filteredModels = m_SUT.getCurrentFilteredMissions(123); 
        
        //6 are returned because error statuses are always shown, even thought they aren't
        //specified in the filter list.
        assertThat(filteredModels.size(), is(6));
        
        for(CurrentMissionModel fModel : filteredModels)
        {
            if (!fModel.getMissionName().equals("Mission 3") && !fModel.getMissionName().equals("Mission 5")
                    && !fModel.getMissionName().equals("Mission 7") && !fModel.getMissionName().equals("Mission 8")
                    && !fModel.getMissionName().equals("Mission 10") && !fModel.getMissionName().equals("Mission 11"))
            {
                fail("A mission was returned that is not expected. The mission name was " + fModel.getMissionName() 
                        + " and had a program status of " + fModel.getMissionStatus());
            }
        }
    }
    
    /**
     * Verify the list of displayed filters contains the correct items.
     */
    @Test
    public void testGetDisplayedMissionFilters()
    {
        List<ProgramStatus> statuses = m_SUT.getDisplayedMissionFilters();
        
        assertThat(statuses.size(), is(8));
        
        assertThat(statuses, not(contains(ProgramStatus.EXECUTING)));
        assertThat(statuses, not(contains(ProgramStatus.SHUTTING_DOWN)));
        assertThat(statuses, not(contains(ProgramStatus.INITIALIZATION_ERROR)));
        assertThat(statuses, not(contains(ProgramStatus.VARIABLE_ERROR)));
        assertThat(statuses, not(contains(ProgramStatus.SCRIPT_ERROR)));
    }
}
