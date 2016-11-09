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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mock a {@link FileOutputStream} for testing.
 * 
 * @author dhumeniuk
 *
 */
public class FileOutputStreamMocker
{
    /**
     * Create a {@link FileOutputStream} mock that writes it's data to the given output stream so the output data can be
     * checked easily.
     */
    public static FileOutputStream mockIt(final ByteArrayOutputStream outputData) throws IllegalStateException
    {
        FileOutputStream stream = mock(FileOutputStream.class);
        
        try
        {
            doAnswer(new Answer<Void>()
            {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    outputData.write((byte[])invocation.getArguments()[0]);
                    return null;
                }
            }).when(stream).write(Mockito.any(byte[].class));
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        try
        {
            doAnswer(new Answer<Void>()
            {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    outputData.write((byte[])invocation.getArguments()[0], (int)invocation.getArguments()[1], 
                            (int)invocation.getArguments()[2]);
                    return null;
                }
            }).when(stream).write(Mockito.any(byte[].class), anyInt(), anyInt());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        return stream;
    }
}
