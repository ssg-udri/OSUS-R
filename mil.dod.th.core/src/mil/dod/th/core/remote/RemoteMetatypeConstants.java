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
package mil.dod.th.core.remote;

/**
 * This class contains constants that are used to assist in the dissemination of local metatype information to
 * remote clients. Consumers should not register with the local event admin service to receive these event, these
 * topics are intended to be registered for through remote event registrations.
 * @author callen
 *
 */
public final class RemoteMetatypeConstants
{
    /** Event topic prefix used when metatype information is available. */
    public final static String TOPIC_PREFIX = "mil/dod/th/ose/remote/MetatypeInformationListener/";
    
    /**
     * Event topic for when metatype information is available.
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_PIDS} - a list of PIDs 
     * <li>{@link #EVENT_PROP_BUNDLE_ID} - bundle ID where the information is available
     * </ul>
     */
    public final static String TOPIC_METATYPE_INFORMATION_AVAILABLE = TOPIC_PREFIX + "METATYPE_INFORMATION_AVAILABLE";

    /**
     * Event property for the bundle ID of where metatype information can be found.
     */
    public final static String EVENT_PROP_BUNDLE_ID = "service.bundle.id";

    /**
     * Event property key for PIDS contained in a bundle found to have XML based metatype information.
     * This property maps to a list of strings.
     */
    public final static String EVENT_PROP_PIDS = "service.pids";

    /**
     * Hidden constructor for the utility class.
     */
    private RemoteMetatypeConstants()
    {
        
    }
}
