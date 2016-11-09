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
package mil.dod.th.core.transcoder;

/**
 * Class to handle exceptions related to {@link mil.dod.th.core.transcoder.TranscoderService}.
 * 
 * @author jmiller
 *
 */
public class TranscoderException extends Exception
{

    /**
     * UID for serialization.
     */
    private static final long serialVersionUID = 1469760571421644494L;
    
    /**
     * Basic constructor to keep a message only; use only if exception is not
     * caused by another exception.
     * 
     * @param msg
     *      message to explain exception
     */
    public TranscoderException(final String msg)
    {
        super(msg);
    }
    
    /**
     * Basic constructor to keep a message to explain the exception and the cause
     * of the exception.
     * 
     * @param msg
     *      message to explain exception
     * @param cause
     *      cause of the exception
     */
    public TranscoderException(final String msg, final Throwable cause)
    {
        super(msg, cause);
    }

}
