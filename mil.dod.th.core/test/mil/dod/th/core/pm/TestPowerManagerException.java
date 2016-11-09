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
package mil.dod.th.core.pm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestPowerManagerException
{
    @Test
    public void testPowerManagerException()
    {
        Exception e = new Exception();
        PowerManagerException sut = new PowerManagerException(e);
        assertThat(sut.getCause(), is((Throwable)e));
    }
}
