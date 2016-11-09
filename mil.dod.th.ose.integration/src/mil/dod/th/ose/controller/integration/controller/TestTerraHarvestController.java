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
package mil.dod.th.ose.controller.integration.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * Test the core functionality of the {@link TerraHarvestController} service.
 * 
 * Additional testing is done in {@link TestGenericTerraHarvestController} that does not apply to all targets.
 * 
 * @author frenchpd
 *
 */
public class TestTerraHarvestController
{
    /**
     * Test the operation mode defaults to test, and can be set to operation mode.
     */
    @Test
    public void testGetOperationMode() throws UnmarshalException
    {
        //verify default of test mode
        TerraHarvestController controller = IntegrationTestRunner.getService(TerraHarvestController.class);
        assertThat(controller.getOperationMode(), is(OperationMode.TEST_MODE));

        //set to operational mode
        controller.setOperationMode(OperationMode.OPERATIONAL_MODE);
        assertThat(controller.getOperationMode(), is(OperationMode.OPERATIONAL_MODE));
        
        //put it back to default
        controller.setOperationMode(OperationMode.TEST_MODE);
        assertThat(controller.getOperationMode(), is(OperationMode.TEST_MODE));
    }
    
    /**
     * Test the default ID in the file.
     */
    @Test
    public void testDefaultId()
    {
        TerraHarvestController controller = IntegrationTestRunner.getService(TerraHarvestController.class);
        
        assertThat(controller.getId(), is(0));
    }
}
