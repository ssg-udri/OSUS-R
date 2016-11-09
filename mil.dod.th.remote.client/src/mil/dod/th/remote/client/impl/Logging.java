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
package mil.dod.th.remote.client.impl;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.osgi.service.log.LogService;

/**
 * This class provides a wrapper for the  OSGi {@link LogService} and also handles printing to stdout when a
 * {@link LogService} is not available.
 * 
 * @author dlandoll
 */
@Component(provide = Logging.class)
public class Logging
{
    private LogService m_LogService;

    @Reference(optional = true)
    public void setLogService(final LogService logService)
    {
        m_LogService = logService;
    }

    /**
     * Log a debug message through the log service.  Message will be constructed by putting the <code>args</code> into
     * the <code>format</code> string.
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void debug(final String format, final Object... args)
    {
        log(LogService.LOG_DEBUG, format, args);
    }

    /**
     * Log an info message through the log service.  Message will be constructed by putting the <code>args</code> into
     * the <code>format</code> string.
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void info(final String format, final Object... args)
    {
        log(LogService.LOG_INFO, format, args);
    }

    /**
     * Log a warning message through the log service.  Message will be constructed by putting the <code>args</code> into
     * the <code>format</code> string.
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void warning(final String format, final Object... args)
    {
        log(LogService.LOG_WARNING, format, args);
    }

    /**
     * Log a warning exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void warning(final Throwable exception, final String format, final Object... args)
    {
        log(LogService.LOG_WARNING, exception, format, args);
    }

    /**
     * Log an error message through the log service.  Message will be constructed by putting the <code>args</code> into
     * the <code>format</code> string.
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}. The format string should be a constant and not a
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void error(final String format, final Object... args)
    {
        log(LogService.LOG_ERROR, format, args);
    }

    /**
     * Log an error exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    public void error(final Throwable exception, final String format, final Object... args)
    {
        log(LogService.LOG_ERROR, exception, format, args);
    }

    /**
     * Log a message through the log service.  Message will be constructed by putting the <code>args</code> into the
     * <code>format</code> string.
     * 
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String)}.
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     */
    private void log(final int level, final String format, final Object[] args)
    {
        if (m_LogService == null)
        {
            System.out.println(String.format(format, args)); // NOPMD: Default output when OSGi log service is missing
        }
        else
        {
            m_LogService.log(level, String.format(format, args));
        }
    }

    /**
     * Log an exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String, Throwable)}.
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.
     * @param args
     *      Argument list that will be inserted into the format string
     */
    private void log(final int level, final Throwable exception, final String format, final Object[] args)
    {
        if (m_LogService == null)
        {
            System.out.println(String.format(format, args)); // NOPMD: Default output when OSGi log service is missing
            exception.printStackTrace(); // NOPMD: Default output when OSGi log service is missing
        }
        else
        {
            m_LogService.log(level, String.format(format, args), exception);
        }
    }
}
