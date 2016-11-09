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

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;

import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of {@link ChannelGuiDialogHelper}.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "channelGuiDialogHelper")
@ViewScoped
public class ChannelGuiDialogHelperImpl implements ChannelGuiDialogHelper
{
    /**
     * Variable which holds the type of channel that a user is about
     * to create. Dictates which fragment for the dialog box to show.
     */
    private RemoteChannelTypeEnum m_SelectedChannelType;
    
    /**
     * Variable which holds the controller id which the user would 
     * like to connect to. By default it is set to the GUI's 
     * TerraHarvest system id.
     */
    private int m_NewChannelControllerId;
   
    /**
     * Variable which holds the new socket host name which a user enters
     * in when creating a new socket channel.
     */
    private String m_NewSocketHost;
    
    /**
     * Variable which holds the new socket port which a user enters in when
     * creating a new socket channel.
     */
    private int m_NewSocketPort;
    
    /**
     * Variable which holds the transport layer name. By default it is set to 
     * the name of the transport layer that the user has selected on the initial
     * add a channel dialog page.
     */
    private String m_NewTransportName;
    
    /**
     * Variable which holds the transport layer remote address.
     */
    private String m_NewTransportRemoteAddress;
    
    /**
     * Variable which holds the transport layer local address.
     */
    private String m_NewTransportLocalAddress;
    
    /**
     * Inject the CustomCommsService for retrieving available communications layers.
     */
    @Inject @OSGiService 
    private CustomCommsService m_CommsService;
    
    /**
     * Inject the AddressManagerService for retrieving all currently known addresses.
     */
    @Inject @OSGiService
    private AddressManagerService m_AddressService;
    
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
     * Faces context utility for retrieving a valid faces context.
     */
    @Inject
    private FacesContextUtil m_FacesUtil;
    
    /**
     * Set the address manager service.
     * @param addressService
     *  the address manager service to use
     */
    public void setAddressManagerService(final AddressManagerService addressService)
    {
        m_AddressService = addressService;
    }
    
    
    /**
     * Set the custom comms service. 
     * @param service
     *  the custom comms service to use
     */
    public void setCustomCommsService(final CustomCommsService service)
    {
        m_CommsService = service;
    }
    
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
    
    /**
     * Set the faces context utility service.
     * @param facesUtil
     *  the faces context utility service to use.
     */
    public void setFacesContextUtility(final FacesContextUtil facesUtil)
    {
        m_FacesUtil = facesUtil;
    }
    
