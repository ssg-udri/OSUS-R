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
package mil.dod.th.ose.config.loading.impl;

import java.util.Map;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.FactoryTypeEnum;

import org.osgi.service.event.EventAdmin;

/**
 * This service is used to track the configuration of communication layers.
 * 
 * @author dlandoll
 */
public class CustomCommsTrackerCustomizer extends FactoryObjectTrackerCustomizer
{
    /**
     * Reference to custom comms service.
     */
    private final CustomCommsService m_CustomCommsService;

    /**
     * Reference to the Logging service.
     */
    private final LoggingService m_Logging;
    
    /**
     * Creates a new service tracker customizer.
     * @param objectConfig
     *      Factory object configuration to track
     * @param customCommsService
     *      Custom comms service used to create and manage communication links
     * @param loggingService
     *      Logging service used to record messages in the system log
     * @param eventAdmin
     *      Service for sending OSGi events
     */
    public CustomCommsTrackerCustomizer(final FactoryObjectConfig objectConfig, 
        final CustomCommsService customCommsService, final LoggingService loggingService,
        final EventAdmin eventAdmin)
    {
        super(objectConfig, loggingService, eventAdmin);
        m_CustomCommsService = customCommsService;
        m_Logging = loggingService;
    }

    @Override
    public void addingFactoryDescriptor(final FactoryDescriptor factory) throws FactoryException
    {
        final FactoryObjectConfig objectConfig = getObjectConfig();
        final FactoryTypeEnum type = objectConfig.getFactoryType();
       
        try
        {
            switch (type)
            {
                case TRANSPORT_LAYER:
                    addingTransportLayerService(factory, objectConfig);
                    break;
                case LINK_LAYER:
                    addingLinkLayerService(factory, objectConfig);
                    break;
                case PHYSICAL_LINK:
                    addingPhysicalLinkService(factory, objectConfig);
                    break;
                default:
                    m_Logging.error("Unknown communication layer type [%]", type);
            }
        }
        catch (final CCommException e)
        {
            throw new FactoryException("Unable to create layer: " + objectConfig, e);
        }
    }
    
    /**
     * Handle {@link mil.dod.th.core.ccomm.transport.TransportLayer} service addition.
     * @param factory 
     *      The factory instance
     * @param objectConfig
     *      The correlating object's configuration
     * @throws CCommException
     *      If exception while creating layer
     */
    private void addingTransportLayerService(final FactoryDescriptor factory,
            final FactoryObjectConfig objectConfig) throws CCommException
    {
        final String tranName = objectConfig.getName();
        
        final Map<String, Object> properties = translateStringMap(objectConfig.getProperties(), factory);
        
        final String productClass = objectConfig.getProductType();
        
        m_CustomCommsService.createTransportLayer(productClass, tranName, properties);
    }
    
    /**
     * Handle {@link mil.dod.th.core.ccomm.link.LinkLayer} service addition.
     * @param factory 
     *      The factory instance
     * @param objectConfig
     *      The correlating object's configuration
     * @throws CCommException
     *      If exception while creating layer
     */
    private void addingLinkLayerService(final FactoryDescriptor factory,
            final FactoryObjectConfig objectConfig) throws CCommException
    {
        final String linkName = objectConfig.getName();
        
        final Map<String, Object> properties = translateStringMap(objectConfig.getProperties(), factory);
        
        final String productClass = objectConfig.getProductType();

        m_CustomCommsService.createLinkLayer(productClass, linkName, properties);
    }
    
    /**
     * Handle {@link mil.dod.th.core.ccomm.physical.PhysicalLink} service addition.
     * @param factory 
     *      The factory instance
     * @param objectConfig
     *      The correlating object's configuration
     * @throws CCommException
     *      If exception while creating layer
     */
    private void addingPhysicalLinkService(final FactoryDescriptor factory,
            final FactoryObjectConfig objectConfig) throws CCommException
    {
        final Map<String, Object> properties = translateStringMap(objectConfig.getProperties(), factory);
        
        m_CustomCommsService.tryCreatePhysicalLink(objectConfig.getPhysicalLinkType(), objectConfig.getName(),
            properties);
    }
   
}
