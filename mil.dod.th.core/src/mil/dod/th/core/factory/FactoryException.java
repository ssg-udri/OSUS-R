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
package mil.dod.th.core.factory;

/**
 * General exception class for factory related exceptions.
 * 
 * @author dhumeniuk
 *
 */
public class FactoryException extends Exception
{
    /**
     * UID for serialization.
     */
    private static final long serialVersionUID = -5189630707298420820L;

    /**
     * Default constructor.
     * 
     * @param message
     *      Message to associate with the exception
     */
    public FactoryException(final String message)
    {
        super(message);
    }
    
    /**
     * Basic constructor to keep the cause only, use if exception is caused by another and just wrapping exception.
     * 
     * @param cause
     *      Cause of the exception
     */
    public FactoryException(final Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor for exceptions with a message and caused by another exception.
     * 
     * @param message
     *      Message to associate with the exception
     * @param cause
     *      Exception that caused this exception to be created 
     */
    public FactoryException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
