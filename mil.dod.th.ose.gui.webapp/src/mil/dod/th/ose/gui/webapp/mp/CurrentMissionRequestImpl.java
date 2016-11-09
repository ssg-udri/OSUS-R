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

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import mil.dod.th.core.mp.Program.ProgramStatus;


/**
 * Implementation of {@link CurrentMissionRequest}.
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "curMissionRequest")
@ViewScoped
public class CurrentMissionRequestImpl implements CurrentMissionRequest
{
    /**
     * Reference to the {@link CurrentMissionMgr} managed bean.
     */
    @ManagedProperty(value = "#{currentMissionMgr}")
    private CurrentMissionMgr currentMissionMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Holds a list of valid display filters for missions.
     */
    private List<ProgramStatus> m_Filters;
    
    /**
     * Holds the list of {@link ProgramStatus}es that are used in the display of 
     * filterable choices.
     */
    private final List<ProgramStatus> m_DisplayFilters;
    
    
    /**
     * Constructor.
     */
    public CurrentMissionRequestImpl()
    {
        m_Filters = new ArrayList<ProgramStatus>();
        m_DisplayFilters = new ArrayList<ProgramStatus>();
        
        for (ProgramStatus status : ProgramStatus.values())
        {
            //this is the list that determines what is shown. Needs to have all values initially.
            m_Filters.add(status);
            
            if (!status.equals(ProgramStatus.EXECUTING) 
                    && !status.equals(ProgramStatus.SHUTTING_DOWN) 
                    && !status.equals(ProgramStatus.INITIALIZATION_ERROR)
                    && !status.equals(ProgramStatus.VARIABLE_ERROR)
                    && !status.equals(ProgramStatus.SCRIPT_ERROR))
            {
                //this is the list that is displayed for the options. needs to be limited set 
                //because certain statuses overlap.
                m_DisplayFilters.add(status);
            }
        }
    }
    
    /**
     * Set the {@link CurrentMissionMgr} service to use.
     * @param manager
     *  the {@link CurrentMissionMgr} service to be used.
     */
    public void setCurrentMissionMgr(final CurrentMissionMgr manager)
    {
        currentMissionMgr = manager;
    }
    
    
    /**
     * Function to retrieve all of the currently selected mission filters.
     * @return
     *  a list of mission statuses that represent the currently selected filters.
     */
    public List<ProgramStatus> getMissionFilters()
    {
        return m_Filters;
    }
    
    /**
     * Function to set the mission filters currently selected.
     * @param filters
     *  the selected filters to record.
     */
    public void setMissionFilters(final List<ProgramStatus> filters)
    {
        m_Filters = filters;
    }

    /**
     * Function to return the list of statuses that are displayed in the SelectManyButton menu.
     * 
     * @return
     *  the list of ProgramStatuses that are to be used as filter choices. This is a subset of 
     *  the total ProgramStatuses.
     */
    public List<ProgramStatus> getDisplayedMissionFilters()
    {
        return new ArrayList<ProgramStatus>(m_DisplayFilters);
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.CurrentMissionRequest#getCurrentFilteredMissions(int)
     */
    @Override
    public List<CurrentMissionModel> getCurrentFilteredMissions(final int controllerId)
    {
        final List<CurrentMissionModel> missions = this.currentMissionMgr.
                getCurrentMissionsForControllerAsync(controllerId);
        final List<CurrentMissionModel> missionsToReturn = new ArrayList<CurrentMissionModel>();
        
        for (CurrentMissionModel info : missions)
        {
            if (m_Filters.contains(info.getMissionStatus()) 
                    || (info.getMissionStatus().equals(ProgramStatus.EXECUTING) 
                            && m_Filters.contains(ProgramStatus.EXECUTED))
                    || (info.getMissionStatus().equals(ProgramStatus.SHUTTING_DOWN)
                            && m_Filters.contains(ProgramStatus.EXECUTED))
                    || info.getMissionStatus().equals(ProgramStatus.SCRIPT_ERROR)
                    || info.getMissionStatus().equals(ProgramStatus.INITIALIZATION_ERROR) 
                    || info.getMissionStatus().equals(ProgramStatus.VARIABLE_ERROR))
            {
                missionsToReturn.add(info);
            }
        }
       
        return missionsToReturn;
    }
    

}
