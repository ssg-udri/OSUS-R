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
package mil.dod.th.ose.datastream.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.archiver.ArchiverException;
import mil.dod.th.core.archiver.ArchiverService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileAttributes;
import mil.dod.th.core.datastream.store.DateRange;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.datastream.store.ThreadStatusListener.ThreadState;
import mil.dod.th.ose.datastream.store.data.StreamArchiveMetadata;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(DataStreamStoreImpl.class)
public class TestDataStreamStoreImpl
{   
    private static final String TOP_LEVEL_DIR = "./generated/top/level/dir";
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static String DATA_SOURCE_STRING = "http://1.2.3.4:54321";
    private static int HEARTBEAT_PERIOD = 5;
    
    private DataStreamStoreImpl m_SUT;
    
    @Mock private ArchiverService m_ArchiverService;
    @Mock private DataStreamService m_DataStreamService;
    @Mock private PersistentDataStore m_PersistentDataStore;
    @Mock private StreamProfile m_StreamProfile;
    @Mock private StreamProfileAttributes m_StreamProfileAttributes;
        
    @Before
    public void setUp() throws URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new DataStreamStoreImpl();
        
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setArchiverService(m_ArchiverService);
        m_SUT.setDataStreamService(m_DataStreamService);
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        
        when(m_StreamProfile.getUuid()).thenReturn(TEST_UUID);
        when(m_StreamProfile.getConfig()).thenReturn(m_StreamProfileAttributes);
        when(m_StreamProfileAttributes.dataSource()).thenReturn(new URI(DATA_SOURCE_STRING));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamStoreConfig.CONFIG_PROP_FILESTORE_TOP_DIR, TOP_LEVEL_DIR);
        m_SUT.activate(props);
    }
    
    @SuppressWarnings("rawtypes")
    @After
    public void tearDown()
    {
        m_SUT.deactivate();
        assertThat((Map)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads"), 
                is(equalTo(Collections.EMPTY_MAP)));        
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testActivation()
    {
        assertThat((String)Whitebox.getInternalState(m_SUT, "m_FileStoreTopDir"), is(TOP_LEVEL_DIR));
        assertThat((Map)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads"), 
                is(equalTo(Collections.EMPTY_MAP)));
    }
    
    @Test
    public void testModifyConfiguration()
    {
        String otherTopLevelDir = "./generated/other/top/level/dir";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamStoreConfig.CONFIG_PROP_FILESTORE_TOP_DIR, otherTopLevelDir);
        
        m_SUT.modified(props);
        
        assertThat((String)Whitebox.getInternalState(m_SUT, "m_FileStoreTopDir"), is(otherTopLevelDir));        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEnableArchiving()
    {       
        Set<StreamProfile> streamProfiles = new HashSet<>();
        streamProfiles.add(m_StreamProfile);
        when(m_DataStreamService.getStreamProfiles()).thenReturn(streamProfiles);       
        
        m_SUT.enableArchiving(m_StreamProfile, true, HEARTBEAT_PERIOD, 0);
        
        Map<StreamProfile, ClientResponseThread> clientResponseThreads =
                (Map<StreamProfile, ClientResponseThread>)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads");
        
        assertThat(clientResponseThreads.containsKey(m_StreamProfile), is(true));       
    }
    
    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testEnableArchivingForNonExistentStreamProfile()
    {
        when(m_DataStreamService.getStreamProfiles()).thenReturn(new HashSet<StreamProfile>());
        
        m_SUT.enableArchiving(m_StreamProfile, true, HEARTBEAT_PERIOD, 0);
        
        Map<StreamProfile, ClientResponseThread> clientResponseThreads =
                (Map<StreamProfile, ClientResponseThread>)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads");
        
        assertThat(clientResponseThreads.containsKey(m_StreamProfile), is(false));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDisableArchiving()
    {
        Set<StreamProfile> streamProfiles = new HashSet<>();
        streamProfiles.add(m_StreamProfile);
        when(m_DataStreamService.getStreamProfiles()).thenReturn(streamProfiles);       
        
        ClientResponseThread thread = mock(ClientResponseThread.class);
        
        Map<StreamProfile, ClientResponseThread> threadsBefore =  new HashMap<>();
        threadsBefore.put(m_StreamProfile, thread);
        Whitebox.setInternalState(m_SUT, "m_ClientResponseThreads", threadsBefore);
                
        m_SUT.disableArchiving(m_StreamProfile);
                
        Map<StreamProfile, ClientResponseThread> threadsAfter =
                (Map<StreamProfile, ClientResponseThread>)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads");
        
        assertThat(threadsAfter.containsKey(m_StreamProfile), is(false));
    }
    
    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDisableArchivingForNonExistentStreamProfile()
    {
        when(m_DataStreamService.getStreamProfiles()).thenReturn(new HashSet<StreamProfile>());
        
        m_SUT.disableArchiving(m_StreamProfile);
        
        Map<StreamProfile, ClientResponseThread> clientResponseThreads =
                (Map<StreamProfile, ClientResponseThread>)Whitebox.getInternalState(m_SUT, "m_ClientResponseThreads");
        
        assertThat(clientResponseThreads.containsKey(m_StreamProfile), is(false));
    }
    
    @Test
    public void testClientAck()
    {
        ClientResponseThread thread = mock(ClientResponseThread.class);
        Map<StreamProfile, ClientResponseThread> threadMap = new HashMap<>();
        threadMap.put(m_StreamProfile, thread);
        Whitebox.setInternalState(m_SUT, "m_ClientResponseThreads", threadMap);
        
        m_SUT.clientAck(m_StreamProfile);
        
        verify(thread).reset();
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetArchivePeriods()
    {
        PersistentData pd1 = mock(PersistentData.class);
        PersistentData pd2 = mock(PersistentData.class);
        PersistentData pd3 = mock(PersistentData.class);
        
        StreamArchiveMetadata md1 = mock(StreamArchiveMetadata.class);
        StreamArchiveMetadata md2 = mock(StreamArchiveMetadata.class);
        StreamArchiveMetadata md3 = mock(StreamArchiveMetadata.class);
        
        long start1 = 100L;
        long stop1 = 101L;
        long start2 = 200L;
        long stop2 = 201L;
        long start3 = 300L;
        long stop3 = 301L;
        
        when(md1.getStartTimestamp()).thenReturn(start1);
        when(md1.getStopTimestamp()).thenReturn(stop1);
        when(md2.getStartTimestamp()).thenReturn(start2);
        when(md2.getStopTimestamp()).thenReturn(stop2);
        when(md3.getStartTimestamp()).thenReturn(start3);
        when(md3.getStopTimestamp()).thenReturn(stop3);
        
        when(pd1.getEntity()).thenReturn(md1);
        when(pd2.getEntity()).thenReturn(md2);
        when(pd3.getEntity()).thenReturn(md3);
        
        Collection<PersistentData> records = new ArrayList<>();
        records.add(pd1);
        records.add(pd2);
        records.add(pd3);
        
        when(m_PersistentDataStore.query((Class)any(), anyString())).thenReturn(records);
        
        List<DateRange> ranges = m_SUT.getArchivePeriods(m_StreamProfile);
        
        assertThat(ranges.size(), is(3));
        
        assertThat(ranges.get(0).getStartTime(), is(start1));
        assertThat(ranges.get(0).getStopTime(), is(stop1));
        assertThat(ranges.get(1).getStartTime(), is(start2));
        assertThat(ranges.get(1).getStopTime(), is(stop2));
        assertThat(ranges.get(2).getStartTime(), is(start3));
        assertThat(ranges.get(2).getStopTime(), is(stop3));
        
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetArchiveStream() throws Exception
    {
        PersistentData pd1 = mock(PersistentData.class);
        PersistentData pd2 = mock(PersistentData.class);
        PersistentData pd3 = mock(PersistentData.class);
        
        StreamArchiveMetadata md1 = mock(StreamArchiveMetadata.class);
        StreamArchiveMetadata md2 = mock(StreamArchiveMetadata.class);
        StreamArchiveMetadata md3 = mock(StreamArchiveMetadata.class);
        
        BufferedInputStream buffIn = mock(BufferedInputStream.class);
        SequenceInputStream seqIn = mock(SequenceInputStream.class);
        
        long start1 = 100L;
        long stop1 = 101L;
        long start2 = 200L;
        long stop2 = 201L;
        long start3 = 300L;
        long stop3 = 301L;
        
        URL filePath1 = mock(URL.class);
        URL filePath2 = mock(URL.class);
        URL filePath3 = mock(URL.class);
        
        when(filePath1.openStream()).thenReturn(mock(InputStream.class));
        when(filePath2.openStream()).thenReturn(mock(InputStream.class));
        when(filePath3.openStream()).thenReturn(mock(InputStream.class));
        
        when(md1.getStartTimestamp()).thenReturn(start1);
        when(md1.getStopTimestamp()).thenReturn(stop1);
        when(md1.getFilePath()).thenReturn(filePath1);
        when(md2.getStartTimestamp()).thenReturn(start2);
        when(md2.getStopTimestamp()).thenReturn(stop2);
        when(md2.getFilePath()).thenReturn(filePath2);
        when(md3.getStartTimestamp()).thenReturn(start3);
        when(md3.getStopTimestamp()).thenReturn(stop3);
        when(md3.getFilePath()).thenReturn(filePath3);
        
        when(pd1.getEntity()).thenReturn(md1);
        when(pd2.getEntity()).thenReturn(md2);
        when(pd3.getEntity()).thenReturn(md3);
        
        Collection<PersistentData> records = new ArrayList<>();
        records.add(pd1);
        records.add(pd2);
        records.add(pd3);
        
        when(m_PersistentDataStore.query((Class)any(), anyString())).thenReturn(records);
        
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(buffIn);
        whenNew(SequenceInputStream.class).withAnyArguments().thenReturn(seqIn);
        
        InputStream inStream = m_SUT.getArchiveStream(m_StreamProfile, 
                new DateRange(new Date(start2), new Date(stop2)));
        
        assertThat(inStream, is(not(nullValue())));       
    }
    
    @Test
    public void testNotifyObserverWithFinishedState() throws IllegalStateException, ArchiverException, 
        URISyntaxException
    {
        ClientResponseThread thread = mock(ClientResponseThread.class);
        when(thread.isUseSourceBitrate()).thenReturn(true);
        
        Map<StreamProfile, ClientResponseThread> threadMap = new HashMap<>();
        threadMap.put(m_StreamProfile, thread);
        Whitebox.setInternalState(m_SUT, "m_ClientResponseThreads", threadMap);
        
        m_SUT.notifyObserver(m_StreamProfile, ThreadState.FINISHED);
        
        verify(m_ArchiverService).start(anyString(), any(URI.class), 
                anyString());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testNotifyObserverWithInterruptedState()
    {
        ClientResponseThread thread = mock(ClientResponseThread.class);
        when(thread.isUseSourceBitrate()).thenReturn(true);
        
        Map<StreamProfile, ClientResponseThread> threadMap = new HashMap<>();
        threadMap.put(m_StreamProfile, thread);
        Whitebox.setInternalState(m_SUT, "m_ClientResponseThreads", threadMap);
        
        m_SUT.notifyObserver(m_StreamProfile, ThreadState.INTERRUPTED);
        
        assertThat((Map<StreamProfile, ClientResponseThread>)Whitebox.getInternalState(m_SUT, 
                "m_ClientResponseThreads"), not(hasKey(m_StreamProfile)));
    }
}
