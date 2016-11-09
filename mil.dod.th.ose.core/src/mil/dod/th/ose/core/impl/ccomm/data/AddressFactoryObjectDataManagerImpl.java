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
package mil.dod.th.ose.core.impl.ccomm.data;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager;

/**
 * Address factory object specific implementation of the the {@link BaseFactoryObjectDataManager}. Used to persist
 * the address specific information.
 * @author callen
 *
 */
@Component
public class AddressFactoryObjectDataManagerImpl extends BaseFactoryObjectDataManager implements
        AddressFactoryObjectDataManager
{
    
    @Override
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        //set in the super class
        super.setPersistentDataStore(persistentDataStore);
    }
    
    @Override
    protected Class<? extends FactoryObject> getServiceObjectType()
    {
        return Address.class;
    }
}
