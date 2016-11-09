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
package mil.dod.th.core.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;

import org.junit.Test;

public class TestOperationMode
{
    /**
     * Test method to verify
     * {@link mil.dod.th.core.controller.TerraHarvestController.OperationMode#fromValue(java.lang.String)}.
     */
    @Test
    public void testFromValue()
    {
        assertThat(OperationMode.fromValue("operational"), is(OperationMode.OPERATIONAL_MODE));
        assertThat(OperationMode.fromValue("test"), is(OperationMode.TEST_MODE));

        try
        {
            OperationMode.fromValue("invalid");
            fail("Expected an illegal argument exception");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
}
