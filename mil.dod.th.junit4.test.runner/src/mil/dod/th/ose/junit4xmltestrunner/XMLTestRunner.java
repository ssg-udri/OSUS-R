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
package mil.dod.th.ose.junit4xmltestrunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.junit4xmltestrunner.ant.XMLJUnitResultFormatter;

import org.osgi.service.log.LogService;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/**
 * Main class that handles running test cases and saving the results to an XML file.
 * 
 * @author dhumeniuk
 *
 */
public final class XMLTestRunner
{
    /**
     * Hide constructor to prevent instantiation.
     */
    private XMLTestRunner()
    {
        
    }
    
    /**
     * Run the given test classes using the runner.  A separate XML file will be generated for each class.
     * 
     * @param dir
     *      Base directory to place the XML reports
     * @param suffix
     *      String to append to class name to differentiate from other times the class is executed
     * @param printStackTrace
     *      whether to print stack trace on failures
     * @param classes
     *      All the classes to run, a class must be decorated with the @Test annotation
     * @throws IOException
     *      Error writing to file or creating a file
     */
    public static void run(final File dir, final String suffix, final boolean printStackTrace, 
            final Class<?>... classes) throws IOException
    {
        for (Class<?> klass : classes)
        {
            final JUnitCore core = new JUnitCore();
            final XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
            
            final String qualifiedClassName = String.format("%s-%s", klass.getName(), suffix);
            final File reportFile = new File(dir, "TEST-" + qualifiedClassName + ".xml");
            final OutputStream outStream = new FileOutputStream(reportFile);
            formatter.setOutput(outStream);
            
            core.addListener(new XMLFormatterListener(formatter, printStackTrace));
            
            final Result results;
            try
            {
                formatter.startTestSuite(qualifiedClassName);
                results = core.run(klass);
                formatter.endTestSuite(results);
            }
            finally
            {
                formatter.cleanup();
            }
            
            logMemoryUsage();
        }
    }
    
    /**
     * Log memory usage to the OSGi Log Service.
     */
    public static void logMemoryUsage()
    {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
        {
            Logging.log(LogService.LOG_INFO, "%s (%s): %s", pool.getName(), pool.getType(), pool.getPeakUsage());
        }
        
        Logging.log(LogService.LOG_INFO, "heap: %s", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
        Logging.log(LogService.LOG_INFO, "non-heap: %s", 
                ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
    }
}
