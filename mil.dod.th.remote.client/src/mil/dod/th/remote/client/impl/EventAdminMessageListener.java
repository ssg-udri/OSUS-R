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
package mil.dod.th.remote.client.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.RemoteMessage;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This component automatically registers itself as a {@link MessageListenerCallback} to receive
 * {@link Namespace#EventAdmin} messages and post any remote events received via {@link EventAdmin} as OSGi events.
 * 
 * @author dlandoll
 */
@Component
public class EventAdminMessageListener
{
    private EventAdmin m_EventAdmin;
    private MessageListenerService m_MessageListenerService;
    private MessageCallback m_MessageCallback;
    private Logging m_Logging;

    @Reference
    public void setLogging(final Logging logging)
    {
        m_Logging = logging;
    }

    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    @Reference
    public void setMessageListenerService(final MessageListenerService messageListenerService)
    {
        m_MessageListenerService = messageListenerService;
    }

    /**
     * Activates the component and registers the callback with {@link MessageListenerService}.
     */
    @Activate
    public void activate()
    {
        m_MessageCallback = new MessageCallback();
        m_MessageListenerService.registerCallback(Namespace.EventAdmin, m_MessageCallback);
    }

    /**
     * Deactivates the component and unregisters the callback with {@link MessageListenerService}.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageListenerService.unregisterCallback(m_MessageCallback);
    }

    /**
     * Message listener callback for EventAdmin namespace messages.
     */
    private class MessageCallback implements MessageListenerCallback<EventAdminNamespace>
    {
        @Override
        public void handleMessage(final RemoteMessage<EventAdminNamespace> message)
        {
            switch ((EventAdminMessageType)message.getDataMessageType())
            {
                case SendEvent:
                    final SendEventData data = (SendEventData)message.getDataMessage();
                    sendEvent(data.getTopic(), data.getPropertyList());
                    break;
                default:
                    break;
            }
        }

        /**
         * Creates an OSGi event and posts to {@link EventAdmin}.
         * 
         * @param topic
         *      event topic
         * @param properties
         *      list of properties received in the proto message
         */
        private void sendEvent(final String topic, final List<ComplexTypesMapEntry> properties)
        {
            final Map<String, ComplexTypesMapEntry> convProps = new HashMap<>();
            for (ComplexTypesMapEntry entry : properties)
            {
                convProps.put(entry.getKey(), entry);
            }

            m_Logging.debug("Posting remote event [%s]", topic);

            final Event event = new Event(topic, convProps);
            m_EventAdmin.postEvent(event);
        }
    }
}
