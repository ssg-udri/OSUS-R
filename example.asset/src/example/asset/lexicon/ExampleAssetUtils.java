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
package example.asset.lexicon;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.types.MapEntry;

/**
 * Contains functions often used in the example assets. *
 */
public class ExampleAssetUtils
{
    /**
     * Create a sample MapEntry list to be used in the reserved field.
     * @return
     *      Predetermined values for testing reserved list fields.
     */
    public static List<MapEntry> buildReservedList()
    {
        List<MapEntry> reserved = new ArrayList<MapEntry>();
        reserved.add(new MapEntry("String", "Bob"));
        reserved.add(new MapEntry("Double", 2.25));
        reserved.add(new MapEntry("Integer", 100));
        reserved.add(new MapEntry("Boolean", true));
        reserved.add(new MapEntry("Long", 12345L));
        return reserved;
    }

}
