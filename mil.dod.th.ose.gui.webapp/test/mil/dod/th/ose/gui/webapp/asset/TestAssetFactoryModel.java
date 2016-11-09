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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the ability for the asset factory model to store information about asset types.
 * @author callen
 *
 */
public class TestAssetFactoryModel 
{
    private static final String PRODUCT_TYPE = "stan.the.man";
    private static final String COMMS_ICON_DFLT = "thoseIcons/default/defaultImage.png";
    
    private AssetImage m_AssetImageInterface;
    
    @Before
    public void init()
    {
        m_AssetImageInterface = new AssetImage();
    }

    /**
     * Test the default values set for an asset factory model. 
     * 
     * Verify that the FQN is returned for the name. 
     */
    @Test
    public void testDefaultValues()
    {
        AssetFactoryModel model = new AssetFactoryModel(PRODUCT_TYPE, m_AssetImageInterface);
        
        //check defaults
        assertThat(model.getFullyQualifiedAssetType(), is(PRODUCT_TYPE));
        assertThat(model.getProductName().isEmpty(), is(true));
        assertThat(model.getFactoryCaps(), is(nullValue()));
        String[] strings = model.getFullyQualifiedAssetType().split("\\.");
        assertThat(model.getSimpleType(), is(strings[strings.length - 1]));
    }
    
    /**
     * Verify the get image method works as expected
     */
    @Test
    public void testGetImage()
    {
        AssetFactoryModel model = new AssetFactoryModel(PRODUCT_TYPE, m_AssetImageInterface);
        
        //0 is used for system ID since the capabilities are already known to the factory model
        assertThat(model.getImage(), is(COMMS_ICON_DFLT));
        
        List<SensingModality> sensingMods = new ArrayList<SensingModality>();
        SensingModality modal = new SensingModality();
        sensingMods.add(modal);
        
        modal.setValue(SensingModalityEnum.ACOUSTIC);
        
        AssetCapabilities assetCap = mock(AssetCapabilities.class);
        when(assetCap.getModalities()).thenReturn(sensingMods);
        
        model.setFactoryCaps(assetCap);
        
        assertThat(model.getImage(), is("thoseIcons/sensingModality/" + 
                modal.getValue().toString().toLowerCase() + ".png"));
    }
}
