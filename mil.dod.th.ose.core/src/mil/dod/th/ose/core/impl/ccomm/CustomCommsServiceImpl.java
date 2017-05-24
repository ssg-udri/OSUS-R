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
package mil.dod.th.ose.core.impl.ccomm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
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

import com.google.common.base.Strings;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;
import mil.dod.th.ose.utils.SingleComponent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * TH Implementation of {@link CustomCommsService}.
 * 
 * @see CustomCommsService
 * 
 * @author allenchl
 */
@Component
public class CustomCommsServiceImpl extends DirectoryService implements CustomCommsService
{
    /**
     * {@link mil.dod.th.ose.core.factory.api.FactoryServiceContext} for {@link LinkLayer}s.
     */
    private FactoryServiceContext<LinkLayerInternal> m_LinkLayerFactContext;

    /**
     * {@link mil.dod.th.ose.core.factory.api.FactoryServiceContext} for {@link TransportLayer}s.
     */
    private FactoryServiceContext<TransportLayerInternal> m_TransLayerFactContext;

    /**
     * {@link mil.dod.th.ose.core.factory.api.FactoryServiceContext} for {@link PhysicalLink}s.
     */
    private FactoryServiceContext<PhysicalLinkInternal> m_PhysLinkFactContext;
    
    /**
     * Reference to the component that is created for a {@link FactoryServiceContext} instance.
     */
    private SingleComponent<FactoryServiceContext<PhysicalLinkInternal>> m_PhysicalLinkContextComponent; 
    
    /**
     * Reference to the component that is created for a {@link FactoryServiceContext} instance.
     */
    private SingleComponent<FactoryServiceContext<LinkLayerInternal>> m_LinkLayerContextComponent; 
    
    /**
     * Reference to the component that is created for a {@link FactoryServiceContext} instance.
     */
    private SingleComponent<FactoryServiceContext<TransportLayerInternal>> m_TransportContextComponent; 
    
    /**
     * Lock object.
     */
    private final Object m_CreationLock = new Object();

    /**
     * Physical link service proxy reference.
     */
    private FactoryServiceProxy<PhysicalLinkInternal> m_PhysicalLinkServiceProxy;
    
    /**
     * Link layer service proxy reference.
     */
    private FactoryServiceProxy<LinkLayerInternal> m_LinkLayerServiceProxy;
    
    /**
     * Transport layer service proxy reference.
     */
    private FactoryServiceProxy<TransportLayerInternal> m_TransportLayerServiceProxy;
    
    /**
     * Wake lock used for custom comms service operations.
     */
    private WakeLock m_WakeLock;

