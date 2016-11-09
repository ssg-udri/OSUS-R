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
package edu.udayton.udri.asset.novatel.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import edu.udayton.udri.asset.novatel.message.NovatelMessageException.FormatProblem;

/**
 * Test class for {@link NovatelMessageException}.
 * @author allenchl
 *
 */
public class TestNovatelMessageException
{
    /**
     * Test creating an exception and retrieving the message from the exception.
     */
    @Test
    public void testRetrieveExcetionMessage()
    {
        //error message
        String message = "blue looks red";
        NovatelMessageException ex = new NovatelMessageException(message, FormatProblem.INCOMPLETE_INS_MESSAGE);
        
        assertThat(ex.getMessage(), is(message + ": INCOMPLETE_INS_MESSAGE"));
    }
    
    /**
     * Test creating an exception with only the format problem and retrieving the "format problem" from the exception.
     */
    @Test
    public void testGetFormatProblem()
    {
        NovatelMessageException ex = new NovatelMessageException(FormatProblem.INCOMPLETE_INS_MESSAGE);
        
        assertThat(ex.getFormatProblem(), is(FormatProblem.INCOMPLETE_INS_MESSAGE));
    }
    
    /**
     * Test creating an exception with a throwable and message and format problem.
     * Verify all fields can be retrieved.
     */
    @Test
    public void testFullyInitConstructor()
    {
        Throwable throwable = new Throwable("illegal");
        NovatelMessageException ex = 
                new NovatelMessageException("derp", throwable, FormatProblem.INCOMPLETE_INS_MESSAGE);
        
        //verify fields
        assertThat(ex.getMessage(), is("derp" + ": INCOMPLETE_INS_MESSAGE"));
        assertThat(ex.getFormatProblem(), is(FormatProblem.INCOMPLETE_INS_MESSAGE));
        assertThat(ex.getCause(), is(throwable));
    }
    
    /**
     * Test creating an exception with a throwable and "format problem".
     * Verify fields can be retrieved.
     */
    @Test
    public void testThrowableAndProblem()
    {
        Throwable throwable = new Throwable("illegal");
        NovatelMessageException ex = 
                new NovatelMessageException(throwable, FormatProblem.INCOMPLETE_INS_MESSAGE);
        
        //verify fields
        assertThat(ex.getFormatProblem(), is(FormatProblem.INCOMPLETE_INS_MESSAGE));
        assertThat(ex.getCause(), is(throwable));
    }
}
