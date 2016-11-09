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

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;

/**
 * Address implementation of a {@link mil.dod.th.core.factory.FactoryObject}.
 * 
 * @author dlandoll
 */
@Component(factory = AddressInternal.COMPONENT_FACTORY_REG_ID)
public class AddressImpl extends AbstractFactoryObject implements AddressInternal
{
    @Override
    public Map<String, Object> getEventProperties(final String prefix)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(prefix + Address.EVENT_PROP_ADDRESS_SUFFIX, this);
        props.put(prefix + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX, getFactory().getProductType()); 
        props.put(prefix + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX, getName());
        props.put(prefix + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX, getDescription());
        return props;
    }
    
    @Override
    public String getDescription()
    {
        final AddressCapabilities addrCaps = getFactory().getAddressCapabilities();
        final AddressProxy proxy = (AddressProxy)getProxy();
        return addrCaps.getPrefix() + ":" + proxy.getAddressDescriptionSuffix();
    }
    
    @Override
    public String toString()
    {
        return getDescription();
    }
    
    @Override
    public boolean equalProperties(final Map<String, Object> properties)
    {
        final AddressProxy proxy = (AddressProxy)getProxy();
        return proxy.equalProperties(properties);
    }
}
