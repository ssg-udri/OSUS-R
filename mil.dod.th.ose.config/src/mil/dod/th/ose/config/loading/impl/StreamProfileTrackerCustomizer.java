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

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.FactoryObjectConfig;

import org.osgi.service.event.EventAdmin;

/**
 * This service is used to track the configuration of stream profiles.
 * 
 * @author jmiller
 *
 */
public class StreamProfileTrackerCustomizer extends FactoryObjectTrackerCustomizer
{

    /**
     * Reference to the datastream service.
     */
    private final DataStreamService m_DataStreamService;
    
    /**
     * Creates a new service tracker customizer.
     * 
     * @param objectConfig
     *      Factory object configuration
     * @param dataStreamService
     *      Service used to create and manage StreamProfile instances
     * @param loggingService
     *      Logging service used to record messages in the system log
     * @param eventAdmin
     *      Service for sending OSGi events
     */
    public StreamProfileTrackerCustomizer(final FactoryObjectConfig objectConfig, 
            final DataStreamService dataStreamService, final LoggingService loggingService,
            final EventAdmin eventAdmin)
    {
        super(objectConfig, loggingService, eventAdmin);
        m_DataStreamService = dataStreamService;
    }
    
    @Override
    void addingFactoryDescriptor(final FactoryDescriptor factory) throws FactoryException
    {
        final FactoryObjectConfig objectConfig = getObjectConfig();
        final String streamProfileName = objectConfig.getName();
        final String streamProfileProductType = objectConfig.getProductType();
        
        //Check if StreamProfile object already exists
        boolean exists = false;
        for (StreamProfile profile : m_DataStreamService.getStreamProfiles())
        {
            if (profile.getName().equals(streamProfileName))
            {
                exists = true;
                break;
            }
        }
        
        if (!exists)
        {
            final Map<String, Object> properties = translateStringMap(objectConfig.getProperties(), factory);
            
            try
            {
                m_DataStreamService.createStreamProfile(streamProfileProductType, streamProfileName, properties);
            }
            catch (final StreamProfileException spe)
            {
                throw new FactoryException("Unable to create stream profile: " + objectConfig, spe);
            }
        }
        

    }

}
