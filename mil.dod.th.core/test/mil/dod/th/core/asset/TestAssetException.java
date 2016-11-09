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
package mil.dod.th.core.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestAssetException
{
    private AssetException m_SUT;
    
    /**
     * Test method for {@link mil.dod.th.core.asset.AssetException#AssetException(java.lang.String)}.
     */
    @Test
    public void testAssetExceptionString()
    {
        m_SUT = new AssetException("test msg");
        assertThat(m_SUT.getMessage(), is("test msg"));
    }

    /**
     * Test method for {@link mil.dod.th.core.asset.AssetException#AssetException(java.lang.Throwable)}.
     */
    @Test
    public void testAssetExceptionThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new AssetException(cause);
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }

    /**
     * Test method for {@link mil.dod.th.core.asset.AssetException#AssetException
     * (java.lang.String, java.lang.Throwable)}.
     */
    @Test
    public void testAssetExceptionStringThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new AssetException("test msg", cause);
        assertThat(m_SUT.getMessage(), is("test msg"));
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
}
