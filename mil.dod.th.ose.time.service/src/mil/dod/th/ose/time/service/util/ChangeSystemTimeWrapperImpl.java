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
package mil.dod.th.ose.time.service.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import mil.dod.th.ose.time.service.logging.TimeChangeServiceLogger;

/**
 * The implementation of the {@link ChangeSystemTimeWrapper}.
 * @author nickmarcucci
 *
 */
public class ChangeSystemTimeWrapperImpl implements ChangeSystemTimeWrapper
{
    /**
     * Time format string.
     */
    private static final String TIME_FORMAT = "HH:mm:ss";
    
    /**
     * Date format string.
     */
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    
    /**
     * Time command that is to be executed.
     */
    private static final String TIME_COMMAND = "time";
    
    /**
     * Date command that is to be executed.
     */
    private static final String DATE_COMMAND = "date";
    
    /**
     * Base string that will launch a command prompt.
     */
    private static final String LAUNCH_CMD_PROMPT = "cmd /C";
    
    /**
     * The runtime service to use.
     */
    private final Runtime m_Runtime;
    
    /**
     * Constructor.
     */
    public ChangeSystemTimeWrapperImpl()
    {
        m_Runtime = Runtime.getRuntime();
    }
    
    @Override
    public void setSystemTime(final long timeToSet)
    {
        final Date date = new Date(timeToSet);
        
        final String commandPrefix = LAUNCH_CMD_PROMPT;
        final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        final String formatString = "%s %s %s";
        
        final String timeCmd = String.format(formatString, commandPrefix, TIME_COMMAND, timeFormat.format(date));
        final String dateCmd = String.format(formatString, commandPrefix, DATE_COMMAND, dateFormat.format(date));
        try
        {
            m_Runtime.exec(timeCmd);
            m_Runtime.exec(dateCmd);
        }
        catch (final IOException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "An error occurred trying to change the system time.", exception);
            return;
        }
        
        TimeChangeServiceLogger.logMessage(Level.INFO, "Set the system time to %s" + date.toString());
    }
}
