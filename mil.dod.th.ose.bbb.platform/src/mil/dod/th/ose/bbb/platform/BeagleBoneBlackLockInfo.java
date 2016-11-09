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

import mil.dod.th.core.pm.WakeLock;

/**
 * Class used to store information about a wake lock.
 * 
 * @author cweisenborn
 */
public class BeagleBoneBlackLockInfo
{
    private final WakeLock m_Lock;
    private final long m_StartTimeMs;
    private final long m_EndTimeMs;
    
    /**
     * Constructor.
     * 
     * @param lock
     *      Wake lock the information is associated with.
     * @param startTimeMs
     *      Start time for the wake lock.
     * @param endTimeMs
     *      End time for the wake lock.
     */
    BeagleBoneBlackLockInfo(final WakeLock lock, final long startTimeMs, final long endTimeMs)
    {
        m_Lock = lock;
        m_StartTimeMs = startTimeMs;
        m_EndTimeMs = endTimeMs;
    }

    /**
     * Returns the wake lock associated with the information.
     * 
     * @return 
     *      The wake lock associated with the information.
     */
    public WakeLock getLock()
    {
        return m_Lock;
    }

    /**
     * Returns the start time for the lock.
     * 
     * @return 
     *      Start time in milliseconds.
     */
    public long getStartTimeMs()
    {
        return m_StartTimeMs;
    }

    /**
     * Returns the end time for the lock.
     * 
     * @return 
     *      End time in milliseconds.
     */
    public long getEndTimeMs()
    {
        return m_EndTimeMs;
    }
}
