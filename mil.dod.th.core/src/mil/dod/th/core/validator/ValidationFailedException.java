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
package mil.dod.th.core.validator;

/**
 * ValidationFailedException is a checked exception to be thrown if validation fails.
 */
public class ValidationFailedException extends Exception
{
    /**
     * Generated serial version unique identifier.
     */
    private static final long serialVersionUID = 4043314925279373870L;

    /**
     * Default constructor.
     */
    public ValidationFailedException()
    {
        super();
    }

    /**
     * Constructs a new ValidationFailedException with the specified detail message.
     * 
     * @param message
     *            the specified detail message
     */
    public ValidationFailedException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a new ValidationFailedException with the specified cause and a detail message.
     * 
     * @param message
     *            the specified detail message
     * @param cause
     *            the original Throwable exception associated with this validation failure
     */
    public ValidationFailedException(final String message,
                                     final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new ValidationFailedException with the specified cause.
     * 
     * @param cause
     *            the original Throwable exception associated with this validation failure
     */
    public ValidationFailedException(final Throwable cause)
    {
        super(cause);
    }

}
