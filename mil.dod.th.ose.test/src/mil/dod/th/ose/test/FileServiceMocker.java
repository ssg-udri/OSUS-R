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

import java.io.File;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import mil.dod.th.ose.utils.FileService;

/**
 * @author dhumeniuk
 *
 */
public class FileServiceMocker
{
    public static FileService mockIt()
    {
        FileService fs = mock(FileService.class);
        
        when(fs.getFile(anyString())).thenAnswer(new Answer<File>()
        {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable
            {
                return FileMocker.mockIt((String)invocation.getArguments()[0]);
            }
        });
        
        when(fs.getFile(Mockito.any(File.class), anyString())).thenAnswer(new Answer<File>()
        {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable
            {
                return FileMocker.mockIt((File)invocation.getArguments()[0], (String)invocation.getArguments()[1]);
            }
        });
        
        return fs;
    }
}
