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

package mil.dod.th.core.remote;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * Maintains a list of channels and matches them up with a TerraHarvest system id that identifies the endpoint.
 * TerraHarvestMessages are sent with a source and destination address which is a system id. This lookup is used to get
 * the channel to actually send the message. Lookup is populated as messages are received from channels. All channels 
 * shall be persisted except for server sockets. Server sockets must be synced again after startup to be added back to 
 * the lookup (reconnect).
 * 
 * @author Dave Humeniuk
 */
@ProviderType
public interface RemoteChannelLookup
{
    /** Event topic prefix to use for all topics in RemoteConstants. */
    String TOPIC_PREFIX = "mil/dod/th/core/remote/RemoteChannelLookup/";
    
    /**
     * Topic used when a channel has been removed.
     * 
     */
    String TOPIC_CHANNEL_REMOVED = TOPIC_PREFIX + "CHANNEL_REMOVED";
    
    /**
     * Topic used when a channel has been updated.  Occurs either when a new channel is created and associated with a 
     * system or an existing channel is now associated with a new system id.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link RemoteConstants#EVENT_PROP_SYS_ID}  - the id of the system to which the channel has been created</li>
     * <li>{@link RemoteConstants#EVENT_PROP_CHANNEL} - the channel that has been created</li>
     * </ul>
     */
    String TOPIC_CHANNEL_UPDATED = TOPIC_PREFIX + "CHANNEL_UPDATED";
    
    
    /**
     * Lookup a channel by the system id that represents the endpoint of the channel. A single endpoint may have
     * multiple channels, just return one of them.
     * 
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @return
     *      the channel for the given id
     * @throws IllegalArgumentException
     *      if the given id is not in the lookup
     */
    RemoteChannel getChannel(int systemId) throws IllegalArgumentException;
    
    /**
     * Lookup the system ID that a channel belongs to.
     * 
     * @param channel
     *      the {@link RemoteChannel} to find the system ID for.
     * @return
     *      integer that represents the system ID the channel belongs to.
     */
    int getChannelSystemId(RemoteChannel channel);
    
    /**
     * Get the socket based {@link RemoteChannel} in the lookup based on host and port.  Channel does not have to be 
     * associated with a system to be returned.
     * 
     * @param hostname
     *      hostname of the remote endpoint
     * @param port
     *      port of the remote endpoint
     * @return
     *      instance of the channel that is associated with the socket with hostname and port given or null if not found
     */
    SocketChannel getSocketChannel(String hostname, int port);
    
    /**
     * Get the {@link mil.dod.th.core.ccomm.transport.TransportLayer} based {@link RemoteChannel} in the lookup.  
     * Channel does not have to be associated with a system to be returned.
     * 
     * @param transportLayerName
     *      name of the transport layer used to send/receive messages
     * @param localMessageAddress
     *      address of the local endpoint where messages are sent from/come in
     * @param remoteMessageAddress
     *      address of the remote endpoint where messages go/come from
     * @return
     *      instance of the channel that is associated with the transport layer with hostname and port given
     */
    TransportChannel getTransportChannel(String transportLayerName, String localMessageAddress, 
            String remoteMessageAddress);
    
    /**
     * Lookup a channels by the system id that represents the endpoint of the channel. A single endpoint may have
     * multiple channels. This call will return all of them. 
     * 
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @return
     *      list of channels for the given id, list will be empty if no channels found
     */
    List<RemoteChannel> getChannels(int systemId);
    
    /**
     * Lookup all channels known to the local system. A single endpoint may have
     * multiple channels. This call will return a map of all remote channels for all 
     * {@link mil.dod.th.core.system.TerraHarvestSystem} ids. 
     * 
     * @return
     *      map of all channels known to the system, by id
     */
    Map<Integer, Set<RemoteChannel>> getAllChannels();

    /**
     * Create a new server socket based {@link RemoteChannel}.
     * 
     * This method should be called if this is the socket that was accepted by the server socket and not a new client 
     * side socket.  Call {@link #syncClientSocketChannel(String, int, int)} instead.
     * 
     * This will only create a remote channel, need to call {@link #syncChannel(RemoteChannel, int)} to associate the 
     * channel with a system so the {@link mil.dod.th.core.remote.messaging.MessageFactory} can be used to send 
     * messages through the channel based on the system id.
     * 
     * @param socket
     *      socket accepted by the server socket for a connection
     * @return
     *      the new socket channel created
     */
    SocketChannel newServerSocketChannel(Socket socket);
    
    /**
     * Synchronize the channel with the system id.  This allows the 
     * {@link mil.dod.th.core.remote.messaging.MessageFactory} to send messages to the given system id with the 
     * given channel.
     * 
     * This method should be used once a channel has been created and called any time the system id changes. This method
     * optionally persists the channel.
     * 
     * @param channel
     *      the channel to sync
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @param persist
     *      true if the channel should be persisted, false if it should not be persisted
     */
    void syncChannel(RemoteChannel channel, int systemId, boolean persist);
    
