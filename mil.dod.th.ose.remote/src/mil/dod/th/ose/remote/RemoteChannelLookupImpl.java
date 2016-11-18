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

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.SocketChannelType;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.SystemChannels;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.TransportChannelType;
import mil.dod.th.ose.remote.transport.TransportChannelImpl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Implementation of the {@link RemoteChannelLookup} interface.
 * 
 * @author Dave Humeniuk
 *
 */
@Component // NOCHECKSTYLE: Fan out complexity
           // not easily able to reduce complexity, unless persistent data store is simplified
public class RemoteChannelLookupImpl implements RemoteChannelLookup
{

    /**
     * Map of all channels in the lookup, keyed by the system id of the controller, value is a set of channels for that
     * controller.
     */
    private final Map<Integer, Set<RemoteChannel>> m_ChannelMap = new HashMap<Integer, Set<RemoteChannel>>();

    /**
     * Map of all {@link RemoteChannel} instances created keyed by channel.  Keep for later disposal. 
     */
    private final Map<RemoteChannel, ComponentInstance> m_Instances = new HashMap<RemoteChannel, ComponentInstance>();
    
    /**
     * Factory used to create {@link ClientSocketChannel}s.
     */
    private ComponentFactory m_ClientSocketChannelFactory;
    
    /**
     * Factory used to create {@link ServerSocketChannel}s.
     */
    private ComponentFactory m_ServerSocketChannelFactory;
    
    /**
     * Factory used to create {@link TransportChannelImpl}s.
     */
    private ComponentFactory m_TransportChannelFactory;
    
    /**
     * Reference to the event handler that is listening for sync and remove events originating from the
     * {@link SocketMessageListener}.
     */
    private EventHandlerImpl m_SyncRemoveEventHandler;

    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Service for persisting generic data, will be used to persist remote channels.
     */
    private PersistentDataStore m_PersistentDataStore;

    /**
     * Used for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Message Router the remote channel lookup service will be bound to.
     */
    private MessageRouterInternal m_MessageRouter;

    /**
     * Controller system information.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;

    /**
     * Remote interface configuration settings.
     */
    private RemoteSettings m_RemoteSettings;

