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
package mil.dod.th.ose.datastream;


import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.RegistryDependency;

import org.osgi.service.cm.ConfigurationException;


/**
 * Registry that holds {@link mil.dod.th.core.datastream.StreamProfile}s.
 * 
 * @author jmiller
 * 
 *
 */
final class StreamProfileRegistryCallback implements FactoryRegistryCallback<StreamProfileInternal>
{
    
    @Override
    public void preObjectInitialize(final StreamProfileInternal object) throws FactoryException, ConfigurationException
    {
        // no action needed
        
    }

    @Override
    public void postObjectInitialize(final StreamProfileInternal object) throws FactoryException
    {        
        // no action needed
        
    }

    @Override
    public void preObjectUpdated(final StreamProfileInternal object) throws FactoryException, ConfigurationException
    {
        // no action needed
        
    }

    @Override
    public void onRemovedObject(final StreamProfileInternal object)
    {
        // no action needed
        
    }

    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        return new ArrayList<RegistryDependency>();
    }

}
