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
package mil.dod.th.ose.time.service.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Class to provide logging utility to all system time service classes.
 * @author nickmarcucci
 *
 */
public final class TimeChangeServiceLogger
{
    /**
     * The name of the log file that all output is logged to.
     */
    private static final String LOG_FILE_NAME = "timeserver.log";
    
    /**
     * Default name for the logger.
     */
    private static final String DEFAULT_LOGGER_NAME = "TimeServiceLogger";
    
    /**
     * The logger to log messages for the time service.
     */
    private static final Logger LOGGER = Logger.getLogger(DEFAULT_LOGGER_NAME);
    
    /**
     * Private Constructor.
     */
    private TimeChangeServiceLogger()
    {
        
    }
    
    /**
     * Function logs a message through JUL. 
     * @param level
     *  the level at which the message should be logged
     * @param message
     *  the message that is to be logged at this level
     */
    public static void logMessage(final Level level, final String message)
    {
        logMessage(level, message, null);
    }
    
    /**
     * Function logs a message through JUL with a specified exception.
     * @param level
     *  the level to log the message at which the message should be logged
     * @param message
     *  the message that is to be log
     * @param throwable
     *  an exception that may have occurred with the message
     */
    public static void logMessage(final Level level, final String message, final Throwable throwable)
    {
        if (throwable == null)
        {
            LOGGER.log(level, message);
        }
        else
        {
            LOGGER.log(level, message, throwable);
        }
    }
    
    /**
     * Sets up the JUL for use. Pass a null string if the location to be used is 
     * to be the default location (C:\timeservice.log). Otherwise, specify the full 
     * path to the log file. The log file will always be named timeservice.log
     * @param logLocation
     *  the location to which to write the log to.
     */
    public static void setupLogger(final String logLocation)
    {
        final SimpleFormatter formatter = new SimpleFormatter();
        
        try
        {
            final FileHandler fHandler;
            
            final File logFileLocation = new File(logLocation);
            if (!logFileLocation.exists())
            {
                logFileLocation.mkdirs();
            }
            
            fHandler = new FileHandler(logLocation + LOG_FILE_NAME);
           
            fHandler.setFormatter(formatter);
           
            LOGGER.addHandler(fHandler);
        }
        catch (final SecurityException exception)
        {
            LOGGER.log(Level.SEVERE, "Could not initialize formatter for logger.", exception);
        }
        catch (final IOException exception)
        {
            LOGGER.log(Level.SEVERE, "Unable to create a new file handler.", exception);
        }
    }
    
}
