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
package mil.dod.th.ose.test;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matcher;

/**
 * @author dhumeniuk
 *
 */
public class AssertUtils
{
    public static void assertSubStringCount(String string, String substring, Matcher<? super Integer> matcher)
    {
        int count = 0;
        int indexOf = string.indexOf(substring);
        while (indexOf != -1)
        {
            count++;
            
            // skip past found substring
            string = string.substring(indexOf + substring.length());
            
            // look for it again
            indexOf = string.indexOf(substring);
        }
        
        assertThat(String.format("Expecting '%s' to contain '%s'", string, substring), count, matcher);
    }
}
