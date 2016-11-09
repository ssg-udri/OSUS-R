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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.ose.config.loading.AddressLoader;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This service is responsible for loading address configurations at the start
 * up of the service.
 * 
 * @author allenchl
 */
@Component
public class AddressLoaderImpl implements AddressLoader
{
    /**
     * List of service trackers used for each address object configuration.
     */
    private List<ServiceTracker<FactoryDescriptor, FactoryDescriptor>> m_ServiceTrackerList;

    /**
     * Used to track the registration of the {@link EventHandler}.
     */
    private ServiceRegistration<EventHandler> m_EventHandlerReg;

    /**
     * The number of service trackers created based on the address objects being processed.
     */
    private int m_NumServiceTrackersCreated;

    /**
     * Address management service to load addresses to.
     */
    private AddressManagerService m_AddrManager;
    
    /**
     * Logging service to use.
     */
    private LoggingService m_Log;
    
    /**
     * The event admin service used to send an event when processing of addresses have been completed.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Reference to this bundle's OSGi context.
     */
    private BundleContext m_BundleContext;

    /**
     * Set the address service.
     * 
     * @param addressManager
     *      the service instance to use
     */
    @Reference
    public void setAddressManager(final AddressManagerService addressManager)
    {
        m_AddrManager = addressManager;
    }
    
    /**
     * Set the log service to use.
     * 
     * @param logService
     *      the service instance to use
     */
    @Reference
    public void setLoggingService(final LoggingService logService)
    {
        m_Log = logService;
    }
    
    /**
     * Set the event admin service to use.
     * 
     * @param eventAdmin
     *      the service instance to use
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * The service component activation method.
     * 
     * @param context
     *            context for the bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
        m_ServiceTrackerList = new ArrayList<ServiceTracker<FactoryDescriptor, FactoryDescriptor>>();

        // Create an event handler to wait for all factory objects to be loaded
        final EventHandler eventHandler = new EventHandler()
        {
            private int m_AddressObjCount;

            @Override
            public void handleEvent(final Event event)
            {
                m_AddressObjCount++;
                if (m_AddressObjCount == m_NumServiceTrackersCreated)
                {
                    sendAddressesProcessedEvent();
                }
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_LOADED_EVENT);
        m_EventHandlerReg = m_BundleContext.registerService(EventHandler.class, eventHandler, props);
    }

    /**
     * The service component deactivation method.
     * 
     * Any services being tracked by this component are removed.
     */
    @Deactivate
    public void deactivate()
    {
        for (ServiceTracker<FactoryDescriptor, FactoryDescriptor> tracker : m_ServiceTrackerList)
        {
            tracker.close();
        }

        m_EventHandlerReg.unregister();
    }

    @Override
    public synchronized void process(final List<AddressConfig> addressConfigs, 
            final boolean firstRun)
    {
        // Determine how many service trackers will be created. This is needed to avoid race conditions that occur when
        // receiving factory object loaded events before all service trackers have been created.
        for (AddressConfig config : addressConfigs)
        {
            if ((config.getCreatePolicy() == CreatePolicyEnum.FIRST_RUN && firstRun)
                    || config.getCreatePolicy() == CreatePolicyEnum.IF_MISSING)
            {
                m_NumServiceTrackersCreated++;
            }
        }

        for (AddressConfig config : addressConfigs)
        {
            try
            {
                // If the create policy is "if missing" the address should be created/fetched always,
                // as the call to the address manager creates it if missing, or gets it if it's already in
                // existence. Otherwise only create ONCE if the create policy is
                // "first run" and the first run flag is true.
                if ((config.getCreatePolicy() == CreatePolicyEnum.FIRST_RUN && firstRun)
                        || config.getCreatePolicy() == CreatePolicyEnum.IF_MISSING)
                {
                    m_ServiceTrackerList.add(createTracker(config));
                }
            }
            catch (final Exception ex)
            {
                m_Log.error(ex, "Invalid address config: %s", config);
            }
        }

        if (m_ServiceTrackerList.isEmpty())
        {
            sendAddressesProcessedEvent();
        }
    }

    /**
     * Creates the service tracker customizer for a {@link mil.dod.th.model.config.AddressConfig}.
     * 
     * @param config
     *            Address configuration
     * @return Service tracker customizer for the address config type
     * @throws IllegalArgumentException
     *            If an unknown address type or invalid product type is provided in the address config
     */
    private ServiceTracker<FactoryDescriptor, FactoryDescriptor> createTracker(final AddressConfig config)
            throws IllegalArgumentException
    {
        final ServiceTrackerCustomizer<FactoryDescriptor, FactoryDescriptor> customizer;
        customizer = new AddressTrackerCustomizer(config, m_AddrManager, m_Log, m_EventAdmin);

        final Filter filter = filterFromAddrDesc(config.getAddressDescription());

        final ServiceTracker<FactoryDescriptor, FactoryDescriptor> tracker = 
                new ServiceTracker<FactoryDescriptor, FactoryDescriptor>(m_BundleContext, filter, customizer);
        tracker.open();

        return tracker;
    }
    
    /**
     * Method to generate an OSGi filter from an address description.
     * 
     * @param addrDesc
     *  address description that is to be used for the filter
     * @return
     *  the {@link Filter} object that has the prefix from the given description as its filter
     */
    private Filter filterFromAddrDesc(final String addrDesc)
    {
        final String errorMessage = String.format(
                "AddressConfig description [%s] is invalid as it does not contain a prefix and a suffix",
                addrDesc);
        
        final String[] addressParts = addrDesc.split(":", 2);
        if (addressParts.length != 2)
        {
            throw new IllegalArgumentException(errorMessage);
        }
        
        final String prefix = addressParts[0];
        if (prefix.isEmpty())
        {
            throw new IllegalArgumentException(errorMessage);
        }

        final String serviceMatch = String.format("(%s=%s)", AddressFactory.ADDRESS_FACTORY_PREFIX_SERVICE_PROPERTY,
                prefix);

        return filterFromFilterString(serviceMatch);
    }
    
    /**
     * Method to create an OSGi filter used when creating a service tracker instance.
     * 
     * @param serviceMatch
     *  the filter string that is to be used when creating the OSGi {@link Filter}
     * @return
     *  the {@link Filter} object that has the given key-value pair as its filter
     */
    private Filter filterFromFilterString(final String serviceMatch)
    {
        final Filter filter;
        try
        {
            filter = m_BundleContext.createFilter(serviceMatch);
        }
        catch (final InvalidSyntaxException ex)
        {
            throw new IllegalArgumentException(String.format("Given filter [ %s ] cannot be parsed",
                     serviceMatch), ex);
        }
        
        return filter;
    }

    /**
     * Send event for notification of configurations being loaded.
     */
    private void sendAddressesProcessedEvent()
    {
        final String topic = ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT;
        final Event configCompleteEvt = new Event(topic, new HashMap<String, Object>());
        m_EventAdmin.postEvent(configCompleteEvt);
        m_Log.info("Address loading completed.");
    }
}
