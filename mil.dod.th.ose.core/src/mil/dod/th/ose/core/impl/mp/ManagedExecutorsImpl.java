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
package mil.dod.th.ose.core.impl.mp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.ose.shared.ExceptionLoggingThreadPool;
import mil.dod.th.ose.shared.ScheduledExceptionLoggingThreadPool;

/**
 * Implementation of the {@link ManagedExecutors} service, registered as an OSGi service for access by script.
 * 
 * @author Dave Humeniuk
 *
 */
@Component
public class ManagedExecutorsImpl implements ManagedExecutors
{    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Log;

    
    /**
     * List of executor services that have been created, but not shutdown yet. 
     */
    final private List<ExecutorService> m_ActiveExecutors = 
        Collections.synchronizedList(new ArrayList<ExecutorService>());
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Log = logging;
    }

    
    @Override
    public ExecutorService newCachedThreadPool()
    {
        // the synchronous queue does not hold tasks, instead it immediately moves tasks onto available threads,
        // blocking the producer until a consumer becomes available.
        final BlockingQueue<Runnable> workQueue = new SynchronousQueue<Runnable>();
        // the thread pool is defined so that an unbounded number of threads can be created but any thread that is idle
        // more than 60 seconds will be killed.
        final ExecutorService service = 
                 new ExceptionLoggingThreadPool(m_Log, 0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, workQueue);
        m_ActiveExecutors.add(service);
        return service;
    }

    @Override
    public ExecutorService newFixedThreadPool(final int nThreads)
    {
        // the linked blocking queue holds an unbounded number of tasks in FIFO order. The queue will only send tasks
        // to the thread pool when a thread is available.
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
        // the fixed thread pool has a size of nThreads and never adds or times out threads.
        final ExecutorService service = 
                new ExceptionLoggingThreadPool(m_Log, nThreads, nThreads, 0L, TimeUnit.SECONDS, workQueue);
        m_ActiveExecutors.add(service);
        return service;
    }

    @Override
    public ScheduledExecutorService newScheduledThreadPool(final int corePoolSize)
    {
        // the scheduled thread pool uses a fixed pool size of corePoolSize and an unbounded work queue
        final ScheduledExecutorService service = new ScheduledExceptionLoggingThreadPool(m_Log, corePoolSize);
        m_ActiveExecutors.add(service);
        return service;
    }

    @Override
    public ExecutorService newSingleThreadExecutor()
    {
        // the linked blocking queue is holds an unbounded number of tasks in FIFO order.
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
        // tasks will be sent to the thread one at a time as the thread become available.
        final ExecutorService service = new ExceptionLoggingThreadPool(m_Log, 1, 1, 0, TimeUnit.SECONDS, workQueue);
        m_ActiveExecutors.add(service);
        return service;
    }

    @Override
    public ScheduledExecutorService newSingleThreadScheduledExecutor()
    {
        // an unbounded working queue is used to hold tasks in FIFO order before executing them on a single thread.
        final ScheduledExecutorService service = new ScheduledExceptionLoggingThreadPool(m_Log, 1);
        m_ActiveExecutors.add(service);
        return service;
    }

    @Override
    public void shutdownAllExecutorServices()
    {
        synchronized (m_ActiveExecutors)
        {
            for (ExecutorService service : m_ActiveExecutors)
            {
                service.shutdown();
            }
            
            // keep list intact to allow call to shutdown now
        }
    }

    @Override
    public void shutdownAllExecutorServicesNow()
    {
        synchronized (m_ActiveExecutors)
        {
            for (ExecutorService service : m_ActiveExecutors)
            {
                service.shutdownNow();
            }
            
            m_ActiveExecutors.clear();  // clear list, further calls to shutdown will have no effect
        }
    }
}
