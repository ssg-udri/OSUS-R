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
package mil.dod.th.ose.shared.pm;

import mil.dod.th.core.pm.WakeLock;

/**
 * Wrapper class for {@link WakeLock} that supports multiple calls to {@link WakeLock#activate()} and
 * {@link WakeLock#cancel()}, keeping track of the number of calls to activate and cancel. Calls to activate increment a
 * counter and calls to cancel decrement a counter. When the counter is 0, activate or cancel is called on the
 * underlying wake lock as appropriate.
 * <p>
 * If the underlying wake lock is not set this class does nothing (it does not throw exceptions, etc.).
 * 
 * @author jkovach
 */
public class CountingWakeLock
{
    private final Object m_LockObj = new Object();
    private WakeLock m_TheWakeLock;
    private int m_Count;

    /**
     * Create instance without an associated {@link WakeLock}.
     */
    public CountingWakeLock()
    {
        // no wake lock is assigned yet
    }

    /**
     * Create instance with a wake lock.
     * 
     * @param wakeLock
     *      wake lock instance to use
     */
    public CountingWakeLock(final WakeLock wakeLock)
    {
        setWakeLock(wakeLock);
    }

    public WakeLock getWakeLock()
    {
        return m_TheWakeLock;
    }

    /**
     * Set a new wake lock instance that should be associated with the counting wake lock.
     * 
     * @param wakeLock
     *      wake lock instance to use
     */
    public final void setWakeLock(final WakeLock wakeLock)
    {
        synchronized (m_LockObj)
        {
            if (m_TheWakeLock != null && m_Count != 0)
            {
                m_TheWakeLock.cancel();
            }

            m_TheWakeLock = wakeLock;

            if (m_TheWakeLock != null && m_Count != 0)
            {
                m_TheWakeLock.activate();
            }
        }
    }

    /**
     * Call {@link WakeLock#delete()} if there is a wake lock set.
     */
    public void deleteWakeLock()
    {
        synchronized (m_LockObj)
        {
            if (m_TheWakeLock != null)
            {
                m_TheWakeLock.delete();
                m_TheWakeLock = null; // NOPMD: NullAssignment, Must assign to null, field is checked before using
            }
        }
    }

    /**
     * Activate the wake lock with a handle supported by the Java try with resource mechanism.
     * 
     * @return
     *      handle that implements {@link AutoCloseable}
     * @see WakeLock#activate()
     */
    public CountingWakeLockHandle activateWithHandle()
    {
        activate();
        return new CountingWakeLockHandle();
    }

    /**
     * Call {@link WakeLock#activate()} if the current count is zero, otherwise just increment the count.
     */
    public void activate()
    {
        synchronized (m_LockObj)
        {
            if (m_Count == 0 && m_TheWakeLock != null)
            {
                m_TheWakeLock.activate();
            }
            m_Count++;
        }
    }

    /**
     * Decrement the current count and call {@link WakeLock#cancel()} if the new count is zero.
     */
    public void cancel()
    {
        synchronized (m_LockObj)
        {
            if (m_Count > 0)
            {
                m_Count--;
    
                if (m_Count == 0 && m_TheWakeLock != null)
                {
                    m_TheWakeLock.cancel();
                }
            }
        }
    }

    /**
     * Helper class for use with Java7 automatic resource management.
     */
    public final class CountingWakeLockHandle implements AutoCloseable
    {
        /**
         * Only allow creation of handles from within the {@link CountingWakeLock} implementation.
         */
        CountingWakeLockHandle()
        {
            // constructor used to restrict creation of handles
        }

        @Override
        public void close()
        {
            cancel();
        }
    }
}
