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
package mil.dod.th.ose.transcoder.vlc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.transcoder.TranscoderException;
import mil.dod.th.core.transcoder.TranscoderService;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * Test class for VlcTranscoder, an implementation of {@link mil.dod.th.core.transcoder.TranscoderService}.
 * 
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VlcUtils.class, NativeLibraryLoader.class})
public class TestVlcTranscoder
{
    private VlcTranscoder m_SUT; 
    final private String m_MulticastHost = "225.1.2.3"; 
    final private int m_MulticastPort = 5004;
    private URI m_MulticastUri;
    private URI m_SourceStream;
    final private String m_ProcessId = "PROCESS.ID";
    final private String m_MimeTypeMpeg = "video/mpeg";
    
    @Mock private HeadlessMediaPlayer m_MediaPlayer;
    @Mock private LoggingService m_Logging;
       
    @Before
    public void setUp() throws URISyntaxException, Exception
    {
        MockitoAnnotations.initMocks(this);
  
        PowerMockito.spy(VlcUtils.class);
        PowerMockito.doReturn(m_MediaPlayer).when(VlcUtils.class, "newPlayer");
        when(m_MediaPlayer.playMedia(anyString(), (String)anyVararg())).thenReturn(true);
        
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class, "load");
                
        m_SUT = new VlcTranscoder();
        m_SUT.setLoggingService(m_Logging);
        
        m_MulticastUri = new URI(null, null, m_MulticastHost, m_MulticastPort, null, null, null);
        
        m_SourceStream = new URI("rtsp://195.168.1.254:554/mpeg4/media.amp");              
    }
            
    /**
     * Verify that a transcoding process can be started.
     * 
     * @throws TranscoderException
     *      if the transcoder instance produces an error.
     */ 
    @Test
    public void testStart() throws TranscoderException
    {
        final double bitrateKbps = 10.0;
        
        final Map<String, Object> configParams = new HashMap<>();
        configParams.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, bitrateKbps);
        configParams.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMpeg);
        
        m_SUT.activate();    
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_MulticastUri, configParams);
        
        verify(m_MediaPlayer).addMediaPlayerEventListener(Mockito.any(MediaPlayerEventListener.class));
        verify(m_MediaPlayer).playMedia(anyString(), (String)anyVararg());
        
        assertThat(m_SUT.getActiveProcessIds(), hasItem(m_ProcessId));
        
        m_SUT.deactivate();
        verify(m_MediaPlayer).stop();
        verify(m_MediaPlayer).release();        
    }
    
    @Test(expected = IllegalStateException.class)
    public void testStartWithExistingId() throws TranscoderException
    {
        final double bitrateKbps = 10.0;
        
        final Map<String, Object> configParams = new HashMap<>();
        configParams.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, bitrateKbps);
        configParams.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMpeg);      
        
        Map<String, HeadlessMediaPlayer> processMap = new HashMap<>();
        processMap.put(m_ProcessId, m_MediaPlayer);
        Whitebox.setInternalState(m_SUT, "m_ProcessMap", processMap);
        
        //Try to start transcoding process using the same ID
        m_SUT.start(m_ProcessId, m_SourceStream, m_MulticastUri, configParams);        
    }
    
    @Test(expected = TranscoderException.class)
    public void testStartWithPlayerError() throws TranscoderException
    {
        final double bitrateKbps = 10.0;
        
        final Map<String, Object> configParams = new HashMap<>();
        configParams.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, bitrateKbps);
        configParams.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMpeg);
        
        m_SUT.activate();    
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        when(m_MediaPlayer.playMedia(anyString(), (String)anyVararg())).thenReturn(false);
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_MulticastUri, configParams);
    }
    
    /**
     * Verify that a transcoding process can be started and subsequently stopped.
     * 
     * @throws TranscoderException
     *      if the transcoder instance produces an error.
     */
    @Test
    public void testStop() throws TranscoderException
    {
        final double bitrateKbps = 10.0;
        
        final Map<String, Object> configParams = new HashMap<>();
        configParams.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, bitrateKbps);
        configParams.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMpeg);
        
        m_SUT.activate();     
        assertThat(m_SUT.getActiveProcessIds(), is(Matchers.<String>empty()));
        
        m_SUT.start(m_ProcessId, m_SourceStream, m_MulticastUri, configParams);
        
        verify(m_MediaPlayer).addMediaPlayerEventListener(Mockito.any(MediaPlayerEventListener.class));
        verify(m_MediaPlayer).playMedia(anyString(), (String)anyVararg());
        assertThat(m_SUT.getActiveProcessIds(), hasItem(m_ProcessId));
                
        //Stop the transcoding process
        m_SUT.stop(m_ProcessId);
        
        assertThat(m_SUT.getActiveProcessIds(), not(hasItem(m_ProcessId)));        
    }

    /**
     * Verify that an exception is thrown if {@link mil.dod.th.core.transcoder.TranscoderService#stop(String)}
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
