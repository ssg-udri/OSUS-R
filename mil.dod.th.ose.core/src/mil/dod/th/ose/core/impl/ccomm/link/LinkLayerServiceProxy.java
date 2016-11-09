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
package mil.dod.th.ose.core.impl.ccomm.link;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.CustomCommsServiceImpl;
import mil.dod.th.ose.core.impl.ccomm.LinkLayerRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.link.data.LinkLayerFactoryObjectDataManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Proxy to provide {@link mil.dod.th.core.ccomm.link.LinkLayer} specific service functionality.
 * 
 * @author allenchl
 *
 */
@Component(properties = { LinkLayerInternal.SERVICE_TYPE_PAIR })
public class LinkLayerServiceProxy implements FactoryServiceProxy<LinkLayerInternal>
{
    /**
     * Component factory used to create {@link LinkLayerImpl}'s.
     */
    private ComponentFactory m_LinkLayerComponentFactory;
    
    /**
     * Service for storing persisted data about link layers.
     */
    private LinkLayerFactoryObjectDataManager m_LinkFactoryObjectDataManager;
    
    /**
     * Event handler service registration, used for deactivation events.
     */
    private ServiceRegistration<EventHandler> m_EventDeactivatedService;
    
    /**
     * Semaphore used to synchronize when waiting to deactivate component.
     */
    private Semaphore m_DeactivateWaitSem;

    private LoggingService m_Logging;

    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link LinkLayerImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(component.factory=" + LinkLayerInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setLinkLayerFactory(final ComponentFactory factory)
    {
        m_LinkLayerComponentFactory = factory;
    }
    
    /**
     * Bind the factory object data information manager for link layers.
     * 
     * @param linkFactObjMgr
     *      used to store the persisted data
     */
    @Reference
    public void setLinkLayerFactoryObjectDataManager(final LinkLayerFactoryObjectDataManager linkFactObjMgr)
    {
        m_LinkFactoryObjectDataManager = linkFactObjMgr;
    }
    
    /**
     * Activate the component by registering for deactivation events.
     * 
     * @param context
     *      bundle context
     */
    @Activate
    public void activate(final BundleContext context)
    {
        // register to get deactivation events from each link layer, can't register a service during deactivation so
        // must do it now
        m_DeactivateWaitSem = new Semaphore(0);
        final EventHandler handler = new EventHandler()
        {
            @Override
            public void handleEvent(final Event event)
            {
                m_DeactivateWaitSem.release();
            }
        };
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, LinkLayer.TOPIC_DEACTIVATED);
        m_EventDeactivatedService = context.registerService(EventHandler.class, handler, props);
    }
    
    /**
     * Deactivate the component.
     */
    @Deactivate
    public void deactivate()
    {
        m_EventDeactivatedService.unregister();
    }
    
    @Override
    public void initializeProxy(final LinkLayerInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final LinkLayerProxy linkProxy = (LinkLayerProxy)proxy;
        linkProxy.initialize(object, props);
    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        return m_LinkLayerComponentFactory.newInstance(new Hashtable<String, Object>());
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<LinkLayerInternal> factoryServiceContext,
            final  FactoryInternal factory, final int filter)
    {
        return new AttributeDefinition[]{};
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return LinkLayerCapabilities.class;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return LinkLayer.class;
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> linkLayerFactProps = new Hashtable<>();
        linkLayerFactProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, LinkLayerFactory.class);
        return linkLayerFactProps;
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<LinkLayerInternal> factoryServiceContext,
           final  FactoryInternal factory)
    {
        final FactoryRegistry<LinkLayerInternal> reg = factoryServiceContext.getRegistry();
        
        // deactivate each active Link Layer
        int activatedCount = 0;
        // Deactivate link layers from the given factory
        for (LinkLayerInternal linkLayer : reg.getObjectsByProductType(factory.getProductType()))
        {
            if (linkLayer.isActivated())
            {
                activatedCount++;
                m_Logging.info("Deactivating Link Layer: %s.", linkLayer.getName());
                linkLayer.deactivateLayer();
            }
        }
        
        // wait for all Link Layers to be deactivated
        try
        {
            final int TIMEOUT = 5;
            m_DeactivateWaitSem.tryAcquire(activatedCount, TIMEOUT, TimeUnit.SECONDS);
            m_DeactivateWaitSem.availablePermits();
        }
        catch (final InterruptedException e)
        {
            m_Logging.warning("Interrupted waiting for Link Layers to deactivate. Caused by: %s", e.getMessage());
        }
        
        // check status of each Link Layer to make sure they all were deactivated
        for (LinkLayer linkLayer : reg.getObjectsByProductType(factory.getProductType()))
        {
            if (linkLayer.isActivated())
            {
                m_Logging.warning("Unable to deactivate Link Layer: %s.", linkLayer.getName());
            }
        }
    }

    @Override
    public void beforeAddFactory(final FactoryServiceContext<LinkLayerInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        // nothing to check
    }

    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_LinkFactoryObjectDataManager;
    }

    @Override
    public FactoryRegistryCallback<LinkLayerInternal> createCallback(
            final FactoryServiceContext<LinkLayerInternal> factoryServiceContext)
    {
        final CustomCommsServiceImpl cComms = (CustomCommsServiceImpl)factoryServiceContext.getDirectoryService();
        
        return new LinkLayerRegistryCallback(cComms, factoryServiceContext, m_Logging);
    }
}
