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

import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.AddressConfig;

import org.osgi.service.event.EventAdmin;

/**
 * This service is used to track the configuration of addresses.
 * 
 * @author dlandoll
 */
public class AddressTrackerCustomizer extends FactoryObjectTrackerCustomizer
{
    /**
     * Reference to the asset directory service.
     */
    private final AddressManagerService m_AddressManagerService;
    
    /**
     * Creates a new service tracker customizer.
     * 
     * @param addressConfig
     *      Address configuration
     * @param addressManagerService
     *      Address manager service used to create and manage addresses
     * @param loggingService
     *      Logging service used to record messages in the system log
     * @param eventAdmin
     *      Service for sending OSGi events
     */
    public AddressTrackerCustomizer(final AddressConfig addressConfig, 
            final AddressManagerService addressManagerService, final LoggingService loggingService,
            final EventAdmin eventAdmin)
    {
        super(addressConfig, loggingService, eventAdmin);
        m_AddressManagerService = addressManagerService;
    }

    @Override
    public void addingFactoryDescriptor(final FactoryDescriptor factory) throws FactoryException
    {
        // At this point it is either the first run of the configuration or the create policy is set to IfMissing.
        // Therefore, if the address does not exist in either of these cases then it should be created or retrieved.
        try
        {
            m_AddressManagerService.getOrCreateAddress(getAddressConfig().getAddressDescription());
        }
        catch (final IllegalArgumentException | CCommException ex)
        {
            throw new FactoryException("Unable to get or create address: " + getAddressConfig().getAddressDescription(),
                    ex);
        }
    }
}
