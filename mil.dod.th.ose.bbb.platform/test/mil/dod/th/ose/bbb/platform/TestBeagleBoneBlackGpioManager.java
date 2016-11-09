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
package mil.dod.th.ose.bbb.platform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mil.dod.th.ose.bbb.platform.BeagleBoneBlackGpioManager.GpioState;
import mil.dod.th.ose.utils.FileService;

/**
 * @author cweisenborn
 */
public class TestBeagleBoneBlackGpioManager
{
    private static final int GPIO_PIN = 5;
    private static final String GPIO_PATH = "/sys/class/gpio";
    
    private BeagleBoneBlackGpioManager m_SUT;
    @Mock private FileService m_FileService;
    @Mock private File m_GpioFile;
    @Mock private FileOutputStream m_FileOutputStream;
    @Mock private PrintStream m_PrintStream;
    
    @Before
    public void setup() throws FileNotFoundException
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new BeagleBoneBlackGpioManager();
        
        m_SUT.setFileService(m_FileService);
        
        when(m_FileService.getFile(GPIO_PATH, "gpio" + GPIO_PIN + "/value")).thenReturn(m_GpioFile);
        when(m_FileService.createFileOutputStream(m_GpioFile, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
    }
    
    @Test
    public void testSetGpioState() throws FileNotFoundException
    {
        final File gpioExport = mock(File.class);
        final File gpioDirection = mock(File.class);
        
        when(m_GpioFile.exists()).thenReturn(false);
        when(m_FileService.getFile(GPIO_PATH, "export")).thenReturn(gpioExport);
        when(m_FileService.createFileOutputStream(gpioExport, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.getFile(GPIO_PATH, "gpio" + GPIO_PIN + "/direction")).thenReturn(gpioDirection);
        when(m_FileService.createFileOutputStream(gpioDirection, false)).thenReturn(m_FileOutputStream);
        
        m_SUT.setGpioState(GPIO_PIN, GpioState.HIGH);
        
        verify(m_PrintStream).print(GPIO_PIN);
        verify(m_PrintStream).print("out");
        verify(m_PrintStream).print(1);
    }
    
    @Test
    public void testSetGpioStateExists()
    {
        when(m_GpioFile.exists()).thenReturn(true);
        
        m_SUT.setGpioState(GPIO_PIN, GpioState.LOW);
        
        verify(m_PrintStream, never()).print(GPIO_PIN);
        verify(m_PrintStream, never()).print("out");
        verify(m_PrintStream).print(0);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testSetGpioStateException() throws FileNotFoundException
    {
        final File gpioExport = mock(File.class);
        
        when(m_GpioFile.exists()).thenReturn(false);
        when(m_FileService.getFile(GPIO_PATH, "export")).thenReturn(gpioExport);
        when(m_FileService.createFileOutputStream(gpioExport, false)).thenThrow(IOException.class);
        
        try
        {
            m_SUT.setGpioState(GPIO_PIN, GpioState.HIGH);
            fail("Expected Illegal State Exception");
        }
        catch (final IllegalStateException ex)
        {
            assertThat(ex.getMessage(), 
                    equalTo("Cannot set GPIO [" + GPIO_PIN + "] to state [HIGH] as it does not exists."));
        }
    }
}
