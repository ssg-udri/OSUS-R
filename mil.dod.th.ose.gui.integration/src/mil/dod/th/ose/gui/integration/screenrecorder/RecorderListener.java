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
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;

/**
 * JUnit {@link JUnitResultFormatter} used to listen for when a test starts and ends. When a test starts this 
 * class will trigger a screen recorder to start recording the test. When the test ends this class
 * will trigger the screen recorder to stop recording and save the file with the name of the test that was
 * recorded.
 * 
 * @author cweisenborn
 */
public class RecorderListener implements JUnitResultFormatter 
{
    /**
     * Logger used to log screen recorder information.
     */
    private static Logger LOGGER = Logger.getLogger("mil.dod.th.ose.gui.integration");
    
    /**
     * Variable used to store an instance of the screen recorder used to record tests.
     */
    private ScreenRecorderExt m_ScreenRecorder;
    
    @Override
    public void addError(final Test test, final Throwable throwable)
    {
        //Unneeded method. Required by implementation of listener interface.
    }

    @Override
    public void addFailure(final Test test, final AssertionFailedError error)
    {
        //Unneeded method. Required by implementation of listener interface.
    }

    @Override
    public void endTest(final Test test)
    {
        try
        {
            final String testName = JUnitVersionHelper.getTestCaseName(test);
            final String[] fqcn = JUnitVersionHelper.getTestCaseClassName(test).split("\\.");
            final String className = fqcn[fqcn.length - 1];
            m_ScreenRecorder.stopRecording(testName, className);
        }
        catch (final IOException exception)
        {
            LOGGER.log(Level.WARNING, exception.getMessage(), exception);
        }
        catch (final InterruptedException exception)
        {
            LOGGER.log(Level.WARNING, exception.getMessage(), exception);
        }
    }

    @Override
    public void startTest(final Test test)
    {
        try
        {
            m_ScreenRecorder = ScreenRecorderFactory.getRecorder();
            m_ScreenRecorder.start();
        }
        catch (final IOException exception)
        {
            LOGGER.log(Level.WARNING, exception.getMessage(), exception);
        }
        catch (final AWTException exception)
        {
            LOGGER.log(Level.WARNING, exception.getMessage(), exception);
        }
    }

    @Override
    public void endTestSuite(final JUnitTest test) throws BuildException
    {
        //Unneeded method. Required by implementation of listener interface.
    }

    @Override
    public void setOutput(final OutputStream out)
    {
        //Unneeded method. Required by implementation of listener interface. 
    }

    @Override
    public void setSystemError(final String error)
    {
        //Unneeded method. Required by implementation of listener interface.
    }

    @Override
    public void setSystemOutput(final String out)
    {
        //Unneeded method. Required by implementation of listener interface.
    }

    @Override
    public void startTestSuite(final JUnitTest test) throws BuildException
    {
        //Unneeded method. Required by implementation of listener interface.
    }
}
