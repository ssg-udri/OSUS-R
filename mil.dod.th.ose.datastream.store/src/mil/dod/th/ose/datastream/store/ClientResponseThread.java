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
package mil.dod.th.ose.datastream.store;

import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.ose.datastream.store.ThreadStatusListener.ThreadState;

/**
 * This Thread class acts as a countdown timer and can be reset by a call to
 * its reset() method.
 * 
 * @author jmiller
 *
 */
public class ClientResponseThread extends Thread
{
    
    //Time in milliseconds to sleep
    private static final long SLEEP_INTERVAL = 100;
    
    //Millisecond conversion
    private static final long SECONDS_TO_MILLISECONDS = 1000;

    private ThreadStatusListener m_Listener;
    private StreamProfile m_StreamProfile;
    private long m_HeartbeatPeriod;
    private long m_Delay;
    private boolean m_UseSourceBitrate;
    
    //Time in milliseconds at which this thread "times out"
    private long m_CountdownTime;
    
    //The system time in milliseconds at which this thread completes and kicks off an archiving process
    private long m_ArchiveStartTime;
    
    
    /**
     * Private constructor to prevent external initialization.
     */
    private ClientResponseThread()
    {
        super();
    }
    
    /**
     * Constructs a thread that remains alive as long as client continues to respond and
     * reset the timer.
     * 
     * @param listener
     *      responds to events created by this Thread   
     * @param streamProfile
     *      the {@link StreamProfile} object to which this Thread refers
     * @param heartbeatPeriod
     *      time in seconds that the thread will stay alive unless a client resets the timer
     * @param delay
     *      initial time delay in seconds before the timer countdown begins
     * @param useSourceBitrate
     *      indicates if the archived data should be from the streaming asset directly or from the output
     *      of a transcoder process
     */
    public ClientResponseThread(final ThreadStatusListener listener, final StreamProfile streamProfile, 
            final long heartbeatPeriod, final long delay, final boolean useSourceBitrate)
    {
        this();
        m_Listener = listener;
        m_StreamProfile = streamProfile;
        m_HeartbeatPeriod = heartbeatPeriod;
        m_Delay = delay;
        m_UseSourceBitrate = useSourceBitrate;
        
        m_ArchiveStartTime = -1;

    }
    
    public boolean isUseSourceBitrate()
    {
        return m_UseSourceBitrate;
    }
    
    public long getArchiveStartTime()
    {
        return m_ArchiveStartTime;
    }
    
    /**
     * Reset the timer by making the new countdown limit {@code m_HeartbeatPeriod} seconds
     * from the current time.
     */
    public void reset()
    {
        synchronized (this)
        {
            m_CountdownTime = System.currentTimeMillis() + m_HeartbeatPeriod * SECONDS_TO_MILLISECONDS;
        }
    }
    
    
    @Override
    public void run()
    {     
        
        // If heartbeat period is 0, notify observer to go straight to archiving
        if (m_HeartbeatPeriod == 0)
        {
            m_ArchiveStartTime = System.currentTimeMillis();
            m_Listener.notifyObserver(m_StreamProfile, ThreadState.FINISHED);
            return;
        }
        
        synchronized (this)
        {
            m_CountdownTime = System.currentTimeMillis() + SECONDS_TO_MILLISECONDS * (m_HeartbeatPeriod + m_Delay);
        }
        
        while (System.currentTimeMillis() < m_CountdownTime)
        {
            try
            {
                Thread.sleep(SLEEP_INTERVAL);
            }
            catch (final InterruptedException e)
            {
                m_Listener.notifyObserver(m_StreamProfile, ThreadState.INTERRUPTED);
                return;
            }
        }

        //Countdown timer has expired; Notify listener
        m_ArchiveStartTime = System.currentTimeMillis();
        m_Listener.notifyObserver(m_StreamProfile, ThreadState.FINISHED);

    }

}