    /**
     * Function which grabs a reference to the channel manager class and 
     * sets the new controller id variable to this system's system id.
     */
    @PostConstruct
    public void channelManagerRequest()
    {
        // default to zero, doesn't really matter at this point as the actual ID will be sent to the GUI and used later.
        m_NewChannelControllerId = 0; 
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewChannelControllerId()
     */
    @Override
    public int getNewChannelControllerId()
    {
        return m_NewChannelControllerId;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#setNewChannelControllerId(int)
     */
    @Override
    public void setNewChannelControllerId(final int controllerId)
    {
        m_NewChannelControllerId = controllerId;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewSocketHost()
     */
    @Override
    public String getNewSocketHost()
    {
        return m_NewSocketHost;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#setNewSocketHost(java.lang.String)
     */
    @Override
    public void setNewSocketHost(final String host)
    {
        m_NewSocketHost = host;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewSocketPort()
     */
    @Override
    public int getNewSocketPort()
    {
        return m_NewSocketPort;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#setNewSocketPort(int)
     */
    @Override
    public void setNewSocketPort(final int port)
    {
        m_NewSocketPort = port;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewTransportName()
     */
    @Override
    public String getNewTransportName()
    {
        return m_NewTransportName;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#setNewTransportName(java.lang.String)
     */
    @Override
    public void setNewTransportName(final String name)
    {
        m_NewTransportName = name;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewTransportRemoteAddress()
     */
    @Override
    public String getNewTransportRemoteAddress()
    {
        return m_NewTransportRemoteAddress;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#setNewTransportRemoteAddress(java.lang.String)
     */
    @Override
    public void setNewTransportRemoteAddress(final String address)
    {
        m_NewTransportRemoteAddress = address;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper#getNewTransportLocalAddress()
     */
    @Override
    public String getNewTransportLocalAddress()
    {
        return m_NewTransportLocalAddress;
    }

    @Override
    public void setNewTransportLocalAddress(final String address)
    {
        m_NewTransportLocalAddress = address;
    }

    @Override
    public RemoteChannelTypeEnum getSelectedNewChannelType()
    {
        return m_SelectedChannelType;
    }

    @Override
    public void setSelectedNewChannelType(final NewChannelChoice channelChoice)
    {
        m_SelectedChannelType = channelChoice.getChannelType();
        
        //this check is here to make sure that because it 
        //is a specific transport that has been chosen that 
        //some of the fields be preset when the set properties 
        //dialog is displayed.
        if (m_SelectedChannelType == RemoteChannelTypeEnum.TRANSPORT)
        {
            m_NewTransportName = channelChoice.getChannelName();
        }
    }

    @Override
    public List<NewChannelChoice> getChannelChoices()
    {
        final List<NewChannelChoice> channels = new ArrayList<NewChannelChoice>();
        
        //Make one choice be a socket, then for all transport layers 
        //show each individual transport layer name
        channels.add(new NewChannelChoice("Socket", RemoteChannelTypeEnum.SOCKET));
        
        for (TransportLayer layer: getAllKnownTransportLayers())
        {
            final NewChannelChoice choice = 
                    new NewChannelChoice(layer.getName(), RemoteChannelTypeEnum.TRANSPORT);           
            choice.setCapabilities((TransportLayerCapabilities)layer.getFactory().getCapabilities());
            channels.add(choice);
        }
        
        return channels;
    }

    @Override
    public void clearNewChannelInput()
    {
        m_NewTransportRemoteAddress = "";
        m_NewTransportLocalAddress = "";
        m_NewTransportName = "";
        m_NewSocketHost = "localhost";
        
        m_NewChannelControllerId = 0;
        
        m_NewSocketPort = ChannelMgr.DEFAULT_PORT;
        
        m_SelectedChannelType = RemoteChannelTypeEnum.SOCKET;
    }

    @Override
    public List<TransportLayer> getAllKnownTransportLayers()
    {
        return m_CommsService.getTransportLayers();
    }
    
    @Override
    public List<String> getAllKnownAddresses()
    {
        return m_AddressService.getAddressDescriptiveStrings();
    }
    
    /**
     * PostValidateEvent function which checks to see if validated input
     * is truly unique. I.E. this function checks to make sure that a socket
     * does not already exist with the inputted information.
     * @param event
     *  the event which has been triggered on the PostValidateEvent
     */
    public void validateSocketDoesNotExist(final ComponentSystemEvent event)
    {
        //this component returned represents the form on the dialog.
        //that is why the following components can be found from the 
        //components UIComponent
        final UIComponent components = event.getComponent();
       
        final UIInput hostName = (UIInput)components.findComponent("hostName");
       
        final UIInput port = (UIInput)components.findComponent("hostPort");
        
        final String host = (String)hostName.getLocalValue();
        final Integer portNum = (Integer)port.getLocalValue();
        
        if (m_RemoteChannelLookup.checkChannelSocketExists(host, portNum))
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "Socket Exists",
                    String.format("A socket with hostname %s and port %d already exists.", host, portNum));
            
            final FacesContext context = m_FacesUtil.getFacesContext();
            
            final FacesMessage messageHost = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "", "Check that your host name is valid.");
            
            final FacesMessage messagePort = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "", "Check that your port is valid.");
            
            context.addMessage(hostName.getClientId(), messageHost);
            context.addMessage(port.getClientId(), messagePort);
            context.validationFailed();
            context.renderResponse();
        }
    }
    
    /**
     * PostValidateEvent function which checks to see if validated input
     * is truly unique. I.E. this function checks to make sure that a socket
     * does not already exist with the inputed information.
     * 
     * @param event
     *  the event which has been triggered on the PostValidateEvent 
     */
    public void validateTransportDoesNotExist(final ComponentSystemEvent event)
    {
        final UIComponent components = event.getComponent();
        
        final UIInput local = (UIInput)components.findComponent("localAddress");
        final UIInput remote = (UIInput)components.findComponent("remoteAddress");
        
        final String localAddress = (String)local.getLocalValue();
        final String remoteAddress = (String)remote.getLocalValue();
        
        if (m_RemoteChannelLookup.checkChannelTransportExists(localAddress, remoteAddress))
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "Transport Exists",
                    String.format("A transport with local address %s and remote address %s " 
                            + "already exists.", localAddress, remoteAddress));
            
            final FacesContext context = m_FacesUtil.getFacesContext();
            
            final FacesMessage messageLocal = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "", "Check that your local transport address is valid.");
            
            final FacesMessage messageRemote = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "", "Check that your remote transport address is valid.");
            
            context.addMessage(local.getClientId(), messageLocal);
            context.addMessage(remote.getClientId(), messageRemote);
            context.validationFailed();
            context.renderResponse();
        }
    }
    
}
