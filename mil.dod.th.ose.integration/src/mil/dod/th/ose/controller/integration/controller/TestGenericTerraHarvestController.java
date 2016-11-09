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
import static org.hamcrest.Matchers.notNullValue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * Test the {@link TerraHarvestController} service in case where nothing is overridden.
 * 
 * @author frenchpd
 *
 */
public class TestGenericTerraHarvestController
{
    /**
     * Test the default name in the file.
     */
    @Test
    public void testDefaultName()
    {
        TerraHarvestController controller = IntegrationTestRunner.getService(TerraHarvestController.class);
        
        assertThat(controller.getName(), is("controller-app-generic"));
    }
    
    /**
     * Test for that the XML unmarshal service correctly reads and translates a capabilities XML file.
     */
    @Test
    public void testGetCapabilities() throws UnmarshalException, IOException
    {
        TerraHarvestController terraHarvestController = IntegrationTestRunner.getService(TerraHarvestController.class);

        assertThat(terraHarvestController,is(notNullValue()));

        ControllerCapabilities caps = terraHarvestController.getCapabilities();
        
        assertThat(caps, is(notNullValue()));
        assertThat(caps.getManufacturer(), is("Unknown"));
        assertThat(caps.getDescription(), is("Example Controller"));
        assertThat(caps.getProductName(), is("Example Controller"));
        
        // make sure that values are not even set
        assertThat(caps.isSetIdOverridden(), is(false));
        assertThat(caps.isSetNameOverridden(), is(false));
        assertThat(caps.isSetVersionOverridden(), is(false));
        assertThat(caps.isSetBuildInfoOverridden(), is(false));

        assertThat(caps.isSetBatteryAmpHourReported(), is(false));
        assertThat(caps.isSetLowPowerModeSupported(), is(false));
        assertThat(caps.isSetVoltageReported(), is(false));
        
        BufferedImage img= ImageIO.read(new ByteArrayInputStream(caps.getPrimaryImage().getValue()));
        int imHeight = img.getHeight();
        int imWidth = img.getWidth();
        assertThat(imHeight, is(128));
        assertThat(imWidth, is(128));        
    }
}
