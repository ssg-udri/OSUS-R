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

import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.RegistryDependency;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;

import org.osgi.service.cm.ConfigurationException;

/**
 * Callback registry implementation for {@link mil.dod.th.core.ccomm.physical.PhysicalLink}s.
 * @author allenchl
 *
 */
public class PhysicalLinkRegistryCallback implements FactoryRegistryCallback<PhysicalLinkInternal>
{
    /**
     * Logging service reference.
     */
    private final LoggingService m_Log;
    
    /**
     * Create a new instance of the physical link registry callback class.
     * @param logger
     *      the service used for logging information
     */
    public PhysicalLinkRegistryCallback(final LoggingService logger)
    {
        m_Log = logger;
    }
    
    @Override
    public void preObjectInitialize(final PhysicalLinkInternal object) 
            throws FactoryException, ConfigurationException
    {
        //nothing for now
    }
    
    @Override
    public void postObjectInitialize(final PhysicalLinkInternal object) throws FactoryException
    {
        //nothing for now
    }
    
    @Override
    public void preObjectUpdated(final PhysicalLinkInternal object) 
            throws FactoryException, ConfigurationException
    {
        //nothing for now
    }

    @Override
    public void onRemovedObject(final PhysicalLinkInternal object)
    {
        if (object.isOpen())
        {
            try
            {
                object.close();
            }
            catch (final PhysicalLinkException e)
            {
                m_Log.error(e, "Unable to close physical link when unregistering physical link factory");
            }
        }
    }
    
    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        return new ArrayList<RegistryDependency>();
    }
}
