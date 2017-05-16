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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.shared.LogLevel;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.apache.commons.lang.time.FastDateFormat;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * <code>LogWriter</code> implements {@link LogListener} and prints log entries to STDOUT and/or file.
 * 
 * @author dlandoll
 */
@Component(immediate = true, designate = LogWriterConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class LogWriter implements LogListener
{
    /**
     * Name of the OSGi framework property containing the directory to use for log files.
     */
    public static final String LOG_DIR_PROPERTY = "mil.dod.th.ose.logging.dir";

    /**
     * Name of the OSGi framework property that enables logging to STDOUT.
     */
    public static final String LOG_STDOUT_PROPERTY = "mil.dod.th.ose.logging.stdout";

    /**
     * Name of the OSGi framework property that enables logging of exceptions.
     */
    public static final String LOG_EXCEPTIONS_PROPERTY = "mil.dod.th.ose.logging.exceptions";

    /**
     * Name of the OSGi framework property containing the default log level.
     */
    public static final String LOG_LEVEL_PROPERTY = "mil.dod.th.ose.logging.logLevel";
    
    /**
     * Name of the OSGi framework property containing the default days a log file is alive.
     */
    public static final String LOG_DAYS_ALIVE_PROPERTY = "mil.dod.th.ose.logging.logDaysAlive";

    /**
     * Name of the OSGi framework property containing the default log file size.
     */
    public static final String LOG_SIZE_PROPERTY = "mil.dod.th.ose.logging.logMBSizeLimit";
    
    /**
     * Name of the OSGI framework property containing the default maximum log file count.
     */
    public static final String LOG_FILE_COUNT_PROPERTY = "mil.dod.th.ose.logging.logMaxFileCount";

    /**
     * Name of the OSGi framework property containing the default bundle filter value.
     */
    public static final String LOG_FILE_BUFFER_SIZE_PROPERTY = "mil.dod.th.ose.logging.fileBufferSize";

    /**
     * Name of the OSGi framework property prefix containing the bundle filter profile value(s). Actual profiles
     * must be numbered (sequentially).
     */
    public static final String LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY = "mil.dod.th.ose.logging.filter.profile";

    /**
     * Prefix for log files.
     */
    private static final String LOG_FILE_PREFIX = "those_";
    
    /**
     * Extension for log files.
     */
    private static final String LOG_FILE_EXTENSION = ".log";
    
    /**
     * Reference to the OSGi log reader service.
     */    
    private LogReaderService m_LogReader;

    /**
     * Directory used to store log files.
     */
    private File m_LogFileDir;

    
    /**
     * Encapsulates the file output stream when logging to a file is enabled. 
     * A buffered output stream is used in place of print writer, as this allows direct access to setting
     * and adjusting the buffer size. 
     */
    
    private BufferedOutputStream m_LogFileWriter;
    
    /**
     * Property to hold buffer size.
     */
    private int m_LogFileBuffer;

    /**
     * Flag used to indicate whether log messages should be sent to System.out.
     */
    private boolean m_LogStdOut;

    /**
     * Flag used to indicate whether exceptions should be logged.
     */
    private boolean m_LogExceptions;

    /**
     * Configured logging level.
     */
    private int m_LogLevel;
    
    /**
     * List that holds the log filters known to this service. 
     */
    private List<LogFilter> m_LogFilterList;
    
    /**
     * The number of days a single log file can be used. In the event that the system is 
     * restarted, regardless of this setting a new log file will be made.
     */
    private int m_LogDaysAlive;
    
    /**
     * The size limit in bytes that a log file can be. Once this limit is reached a new log will be started.
     */
    private long m_LogMaxByteLimit;
    
    /**
     * The maximum amount of log files kept in the designated log directory. When there are more logs than this
     * number, the oldest log file will be deleted. 0 represents no maximum amount of log files.
     */
    private int m_LogMaxFileCount;
    
    /**
     * The log file name.
     */
    private File m_LogFile;
        
    /**
     * Property that holds the date of the next log creation.
     */
    private Calendar m_NextCreationDate;
    
    /**
     * String to hold operating system independent line separator.
     */
    final private String m_LineSeparator;

    /**
     * The bundle context from the bundle containing this component.
     */
    private BundleContext m_Context;
    
    /**
     * <code>LogWriter</code> constructor.
     */
    public LogWriter()
    {
        m_LogStdOut = true;
        m_LogExceptions = true;        
        m_LineSeparator = System.getProperty("line.separator");
        m_LogFilterList = new ArrayList<>();
    }

    /**
     * Bind the log filter instances.
     * @param logFilter
     *      instance of log filter to bind to this service
     */
    @Reference(dynamic = true, optional = true, multiple = true)
    public void setLogFilter(final LogFilter logFilter)
    {
        m_LogFilterList.add(logFilter);
    }
    
    /**
     * Unbind the given log filter instance.
     * @param logFilter
     *      instance of log filter to unbind from this service
     */
    public void unsetLogFilter(final LogFilter logFilter)
    {
        m_LogFilterList.remove(logFilter);
    }
    
    /**
     * Activate the component. At this point all service dependencies have been satisfied.
     * 
     * @param context
     *            OSGi bundle context
     * @param props
     *            Configuration Map
     */
    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        m_Context = context;
        updateProps(props);
        final String logdir = m_Context.getProperty(LOG_DIR_PROPERTY);
        if (logdir != null)
        {
            m_LogFileDir = new File(logdir);

            if (!m_LogFileDir.exists() && !m_LogFileDir.mkdir())
            {
                throw new IllegalStateException("Unable to create directory used to stored log files!");
            }
            openLogFile();
        }
        final String logStdOut = m_Context.getProperty(LOG_STDOUT_PROPERTY);
        if ((logStdOut != null) && !setLogStdOut(logStdOut))
        {
            System.err.println(String.format(// NOPMD: System.out is necessary for the logging implementation
                    "Invalid %s property key (%s).  LogStdOut defaulting to %s.  Valid values are TRUE or FALSE",
                    LOG_STDOUT_PROPERTY, logStdOut, m_LogStdOut));
        }

        final String logExceptions = m_Context.getProperty(LOG_EXCEPTIONS_PROPERTY);
        if ((logExceptions != null) && !setLogExceptions(logExceptions))
        {
            System.err.println(String.format(// NOPMD: System.out is necessary for the logging implementation
                    "Invalid %s property key (%s).  LogExceptions defaulting to %s.  Valid values are TRUE or FALSE",
                    LOG_EXCEPTIONS_PROPERTY, logExceptions, m_LogExceptions));
        }

        processFiltersFromFrameworkProps();
        
        // Get existing log entries (in order by newest) and log each entry
        @SuppressWarnings("unchecked")
        final Enumeration<LogEntry> logEntries = m_LogReader.getLog();
        final List<LogEntry> logList = Collections.list(logEntries);
        for (int i = logList.size() - 1; i >= 0; --i)
        {
            logged(logList.get(i));
        }
    }

    /**
     * Deactivate the component. Bundle stopped or needed services no longer available.
     * 
     */
    @Deactivate
    public void deactivate()
    {
        if (m_LogFileDir != null)
        {
            closeLogFile();
        }
    }

    /**
     * Provides {@link LogReaderService} reference that is available for use.
     * 
     * @param logReader
     *            Reference to the OSGi log reader service.
     */
    @Reference
    public void setLogReaderService(final LogReaderService logReader)
    {
        m_LogReader = logReader;

        // Listen for new log entries
        m_LogReader.addLogListener(this);
    }

    /**
     * Resets {@link LogWriterConfig} properties.
     * 
     * @param props
     *     key, value pairs to update the log writer configurations  
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        closeLogFile();
        updateProps(props);
        openLogFile();
    }
    
    /**
     * Provides {@link LogReaderService} reference that is no longer available for use.
     * 
     * @param logReader
     *            Reference to the OSGi log reader service.
     */
    public void unsetLogReaderService(final LogReaderService logReader)
    {
        // Stop listening for log entries
        logReader.removeLogListener(this);
    }

    @Override
    public void logged(final LogEntry entry)
    {
        final int level = entry.getLevel();
        // Log the entry if within the configured logging level
        if (level > m_LogLevel)
        {
            return;
        }
        //The bundle is needed for multiple operations
        final Bundle bundle = entry.getBundle();

        LogFilter filterMatch = null;
        //sort through filters
        for (LogFilter filter : m_LogFilterList)
        {
            if (filter.matches(bundle))
            {
                //found a match, keep it
                if (filterMatch == null)
                {
                    filterMatch = filter;
                }
                //replace the old one if this is true because we want to go with the largest number, which
                //means the least log level severity
                else if (filterMatch.getSeverity() < filter.getSeverity())
                {
                    filterMatch = filter;
                }
            }
        }
        if (filterMatch != null && filterMatch.getSeverity() < level)
        {
            //ignore the message as messages matching this bundle's symbolic name at the given level have been
            //requested to be ignored
            return;
        }
        
        // Convert log entry to a string buffer
        final String entryString = LogUtil.fromLogEntry(level, entry.getTime(), bundle,
                 entry.getServiceReference(), entry.getMessage(), entry.getException(), m_LogExceptions);
            
        // Send to STDOUT if enabled
        if (m_LogStdOut)
        {
            System.out.println(entryString); // NOPMD: System.out is necessary for the logging implementation
        }
        // Send to file if log file is opened
        if (m_LogFileWriter != null) 
        {
            try
            {
                // Send byte encoded string to buffered stream; a newline is added as the write method does not
                m_LogFileWriter.write((entryString + m_LineSeparator).getBytes());
            }
            catch (final IOException ex1)
            {
                System.err.println("Failed to write: " + entryString + " because of: " // NOPMD
                     + ex1.getMessage());
            }
            //check the size and age of the file
            checkLogSizeAndAge();
        }
    }

    /**
     * Sets the flag that controls logging to stdout. If the string parameter is invalid, flag is not changed.
     * 
     * @param logStdOut
     *            'true' or 'false', ignoring case, are valid strings
     * @return 'true' if property was valid, else 'false'
     */
    public boolean setLogStdOut(final String logStdOut)
    {
        if (Boolean.TRUE.toString().equalsIgnoreCase(logStdOut))
        {
            m_LogStdOut = true;
        }
        else if (Boolean.FALSE.toString().equalsIgnoreCase(logStdOut))
        {
            m_LogStdOut = false;
        }
        else
        {
            return false;
        }
        return true;
    }

    /**
     * Sets the flag that controls logging of exceptions. If the string parameter is invalid, flag is not changed.
     * 
     * @param logExceptions
     *            'true' or 'false', ignoring case, are valid strings
     * @return 'true' if property was valid, else 'false'
     */
    public boolean setLogExceptions(final String logExceptions)
    {
        if (Boolean.TRUE.toString().equalsIgnoreCase(logExceptions))
        {
            m_LogExceptions = true;
        }
        else if (Boolean.FALSE.toString().equalsIgnoreCase(logExceptions))
        {
            m_LogExceptions = false;
        }
        else
        {
            return false;
        }
        return true;
    }

    /**
     * Opens a new log file for writing. Log name is "Those_'Date'_'Time'.log".
     */
    private void openLogFile()
    {
        if (m_LogFileDir == null)
        {
            return;
        }
        final Locale locale = Locale.US;
        final FastDateFormat fileDateFormatter = FastDateFormat.getInstance("yyyyMMdd'_'HHmm", locale);
        final String fileName = LOG_FILE_PREFIX + fileDateFormatter.format(new Date()) + LOG_FILE_EXTENSION;
        m_LogFile = new File(m_LogFileDir, fileName);
        try
        {
            m_LogFileWriter = new BufferedOutputStream(new FileOutputStream(m_LogFile), m_LogFileBuffer);
            // Constructs the file header and appends a newline as the LogFileWriter's write method does not
            final FastDateFormat headerFomatter = FastDateFormat.getInstance("yyyyMMdd HH:mm:ss", locale);
            final String fileHeader = "*** THOSE log started " + headerFomatter.format(new Date()) + " ***" 
                + m_LineSeparator;
            m_LogFileWriter.write(fileHeader.getBytes());
            m_LogFileWriter.flush();
        }
        catch (final IOException ex)
        {
            // System.err is necessary for the logging implementation
            System.err.println("Failed to open logfile " + m_LogFile + " due to: " + ex.getMessage()); // NOPMD
            m_LogFileWriter = null; // NOPMD: Null assignment: Needed to identify that an error occurred
        }
        m_NextCreationDate = Calendar.getInstance();
        m_NextCreationDate.add(Calendar.DAY_OF_MONTH, m_LogDaysAlive);
    }

    /**
     * Closes the currently open log file.
     */
    private void closeLogFile()
    {
        if (m_LogFileWriter != null)
        {
            try
            {
                m_LogFileWriter.flush();
                m_LogFileWriter.close();
                m_LogFileWriter = null; // NOPMD: Null assignment: Needed to identify when logger has been closed
            }
            catch (final Exception ex2)
            {
                System.err.println("Failed to close logfile due to: " + ex2.getMessage()); // NOPMD
            }
        }
    } 
    
    /**
     * Sets Log writer configurations. 
     * 
     * @param props
     *    key, value pairs for setting the {@link LogWriterConfig} values  
     */
    private void updateProps(final Map<String, Object> props)
    {
        //check for properties, there is always the component name and id in the properties, if there is more than
        //that then assume they are configuration properties
        if (ConfigurationUtils.configPropertiesSet(props))
        {
            //configuration
            final LogWriterConfig config = Configurable.createConfigurable(LogWriterConfig.class, props);
            setLogFileBuffer(config.fileBufferSize());
            setLogDaysAlive(config.logDaysAlive());
            setLogSizeLimit(config.logMBSizeLimit());
            setLogLevel(config.logLevel());
            setLogMaxFileCount(config.logMaxFileCount());
        }
        else
        {
            //framework property for the buffer size
            final String bufferProp = m_Context.getProperty(LOG_FILE_BUFFER_SIZE_PROPERTY);

            //if the property from the context is null then use a default
            final int defaultBufferSize = 1;
            final int bufferSize = bufferProp == null ? defaultBufferSize : Integer.parseInt(bufferProp);
            setLogFileBuffer(bufferSize);

            //framework property for the number of days to use the same log file
            final String daysAliveProp = m_Context.getProperty(LOG_DAYS_ALIVE_PROPERTY);

            //if the property from the context is null then use a default
            final int defaultDaysAlive = 14;
            final int daysAlive = daysAliveProp == null ? defaultDaysAlive : Integer.parseInt(daysAliveProp); 
            setLogDaysAlive(daysAlive);

            //framework property for the maximum size a log file can be
            final String logSizeProp = m_Context.getProperty(LOG_SIZE_PROPERTY);

            //if the property from the context is null then use a default
            final int defaultLogSize = 1;
            final int logSize = logSizeProp == null ? defaultLogSize : Integer.parseInt(logSizeProp);
            setLogSizeLimit(logSize);
            
            //framework property for the maximum amount of log files
            final String maxLogsProp = m_Context.getProperty(LOG_FILE_COUNT_PROPERTY);
            
            //if the property from the context is null then use a default
            final int defaultMaxLogs = 25;
            final int maxLogs = maxLogsProp == null ? defaultMaxLogs : Integer.parseInt(maxLogsProp);
            setLogMaxFileCount(maxLogs);

            //framework property for log level
            final String levelProp = m_Context.getProperty(LOG_LEVEL_PROPERTY);

            //if the property from the context is null then use a default
            final LogLevel logLevel;
            try
            {
                logLevel = levelProp == null ? LogLevel.Debug : LogLevel.valueOf(levelProp);
                setLogLevel(logLevel);
            }
            catch (final IllegalArgumentException e)
            {
                //bad data
                setLogLevel(LogLevel.Debug);
                Logging.log(LogService.LOG_WARNING, "Invalid initial log level, using default [%s]", 
                        LogLevel.Debug.toString());
            }
            Logging.log(LogService.LOG_DEBUG, "Logging service is set to log level of [%s]", 
                       LogUtil.convertOsgiToNativeLevel(m_LogLevel));
        }
    }
    
    /**
     * Sets the FileWriter buffer size.
     * 
     * @param size
     *  the desired size of the log buffer
     */
    private void setLogFileBuffer(final int size)
    {
        m_LogFileBuffer = size;
    }

    /**
     * Sets the number of days that a single log is kept active for.
     * 
     * @param days
     *     the number of days to set the maximum life span to
     */
    private void setLogDaysAlive(final int days)
    {
        m_LogDaysAlive = days;
    }
    
    /**
     * Sets a size limit in bytes that a log file is allowed to be before a new log is started.
     * The value passed is MB, which is converted to bytes.
     * 
     * @param size
     *    the maximum size a log file can be in MB 
     */
    private void setLogSizeLimit(final long size)
    {
        final long megabyte = 1024L * 1024L;
        //convert to bytes to expedite the checking of the file size
        m_LogMaxByteLimit = size * megabyte; 
    }
    
    /**
     * Sets the amount of log files allowed before the oldest log files are deleted.
     * 
     * @param count
     *      the maximum amount of log files
     */
    private void setLogMaxFileCount(final int count)
    {
        m_LogMaxFileCount = count;
    }
    
    /**
     * Sets the log level. If the string parameter is invalid, log level is not changed.
     * 
     * @param logLevel
     *            1-4 are acceptable values for the log level  which represent "ERROR", "WARNING", "INFO", "DEBUG".
     */
    private void setLogLevel(final LogLevel logLevel)
    {
        m_LogLevel = LogUtil.convertNativeToOsgiLevel(logLevel);
    }
    
    /**
     * Method that checks the log file size and whether the date is past the configured log age date, 
     * once either of these limits are reached a new log will be started.
     */
    private void checkLogSizeAndAge()
    {
        if (m_LogFile.length() >= m_LogMaxByteLimit || Calendar.getInstance().compareTo(m_NextCreationDate) > 0)
        {
            closeLogFile();
            openLogFile();
            
            if (m_LogMaxFileCount != 0) 
            {
                checkNumberOfLogFiles(); 
            }
        }
    }
    
    /**
     * Method that checks the number of log files that are currently existing and determines whether or not the total 
     * number of files is greater than the maximum allowed. If the total is greater than the maximum, then the oldest 
     * log files in the designated log directory are deleted.
     */
    private void checkNumberOfLogFiles()
    {
        final int numberOfLogFiles = getAllLogFiles().length; 
        
        for (int timesToDelete = numberOfLogFiles - m_LogMaxFileCount; timesToDelete >= 1; timesToDelete--)
        {
            final File[] existingLogFiles = getAllLogFiles();
            File logToBeDeleted = existingLogFiles[0];
            
            for (int i = 1; i < existingLogFiles.length; i++)
            {
                final File logBeingChecked = existingLogFiles[i];
                
                logToBeDeleted = 
                        logBeingChecked.lastModified() > logToBeDeleted.lastModified() 
                        ? logToBeDeleted : logBeingChecked;
            }
            
            logToBeDeleted.delete();
        }
    }
    
    /**
     * Gets all existing files within the logs directory. 
     * 
     * @return
     *      an array of existing files.
     */
    private File[] getAllLogFiles()
    {
        return m_LogFileDir.listFiles((dir, name) -> //NOCHECKSTYLE: Variables can't be final in lambda
        {
            return name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION) ? true : false;
        });
    }
    
    /**
     * Read OSGi framework properties that begin with {@link #LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY}. Add a filter
     * for each valid property found. A valid property value is "name:severity" where "name" is the bundler filter
     * expression and "severity" is the {@link LogLevel} string equivalent. 
     */
    private void processFiltersFromFrameworkProps()
    {
        boolean moreFilters = true;
        int filterCounter = 1;
        while (moreFilters)
        {
            //use counter to sequentially acquire the profiles !using post access incrementing!
            final String filter = m_Context.getProperty(LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY + filterCounter++);
            //if null that means there are no more profiles defined
            if (filter == null)
            {
                moreFilters = false;
            }
            else
            {
                //create bundle filter
                final String[] splitFilter = filter.split(":");
                if (splitFilter.length != 2)
                {
                    //bad data
                    Logging.log(LogService.LOG_WARNING, "Invalid Log Filter: [%s]", filter);
                    continue;
                }
                final String symbolicNameFilter = splitFilter[0]; // split from config file entry
                final String severityLevel = splitFilter[1]; // split from config file entry
                final int logLevel;
                try
                {
                    logLevel = LogUtil.convertNativeToOsgiLevel(LogLevel.valueOf(severityLevel));
                }
                catch (final IllegalArgumentException e)
                {
                    //bad data
                    Logging.log(LogService.LOG_WARNING, "Invalid log log level within filter: [%s]", filter);
                    continue;
                }
                final LogFilter logFilter = createLogFilter(symbolicNameFilter, logLevel);
                m_LogFilterList.add(logFilter);
            }
        }
    }

    /**
     * Create a Log Filter with the given sym name and severity.
     * @param symName
     *      the symbolic name regex to be used for this log filter
     * @param severity
     *      the severity level as a string, will be translated into integer level during
     *      construction of instance
     * @return
     *      instance of a log filter
     */
    private LogFilter createLogFilter(final String symName, final int severity)
    {
        return new LogFilter()
        {
            @Override
            public boolean matches(final Bundle bundle)
            {
                return bundle.getSymbolicName().matches(symName);
            }
            
            @Override
            public int getSeverity()
            {
                return severity;
            }
        };
    }
}
