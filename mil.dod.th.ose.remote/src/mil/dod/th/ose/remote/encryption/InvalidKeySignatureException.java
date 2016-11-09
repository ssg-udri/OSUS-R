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
package mil.dod.th.ose.remote.encryption;

/**
 * Exception class that is specifically used to handle Invalid key signatures, where the
 * key signature can not be verified during decrypting a message and we would end up
 * throwing an invalid key signature.
 * @author powarniu
 *
 */
public class InvalidKeySignatureException extends Exception
{
    /**
     * UID for serialization.
     */
    private static final long serialVersionUID = 3161311150332974628L;

   /**
     * Default constructor.
     * 
     * @param message
     *      Message to associate with the exception
     */
    public InvalidKeySignatureException(final String message)
    {
        super(message);
    }

    /**
     * Constructor for exceptions with a message and caused by another exception.
     * 
     * @param message
     *      Message to associate with the exception
     * @param cause
     *      Exception that caused this exception to be created 
     */
    public InvalidKeySignatureException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
