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
package mil.dod.th.ose.jaxbprotoconverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Test class for the {@link PathUtils} class.
 * 
 * @author cweisenborn
 */
public class TestPathUtils
{
    /**
     * Verify that the appropriate path is returned by the get relative path method.
     */
    @Test
    public void testGetRelativePath()
    {
        File file = mock(File.class);
        File baseDir = mock(File.class);
        Path filePath = mock(Path.class);
        Path baseDirPath = mock(Path.class);
        Path relativePath = mock(Path.class);
        
        when(file.toPath()).thenReturn(filePath);
        when(baseDir.toPath()).thenReturn(baseDirPath);
        when(baseDirPath.relativize(filePath)).thenReturn(relativePath);
        
        assertThat(PathUtils.getRelativePath(file, baseDir), is(relativePath));
    }
}
