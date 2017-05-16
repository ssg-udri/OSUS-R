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

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.archiver.ArchiverException;
import mil.dod.th.core.archiver.ArchiverService;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;

import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * This class uses the VLC media player to implement the {@link ArchiverService}. Specifically,
 * it uses the vlcj Java bindings for libVLC. See <a href="https://github.com/caprica/vlcj">
 * https://github.com/caprica/vlcj</a> for more information.
 * 
 * @author jmiller
 *
 */
@Component
public class VlcArchiver implements ArchiverService
{
    /** Map structure to keep track of multiple media player instances. */
    private Map<String, HeadlessMediaPlayer> m_ProcessMap;
    
    /** Logging service instance. */
    private LoggingService m_LoggingService;

    /** Power manager service instance. */
    private PowerManager m_PowerManager;

    /** Wake lock used for keeping system awake during an archive process. */
    private WakeLock m_WakeLock;

    static
    {
        NativeLibraryLoader.load();
    }
    
    /**
     * Bind the logging service.
     * @param logging
     *      service to bind.
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_LoggingService = logging;
    }

    /**
     * Bind the power manager.
     * @param powerManager
     *      service to bind.
     */
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }

    /**
     * Method called when component is activated.
     */
    @Activate
    public void activate()
    {
        m_ProcessMap = new HashMap<>();
        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreVlcArchiver");
    }
    
    /**
     * Method called when component is deactivated. Stop all existing processes.
     * 
     */
    @Deactivate
    public void deactivate()
    {
        for (HeadlessMediaPlayer player:m_ProcessMap.values())
        {
            player.stop();
            player.release();
        }

        m_ProcessMap = null;
        m_WakeLock.delete();
    }
    
    @Override
    public void start(final String processId, final URI sourceUri, final String filePath) throws ArchiverException,
            IllegalStateException
    {
        if (m_ProcessMap.containsKey(processId))
        {
            throw new IllegalStateException("Archiving process for ID: " + processId + " is already running");
        }

        final HeadlessMediaPlayer player = VlcUtils.newPlayer();

        player.addMediaPlayerEventListener(new VlcEventListener(this, processId));
        final String options = VlcUtils.createOptionsString(filePath); 
        
        //Create directory within filestore if it doesn't exist
        final File parentDir = new File(new File(filePath).getParent());
        if (!parentDir.exists())
        {
            try
            {
                parentDir.mkdirs();
            }
            catch (final SecurityException se)
            {
                m_LoggingService.error("Could not create directory %s for archive file", parentDir.toString());
            }
        }
        
        if (player.playMedia(sourceUri.toString(), options))
        {
            //If media item is created successfully, keep track in process map
            m_ProcessMap.put(processId, player);

            m_WakeLock.activate();
        }
        else
        {
            throw new ArchiverException("Error creating archiving process with processId: " + processId);
        }
    }
    
    @Override
    public void stop(final String processId) throws IllegalArgumentException
    {
        final HeadlessMediaPlayer player = m_ProcessMap.get(processId);
        if (player == null)
        {
            throw new IllegalArgumentException("No archiving process exists for ID: " + processId);
        }
        
        player.stop();
        player.release();
        removeProcess(processId);
    }
    
    @Override
    public Set<String> getActiveProcessIds()
    {
        return m_ProcessMap.keySet();
    }    
    
    /**
     * Method to handle {@link uk.co.caprica.vlcj.player.MediaPlayerEventListener#playing(
     * uk.co.caprica.vlcj.player.MediaPlayer)} events generated by {@link HeadlessMediaPlayer}
     * instances.
     * 
     * @param eventListenerId
     *      String identifier assigned to the event listener
     */
    public void playingEvent(final String eventListenerId)
    {
        m_LoggingService.info("Received playing event for event listener ID: %s", eventListenerId);
    }

    /**
     * Method to handle {@link uk.co.caprica.vlcj.player.MediaPlayerEventListener#error(
     * uk.co.caprica.vlcj.player.MediaPlayer)} events generated by {@link HeadlessMediaPlayer}
     * instances.
     * 
     * @param eventListenerId
     *      String identifier assigned to the event listener
     */
    public void errorEvent(final String eventListenerId)
    {
        m_LoggingService.error("Received error event for event listener ID: %s", eventListenerId);
        removeProcess(eventListenerId);
    }

    /**
     * Method to handle {@link uk.co.caprica.vlcj.player.MediaPlayerEventListener#stopped(
     * uk.co.caprica.vlcj.player.MediaPlayer)} events generated by {@link HeadlessMediaPlayer}
     * instances.
     * 
     * @param eventListenerId
     *      String identifier assigned to the event listener
     */
    public void stoppedEvent(final String eventListenerId)
    {
        m_LoggingService.info("Received stopped event for event listener ID: %s", eventListenerId);
    }

    /**
     * Remove process from the map and release wake lock if no other processes are still running.
     * 
     * @param processId
     *      String identifier which refers to an archiving process
     */
    private void removeProcess(final String processId)
    {
        if (m_ProcessMap.containsKey(processId))
        {
            m_ProcessMap.remove(processId);
        }

        if (m_ProcessMap.isEmpty())
        {
            m_WakeLock.cancel();
        }
    }
}