    /**
     * Synchronize the channel with the system id.  This allows the 
     * {@link mil.dod.th.core.remote.messaging.MessageFactory} to send messages to the given system id with the 
     * given channel.
     * 
     * This method should be used once a channel has been created and called any time the system id changes. This method
     * will always persist the channel.
     * 
     * @param channel
     *      the channel to sync
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     */
    void syncChannel(RemoteChannel channel, int systemId);

    /**
     * Add the {@link SocketChannel} to the lookup if new.  Channel is new if the combination of host and port is not in
     * the lookup table.
     * 
     * If the channel is not new, update the associated system id. This method optionally persists the new or updated
     * socket channel and optionally enables SSL on the socket.
     * 
     * @param host
     *      hostname of the remote system to connect to
     * @param port
     *      port that the remote system is listening on
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @param persist
     *      true if the channel should be persisted, false otherwise
     * @param useSsl
     *      true if SSL should be enabled on the socket, false otherwise
     * @return
     *      channel instance that is associated with the host and port
     */
    SocketChannel syncClientSocketChannel(String host, int port, int systemId, boolean persist, boolean useSsl);
    
    /**
     * Add the {@link SocketChannel} to the lookup if new.  Channel is new if the combination of host and port is not in
     * the lookup table.
     * 
     * If the channel is not new, update the associated system id. This method optionally persists the new or updated
     * socket channel. SSL will be disabled on the socket.
     * 
     * @param host
     *      hostname of the remote system to connect to
     * @param port
     *      port that the remote system is listening on
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @param persist
     *      true if the channel should be persisted, false otherwise
     * @return
     *      channel instance that is associated with the host and port
     */
    SocketChannel syncClientSocketChannel(String host, int port, int systemId, boolean persist);
    
    /**
     * Add the {@link SocketChannel} to the lookup if new.  Channel is new if the combination of host and port is not in
     * the lookup table.
     * 
     * If the channel is not new, update the associated system id. This method will always persist the new or
     * updated socket channel. SSL will be disabled on the socket.
     * 
     * @param host
     *      hostname of the remote system to connect to
     * @param port
     *      port that the remote system is listening on
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @return
     *      channel instance that is associated with the host and port
     */
    SocketChannel syncClientSocketChannel(String host, int port, int systemId);
    
    /**
     * Add the TransportLayer based {@link RemoteChannel} to the lookup if new.
     * 
     * Channel is new if the combination of layer and address are not in the lookup table.
     * 
     * If channel is not new, update the associated system id. This method optionally persists the new or updated
     * transport channel.
     * 
     * @param transportLayerName
     *      name of the transport layer used to send/receive messages
     * @param localMessageAddress
     *      address of the local endpoint where messages are sent from/come in
     * @param remoteMessageAddress
     *      address of the remote endpoint where messages go/come from
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @param persist
     *      true if the channel should be persisted, false if it should not be persisted
     * @return
     *      channel instance that is associated with the transport layer and address
     */
    TransportChannel syncTransportChannel(String transportLayerName, String localMessageAddress, 
            String remoteMessageAddress, int systemId, boolean persist);
    
    /**
     * Add the TransportLayer based {@link RemoteChannel} to the lookup if new.
     * 
     * Channel is new if the combination of layer and address are not in the lookup table.
     * 
     * If channel is not new, update the associated system id. This method will always persist the new or updated
     * transport channel.
     * 
     * @param transportLayerName
     *      name of the transport layer used to send/receive messages
     * @param localMessageAddress
     *      address of the local endpoint where messages are sent from/come in
     * @param remoteMessageAddress
     *      address of the remote endpoint where messages go/come from
     * @param systemId
     *      the {@link mil.dod.th.core.system.TerraHarvestSystem} id
     * @return
     *      channel instance that is associated with the transport layer and address
     */
    TransportChannel syncTransportChannel(String transportLayerName, String localMessageAddress, 
            String remoteMessageAddress, int systemId);
    
    /**
     * Remove the channel from the lookup.
     * 
     * @param channel
     *      channel to remove from the lookup
     * @return
     *      if channel was removed from lookup
     */
    boolean removeChannel(RemoteChannel channel);
    
    /**
     * Function checks for the existence of a socket channel.
     * @param host
     *  the hostname to match on
     * @param port
     *  the port to match on
     * @return
     *  If a socket channel and port exist for a channel associated with the {@link RemoteChannelLookup}
     *  then return true. Return false otherwise.
     */
    boolean checkChannelSocketExists(String host, int port);
    
    /**
     * Function checks for the existence of a transport channel.
     * @param localAddr
     *  the local address to check against
     * @param remoteAddr
     *  the remote address to check against
     * @return
     *  if a transport channel exists with the inputed remote and local addresses in the 
     *  {@link RemoteChannelLookup} then return true. Return false otherwise.
     */
    boolean checkChannelTransportExists(String localAddr, String remoteAddr);
}
