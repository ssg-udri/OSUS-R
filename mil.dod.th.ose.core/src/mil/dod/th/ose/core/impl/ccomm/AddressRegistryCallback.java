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

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.RegistryDependency;

import org.osgi.service.cm.ConfigurationException;

/**
 * Registry callback for addresses.
 */
final class AddressRegistryCallback implements FactoryRegistryCallback<AddressInternal>
{
    @Override
    public void preObjectInitialize(final AddressInternal object) 
            throws FactoryException, ConfigurationException
    {
        //nothing for now
    }
    
    @Override
    public void postObjectInitialize(final AddressInternal object) throws FactoryException
    {
        //nothing for now
    }
    
    @Override
    public void preObjectUpdated(final AddressInternal object) 
            throws FactoryException, ConfigurationException
    {
        //nothing for now
    }

    @Override
    public void onRemovedObject(final AddressInternal object)
    {
        // No actions necessary
    }
    
    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        return new ArrayList<RegistryDependency>();
    }
}