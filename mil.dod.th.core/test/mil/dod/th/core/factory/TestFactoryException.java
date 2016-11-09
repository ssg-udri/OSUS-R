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
package mil.dod.th.core.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestFactoryException
{
    private FactoryException m_SUT;
    
    /**
     * Test method for {@link mil.dod.th.core.factory.FactoryException#FactoryException(java.lang.String)}.
     */
    @Test
    public void testFactoryExceptionString()
    {
        m_SUT = new FactoryException("test msg");
        assertThat(m_SUT.getMessage(), is("test msg"));
    }

    /**
     * Test method for {@link mil.dod.th.core.factory.FactoryException#FactoryException(java.lang.Throwable)}.
     */
    @Test
    public void testFactoryExceptionThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new FactoryException(cause);
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }

    /**
     * Test method for {@link mil.dod.th.core.factory.FactoryException#FactoryException(java.lang.String, 
     * java.lang.Throwable)}
     */
    @Test
    public void testFactoryExceptionStringThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new FactoryException("test msg", cause);
        assertThat(m_SUT.getMessage(), is("test msg"));
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
}
