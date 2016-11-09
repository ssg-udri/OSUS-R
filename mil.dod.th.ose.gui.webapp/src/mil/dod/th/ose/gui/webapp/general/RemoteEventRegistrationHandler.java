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
package mil.dod.th.ose.gui.webapp.general;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Response handler for remote event registration responses.
 */
public class RemoteEventRegistrationHandler implements ResponseHandler
{
    /**
     * MessageFactory service for creating remote messages.
     */
    private final  MessageFactory m_MessageFactory;

    /**
     * Map of remote event registration ids. Used to unregister from receiving remote events.
     */
    private final Map<Integer, Set<Integer>> m_RemoteRegistrations = new HashMap<Integer, Set<Integer>>();

    /**
     * Default Constructor.  MessageFactory is required.
     * @param messageFactory The object injected into the ctor
     */
    public RemoteEventRegistrationHandler(final MessageFactory messageFactory)
    {
        Preconditions.checkNotNull(messageFactory);
        m_MessageFactory = messageFactory;
    }

    @Override
    public void handleResponse(final TerraHarvestMessage thMessage, final TerraHarvestPayload payload, 
         final Message namespaceMessage, 
        final Message dataMessage)
    {
        final EventRegistrationResponseData regResponse = (EventRegistrationResponseData) dataMessage;

        final int systemId = thMessage.getSourceId();
         
        if (m_RemoteRegistrations.containsKey(systemId))
        {
            //add reg. id
            m_RemoteRegistrations.get(systemId).add(regResponse.getId());
        }
        else
        {
            //create new entry set
            final Set<Integer> newRegSet = new HashSet<Integer>();
            newRegSet.add(regResponse.getId());
            m_RemoteRegistrations.put(systemId, newRegSet);
        }
    }

    /**
     * Unregister the remote event registrations.
     */
    public void unregisterRegistrations()
    {
        for (int sysId : m_RemoteRegistrations.keySet())
        {
            for (int regId : m_RemoteRegistrations.get(sysId))
            {
                final UnregisterEventRequestData unRegReq = UnregisterEventRequestData.newBuilder().
                    setId(regId).build();
                m_MessageFactory.createEventAdminMessage(EventAdminMessageType.UnregisterEventRequest,
                    unRegReq).queue(sysId, null);
            }
        }
    }
}
