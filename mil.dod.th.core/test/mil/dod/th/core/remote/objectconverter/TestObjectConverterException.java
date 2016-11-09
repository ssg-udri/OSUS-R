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
package mil.dod.th.core.remote.objectconverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * Unit test for the ObjectConverterException class.
 * 
 * @author cweisenborn
 */
public class TestObjectConverterException
{
    private ObjectConverterException m_SUT;
    
    /**
     * Tests the ObjectConverterException constructor that accepts a string as a parameter.
     */
    @Test
    public void testObjectConverterExceptionString()
    {
        m_SUT = new ObjectConverterException("Test Message!");
        assertThat(m_SUT.getMessage(), equalTo("Test Message!"));
    }
    
    /**
     * Tests the ObjectConverterException constructor that accepts a throwable as a parameter.
     */
    @Test
    public void testObjectConverterExceptionThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new ObjectConverterException(cause);
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
    
    /**
     * Tests the ObjectConverterException constructor that accepts a string and a throwable as parameters.
     */
    @Test
    public void testObjectConverterExceptionThrowableString()
    {
        Exception cause = new Exception();
        m_SUT = new ObjectConverterException("Test Message!", cause);
        assertThat(m_SUT.getMessage(), equalTo("Test Message!"));
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
}
