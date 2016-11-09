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
import java.util.Map;

import mil.dod.th.core.transcoder.TranscoderService;
import mil.dod.th.ose.utils.CoverageIgnore;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.player.headless.DefaultHeadlessMediaPlayer;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * Utility class used for setting up a VLC process.
 * 
 * @author jmiller
 *
 */
public final class VlcUtils
{
    
    /** Codec label for mpeg in VLC. */
    final private static String MPEG_CODEC = "mp2v";
    
    /** Codec label for mp4 in VLC. */
    final private static String MP4_CODEC = "mp4v";
    

    /**
     * Private constructor to prevent instantiation.
     */
    private VlcUtils()
    {
        
    }
    
    /**
     * Consumes the stream destination and configuration parameters to generate a String to be used
     * by VLC.
     * 
     * @param destination
     *      Address for output stream
     * @param configParams
     *      Parameters to be used for the transcoding process 
     * @return 
     *      Properly formatted configuration String to be used by VLC
     */
    public static String createConfigString(final URI destination, final Map<String, Object> configParams)
    {
        
        final StringBuilder builder = new StringBuilder(":sout=#");
        
        //Check configParams for keys defined in {@link TranscoderService}
        if (configParams.containsKey(TranscoderService.CONFIG_PROP_BITRATE_KBPS))
        {
            final double bitrateKbps = ((Double)(configParams.get(
                    TranscoderService.CONFIG_PROP_BITRATE_KBPS))).doubleValue();

            //Value less than 0 indicates no transcoding should be done; pass along streaming data as is
            if (bitrateKbps > 0 && configParams.containsKey(TranscoderService.CONFIG_PROP_FORMAT))
            {


                final String format = (String)configParams.get(TranscoderService.CONFIG_PROP_FORMAT);
                String codecString;//NOCHECKSTYLE because it will be assigned inside the if/else statements

                if (format.equalsIgnoreCase("video/mpeg"))
                {
                    codecString = MPEG_CODEC;
                }
                else if (format.equalsIgnoreCase("video/mp4"))
                {
                    codecString = MP4_CODEC;
                }
                else
                {
                    //Default to mp2v
                    codecString = MPEG_CODEC;
                }

                builder.append("transcode{vcodec=");
                builder.append(codecString);
                builder.append(",vb=");
                builder.append(bitrateKbps);
                builder.append(",acodec=none}:");

            }

        }

        builder.append("rtp{dst=");
        builder.append(destination.getHost());
        builder.append(",port=");
        builder.append(destination.getPort());
        builder.append(",mux=ts,ttl=128}");

        return builder.toString();
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
