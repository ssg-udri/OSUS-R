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
package mil.dod.th.ose.gui.webapp.channel;

import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;

/**
 * Socket channel type implementation of the {@link Channel} interface. 
 * @author callen
 *
 */
public class SocketChannelModel extends Channel 
{
    /**
     * Human-readable 'name' or 'host' of the channel.
     */
    private final String m_HostName;

    /**
     * Address or port for the channel.
     */
    private final int m_Port;
        
    /**
     * Public constructor for the SocketChannelModel type.
     * @param controllerId
     *    the id of the controller to which this channel belongs to
     * @param channel
     *     channel this model represents
     */
    public SocketChannelModel(final int controllerId, final SocketChannel channel)
    {
        super(controllerId, RemoteChannelTypeEnum.SOCKET, channel.getStatus(), channel.getBytesTransmitted(), 
                channel.getBytesReceived(), channel.getQueuedMessageCount());
        m_HostName = channel.getHost();
        m_Port = channel.getPort();
    }

    /**
     * Get the assigned port for this connection.
     * @return
     *    the port number for this channel
     */
    public int getPort()
    {
        return m_Port;
    }

    /**
     * Get the host for this connection.
     * @return
     *    string representing the host for this channel
     */
    public String getHost()
    {
        return m_HostName;
    }
}
