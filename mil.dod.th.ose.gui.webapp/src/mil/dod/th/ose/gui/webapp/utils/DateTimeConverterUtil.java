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
package mil.dod.th.ose.gui.webapp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * Converter utility to format dates into the proper format of MM/dd/yyyy HH:mm:ss'Z'.
 * 
 * @author nickmarcucci
 *
 */
@FacesConverter("dateTimeConverter")
public class DateTimeConverterUtil implements Converter
{
    /**
     * The format string for time intervals.
     */
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss'Z'";
    
    /**
     * The timezone dates should be displayed in.
     */
    private static final String DATE_TIME_ZONE = "UTC";
    

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String value)
    {
        return formatDateFromString(value);
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object value)
    {
        return formatDateFromLong((Long)value);
    }
    
    /**
     * Function which takes a date represented as a long and converts it to a 
     * formatted date time string.
     * @param time
     *  the time in number of milliseconds from the epoch to convert.
     * @return
     *  the formatted string for the entered time in milliseconds.
     */
    public static String formatDateFromLong(final Long time)
    {
        final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        format.setTimeZone(TimeZone.getTimeZone(DATE_TIME_ZONE));
        
        return format.format(new Date(time));
        
    }
    
    /**
     * Function which takes a formatted date string and converts it to a long
     * representing the number of milliseconds from the Jan 1 1970 epoch.
     * @param time
     *  the formatted date string 
     * @return
     *  the time in milliseconds since the Jan 1 1970 epoch.
     */
    public static Long formatDateFromString(final String time)
    {
        final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        format.setTimeZone(TimeZone.getTimeZone(DATE_TIME_ZONE));
        
        try
        {
            final Date date = format.parse(time);
            return date.getTime();
        }
        catch (final ParseException exception)
        {
            final FacesMessage parseMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error parsing date string value", 
                    "The date value passed in " + time + " cannot be parsed because of the following reason "
                    + exception.getMessage());
            
            throw new ConverterException(parseMessage, exception);
        }
    }
    
    /**
     * Rounds the specified date to the last millisecond of the given second for the date.
     * 
     * @param date
     *      The date to be rounded.
     * @return
     *      The rounded date.
     */
    public static Date roundToEndOfSecond(final Date date)
    {
        final int maxMsToAdd = 999;
        final int millisecondsInASecond = 1000;
        final long remainder = date.getTime() % millisecondsInASecond;
        final long millisecondsToAdd = maxMsToAdd - remainder;
        
        final Instant adjustedEndDate = date.toInstant().plusMillis(millisecondsToAdd);
        return Date.from(adjustedEndDate);
    }
}
