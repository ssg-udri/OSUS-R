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

package mil.dod.th.ose.linux.physical;

import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;
import java.io.IOException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.linux.gnu_c.FileOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("mil.dod.th.ose.linux.gnu_c.FileOperationsConstants")
@PrepareForTest(FileOperations.class)
public class TestSerialPortOutputStream
{
    private SerialPortOutputStream m_SUT;

    @Before
    public void setUp() throws Exception
    {
        PowerMockito.mockStatic(FileOperations.class);
        
        m_SUT = new SerialPortOutputStream(10);
    }
    
    /**
     * Verify write calls on {@link FileOperations} properly.
     */
    @Test
    public void testWrite() throws IOException, FactoryException
    {
        m_SUT.write(5);
        
        PowerMockito.verifyStatic();
        FileOperations.writeBuffer(10, new byte[] {5}, 0, 1);
    }
    
    /**
     * Verify write throws exception if failure.
     */
    @Test
    public void testWriteFailure() throws IOException, FactoryException
    {
        when(FileOperations.writeBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt())).thenReturn(-1);

        try
        {
            m_SUT.write(5);
            fail("Expecting exception");
        }
        catch (IOException e)
        {
            
        }
    }
    
    /**
     * Verify write calls on {@link FileOperations} properly.
     */
    @Test
    public void testWriteBuffer() throws IOException, FactoryException
    {
        // mock to write out 5 bytes at a time
        when(FileOperations.writeBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt())).thenReturn(5);

        byte[] buffer = new byte[10];
        m_SUT.write(buffer, 5, 20);
        
        PowerMockito.verifyStatic(); // should take 4 writeBuffer calls, 5 bytes each
        FileOperations.writeBuffer(10, buffer, 5, 20);
        FileOperations.writeBuffer(10, buffer, 10, 15);
        FileOperations.writeBuffer(10, buffer, 15, 10);
        FileOperations.writeBuffer(10, buffer, 20, 5);
    }
    
    /**
     * Verify write throws exception if failure.
     */
    @Test
    public void testWriteBufferFailure() throws IOException, FactoryException
    {
        when(FileOperations.writeBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt())).thenReturn(-1);

        byte[] buffer = new byte[10];
        try
        {
            m_SUT.write(buffer, 0, 10);
            fail("Expecting exception");
        }
        catch (IOException e)
        {
            
        }
    }
}
