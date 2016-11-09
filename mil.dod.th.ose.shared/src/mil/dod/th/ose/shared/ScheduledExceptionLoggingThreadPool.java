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

import java.util.concurrent.ScheduledThreadPoolExecutor;

import mil.dod.th.core.log.LoggingService;

/**
 * An extension of {@link java.util.concurrent.ScheduledThreadPoolExecutor} that creates a ScheduledExecutorService
 * using a thread pool. This service logs the exceptions that occur within the managed threads.
 * 
 * @author cashionk1
 */

public class ScheduledExceptionLoggingThreadPool extends ScheduledThreadPoolExecutor
{
    /**
     * Reference to the logging service to be used to log exceptions.
     */
    private final LoggingService m_Log;
    
    /**
     * Constructor for ScheduledExceptionLoggingThreadPool. 
     * This thread pool is fixed and has an unbounded work queue.
     * 
     * @param logging
     *         logger to be used to log exceptions
     * @param corePoolSize
     *         the number of threads in the pool
     */
    public ScheduledExceptionLoggingThreadPool(final LoggingService logging, final int corePoolSize)
    {
        super(corePoolSize);
        m_Log = logging;
    }

    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable)
    {
        super.afterExecute(runnable, throwable);
        ExceptionUtil.handleExecutorExceptions(m_Log, runnable, throwable);
    }
}
