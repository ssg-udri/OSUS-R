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
package mil.dod.th.ose.archiver.vlc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.archiver.ArchiverException;
import mil.dod.th.core.log.LoggingService;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * Test class for VlcArchiver, an implementation of {@link mil.dod.th.core.archiver.ArchiverService}.
 * 
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VlcUtils.class, NativeLibraryLoader.class})
public class TestVlcArchiver
{
    private VlcArchiver m_SUT;
    private URI m_SourceStream;
    private String m_OutFilePath;
    final private String m_ProcessId = "PROCESS.ID";
    
    @Mock private HeadlessMediaPlayer m_MediaPlayer;
    @Mock private LoggingService m_Logging;
    
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        PowerMockito.spy(VlcUtils.class);
        PowerMockito.doReturn(m_MediaPlayer).when(VlcUtils.class, "newPlayer");
        when(m_MediaPlayer.playMedia(anyString(), (String)anyVararg())).thenReturn(true);
        
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class, "load");
        
        m_SUT = new VlcArchiver();
        m_SUT.setLoggingService(m_Logging);
        
        m_SourceStream = new URI("rtsp://195.168.1.254:554/mpeg4/media.amp");
        m_OutFilePath = "/top/level/dir/video-file";
        
    }
    
    /**
     * Verify that an archiving process can be started.
     * 
     * @throws ArchiverException
     *      if the archiver instance produces an error.
     */
    @Test 
    public void testStart() throws ArchiverException
    {
        m_SUT.activate();
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_OutFilePath);
        
        verify(m_MediaPlayer).addMediaPlayerEventListener(Mockito.any(MediaPlayerEventListener.class));
        verify(m_MediaPlayer).playMedia(anyString(), (String)anyVararg());
        
        assertThat(m_SUT.getActiveProcessIds(), hasItem(m_ProcessId));
        
        m_SUT.deactivate();
        verify(m_MediaPlayer).stop();
        verify(m_MediaPlayer).release();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testStartWithExistingId() throws ArchiverException
    {
        Map<String, HeadlessMediaPlayer> processMap = new HashMap<>();
        processMap.put(m_ProcessId, m_MediaPlayer);
        Whitebox.setInternalState(m_SUT, "m_ProcessMap", processMap);
        
        //Try to start archiving process using the same ID
        m_SUT.start(m_ProcessId, m_SourceStream, m_OutFilePath);        
        
    }
    
    @Test(expected = ArchiverException.class)
    public void testStartWithPlayerError() throws ArchiverException
    {
        m_SUT.activate();
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        when(m_MediaPlayer.playMedia(anyString(), (String)anyVararg())).thenReturn(false);
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_OutFilePath);
    }
    
    /**
     * Verify that an archiving process can be started and subsequently stopped.
     * 
     * @throws ArchiverException
     *      if the archiver instance produces an error
     */
    @Test
    public void testStop() throws ArchiverException
    {
        m_SUT.activate();
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_OutFilePath);
        
        verify(m_MediaPlayer).addMediaPlayerEventListener(Mockito.any(MediaPlayerEventListener.class));
        verify(m_MediaPlayer).playMedia(anyString(), (String)anyVararg());
        assertThat(m_SUT.getActiveProcessIds(), hasItem(m_ProcessId));
        
        //Stop the archiver process
        m_SUT.stop(m_ProcessId);
        
        assertThat(m_SUT.getActiveProcessIds(), not(hasItem(m_ProcessId)));
    }
    
    /**
     * Verify that an exception is thrown if {@link mil.dod.th.core.archiver.ArchiverService#stop(String)}
     * is called for a process that was never started.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStopWithoutExistingId()
    {
        final String bogusProcessId = "BOGUS.PROCESS.ID";
        
        m_SUT.activate();
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        m_SUT.stop(bogusProcessId);
        
    }
    
    @Test
    public void testPlayingEvent()
    {
        m_SUT.playingEvent("LISTENER.ID");
        verify(m_Logging).info(anyString(), anyVararg());
    }
    
    @Test
    public void testErrorEvent()
    {
        Map<String, HeadlessMediaPlayer> processMap = new HashMap<>();
        processMap.put(m_ProcessId, m_MediaPlayer);
        Whitebox.setInternalState(m_SUT, "m_ProcessMap", processMap);
        assertThat(m_SUT.getActiveProcessIds(), hasItem(m_ProcessId));
        
        m_SUT.errorEvent(m_ProcessId);
        verify(m_Logging).error(anyString(), anyVararg());
        assertThat(m_SUT.getActiveProcessIds(), not(hasItem(m_ProcessId)));
        
        //Test when process ID is not already in the set
        processMap = new HashMap<>();
        Whitebox.setInternalState(m_SUT, "m_ProcessMap", processMap);
        m_SUT.errorEvent(m_ProcessId);
        verify(m_Logging, times(2)).error(anyString(), anyVararg());
    }
    
    @Test
    public void testStoppedEvent()
    {
        m_SUT.stoppedEvent(m_ProcessId);
        verify(m_Logging).info(anyString(), anyVararg());
    }
    

}
