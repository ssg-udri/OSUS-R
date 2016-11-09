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
package mil.dod.th.ose.gui.integration.screenrecorder;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.VideoFormatKeys;
import org.monte.media.math.Rational;

/**
 * Class that creates and returns a {@link ScreenRecorderExt} used capture video of the gui-integration tests.
 * 
 * @author cweisenborn
 */
public final class ScreenRecorderFactory
{   
    /**
     * Private constructor to prevent instantiation.
     */
    private ScreenRecorderFactory()
    {
        
    }
    
    /**
     * Method used to retrieve the stored instance of the {@link ScreenRecorderExt} for recording testing.
     * 
     * @return
     *          An instance of the {@link ScreenRecorderExt} class.
     * @throws IOException
     *      Thrown if the screen recorder cannot be instantiated.
     * @throws AWTException
     *      Thrown if the screen recorder cannot be instantiated.
     */
    public static ScreenRecorderExt getRecorder() throws IOException, AWTException
    {
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().
                getDefaultConfiguration();
        
        final ScreenRecorderExt screenRecorder = new ScreenRecorderExt(gc, null,
                new Format(FormatKeys.MediaTypeKey, MediaType.FILE, FormatKeys.MimeTypeKey, FormatKeys.MIME_AVI),
                new Format(FormatKeys.MediaTypeKey, MediaType.VIDEO, FormatKeys.EncodingKey, 
                VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                VideoFormatKeys.CompressorNameKey, VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                VideoFormatKeys.DepthKey, 24, FormatKeys.FrameRateKey, Rational.valueOf(15),
                VideoFormatKeys.QualityKey, 1.0f, FormatKeys.KeyFrameIntervalKey, 15 * 60), null, null, 
                new File(ResourceLocatorUtil.getGuiTestDataPath(), "recordings"));

        return screenRecorder;
    }
}
