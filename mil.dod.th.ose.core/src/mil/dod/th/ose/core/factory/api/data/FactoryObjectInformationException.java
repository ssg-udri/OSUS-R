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
package mil.dod.th.ose.core.factory.api.data;

/**
 * Wrapper exception thrown when an attempt to persist factory object information fails.
 * @author callen
 *
 */
public class FactoryObjectInformationException extends Exception
{

    /**
     * The serial UUID for this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor to keep a message only, use if exception is not caused by another exception.
     * 
     * @param message
     *      Message to explain exception
     */
    public FactoryObjectInformationException(final String message)
    {
        super(message);
    }

    /**
     * Basic constructor to keep the cause only, use if exception is caused by another and just wrapping exception.
     * 
     * @param cause
     *      Cause of the exception
     */
    public FactoryObjectInformationException(final Throwable cause)
    {
        super(cause);
    }

    /**
     * Basic constructor to keep a message to explain the exception and the cause of the exception.
     * 
     * @param message
     *      Message to explain exception
     * @param cause
     *      Cause of the exception
     */
    public FactoryObjectInformationException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
