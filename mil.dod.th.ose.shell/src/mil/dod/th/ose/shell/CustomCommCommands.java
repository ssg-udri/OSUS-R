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
package mil.dod.th.ose.shell;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Custom Comm Commands.
 * 
 * @author cweisenborn
 */
@Component(provide = CustomCommCommands.class, properties = {"osgi.command.scope=thcomm", 
        "osgi.command.function=createPhysicalLink|deletePhysicalLink|createLinkLayer|createTransportLayer|"
        + "getPhysicalLinkNames|getLinkLayer|getLinkLayers|getTransportLayer|getTransportLayers|printDeep" })
public class CustomCommCommands
{
    /**
     * Reference to custom comms service.
     */
    private CustomCommsService m_CustomCommsService;
    
    /**
     * Sets the custom comms service to be used.
     * 
     * @param customCommsService the m_CustomCommsService to set
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CustomCommsService = customCommsService;
    }
    
    /**
     * Returns a new physical link with specified type and name.
     * 
     * @param plType
     *          name ({@link PhysicalLinkTypeEnum#fromValue(String)} value) of the type of 
     *          {@link PhysicalLink} to be created
     * @param name
     *          name of the physical link
     * @return the created {@link PhysicalLink}
     * @throws CCommException
     *          if the desired physical link cannot be created
     * @throws PersistenceFailedException
     *          if the physical link properties cannot be persisted
     * @throws IOException
     *          if the data about the object cannot be pulled from the data store
     */
    @Descriptor("Creates a physical link with the specified type and name.")
    public PhysicalLink createPhysicalLink(
            @Descriptor("String representation of the type of physical link to be created.")
            final String plType, 
            @Descriptor("Name of the physical link.")
            final String name) throws CCommException, PersistenceFailedException, IOException
    {
        return m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.fromValue(plType), name);
    }
    
    /**
     * Removes the desired physical link.
     * 
     * @param phyLink
     *          name of the physical link to be removed
     * @throws IllegalArgumentException
     *          if the physical link is null
     * @throws IllegalStateException
     *          if the physical link is in use or open
     * @throws CCommException
     *          if an error occurs removing the physical link
     */
    @Descriptor("Delete specified physical link.")
    public void deletePhysicalLink(
            @Descriptor("Name of the physical link to be deleted.")
            final String phyLink) throws IllegalArgumentException, IllegalStateException, CCommException
    {
        m_CustomCommsService.deletePhysicalLink(phyLink);
    }
    
    /**
     * Returns the link layer with the given link layer product type (
     * {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}) and {@link PhysicalLink} name. If such a
     * link layer already exists it is returned, otherwise one is created.
     * 
     * @param linkLayerProductType
     *            Product type of the link layer in fully qualified class name format.
     * @param phyLinkName
     *            Name of the {@link PhysicalLink} to use
     * @return the created {@link LinkLayer} or an existing one
     * @throws CCommException
     *            if the desired link layer cannot be created
     */
    public LinkLayer createLinkLayer(
            @Descriptor("Unique product type string of the link layer in fully qualified class name format.")
            final String linkLayerProductType, 
            @Descriptor("Physical link name to use, if null no physical link will be used.")
            final String phyLinkName) throws CCommException
    {
        return m_CustomCommsService.createLinkLayer(linkLayerProductType, phyLinkName);
    }
    
    /**
     * Returns a transport layer with the given transport layer product type (
     * {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}) and {@link LinkLayer} name. If such a
     * transport layer already exists it is returned, otherwise one is created.
     * 
     * @param transportLayerProductType
     *            Product type of the transport layer in fully qualified class name format.
     * @param name
     *            Name of the transport layer to create
     * @param linkLayerName
     *            Name of the {@link LinkLayer} that should be used or null if no link layer is to be used
     * @return the created {@link TransportLayer} or an existing one
     * @throws CCommException
     *             if the desired transport layer cannot be created
     */
    @Descriptor("Creates a transport layer with specified type, name, timeout, and link layer.")
    public TransportLayer createTransportLayer(
            @Descriptor("Unique product type string of the transport layer in fully qualified class name format.")
            final String transportLayerProductType, 
            @Descriptor("Name of the transport layer.")
            final String name, 
            @Descriptor("Name of link layer to use, if null no link layer will be used.")
            final String linkLayerName) throws CCommException
    {
        return m_CustomCommsService.createTransportLayer(transportLayerProductType, name, linkLayerName);
    }
    
    /**
     * Returns a list of all physical links.
     * 
     * @return
     *      a list of all physical links
     */
    @Descriptor("Returns a list of all physical link names.")
    public List<String> getPhysicalLinkNames()
    {
        return m_CustomCommsService.getPhysicalLinkNames();
    }
    
    /**
     * Returns the specified link layer if it exists.
     * 
     * @param name
     *          name of the {@link LinkLayer} to be returned
     * @return
     *          the {@link LinkLayer} that corresponds with the specified name
     */
    @Descriptor("Returns a link layer with the specified name if it exists.")
    public LinkLayer getLinkLayer(
            @Descriptor("Name of the desired link layer.")
            final String name)
    {
        return m_CustomCommsService.getLinkLayer(name);
    }
    
    /**
     * Returns a list of all link layers.
     * 
     * @return list of link layers
     */
    @Descriptor("Returns a list of all link layers.")
    public List<LinkLayer> getLinkLayers()
    {
        return m_CustomCommsService.getLinkLayers();
    }
    
    /**
     * Returns a transport layer with the specified name.
     * 
     * @param name
     *          name of the transport layer to be returned
     * @return
     *          the transport layer that corresponds with the name or null if not found
     */
    @Descriptor("Returns a transport layer with the specified name if it exists.")
    public TransportLayer getTransportLayer(
            @Descriptor("Name of the desired transport layer.")
            final String name)
    {
        return m_CustomCommsService.getTransportLayer(name);
    }
    
    /**
     * Return the list of TransportLayers.
     * 
     * @return 
     *  The list of all the TransportLayers held by the 
     *  CustomCommsService.
     */
    @Descriptor("Returns a list of all transport layers.")
    public List<TransportLayer> getTransportLayers()
    {
        return m_CustomCommsService.getTransportLayers();
    }
    
    /**
     * Print status of service to console.
     * 
     * @param session
     *      command session containing the console 
     */
    @Descriptor("Display status of the service console.")
    public void printDeep(final CommandSession session)
    {
        m_CustomCommsService.printDeep(session.getConsole());
    }
}