    /**
     * Flag to keep track if component is in the process of restoring channels from the persistent store. 
     */
    private boolean m_Restoring;
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Message router service the remote channel lookup will be bound to.
     * 
     * @param router
     *      Message router service
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal router)
    {
        m_MessageRouter = router;
    }
    
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Bind the factory used to create {@link ClientSocketChannel}s.
     * 
     * @param clientSocketChannelFactory
     *      factory that builds {@link ClientSocketChannel}s
     */
    @Reference(target = "(component.factory=" + ClientSocketChannel.FACTORY_NAME + ")")
    public void setClientSocketChannelFactory(final ComponentFactory clientSocketChannelFactory)
    {
        m_ClientSocketChannelFactory = clientSocketChannelFactory;
    }
    
    /**
     * Bind the factory used to create {@link ServerSocketChannel}s.
     * 
     * @param serverSocketChannelFactory
     *      factory that builds {@link ServerSocketChannel}s
     */
    @Reference(target = "(component.factory=" + ServerSocketChannel.FACTORY_NAME + ")")
    public void setServerSocketChannelFactory(final ComponentFactory serverSocketChannelFactory)
    {
        m_ServerSocketChannelFactory = serverSocketChannelFactory;
    }

    /**
     * Bind the factory used to create {@link TransportChannelImpl}s.
     * 
     * @param transportChannelFactory
     *      factory that builds {@link TransportChannelImpl}s
     */
    @Reference(target = "(component.factory=" + TransportChannelImpl.FACTORY_NAME + ")")
    public void setTransportChannelFactory(final ComponentFactory transportChannelFactory)
    {
        m_TransportChannelFactory = transportChannelFactory;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind the service for persisting generic data to the database.
     * 
     * @param persistentDataStore
     *      service for persisting generic data
     */
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }
    
    /**
     * Activate this component and pass the context to the event handler that 
     * listens to sync and remove channel events.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_SyncRemoveEventHandler = new EventHandlerImpl(context);
        m_SyncRemoveEventHandler.registerForSyncRemoveEvents();
        
        m_MessageRouter.bindRemoteChannelLookup(this);
        
        restoreChannels();
    }

    /**
     * Deactivate the component by disposing of all created {@link RemoteChannel}s.
     */
    @Deactivate
    public void deactivate()
    {
        //unregister the event listener for sync messages
        m_SyncRemoveEventHandler.unregisterServiceRegistration();
        
        m_MessageRouter.unbindRemoteChannelLookup(this);
        
        for (ComponentInstance instance : m_Instances.values())
        {
            instance.dispose();
        }
    }
    
    @Override
    public synchronized RemoteChannel getChannel(final int systemId) throws IllegalArgumentException
    {
        final Set<RemoteChannel> channels = m_ChannelMap.get(systemId);
        if (channels == null)
        {
            throw new IllegalArgumentException(String.format("The id 0x%08x is an invalid system id", systemId));
        }
        
        if (channels.iterator().hasNext())
        {
            // for now, just return the first in the set
            return channels.iterator().next();
        }

        throw new IllegalArgumentException(String.format("No longer a valid system id 0x%08x", systemId));
    }
    
    @Override
    public SocketChannel getSocketChannel(final String hostname, final int port)
    {
        for (RemoteChannel channel : m_Instances.keySet())
        {
            if (!(channel instanceof SocketChannel))
            {
                continue;
            }
            
            final SocketChannel socketChannel = (SocketChannel)channel;
            if (socketChannel.getHost().equals(hostname) && socketChannel.getPort() == port)
            {
                return socketChannel;
            }
        }
        return null;
    }

    @Override
    public TransportChannel getTransportChannel(final String transportLayerName, final String localMessageAddress,
            final String remoteMessageAddress)
    {
        for (RemoteChannel channel : m_Instances.keySet())
        {
            if (!(channel instanceof TransportChannel))
            {
                continue;
            }
            
            final TransportChannel transportChannel = (TransportChannel)channel;
            if (transportChannel.getTransportLayerName().equals(transportLayerName)
                    && transportChannel.getLocalMessageAddress().equals(localMessageAddress)
                    && transportChannel.getRemoteMessageAddress().equals(remoteMessageAddress))
            {
                return transportChannel;
            }
        }
        return null;
    }
    
    @Override
    public synchronized List<RemoteChannel> getChannels(final int systemId)
    {
        final Set<RemoteChannel> channels = m_ChannelMap.get(systemId);
        if (channels == null || channels.isEmpty())
        {
            return new ArrayList<RemoteChannel>();
        }
        return new ArrayList<RemoteChannel>(channels);
    }
    
    @Override
    public synchronized Map<Integer, Set<RemoteChannel>> getAllChannels()
    {
        final Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        for (int id : m_ChannelMap.keySet())
        {
            channels.put(id, Collections.unmodifiableSet(m_ChannelMap.get(id)));
        }
        return channels;
    }
    
    @Override 
    public int getChannelSystemId(final RemoteChannel channel)
    {
        for (int sysId: getAllChannels().keySet())
        {
            for (RemoteChannel sysChannel: m_ChannelMap.get(sysId))
            {
                if (sysChannel.equals(channel))
                {
                    return sysId;
                }
            }
        }
        throw new IllegalArgumentException("Specified channel is not associated with a system!");
    }
    
    @Override
    public SocketChannel newServerSocketChannel(final Socket socket)
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);

        final ComponentInstance instance = 
                m_ServerSocketChannelFactory.newInstance(new Hashtable<String, Object>(properties));
        final SocketChannel channel = (SocketChannel)instance.getInstance();
        m_Instances.put(channel, instance);
        
        return channel;
    }

    @Override
    public synchronized void syncChannel(final RemoteChannel channel, final int systemId)
    {
        syncChannel(channel, systemId, true);
    }
    
    @Override
    public synchronized void syncChannel(final RemoteChannel channel, final int systemId, final boolean persist)
    {
        Set<RemoteChannel> channels = m_ChannelMap.get(systemId);
        if (channels == null)
        {
            // first channel for this id, create the set
            channels = new HashSet<RemoteChannel>();
            m_ChannelMap.put(systemId, channels);
            
        }
        
        // see if channel is already associated with a system id
        for (int idInList : m_ChannelMap.keySet())
        {
            for (RemoteChannel channelInList : m_ChannelMap.get(idInList))
            {
                if (channelInList.equals(channel))
                {
                    // found channel
                    if (idInList == systemId)
                    {
                        return; // same system id as before, nothing more to do
                    }
                    // syncing to new id, remove from old id list
                    removeChannelFromSet(m_ChannelMap.get(idInList), channelInList, idInList);
                    break;
                }
            }
        }
        
        // add to list of channels for the new id
        addChannelToSet(channels, channel, systemId, persist);
    }
    
    @Override
    public SocketChannel syncClientSocketChannel(final String host, final int port, final int systemId)
    {
        return syncClientSocketChannel(host, port, systemId, true);
    }
    
    @Override
    public SocketChannel syncClientSocketChannel(final String host, final int port, final int systemId, 
            final boolean persist)
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ClientSocketChannel.HOST_PROP_KEY, host);
        properties.put(ClientSocketChannel.PORT_PROP_KEY, port);
        
        final SocketChannel channel = 
                (SocketChannel)syncChannel(systemId, properties, m_ClientSocketChannelFactory, persist);
        sendInitialConnectionMessage(channel);
        return channel;
    }
    
    @Override
    public synchronized TransportChannel syncTransportChannel(final String transportLayerName,
            final String localMessageAddress, final String remoteMessageAddress, final int systemId)
    {
        return syncTransportChannel(transportLayerName, localMessageAddress, remoteMessageAddress, systemId, true);
    }
    
    @Override
    public synchronized TransportChannel syncTransportChannel(final String transportLayerName, 
            final String localMessageAddress, final String remoteMessageAddress, final int systemId, 
            final boolean persist)
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY, transportLayerName);
        properties.put(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY, localMessageAddress);
        properties.put(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY, remoteMessageAddress);
        
        final TransportChannel channel = 
                (TransportChannel)syncChannel(systemId, properties, m_TransportChannelFactory, persist); 
        return channel;
    }
    
    @Override
    public synchronized boolean removeChannel(final RemoteChannel channel)
    {
        boolean foundChannelInList = false;
        // find the channel in the map
        for (int id : m_ChannelMap.keySet())
        {
            for (RemoteChannel channelInList : m_ChannelMap.get(id))
            {
                if (channelInList.equals(channel))
                {
                    // found channel in lookup, remove it and stop looking
                    removeChannelFromSet(m_ChannelMap.get(id), channelInList, id);
                    foundChannelInList = true;
                    break;
                }
            }
        }
        // dispose of instance even if not in lookup list, might be in instance list if never synced
        final ComponentInstance instance = m_Instances.remove(channel);
        if (instance != null)
        {
            instance.dispose();
        }
        
        if (foundChannelInList)
        {
            //notify that a channel has been removed
            final Event channeRemoved = new Event(TOPIC_CHANNEL_REMOVED, new HashMap<String, Object>());
            m_EventAdmin.postEvent(channeRemoved);
        }
        
        return foundChannelInList;
    }
    
    @Override
    public boolean checkChannelSocketExists(final String host, final  int port)
    {
        final Map<Integer, Set<RemoteChannel>> channels = getAllChannels();
        
        for (Set<RemoteChannel> channelSet : channels.values())
        {
            for (RemoteChannel channel : channelSet)
            {
                if (channel instanceof SocketChannel)
                {
                    final SocketChannel socket = (SocketChannel)channel;
                    
                    if (socket.getHost().equals(host) && socket.getPort() == port)
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    

    @Override
    public boolean checkChannelTransportExists(final String localAddr, final String remoteAddr)
    {
        final Map<Integer, Set<RemoteChannel>> channels = getAllChannels();
        
        for (Set<RemoteChannel> channelSet : channels.values())
        {
            for (RemoteChannel channel : channelSet)
            {
                if (channel instanceof TransportChannel)
                {
                    final TransportChannel transport = (TransportChannel)channel;
                    
                    if (transport.getLocalMessageAddress().equals(localAddr) 
                            && transport.getRemoteMessageAddress().equals(remoteAddr))
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Restore all previously saved channels.
     */
    private void restoreChannels()
    {
        m_Restoring = true;
        
        // read data from persistent store for each system
        final Collection<PersistentData> systemChannelDataList = m_PersistentDataStore.query(getClass());
        for (PersistentData systemChannelData : systemChannelDataList)
        {
            final SystemChannels systemChannels;
            try
            {
                systemChannels = SystemChannels.parseFrom((byte[])systemChannelData.getEntity());
            }
            catch (final InvalidProtocolBufferException e)
            {
                m_Logging.error(e, "Unable to parse channel for system %s", systemChannelData.getDescription());
                return;
            }
            
            // restore all socket channels
            for (SocketChannelType socketChannel : systemChannels.getSocketChannelList())
            {
                syncClientSocketChannel(socketChannel.getHost(), socketChannel.getPort(), systemChannels.getSysId());
            }
            
            // restore all transport channels
            for (TransportChannelType transportChannel : systemChannels.getTransportChannelList())
            {
                syncTransportChannel(transportChannel.getTransportName(), transportChannel.getLocalAddress(), 
                        transportChannel.getRemoteAddress(), systemChannels.getSysId());
            }
        }
        
        m_Restoring = false;
    }
    
    /**
     * Sync the channel with the lookup using the given properties and create a new channel using the given factory if 
     * channel does not exist in the loop.
     * 
     * @param systemId
     *      id of the controller to associate the channel with, channel can be used to send message to controller with
     *      this id
     * @param properties
     *      properties of the channel, keys are specific to the implementation of the {@link RemoteChannel}.
     * @param factory
     *      factory used to create the channel if it doesn't exist in the lookup
     * @param persist
     *      boolean used to determine if the channel being synced should be persisted
     * @return
     *      Returns the newly synced channel
     */
    private RemoteChannel syncChannel(final int systemId, final Map<String, Object> properties, 
            final ComponentFactory factory, final boolean persist)
    {
        Set<RemoteChannel> channels = m_ChannelMap.get(systemId);
        if (channels == null)
        {
            // first channel for this id, create the set
            channels = new HashSet<RemoteChannel>();
            m_ChannelMap.put(systemId, channels);
        }
        
        final int currentId = findIdByChannelProps(properties);
        
        // if channel not found add it
        if (currentId == -1)
        {
            final ComponentInstance instance = factory.newInstance(new Hashtable<String, Object>(properties));
            final RemoteChannel channel = (RemoteChannel)instance.getInstance();
            m_Instances.put(channel, instance);
            addChannelToSet(channels, channel, systemId, persist);
            
            m_Logging.debug("Created new channel [%s] for system id 0x%08x", channel, systemId);
            
            return channel;
        }
        
        // remove channel from current system list and add to new id
        final Set<RemoteChannel> otherSet = m_ChannelMap.get(currentId);
        for (RemoteChannel channel : otherSet)
        {
            if (channel.matches(properties))
            {
                removeChannelFromSet(otherSet, channel, currentId);
                addChannelToSet(channels, channel, systemId, persist);
                return channel;
            }
        }
        
        // should not be logically possible to get here, but the compiler is not that smart
        throw new IllegalStateException("Could not find channel, was there in call to findIdByChannelProps");
    }
    
    /**
     * Add a new channel to the given set and optionally persists the channel set to the {@link PersistentDataStore}. 
     * {@link ServerSocketChannel} will not be persisted, only added to the set.
     * 
     * @param channels
     *      Set to add new channel to and persist
     * @param newChannel
     *      New channel to add and persist
     * @param systemId
     *      System id for the channel set
     * @param persist
     *      Boolean used to determine if the channel being added to the set should also be persisted.
     */
    private void addChannelToSet(final Set<RemoteChannel> channels, final RemoteChannel newChannel, final int systemId,
            final boolean persist)
    {
        channels.add(newChannel);

        // ignore server socket channels, they don't get persisted, must be accepted by socket server each time system
        // is started
        if (newChannel instanceof ServerSocketChannel)
        {
            return;
        }
        
        if (!m_Restoring && persist) // if channels are being added due to restoration, no need to persist
        {
            final SystemChannels systemChannels = translateToProto(channels, systemId);
            persistChannelData(systemChannels);
        }
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, newChannel);
        
        final Event channelUpdated = new Event(TOPIC_CHANNEL_UPDATED, props);
        m_EventAdmin.postEvent(channelUpdated);
    }

    /**
     * Remove the channel from the set and persist the changes.
     *  
     * @param channels
     *      Set of channels to remove channel from
     * @param channel
     *      Channel to remove from set
     * @param systemId
     *      System id for channel set
     */
    private void removeChannelFromSet(final Set<RemoteChannel> channels, final RemoteChannel channel, 
            final int systemId)
    {
        channels.remove(channel);
        
        // ignore server socket channels, they don't get persisted, must be accepted by socket server each time system
        // is started
        if (channel instanceof ServerSocketChannel)
        {
            return;
        }
        
        final SystemChannels systemChannels = translateToProto(channels, systemId);
        persistChannelData(systemChannels);
    }
    
    /**
     * Translate the channel set to data modeled by proto class {@link SystemChannels}.
     * 
     * @param channels
     *      channel set to serialize
     * @param systemId
     *      system id for the channel set, will be included in XML data
     * @return
     *      system channel data
     */
    private SystemChannels translateToProto(final Set<RemoteChannel> channels, final int systemId)
    {
        final SystemChannels.Builder builder = SystemChannels.newBuilder().setSysId(systemId);
        
        for (RemoteChannel channel : channels)
        {
            if (channel instanceof ClientSocketChannel)
            {
                final ClientSocketChannel clientSocketChannel = (ClientSocketChannel)channel;
                builder.addSocketChannel(SocketChannelType.newBuilder()
                        .setHost(clientSocketChannel.getHost())
                        .setPort(clientSocketChannel.getPort())
                        .build());
            }
            else if (channel instanceof TransportChannel)
            {
                final TransportChannel transportChannel = (TransportChannel)channel;
                builder.addTransportChannel(TransportChannelType.newBuilder()
                        .setTransportName(transportChannel.getTransportLayerName())
                        .setLocalAddress(transportChannel.getLocalMessageAddress())
                        .setRemoteAddress(transportChannel.getRemoteMessageAddress())
                        .build());
            }
        }           
        
        return builder.build();
    }
    
    /**
     * Persist data for a whole channel set where the string description in the store will be the system id.
     * 
     * @param systemChannels
     *      channel data to persist
     */
    private void persistChannelData(final SystemChannels systemChannels)
    {
        final String systemIdStr = Integer.toString(systemChannels.getSysId());
        // merge with existing if already there
        final Collection<PersistentData> existingDataList = m_PersistentDataStore.query(getClass(), systemIdStr);
        if (existingDataList.isEmpty())
        {
            // not found, persist new entry
            try
            {
                m_PersistentDataStore.persist(getClass(), UUID.randomUUID(), systemIdStr, systemChannels.toByteArray());
            }
            catch (final PersistenceFailedException e)
            {
                m_Logging.error(e, "Unable to persist new channel for system id 0x%08x", systemChannels.getSysId());
            }
        }
        else
        {
            final PersistentData existingData = existingDataList.iterator().next();
            existingData.setEntity(systemChannels.toByteArray());
            try
            {
                m_PersistentDataStore.merge(existingData);
            }
            catch (final PersistenceFailedException | IllegalArgumentException | ValidationFailedException e)
            {
                m_Logging.error(e, "Unable to update existing channel(s) for system id 0x%08x", 
                        systemChannels.getSysId());
            }
        }
    }

    /**
     * Find the controller id that is associated with the given channel.
     * 
     * @param properties
     *      properties associated with the channel
     * @return
     *      id of the channel or -1 if not found
     */
    private int findIdByChannelProps(final Map<String, Object> properties)
    {
        for (int id : m_ChannelMap.keySet())
        {
            for (RemoteChannel channel : m_ChannelMap.get(id))
            {
                if (channel.matches(properties))
                {
                    return id;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Sends a controller info response message upon connection of a channel.
     * 
     * @param channel
     *      The channel to send the controller info message over.
     */
    private void sendInitialConnectionMessage(final RemoteChannel channel)
    {
        final int destId = getChannelSystemId(channel);
        
        final EncryptType encryptionType = 
                EnumConverter.convertEncryptionModeToEncryptType(m_RemoteSettings.getEncryptionMode());
        final GetEncryptionTypeResponseData encryptionInfo = 
                GetEncryptionTypeResponseData.newBuilder().setType(encryptionType).build();
        
        final EncryptionInfoNamespace namespace = EncryptionInfoNamespace.newBuilder()
                .setData(encryptionInfo.toByteString())
                .setType(EncryptionInfoMessageType.GetEncryptionTypeResponse).build();
        final TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.EncryptionInfo)
                .setNamespaceMessage(namespace.toByteString()).build();
        final TerraHarvestMessage message = TerraHarvestMessage.newBuilder().setDestId(destId)
                .setSourceId(m_TerraHarvestSystem.getId()).setIsResponse(true).setMessageId(Integer.MAX_VALUE)
                .setVersion(RemoteConstants.SPEC_VERSION).setTerraHarvestPayload(payload.toByteString())
                .setEncryptType(EncryptType.NONE).build();
        channel.queueMessage(message);
    }
    
    /**
     * Handles local events and performs action based on event received.
     *
     */
    class EventHandlerImpl implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;
        
        /**
         * The OSGi bundle context for this bundle.
         */
        private final BundleContext m_Context;
        
        /**
         * Public constructor for the event handler.
         * @param context
         *   the bundle context used to register the event handler   
         */
        EventHandlerImpl(final BundleContext context)
        {
            m_Context = context;
        }

        @Override
        public void handleEvent(final Event event)
        {
            final RemoteChannel channel = (RemoteChannel)event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL);
            
            if (event.getTopic().equals(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID))
            {
                // new/changed id, call sync
                final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SYS_ID);
                syncChannel(channel, systemId);
            }
            else if (event.getTopic().equals(RemoteConstants.TOPIC_REMOVE_CHANNEL))
            {
                // remove channel
                removeChannel(channel);
            }
        }
        
        /**
         * Method to register this event handler for the sync and remove event 
         * topics.
         */
        public void registerForSyncRemoveEvents()
        {
            //dictionary of properties for the event handler for TOPIC_SYNC_CHANNEL
            final Dictionary<String, Object> propsSyncRemove = new Hashtable<String, Object>();
            final String[] topicSyncRemove = {RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, 
                RemoteConstants.TOPIC_REMOVE_CHANNEL}; //event filter
            propsSyncRemove.put(EventConstants.EVENT_TOPIC, topicSyncRemove);
            
            //register the event handler that listens for cue to sync a channel and remove a channel
            m_ServiceReg = m_Context.registerService(EventHandler.class, this, propsSyncRemove);
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            m_ServiceReg.unregister();
        }
    }
}
