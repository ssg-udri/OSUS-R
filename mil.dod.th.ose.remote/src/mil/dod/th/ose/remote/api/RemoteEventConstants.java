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
package mil.dod.th.ose.remote.api;


/**
 * Class for maintaining remote event constants pertaining to the remote interface API.  
 * @author bachmakm
 *
 */
public final class RemoteEventConstants
{
    /** Event topic prefix for all remote api events. */
    final public static String TOPIC_PREFIX = "mil/dod/th/ose/remote/api/";
    
    /** 
     * Event topic denoting when the observation store has been completely updated after a GetObservationsRequest.
     */
    final public static String TOPIC_OBS_STORE_RETRIEVE_COMPLETE = TOPIC_PREFIX + "OBS_STORE_RETRIEVE_COMPLETE";
    
    /**
     * Event property denoting the number of observations that have been retrieved.  
     */
    final public static String EVENT_PROP_OBS_NUMBER_RETRIEVED = "obs.number.retrieved";
    
    /**
     * Constructor to prevent instantiation. 
     */
    private RemoteEventConstants()
    {
        
    }
}
