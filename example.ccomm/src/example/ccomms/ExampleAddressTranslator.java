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

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.ccomm.AddressTranslator;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.ProductType;

/**
 * Example address translator. This translator has the same product type and implementation as
 * {@link example.ccomms.duplicates.ExampleAddressTranslatorDuplicate}, this is intended and should not be changed. This
 * translator should always be registered first by being included in the start up bundles where the duplicate bundle 
 * will be installed at runtime. This is intended as the second one will log an error to the log stating that it could 
 * not be registered. This is done to 'prove' that only ONE translator can be registered for a particular type without 
 * creating a race condition and intermittent test failures. 
 * 
 * @author allenchl
 *
 */
@Component
@ProductType(ExampleAddress.class)
public class ExampleAddressTranslator implements AddressTranslator
{
    @Override
    public Map<String, Object> getAddressPropsFromString(final String addressDescriptionSuffix) 
        throws CCommException
    {
        final Map<String,Object> properties = new HashMap<String,Object>();
        properties.put("a", Integer.parseInt(addressDescriptionSuffix));
        return properties;
    }
}
