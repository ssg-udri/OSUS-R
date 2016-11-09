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
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestStringUtils
{
    @Test
    public void testSplitCamelCase()
    {
        assertThat(StringUtils.splitCamelCase("lowercase"), is("lowercase"));
        assertThat(StringUtils.splitCamelCase("Class"), is("Class"));
        assertThat(StringUtils.splitCamelCase("MyClass"), is("My Class"));
        assertThat(StringUtils.splitCamelCase("HTML"), is("HTML"));
        assertThat(StringUtils.splitCamelCase("PDFLoader"), is("PDF Loader"));
        assertThat(StringUtils.splitCamelCase("AString"), is("A String"));
        assertThat(StringUtils.splitCamelCase("SimpleXMLParser"), is("Simple XML Parser"));
        assertThat(StringUtils.splitCamelCase("GL11Version"), is("GL 11 Version"));
    }
}
