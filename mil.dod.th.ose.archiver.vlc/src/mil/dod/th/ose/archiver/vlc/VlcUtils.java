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

import mil.dod.th.ose.utils.CoverageIgnore;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.player.headless.DefaultHeadlessMediaPlayer;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * Utility class used for setting up a VLC archiving process.
 * 
 * @author jmiller
 *
 */
public final class VlcUtils
{
    
    /**
     * Private constructor to prevent instantiation.
     */
    private VlcUtils()
    {
        
    }
    
    /**
     * Create a String to pass as an option to {@link HeadlessMediaPlayer#playMedia}.
     * This directive routes the video data to a file.
     * 
     * @param filePath
     *      the location of the file that will store the video data
     * @return
     *      option String
     */
    public static String createOptionsString(final String filePath)
    {
        return String.format(":sout=file/ts:%s", filePath);
    }
    
    /**
     * Creates a new headless media player instance.
     * 
     * @return 
     *      New instance of HeadlessMediaPlayer
     */
    @CoverageIgnore
    public static HeadlessMediaPlayer newPlayer()
    {
        
        // Usage with the atLeast directive is taken from VLC's own media player 
        // factory.  The 2.1.0 version appears to correspond to a watershed release 
        // that added critical functionality, so we keep it as is.  For more info,
        // see: http://git.io/pV0u
        final LibVlc lib = LibVlcFactory.factory().atLeast("2.1.0").create();        
        final String[] libvlcArgs = new String[]{""};
        final libvlc_instance_t instance = lib.libvlc_new(libvlcArgs.length, libvlcArgs);
        return new DefaultHeadlessMediaPlayer(lib, instance);
    }
}
