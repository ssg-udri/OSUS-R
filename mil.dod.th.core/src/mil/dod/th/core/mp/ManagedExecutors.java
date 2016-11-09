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
package mil.dod.th.core.mp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface mirrors the {@link java.util.concurrent.Executors} service, but manages all new {@link ExecutorService} so 
 * they can all be directed to shutdown at the same time.  This service must be used by mission programs when 
 * multi-threading or concurrency is needed.  Mission programs should not use {@link Thread}s or {@link 
 * java.util.Timer}s directly.  Using this service, the system can then request all mission program activity to cease. 
 * Service can be used outside of mission programming, but then the {@link ExecutorService} is susceptible to being 
 * shutdown by this service.
 * <p>
 * As long as there is a strong reference to an {@link ExecutorService} created by this service, the service will be 
 * able to shut it down.  If the {@link ExecutorService} is no longer referenced, then this service should not reference
 * it either.
 * <p>
 * Service only mirrors the main methods for get a new {@link ExecutorService} and does not support custom {@link 
 * java.util.concurrent.ThreadFactory}s.
 * <p>
 * Interface is provided as an OSGi service.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface ManagedExecutors
{
    /**
     * Creates a managed cached thread pool.
     * 
     * @return  service that tasks can be submitted to, but can be shutdown using this service 
     * 
     * @see java.util.concurrent.Executors#newCachedThreadPool()
     */
    ExecutorService newCachedThreadPool();
    
    /**
     * Creates a managed fixed thread pool.
     * 
     * @param nThreads
     *          the number in threads in the pool
     * @return  service that tasks can be submitted to, but can be shutdown using this service 
     * 
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     */
    ExecutorService newFixedThreadPool(int nThreads);
    
    /**
     * Creates a managed scheduled thread pool.
     * 
     * @param corePoolSize
     *          the number of threads to keep in the pool, even if they are idle
     * @return  service that tasks can be submitted to run in a scheduled way, but can be shutdown using this service 
     * 
     * @see java.util.concurrent.Executors#newScheduledThreadPool(int)
     */
    ScheduledExecutorService newScheduledThreadPool(int corePoolSize);
    
    /**
     * Creates a managed single thread executor.
     * 
     * @return  service that tasks can be submitted to, but can be shutdown using this service 
     * 
     * @see java.util.concurrent.Executors#newSingleThreadExecutor()
     */
    ExecutorService newSingleThreadExecutor();
    
    /**
     * Creates a managed single thread scheduled executor.
     * 
     * @return  service that tasks can be submitted to run in a scheduled way, but can be shutdown using this service 
     * 
     * @see java.util.concurrent.Executors#newSingleThreadScheduledExecutor()
     */
    ScheduledExecutorService newSingleThreadScheduledExecutor();
    
    /**
     * Invokes {@link ExecutorService#shutdown()} on all managed {@link ExecutorService}s.
     */
    void shutdownAllExecutorServices();
    
    /**
     * Invokes {@link ExecutorService#shutdownNow()} on all managed {@link ExecutorService}s. 
     */
    void shutdownAllExecutorServicesNow();
}