    @Override
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        super.setLoggingService(logging);
    }

    @Override
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }
    
    @Override
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Bind the physical link factory service proxy.
     * @param factoryServiceProxy
     *      the physical link service proxy to use
     */
    @Reference(target = "(" + PhysicalLinkInternal.SERVICE_TYPE_PAIR + ")")
    public void setPhysicalLinkFactoryServiceProxy(final FactoryServiceProxy<PhysicalLinkInternal> factoryServiceProxy)
    {
        m_PhysicalLinkServiceProxy = factoryServiceProxy;
    }
    
    /**
     * Bind the link layer factory service proxy.
     * @param factoryServiceProxy
     *      the link layer service proxy to use
     */
    @Reference(target = "(" + LinkLayerInternal.SERVICE_TYPE_PAIR + ")")
    public void setLinkLayerFactoryServiceProxy(final FactoryServiceProxy<LinkLayerInternal> factoryServiceProxy)
    {
        m_LinkLayerServiceProxy = factoryServiceProxy;
    }
    
    /**
     * Bind the transport layer factory service proxy.
     * @param factoryServiceProxy
     *      the transport layer service proxy to use
     */
    @Reference(target = "(" + TransportLayerInternal.SERVICE_TYPE_PAIR + ")")
    public void setTransportLayerFactoryServiceProxy(
            final FactoryServiceProxy<TransportLayerInternal> factoryServiceProxy)
    {
        m_TransportLayerServiceProxy = factoryServiceProxy;
    }
    
    /**
     * Bind the factory for creating {@link FactoryServiceContext} instances.
     * 
     * @param serviceContextFactory
     *      factory that creates {@link FactoryServiceContext} instances
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryServiceContext.FACTORY_NAME + ")")
    public void setFactoryServiceContextFactory(final ComponentFactory serviceContextFactory)
    {
        m_PhysicalLinkContextComponent = 
                new SingleComponent<FactoryServiceContext<PhysicalLinkInternal>>(serviceContextFactory);
        m_LinkLayerContextComponent = 
                new SingleComponent<FactoryServiceContext<LinkLayerInternal>>(serviceContextFactory);
        m_TransportContextComponent = 
                new SingleComponent<FactoryServiceContext<TransportLayerInternal>>(serviceContextFactory);
    }
    
    /**
     * The service component activation method.
     * 
     * @param context
     *      bundle context for the bundle containing this component
     * @throws InvalidSyntaxException
     *      if any of the {@link FactoryServiceContext} provide the wrong filter
     */
    @Activate
    public void activate(final BundleContext context) throws InvalidSyntaxException
    {
        m_PhysLinkFactContext = m_PhysicalLinkContextComponent.newInstance(null);
        m_PhysLinkFactContext.initialize(context, m_PhysicalLinkServiceProxy, this);
        
        m_LinkLayerFactContext = m_LinkLayerContextComponent.newInstance(null);
        m_LinkLayerFactContext.initialize(context, m_LinkLayerServiceProxy, this);
        
        m_TransLayerFactContext = m_TransportContextComponent.newInstance(null);
        m_TransLayerFactContext.initialize(context, m_TransportLayerServiceProxy, this);

        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreCustomCommsService");
    }

    /**
     * Deactivate the service, shutdown all transport layers, deactivate all link layers and disconnect all
     * physical links.
     */
    @Deactivate
    public void deactivate()
    {
        m_PhysicalLinkContextComponent.tryDispose();
        m_LinkLayerContextComponent.tryDispose();
        m_TransportContextComponent.tryDispose();
        m_WakeLock.delete();
    }
    
    @Override
    public void deletePhysicalLink(final String phyLinkName)
    {
        final PhysicalLink phyLink = m_PhysLinkFactContext.getRegistry().findObjectByName(phyLinkName);
        phyLink.delete();
    }

    @Override
    public LinkLayer createLinkLayer(final String llType, final String physicalLinkName) 
            throws CCommException
    {
        return createLinkLayer(llType, null, physicalLinkName);
    }

    @Override
    public LinkLayer createLinkLayer(final String llType, final String name, final String physicalLinkName) 
            throws CCommException
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        if (physicalLinkName != null)
        {
            properties.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, physicalLinkName);
        }

        return createLinkLayer(llType, name, properties);
    }

    @Override
    public LinkLayer createLinkLayer(final String llType, final String name, final Map<String, Object> properties) 
            throws CCommException
    {
        boolean hasPhysicalLinkName = false;
        if (properties.containsKey(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME))
        {
            final String physicalLinkName = (String)properties.get(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME);
            if (Strings.isNullOrEmpty(physicalLinkName))
            {
                throw new IllegalArgumentException("Physical Link name cannot be null.");
            }
            hasPhysicalLinkName = true;
        }

        // existing link not found or no physical link set to match on
        final FactoryInternal factory = getLinkLayerFactoryByProductType(llType);
        
        if (factory.getLinkLayerCapabilities().isPhysicalLinkRequired() && !hasPhysicalLinkName) 
        {
            throw new IllegalArgumentException(
                    String.format("Link layer [%s] requires a physical link, but one has not be provided", llType));
        }
        
        m_Logging.log(LogService.LOG_INFO, "Creating Link Layer of type [%s]", llType);

        synchronized (m_CreationLock)
        {
            try
            {
                m_WakeLock.activate();

                // Create the configuration
                return m_LinkLayerFactContext.getRegistry().createNewObject(factory, name, properties);
            }
            catch (final Exception ex)
            {
                throw new CCommException("Error creating new Link Layer.", ex, FormatProblem.OTHER);
            }
            finally
            {
                m_WakeLock.cancel();
            }
        }
    }

    @Override
    public TransportLayer createTransportLayer(final String tlType, final String name, final String linkLayerName) 
            throws CCommException
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        if (linkLayerName != null)
        {
            properties.put(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME, linkLayerName);
        }

        return createTransportLayer(tlType, name, properties);
    }

    @Override
    public TransportLayer createTransportLayer(final String tlType, final String name, 
            final Map<String, Object> properties) throws IllegalArgumentException, CCommException
    {
        final String linkLayerName = (String)properties.get(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME);

        // search to see if transport layer is already in list
        for (TransportLayer tLayer : m_TransLayerFactContext.getRegistry().getObjects())
        {
            if (tLayer.getFactory().getProductType().equals(tlType) 
                    && tLayer.getLinkLayer() != null
                    && tLayer.getLinkLayer().getName().equals(linkLayerName))
            {
                m_Logging.log(LogService.LOG_INFO,
                        "Retrieving pre-existing Transport Layer [%s] and updating properties.", tLayer.getName());
                try
                {
                    tLayer.setProperties(properties);
                }
                catch (final FactoryException ex)
                {
                    throw new CCommException(String.format("Error setting properties for Transport Layer [%s]",
                            tLayer.getName()), ex, FormatProblem.OTHER);
                }
                return tLayer;
            }
        }

        // otherwise create a new one
        final FactoryInternal factory = getTransportLayerFactoryByProductType(tlType);
        
        m_Logging.log(LogService.LOG_INFO, "Creating Transport Layer of type [%s].", tlType);

        final TransportLayer tLayer;
        synchronized (m_CreationLock)
        {
            try
            {
                m_WakeLock.activate();

                // Create the configuration
                tLayer = m_TransLayerFactContext.getRegistry().createNewObject(factory, name, properties);
            }
            catch (final Exception ex)
            {
                throw new CCommException("Error creating new Transport Layer.", ex, FormatProblem.OTHER);
            }
            finally
            {
                m_WakeLock.cancel();
            }
        }

        return tLayer;
    }

    @Override
    public List<String> getPhysicalLinkNames()
    {
        return m_PhysLinkFactContext.getRegistry().getObjectNames();
    }

    @Override
    public List<PhysicalLink> getPhysicalLinks()
    {
        return Collections.unmodifiableList(new ArrayList<PhysicalLink>(
                m_PhysLinkFactContext.getRegistry().getObjects()));
    }

    @Override
    public LinkLayer getLinkLayer(final String name)
    {
        return m_LinkLayerFactContext.getRegistry().getObjectByName(name);
    }
    
    @Override
    public LinkLayer findLinkLayer(final String name)
    {
        return m_LinkLayerFactContext.getRegistry().findObjectByName(name);
    }

    @Override
    public List<LinkLayer> getLinkLayers()
    {
        return Collections.unmodifiableList(new ArrayList<LinkLayer>(
                m_LinkLayerFactContext.getRegistry().getObjects()));
    }

    @Override
    public TransportLayer getTransportLayer(final String name)
    {
        return m_TransLayerFactContext.getRegistry().getObjectByName(name);
    }
    
    @Override
    public TransportLayer findTransportLayer(final String name)
    {
        return m_TransLayerFactContext.getRegistry().findObjectByName(name);
    }

    @Override
    public List<TransportLayer> getTransportLayers()
    {
        return Collections.unmodifiableList(
                new ArrayList<TransportLayer>(m_TransLayerFactContext.getRegistry().getObjects()));
    }

    @Override
    public void printDeep(final PrintStream printstream)
    {
        final String EMPTY_LAYER = "<None>";

        printstream.println("****************Printing Deep:");

        // output tLayer, lLayer, cLayer
        printstream.println("");
        printstream.println("------Transport Layer------");
        for (TransportLayer layer : m_TransLayerFactContext.getRegistry().getObjects())
        {
            final String phyLinkName;
            final String linkName;
            final LinkLayer linkLayer = layer.getLinkLayer();
            if (linkLayer == null)
            {
                phyLinkName = EMPTY_LAYER;
                linkName = EMPTY_LAYER;
            }
            else
            {
                final PhysicalLink phyLayer = linkLayer.getPhysicalLink();
                phyLinkName = phyLayer == null ? EMPTY_LAYER : phyLayer.getName();
                linkName = linkLayer.getName();
            }

            printstream.format("%s: Link Layer: %s, Physical Link: %s%n", layer.getName(), linkName, phyLinkName);
        }

        printstream.println("");
        printstream.println("------Link Layer-----------");
        for (LinkLayer layer : m_LinkLayerFactContext.getRegistry().getObjects())
        {
            final String phyLinkName;
            final PhysicalLink phyLayer = layer.getPhysicalLink();
            phyLinkName = phyLayer == null ? EMPTY_LAYER : phyLayer.getName();

            printstream.format("%s: Physical Link: %s%n", layer.getName(), phyLinkName);
        }
        
        printstream.println("");
        printstream.println("------PhysicalLink Layer-------");
        for (PhysicalLink link : m_PhysLinkFactContext.getRegistry().getObjects())
        {
            printstream.format("%s: Open: %b, InUse: %b, Properties:%n", link.getName(), link.isOpen(), link.isInUse());
            for (String key : link.getProperties().keySet())
            {
                printstream.format("    %s = %s%n", key, link.getProperties().get(key));
            }
        }

        printstream.println("");
        printstream.println("****************End of print deep");
    }

    @Override
    public void releasePhysicalLink(final String name)
    {
        final PhysicalLinkInternal physLink = m_PhysLinkFactContext.getRegistry().getObjectByName(name);
        physLink.release();
    }

    @Override
    public PhysicalLink requestPhysicalLink(final String name)
    {
        return requestPhysicalLink(m_PhysLinkFactContext.getRegistry().getObjectByName(name));
    }

    @Override
    public boolean isPhysicalLinkInUse(final String name) throws IllegalArgumentException
    {
        final PhysicalLinkInternal physLink = m_PhysLinkFactContext.getRegistry().getObjectByName(name);
        return physLink.isInUse();
    }
    
    @Override
    public boolean isPhysicalLinkOpen(final String name) throws IllegalArgumentException
    {
        return m_PhysLinkFactContext.getRegistry().getObjectByName(name).isOpen();
    }
    
    @Override
    public boolean isLinkLayerCreated(final String name) 
    {
        return m_LinkLayerFactContext.getRegistry().isObjectCreated(name);
    }
    
    @Override
    public boolean isTransportLayerCreated(final String name)
    { 
        return m_TransLayerFactContext.getRegistry().isObjectCreated(name);
    }
    
    @Override
    public PhysicalLink requestPhysicalLink(final UUID uuid)
    {
        return requestPhysicalLink(m_PhysLinkFactContext.getRegistry().getObjectByUuid(uuid));
    }
    
    @Override
    public List<UUID> getPhysicalLinkUuids()
    {
        return m_PhysLinkFactContext.getRegistry().getUuids();
    }
    
    @Override
    public String getPhysicalLinkName(final UUID uuid)
    {
        return m_PhysLinkFactContext.getRegistry().getObjectByUuid(uuid).getName();
    }
    
    @Override
    public PhysicalLinkFactory getPhysicalLinkFactory(final UUID uuid)
    {
        return m_PhysLinkFactContext.getRegistry().getObjectByUuid(uuid).getFactory();
    }
    
    @Override
    public String getPhysicalLinkPid(final UUID uuid)
    {
        return m_PhysLinkFactContext.getRegistry().getObjectByUuid(uuid).getPid();
    }
    
    @Override
    public void setPhysicalLinkName(final UUID uuid, final String name) throws IllegalArgumentException, CCommException
    {
        try
        {
            m_PhysLinkFactContext.getRegistry().setName(uuid, name);
        }
        catch (final FactoryObjectInformationException e)
        {
            throw new CCommException(e, FormatProblem.OTHER);
        }
    }
    
    @Override
    public PhysicalLink createPhysicalLink(final PhysicalLinkTypeEnum plType) 
            throws CCommException, IllegalArgumentException
    {
        return createPhysicalLink(plType, null);
    }

    @Override
    public PhysicalLink createPhysicalLink(final PhysicalLinkTypeEnum plType, final String name) 
            throws CCommException, IllegalArgumentException
    {
        return createPhysicalLink(plType, name, new Hashtable<String, Object>());
    }
    
    @Override
    public PhysicalLink createPhysicalLink(final PhysicalLinkTypeEnum plType, final String name, 
        final Map<String, Object> props) throws CCommException, IllegalArgumentException
    {
        final FactoryInternal factory = getPhysicalLinkFactoryByPhysicalLinkType(plType); 

        m_Logging.log(LogService.LOG_INFO, "Creating Physical Link of type [%s].", plType);
        
        final PhysicalLink pLink;
        synchronized (m_CreationLock)
        {
            try
            {
                m_WakeLock.activate();

                // Create the configuration
                pLink = m_PhysLinkFactContext.getRegistry().createNewObject(factory, name, props);
            }
            catch (final Exception ex)
            {
                throw new CCommException("Error creating new Physical Link.", ex, FormatProblem.OTHER);
            }
            finally
            {
                m_WakeLock.cancel();
            }
        }
        return pLink;
    }

    @Override
    public UUID tryCreatePhysicalLink(final PhysicalLinkTypeEnum plType, final String name) throws CCommException,
            IllegalArgumentException
    {
        return tryCreatePhysicalLink(plType, name, new Hashtable<String, Object>());
    }

    @Override
    public UUID tryCreatePhysicalLink(final PhysicalLinkTypeEnum plType, final String name, 
            final Map<String, Object> properties) throws CCommException, IllegalArgumentException
    {
        synchronized (m_CreationLock)
        {
            final PhysicalLink phys;
            
            //check if physical link already exists
            if (m_PhysLinkFactContext.getRegistry().isObjectCreated(name))
            {
                //get the the object from the registry
                phys = m_PhysLinkFactContext.getRegistry().getObjectByName(name);
                m_Logging.log(LogService.LOG_INFO,
                    "Retrieving pre-existing Physical Link [%s] and updating properties.", phys.getName());
                try
                {
                    phys.setProperties(properties);
                }
                catch (final FactoryException ex)
                {
                    throw new CCommException(String.format("Error setting properties for Physical Link [%s]",
                            phys.getName()), ex, FormatProblem.OTHER);
                }
            }
            else
            {
               // otherwise create one
                m_Logging.log(LogService.LOG_INFO, "Creating Physical Link of type [%s]", plType);
                phys = createPhysicalLink(plType, name, properties);
            }

            return phys.getUuid();
        }
    }

    @Override
    public Set<LinkLayerFactory> getLinkLayerFactories()
    {
        final Set<FactoryInternal> set = new HashSet<>(m_LinkLayerFactContext.getFactories().values());
        @SuppressWarnings("unchecked")
        final Set<LinkLayerFactory> toReturn = Collections.unmodifiableSet((Set<LinkLayerFactory>)(Set<?>)set);
        return toReturn;
    }

    @Override
    public Set<PhysicalLinkFactory> getPhysicalLinkFactories()
    {
        final Set<FactoryInternal> set = new HashSet<>(m_PhysLinkFactContext.getFactories().values());
        @SuppressWarnings("unchecked")
        final Set<PhysicalLinkFactory> toReturn = Collections.unmodifiableSet((Set<PhysicalLinkFactory>)(Set<?>)set);
        return toReturn;
    }

    @Override
    public Set<TransportLayerFactory> getTransportLayerFactories()
    {
        final Set<FactoryInternal> set = new HashSet<>(m_TransLayerFactContext.getFactories().values());
        @SuppressWarnings("unchecked")
        final Set<TransportLayerFactory> toReturn = 
            Collections.unmodifiableSet((Set<TransportLayerFactory>)(Set<?>)set);
        return toReturn;
    }
    
    FactoryRegistry<PhysicalLinkInternal> getPhysicalLinkRegistry()
    {
        return m_PhysLinkFactContext.getRegistry();
    }
    
    /**
     * Get the in use flag value from the give physical link.
     * @param physicalLink
     *      the physical link to request
     * @return
     *      the requested physical link object
     */
    private PhysicalLinkInternal requestPhysicalLink(final PhysicalLinkInternal physicalLink)
    {
        if (physicalLink.isInUse())
        {
            throw new IllegalStateException("Unable to request Physical Link [" + physicalLink.getName() 
                + "] it is already in use");
        }
        else
        {
            physicalLink.setInUse(true);
            return physicalLink;
        }
    }
    
    /**
     * Get a comms layer factory given the product type.
     * 
     * @param context
     *      the factory service context to use
     * @param commsType
     *      type of comms layer the factory must create
     * @return
     *      factory that creates the given product type
     * @throws CCommException
     *      if the factory is not registered
     */
    private FactoryInternal getCommsLayerFactoryByProductType(final FactoryServiceContext<?> context, 
            final String commsType) throws CCommException
    {
        final FactoryInternal factory = context.getFactories().get(commsType);
        
        if (factory != null)
        {
            return factory;
        }

        throw new CCommException(String.format("Unable to find requested comms layer type: %s", commsType), 
                FormatProblem.OTHER);
    }
    
    /**
     * Get the Physical Link factory given the physical link type.
     * 
     * @param plType
     *      type of Physical Link the factory must create
     * @return
     *      factory that creates the given physical link type
     * @throws CCommException
     *      if the factory is not registered
     */
    private FactoryInternal getPhysicalLinkFactoryByPhysicalLinkType(final PhysicalLinkTypeEnum plType) 
            throws CCommException
    {
        final Map<String, FactoryInternal> factories = m_PhysLinkFactContext.getFactories();
        
        synchronized (factories)
        {
            for (FactoryInternal factory : factories.values())
            {
                if (factory.getPhysicalLinkCapabilities().getLinkType() == plType)
                {
                    return factory;
                }
            }
        }

        throw new CCommException(String.format("Unable to find requested PhysicalLink type: %s", plType), 
                FormatProblem.OTHER);
    }
    
    /**
     * Get the Link Layer factory given the product type.
     * 
     * @param llType
     *      type of Link Layer the factory must create
     * @return
     *      factory that creates the given product type
     * @throws CCommException
     *      if the factory is not registered
     */
    private FactoryInternal getLinkLayerFactoryByProductType(final String llType) throws CCommException
    {
        return getCommsLayerFactoryByProductType(m_LinkLayerFactContext, llType);
    }
    
    /**
     * Get the Transport Layer factory given the product type.
     * 
     * @param tlType
     *      type of Transport Layer the factory must create
     * @return
     *      factory that creates the given product type
     * @throws CCommException
     *      if the factory is not registered
     */
    private FactoryInternal getTransportLayerFactoryByProductType(final String tlType) 
            throws CCommException
    {
        return getCommsLayerFactoryByProductType(m_TransLayerFactContext, tlType);
    }
}
