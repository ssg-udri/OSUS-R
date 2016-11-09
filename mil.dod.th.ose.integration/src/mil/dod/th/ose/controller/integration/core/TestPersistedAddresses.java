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
package mil.dod.th.ose.controller.integration.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.Test;

import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

/**
 * @author dhumeniuk
 *
 */
public class TestPersistedAddresses
{
    /**
     * Verify addresses created in {@link mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createAddresses()} are
     * still around.
     */
    @Test
    public void testAddressesRestored() throws InterruptedException, CCommException
    {
        // wait until address factory has been registered, at that point all addresses must be restored
        IntegrationTestRunner.assertServiceReferenceFound("exampleAddressFactory", 5000);
        
        AddressManagerService addressMgrSvc = IntegrationTestRunner.getService(AddressManagerService.class);
        List<String> addressStrings = addressMgrSvc.getAddressDescriptiveStrings();
        
        // Ensure the addresses are still in the lookup
        assertThat(addressStrings, hasItem("Example:1001"));
        assertThat(addressStrings, hasItem("Example:1002"));
        assertThat(addressStrings, hasItem("Example:1003"));
    }
}
