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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package mil.dod.th.ose.junit4xmltestrunner.ant;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.AssertionFailedError;

import mil.dod.th.core.log.Logging;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


/**
 * Prints XML output of the test to a specified Writer.
 */

public class XMLJUnitResultFormatter implements XMLConstants 
{
    /** used to convert milliseconds to seconds. */
    private static final double ONE_SECOND = 1000.0;

    /** constant for unnnamed testsuites/cases. */
    private static final String UNKNOWN = "unknown";

    /**
     * The XML document.
     */
    private Document m_Doc;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element m_RootElement;
    /**
     * Element for the current test.
     */
    private Hashtable<Description, Element> m_TestElements = new Hashtable<Description, Element>();
    /**
     * tests that failed.
     */
    private Hashtable<Description, Description> m_FailedTests = new Hashtable<Description, Description>();
    /**
     * Timing helper.
     */
    private Hashtable<Description, Long> m_TestStarts = new Hashtable<Description, Long>();
    /**
     * Where to write the XML data out to.
     */
    private OutputStream m_Out;
    
    /**
     * Counter for the number of failures.  
     * JUnit does not differentiate between failures and errors but the XML formatter does. 
     */
    private int m_Failures;
    
    /**
     * Counter for the number of errors.
     */
    private int m_Errors;

    /**
     * Buffered output stream to store standard out during testing.
     */
    private OutputStream m_StdOutContent = new ByteArrayOutputStream();

    /**
     * Buffered output stream to store standard error during testing.
     */
    private OutputStream m_StdErrContent = new ByteArrayOutputStream();
    
    /**
     * Splits the standard out to the actual standard out and to the buffered output to save for later.
     */
    private OutputStream m_TeeOutStream = new TeeOutputStream(System.out, m_StdOutContent);
    
    /**
     * Splits the standard error to the actual standard error and to the buffered output to save for later.
     */
    private OutputStream m_TeeErrStream = new TeeOutputStream(System.err, m_StdErrContent);

    private PrintStream m_OldStdOut;

    private PrintStream m_OldStdErr;

