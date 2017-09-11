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
package mil.dod.th.ose.bbb.platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

/**
 * Service used to manage periodic, time based tasks.
 * 
 * @author cweisenborn
 */
@Component(provide = BeagleBoneBlackTimerManager.class)
public class BeagleBoneBlackTimerManager
{
    private ScheduledExecutorService m_Executor;
    
    /**
     * Activate method. Responsible for setting up the scheduler.
     */
    @Activate
    public void activate()
    {
        m_Executor = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Deactivation method. Responsible for canceling all scheduled tasks.
     * 
     * @throws InterruptedException
     *      if interrupted while waiting for tasks to terminate
     */
    @Deactivate
    public void deactivate() throws InterruptedException
    {
        m_Executor.shutdown();
        m_Executor.awaitTermination(2, TimeUnit.SECONDS);
    }
    
    /**
     * Scheduled the specified task a fixed rate.
     * 
     * @param task
     *      {@link Runnable} to be scheduled at a fixed rate.
     * @param delayMs
     *      Delay in milliseconds before the task should start.
     * @param periodMs
     *      Fixed rate in milliseconds between calls to the task.
     */
    public void addScheduledAtFixedRateTask(final Runnable task, final long delayMs, final long periodMs)
    {
        m_Executor.scheduleWithFixedDelay(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
    }
}
