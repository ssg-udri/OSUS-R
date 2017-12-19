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
package mil.dod.th.ose.gui.webapp.remote;

import java.util.List;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;

/**
 * This class handles utility functions relating to remote events.
 * 
 * @author jlatham
 */
final public class RemoteEvents
{
    static private int m_EventRegTimeoutHours = RemoteConstants.REMOTE_EVENT_DEFAULT_REG_TIMEOUT_HOURS;
    
    /**
     * Empty constructor for utility class.
     */
    private RemoteEvents()
    {        
    }
    
    /**
     * Registers for events remotely for a system.
     * 
     * @param messageFactory
     *     the message factory to use for creating messages
     * @param topics
     *     the list of event topics to register for
     * @param filterString
     *     string to filter the events
     * @param canQueueEvent
     *     if the events should be queued or not
     * @param systemId
     *     id of the registering system
     * @param responseHandler
     *     remote event registration handler for this registration
     */
    static public void sendEventRegistration(final MessageFactory messageFactory, final List<String> topics, 
            final String filterString, final boolean canQueueEvent, final int systemId, 
            final ResponseHandler responseHandler)
    {
        final EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder().
                addAllTopic(topics)
                .setExpirationTimeHours(m_EventRegTimeoutHours)
                .setCanQueueEvent(canQueueEvent);

        if (filterString != null)
        {
            requestMessage.setFilter(filterString);
        }               

        messageFactory.createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build()).queue(systemId, responseHandler);
    }

    /**
     * Get the remote event expiration hours used for new event registrations.
     * 
     * @return
     *      expiration timeout in hours
     */
    public static int getRemoteEventExpirationHours()
    {
        return m_EventRegTimeoutHours;
    }

    /**
     * Set the remote event expiration hours used for new event registrations.
     * 
     * @param expHours
     *      expiration timeout in hours
     */
    public static void setRemoteEventExpirationHours(final int expHours)
    {
        m_EventRegTimeoutHours = expHours;
    }
}
