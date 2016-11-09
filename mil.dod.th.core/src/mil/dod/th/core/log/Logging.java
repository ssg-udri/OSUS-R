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

package mil.dod.th.core.log;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

/**
 * This class provides static access to the log service. This is a legacy API method and should not be used going 
 * forward.  Instead the {@link LogService} should be injected directly.
 */
@Component
public class Logging
{
    /**
     * Static logging marker. This clarifies that the log entry came from this static logging service.
     */
    private final static String STATIC_LOG_MARKER = "!";
    
    /**
     * Reference to log service object.
     */
    private static LogService m_LogService;

    /**
     * Used to synchronize access to {@link #m_LogService}.
     */
    private static Object m_LogServiceLock = new Object();
    
    /**
     * Bind the log service to this component.
     * 
     * @param logService
     *      Log service object
     */
    @Reference (optional = true, dynamic = true)
    public void setLogService(final LogService logService)
    {
        synchronized (m_LogServiceLock)
        {
            m_LogService = logService;
        }
    }
    
    /**
     * Unbind the log service to this component.
     * 
     * @param logService
     *      parameter not used, must match binding method signature
     */
    public void unsetLogService(final LogService logService)
    {
        synchronized (m_LogServiceLock)
        {
            m_LogService = null; // NOPMD: NullAssignment, Must assign to null as field is checked before using
        }
    }

    /** Bind the log listener to this component.
     * 
     * @param logListener
     *      any log listener, could be multiple
     */
    @Reference
    public void setLogListener(final LogListener logListener)
    {
        // only here to make this component depend on the LogListener to ensure somebody is listening before logging to
        // the log service
    }
    
    /**
     * Log a message through the log service. Message will be constructed by putting the <code>args</code> into the
     * <code>format</code> string.
     * 
     * This method should only be used if not able to use the {@link LogService} directly with the
     * <code>LoggingService</code> class.
     * 
     * @param level
     *            Log level of the message, see {@link LogService#log(int, String)}.
     * @param format
     *            Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *            concatenation. If the log service is not available, the format string will not be evaluated, limiting
     *            processing.
     * @param args
     *            Argument list that will be inserted into the format string
     * @return Whether the message was logged (whether service is available)
     * 
     * @see String#format(String, Object...)
     */
    public static boolean log(final int level, final String format, final Object... args)
    {
        synchronized (m_LogServiceLock)
        {
            if (m_LogService == null)
            {
                logStdOut(level, format, args);
                return false;
            }
            else
            {
                m_LogService.log(level, STATIC_LOG_MARKER + String.format(format, args));
                return true;
            }
        }
    }

    /**
     * Log an exception message through the log service. Message will be constructed by putting the <code>args</code>
     * into the <code>format</code> string.
     * 
     * This method should only be used if not able to use the {@link LogService} directly with the
     * <code>LoggingService</code> class.
     * 
     * @param level
     *            Log level of the message, see {@link LogService#log(int, String)}.
     * @param exception
     *            Exception related with the cause of the log message
     * @param format
     *            Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *            concatenation. If the log service is not available, the format string will not be evaluated, limiting
     *            processing.
     * @param args
     *            Argument list that will be inserted into the format string
     * @return Whether the message was logged (whether service is available)
     * 
     * @see String#format(String, Object...)
     */
    public static boolean log(final int level, final Throwable exception, final String format, final Object... args)
    {
        synchronized (m_LogServiceLock)
        {
            if (m_LogService == null)
            {
                logStdOut(level, format, args);
                return false;
            }
            else
            {
                m_LogService.log(level, STATIC_LOG_MARKER + String.format(format, args), exception);
                return true;
            }
        }
    }
    
    /**
     * Log a message directly to standard out.  Should only be used if the {@link LogService} is unavailable.
     * 
     * @param level
     *      Log level of the message, see {@link LogService#log(int, String)}.
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     */
    private static void logStdOut(final int level, final String format, final Object[] args)
    {
        final String message = String.format(format, args);
        final String logMessage = String.format("!!! %d %d %s", System.currentTimeMillis(), level, message); 
        System.out.println(logMessage); // NOPMD: allow system print as logging is not available 
    }
}
