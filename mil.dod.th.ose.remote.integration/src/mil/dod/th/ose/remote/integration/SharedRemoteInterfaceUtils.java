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
package mil.dod.th.ose.remote.integration;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry.ValueCase;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * Utility class to help with common interactions that happen while testing and analyzing information received from
 * the Remote Interface.
 * @author callen
 *
 */
public final class SharedRemoteInterfaceUtils
{
    /**
     * Create a map with simply entries from the complex type map. Any complex types will be left out.
     */
    public static Map<String, Object> getSimpleMapFromComplexTypesMap(final List<ComplexTypesMapEntry> complexTypesMap)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        for (ComplexTypesMapEntry entry : complexTypesMap)
        {
            if (entry.getValueCase() == ValueCase.MULTI)
            {
                propertyMap.put(entry.getKey(), SharedMessageUtils.convertMultitypeToObject(entry.getMulti()));
            }
        }
        return propertyMap;
    }
}
