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

import java.util.Map;

import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;

/**
 * This interface allows access to remote event admin registrations.
 * @author callen
 *
 */
public interface RemoteEventAdmin
{
    /**
     * Get a map of current remote event registrations for all known systemIds. 
     * Will return an empty map if there are no known registrations.
     * @return
     *     map of registration IDs and their corresponding event registration messages
     */
    Map<Integer, RemoteEventRegistration> getRemoteEventRegistrations();
    
    /**
     * Automatically register for events when the system starts. These registrations are not persisted, therefore are
     * only good for as long as the system is on. 
     * @param systemId
     *      the ID of the system which will receive the events
     * @param eventRegMessage
     *      the event registration message, an expiration value of <code>-1</code> means that the registration
     *      will never expire 
     */
    void addRemoteEventRegistration(int systemId, EventRegistrationRequestData eventRegMessage);
}
