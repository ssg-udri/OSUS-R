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
package mil.dod.th.ose.gui.webapp.comms;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.LinkLayerMessages;
import mil.dod.th.core.remote.proto.LinkLayerMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link SelectedCommsHelper} interface.
 * @author bachmakm
 *
 */
@ManagedBean(name = "selectedCommsHelper")
@SessionScoped
public class SelectedCommsHelperImpl implements SelectedCommsHelper
{
    /**
     * Represents the user-selected comms stack to be displayed in the web app.
     */
    private CommsStackModel m_SelectedCommsStack;
    
    /**
     * Contains the name(s) of the layer(s) to be removed from a stack.  Used as a way to confirm
     */
    private CommsStackModel m_RemoveStack;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    @Override
    public void setSelectedComms(final CommsStackModel comms)
    {
        m_SelectedCommsStack = comms;
    }    

    @Override
    public CommsStackModel getSelectedComms()
    {
        return m_SelectedCommsStack;
    }

    @Override
    public void unSetSelectedComms()
    {
        m_SelectedCommsStack = null; //NOPMD - assigning object to null - assigning it to empty object is misleading
    }
    
    @Override
    public void sendLinkActivationRequest(final CommsLayerLinkModel link, final int systemId)
    {
        if (link.isActivated())
        {
            final DeactivateRequestData deactivateRequest = DeactivateRequestData.newBuilder().
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(link.getUuid())).build();
            m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.DeactivateRequest, 
                    deactivateRequest).queue(systemId, null);
        }
        else
        {
            final ActivateRequestData activateRequest = ActivateRequestData.newBuilder().
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(link.getUuid())).build();
            m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.ActivateRequest, 
                    activateRequest).queue(systemId, null);
        }        
    }

    @Override
    public void sendLinkPerformBitRequest(final CommsLayerLinkModel link, final int systemId)
    {
        final PerformBITRequestData bitRequest = PerformBITRequestData.newBuilder().
                setLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(link.getUuid())).build();
        m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.PerformBITRequest, 
                bitRequest).queue(systemId, null);
        
    }

    @Override
    public void sendRemoveStackRequest(final int systemId)
    {
        final FactoryBaseModel transport = m_RemoveStack == null ? null : m_RemoveStack.getTransport();
        final CommsLayerLinkModel link = m_RemoveStack == null ? null : m_RemoveStack.getLink();
        
        if (transport != null)
        {
            final TransportLayerMessages.DeleteRequestData removeTransport = TransportLayerMessages.DeleteRequestData
                .newBuilder().setTransportLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(transport.getUuid()))
                .build();
            
            m_MessageFactory.createTransportLayerMessage(TransportLayerMessageType.DeleteRequest, 
                    removeTransport).queue(systemId, null);
        }        
        if (link != null)
        {
            final LinkLayerMessages.DeleteRequestData removeLink = LinkLayerMessages.DeleteRequestData.newBuilder().
                    setLinkLayerUuid(SharedMessageUtils.convertUUIDToProtoUUID(link.getUuid())).build();  
            m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.DeleteRequest, 
                    removeLink).queue(systemId, null);
        }      
    }
    
    @Override
    public String getRemoveStackLayerNames()
    {
        final FactoryBaseModel transport = m_RemoveStack == null ? null : m_RemoveStack.getTransport();
        final CommsLayerLinkModel link = m_RemoveStack == null ? null : m_RemoveStack.getLink();

        String removeLayers = "";

        if (transport != null)
        {
            removeLayers = transport.getName();
        }

        if (link != null)
        {
            removeLayers = removeLayers.isEmpty() ? link.getName() : new StringBuffer().append(removeLayers).
                    append(" and ").append(link.getName()).toString();
        }        
        return removeLayers;
    }

    @Override
    public void setRemoveStack(final CommsStackModel stack)
    {
        m_RemoveStack = stack;
    }
}
