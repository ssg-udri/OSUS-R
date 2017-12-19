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
package mil.dod.th.ose.test.remote;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;

/**
 * This class has utility methods needed for remote interface testing.
 * 
 * @author jlatham
 *
 */
final public class RemoteUtils
{
    /**
     * Appends supplemental required fields to {@link EventRegistrationRequestData} messages that do not have them.
     * 
     * @param requestMessage
     *     The message to append the fields to
     * @param queueEvent
     *     The value for the canQueueEvent field
     * @return
     *     The modified {@link EventRegistrationRequestData}
     */
    static public EventRegistrationRequestData.Builder appendRequiredFields(
            final EventRegistrationRequestData.Builder requestMessage, final boolean queueEvent)
    {
        if (!requestMessage.hasExpirationTimeHours())
        {
            requestMessage.setExpirationTimeHours(RemoteConstants.REMOTE_EVENT_DEFAULT_REG_TIMEOUT_HOURS);
        }
        
        requestMessage.setCanQueueEvent(queueEvent);
        
        return requestMessage;
    }
}
