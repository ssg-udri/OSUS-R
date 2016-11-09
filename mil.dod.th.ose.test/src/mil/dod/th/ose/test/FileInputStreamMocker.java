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
import java.io.FileInputStream;
import java.io.IOException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mock a {@link FileInputStream} for testing.
 * 
 * @author dhumeniuk
 *
 */
public class FileInputStreamMocker
{
    /**
     * Mock a {@link FileInputStream} so that a consumer will read in the given string as input.
     * 
     * @param input
     *      what to read in as input
     */
    public static FileInputStream mockIt(String input)
    {
        FileInputStream stream = mock(FileInputStream.class);
        final ByteArrayInputStream arrayInput = new ByteArrayInputStream(input.getBytes());
        
        try
        {
            when(stream.read(Mockito.any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>()
            {

                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable
                {
                    return arrayInput.read((byte[])invocation.getArguments()[0], (int)invocation.getArguments()[1], 
                            (int)invocation.getArguments()[2]); 
                }
            });
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        try
        {
            when(stream.read(Mockito.any(byte[].class))).thenAnswer(new Answer<Integer>()
            {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable
                {
                    return arrayInput.read((byte[])invocation.getArguments()[0]); 
                }
            });
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        return stream;
    }
}
