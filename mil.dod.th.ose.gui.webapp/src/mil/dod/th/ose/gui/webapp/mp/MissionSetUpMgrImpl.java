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

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;

import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.primefaces.component.calendar.Calendar;
import org.primefaces.event.DateSelectEvent;
import org.primefaces.event.FlowEvent;

/**
 * Implementation of the {@link MissionSetUpMgr} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "missionSetUpMgr")
@ViewScoped
public class MissionSetUpMgrImpl implements MissionSetUpMgr
{   
    /**
     * String which represents the id of mission tab in the mission setup page wizard.
     */
    private static final String MISSION_TAB_ID = "mission";
    
    /**
     * String which represents the id of the parameters tab in the mission setup page wizard.
     */
    private static final String VARIABLES_TAB_ID = "variables";
    
    /**
     * String which represents the id of the schedule tab in the mission setup page wizard.
     */
    private static final String SCHEDULE_TAB_ID = "schedule";
    
    /**
     * Reference to the growl utility used to create growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;
    
    /**
     * Reference to a {@link MissionModel} which is the mission that is currently selected.
     */
    private MissionModel m_SelectedMission;
    
    /**
     * Reference to a {@link MissionProgramSchedule} used mission start and stop information.
     */
    private final MissionProgramSchedule m_Schedule;

    /**
     * The mission program's name.
     */
    private String m_Name;

    /**
     * Default constructor for the managed bean.
     */
    public MissionSetUpMgrImpl()
    {
        m_Schedule = new MissionProgramSchedule();
        m_Schedule.setAtReset(true);
        m_Schedule.setImmediately(true);
        m_Schedule.setIndefiniteInterval(true);
        m_Name = "";
    }
    
    /**
     * Method used to set the growl message utility used to create growl messages.
     * 
     * @param growlMessageUtil
     *          The {@link GrowlMessageUtil} to be set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlMessageUtil)
    {
        m_GrowlMessageUtil = growlMessageUtil;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#getMission()
     */
    @Override
    public MissionModel getMission()
    {
        return m_SelectedMission;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setMission(mil.dod.th.ose.gui.webapp.mp.MissionModel)
     */
    @Override
    public void setMission(final MissionModel mission)
    {
        m_SelectedMission = mission;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#getSchedule()
     */
    @Override
    public MissionProgramSchedule getSchedule()
    {
        return m_Schedule;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setStartInterval(java.util.Date)
     */
    @Override
    public void setStartInterval(final Date startTime)
    {
        m_Schedule.setStartInterval(startTime.getTime());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#ajaxStartInterval(org.primefaces.event.DateSelectEvent)
     */
    @Override
    public void ajaxDateSelectStartInterval(final DateSelectEvent event)
    {
        setStartInterval(event.getDate());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionSetUpMgr#ajaxStartInterval(javax.faces.event.AjaxBehaviorEvent)
     */
    @Override
    public void ajaxValueChangeStartInterval(final AjaxBehaviorEvent event)
    {
        final Calendar startCalendar = (Calendar)event.getComponent();
        final Date changedDate = (Date)startCalendar.getValue();
       
        setStartInterval(changedDate);
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#getStartInterval()
     */
    @Override
    public Date getStartInterval()
    {
        if (m_Schedule.getStartInterval() == null)
        {
            return new Date();
        }
        return new Date(m_Schedule.getStartInterval());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setStopInterval(java.util.Date)
     */
    @Override
    public void setStopInterval(final Date stopTime)
    {
        m_Schedule.setStopInterval(stopTime.getTime());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#ajaxStopInterval(org.primefaces.event.DateSelectEvent)
     */
    @Override
    public void ajaxDateSelectStopInterval(final DateSelectEvent event)
    {
        setStopInterval(event.getDate());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionSetUpMgr#ajaxStopInterval(javax.faces.event.AjaxBehaviorEvent)
     */
    @Override
    public void ajaxValueChangeStopInterval(final AjaxBehaviorEvent event)
    {
        final Calendar stopCalendar = (Calendar)event.getComponent();
        final Date changedDate = (Date)stopCalendar.getValue();
        
        setStopInterval(changedDate);
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#getStopInterval()
     */
    @Override
    public Date getStopInterval()
    {
        if (m_Schedule.getStopInterval() == null)
        {
            return new Date();
        }
        return new Date(m_Schedule.getStopInterval());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setImmediate(boolean)
     */
    @Override
    public void setImmediate(final boolean frame)
    {
        if (frame)
        {
            m_Schedule.setStartInterval(null);
            m_Schedule.setImmediately(frame);
        }
        else
        {
            m_Schedule.setImmediately(null);
        }
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setIndefinate(boolean)
     */
    @Override
    public void setIndefinate(final boolean frame)
    {
        if (frame)
        {
            m_Schedule.setStopInterval(null);
            m_Schedule.setIndefiniteInterval(frame);
        }
        else
        {
            m_Schedule.setIndefiniteInterval(null);
        }
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#isImmediate()
     */
    @Override
    public boolean isImmediate()
    {
        if (m_Schedule.isImmediately() == null)
        {
            return false;
        }
        return m_Schedule.isImmediately();
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#isIndefinate()
     */
    @Override
    public boolean isIndefinate()
    {
        if (m_Schedule.isIndefiniteInterval() == null)
        {
            return false;
        }
        return m_Schedule.isIndefiniteInterval();
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#getProgramName()
     */
    @Override
    public String getProgramName() 
    {
        return m_Name;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#setProgramName(String)
     */
    @Override
    public void setProgramName(final String name) 
    {
        m_Name = name;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionMgr#onFlowProcess(org.primefaces.event.FlowEvent)
     */
    @Override
    public String onFlowProcess(final FlowEvent event)
    {
        //Checks to make sure that a mission has been selected before proceeding to the next tab of the wizard.
        if (event.getOldStep().equals(MISSION_TAB_ID) && m_SelectedMission == null)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "Mission not selected!", 
                    "Please select a mission template! Next button is invalid.");
            return event.getOldStep();
        }
        
        //Calls the method used to validate that all mission parameters are set and valid.
        if (event.getOldStep().equals(VARIABLES_TAB_ID))
        {
            return handleVariableFlowEvent(event);
        }
        
        //Calls the method used to validate start time and end time.
        if (event.getOldStep().equals(SCHEDULE_TAB_ID))
        {
            return handleScheduleFlowEvent(event);
        }
        
        return event.getNewStep();
    }
    
    /**
     * Method that handles checking to make sure that all parameters for the mission are not blank and are set to 
     * valid values be fore allow the wizard to advance to the next tab.
     * 
     * @param event
     *          {@link FlowEvent} that represents the call to change from the variables to the next or previous tab
     *          in the wizard within the mission setup page.
     * @return
     *          The string that represents the id of the wizard tab in which to switch to.
     */
    private String handleVariableFlowEvent(final FlowEvent event)
    {
        //Used to allow returning to a previous tab without validating parameter values.
        if (event.getNewStep().equals(MISSION_TAB_ID))
        {
            return event.getNewStep();
        }
        
        for (MissionArgumentModel argument : m_SelectedMission.getArguments())
        {
            //Checks the arguments value for being null or blank since value is a string.
            if (argument.getCurrentValue() == null || argument.getCurrentValue().equals(""))
            {
                m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "No value for " 
                        + argument.getName() + " has been set!", "Please set a value for the parameter!");
                return event.getOldStep();
            }
        }

        return event.getNewStep();
    }
    
    /**
     * Method that handles checking the start time and stop time to make sure they are valid before the wizard can
     * advance to the next tab in the process.
     *  
     * @param event
     *          {@link FlowEvent} that represents the call to change from the schedule tab to the next or previous tab
     *          in the wizard within the mission setup page.
     * @return
     *          The string that represents the id of the wizard tab in which to switch to.
     */
    private String handleScheduleFlowEvent(final FlowEvent event)
    {
        //Used to allow returning to the previous tab without validating date.
        if (event.getNewStep().equals(VARIABLES_TAB_ID))
        {
            return event.getNewStep();
        }
    
        final Date startTime = getStartInterval();
        final Date stopTime = getStopInterval(); 
    
        if (!isImmediate() && !isIndefinate() && startTime.compareTo(stopTime) >= 0)
        {
            //Checks to make sure start and stop time are not equal.
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, 
                    "Warning! Stop time must be after the start time.", 
                    "Please select a new start or stop time!");
            return event.getOldStep();
        }
        
        //Checks to make sure start time is not before the current date/time.
        if (!isImmediate() && startTime.before(new Date()))
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, 
                    "Warning! Start time cannot be before the current date/time.", 
                    "Please select a new start time!");
            return event.getOldStep();
        }

        //Checks to make sure stop time is not before current date/time.
        if (!isIndefinate() && stopTime.before(new Date()))
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, 
                    "Warning! Stop time cannot be before the current date/time.", 
                    "Please select a new stop time!");
            return event.getOldStep();
        }
        
        return event.getNewStep();
    }
    
}
