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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link AssetImageService}.
 * @author allenchl
 *
 */
public class TestAssetImageService
{
    private static int SYSTEM_ID = 2;
    
    private AssetImageService m_SUT;
    private ActiveController m_ActiveController;
    private AssetMgr m_AssetManager;
    
    @Before
    public void setup()
    {
        m_SUT = new AssetImageService();
        m_ActiveController = mock(ActiveController.class);
        m_AssetManager = mock(AssetMgr.class);
        
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setAssetMgr(m_AssetManager);
    }
    
    /**
     * Verify tryGetImage, will return an image string, from the asset model,
     * if the asset uuid is known.
     */
    @Test
    public void testTryGetImageKnownModel()
    {
        ControllerModel model = mock(ControllerModel.class);
        when(m_ActiveController.getActiveController()).thenReturn(model);
        when(model.getId()).thenReturn(SYSTEM_ID);
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        
        UUID assetUuid = UUID.randomUUID();
        AssetModel assetModel = mock(AssetModel.class);
        when(assetModel.getImage()).thenReturn("pic");
        when(m_AssetManager.getAssetModelByUuid(assetUuid, SYSTEM_ID)).
            thenReturn(assetModel);
        
        String image = m_SUT.tryGetAssetImage(assetUuid);
        
        assertThat(image, is("pic"));
    }
    
    /**
     * Verify that tryGetImage, will return an image string if the asset model is not
     * known.
     */
    @Test
    public void testTryGetImageUnknownModel()
    {
        ControllerModel model = mock(ControllerModel.class);
        when(m_ActiveController.getActiveController()).thenReturn(model);
        when(model.getId()).thenReturn(SYSTEM_ID);
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        
        UUID assetUuid = UUID.randomUUID();
        when(m_AssetManager.getAssetModelByUuid(assetUuid, SYSTEM_ID)).
            thenReturn(null);

        String image = m_SUT.tryGetAssetImage(assetUuid);
        
        assertThat(image, is("thoseIcons/default/defaultImage.png"));
    }
    
    /**
     * Verify default image string is returned if there is not an active controller set.
     */
    @Test
    public void testTryGetImageNoActiveController()
    {
        when(m_ActiveController.isActiveControllerSet()).thenReturn(false);

        String image = m_SUT.tryGetAssetImage(UUID.randomUUID());
        
        assertThat(image, is("thoseIcons/default/defaultImage.png"));
    }
}
