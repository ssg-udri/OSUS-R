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
package mil.dod.th.ose.shell;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;

/**
 * @author cweisenborn
 */
public class TestAssetCommands
{
    private AssetCommands m_SUT;
    private AssetDirectoryService m_AssetDirectoryService;
    private Asset m_TestAsset;
    
    @Before
    public void setup()
    {
        m_SUT = new AssetCommands();
        
        m_AssetDirectoryService = mock(AssetDirectoryService.class);
        m_TestAsset = mock(Asset.class);
        
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);
    }
    
    @Test
    public void testGetAssets()
    {
        Set<Asset> assets = new HashSet<Asset>();
        assets.add(m_TestAsset);
        
        when(m_AssetDirectoryService.getAssets()).thenReturn(assets);
        
        assertThat(m_SUT.getAssets(), equalTo(assets));
    }
    
    @Test
    public void testGetAssetByName()
    {   
        when(m_AssetDirectoryService.getAssetByName(anyString())).thenReturn(m_TestAsset);
        
        assertThat(m_SUT.getAssetByName("test"), equalTo(m_TestAsset));
        verify(m_AssetDirectoryService).getAssetByName(anyString());
    }
    
    @Test
    public void testCreateAsset() throws IllegalArgumentException, AssetException
    {   
        when(m_AssetDirectoryService.createAsset(anyString())).thenReturn(m_TestAsset);
        
        assertThat(m_SUT.createAsset("test"), equalTo(m_TestAsset));
    }
    
    @Test
    public void testScanForNewAssets()
    {
        m_SUT.scanForNewAssets();
        
        verify(m_AssetDirectoryService).scanForNewAssets();
    }
    
    @Test
    public void testAssetActiveStatus()
    {
        Set<Asset> assets = new HashSet<Asset>();
        assets.add(m_TestAsset);

        when(m_TestAsset.getName()).thenReturn("Test Asset");
        AssetAttributes config = mock(AssetAttributes.class);
        when(config.activateOnStartup()).thenReturn(false);
        when(m_AssetDirectoryService.getAssets()).thenReturn(assets);
        when(m_TestAsset.getConfig()).thenReturn(config);
        when(m_TestAsset.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        
        String test = "Test Asset"
                + ":\n" 
                + "\tActivate On Startup = false" 
                + "\n\tActive Status = DEACTIVATED" 
                + '\n';
        
        assertThat(m_SUT.assetActiveStatus(), equalTo(test));
        
        verify(m_TestAsset).getName();
        verify(m_AssetDirectoryService).getAssets();
        verify(m_TestAsset).getConfig();
        verify(config).activateOnStartup();
        verify(m_TestAsset).getActiveStatus();
    }
}
