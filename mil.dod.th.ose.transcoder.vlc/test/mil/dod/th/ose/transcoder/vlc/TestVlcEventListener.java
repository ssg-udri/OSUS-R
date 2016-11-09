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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.co.caprica.vlcj.player.MediaPlayer;
import static org.mockito.Mockito.*;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
@SuppressStaticInitializationFor("mil.dod.th.ose.transcoder.vlc.VlcTranscoder")
public class TestVlcEventListener
{
    @Mock private VlcTranscoder m_Transcoder;
    @Mock private MediaPlayer m_MediaPlayer;
    
    private VlcEventListener m_SUT;
    private String m_ListenerId; 
    
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        m_ListenerId = "Listener.ID";
        
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class, "load");
        
        m_SUT = new VlcEventListener(m_Transcoder, m_ListenerId);       
    }
    
    @Test
    public void testPlaying()
    {
        m_SUT.playing(m_MediaPlayer);
        verify(m_Transcoder).playingEvent(m_ListenerId);
    }
    
    @Test
    public void testError()
    {
        m_SUT.error(m_MediaPlayer);
        verify(m_Transcoder).errorEvent(m_ListenerId);
    }
    
    @Test
    public void testStopped()
    {
        m_SUT.stopped(m_MediaPlayer);
        verify(m_Transcoder).stoppedEvent(m_ListenerId);
    }
}
