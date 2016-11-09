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
package mil.dod.th.ose.utils.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.Test;

/**
 * Test the file service.
 * @author allenchl
 *
 */
public class TestFileServiceImpl
{
    private FileServiceImpl m_SUT = new FileServiceImpl();
    
    /**
     * Test getting a file.
     */
    @Test
    public void testGetFile()
    {
        File file = m_SUT.getFile("dir");
        //get the file reference
        assertThat(file.getPath(), is("dir"));
    }
    
   /**
     * Test verifying that a file exists.
     */
    @Test
    public void testDoesFileExist()
    {
        File testFile = mock(File.class);
        when(testFile.exists()).thenReturn(true);
        assertThat(m_SUT.doesFileExist(testFile), is(true));
        
        when(testFile.exists()).thenReturn(false);
        assertThat(m_SUT.doesFileExist(testFile), is(false));
    }
}
