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

/**
 * This class is used to get the actual exceptions out of the wrapped exceptions generally encountered with mission
 * programming interactions.
 * @author callen
 *
 */
public final class WrappedExceptionExtractor
{
    /**
     * Private constructor that prevents instantiation. 
     */
    private WrappedExceptionExtractor()
    {
        //empty because this is a utility class
    }
    
    /**
     * Get the root message from a wrapped exception. 
     * @param exception
     *     the outer most exception to unwrap
     * @return
     *     the root message
     */
    public static String getRootCauseMessage(final Throwable exception)
    {
        final Throwable innerThrowable = exception.getCause();
        if (innerThrowable == null)
        {
            return exception.getMessage();
        }
        else
        {
            return getRootCauseMessage(innerThrowable);
        }
    }
}
