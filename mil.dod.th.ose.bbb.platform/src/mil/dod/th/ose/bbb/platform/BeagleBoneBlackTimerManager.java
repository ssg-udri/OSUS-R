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

import java.util.Timer;
import java.util.TimerTask;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

/**
 * Service used to manager timer tasks.
 * 
 * @author cweisenborn
 */
@Component(provide = BeagleBoneBlackTimerManager.class)
public class BeagleBoneBlackTimerManager
{
    private Timer m_Timer;
    
    /**
     * Activate method. Responsible for setting up the timer.
     */
    @Activate
    public void activate()
    {
        m_Timer = new Timer();
    }
    
    /**
     * Deactivation method. Responsible for canceling all scheduled timer tasks.
     */
    @Deactivate
    public void deactivate()
    {
        m_Timer.cancel();
        m_Timer.purge();
    }
    
    /**
     * Scheduled the specified task a fixed rate.
     * 
     * @param task
     *      {@link TimerTask} to scheduled at a fixed rate.
     * @param delayMs
     *      Delay in milliseconds before the task should start.
     * @param periodMs
     *      Fixed rate in milliseconds between calls to the task.
     */
    public void addScheduledAtFixedRateTask(final TimerTask task, final long delayMs, final long periodMs)
    {
        m_Timer.scheduleAtFixedRate(task, delayMs, periodMs);
    }
}
