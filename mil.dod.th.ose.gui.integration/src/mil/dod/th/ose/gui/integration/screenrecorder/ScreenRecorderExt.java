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
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Logger;

import org.monte.media.Format;
import org.monte.screenrecorder.ScreenRecorder;

/**
 * This class is used to extend the functionality of the {@link ScreenRecorder}. The ScreenRecorder class does not
 * allow the user to specify the name of the file with which to record so this class with custom start and stop methods
 * to allow specifying a file name with which to record to.
 * 
 * @author cweisenborn
 */
public class ScreenRecorderExt extends ScreenRecorder
{
    /**
     * Logger used to log screen recorder information.
     */
    private static Logger LOGGER = Logger.getLogger("mil.dod.th.ose.gui.integration");
    
    /**
     * Constructor method see {@link ScreenRecorder#ScreenRecorder
     * (GraphicsConfiguration, Rectangle, Format, Format, Format, Format, File)}.
     * 
     * @param cfg
     *      {@link GraphicsConfiguration} to be used.
     * @param captureArea
     *      {@link Rectangle} to be used.
     * @param fileFormat
     *      {@link Format} to be used for the file format.
     * @param screenFormat
     *      {@link Format} to be used for the screen format.
     * @param mouseFormat
     *      {@link Format} to be used for the mouse format.
     * @param audioFormat
     *      {@link Format} to be used for the audio format.
     * @param movieFolder
     *      {@link File} directory the screen recording is to be stored in.
     * @throws IOException
     *      Thrown if there is an IOException instantiating the screen recorder.
     * @throws AWTException
     *      Thrown is there is an exception instantiating the objects used to record the screen.
     */
    public ScreenRecorderExt(final GraphicsConfiguration cfg, final Rectangle captureArea, final Format fileFormat, 
        final Format screenFormat, final Format mouseFormat, final Format audioFormat, final File movieFolder) 
        throws IOException, AWTException
    {
        super(cfg, captureArea, fileFormat, screenFormat, mouseFormat, audioFormat, movieFolder);
    }
    
    /**
     * Method that is responsible for stopping recording. Sets the name of the file the screen recorder was recording to
     * if one was set. Has to be done after the file is created because the default {@link ScreenRecorder} class does 
     * not allow for setting the file name. This method will check if the screen recording file already exists and 
     * delete it if it does.
     * 
     * @param testName
     *      Name of the test that was being recorded.
     * @param className
     *      Name of the class the test being recorded belonged to.
     * @throws InterruptedException
     *      Thrown if a current thread is interrupted during a thread.sleep().
     * @throws IOException
     *      Thrown if the screen recording cannot be renamed.
     */
    public void stopRecording(final String testName, final String className) throws InterruptedException, IOException
    {
        //Added second sleep so recordings don't end as abruptly making it hard to sometimes see the final state 
        //of a test.
        final int extendRecordingTimeMS = 1000;
        Thread.sleep(extendRecordingTimeMS);
        super.stop();
        if (testName != null && !testName.isEmpty() && className != null && !className.isEmpty())
        {
            final int lastIndex = super.getCreatedMovieFiles().size() - 1;
            final File movieFile = super.getCreatedMovieFiles().get(lastIndex);
            final File movieDir = new File(movieFile.getParent() + File.separator + className);
            final File renamedFile = new File(movieDir.getAbsolutePath(), testName + ".avi");

            RandomAccessFile randFile = null; 
            FileChannel channel = null;
            FileLock lock = null;
            try
            {
                //Retrieve the lock on the created screen capture file. This is needed to insure that the screen capture
                //software has finished writing to the file.
                randFile = new RandomAccessFile(movieFile, "rw");
                channel = randFile.getChannel();
                lock = channel.lock();
            }
            finally
            {
                //Release the lock on the file so that it can be renamed. Lock was needed or otherwise the old file 
                //might still remain as an empty file after being renamed since the screen capture may not have released
                //its lock on the file.
                if (lock != null)
                {
                    try
                    {
                        lock.release();
                    }
                    finally
                    {
                        if (randFile != null)
                        {
                            randFile.close();
                        }
                    }
                } 
            }
            
            //Check if the directory exists and create it if it does not.
            if (!movieDir.exists())
            {
                LOGGER.info(String.format("Directory with the name %s does not exist and will be created.", 
                        movieDir.getName()));
                movieDir.mkdir();
            }
            //File cannot be renamed if a file with the specified name already exists.
            if (renamedFile.exists())
            {
                LOGGER.info(String.format("Screen recording file with the name %s already exists and will be removed.", 
                        renamedFile.getName()));
                renamedFile.delete();
            }
            
            //Rename the file.
            movieFile.renameTo(renamedFile);
        }
    }
}
