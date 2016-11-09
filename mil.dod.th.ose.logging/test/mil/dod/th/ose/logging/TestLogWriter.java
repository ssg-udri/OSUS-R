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
package mil.dod.th.ose.logging;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import mil.dod.th.ose.shared.LogLevel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * 
 * @author dlandoll
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LogWriter.class)
public class TestLogWriter
{
    private LogWriter m_SUT;
    private LogReaderService m_LogReader;
    private Bundle m_Bundle;
    private File m_LogFile;
    private File m_LogFileDir = new File(".", "TempLogDir");

    @Before
    public void setUp() throws Exception
    {
        m_LogReader = mock(LogReaderService.class);
        when(m_LogReader.getLog()).thenReturn(new Vector<LogEntry>().elements());
        m_SUT = new LogWriter();
        m_SUT.setLogReaderService(m_LogReader);
        m_Bundle = mock(Bundle.class);
        m_LogFileDir.mkdir();
    }

    @After
    public void tearDown() throws Exception
    {
        m_SUT.unsetLogReaderService(m_LogReader);
        if (m_LogFileDir != null)
        {
            for (File file : m_LogFileDir.listFiles())
            {
                file.delete();
            }
            m_LogFileDir.delete();
        }
    }

    /**
     * Test that activation retrieves needed properties, and opens a log file.
     */
    @Test
    public void testActivate()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(null);
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // Verify that null properties don't cause an exception
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn(null);
        when(context.getProperty(LogWriter.LOG_STDOUT_PROPERTY)).thenReturn(null);

