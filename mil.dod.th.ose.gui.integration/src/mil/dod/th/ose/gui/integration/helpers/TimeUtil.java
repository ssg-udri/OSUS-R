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
package mil.dod.th.ose.gui.integration.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Provides functions to convert to and from THOSE formatted time Strings.
 * 
 * @author nickmarcucci
 *
 */
public class TimeUtil
{
    /**
     * Function to return a calendar object holding the UTC representation of the current time.
     * @param timeInMilliseconds
     *  the time in milliseconds from the Jan 1 1970 epoch that is to be used as the time to set 
     *  the calendar to.
     * @return
     *  the calendar object which represents the time that was passed in in UTC
     */
    public static Calendar getCalendarForTime(final long timeInMilliseconds)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(timeInMilliseconds);
        
        return calendar;
    }
    
    /**
     * Function which takes a formatted date time string and returns a calendar object which 
     * represents the date time string passed in.
     * @param formattedDate
     *  the date time string to turn into a calendar object
     * @return
     *  the calendar object which represents the passed in date time string
     */
    public static Calendar getCalendarFromFormattedDate(final String formattedDate) throws ParseException
    {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date = format.parse(formattedDate);
        
        return getCalendarForTime(date.getTime());
    }
    
    /**
     * Function to return formatted time 
     * @param calendar
     *  calendar object which holds the current time that needs to be formatted.
     * @return
     *  the String format of the date and time that the calendar object contained
     */
    public static String getFormattedTime(final Calendar calendar)
    {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        return format.format(calendar.getTime());
    }
    
    /**
     * Get the standard formatted time string for the given time in milliseconds
     */
    public static String getFormattedTime(long timeInMillis)
    {
        return getFormattedTime(getCalendarForTime(timeInMillis));
    }
}
