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
package mil.dod.th.core.datastream.store;

import java.util.Date;

/**
 * Structure to hold the start and stop dates of a continuous time period.
 * 
 * @author jmiller
 *
 */
public class DateRange
{
    /** Start date/time of the period in milliseconds. */
    private long m_StartTime;
    
    /** Stop date/time of the period in milliseconds. */
    private long m_StopTime;
    
    /**
     * Constructor with start and stop dates expressed in milliseconds.
     * 
     * @param startTime 
     *      start time of the period, in milliseconds
     * @param stopTime 
     *      stop time of the period, in milliseconds
     */
    public DateRange(final long startTime, final long stopTime)
    {
        m_StartTime = startTime;
        m_StopTime = stopTime;
    }
    
    /**
     * Constructor with start and stop dates given as {@link Date} objects.
     * 
     * @param startDate
     *      start date of the period, as a Date object
     * @param stopDate
     *      stop date of the period, as a Date object
     */
    public DateRange(final Date startDate, final Date stopDate)
    {
        m_StartTime = startDate.getTime();
        m_StopTime = stopDate.getTime();
    }

    public long getStartTime()
    {
        return m_StartTime;
    }

    public void setStartTime(final long startTime)
    {
        m_StartTime = startTime;
    }

    public long getStopTime()
    {
        return m_StopTime;
    }

    public void setStopTime(final long stopTime)
    {
        m_StopTime = stopTime;
    }
    
    

}
