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
package mil.dod.th.ose.remote;

import java.util.Map;

import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Remote channel used when no remote channel to a system can be found.
 * 
 * @author cweisenborn
 */
public class EventChannel implements RemoteChannel
{
    private final EventAdmin m_EventAdmin;
    private final int  m_RemoteSystemId;
    
    /**
     * Constructor that accepts the ID of the remote system the channel represents and the event admin used to post
     * events to the system.
     * 
     * @param remoteSystemId
     *      ID of the remote system the channel represents.
     * @param eventAdmin
     *      Event admin service used to post unreachable send events.
     */
    public EventChannel(final int remoteSystemId, final EventAdmin eventAdmin)
    {
        m_RemoteSystemId = remoteSystemId;
        m_EventAdmin = eventAdmin;
    }
    
    public int getRemoteSystemId()
    {
        return m_RemoteSystemId;
    }
    
    @Override
    public boolean trySendMessage(final TerraHarvestMessage message)
    {
        final Event unreachableDestEvent = RemoteInterfaceUtilities.createMessageUnreachableSendEvent(message);
        m_EventAdmin.postEvent(unreachableDestEvent);
        return true;
    }

    @Override
    public boolean queueMessage(final TerraHarvestMessage message)
    {
        final Event unreachableDestEvent = RemoteInterfaceUtilities.createMessageUnreachableSendEvent(message);
        m_EventAdmin.postEvent(unreachableDestEvent);
        return true;
    }

    @Override
    public boolean matches(final Map<String, Object> properties)
    {
        return false;
    }

    @Override
    public ChannelStatus getStatus()
    {
        return null;
    }

    @Override
    public RemoteChannelTypeEnum getChannelType()
    {
        return null;
    }

    @Override
    public int getQueuedMessageCount()
    {
        return 0;
    }

    @Override
    public long getBytesTransmitted()
    {
        return 0;
    }

    @Override
    public long getBytesReceived()
    {
        return 0;
    }

    @Override
    public void clearQueuedMessages()
    {
        //Empty method. Method should do nothing as no message will ever be place in a queue.
    }
}
