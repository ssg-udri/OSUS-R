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

import mil.dod.th.core.ccomm.AddressContext;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Used by the {@link mil.dod.th.core.ccomm.AddressManagerService} service to access an Address.
 * 
 * @author dlandoll
 */
public interface AddressInternal extends AddressContext, FactoryObjectInternal // NOPMD: avoid constants interface
                                                                         // not constants interface, extends interfaces
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.ccomm.AddressInternal";
    
    /**
     * Key value pair to denote an address service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=Address";
}
