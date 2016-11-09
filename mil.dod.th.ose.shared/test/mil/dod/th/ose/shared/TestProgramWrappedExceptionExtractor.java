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
package mil.dod.th.ose.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.security.PrivilegedActionException;

import org.junit.Test;

import mil.dod.th.core.commands.CommandExecutionException;

/**
 * Test extracting the inner most message of an exception.
 * @author callen
 *
 */
public class TestProgramWrappedExceptionExtractor
{
    /**
     * Test extracting the original message of an exception.
     * Verify expected message is returned.
     */
    @Test
    public void testGetRootMessage()
    {
        PrivilegedActionException e = new PrivilegedActionException(new CommandExecutionException("Unsupported!"));
        
        final String message = WrappedExceptionExtractor.getRootCauseMessage(e);
        
        assertThat(message, is("Unsupported!"));
    }
    
    /**
     * Test extracting the original message of an exception. One layer, IE not a wrapped exception.
     * Verify expected message is returned.
     */
    @Test
    public void testGetMessage()
    {
        CommandExecutionException e = new CommandExecutionException("Unsupported!");
        
        final String message = WrappedExceptionExtractor.getRootCauseMessage(e);
        
        assertThat(message, is("Unsupported!"));
    }
    
    /**
     * Test extracting the original message of a three layer exception.
     * Verify expected message is returned.
     */
    @Test
    public void testGetRootMessageThreeExceptions()
    {
        PrivilegedActionException e = new PrivilegedActionException(
                new Exception("I am an exception message tooooo", 
                        new CommandExecutionException("Unsupported!")));
        
        final String message = WrappedExceptionExtractor.getRootCauseMessage(e);
        
        assertThat(message, is("Unsupported!"));
        assertThat(message, is(not("I am an exception message toooo")));
    }
}