    private static DocumentBuilder getDocumentBuilder()
    {
        try 
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } 
        catch (final Exception exc) 
        {
            throw new ExceptionInInitializerError(exc);
        }
    }

    public void setOutput(final OutputStream out) 
    {
        this.m_Out = out;
    }

    public void setSystemOutput() 
    {
        final String stdOut = m_StdOutContent.toString();
        formatOutput(SYSTEM_OUT, stdOut);
    }

    public void setSystemError() 
    {
        final String stdErr = m_StdErrContent.toString();
        formatOutput(SYSTEM_ERR, stdErr);
    }

    /**
     * The whole test suite started.
     */
    public void startTestSuite(final String name) // NOCHECKSTYLE: no documentation, file modified from 3rd party
    {
        m_Doc = getDocumentBuilder().newDocument();
        m_RootElement = m_Doc.createElement(TESTSUITE);
        m_RootElement.setAttribute(ATTR_NAME, name);

        //add the timestamp
        final String timestamp = DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN);
        m_RootElement.setAttribute(TIMESTAMP, timestamp);
        //and the hostname.
        m_RootElement.setAttribute(HOSTNAME, getHostname());

        // Output properties
        final Element propsElement = m_Doc.createElement(PROPERTIES);
        m_RootElement.appendChild(propsElement);
        final Properties props = System.getProperties();
        if (props != null) 
        {
            final Enumeration<?> e = props.propertyNames();
            while (e.hasMoreElements()) 
            {
                final String propName = (String)e.nextElement();
                final Element propElement = m_Doc.createElement(PROPERTY);
                propElement.setAttribute(ATTR_NAME, propName);
                propElement.setAttribute(ATTR_VALUE, props.getProperty(propName));
                propsElement.appendChild(propElement);
            }
        }
        
        m_OldStdOut = System.out;
        System.setOut(new PrintStream(m_TeeOutStream));
        m_OldStdErr = System.err;
        System.setErr(new PrintStream(m_TeeErrStream));
    }

    /**
     * Get the local hostname.
     * 
     * @return the name of the local host, or "localhost" if we cannot work it out
     */
    private String getHostname()  
    {
        try 
        {
            return InetAddress.getLocalHost().getHostName();
        } 
        catch (final UnknownHostException e) 
        {
            return "localhost";
        }
    }

    /**
     * The whole test suite ended.
     * 
     * @param results
     *      Results of the testing
     * @throws IOException
     *      Thrown if unable to write results to output stream 
     */
    public void endTestSuite(final Result results) throws IOException 
    {
        System.setOut(m_OldStdOut);
        System.setErr(m_OldStdErr);
        
        Logging.log(LogService.LOG_INFO, "%n############%n%s%ntests ran: %d, failures: %d%n############", 
                m_RootElement.getAttribute(ATTR_NAME), results.getRunCount(), results.getFailureCount());
        
        setSystemOutput();
        setSystemError();

        m_RootElement.setAttribute(ATTR_TESTS, "" + results.getRunCount());
        m_RootElement.setAttribute(ATTR_FAILURES, "" + m_Failures);
        m_RootElement.setAttribute(ATTR_ERRORS, "" + m_Errors);
        m_RootElement.setAttribute(ATTR_TIME, "" + (results.getRunTime() / ONE_SECOND));
        if (m_Out != null) 
        {
            Writer wri = null;
            try 
            {
                wri = new BufferedWriter(new OutputStreamWriter(m_Out, "UTF8"));
                wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                (new DOMElementWriter()).write(m_RootElement, wri, 0, "  ");
            } 
            finally 
            {
                if (wri != null) 
                {
                    try 
                    {
                        wri.flush();
                    } 
                    catch (final IOException ex) 
                    { 
                        // ignore
                    }
                }
                if (m_Out != System.out && m_Out != System.err) 
                {
                    wri.close();
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     * @param t the test.
     */
    public void startTest(final Description t) 
    {
        Logging.log(LogService.LOG_INFO, "%n######################%n### STARTING TEST ### %s%n######################", 
                t.getMethodName());
        m_TestStarts.put(t, System.currentTimeMillis());
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     * @param test the test.
     */
    public void endTest(final Description test) 
    {
        // Fix for bug #5637 - if a junit.extensions.TestSetup is
        // used and throws an exception during setUp then startTest
        // would never have been called
        if (!m_TestStarts.containsKey(test)) 
        {
            startTest(test);
        }

        Element currentTest = null;
        if (!m_FailedTests.containsKey(test)) 
        {
            currentTest = m_Doc.createElement(TESTCASE);
            final String name = test.getMethodName();
            currentTest.setAttribute(ATTR_NAME, name == null ? UNKNOWN : name);
            // a TestSuite can contain Tests from multiple classes,
            // even tests with the same name - disambiguate them.
            currentTest.setAttribute(ATTR_CLASSNAME, test.getClassName());
            m_RootElement.appendChild(currentTest);
            m_TestElements.put(test, currentTest);
        } 
        else 
        {
            currentTest = m_TestElements.get(test);
        }

        final Long l = m_TestStarts.get(test);
        currentTest.setAttribute(ATTR_TIME, "" + ((System.currentTimeMillis() - l.longValue()) / ONE_SECOND));
        
        Logging.log(LogService.LOG_INFO, "%n######################%n### COMPLETED TEST ### %s%n######################", 
                test.getMethodName());
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t the exception.
     */
    public void addFailure(final Description test, final Throwable t) 
    {
        formatError(FAILURE, test, t);
        m_Failures++;
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t the assertion.
     */
    public void addFailure(final Description test, final AssertionFailedError t) 
    {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     * @param test the test.
     * @param t the error.
     */
    public void addError(final Description test, final Throwable t) 
    {
        formatError(ERROR, test, t);
        m_Errors++;
    }

    private void formatError(final String type, final Description test, final Throwable t) // NOCHECKSTYLE
    // no documentation, file modified from 3rd party
    {
        if (test != null) 
        {
            endTest(test); // removed already called directly
            m_FailedTests.put(test, test);
        }

        final Element nested = m_Doc.createElement(type);
        Element currentTest = null;
        if (test != null) 
        {
            currentTest = m_TestElements.get(test);
        } 
        else 
        {
            currentTest = m_RootElement;
        }

        currentTest.appendChild(nested);

        final String message = t.getMessage();
        if (message != null && message.length() > 0) 
        {
            nested.setAttribute(ATTR_MESSAGE, t.getMessage());
        }
        nested.setAttribute(ATTR_TYPE, t.getClass().getName());

        /*
           Used to call JUnitTestRunner.getFilteredTrace which would filter the following:
                "junit.framework.TestCase",
                "junit.framework.TestResult",
                "junit.framework.TestSuite",
                "junit.framework.Assert.", // don't filter AssertionFailure
                "junit.swingui.TestRunner",
                "junit.awtui.TestRunner",
                "junit.textui.TestRunner",
                "java.lang.reflect.Method.invoke(",
                "sun.reflect.",
                "org.apache.tools.ant.",
                // JUnit 4 support:
                "org.junit.",
                "junit.framework.JUnit4TestAdapter",
                " more",
                
            Doesn't seem to be necessary at least for Eclipse which filters it out already.
         */
        final String strace = getStackTrace(t);
        final Text trace = m_Doc.createTextNode(strace);
        nested.appendChild(trace);
    }

    private void formatOutput(String type, String output)
    {
        final Element nested = m_Doc.createElement(type);
        m_RootElement.appendChild(nested);
        nested.appendChild(m_Doc.createCDATASection(output));
    }

    /**
     * Converts the stack trace from the {@link Throwable} item to a string. 
     * @param t
     *      {@link Throwable} object containing the stack trace
     * @return
     *      string representation of the stack trace
     */
    public static String getStackTrace(final Throwable t) 
    {
        final StringWriter sw = new StringWriter();
        try (final PrintWriter pw = new PrintWriter(sw, true))
        {
            t.printStackTrace(pw);
            pw.flush();
        }
        return sw.toString();
    }

    public void cleanup()
    {
        System.setOut(m_OldStdOut);
        System.setErr(m_OldStdErr);
    }
}
