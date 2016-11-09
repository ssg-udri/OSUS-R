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

import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;

/**
 * Class to represent a current mission program that is running on a controller.
 * @author nickmarcucci
 *
 */
public class CurrentMissionModel
{
    /**
     * The name of the mission.
     */
    private final String m_MissionName;
    
    /**
     * The status of the mission.
     */
    private ProgramStatus m_ProgramStatus;
    
    /**
     * The start interval for the mission in milliseconds from 
     * the Jan 1 1970 epoch UTC.
     */
    private final Long m_StartInterval;
    
    /**
     * The stop interval for the mission in milliseconds from 
     * the Jan 1 1970 epoch UTC.
     */
    private final Long m_StopInterval;
    
    /**
     * The mission template name for this current mission.
     */
    private final String m_MissionTemplateName; 
    
    
    /**
     * The last test status for this mission.
     */
    private TestResult m_LastResult;
    
    /**
     * Constructor.
     * @param missionName
     *  the name of the mission program.
     * @param status
     *  the status of the mission program.
     * @param templateName
     *  the name of the mission template for this mission.
     * @param start
     *  the start interval for the mission program, if none exists pass null.
     * @param stop
     *  the stop interval for the mission program, if none exists pass null.
     */
    public CurrentMissionModel(final String missionName, final ProgramStatus status, 
            final String templateName, final Long start, final Long stop)
    {
        m_MissionName = missionName;
        m_ProgramStatus = status;
        
        m_MissionTemplateName = templateName;
        
        //start interval
        m_StartInterval = start;
       
        //stop interval 
        m_StopInterval = stop;
    }
    
    /**
     * Indicates whether or not a start interval exists for this mission.
     * @return
     *  true if a start interval has been set. false otherwise 
     */
    public boolean hasStartInterval()
    {
        if (m_StartInterval != 0)
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Indicates whether or not a stop interval exists for this mission.
     * @return
     *  true if a stop interval has been set. false otherwise
     */
    public boolean hasStopInterval()
    {
        if (m_StopInterval != 0)
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Function to update the mission status.
     * @param status
     *  the updated status value.
     */
    public void updateMissionStatus(final ProgramStatus status)
    {
        m_ProgramStatus = status;
    }
    
    /**
     * Function to return the name of the mission.
     * @return
     *  the name for this mission.
     */
    public String getMissionName()
    {
        return m_MissionName;
    }
    
    /**
     * Function to retrieve the mission status.
     * @return
     *  the current status for a mission.
     */
    public ProgramStatus getMissionStatus()
    {
        return m_ProgramStatus;
    }
    
    
    /**
     * Returns a formatted date string representation of the 
     * mission's set start interval.
     * @return
     *  the time in milliseconds from the the Jan 1 1970 epoch UTC
     */
    public Long getStartInterval()
    {
        return m_StartInterval;
    }
    
    /**
     * Returns a formatted date string representation of the 
     * mission's set stop interval.
     * @return
     *  the time in milliseconds from the the Jan 1 1970 epoch UTC
     */
    public Long getStopInterval()
    {
        return m_StopInterval;
    }
    
    /**
     * Function to set the result of the last test run for this mission.
     * @param result
     *  the result of the running the test
     */
    public void setLastTestResult(final TestResult result)
    {
        m_LastResult = result;
    }
    
    /**
     * Function returns the last test result when this mission was tested.
     * @return
     *  the mission test result.
     */
    public TestResult getLastTestResult()
    {
        return m_LastResult;
    }
    
    /**
     * Return the name of the template that this mission uses.
     * @return
     *  the string name of the mission template
     */
    public String getTemplateName()
    {
        return m_MissionTemplateName;
    }
}
