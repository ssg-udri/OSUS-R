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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test the property retriever implementation.
 * @author callen
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertyRetrieverImpl.class})
public class TestPropertyRetrieverImpl
{
    private PropertyRetrieverImpl m_SUT;
    
    @Before
    public void setup()
    {
        //set up the implementation class
        m_SUT = new PropertyRetrieverImpl();
    }
    
    /**
     * Test that a given URL can be connected to via a URL connection, and that a properties object is returned.
     * Verify that a {@link Properties} object is returned.
     * Verify that the input stream is closed after properties are loaded.
     */
    @Test
    public void testGetProperty() throws IOException
    {
        //URL connection and input stream mocking
        URL url = PowerMockito.mock(URL.class);
        URLConnection connection = PowerMockito.mock(URLConnection.class);
        InputStream is = mock(InputStream.class);
        when(url.openConnection()).thenReturn(connection);
        when(connection.getInputStream()).thenReturn(is);
        
        //pass the url
        Properties props = m_SUT.getPropertiesFromUrl(url);
        
        //verify that properties object is not null
        assertThat(props, is(notNullValue()));
        
        //verify that the input stream is closed
        verify(is).close();
    }
    
    /**
     * Test that for a given file directory and file name a valid properties object is returned.
     */
    @Test
    public void testGetPropertyFromFile() throws Exception
    {
        String fileDir = "dir";
        String fileName = "test.properties";
        File mockedFile = mock(File.class);
        FileInputStream inputStream = mock(FileInputStream.class);
        
        PowerMockito.whenNew(File.class).withArguments(fileDir, fileName).thenReturn(mockedFile);
        PowerMockito.whenNew(FileInputStream.class).withArguments(mockedFile).thenReturn(inputStream);
        
        Properties props = m_SUT.getPropertiesFromFile(fileDir, fileName);
        
        assertThat(props, is(notNullValue()));
        
        verify(inputStream).close();
    }
}
