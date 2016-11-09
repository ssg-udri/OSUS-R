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

import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;

/**
 * Transport Layer type implementation of the {@link Channel} type.
 * @author callen
 *
 */
public class TransportChannelModel extends Channel 
{
    /**
     * The name of the transport layer that makes up this channel.
     */
    private final String m_TransportName;
    
    /**
     * Message address for the local endpoint.
     */
    private final String m_LocalMessageAddress;

    /**
     * Message address for the remote endpoint.
     */
    private final String m_RemoteMessageAddress;
        
    /**
     * Public constructor for the TransportChannelModel type.
     * @param controllerId
     *     the id of the controller to which this channel belongs to
     * @param channel
     *      underlying channel the model represents  
     */
    public TransportChannelModel(final int controllerId, final TransportChannel channel)
    {
        super(controllerId, RemoteChannelTypeEnum.TRANSPORT, channel.getStatus(), channel.getBytesTransmitted(), 
                channel.getBytesReceived(), channel.getQueuedMessageCount());
        m_TransportName = channel.getTransportLayerName();
        m_LocalMessageAddress = channel.getLocalMessageAddress();
        m_RemoteMessageAddress = channel.getRemoteMessageAddress();
    }

    /**
     * Get the name of the transport layer used for this channel.
     * @return
     *    the name of the transport layer this channel uses
     */
    public String getName()
    {
        return m_TransportName;
    }

    /**
     * Get the message address for the local endpoint of the channel.
     * @return
     *     the local address assigned to this channel
     */
    public String getLocalMessageAddress()
    {
        return m_LocalMessageAddress;
    }

  
    /**
     * Get the message address for the remote endpoint of the channel.
     * @return
     *     the remote address assigned to this channel
     */
    public String getRemoteMessageAddress()
    {
        return m_RemoteMessageAddress;
    }    
}
