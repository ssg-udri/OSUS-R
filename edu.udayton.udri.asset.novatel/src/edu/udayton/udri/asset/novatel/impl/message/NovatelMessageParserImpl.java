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
package edu.udayton.udri.asset.novatel.impl.message;

import java.util.Calendar;
import java.util.TimeZone;

import aQute.bnd.annotation.component.Component;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

import edu.udayton.udri.asset.novatel.NovatelConstants;
import edu.udayton.udri.asset.novatel.message.NovatelInsMessage;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException.FormatProblem;
import edu.udayton.udri.asset.novatel.message.NovatelMessageParser;

/**
 * This class takes data strings which are expected to contain data from 
 * the {@link edu.udayton.udri.asset.novatel.NovatelAsset}.
 * @author allenchl
 *
 */
@Component
public class NovatelMessageParserImpl implements NovatelMessageParser
{
    /**
     * Constant for the semicolon which separates the header from the data.
     */
    final static private String SEMI_COLON_SEPARATOR = ";";
    
    /**
     * Constant for the comma which separates data fields from one another.
     */
    final static private String COMMA_SEPARATOR = ",";
    
    /**
     * The most recent time offset to use for calculating the UTC time from the GPS time reported in the data message
     * headers.
     */
    private Long m_Offset;
    
    @Override
    public NovatelInsMessage parseInsMessage(final String data) throws NovatelMessageException
    {
        //constants that represent the indices where the expected data should be
        final int longitudePosistion = 3;
        final int latitudePosition = 2;
        final int altitudePosition = 4;
        final int headingPosition = 10;
        final int elevationPosition = 9;
        final int bankPosition = 8;
        //total number of data fields
        final int totalNumberOfDataFields = 12;
        //fields from header
        final int expectedNumberOfHeaderFields = 10;
        //header data indices
        final int gpsWeekPosition = 5;
        final int gpsSecondsPosition = 6;

        //verify that message is actually valid
        if (!data.contains("INS_SOLUTION_GOOD"))
        {
            throw new NovatelMessageException("The INSPVA message is not valid, as reported by the unit.", 
                    FormatProblem.INS_STATUS_NOT_GOOD);
        }
        
        //get rid of header // make sure time is not UNKNOWN
        final String[] firstRunSplit = data.split(SEMI_COLON_SEPARATOR);
        //make sure that there was at least a header, should be two because there is the header and then
        if (firstRunSplit.length < 2) //the data sections in a single message
        {
            throw new NovatelMessageException("The INSPVA message is NOT complete! Message: " + data, 
                    FormatProblem.INCOMPLETE_INS_MESSAGE);
        }
        
        //get GPS time data
        final String[] headerFields = firstRunSplit[0].split(COMMA_SEPARATOR);
        if (headerFields.length < expectedNumberOfHeaderFields)
        {
            throw new NovatelMessageException("The INSPVA message header is NOT complete! Message: " + data, 
                    FormatProblem.INCOMPLETE_INS_MESSAGE);
        }
        final Integer gpsWeek = Ints.tryParse(headerFields[gpsWeekPosition]);
        final Float gpsSeconds = Floats.tryParse(headerFields[gpsSecondsPosition]);
        checkArgsForNull("GPS time", NovatelConstants.NOVATEL_INSPVA_MESSAGE, gpsWeek, gpsSeconds);
        
        //split data portion of the message
        final String[] dataSplit = firstRunSplit[1].split(COMMA_SEPARATOR);
        //expecting 11 fields
        if (dataSplit.length < totalNumberOfDataFields)
        {
            throw new NovatelMessageException("The INSPVA data is missing fields! Message: " + data, 
                    FormatProblem.INCOMPLETE_INS_MESSAGE);
        }
        //gather the values
        final Double lon = Doubles.tryParse(dataSplit[longitudePosistion]);
        final Double lat = Doubles.tryParse(dataSplit[latitudePosition]);
        final Double alt = Doubles.tryParse(dataSplit[altitudePosition]);
        final Double azi = Doubles.tryParse(dataSplit[headingPosition]);
        final Double pitch = Doubles.tryParse(dataSplit[elevationPosition]);
        final Double roll = Doubles.tryParse(dataSplit[bankPosition]);
        
        //check that the values were parseable
        checkArgsForNull("Double", NovatelConstants.NOVATEL_INSPVA_MESSAGE, lon, lat, alt, azi, pitch, roll);
        return new NovatelInsMessage(lon, lat, alt, azi, pitch, roll, getTime(gpsWeek, gpsSeconds.intValue()));
    }

