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
package mil.dod.th.ose.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.types.MapEntry;

/**
 * Helper class for translating a Key-Value pair list into a map. 
 * 
 * @author callen
 *
 */
public final class MapTranslator 
{
     /**
     * Private inner constructor.   
     */
    private MapTranslator()
    { 
    }
    
    /**
     * This method takes a list that is MapEntry type, and returns a Map.  
     * 
     * @param list
     *     List to translate to a map
     * @return 
     *     String-Object paired map.
     */
    public static Map<String, Object> translatePairList(final List<MapEntry> list)
    {
        final Map<String, Object> argMap = new HashMap<String, Object>();
        for (MapEntry entry : list)
        {
            argMap.put(entry.getKey(), entry.getValue());
        }
        return argMap;
    }
    
    /**
     * Translate a Map into an List of MapEntry type entries.
     * 
     * @param map
     *     The Map to translate to an List
     * @return 
     *     List that contains the entries from the map
     */   
    public static List<MapEntry> translateMap(final Map<String, Object> map)
    {
        final List<MapEntry> mapList = new ArrayList<MapEntry>();
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            final MapEntry mapEntry =  
                new MapEntry(entry.getKey(), entry.getValue()); //NOPMD needed for translation
            mapList.add(mapEntry);
        }
        return mapList;
    }
}
