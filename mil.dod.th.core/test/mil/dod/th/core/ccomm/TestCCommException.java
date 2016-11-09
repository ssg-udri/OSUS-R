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
//
// DESCRIPTION:
// This test class is used to test the CComm Exception class
//
//==============================================================================
package mil.dod.th.core.ccomm;

import static org.junit.Assert.assertEquals;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;

import org.junit.Test;

public class TestCCommException
{
    private String message = "Test Exception Message :";

    @Test
    public void testCCommExceptionFormatProblem()
    {
        CCommException e = new CCommException(FormatProblem.INVALID_SIZE);

        assertEquals("getFormatProblem not the expected value: ", e.getFormatProblem(),
                FormatProblem.INVALID_SIZE);
    }

    @Test
    public void testCCommExceptionStringFormatProblem()
    {
        CCommException e = new CCommException(message, FormatProblem.TIMEOUT);

        assertEquals("getFormatProblem not the expected value: ", e.getFormatProblem(),
                FormatProblem.TIMEOUT);
        // CCommException : Test Exception Message :: TIMEOUT
    }

    @Test(expected = CCommException.class)
    public void testCCommExceptionStringThrowableFormatProblem() throws CCommException
    {
        NullPointerException npe = new NullPointerException();
        CCommException e = new CCommException(message, npe, FormatProblem.BUFFER_UNDERFLOW);
        assertEquals("getFormatProblem not the expected value: ",
                e.getFormatProblem(), FormatProblem.BUFFER_UNDERFLOW);
        assertEquals("getCause not the expected value: ", e.getCause(), npe);
        
        throw e;
    }

    @Test
    public void testCCommExceptionThrowableFormatProblem()
    {
        Exception ex = new Exception();
        CCommException e = new CCommException(ex, FormatProblem.ADDRESS_TYPE);
        assertEquals("getFormatProblem not the expected value: ",
                e.getFormatProblem(), FormatProblem.ADDRESS_TYPE);
    }
}
