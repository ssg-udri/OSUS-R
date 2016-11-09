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

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

/**
 * Implementation of the {@link CommsStackRequest} interface.
 * @author Dave Humeniuk
 *
 */
@ManagedBean(name = "commsStackRequest")
@ViewScoped
public class CommsStackRequestImpl implements CommsStackRequest
{    
    /**
     * Comms manager service to use.
     */
    @ManagedProperty(value = "#{commsMgr}")
    private CommsMgr commsMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * The image display interface service to use.
     */
    @Inject
    private CommsImage m_CommsImageInterface;
    
    /**
     * Set the comms manager service.
     * @param commsManager
     *      comms manager service to set.
     */
    public void setCommsMgr(final CommsMgr commsManager)
    {
        commsMgr = commsManager;
    } 
    
    
    /**
     * Set the image display interface to use.
     * @param imgInterface
     *  the image display interface to use.
     */
    public void setCommsImageInterface(final CommsImage imgInterface)
    {
        m_CommsImageInterface = imgInterface;
    }
    
    @Override
    public List<CommsStackModel> getCommsStacksAsync(final int systemId)
    {        
        final ArrayList<CommsStackModel> stacks = new ArrayList<CommsStackModel>();
        
        // get a list of layers, will remove from this list as they are added to stacks
        final List<CommsLayerLinkModelImpl> linkLayers = 
                new ArrayList<CommsLayerLinkModelImpl>(commsMgr.getLinksAsync(systemId));
        final List<CommsLayerBaseModel> physicalLinks = 
                new ArrayList<CommsLayerBaseModel>(commsMgr.getPhysicalsAsync(systemId));
        
        // build up all transport based stacks
        for (CommsLayerBaseModel transport : commsMgr.getTransportsAsync(systemId))
        {
            final CommsStackModelImpl stack = new CommsStackModelImpl(m_CommsImageInterface);
            stack.setTransport(transport);
            
            setTransportChildren(linkLayers, physicalLinks, transport, stack);
            
            stacks.add(stack);
        }        
        // build up stacks that have a top layer of a link layer
        for (CommsLayerLinkModelImpl linkLayer : linkLayers)
        {
            final CommsStackModelImpl stack = new CommsStackModelImpl(m_CommsImageInterface);
            stack.setLink(linkLayer);
            
            setLinkChild(physicalLinks, linkLayer, stack);
            
            stacks.add(stack);
        }        
        // build all stacks that are only physical links, list will now only contain those
        for (CommsLayerBaseModel physicalLink : physicalLinks)
        {
            final CommsStackModelImpl stack = new CommsStackModelImpl(m_CommsImageInterface);
            stack.setPhysical(physicalLink);
            stack.setStackComplete();
            stacks.add(stack);
        }
        //at this point, physicalLinks should only contain physical links not used by other layers
        commsMgr.setUnusedPhysicalLinks(systemId, physicalLinks);         
        return stacks;
    }
    
    @Override
    public List<CommsStackModel> getSelectedCommsStacksAsync(final int systemId, final CommsStackModel selectedStack)
    {
        if (selectedStack == null) // nothing selected, get them all
        {
            return getCommsStacksAsync(systemId);
        }
        else
        {
            for (CommsStackModel stack : getCommsStacksAsync(systemId))
            {
                if (stack.getCommsTopLayerName().equals(selectedStack.getCommsTopLayerName()))
                {
                    final List<CommsStackModel> stacks = new ArrayList<CommsStackModel>();
                    stacks.add(stack);
                    return stacks;
                }
            }            
            // not found, just return all, selected item might no longer be valid
            return getCommsStacksAsync(systemId);
        }
    }

