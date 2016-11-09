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
package mil.dod.th.ose.time.service.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Test;

/**
 * Tests JUL logger for time server.
 * 
 * @author nickmarcucci
 *
 */
public class TestTimeChangeServiceLogger
{
    private static final String EXPECTED_LOG_FILENAME = "timeserver.log";
    
    private static final String CUSTOM_LOG_DIR = "C:\\custom_loc\\";
    
    private static final String CUSTOM_LOG_LOCATION = CUSTOM_LOG_DIR + EXPECTED_LOG_FILENAME;
    
    @After
    public void tearDown()
    {
        //break lock on folders.
        Logger logger = Logger.getLogger("TimeServiceLogger");
        
        for (Handler handler : logger.getHandlers())
        {
            handler.close();
        }
        
        //tear down custom log
        File customLogFile = new File(CUSTOM_LOG_LOCATION);
        if (customLogFile.exists())
        {
            customLogFile.delete();
        }
        
        //remove custom log directory.
        File customLogDir = new File(CUSTOM_LOG_DIR);
        if (customLogDir.exists())
        {
            customLogDir.delete();
        }
    }
    
    /**
     * Test that logger is setup to write to a custom location.
     */
    @Test
    public void testSetupLoggerCustomLoc()
    {
        File file = new File(CUSTOM_LOG_DIR);
        if (!file.exists())
        {
            file.mkdirs();
        }
        
        TimeChangeServiceLogger.setupLogger(CUSTOM_LOG_DIR);
        
        File customFile = new File(CUSTOM_LOG_LOCATION);
        assertThat(customFile.exists(), is(true));
    }
    
    /**
     * Verify that the message is logged to the correct file.
     */
    @Test
    public void testLogMessage() throws IOException
    {
        TimeChangeServiceLogger.setupLogger(CUSTOM_LOG_DIR);
        
        TimeChangeServiceLogger.logMessage(Level.INFO, "This is a message");
        
        Logger logger = Logger.getLogger("TimeServiceLogger");
        
        for (Handler handler : logger.getHandlers())
        {
            handler.close();
        }
        
        File file = new File(CUSTOM_LOG_LOCATION);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder builder = new StringBuilder();
        
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            builder.append(line);
        }
        
        reader.close();
        assertThat(builder.toString().contains("This is a message"), is(true));
    }
    
    /**
     * Verify that the message is logged to the correct file.
     */
    @Test
    public void testLogMessageWithException() throws IOException
    {
        TimeChangeServiceLogger.setupLogger(CUSTOM_LOG_DIR);
        
        TimeChangeServiceLogger.logMessage(Level.INFO, "This is a message", new IOException());
        
        Logger logger = Logger.getLogger("TimeServiceLogger");
        
        for (Handler handler : logger.getHandlers())
        {
            handler.close();
        }
        
        File file = new File(CUSTOM_LOG_LOCATION);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder builder = new StringBuilder();
        
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            builder.append(line);
        }
        
        reader.close();
        assertThat(builder.toString().contains("This is a message"), is(true));
    }
}
