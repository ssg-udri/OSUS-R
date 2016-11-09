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

package mil.dod.th.core.ccomm;

import mil.dod.th.core.factory.FactoryException;

/**
 * This exception is used when there are common problems found within the
 * CustomComms package and sub packages.
 */
public class CCommException extends FactoryException
{
    /**
     * Generated UID.
     */
    private static final long serialVersionUID = -6676107656595214150L;

    /**
     * FormatProblem variable to hold the enum FormatProblem passed to one of
     * the overloaded constructors if one was sent with the exception.
     */
    final private FormatProblem m_FormatProblem;

    /**
     * Create a new malformed message exception. 
     * 
     * @param type from the enum {@link FormatProblem}
     */
    public CCommException(final FormatProblem type)
    {
        super(type.toString());
        
        m_FormatProblem = type;
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param message The error message.
     * @param type from the enum {@link FormatProblem}
     */
    public CCommException(final String message, final FormatProblem type)
    {
        super(getMessage(message, type));
        
        m_FormatProblem = type;
        
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param message The error message.
     * @param cause A possible initial exception caught before this is thrown.
     * @param type from the enum {@link FormatProblem}
     */
    public CCommException(final String message, final Throwable cause,
            final FormatProblem type)
    {
        super(getMessage(message, type), cause);

        m_FormatProblem = type;
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param cause A possible initial exception caught before this is thrown.
     * @param type from the enum {@link FormatProblem}
     */
    public CCommException(final Throwable cause, final FormatProblem type)
    {
        super(type.toString(), cause);
        
        m_FormatProblem = type;
    }

    /**
     * Get the format problem that caused this exception.
     * 
     * @return The enum which represents the problem 
     *  encountered. 
     */
    public FormatProblem getFormatProblem()
    {
        return m_FormatProblem;
    }

    /**
     * This enum defines the types of problems.
     */
    public enum FormatProblem
    {
        /** Address Mismatch. */
        ADDRESS_MISMATCH,

        /** Address Type. */
        ADDRESS_TYPE,

        /** Buffer Overflow. */
        BUFFER_OVERFLOW,

        /** Buffer Underflow. */
        BUFFER_UNDERFLOW,

        /** Inactive. */
        INACTIVE,

        /** Invalid crc. */
        INVALID_CRC,

        /** Invalid Header. */
        INVALID_HEADER,

        /** Invalid Payload. */
        INVALID_PAYLOAD,

        /** Invalid Size. */
        INVALID_SIZE,

        /** Timeout. */
        TIMEOUT,
        
        /** Condition other than described by other values. */
        OTHER;

    }
    
    /**
     * Construct a message from the base message and the format problem type.
     * 
     * @param message
     *      Base exception message
     * @param type
     *      Type of format problem to add to message
     * @return
     *      A string constructed from the base message and the format problem type
     */
    private static String getMessage(final String message, final FormatProblem type)
    {
        return message + ":" + type;
    }
}
