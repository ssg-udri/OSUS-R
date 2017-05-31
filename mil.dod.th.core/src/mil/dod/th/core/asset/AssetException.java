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
package mil.dod.th.core.asset;

import mil.dod.th.core.factory.FactoryException;

/**
 * General exception class used for asset related exceptions.
 * 
 * @author dhumeniuk
 */
public class AssetException extends FactoryException
{
    /**
     * UID for serialization.
     */
    private static final long serialVersionUID = -2183311992432269280L;

    /**
     * Basic constructor to keep a message only, use if exception is not caused by another exception.
     * 
     * @param message
     *      Message to explain exception
     */
    public AssetException(final String message)
    {
        super(message);
    }

    /**
     * Basic constructor to keep the cause only, use if exception is caused by another and just wrapping exception.
     * 
     * @param cause
     *      Cause of the exception
     */
    public AssetException(final Throwable cause)
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
    public AssetException(final String message, final Throwable cause)
    {
        super(message, cause);
    }    
}