        m_SUT.deactivate();
    }
    
    @Test
    public void testActivateFailure() throws Exception
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        File fileMock = mock(File.class);
        PowerMockito.whenNew(File.class).withArguments("TEST").thenReturn(fileMock);
        when(fileMock.mkdir()).thenReturn(false);
        when(fileMock.exists()).thenReturn(false);
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TEST");
        
        try
        {
            m_SUT.activate(context, props);
            fail("Expecting illegal state exception!");
        }
        catch (IllegalStateException ex)
        {
            //Expected Exception
        }
    }

    /**
     * Test that activation with improperly spelled log filter log level.
     * Verify that the log writer still activates.
     */
    @Test
    public void testActivateBadFilterProfile()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY + 1)).
            thenReturn("green.charcoal.*:moooooooooo");
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(null);
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        m_SUT.deactivate();
    }
    
    /**
     * Test that activation with improperly spelled initial log level.
     * Verify that the log writer still activates.
     */
    @Test
    public void testActivateBadInitialLogLevel()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn("moooooooooo");
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        m_SUT.deactivate();
    }
    
    /**
     * Verify that messages can in fact be logged.
     */
    @Test
    public void testLogged()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());

        // Verify stdout
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn(null);
        when(context.getProperty(LogWriter.LOG_STDOUT_PROPERTY)).thenReturn("true");
        when(context.getProperty(LogWriter.LOG_EXCEPTIONS_PROPERTY)).thenReturn("true");
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();
        
        LogEntry logEntry = mock(LogEntry.class);
        //redirect system out
        PrintStream origOutStream = System.out;
        PrintStream testOutStream = mock(PrintStream.class);
        try
        {
            System.setOut(testOutStream);
            
            when(logEntry.getBundle()).thenReturn(m_Bundle);
            when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
            when(logEntry.getTime()).thenReturn(1L);
            when(logEntry.getMessage()).thenReturn("blah");

            m_SUT.logged(logEntry);

            verify(testOutStream).println(anyString());
        }
        finally
        { //need to make sure that even if there is a failure that system out is reset
            System.setOut(origOutStream);
        }

        m_SUT.deactivate();

        // Verify file output
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_STDOUT_PROPERTY)).thenReturn("false");
        when(context.getProperty(LogWriter.LOG_EXCEPTIONS_PROPERTY)).thenReturn("false");

        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        long fileSize = m_LogFile.length();
        assertThat(m_LogFile.exists(), is(true));

        m_SUT.logged(logEntry);

        m_SUT.deactivate();

        assertThat(m_LogFile.length(), greaterThan(fileSize));
    }

    /**
     * Test standard out property.
     */
    @Test
    public void testSetLogStdOut()
    {
        assertThat(m_SUT.setLogStdOut("true"), is(true));
        assertThat(m_SUT.setLogStdOut("false"), is(true));
        assertThat(m_SUT.setLogStdOut(""), is(false));
    }

    /**
     * Test log exception property.
     */
    @Test
    public void testSetLogExceptions()
    {
        assertThat(m_SUT.setLogExceptions("true"), is(true));
        assertThat(m_SUT.setLogExceptions("false"), is(true));
        assertThat(m_SUT.setLogExceptions(""), is(false));
    }

    /**
     * Verify that the buffer size is used to limit the amount of log messages held in memory before being written
     * to file.
     */
    @Test
    public void testSetBufferSize() throws Exception
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        //set the buffer size to something that isn't the default
        props.put("fileBufferSize", 512);
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();

        // ensuring the file size has not changed
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        m_SUT.logged(logEntry);
        assertThat(m_LogFile.length(), is(fileLength));

        // Filling string to make message for logEntry
        String str = "";
        for (int i = 0; i < 550; i++)
        {
            str += "a";
        }
        when(logEntry.getMessage()).thenReturn(str);
        m_SUT.logged(logEntry);

        // Verify that file size has increased
        fileLength = m_LogFile.length();
        assertThat(fileLength, greaterThan(512L));
    }

    /**
     * Test the default buffer size is 1, essentially set to not buffer.
     * Verify that the file size increases instantly.
     */
    @Test
    public void testDefaultBufferSize() throws Exception
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();

        // ensuring the file size has not changed
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        m_SUT.logged(logEntry);
        //should be immediately be larger as the default buffer size is 1
        assertThat(m_LogFile.length(), greaterThan(fileLength));
    }

    /**
     * Test reading the log level property from the property file.
     */
    @Test
    public void testLogLevelProperty() throws Exception
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(LogLevel.Warning.toString());

        // Setting key value pair
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();

        // log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        m_SUT.logged(logEntry);
        //should no be larger because debug is log level 4
        assertThat(m_LogFile.length(), is(fileLength));

        //replay this time including the debug log level
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(LogLevel.Debug.toString());
        m_SUT.modified(props);

        // testing that activation/openLogFile methods
        fileLength = m_LogFile.length();

        // log entry
        m_SUT.logged(logEntry);
        //should be immediately be larger
        assertThat(m_LogFile.length(), greaterThan(fileLength));
    }

    @Test
    public void testLineReturn() throws Exception
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_STDOUT_PROPERTY)).thenReturn("false");
        when(context.getProperty(LogWriter.LOG_EXCEPTIONS_PROPERTY)).thenReturn("false");

        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        LogEntry logEntry = mock(LogEntry.class);

        when(logEntry.getMessage()).thenReturn("Blargh");
        when(logEntry.getLevel()).thenReturn(1);

        m_SUT.logged(logEntry); // first log entry
        m_SUT.logged(logEntry); // second log entry
        m_SUT.logged(logEntry); // third log entry

        m_SUT.deactivate();

        try (BufferedReader reader = new BufferedReader(new FileReader(m_LogFile)))
        {
            int numberOfLines = 0;
            while (reader.readLine() != null)
            {
                numberOfLines++;
            }
    
            // it is known that three entries were made in addition to the header
            assertThat(numberOfLines, is(4));
        }
    }

    /**
     * This tests if there is a log filter, that messages matching the filter's bundle and severity restrictions, are 
     * logged.
     */
    @Test
    public void testLogFilterSingleFromFile()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY + 1)).
            thenReturn("green.charcoal.*:Warning");
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);

        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();
        
        // mock a log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getBundle().getSymbolicName()).thenReturn("green.charcoal.puppies");
        when(logEntry.getMessage()).thenReturn("Test");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        
        //should not be logged because it is a debug message and the filter says not to log below Warning
        m_SUT.logged(logEntry);
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        // check the log file did not become larger
        assertThat(m_LogFile.length(), is(fileLength));
    }
    
    /**
     * Test an incomplete log filter profile. Verify things don't error out.
     */
    @Test
    public void testLogFilterSingleFromFileBad()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        //force more frequent flushing
        props.put("fileBufferSize", 1);
        when(context.getProperty(LogWriter.LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY + 1)).
            thenReturn("green.charcoal.*");
        when(context.getProperty(LogWriter.LOG_BUNDLE_FILTER_PROFILE_PREFIX_PROPERTY + 2)).
            thenReturn("green.charcoal.*:Warning");
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);

        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();
        
        // mock a log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getBundle().getSymbolicName()).thenReturn("green.charcoal.puppies");
        when(logEntry.getMessage()).thenReturn("Test");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_ERROR);
        when(logEntry.getTime()).thenReturn(1L);
        
        //should be logged, because things are still working despite the bad filter
        m_SUT.logged(logEntry);
        
        long fileLengthAfterGoodEntry = m_LogFile.length();
        
        // mock a log entry
        when(logEntry.getBundle().getSymbolicName()).thenReturn("green.charcoal.puppies.kittens");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        
        //should not be logged, because things are still working despite the bad filter
        m_SUT.logged(logEntry);
        
        long fileLengthAfterBadEntry = m_LogFile.length();
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        
        // check the log file 
        assertThat(fileLengthAfterGoodEntry, greaterThan(fileLength));
        assertThat(fileLengthAfterBadEntry, is(fileLengthAfterGoodEntry));
    }
    
    /**
     * This tests if there is a log filter that messages matching the filter's bundle and severity restrictions are 
     * logged.
     */
    @Test
    public void testLogFilterSingle()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        
        //create a log filter
        LogFilter filter = createFilter("my.bundle.*", 3);

        // Setting key value pair
        m_SUT.activate(context, props);
        //set log filter
        m_SUT.setLogFilter(filter);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();
        
        // mock a log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getBundle().getSymbolicName()).thenReturn("my.bundle.puppies");
        when(logEntry.getMessage()).thenReturn("Test");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        m_SUT.logged(logEntry);
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        // check the log file became larger due the entry being logged
        assertThat(m_LogFile.length(), is(fileLength));
        
        // Setting key value pair
        m_SUT.activate(context, props);
        //set log filter
        m_SUT.setLogFilter(filter);
        // This is to mimic the log file created during activation.
        setUpFileNaming();
        
        when(logEntry.getLevel()).thenReturn(LogService.LOG_WARNING);
        m_SUT.logged(logEntry);
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        // check the log file became larger due the entry being logged
        assertThat(m_LogFile.length(), greaterThan(fileLength));
    }
    
    /**
     * This tests if there is a log filter, that a message matching the filter's bundle and severity restriction is 
     * logged.
     */
    @Test
    public void testLogFilterMatch()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        
        //create a log filter
        LogFilter filter = createFilter("my.bundle.*", 3);

        // Setting key value pair
        m_SUT.activate(context, props);
        //set log filter
        m_SUT.setLogFilter(filter);
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // testing that activation/openLogFile methods
        long fileLength = m_LogFile.length();
        
        // mock a log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getBundle().getSymbolicName()).thenReturn("my.bundle.puppies");
        when(logEntry.getMessage()).thenReturn("Test");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_INFO);
        when(logEntry.getTime()).thenReturn(1L);
        m_SUT.logged(logEntry);
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        // check the log file became larger due the entry being logged
        assertThat(m_LogFile.length(), greaterThan(fileLength));
    }
    
    /**
     * This tests if there is are log filters, that messages matching a filter's bundle and severity restriction are 
     * logged when appropriate.
     */
    @Test
    public void testLogFiltersMultiples()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        //force more frequent flushing
        props.put("fileBufferSize", 1);
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        
        //create a log filter
        LogFilter filterA = createFilter("my.bundle.*", 3);
        LogFilter filterB = createFilter("your.bundle.kittens.*", 1);
        LogFilter filterC = createFilter("their.giraffe.cam.*", 3);
        LogFilter filterD = createFilter("their.giraffe.cam.guppy", 1);

        // Setting key value pair
        m_SUT.activate(context, props);
        
        //set log filters
        m_SUT.setLogFilter(filterA);
        m_SUT.setLogFilter(filterB);
        m_SUT.setLogFilter(filterC);
        m_SUT.setLogFilter(filterD);
        
        // This is to mimic the log file created during activation.
        setUpFileNaming();

        // initial file size
        long fileLength = m_LogFile.length();
        
        // mock a log entry
        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        when(logEntry.getMessage()).thenReturn("Test");
        when(logEntry.getTime()).thenReturn(1L);
        
        //test filter A
        when(logEntry.getBundle().getSymbolicName()).thenReturn("my.bundle");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        m_SUT.logged(logEntry);
        
        long filePostALength = m_LogFile.length();
        
        //test filter B
        when(logEntry.getBundle().getSymbolicName()).thenReturn("your.bundle.kittens.fuzzy");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_ERROR);
        m_SUT.logged(logEntry);
        
        long filePostBLength = m_LogFile.length();

        //test filter D and C, should be logged, because Filter C covers the bundle's name,
        //even though D is more specific and higher severity, go with lower severity
        when(logEntry.getBundle().getSymbolicName()).thenReturn("their.giraffe.cam.guppy");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_WARNING);
        m_SUT.logged(logEntry);

        long filePostCLength = m_LogFile.length();
        
        //unset filter C and retry
        m_SUT.unsetLogFilter(filterC);
        
        //shouldn't log this time
        when(logEntry.getBundle().getSymbolicName()).thenReturn("their.giraffe.cam.guppy");
        when(logEntry.getLevel()).thenReturn(LogService.LOG_WARNING);
        m_SUT.logged(logEntry);

        long filePostDLength = m_LogFile.length();
        
        // this forces the flushing of the buffer.
        m_SUT.deactivate();
        
        // check the log file became larger due the entries being logged
        assertThat(filePostALength, is(fileLength));
        assertThat(filePostBLength, greaterThan(filePostALength));
        assertThat(filePostCLength, greaterThan(filePostBLength));
        assertThat(filePostDLength, is(filePostCLength));
    }
    
    /**
     * Test that once the limit for the file size is reached that a new log file is created.
     */
    @Test
    public void testLogSizeLimit()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        props.put("logSizeLimit", 1);
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);
        setUpFileNaming();
        
        //redirect print stream
        PrintStream origOutStream = System.out;
        PrintStream testOutStream = mock(PrintStream.class);
        
        try
        {
            System.setOut(testOutStream);

            // mock a log entry
            while (m_LogFile.length() < (1 * 1024 * 1024L))
            {
                LogEntry logEntry = mock(LogEntry.class);
                when(logEntry.getBundle()).thenReturn(m_Bundle);
                when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
                when(logEntry.getTime()).thenReturn(1L);
                when(logEntry.getMessage()).thenReturn("Rut Row Raggy!");
                m_SUT.logged(logEntry);
            }
            // because a new log file should of been created
            File[] files = m_LogFileDir.listFiles();
            // look for the new log file
            File logFile2 = null;
            for (File file : files)
            {
                if (!file.equals(m_LogFile))
                {
                    logFile2 = file;
                }
            }
            if (logFile2 != null)
            {
                assertThat(logFile2.exists(), is(true));
                // assert that the file is at least the length of the timestamp that starts every new log file.
                assertThat(logFile2.length(), greaterThan(40L));
                // make sure that this isn't the same log file
                assertThat(logFile2.getName(), not(equalTo(m_LogFile.getName())));
            }
            else
            {
                fail("expected file to exist");
            }
            // check that the original file reached the default limit of (about) 1MB
            assertThat(m_LogFile.length(), greaterThan(1 * 1024 * 1024L));
        }
        finally
        {
            //reset print stream
            System.setOut(origOutStream);
        }
        
        m_SUT.deactivate();
    }

    /**
     * Test that if config admin is not available that the property file value is used for the log size.
     */
    @Test
    public void testLogSizeLimitPropertyValue()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_SIZE_PROPERTY)).thenReturn(null);

        // Setting key value pair
        m_SUT.activate(context, props);
        setUpFileNaming();
        
        //redirect print stream
        PrintStream origOutStream = System.out;
        PrintStream testOutStream = mock(PrintStream.class);
        
        try
        {
            System.setOut(testOutStream);

            // mock a log entry
            while (m_LogFile.length() < (1 * 1024 * 1024L))
            {
                LogEntry logEntry = mock(LogEntry.class);
                when(logEntry.getBundle()).thenReturn(m_Bundle);
                when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
                when(logEntry.getTime()).thenReturn(1L);
                when(logEntry.getMessage()).thenReturn("Rut Row Raggy!");
                m_SUT.logged(logEntry);
            }
            // because a new log file should of been created
            File[] files = m_LogFileDir.listFiles();
            // look for the new log file
            File logFile2 = null;
            for (File file : files)
            {
                if (!file.equals(m_LogFile))
                {
                    logFile2 = file;
                }
            }
            if (logFile2 != null)
            {
                assertThat(logFile2.exists(), is(true));
                // assert that the file is at least the length of the timestamp that starts every new log file.
                assertThat(logFile2.length(), greaterThan(40L));
                // make sure that this isn't the same log file
                assertThat(logFile2.getName(), not(equalTo(m_LogFile.getName())));
            }
            else
            {
                fail("expected file to exist");
            }
            // check that the original file reached the default limit of (about) 1MB
            assertThat(m_LogFile.length(), greaterThan(1 * 1024 * 1024L));
        }
        finally
        {
            //reset print stream
            System.setOut(origOutStream);
        }
        
        m_SUT.deactivate();
    }

    /**
     * Test setting the log level.
     * Verify that only message meeting the levels severity or greater are logged.
     */
    @Test
    public void testLogLevel()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        props.put("logLevel", LogLevel.Error.toString());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");

        // Setting key value pair
        m_SUT.activate(context, props);
        setUpFileNaming();

        long prelength = m_LogFile.length();

        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        // ask to log at a level too high
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        when(logEntry.getMessage()).thenReturn("AROOOGA!");
        m_SUT.logged(logEntry);

        // clear buffer and make sure that the file is the same size as it was in the beginning
        m_SUT.deactivate();
        assertThat(m_LogFile.length(), is(prelength));
        m_LogFile.delete();

        // start the log writer again
        m_SUT.activate(context, props);
        // This is to mimic the log file created during activation.
        setUpFileNaming();
        prelength = m_LogFile.length();

        // log for the correct level
        when(logEntry.getLevel()).thenReturn(LogService.LOG_ERROR);
        m_SUT.logged(logEntry);

        // clear buffer and make sure the log file is bigger.
        m_SUT.deactivate();
        assertThat(m_LogFile.length(), greaterThan(prelength));
    }

    /**
     * Test that if no values are set in the property file that local defaults are used.
     * Verify logging still works.
     */
    @Test
    public void testNullPropertiesNoConfigAdmin()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_DAYS_ALIVE_PROPERTY)).thenReturn(null);
        when(context.getProperty(LogWriter.LOG_FILE_BUFFER_SIZE_PROPERTY)).thenReturn(null);
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(null);
        when(context.getProperty(LogWriter.LOG_SIZE_PROPERTY)).thenReturn(null);
        // Setting key value pair
        m_SUT.activate(context, props);
        setUpFileNaming();

        long prelength = m_LogFile.length();

        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        // ask to log at a level that is acceptable
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        when(logEntry.getMessage()).thenReturn("AROOOGA!");
        m_SUT.logged(logEntry);

        //verify that the log entry was successful, this proves that even if the config admin is unavailable and there
        //are no properties known to the framework that the log service will still function.
        assertThat(m_LogFile.length(), greaterThan(prelength));
    }

    /**
     * Test that if values are set in the property file that those values are used when config admin is not available.
     * Verify logging works with those values.
     */
    @Test
    public void testPropertiesNoConfigAdmin()
    {
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> props = new HashMap<String, Object>(getComponentProperties());
        when(context.getProperty(LogWriter.LOG_DIR_PROPERTY)).thenReturn("TempLogDir");
        when(context.getProperty(LogWriter.LOG_DAYS_ALIVE_PROPERTY)).thenReturn("1");
        when(context.getProperty(LogWriter.LOG_FILE_BUFFER_SIZE_PROPERTY)).thenReturn("2");
        when(context.getProperty(LogWriter.LOG_LEVEL_PROPERTY)).thenReturn(LogLevel.Warning.toString());
        when(context.getProperty(LogWriter.LOG_SIZE_PROPERTY)).thenReturn("2");
        // Setting key value pair
        m_SUT.activate(context, props);
        setUpFileNaming();

        long prelength = m_LogFile.length();

        LogEntry logEntry = mock(LogEntry.class);
        when(logEntry.getBundle()).thenReturn(m_Bundle);
        // ask to log at a level too high
        when(logEntry.getLevel()).thenReturn(LogService.LOG_DEBUG);
        when(logEntry.getTime()).thenReturn(1L);
        when(logEntry.getMessage()).thenReturn("AROOOGA!");
        m_SUT.logged(logEntry);

        //verify that data is not logged at the log level is no acceptable
        assertThat(m_LogFile.length(), is(prelength));

        //change level to acceptable range
        when(logEntry.getLevel()).thenReturn(LogService.LOG_ERROR);
        m_SUT.logged(logEntry);

        //verify that the log entry was successful, this proves that even if the config admin is unavailable and there
        //are no properties known to the framework that the log service will still function.
        assertThat(m_LogFile.length(), greaterThan(prelength));
    }

    /*
     * Due to the log files being created with the time of creation in the name this method is needed to capture the
     * newly created log file.
     */
    private void setUpFileNaming()
    {
        // get an array of all files in the directory
        File[] files = m_LogFileDir.listFiles();
        if (files.length > 0)
        {
            assertThat(files.length, is(1));
            // look for the new log file
            m_LogFile = files[0];
        }
    }
    
    /**
     * Mimic the properties usually passed as properties in the activate and modify methods.
     */
    private Map<String, Object> getComponentProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ComponentConstants.COMPONENT_NAME, "log.writer.awesome.Logwriter");
        props.put(ComponentConstants.COMPONENT_ID, "1");
        return props;
    }
    
    /**
     * Create a Log Filter with the given sym name and severity.
     */
    private LogFilter createFilter(final String symName, final int severity)
    {
        LogFilter filter =  mock(LogFilter.class);
        final String m_SymName = symName;
        when(filter.getSeverity()).thenReturn(severity);
        when(filter.matches(Mockito.any(Bundle.class))).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                final Bundle bundle = (Bundle)invocation.getArguments()[0];
                return bundle.getSymbolicName().matches(m_SymName);
            }
        });
        return filter;
    }
}
