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

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.junit4xmltestrunner.ant.XMLJUnitResultFormatter;

import org.osgi.service.log.LogService;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Class listens to JUnit runs and logs the information to the given formatter.
 * 
 * @author dhumeniuk
 */
final class XMLFormatterListener extends RunListener
{
    /**
     * Reference to the formatter that creates the XML file.
     */
    private final XMLJUnitResultFormatter m_Formatter;
    
    /**
     * Whether to print the stack trace on failures.
     */
    private final boolean m_PrintStackTrace;
    
    /**
     * Main constructor.
     * 
     * @param formatter
     *      Formatter to use to inform of testing
     * @param printStackTrace
     *      whether to print stack trace on failures
     */
    XMLFormatterListener(final XMLJUnitResultFormatter formatter, final boolean printStackTrace)
    {
        super();
        m_Formatter = formatter;
        m_PrintStackTrace = printStackTrace;
    }

    @Override
    public void testFailure(final Failure failure)
    {
        final Class<? extends Throwable> failureClass = failure.getException().getClass();
        final String failureMessage = String.format("### FAILURE ### %s", failureClass.toString());
        if (m_PrintStackTrace)
        {
            Logging.log(LogService.LOG_ERROR, failure.getException(), failureMessage);
        }
        else
        {
            Logging.log(LogService.LOG_ERROR, failureMessage);
        }
        if (failureClass.equals(AssertionError.class))
        {
            m_Formatter.addFailure(failure.getDescription(), failure.getException());
        }
        else
        {
            m_Formatter.addError(failure.getDescription(), failure.getException());
        }
    }

    @Override
    public void testStarted(final Description description)
    {
        m_Formatter.startTest(description);
    }

    @Override
    public void testFinished(final Description description)
    {
        m_Formatter.endTest(description);
    }
}
