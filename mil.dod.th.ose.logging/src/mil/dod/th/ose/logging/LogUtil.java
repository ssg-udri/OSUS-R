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
package mil.dod.th.ose.logging;

import java.util.Locale;

import mil.dod.th.ose.shared.LogLevel;

import org.apache.commons.lang.time.FastDateFormat;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * THOSE log writer service utilities.
 *
 * @author dlandoll
 */
public final class LogUtil
{
    /**
     * Date formatter used by the log writer.
     */
    private static final FastDateFormat FDF = FastDateFormat.getInstance("yyyyMMdd HH:mm:ss", Locale.US);

    /**
     * Singleton logging utility class.
     */
    private LogUtil()
    {
        // do nothing
    }

    /**
     * Get the native {@link LogLevel} from the level defined by {@link LogService}.
     * 
     * @param level
     *      {@link LogService} level to convert
     * @return
     *      native log level
     */
    public static LogLevel convertOsgiToNativeLevel(final int level)
    {
        switch (level)
        {
            case LogService.LOG_DEBUG:
                return LogLevel.Debug;
            case LogService.LOG_INFO:
                return LogLevel.Info;
            case LogService.LOG_WARNING:
                return LogLevel.Warning;
            case LogService.LOG_ERROR:
                return LogLevel.Error;
            default:
                throw new IllegalArgumentException(String.format("Level %d is not a valid log level", level));
        }
    }
    
    /**
     * Get the log level defined by {@link LogService} from the native {@link LogLevel}.
     * 
     * @param level
     *      native log level
     * @return
     *      {@link LogService} level
     */
    public static int convertNativeToOsgiLevel(final LogLevel level)
    {
        switch (level)
        {
            case Debug:
                return LogService.LOG_DEBUG;
            case Info:
                return LogService.LOG_INFO;
            case Warning:
                return LogService.LOG_WARNING;
            case Error:
                return LogService.LOG_ERROR;
            default:
                throw new IllegalArgumentException(String.format("Level %s is not a valid log level", level));
        }
    }

    /**
     * Converts a log entry to String.
     *
     * @param level
     *      Log level of the message, see {@link LogService#log(int, String)}.
     * @param time
     *      Time in milliseconds based on {@link System#currentTimeMillis()} when the log event occurred
     * @param bundle
     *      Source of the log event
     * @param serviceRef
     *      The <code>ServiceReference</code> object of the service that this message is associated with
     * @param message
     *      Log event message
     * @param except
     *      Exception related with the cause of the log message
     * @param logExceptions
     *            A boolean flag that if true adds the exception stack trace to the log message.
     * @return Readable string containing elements from the log entry
     */
    public static String fromLogEntry(final int level, final long time, final Bundle bundle, 
            final ServiceReference<?> serviceRef, final String message, final Throwable except, 
            final boolean logExceptions)
    {
        final StringBuilder stringBuilder = new StringBuilder(100);

        // Add the log level of the entry
        stringBuilder.append(String.format("%-9S", convertOsgiToNativeLevel(level)));

        // Get the time stamp
        stringBuilder.append(FDF.format(Long.valueOf(time)));

        // Check for bundle information
        stringBuilder.append(" ID#");
        if (bundle != null)
        {
            stringBuilder.append(bundle.getBundleId());
        }

        // Move the start of the message out to a fixed column
        if (stringBuilder.length() < 32) // NOCHECKSTYLE: Magic number: Hard coded value used to format the message
        {
            stringBuilder.append("   ");
            stringBuilder.setLength(32); // NOCHECKSTYLE: Magic number: Hard coded value used to format the message
        }

        // Check for a service reference
        stringBuilder.append("- ");
        if (serviceRef != null)
        {
            stringBuilder.append('[');
            stringBuilder.append(serviceRef.getProperty(Constants.SERVICE_ID).toString());
            stringBuilder.append(';');
            final String[] clazzes = (String[])serviceRef.getProperty(Constants.OBJECTCLASS);
            for (int i = 0; i < clazzes.length; i++)
            {
                if (i > 0)
                {
                    stringBuilder.append(',');
                }
                stringBuilder.append(clazzes[i]);
            }
            stringBuilder.append("] ");
        }

        // Add the log message
        stringBuilder.append(message);

        // Check if logging of exceptions is enabled
        if (logExceptions && except != null)
        {
            stringBuilder.append('\n');
            
            // build traces together in separate builder so each cause message can be added as we go through the chain
            final StringBuilder traceStringBuilder = new StringBuilder(100);
            
            // Add the trace information
            addExceptionTrace(except, traceStringBuilder);
            stringBuilder.append(except.getMessage());

            // Iterate through any nested exceptions
            Throwable cause = except.getCause();
            while (cause != null)
            {
                // Add the trace information
                addExceptionTrace(cause, traceStringBuilder);
                if (cause.getMessage() != null)
                {
                    stringBuilder.append(": ");  // NOCHECKSTYLE: repeated string, simple delimiter, can be different
                    stringBuilder.append(cause.getMessage());
                }
                
                // Get the next cause
                cause = cause.getCause();
            }
            stringBuilder.append('\n');
            stringBuilder.append(traceStringBuilder.toString());
        }

        return stringBuilder.toString();
    }

    /**
     * Creates an exception trace using the given {@link StringBuilder} reference.
     *
     * @param except
     *            Exception being logged.
     * @param stringBuilder
     *            String builder used in creating the log message.
     */
    private static void addExceptionTrace(final Throwable except, final StringBuilder stringBuilder)
    {
        stringBuilder.append("Caused by: ");

        final Class<?> exceptClass = except.getClass();
        if (exceptClass == null)
        {
            stringBuilder.append("*Unknown Exception*");
        }
        else
        {
            stringBuilder.append(exceptClass.getName());
        }

        final String exceptionMessage = except.getMessage();
        if (exceptionMessage != null)
        {
            stringBuilder.append(": ");
            stringBuilder.append(exceptionMessage);
        }

        stringBuilder.append('\n');
        final int maxTraceSize = 15;
        for (int i = 0; i < except.getStackTrace().length && i < maxTraceSize; i++)
        {
            final StackTraceElement element = except.getStackTrace()[i];
            stringBuilder.append("    at ");
            stringBuilder.append(element.toString());
            stringBuilder.append('\n');
        }
        
        if (except.getStackTrace().length > maxTraceSize)
        {
            stringBuilder.append("    ... ");
            stringBuilder.append(except.getStackTrace().length - maxTraceSize);
            stringBuilder.append(" more\n");
        }
    }
}
