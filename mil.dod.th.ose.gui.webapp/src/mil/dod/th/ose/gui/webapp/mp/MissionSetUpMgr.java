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

import javax.faces.event.AjaxBehaviorEvent;

import mil.dod.th.core.mp.model.MissionProgramSchedule;

import org.primefaces.event.DateSelectEvent;
import org.primefaces.event.FlowEvent;

/**
 * Manages the setup of a mission for the currently active controller. The implementation of this class should be view
 * scoped. This allows multiple users to setup a mission at the same time and makes sure that the data stored does not
 * become stale. 
 * 
 * @author cweisenborn
 */
public interface MissionSetUpMgr
{

    /**
     * Method that returns the currently selected mission.
     * 
     * @return
     *          The currently selected mission.
     */
    MissionModel getMission();

    /**
     * Method that sets the currently selected mission.
     * 
     * @param mission
     *          The mission to be set.
     */
    void setMission(MissionModel mission);

    /**
     * Method that retrieves the mission program schedule.
     * 
     * @return
     *          Schedule used to store start and stop information for the mission.
     */
    MissionProgramSchedule getSchedule();

    /**
     * Method used to set the {@link MissionProgramSchedule} start interval.
     * 
     * @param startTime
     *           The {@link Date} which represents the start time to be set.
     */
    void setStartInterval(Date startTime);

    /**
     * The method that is called by ajax to set the {@link MissionProgramSchedule} start interval on 
     * a date selected event.
     * 
     * @param event
     *         The event that represents the start time to be set.
     */
    void ajaxDateSelectStartInterval(DateSelectEvent event);
    
    /**
     * The method that is called by ajax to set the {@link MissionProgramSchedule} start interval on 
     * a value changed event.
     * @param event
     *      The event that represents the start time to be set.
     */
    void ajaxValueChangeStartInterval(AjaxBehaviorEvent event);

    /**
     * Method that retrieves the start interval from the {@link MissionProgramSchedule} and returns it as a date.
     * 
     * @return
     *          The {@link Date} that represents the start interval stored in the mission program schedule.
     */
    Date getStartInterval();

    /**
     * Method used to set the {@link MissionProgramSchedule} stop interval.
     * 
     * @param stopTime
     *           The {@link Date} which represents the stop time to be set.
     */
    void setStopInterval(Date stopTime);

    /**
     * The method that is called by ajax to set the {@link MissionProgramSchedule} stop interval on 
     * a date selected event.
     * 
     * @param event
     *         The event that represents the stop time to be set.
     */
    void ajaxDateSelectStopInterval(DateSelectEvent event);
    
    /**
     * The method that is called by ajax to set the {@link MissionProgramSchedule} stop interval on 
     * a value changed event.
     * @param event
     *      The event that represents the stop time to be set.
     */
    void ajaxValueChangeStopInterval(AjaxBehaviorEvent event);

    /**
     * Method that retrieves the stop interval from the {@link MissionProgramSchedule} and returns it as a date.
     * 
     * @return
     *          The {@link Date} that represents the stop interval stored in the mission program schedule.
     */
    Date getStopInterval();

    /**
     * Method sets the boolean value of the {@link MissionProgramSchedule} immediately field.
     * 
     * @param frame
     *          Boolean value of whether the start time is immediate or not.
     */
    void setImmediate(boolean frame);

    /**
     * Method that sets the boolean value of the {@link MissionProgramSchedule} indefinitely field.
     * 
     * @param frame
     *          Boolean value of whether the stop time is indefinite or not.
     */
    void setIndefinate(boolean frame);

    /**
     * Method that returns the boolean represents if the start time is immediately or not.
     * 
     * @return
     *          Boolean that represents whether the start time is immediate or not.
     */
    boolean isImmediate();

    /**
     * Method that returns the boolean that represents if the stop time is indefinite or not.
     * 
     * @return
     *          Boolean that represents whether the stop time is indefinite or not.
     */
    boolean isIndefinate();

    /**
     * Method that retrieves the mission program name.
     * 
     * @return
     *          The user defined program name, maybe empty if the user chose not to set a name.
     */
    String getProgramName();

    /**
     * Method sets the mission program name.
     * 
     * @param name
     *          The user defined program name.
     */
    void setProgramName(String name);

    /**
     * Method that handles changing from one tab of the wizard on the mission setup page.
     * 
     * @param event
     *          The event that represents the changing from one tab in the wizard to another.
     * @return
     *          String that represents id of the wizard tab in which to switch to.
     */
    String onFlowProcess(FlowEvent event);
}
