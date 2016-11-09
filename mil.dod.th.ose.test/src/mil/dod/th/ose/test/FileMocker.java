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

/**
 * @author dhumeniuk
 *
 */
public class FileMocker
{
    public static File mockIt(String path)
    {
        File file = mock(File.class);
        when(file.getPath()).thenReturn(path);
        when(file.toString()).thenReturn(path);
        when(file.mkdir()).thenReturn(true);
        return file;
    }

    public static File mockIt(File parent, String path)
    {
        File file = mock(File.class);
        String totalPath = parent.getPath() + "/" + path;
        when(file.getPath()).thenReturn(totalPath);
        when(file.toString()).thenReturn(totalPath);
        when(file.mkdir()).thenReturn(true);
        return file;
    }
}
