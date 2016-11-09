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

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link LoggingService}.
 * 
 * @author dhumeniuk
 */
@Component(servicefactory = true)
public class LoggingServiceImpl implements LoggingService
{
    /**
     * Log service object.
     */
    private LogService m_LogService;
    
    /**
     * Binds the logging service for logging messages.
     *  
     * @param logService
     *      OSGi log service object, argument required for OSGi binding method to define the service to bind
     */
    @Reference
    public void setLogService(final LogService logService)
    {
        // do nothing, just make sure this component cannot be used until the log service is available so activation
        // below will always work
    }
    
    /**
     * Activate the component by setting the log service based on the bundle using this component.
     * 
     * @param context
     *      context for this component
     */
    @Activate
    public void activate(final ComponentContext context)
    {
        final BundleContext usingBundleContext = context.getUsingBundle().getBundleContext();
        final ServiceReference<LogService> ref = usingBundleContext.getServiceReference(LogService.class);
        m_LogService = usingBundleContext.getService(ref);
    }
    
    /**
     * Deactivate the component by ungetting the log service based on the bundle using this component.
     * 
     * @param context
     *      context for this component
     */
    @Deactivate
    public void deactivate(final ComponentContext context)
    {
        final BundleContext usingBundleContext = context.getUsingBundle().getBundleContext();
        final ServiceReference<LogService> ref = usingBundleContext.getServiceReference(LogService.class);
        usingBundleContext.ungetService(ref);
    }
        
    @Override
    public void log(final int level, final String format, final Object... args)
    {
        m_LogService.log(level, String.format(format, args));
    }
    
    @Override
    public void log(final int level, final Throwable exception, final String format, final Object... args)
    {
        m_LogService.log(level, String.format(format, args), exception);
    }
    
    @Override
    public void log(final ServiceReference<?> reference, final int level, final String format, final Object... args)
    {
        m_LogService.log(reference, level, String.format(format, args));
    }
    
    @Override
    public void log(final ServiceReference<?> reference, final int level, final Throwable exception, 
            final String format, final Object... args)
    {
        m_LogService.log(reference, level, String.format(format, args), exception);
    }
    
    @Override
    public void debug(final String format, final Object... args)
    {
        log(LogService.LOG_DEBUG, format, args);
    }
    
    @Override
    public void info(final String format, final Object... args)
    {
        log(LogService.LOG_INFO, format, args);
    }
    
    @Override
    public void warning(final String format, final Object... args)
    {
        log(LogService.LOG_WARNING, format, args);
    }
    
    @Override
    public void warning(final Throwable exception, final String format, final Object... args)
    {
        log(LogService.LOG_WARNING, exception, format, args);
    }

    @Override
    public void error(final String format, final Object... args)
    {
        log(LogService.LOG_ERROR, format, args);
    }

    @Override
    public void error(final Throwable exception, final String format, final Object... args)
    {
        log(LogService.LOG_ERROR, exception, format, args);
    }
}
