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

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author jmiller
 *
 */
public class TestDataStreamServiceCommands
{
    private DataStreamServiceCommands m_SUT;

    @Mock private DataStreamService m_DataStreamService;
    @Mock private AssetDirectoryService m_AssetDirectoryService;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new DataStreamServiceCommands();
        
        m_SUT.setDataStreamService(m_DataStreamService);
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);        
    }
    
    @Test
    public void testGetStreamProfiles()
    {
        Set<StreamProfile> profiles = new HashSet<>();
        
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        profiles.add(profile1);
        profiles.add(profile2);
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        assertThat(m_SUT.getStreamProfiles(), is(equalTo(profiles)));       
    }
    
    @Test
    public void testGetStreamProfileByName()
    {
        Set<StreamProfile> profiles = new HashSet<>();
        
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        when(profile1.getName()).thenReturn("profile1");
        when(profile2.getName()).thenReturn("profile2");

        profiles.add(profile1);
        profiles.add(profile2);
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        assertThat(m_SUT.getStreamProfileByName("profile1"), is(equalTo(profile1)));
        assertThat(m_SUT.getStreamProfileByName("profile2"), is(equalTo(profile2)));       
    }
    
    @Test
    public void testGetStreamProfilesByAssetName()
    {
        Asset asset1 = mock(Asset.class);
        Asset asset2 = mock(Asset.class);
        
        when(m_AssetDirectoryService.getAssetByName("asset1")).thenReturn(asset1);
        when(m_AssetDirectoryService.getAssetByName("asset2")).thenReturn(asset2);
        
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        StreamProfile profile3 = mock(StreamProfile.class);
        
        Set<StreamProfile> profilesForAsset1 = new HashSet<>();
        Set<StreamProfile> profilesForAsset2 = new HashSet<>();
        
        profilesForAsset1.add(profile1);
        profilesForAsset1.add(profile3);
        
        profilesForAsset2.add(profile2);
        
        when(m_DataStreamService.getStreamProfiles(asset1)).thenReturn(profilesForAsset1);
        when(m_DataStreamService.getStreamProfiles(asset2)).thenReturn(profilesForAsset2);
        
        assertThat(m_SUT.getStreamProfilesByAssetName("asset1"), is(equalTo(profilesForAsset1)));
        assertThat(m_SUT.getStreamProfilesByAssetName("asset2"), is(equalTo(profilesForAsset2)));       
    }
}
