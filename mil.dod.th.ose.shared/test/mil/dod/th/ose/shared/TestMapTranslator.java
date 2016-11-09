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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.types.MapEntry;

import org.junit.Test;

/**
 * @author callen
 *
 */
public class TestMapTranslator
{
    @Test
    public void testMapTranslation()
    {
        final Map<String, Object> mapToTrans = new HashMap<String, Object>();
        mapToTrans.put("Cookie", "Jar");
        mapToTrans.put("Junk", "Trunk");
        final List<MapEntry> entries = MapTranslator.translateMap(mapToTrans);
        assertThat(entries, is(notNullValue()));
        assertThat(entries.size(), is(2));

        assertThat(entries.get(0).getKey(), is("Cookie"));
        assertThat((String)entries.get(0).getValue(), is("Jar"));

        assertThat(entries.get(1).getKey(), is("Junk"));
        assertThat((String)entries.get(1).getValue(), is("Trunk"));
    }
    
    @Test
    public void testPairListTranslation()
    {
        final List<MapEntry> mapEntries = new ArrayList<MapEntry>();
        mapEntries.add(new MapEntry("Foo", "Bar"));
        mapEntries.add(new MapEntry("Funky", "Monkey"));
        final Map<String, Object> mapFromList = MapTranslator.translatePairList(mapEntries);
        assertThat(mapFromList, is(notNullValue()));
        assertThat(mapFromList.size(), is(2));
        
        assertThat(mapFromList, hasEntry("Foo", (Object)"Bar"));
        assertThat(mapFromList, hasEntry("Funky", (Object)"Monkey"));
    }    
}
