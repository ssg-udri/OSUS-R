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
package example.ccomms;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressContext;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.factory.Extension;

@Component(factory = Address.FACTORY)
public class ExampleAddress implements AddressProxy
{
    private int m_Property;

    @Override
    public void initialize(final AddressContext context, final Map<String, Object> props)
    {
        m_Property = Configurable.createConfigurable(ExampleAddressAttributes.class, props).a();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        m_Property = Configurable.createConfigurable(ExampleAddressAttributes.class, props).a();
    }

    @Override
    public String getAddressDescriptionSuffix() 
    {
        return String.valueOf(m_Property);
    }

    @Override
    public boolean equalProperties(Map<String, Object> properties) 
    {
        return properties.get("a").equals(m_Property);
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