    /** Using the config properties of the given transport layer, method 
     * finds and sets the link layer for the stack containing the given transport layer, 
     * and then queries the link layer for its corresponding physical link.  
     * @param linkLayers
     *      List of all known unused link layers.
     * @param physicalLinks
     *      List of all known unused physical links.
     * @param transport
     *      The transport layer added to the stack that is currently under construction.
     * @param stack
     *      The stack currently under construction.
     */
    private void setTransportChildren(final List<CommsLayerLinkModelImpl> linkLayers,
            final List<CommsLayerBaseModel> physicalLinks, final CommsLayerBaseModel transport,
            final CommsStackModelImpl stack)
    {
        // try to find link layer, if prop not set, assume no link layer
        final UnmodifiablePropertyModel linkLayerNameProp = 
                transport.getPropertyAsync(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME);
        if (linkLayerNameProp != null)
        {
            final String linkLayerName = (String)linkLayerNameProp.getValue();
            
            // we have a child, find it in the list of unused link layers
            for (CommsLayerLinkModel linkLayer : linkLayers)
            {
                if (linkLayer.getName().equals(linkLayerName))
                {
                    stack.setLink(linkLayer);
                    linkLayers.remove(linkLayer); // remove from list of unused links
                    
                    setLinkChild(physicalLinks, linkLayer, stack);
                    break;
                }
            }            
        }
    }

    /** Using the config properties of the given link layer, method 
     * finds and sets the physical link for the stack containing the given link layer.
     * @param physicalLinks
     *      List of all known unused physical links.
     * @param linkLayer
     *      The link layer added to the stack that is currently under construction.
     * @param stack
     *      The stack that is currently under construction.
     */
    private void setLinkChild(final List<CommsLayerBaseModel> physicalLinks, final CommsLayerLinkModel linkLayer,
            final CommsStackModelImpl stack)
    {
        // try to find physical link, if prop not set, assume no physical link
        final UnmodifiablePropertyModel physicalLinkNameProp = linkLayer.
                getPropertyAsync(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME);
        if (physicalLinkNameProp != null)
        {
            final String physicalLinkName = (String)physicalLinkNameProp.getValue();
            
            // we have a child, find it in the list of unused link layers
            for (FactoryBaseModel physicalLink : physicalLinks)
            {
                if (physicalLink.getName().equals(physicalLinkName))
                {
                    stack.setPhysical(physicalLink);
                    stack.setStackComplete();
                    physicalLinks.remove(physicalLink); // remove from list of unused physical links
                    break;
                }
            }           
        }
    }

    @Override
    public List<FactoryBaseModel> getTopMostComms(final int systemId)
    {
        final List<FactoryBaseModel> topMostComms = new ArrayList<FactoryBaseModel>();        
        final List<CommsStackModel> stacks = getCommsStacksAsync(systemId);
        
        for (CommsStackModel stack : stacks)
        {
            final String topName = stack.getCommsTopLayerName();
            
            if (stack.getTransport() != null
                    && stack.getTransport().getName().equals(topName))
            {
                topMostComms.add(stack.getTransport());
            }
            else if (stack.getLink() != null 
                    && stack.getLink().getName().equals(topName))
            {
                topMostComms.add(stack.getLink());
            }
            else if (stack.getPhysical() != null 
                    && stack.getPhysical().getName().equals(topName))
            {
                topMostComms.add(stack.getPhysical());
            }
        }        
        return topMostComms;
    }

    @Override
    public CommsStackModel getCommsStackForBaseModel(final int systemId, final FactoryBaseModel model)
    {
        final List<CommsStackModel> stacks = getCommsStacksAsync(systemId);
        
        for (CommsStackModel stack : stacks)
        {
            if (stack.getTransport() != null 
                    && stack.getTransport().getName().equals(model.getName()))
            {
                return stack;
            }
            else if (stack.getLink() != null 
                    && stack.getLink().getName().equals(model.getName()))
            {
                return stack;
            }
            else if (stack.getPhysical() != null
                    && stack.getPhysical().getName().equals(model.getName()))
            {
                return stack;
            }
        }        
        return null;
    }       
}
