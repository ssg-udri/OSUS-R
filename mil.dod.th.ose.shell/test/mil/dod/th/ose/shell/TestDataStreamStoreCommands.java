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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.core.datastream.store.DateRange;

/**
 * @author jmiller
 *
 */
public class TestDataStreamStoreCommands
{
    private DataStreamStoreCommands m_SUT;
    
    @Mock private DataStreamService m_DataStreamService;
    @Mock private DataStreamStore m_DataStreamStore;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new DataStreamStoreCommands();
        
        m_SUT.setDataStreamService(m_DataStreamService);
        m_SUT.setDataStreamStore(m_DataStreamStore);
    }
    
    @Test
    public void testEnableArchiving()
    {               
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        when(profile1.getName()).thenReturn("profile1");
        when(profile2.getName()).thenReturn("profile2"); 
        
        Set<StreamProfile> profiles = new HashSet<>();
        profiles.add(profile1);
        profiles.add(profile2);        
               
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        m_SUT.enableArchiving("profile1", true, 30, 0);
        
        verify(m_DataStreamStore).enableArchiving(profile1, true, 30, 0);
    }
    
    @Test
    public void testDisableArchiving()
    {
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        when(profile1.getName()).thenReturn("profile1");
        when(profile2.getName()).thenReturn("profile2"); 
        
        Set<StreamProfile> profiles = new HashSet<>();
        profiles.add(profile1);
        profiles.add(profile2);        
               
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        m_SUT.disableArchiving("profile2");
        
        verify(m_DataStreamStore).disableArchiving(profile2);
    }
    
    @Test
    public void testClientAck()
    {
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        when(profile1.getName()).thenReturn("profile1");
        when(profile2.getName()).thenReturn("profile2"); 
        
        Set<StreamProfile> profiles = new HashSet<>();
        profiles.add(profile1);
        profiles.add(profile2);        
               
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        m_SUT.clientAck("profile1");
        
        verify(m_DataStreamStore).clientAck(profile1);
    }
    
    @Test
    public void testGetArchivePeriods()
    {
        StreamProfile profile1 = mock(StreamProfile.class);
        StreamProfile profile2 = mock(StreamProfile.class);
        
        when(profile1.getName()).thenReturn("profile1");
        when(profile2.getName()).thenReturn("profile2"); 
        
        Set<StreamProfile> profiles = new HashSet<>();
        profiles.add(profile1);
        profiles.add(profile2);        
               
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        List<DateRange> dateRanges = new ArrayList<>();
        dateRanges.add(new DateRange(1000000, 1000100));
        dateRanges.add(new DateRange(2000000, 2000200));
        dateRanges.add(new DateRange(3000000, 3000300));
        
        when(m_DataStreamStore.getArchivePeriods(profile2)).thenReturn(dateRanges);
        
        List<Date> dates = m_SUT.getArchivePeriods("profile2");
        
        assertThat(dates.size(), is(6));
        assertThat((int)dates.get(0).getTime(), is(1000000));
        assertThat((int)dates.get(1).getTime(), is(1000100));
        assertThat((int)dates.get(2).getTime(), is(2000000));
        assertThat((int)dates.get(3).getTime(), is(2000200));
        assertThat((int)dates.get(4).getTime(), is(3000000));
        assertThat((int)dates.get(5).getTime(), is(3000300));         
    }
}
