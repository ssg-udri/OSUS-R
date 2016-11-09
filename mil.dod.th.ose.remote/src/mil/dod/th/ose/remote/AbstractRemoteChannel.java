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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * Abstract class for remote channels with some basic functionality that is shared among all implementations.
 * 
 * @author Dave Humeniuk
 *
 */
public abstract class AbstractRemoteChannel implements RemoteChannel
{
    /**
     * Keep track of the {@link QueuedMessageSender} instance for later disposal.
     */
    private ComponentInstance m_MessageSenderInstance;

    /**
     * Factory for creating {@link QueuedMessageSender} components.
     */
    private ComponentFactory m_MessageSenderFactory;

    /**
     * Service that will send queued messages.
     */
    private QueuedMessageSender m_MessageSender;
    
    /**
     * Type of channel.
     */
    private final RemoteChannelTypeEnum m_Type;

    /**
     * Copy of the socket property for matching.
     */
    private Map<String, Object> m_Properties;
    
    /**
     * Public constructor that sets up the type of the channel.
     * 
     * @param type
     *      type of channel
     */
    public AbstractRemoteChannel(final RemoteChannelTypeEnum type)
    {
        m_Type = type;
    }
    
    /**
     * Bind the factory for message senders.
     * 
     * @param factory
     *      creates {@link QueuedMessageSender}s
     */
    public void setQueuedMessageSenderFactory(final ComponentFactory factory)
    {
        m_MessageSenderFactory = factory;
    }
    
    /**
     * Initialize the message sender.  Must be called by implementer.
     * 
     * @param matchProps 
     *      properties used when matching channels, must only contain channel specific properties to match on 
     */
    public void initMessageSender(final Map<String, Object> matchProps)
    {
        m_Properties = new HashMap<String, Object>(matchProps);
        
        final Dictionary<String, Object> senderProps = new Hashtable<String, Object>();
        senderProps.put(QueuedMessageSender.CHANNEL_PROP_KEY, this);
        m_MessageSenderInstance = m_MessageSenderFactory.newInstance(senderProps);
        m_MessageSender = (QueuedMessageSender)m_MessageSenderInstance.getInstance();
    }

    /**
     * Cleanup the message sender.  Must be called by implementer.
     */
    public void cleanupMessageSender()
    {
        m_MessageSenderInstance.dispose();
    }
    
    @Override
    public boolean matches(final Map<String, Object> properties)
    {
        return m_Properties.equals(properties);
    }
    
    @Override
    public boolean queueMessage(final TerraHarvestMessage message)
    {
        return m_MessageSender.queue(message);
    }

    @Override
    public int getQueuedMessageCount()
    {
        return m_MessageSender.getQueuedMessageCount();
    }

    @Override
    public RemoteChannelTypeEnum getChannelType()
    {
        return m_Type;
    }
    
    @Override
    public void clearQueuedMessages()
    {
        m_MessageSender.clearQueue();
    }
    
    /**
     * Direct reference of the underlying property map containing the channel specific properties.  This is not a copy
     * so care should be taken not to modify the map or expose it outside of the component.
     * 
     * @return
     *      unmodifiable map
     */
    protected Map<String, Object> getProperties()
    {
        return m_Properties;
    }
}
