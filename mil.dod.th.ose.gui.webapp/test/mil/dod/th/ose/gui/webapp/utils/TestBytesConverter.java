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
package mil.dod.th.ose.gui.webapp.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author Dave Humeniuk
 *
 */
public class TestBytesConverter
{
    /**
     * Test that the converter correctly reports not supporting conversion
     */
    @Test
    public void testGetAsObject()
    {
        BytesConverter bytesConverter = new BytesConverter();
        
        try
        {
            bytesConverter.getAsObject(null, null, "500 KB");
            fail("Operation is not currently supported, expecting exception");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }
    
    /**
     * Test that the converter will properly convert a value to a string with the correct units.
     */
    @Test
    public void testGetAsString()
    {
        BytesConverter bytesConverter = new BytesConverter();
        assertThat(bytesConverter.getAsString(null, null, 1023L), is("1023 B"));
        assertThat(bytesConverter.getAsString(null, null, 1024L), is("1 KB"));
        assertThat(bytesConverter.getAsString(null, null, 1023 * 1024L), is("1023 KB"));
        assertThat(bytesConverter.getAsString(null, null, 1024 * 1024L), is("1 MB"));
        assertThat(bytesConverter.getAsString(null, null, 1023 * 1024 * 1024L), is("1023 MB"));
        assertThat(bytesConverter.getAsString(null, null, 1024 * 1024 * 1024L), is("1 GB"));
        assertThat(bytesConverter.getAsString(null, null, 1023 * 1024 * 1024 * 1024L), is("1023 GB"));
        assertThat(bytesConverter.getAsString(null, null, 1024 * 1024 * 1024 * 1024L), is("1 TB"));
    }
}
