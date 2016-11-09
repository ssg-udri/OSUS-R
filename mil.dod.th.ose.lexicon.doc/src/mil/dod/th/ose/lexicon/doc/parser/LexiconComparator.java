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
package mil.dod.th.ose.lexicon.doc.parser;

import java.util.Comparator;

/**
 * Class used to compare lexicon objects by name.
 * 
 * @author cweisenborn
 */
public class LexiconComparator implements Comparator<LexiconBase>
{
    @Override
    public int compare(final LexiconBase object1, final LexiconBase object2)
    {
        return object1.getName().compareTo(object2.getName());
    }
}
