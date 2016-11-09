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

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mocks a {@link FileReader}.
 * @author dhumeniuk
 *
 */
public class FileReaderMocker
{
    /**
     * Mock a file reader that will return the given content.
     */
    public static FileReader mockFileReader(String contents)
    {
        FileReader fileReader = mock(FileReader.class);
        
        // mock the bytes retrieved
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes());
        try
        {
            when(fileReader.read(Mockito.any(char[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>()
            {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable
                {
                    char[] cbuf = (char[])invocation.getArguments()[0];
                    byte[] bbuf = new byte[cbuf.length];
                    int rv = inputStream.read(bbuf, (Integer)invocation.getArguments()[1], 
                            (Integer)invocation.getArguments()[2]);
                    System.arraycopy(new String(bbuf).toCharArray(), 0, cbuf, 0, bbuf.length);
                    return rv;
                }
            });
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        return fileReader;
    }
}
