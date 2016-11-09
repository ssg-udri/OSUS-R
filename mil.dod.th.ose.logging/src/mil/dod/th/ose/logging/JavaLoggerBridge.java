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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;


/**
 * {@link Handler} that publishes records to the Terra Harvest {@link LoggingService}.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class JavaLoggerBridge extends Handler
{
    /**
     * Name of the OSGi framework property containing the default behavior for this component.
     */
    private static final String ENABLED_FRAMEWORK_PROPERTY = "mil.dod.th.ose.logging.javaloggerbridge.enabled";
    
    /**
     * service for directing JUL records to Terra Harvest. 
     */
    private LoggingService m_Logging;

    /**
     * Binds the logging service to use for redirecting.  If service is not available, then bridge will not be active 
     * and JUL logs will go to console.
     * 
     * @param loggingService
     *      logging service to handle log events
     */
    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Logging = loggingService;
    }
    
    /**
     * Activate this component by removing all logger handlers and replace with this one.
     * 
     * @param context
     *      the bundle context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        //framework property
        final String prop = context.getProperty(ENABLED_FRAMEWORK_PROPERTY);
        
        //if the property from the context is null then use default of true
        final boolean enabled = prop == null ? true : Boolean.parseBoolean(prop);
        
        //if enabled is true then register this class as a logging handler
        if (enabled)
        {
            final Logger rootLogger = Logger.getLogger("");
            final Handler[] handlers = rootLogger.getHandlers().clone();
            
            for (Handler handler : handlers)
            {
                rootLogger.removeHandler(handler);
            }
            
            rootLogger.addHandler(this);
        }
    }
    
    @Override
    public void publish(final LogRecord record)
    {
        if (record.getThrown() == null)
        {
            m_Logging.log(leveltoLogServiceLevel(record.getLevel()), record.getMessage());            
        }
        else
        {
            m_Logging.log(leveltoLogServiceLevel(record.getLevel()), record.getThrown(), record.getMessage());
        }
    }

    @Override
    public void flush()
    {
        // do nothing
    }

    @Override
    public void close() throws SecurityException
    {
        // do nothing
    }
    
    
    /**
     * Convert a Java util logger (JUL) {@link Level} to a level usable by the {@link LogService}.
     * 
     * @param level
     *      level to convert from
     * @return
     *      converted {@link LogService} level
     */
    private int leveltoLogServiceLevel(final Level level)
    {
        if (level.intValue() > Level.WARNING.intValue())
        {
            return LogService.LOG_ERROR;
        }
        else if (level.intValue() == Level.WARNING.intValue())
        {
            return LogService.LOG_WARNING;
        }
        else if (level.intValue() < Level.WARNING.intValue() && level.intValue() >= Level.INFO.intValue())
        {
            return LogService.LOG_INFO;
        }
        else
        {
            return LogService.LOG_DEBUG;
        }
    }
}
