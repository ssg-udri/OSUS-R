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
package mil.dod.th.core.ccomm.physical;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestExceptions
{
    @Test
    public void testCoreHardwareException()
    {
        try
        {
            try
            {
                throw new IllegalAccessException("test exception");
            }
            catch (final Exception e)
            {
                throw new PhysicalLinkException("core hardware message", e);
            }
        }
        catch (final PhysicalLinkException e)
        {
            assertEquals("core hardware message", e.getMessage());
            if (!(e.getCause() instanceof IllegalAccessException))
            {
                fail("Cause is not expected exception");
            }
        }
    }
    
    @Test
    public void testPropertyValueNotSupportedException()
    {
        try
        {
            throw new PropertyValueNotSupportedException("baudRate", 2);
        }
        catch (final PropertyValueNotSupportedException e)
        {
            assertEquals("The value '2' for the baudRate property is not supported", e.getMessage());
        }
    }   
    
    @Test
    public void testTimeoutException()
    {
        try
        {
            throw new TimeoutException("some message");
        }
        catch (final TimeoutException e)
        {
            assertEquals("some message", e.getMessage());
        }
    }
}
