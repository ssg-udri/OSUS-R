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

import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.comms.CommsImage;

/**
 * Class handles general gui display interactions for the channels.xhtml page and its corresponding 
 * dialog files. This class contains helper functions which do not have direct interaction with 
 * creating or removing channels. Instead this class facilitates the display of data needed to be 
 * able to create/remove channels appropriately.
 * 
 * @author nickmarcucci
 *
 */
public interface ChannelGuiDialogHelper
{
    /**
     * Getter function for variable which holds the controller id 
     * to connect to.
     * 
     * @return
     *  the integer system id (controller id)
     */
    int getNewChannelControllerId();
    
    /**
     * Setter function for the variable which holds the controller id
     * to connect to.
     * 
     * @param controllerId
     *  the integer system id of the controller the user would like
     *  to make a channel for.
     */
    void setNewChannelControllerId(int controllerId);
    
    /**
     * Getter function for the variable which holds the host name of the 
     * socket that should be created.
     * 
     * @return
     *  the host name of the socket that will be created.
     */
    String getNewSocketHost();
    
    /**
     * Setter function for the variable which holds the host name of the 
     * socket that should be created.
     * 
     * @param host
     *  the host name that the socket should have.
     */
    void setNewSocketHost(String host);
    
    /**
     * Getter function for the variable which holds the port of the socket
     * that should be created.
     * 
     * @return
     *  the integer port number that the socket should use.
     */
    int getNewSocketPort();
    
    /**
     * Setter function for the variable which holds the port of the socket
     * that should be created.
     * 
     * @param port
     *  the integer number that the socket port should be set to.
     */
    void setNewSocketPort(int port);

    /**
     * Getter function for whether the new socket should use SSL or not.
     * 
     * @return
     *  true if SSL should be used, false otherwise
     */
    boolean isNewSocketSsl();

    /**
     * Setter function used to enable or disable SSL on the socket.
     * 
     * @param ssl
     *  true if SSL is enabled, false otherwise
     */
    void setNewSocketSsl(boolean ssl);

    /**
     * Getter function for the variable which holds the name of the transport layer
     * that should be created.
     * 
     * @return
     *  the name of the transport layer.
     */
    String getNewTransportName();
    
    /**
     * Setter function for the variable which holds the name of the transport layer
     * that should be created.
     * 
     * @param name
     *  the name that the transport layer should be set to.
     */
    void setNewTransportName(String name);
    
    /**
     * Getter function for the variable which holds the remote address of the transport 
     * layer that should be created.
     * 
     * @return
     *  the remote address that the transport layer should use.
     */
    String getNewTransportRemoteAddress();
    
    /**
     * Setter function for the variable which holds the remote address of the transport layer
     * that should be created.
     * 
     * @param address
     *  the remote address that the transport layer should use.
     */
    void setNewTransportRemoteAddress(String address);
    
    /**
     * Getter function for the variable which holds the local address of the transport 
     * layer that should be created.
     * 
     * @return
     *  the local address that the transport layer should use.
     */
    String getNewTransportLocalAddress();
    
    /**
     * Setter function for the variable which holds the local address of the transport layer
     * that should be created.
     * 
     * @param address
     *  the local address that the transport layer should use.
     */
    void setNewTransportLocalAddress(String address);
    
    /**
     * Getter function for the variable which determines which second dialog 
     * fragment to show. I.E. should the dialog be rendered with socket host/port
     * text boxes or transport layer name/address drop downs.
     * 
     * @return
     *  the type of {@link RemoteChannelTypeEnum} that has been requested.
     */
    RemoteChannelTypeEnum getSelectedNewChannelType();
    
    /**
     * Setter function for the variable which determines which second dialog fragment
     * to show.
     * 
     * @param channelChoice
     *  the {@link NewChannelChoice} variable which simply identifies the name and type
     *  of channel to create.
     */
    void setSelectedNewChannelType(NewChannelChoice channelChoice);
    
    /**
     * Getter function to return all of the possible channel choices that 
     * a user can create. 
     * 
     * @return
     *  a list of {@link NewChannelChoice} objects that represent available 
     *  channels to use.
     */
    List<NewChannelChoice> getChannelChoices();
    
    /**
     * Function used to clear/reset variables used in the process of adding a new
     * channel.
     */
    void clearNewChannelInput();
    
    /**
     * Get all the known transport layers currently being managed by the 
     * custom comms service.
     * 
     * @return
     *  a list of all known transport layers that the custom comms service has 
     *  a reference to
     */
    List<TransportLayer> getAllKnownTransportLayers();
    
    /**
     * Get all known addresses that are currently in use.
     * 
     * @return
     *  a list of address strings currently in the {@link mil.dod.th.core.ccomm.AddressManagerService}
     */
    List<String> getAllKnownAddresses();
    
    /**
     * Class which used for displaying available channel choices to the user. Currently, a NewChannelChoice
     * object can represent something like a socket or it can represent an individual tranport layer. In the 
     * case of the transport layer, the channel name will be set to the transport layer's name.
     * 
     * @author nickmarcucci
     */
    class NewChannelChoice
    {
        /**
         * The name of the channel that should be created.
         */
        private final String m_ChannelName;
        
        /**
         * The type that corresponds to the channel name that is 
         * shown.
         */
        private final RemoteChannelTypeEnum m_Type;
        
        /**
         * If this channel choice is a transport layer then it will hold its 
         * capabilities object.
         */
        private TransportLayerCapabilities m_TranCaps;
        
        /**
         * Reference to the channel image class.
         */
        private final CommsImage m_CommsImage;
        
        /**
         * Constructor.
         * 
         * @param name
         *  the name of the channel ("Socket" or transport layer name)
         * @param type
         *  the type of the channel
         */
        public NewChannelChoice(final String name, final RemoteChannelTypeEnum type)
        {
            super();
            m_Type = type;
            m_ChannelName = name;
            m_CommsImage = new CommsImage();
        }
        
        /**
         * Getter function for the channel name.
         * 
         * @return
         *  the name of the channel that should be created.
         */
        public String getChannelName()
        {
            return m_ChannelName;
        }
        
        /**
         * Getter function for the type of channel that 
         * this object is representing.
         * 
         * @return
         *  the {@link RemoteChannelTypeEnum} type that this channel represents.
         */
        public RemoteChannelTypeEnum getChannelType()
        {
            return m_Type;
        }
        
        /**
         * Function to set the capabilities object if the choice represents 
         * a transport layer.
         * 
         * @param caps
         *  the capabilities object to set.
         */
        public void setCapabilities(final TransportLayerCapabilities caps)
        {
            m_TranCaps = caps;
        }
        
        /**
         * Function to retrieve the capabilities object. Return value will be null
         * if object is not a transport layer.
         * 
         * @return
         *  the capabilities object if the choice represents a transport layer otherwise
         *  it will return null.
         */
        public TransportLayerCapabilities getCapabilities()
        {
            return m_TranCaps;
        }
        
        /**
         * Method to get an image for channel.
         * 
         * @return
         *      the string URL of the image for the channel
         */
        public String getImage()
        {
            //if channel is of type socket display the socket image
            if (getChannelName().equals("Socket"))
            {
                return m_CommsImage.getSocketImage();
            }
            
            return m_CommsImage.getTransportImage(getCapabilities());
        }
    }
}
