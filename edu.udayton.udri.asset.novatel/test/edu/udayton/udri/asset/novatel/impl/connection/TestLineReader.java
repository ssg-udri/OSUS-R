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
package edu.udayton.udri.asset.novatel.impl.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * @author Cheryl
 *
 */
public class TestLineReader
{
    /**
     * Test the read line will parse the lines correctly.
     */
    @Test
    public void testReadLine() throws IOException
    {
        final String novatelPVAString = "first-line;";
        final String novatelTimeString = "second-line;";
        
        // the input stream
        InputStream inStream = 
                new ByteArrayInputStream(new String(novatelPVAString + "\r\n" + novatelTimeString + "\r\n").getBytes());
        
        LineReader reader = new LineReader(inStream);
        assertThat(reader.readLine(), is(novatelPVAString));
        assertThat(reader.readLine(), is(novatelTimeString));
    }
    
    /**
     * Test that close will close the underlying input stream.
     */
    @Test
    public void testClose() throws IOException
    {
        InputStream inStream = mock(InputStream.class);
        
        LineReader reader = new LineReader(inStream);
        
        reader.close();
        
        verify(inStream).close();
    }
}
