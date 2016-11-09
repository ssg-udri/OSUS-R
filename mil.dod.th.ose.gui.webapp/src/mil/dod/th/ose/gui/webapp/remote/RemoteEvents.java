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

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;

/**
 * This class handles utility functions relating to remote events.
 * 
 * @author jlatham
 *
 */
final public class RemoteEvents
{

    /**
     * Constant used to define the timeout for an event registration in hours. Default is 
     * 3 weeks (504h).
     */
    static final private int DEFAULT_EVENT_REG_TIMEOUT_HOURS = 504;
    
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
                .setExpirationTimeHours(DEFAULT_EVENT_REG_TIMEOUT_HOURS) // TODO: TH-778 will implement handling this.
                .setCanQueueEvent(canQueueEvent); // TODO: TH-1239 will implement handling this.
        
        if (filterString != null)
        {
            requestMessage.setFilter(filterString);
        }               
                
        messageFactory.createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build()).queue(systemId, responseHandler);
    }
}
