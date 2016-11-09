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
package mil.dod.th.core.persistence;

/**
 * PersistenceFailedException is a runtime exception, for the persistence API, to be thrown if a persistence call fails.
 */
public class PersistenceFailedException extends RuntimeException
{
    /**
     * Generated serial version unique identifier.
     */
    private static final long serialVersionUID = 2733732209621638542L;

    /**
     * Default constructor.
     */
    public PersistenceFailedException()
    {
        super();
    }

    /**
     * Constructs a new PersistenceFailedException with the specified detail message.
     * 
     * @param message
     *            the specified detail message
     */
    public PersistenceFailedException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a new PersistenceFailedException with the specified cause and a detail message.
     * 
     * @param message
     *            the specified detail message
     * @param cause
     *            the original Throwable exception associated with this persistence failure
     */
    public PersistenceFailedException(final String message,
                                      final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new PersistenceFailedException with the specified cause.
     * 
     * @param cause
     *            the original Throwable exception associated with this persistence failure
     */
    public PersistenceFailedException(final Throwable cause)
    {
        super(cause);
    }

}
