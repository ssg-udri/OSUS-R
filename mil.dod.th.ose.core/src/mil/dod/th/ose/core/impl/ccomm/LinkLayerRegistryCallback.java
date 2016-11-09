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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.RegistryDependency;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;

import org.osgi.service.cm.ConfigurationException;

/**
 * Callback registry implementation for {@link mil.dod.th.core.ccomm.link.LinkLayer}s.
 * @author allenchl
 *
 */
public class LinkLayerRegistryCallback implements FactoryRegistryCallback<LinkLayerInternal>
{
    /**
     * Reference to comms service to acquire layers.
     */
    private final CustomCommsServiceImpl m_CcomsService;
    
    /**
     * The {@link mil.dod.th.core.ccomm.CustomCommsService}'s factory service context.
     */
    private final FactoryServiceContext<LinkLayerInternal> m_FactoryServiceContext;

    /**
     * Logging service reference.
     */
    private final LoggingService m_Log;
    
    /**
     * Create instance by passing comms service.
     * @param cComms
     *      service used to acquire layers through
     * @param factoryServiceContext
     *      the factory service context of the directory service creating this callback
     * @param logger
     *      the service used for logging information
     */
    public LinkLayerRegistryCallback(final CustomCommsServiceImpl cComms,
            final FactoryServiceContext<LinkLayerInternal> factoryServiceContext, final LoggingService logger)
    {
        m_CcomsService = cComms;
        m_FactoryServiceContext = factoryServiceContext;
        m_Log = logger;
    }

    @Override
    public void preObjectInitialize(final LinkLayerInternal object) throws FactoryException, 
            ConfigurationException
    {
        checkPhysicalLink(object);
    }
    
    @Override
    public void postObjectInitialize(final LinkLayerInternal object)
    {
        final LinkLayerAttributes attributes = object.getConfig();
        
        // activate the link layer IF it is new and the 'activate on startup' property is set to true
        if (!m_FactoryServiceContext.getRegistry().isObjectCreated(object.getName()) 
                && attributes.activateOnStartup())
        {
            // do activation in separate thread
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    object.activateLayer();
                }
            }).start();
        }
    }
    
    @Override
    public void preObjectUpdated(final LinkLayerInternal object) throws FactoryException, 
            ConfigurationException
    {
        checkPhysicalLink(object);
    }

    @Override
    public void onRemovedObject(final LinkLayerInternal object)
    {
        if (object.isActivated())
        {
            object.deactivateLayer();
        }
        
        if (object.getPhysicalLink() != null)
        {
            try
            {
                object.getPhysicalLink().release();
            }
            catch (final IllegalStateException | IllegalArgumentException e)
            {
                m_Log.error(e, "Unable to release physical link [%s]", object.getPhysicalLink().getName());
            }
        }
    }

    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        final List<RegistryDependency> listRegDep = new ArrayList<>();
        
        listRegDep.add(new RegistryDependency(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, false)
        {
            @Override
            public Object findDependency(final String objectName)
            {
                return m_CcomsService.getPhysicalLinkRegistry().findObjectByName(objectName);
            }
        });
        
        return listRegDep;
    }
    
    /**
     * Check the physical link property and request the necessary object.
     * 
     * @param object
     *      object to check
     * @throws FactoryException
     *      if there is an error when processing the physical link configured
     */
    private void checkPhysicalLink(final LinkLayerInternal object) throws FactoryException
    {
        final String physicalLinkName = object.getConfig().physicalLinkName();
        
        if (Strings.isNullOrEmpty(physicalLinkName))
        {
            // if no physical link, nothing to check
            return;
        }
        
        // The Phys Link should be able to be fetched since the deps were cleared as satisfied 
        // by the time this call is made.
        final PhysicalLinkInternal physicalLink;
        try
        {
            physicalLink = m_CcomsService.getPhysicalLinkRegistry().getObjectByName(physicalLinkName);
        }
        catch (final IllegalArgumentException e)
        {
            throw new FactoryException(String.format("Unable to fetch Physical Link [%s].", physicalLinkName), e);
        }
        
        if (!physicalLink.isInUse())
        {
            m_CcomsService.requestPhysicalLink(physicalLinkName);
        }

        //bracket ending for error messages
        if (physicalLink.getOwner() == null)
        {
            // no one owns this yet, requested but not owned, give to this link
            object.setPhysicalLink(physicalLink);
            physicalLink.setOwner(object);
        }
        else if (physicalLink.getOwner() != object)
        {
            throw new FactoryException(String.format(
                    "Physical Link [%s] already has a link layer owner named: [%s]. Unable to give ownership to [%s].", 
                    physicalLink.getName(), physicalLink.getOwner().getName(), object.getName()));
        }
        else
        {
            m_Log.error("Physical link [%s] is already owned by link layer [%s].", 
                    physicalLink.getName(), object.getName());
        }
    }
}
