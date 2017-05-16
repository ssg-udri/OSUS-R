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

import aQute.bnd.annotation.ProviderType;

import org.osgi.framework.ServiceReference;

/**
 * This interface is similar to the {@link org.osgi.service.log.LogService} provided by OSGi, but contains helper 
 * methods.
 * 
 * @author dhumeniuk
 * 
 */
@ProviderType
public interface LoggingService
{
    /**
     * Log a message through the log service.  Message will be constructed by putting the <code>args</code> into the
     * <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String)}.
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void log(int level, String format, Object... args);
    
    /**
     * Log an exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String, Throwable)}.
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void log(int level, Throwable exception, String format, Object... args);
    
    /**
     * Log a message through the log service.  Message will be constructed by putting the <code>args</code> into the
     * <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param reference
     *      The <code>ServiceReference</code> object of the service that this message is associated with
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String)}.
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void log(ServiceReference<?> reference, int level, String format, Object... args);
    
    /**
     * Log an exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param reference
     *      The <code>ServiceReference</code> object of the service that this message is associated with
     * @param level
     *      Log level of the message, see {@link org.osgi.service.log.LogService#log(int, String)}.
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void log(ServiceReference<?> reference, int level, Throwable exception, String format, 
            Object... args);
    
    /**
     * Log a debug message through the log service.  Message will be constructed by putting the <code>args</code> into 
     * the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void debug(String format, Object... args);
    
    /**
     * Log an info message through the log service.  Message will be constructed by putting the <code>args</code> into 
     * the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void info(String format, Object... args);
    
    /**
     * Log a warning message through the log service.  Message will be constructed by putting the <code>args</code> into
     * the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void warning(String format, Object... args);
    
    /**
     * Log a warning exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void warning(Throwable exception, String format, Object... args);

    /**
     * Log an error message through the log service.  Message will be constructed by putting the <code>args</code> into 
     * the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void error(String format, Object... args);

    /**
     * Log an error exception message through the log service.  Message will be constructed by putting the 
     * <code>args</code> into the <code>format</code> string.
     * 
     * <b>Thread safe</b>
     * 
     * @param exception
     *      Exception related with the cause of the log message
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    void error(Throwable exception, String format, Object... args);
}