    @Override
    public void evaluateTimeMessage(final String data) throws NovatelMessageException
    {
        //index of the UTC offset
        final int utcOffsetPos = 3;
        final int expectedNumberOfDataFields = 11;
        
        final String[] firstRunSplit = data.split(SEMI_COLON_SEPARATOR);
        if (firstRunSplit.length < 2)
        {
            throw new NovatelMessageException("The TIME message is NOT complete! Message: " + data, 
                    FormatProblem.INCOMPLETE_TIME_MESSAGE);
        }
        //check that the time is valid
        if (firstRunSplit[1].contains("INVALID"))
        {
            throw new NovatelMessageException("The TIME message is not valid, as reported by the unit.", 
                    FormatProblem.TIME_RELIABILITY);
        }

        final String[] dataSplit = firstRunSplit[1].split(COMMA_SEPARATOR);
        //check that the time is valid
        if (dataSplit.length < expectedNumberOfDataFields)
        {
            throw new NovatelMessageException("The TIME message is not complete. Message: " + data, 
                    FormatProblem.INCOMPLETE_TIME_MESSAGE);
        }
        final Double utcOffset = Doubles.tryParse(dataSplit[utcOffsetPos]);
        checkArgsForNull("Double UTC-OFFSET", "TIMEA", utcOffset);
        //UTC time
        m_Offset = utcOffset.longValue();
    }

    @Override
    public boolean isOffsetKnown()
    {
        if (m_Offset == null)
        {
            return false;
        }
        return true;
    }
     
    /**
     * Check if arguments are null.
     * @param type
     *      the type of variable being checked
     * @param message
     *      the NovAtel message type from which the variable originated
     * @param objectsToCheck
     *      the objects to check if null
     * @throws NovatelMessageException
     *      if there is a null value, which represents a value which was not able to be parsed
     */
    private void checkArgsForNull(final String type, final String message, final Object... objectsToCheck) 
            throws NovatelMessageException
    {
        for (int i = 0; i < objectsToCheck.length; i++)
        {
            if (objectsToCheck[i] == null)
            {
                throw new NovatelMessageException(
                    String.format("The %s value at position %d, from NovAtel message [%s] is not a parseable value.", 
                        type, i, message), FormatProblem.PARSING_ERROR);
            }
        }
    }
    
    /**
     * Get the UTC time using the GPS week and GPS seconds.
     * @param gpsWeek
     *      the recorded GPS week
     * @param gpsSeconds
     *      the recorded GPS week
     * @return
     *      the UTC time
     * @throws NovatelMessageException 
     *      if the UTC offset is not known
     */
    private long getTime(final int gpsWeek, final int gpsSeconds) throws NovatelMessageException
    {
        if (!isOffsetKnown())
        {
            throw new NovatelMessageException("Unable to translate the time for an INSPVA message,"
                + " as the offset for GPS time to UTC is not available.", FormatProblem.UTC_OFFSET_UNKNOWN);
        }
        //the following values are the date from which GPS weeks start
        final int gpsStartYear = 1980;
        final int gpsStartMonth = 0;
        final int gpsStartDate = 6;
        final int gpsStartHour = 0;
        final int gpsStartMinutes = 0;
        final int gpsStartSeconds = 0;
        
        //calendar instance to hold the above date
        final Calendar cal = Calendar.getInstance();
        //call clear to unset all fields
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(gpsStartYear, gpsStartMonth, gpsStartDate, gpsStartHour, gpsStartMinutes, gpsStartSeconds);

        //add the GPS weeks and seconds to the calendar time
        cal.add(Calendar.WEEK_OF_YEAR, gpsWeek);
        
        cal.add(Calendar.SECOND, gpsSeconds);
        
        cal.add(Calendar.SECOND, Ints.checkedCast(m_Offset));
        
        return cal.getTimeInMillis();
    }
}
