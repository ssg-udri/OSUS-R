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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
* @author dhumeniuk
*
*/
public class TestPersistedAsset
{
    /**
     * Verify information for asset created in the first run of the integration tests.
     */
    @Test
    public void testAssetRestored() throws CommandExecutionException, IllegalArgumentException, IllegalStateException, 
        FactoryException, InterruptedException
    {
        AssetDirectoryService assetDirectoryService = 
                IntegrationTestRunner.getService(AssetDirectoryService.class);
        Asset asset = assetDirectoryService.getAssetByName("persistObsAsset");
        
        //verify the asset is activated
        assertThat(asset.getActiveStatus(), is(Asset.AssetActiveStatus.ACTIVATED));
        
        //verify that location information was persisted and set, this should happen at activation.
        //This asset was set to activate at start up so the location should be reflective of the values
        //previously assigned. Longitude should be 1, Latitude should be -2
        Command getPos = new GetPositionCommand();
        
        GetPositionResponse getPTResponse = (GetPositionResponse)asset.executeCommand(getPos);
        assertThat("Get Position Response is null", getPTResponse, is(notNullValue()));
        
        assertThat(getPTResponse.getLocation().getLongitude().getValue(), is((double)1));
        assertThat(getPTResponse.getLocation().getLatitude().getValue(), is((double)-2));
    }
}

