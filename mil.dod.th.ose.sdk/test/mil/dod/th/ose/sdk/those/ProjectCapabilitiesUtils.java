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
package mil.dod.th.ose.sdk.those;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that holds the assertTestPattern method used by the TestProjectCapabilities classes in this test folder.
 * 
 * @author m.elmo
 *
 */
public final class ProjectCapabilitiesUtils
{
    public static void assertElement(String lineAccum, String tag, String value)
    {
        assertTestPattern("<" + tag + ">", "</" + tag + ">", value, lineAccum);
    }
    
    /**
     * Asserts that a pattern is included in a string.
     * @param tagStart the beginning tag used to create the pattern to test
     * @param tagEnd the closing tag used to create the pattern to test
     * @param field the sub-tag or field used to create the pattern to test
     * @param lineAccum the string to test against
     */
    public static void assertTestPattern(String tagStart, String tagEnd, String field, String lineAccum)
    {    
        String regex = ".*" + tagStart + ".*" + field + ".*" + tagEnd;
        Pattern p = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(lineAccum);    
        assertThat(" Pattern " + regex + " is not contained in " + lineAccum, m.find(), is(true));
    }
}
