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
package mil.dod.th.ose.shared;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import mil.dod.th.core.log.LoggingService;

/**
 * Utility class used to deal with exceptions.
 * 
 * @author cweisenborn
 */
public final class ExceptionUtil
{
    /**
     * Private constructor to prevent instantiation.
     */
    private ExceptionUtil()
    {
        
    }
    
    /**
     * Exception logging method used to log an exception thrown by a executor that has completed.
     * 
     * @param logService
     *         the logging service to be used to log the exception
     * @param runnable
     *         the completed runnable
     * @param throwable
     *         the exception or null if the runnable terminated normally
     */
    public static void handleExecutorExceptions(final LoggingService logService, final Runnable runnable, 
            final Throwable throwable) 
    {
        Throwable exception = throwable;
        if (exception == null && runnable instanceof Future<?>) 
        {
            try 
            {
                final Future<?> future = (Future<?>) runnable;
                if (future.isDone())
                {
                    future.get();
                }
            } 
            catch (final CancellationException ce) 
            {
                exception = ce;
            } 
            catch (final ExecutionException ee) 
            {
                exception = ee.getCause();
            }
            catch (final InterruptedException ie) 
            {
                exception = ie;
            }
        }
        if (exception != null)
        {
            logService.error(exception, "Executor threw an exception");
        }
    }
}
