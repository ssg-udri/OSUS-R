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

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

/**
 * Event listener implementing several of the events defined in
 * {@link uk.co.caprica.vlcj.player.MediaPlayerEventListener}.
 * 
 * @author jmiller
 *
 */
public class VlcEventListener extends MediaPlayerEventAdapter
{
    
    /** Reference to VlcTranscoder instance. */
    final private VlcTranscoder m_Transcoder;
    
    /** String ID associated with listener instance. */
    final private String m_ListenerId;

    /**
     * Construct a new listener and assign an ID.
     * 
     * @param transcoder
     *      The VlcTrancoder instance that associated with the media player
     * @param listenerId
     *      The String ID associated with this instance.
     */
    public VlcEventListener(final VlcTranscoder transcoder, final String listenerId)
    {
        super();
        m_Transcoder = transcoder;
        m_ListenerId = listenerId;
    }
    
    /**
     * Event triggered when media has started playing.
     * 
     * @param mediaPlayer
     *      MediaPlayer instance associated with event
     */
    @Override
    public void playing(final MediaPlayer mediaPlayer)
    {
        m_Transcoder.playingEvent(m_ListenerId);
    }
    
    /**
     * Event triggered when there has been an error.
     * 
     * @param mediaPlayer
     *      MediaPlayer instance associated with event
     */
    @Override
    public void error(final MediaPlayer mediaPlayer)
    {
        m_Transcoder.errorEvent(m_ListenerId);        
    }
    
    /**
     * Event triggered when media playback has stopped.
     * 
     * @param mediaPlayer
     *      MediaPlayer instance associated with event
     */
    @Override
    public void stopped(final MediaPlayer mediaPlayer)
    {
        m_Transcoder.stoppedEvent(m_ListenerId);
    }
    
    

}
