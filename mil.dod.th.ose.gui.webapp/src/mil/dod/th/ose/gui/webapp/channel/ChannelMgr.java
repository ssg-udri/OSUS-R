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

import java.util.List;

import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.controller.ControllerStatus;

/**
 * Manages channels to remote Terra Harvest Systems from this system. This manager creates and destroys
 * the channels only. The actual communication channels are not accessible via this manager instead 
 * {@link Channel} objects are created representing these channels. The manager
 * syncs the channel with the {@link mil.dod.th.core.remote.RemoteChannelLookup} service and sends a 
 * verifying Terra Harvest Message to confirm that the channel is valid. 
 * Once a response from the remote system is received, the channel is updated to an active status.
 * Any further interactions utilizing the created channel is done via various
 * remote interface interactions and NOT through this manager.
 * @author callen
 *
 */
public interface ChannelMgr 
{
    /**
     * The default port value.
     */
    int DEFAULT_PORT = 4000;
    
    /**
     * Create a new {@link SocketChannelModel} type channel.
     * @param controllerId
     *     the Terra Harvest System id that the channel will be used to communicate with
     * @param hostName
     *     the hostname of the socket type channel
     * @param socketPort
     *     the port to use for the new socket channel, if the value is 0 the default, 4000, will be assigned      
     */
    void createSocketChannel(int controllerId, String hostName, int socketPort);   
    
    /**
     * Create a new {@link TransportChannelModel} type channel.
     * @param controllerId
     *     the Terra Harvest System id that the channel will be used to communicate with
     * @param transportName
     *     the name of the transport layer to use for this channel
     * @param remoteMessageAddress
     *     the destination address to use for the channel
     * @param localMessageAddress
     *     the local address to use for the channel
     */
    void createTransportChannel(int controllerId, String transportName, String localMessageAddress, 
            String remoteMessageAddress);
    
    /**
     * Remove the specified channel. This removes the channel from the remote channel lookup service.
     * @param channel
     *     the channel object representing the {@link mil.dod.th.core.remote.RemoteChannel} to remove
     */
    void removeChannel(Channel channel);
    
    /**
     * Get a transport layer based channel object by name and address.
     * @param transportName
     *      the name of the transport layer 
     * @param localMessageAddress
     *      the local address of the channel
     * @param remoteMessageAddress
     *      the address of the destination for the channel    
     * @return
     *      transport layer channel base object
     */
    TransportChannelModel getTransportLayerChannel(String transportName, String localMessageAddress, 
            String remoteMessageAddress);
    
    /**
     * List of all transport layer channels known to the local system.
     * @return
     *    list of transport layer based channel base objects
     */
    List<TransportChannelModel> getAllTransportLayerChannels();
    
    /**
     * Get a socket channel base object by host name and port.
     * @param hostName
     *    host name used to register the channel
     * @param port
     *    the port of the channel     
     * @return 
     *    channel base object representing this socket channel
     */
    SocketChannelModel getSocketChannel(String hostName, int port);
    
    /**
     * Get all socket type channels.
     * @return
     *     list of channel base objects representing socket type channels
     */
    List<SocketChannelModel> getAllSocketChannels();
    
    /**
     * Get channels for a controller by {@link RemoteChannelTypeEnum} type.
     * @param controllerId
     *     the Terra Harvest System id
     * @param type
     *     the type of the channel (IE socket, transport etc)    
     * @return
     *     list of channels that belong to the system with the given ID
     */
    List<Channel> getChannelsForController(int controllerId, RemoteChannelTypeEnum type);
    
    /**
     * Get all channels known to the channel manager for the system with the id passed.
     * @param controllerId
     *     the Terra Harvest System id
     * @return
     *     list of channels that belong to the system with the given ID
     */
    List<Channel> getChannelsForController(int controllerId);
    
    /**
     * Get all channels know to the channel manager.
     * @return
     *     a list of channels representing all the channels known to the system
     */
    List<Channel> getAllChannels();
    
    /**
     * Method that renders the controller status for the controller specified.
     * @param controllerId
     *     the id of the controller to get the status of
     * @return
     *     the status of the controller
     */
    ControllerStatus getStatusForController(int controllerId);
    
    /**
     * Method that clears all queued messages from a channel.
     * @param channel
     *      channel to clear all queued messages from
     */
    void clearChannelQueue(Channel channel);
}
