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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.log.LoggingService;

/**
 * An extension of {@link java.util.concurrent.ThreadPoolExecutor} that creates an ExecutorService using 
 * a thread pool. This service logs the exceptions that occur within the managed threads.
 * 
 * @author cashionk1
 */
public class ExceptionLoggingThreadPool extends ThreadPoolExecutor
{
    /**
     * Reference to the logging service to be used to log exceptions.
     */
    private final LoggingService m_Log;
    
    /**
     * Constructor for ExceptionLoggingThreadPool.
     * @param logging
     *         logger to be used to log exceptions
     * @param corePoolSize
     *         the core number of threads in the pool, new threads will always be added to bring the pool up to
     *         this size if a new task is queued
     * @param maximumPoolSize
     *         the maximum number of threads in the pool, new threads will be added above the core pool size up to
     *         this maximum if the queue is full
     *         set this value equal to corePoolSize to generate a fixed size thread pool
     * @param keepAliveTime
     *         if there more thread than the corePoolSize, idle threads will time out after this amount 
     *         of time passes.
     * @param unit
     *         the time unit for the keepAliveTime parameter
     * @param workQueue
     *         a blocking queue used to hold tasks while waiting for an available thread
     */
    public ExceptionLoggingThreadPool(final LoggingService logging, final int corePoolSize, final int maximumPoolSize, 
            final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        m_Log = logging;
    }

    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable)
    {
        super.afterExecute(runnable, throwable);
        ExceptionUtil.handleExecutorExceptions(m_Log, runnable, throwable);
    }
}
