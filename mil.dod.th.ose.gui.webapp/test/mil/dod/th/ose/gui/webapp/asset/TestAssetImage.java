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
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the asset image class.
 * @author matt
 *
 */
public class TestAssetImage
{
    private static final String ASSET_ICON_DFLT = "thoseIcons/default/defaultImage.png";
    private static final String ASSET_ICON_ACOUSTIC = "thoseIcons/sensingModality/acoustic.png";
    
    private AssetImage m_SUT;
    
    @Before
    public void setUp()
    {
        m_SUT = new AssetImage();
    }
    
    /**
     * Verify the getImage method works as intended.
     */
    @Test
    public void testGetImage()
    {
        AssetCapabilities assetCaps = new AssetCapabilities();
        DigitalMedia digMedia = new DigitalMedia();
        assetCaps.setPrimaryImage(digMedia);
        
        //verify default image is returned if no capabilities for the asset
        assertThat(m_SUT.getImage(assetCaps), is(ASSET_ICON_DFLT));
        
        List<SensingModality> sensingList = new ArrayList<SensingModality>();
        SensingModality imageModality = new SensingModality();
        imageModality.setValue(SensingModalityEnum.ACOUSTIC);
        
        AssetCapabilities assetCapsSensing = mock(AssetCapabilities.class);
        when(assetCapsSensing.getModalities()).thenReturn(sensingList);
        
        //verify that if no modalities are returned the default image is returned
        assertThat(m_SUT.getImage(assetCapsSensing), is(ASSET_ICON_DFLT));
        
        sensingList.add(imageModality);
        
        //verify if no primary image and capabilities have sensing modalities the first one is returned
        //for the image
        assertThat(m_SUT.getImage(assetCapsSensing), is(ASSET_ICON_ACOUSTIC));
    }
}
