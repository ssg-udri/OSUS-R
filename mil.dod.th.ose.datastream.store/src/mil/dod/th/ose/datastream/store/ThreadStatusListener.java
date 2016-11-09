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

/**
 * Defines interface for thread to notify observer when it has finished its run method.
 * 
 * @author jmiller
 *
 */
public interface ThreadStatusListener
{
    
    /**
     * Enumeration of possible states for this Thread to be passed to 
     * {@link ThreadStatusListener#notifyObserver(StreamProfile, ThreadState)}.
     */
    enum ThreadState 
    {
        /**
         * Thread is currently running.
         */
        RUNNING, 
        
        /**
         * Thread has been interrupted.
         */
        INTERRUPTED, 
        
        /**
         * Thread has finished its countdown.
         */
        FINISHED, 
        
        /**
         * Thread has encountered an error.
         */
        ERROR 
    };

    /**
     * Notify the observer, referencing a particular StreamProfile instance and
     * one of the enum values from {@link ThreadState}.
     * 
     * @param streamProfile
     *      the stream profile instance to which the listener corresponds
     * @param threadState
     *      indicates the reason the observer is being notified
     */
    void notifyObserver(StreamProfile streamProfile, ThreadState threadState);
    
}
