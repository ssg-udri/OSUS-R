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

import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;


/**
 * Abstract class that describes common channel attributes. These attributes are required to be defined by
 * all channels regardless of the type(ie Socket, link, transport...).
 * @author callen
 *
 */
public abstract class Channel
{
    /**
     * The channel type.
     */
    private final RemoteChannelTypeEnum m_Type;

    /**
     * The controller id that this channel connects to.
     */
    private final int m_ControllerId;
    
    /**
     * The status of this channel.
     */
    private final ChannelStatus m_Status;

    /**
     * Number of bytes received on channel.
     */
    private final Long m_BytesReceived;

    /**
     * Number of bytes transmitted on channel.
     */
    private final Long m_BytesTransmitted;
    
    /**
     * Number of queued messages on channel.
     */
    private final int m_QueuedMessageCount;

    /**
     * Public constructor for the channel class.
     * @param controllerId
     *     the terra harvest system id of the controller that this channel connects to
     * @param type
     *     the type of the channel
     * @param status
     *     the status that this model represents
     * @param bytesTransmitted
     *      number of bytes transmitted on this channel
     * @param bytesReceived
     *      number of bytes received on this channel
     * @param queuedMessageCount
     *      number of messages queued on this channel
     */
    public Channel(final int controllerId, final RemoteChannelTypeEnum type, final ChannelStatus status, 
            final Long bytesTransmitted, final Long bytesReceived, final int queuedMessageCount)
    {
        m_ControllerId = controllerId;
        m_Type = type;
        m_Status = status;
        m_BytesTransmitted = bytesTransmitted;
        m_BytesReceived = bytesReceived;
        m_QueuedMessageCount = queuedMessageCount;
    }

    /**
     * Get the channel type.
     * @return
     *    the type of the channel
     */
    public RemoteChannelTypeEnum getChannelType() 
    {
        return m_Type;
    }
    
    /**
     * Get the {@link mil.dod.th.core.system.TerraHarvestSystem} id of the controller to which this channel connects to.
     * @return 
     *     the controller Id
     */
    public int getControllerId() 
    {
        return m_ControllerId;
    }
    
    /**
     * Get the channel's {@link mil.dod.th.core.remote.ChannelStatus}.
     * @return 
     *     the status of the channel
     */
    public ChannelStatus getChannelStatus() 
    {
        return m_Status;
    }
    
    /**
     * Get the number of bytes transmitted on channel.
     * 
     * @return 
     *      byte transmitted
     */
    public Long getBytesTransmitted()
    {
        return m_BytesTransmitted;
    }
    
    /**
     * Get the number of bytes received on channel.
     * 
     * @return 
     *      byte received
     */
    public Long getBytesReceived()
    {
        return m_BytesReceived;
    }
    
    /**
     * Get the number of queued messages on channel.
     * @return
     *      queued message count
     */
    public int getQueuedMessageCount()
    {
        return m_QueuedMessageCount;
    }
}
