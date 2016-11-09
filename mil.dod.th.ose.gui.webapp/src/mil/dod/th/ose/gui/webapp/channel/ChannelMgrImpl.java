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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;

import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.controller.ControllerStatus;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.service.component.ComponentException;

/**
 * Implementation of the {@link ChannelMgr}.
 * 
 * @author callen
 *
 */
@ManagedBean(name = "channelManager")
@RequestScoped
public class ChannelMgrImpl implements ChannelMgr
{    
    /**
     * Inject the RemoteChannelLookup service for registering new remote channels.
     */
    @Inject @OSGiService
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Set the {@link RemoteChannelLookup} service.
     * @param lookup
     *     the remote channel lookup service to use
     */
    public void setRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        m_RemoteChannelLookup = lookup;
    }
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    @Override
    public void createSocketChannel(final int controllerId, final String hostName, final int portEntered)
    {
        // figure out if 0 is the port passed in, if so then use the default port, otherwise the passed value
        final int actualPort = portEntered == 0 ? DEFAULT_PORT : portEntered;
        
        if (m_RemoteChannelLookup.checkChannelSocketExists(hostName, actualPort))
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Socket channel already exists", 
                    String.format("Socket channel with specified hostname: %s and port: %s already exists.", hostName, 
                            actualPort));
            return;
        }
        
        // create the remote socket channel
        try
        {
            m_RemoteChannelLookup.syncClientSocketChannel(hostName, actualPort, controllerId);
        }
        catch (final ComponentException e)
        {
            // TODO: TH-644: Remove catch once IOException is caught by ClientSocketChannel so exception is not thrown
            // here
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to connect", 
                    String.format("Unable to connect to controller at %s:%d", hostName, actualPort), e);
            return;
        }
    }
    
    @Override
    public void createTransportChannel(final int controllerId, final String transportName, 
            final String localMessageAddress, final String remoteMessageAddress)
    { 
        
        //sync the transport layer as a remote channel
        m_RemoteChannelLookup.syncTransportChannel(transportName, localMessageAddress, 
                remoteMessageAddress, controllerId);
    }
    
    @Override
    public void removeChannel(final Channel channel)
    {
        //pull-out the controller id for the channel
        final RemoteChannel remoteChannel;
        switch (channel.getChannelType())
        {
            case TRANSPORT:
                final TransportChannelModel transportChannelModel = (TransportChannelModel)channel;
                remoteChannel = m_RemoteChannelLookup.getTransportChannel(transportChannelModel.getName(), 
                        transportChannelModel.getLocalMessageAddress(), 
                        transportChannelModel.getRemoteMessageAddress());
                break;
        
            case SOCKET:
                final SocketChannelModel socketChannelModel = (SocketChannelModel)channel;
                remoteChannel = m_RemoteChannelLookup.getSocketChannel(socketChannelModel.getHost(), 
                        socketChannelModel.getPort());
                break;
                
            default:
                throw new UnsupportedOperationException(
                        "Unable to remove channel of type: " + channel.getChannelType());
        }
        m_RemoteChannelLookup.removeChannel(remoteChannel);
        
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Channel Removed", 
                String.format("%s channel removed for controller 0x%08x", 
                        channel.getChannelType(), channel.getControllerId()));
    }
    
    @Override
    public TransportChannelModel getTransportLayerChannel(final String transportName, final String localMessageAddress, 
            final String remoteMessageAddress)
    {
        //get all channels
        final Map<Integer, Set<RemoteChannel>> remoteChannels = m_RemoteChannelLookup.getAllChannels();

        for (Integer systemId : remoteChannels.keySet())
        {
            final Set<RemoteChannel> channels = remoteChannels.get(systemId);
            for (RemoteChannel channel : channels)
            {
                if (channel.getChannelType() == RemoteChannelTypeEnum.TRANSPORT)
                {
                    final TransportChannel transportChannel = (TransportChannel)channel;
                    
                    //check that the name of the layer and message address match the channel's properties
                    if (transportName.equals(transportChannel.getTransportLayerName()) 
                            && localMessageAddress.equals(transportChannel.getLocalMessageAddress())
                            && remoteMessageAddress.equals(transportChannel.getRemoteMessageAddress()))
                    {
                        return new TransportChannelModel(systemId, transportChannel);
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public List<TransportChannelModel> getAllTransportLayerChannels()
    {
        //list to return
        final List<TransportChannelModel> transChannels = new ArrayList<TransportChannelModel>();
        
        //get all channels
        final Map<Integer, Set<RemoteChannel>> remoteChannels = m_RemoteChannelLookup.getAllChannels();

        for (Integer systemId : remoteChannels.keySet())
        {
            final Set<RemoteChannel> channels = remoteChannels.get(systemId);
            for (RemoteChannel channel : channels)
            {
                if (channel.getChannelType() == RemoteChannelTypeEnum.TRANSPORT)
                { 
                    // construct the transport model
                    final TransportChannelModel transModel = 
                            new TransportChannelModel(systemId, (TransportChannel)channel);
                    transChannels.add(transModel);
                }
            }
        }
        return transChannels;
    }
    
    @Override
    public SocketChannelModel getSocketChannel(final String hostName, final int port)
    {
        //get all channels
        final Map<Integer, Set<RemoteChannel>> remoteChannels = m_RemoteChannelLookup.getAllChannels();

        for (Integer systemId : remoteChannels.keySet())
        {
            final Set<RemoteChannel> channels = remoteChannels.get(systemId);
            for (RemoteChannel channel : channels)
            {
                if (channel.getChannelType() == RemoteChannelTypeEnum.SOCKET)
                {
                    final SocketChannel socketChannel = (SocketChannel)channel;
                    
                    //check that the port passed matches the sockets port, and that the InetAddress host name matches
                    //the host name passed in
                    if (socketChannel.getPort() == port && socketChannel.getHost().equals(hostName))
                    {
                        final SocketChannelModel tcpChannel = new SocketChannelModel(systemId, socketChannel);
                        //return the channel
                        return tcpChannel;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public List<SocketChannelModel> getAllSocketChannels()
    {
        //list to return
        final List<SocketChannelModel> tcpChannels = new ArrayList<SocketChannelModel>();
        
        //get all channels
        final Map<Integer, Set<RemoteChannel>> remoteChannels = m_RemoteChannelLookup.getAllChannels();
        
        for (Integer systemId : remoteChannels.keySet())
        {
            final Set<RemoteChannel> channels = remoteChannels.get(systemId);
            for (RemoteChannel channel : channels)
            {
                if (channel.getChannelType() == RemoteChannelTypeEnum.SOCKET)
                {
                    final SocketChannelModel tcpChannel = new SocketChannelModel(systemId, (SocketChannel)channel);
                    tcpChannels.add(tcpChannel);
                }
            }
        }
        return tcpChannels;  
    }
    
    @Override
    public List<Channel> getChannelsForController(final int controllerId)
    {
        final List<Channel> channels = new ArrayList<Channel>();
        
        //Remote channels for the specified system
        final List<RemoteChannel> rChannels = m_RemoteChannelLookup.getChannels(controllerId);
        
        // Go through all channels, figure type and create the channel model for that type
        for (RemoteChannel channel : rChannels)
        {
            //pull-out channel type
            final RemoteChannelTypeEnum chanType = channel.getChannelType();
            if (chanType == RemoteChannelTypeEnum.SOCKET)
            {
                final SocketChannelModel socketChannel = new SocketChannelModel(controllerId, (SocketChannel)channel);
                //add to the list of channels
                channels.add(socketChannel); 
            }
            else if (chanType == RemoteChannelTypeEnum.TRANSPORT)
            {
                final TransportChannelModel transModel = 
                        new TransportChannelModel(controllerId, (TransportChannel)channel);
                //add to the list of channels
                channels.add(transModel);
            }
        }
        return channels;
    }
    
    @Override
    public List<Channel> getChannelsForController(final int controllerId, final RemoteChannelTypeEnum type)
    {
        //list of channels to return
        final List<Channel> channels = new ArrayList<Channel>();
        
        //Remote channels for the specified system
        final List<RemoteChannel> rChannels = m_RemoteChannelLookup.getChannels(controllerId);
        
        // Go through all channels figure type and create the channel model for that type
        for (RemoteChannel channel : rChannels)
        {
            //pull-out channel type
            final RemoteChannelTypeEnum chanType = channel.getChannelType();
            if (chanType.equals(type) && type == RemoteChannelTypeEnum.SOCKET)
            {
                final SocketChannelModel tcpChannel = new SocketChannelModel(controllerId, (SocketChannel)channel);
                //add to the list of channels
                channels.add(tcpChannel);
            }
            else if (chanType.equals(type) && type == RemoteChannelTypeEnum.TRANSPORT)
            {
                final TransportChannelModel transModel = 
                        new TransportChannelModel(controllerId, (TransportChannel)channel);
                //add to the list of channels
                channels.add(transModel);
            }
        }
        return channels; 
    }
    
    @Override
    public List<Channel> getAllChannels()
    {
        final List<Channel> channels = new ArrayList<Channel>();
        
        //Remote channels for the entire system
        final Map<Integer, Set<RemoteChannel>> rChannels = m_RemoteChannelLookup.getAllChannels();
        
        // Go through all channels figure type and create the channel model for that type
        for (Integer systemId : rChannels.keySet())
        {
            final Set<RemoteChannel> channelSet = rChannels.get(systemId);
            for (RemoteChannel channel : channelSet)
            {
                if (channel.getChannelType() == RemoteChannelTypeEnum.SOCKET)
                {
                    final SocketChannelModel tcpChannel = new SocketChannelModel(systemId, (SocketChannel)channel);
                    //add to the list of channels
                    channels.add(tcpChannel);                    
                }
                else
                {
                    final TransportChannelModel transModel = 
                            new TransportChannelModel(systemId, (TransportChannel)channel);
                    //add to the list of channels
                    channels.add(transModel);
                }
            }
        }
        return channels;    
    }
    
    @Override
    public ControllerStatus getStatusForController(final int controllerId)
    {
        final List<RemoteChannel> channels = m_RemoteChannelLookup.getChannels(controllerId);
        boolean channelsUp = false;
        boolean channelsDown = false;
        for (RemoteChannel channel : channels)
        {
            if (channel.getStatus() == ChannelStatus.Active)
            {
                channelsUp = true;
            }
            else
            {
                channelsDown = true;
            }
        }
        if (channelsDown && channelsUp)
        {
            return ControllerStatus.Degraded;
        }
        else if (channelsDown && !channelsUp)
        {
            return ControllerStatus.Bad; 
        }
        else
        {
            return ControllerStatus.Good;
        }
    }
    
    @Override
    public void clearChannelQueue(final Channel channel)
    {
        //pull-out the controller id for the channel
        final RemoteChannel remoteChannel;
        switch (channel.getChannelType())
        {
            case TRANSPORT:
                final TransportChannelModel transportChannelModel = (TransportChannelModel)channel;
                remoteChannel = m_RemoteChannelLookup.getTransportChannel(transportChannelModel.getName(), 
                        transportChannelModel.getLocalMessageAddress(), 
                        transportChannelModel.getRemoteMessageAddress());
                break;
        
            case SOCKET:
                final SocketChannelModel socketChannelModel = (SocketChannelModel)channel;
                remoteChannel = m_RemoteChannelLookup.getSocketChannel(socketChannelModel.getHost(), 
                        socketChannelModel.getPort());
                break;

            default:
                final String error = "Unable to clear channel queue of type: " + channel.getChannelType();
                //notify the user
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Clear Channel Queue Error", 
                        String.format(error + " from Controller 0x%08x", channel.getControllerId()));
                throw new UnsupportedOperationException(error);
        }
        remoteChannel.clearQueuedMessages();
        
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Channel Queue Cleared", 
                String.format("%s channel queue cleared for controller 0x%08x", 
                        channel.getChannelType(), channel.getControllerId()));
    }
}
