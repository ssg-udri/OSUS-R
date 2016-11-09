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
package edu.udayton.udri.asset.novatel.message;

/**
 * This class is used when there are issues processing the INS data from the device.
 * @author allenchl
 *
 */
public class NovatelMessageException extends Exception
{
    /**
     * Generated UUID.
     */
    private static final long serialVersionUID = 2427361549053887039L;

    /**
     * FormatProblem variable to hold the enum FormatProblem passed to one of
     * the overloaded constructors.
     */
    final private FormatProblem m_FormatProblem;

    /**
     * Create a new malformed message exception. 
     * 
     * @param type 
     *      from the enum {@link FormatProblem}
     */
    public NovatelMessageException(final FormatProblem type)
    {
        super(type.toString());
        
        m_FormatProblem = type;
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param message 
     *      error message
     * @param type 
     *      from the enum {@link FormatProblem}
     */
    public NovatelMessageException(final String message, final FormatProblem type)
    {
        super(getMessage(message, type));
        
        m_FormatProblem = type;
        
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param message 
     *      error message
     * @param cause 
     *      possible initial exception caught before this is thrown
     * @param type 
     *      from the enum {@link FormatProblem}
     */
    public NovatelMessageException(final String message, final Throwable cause,
            final FormatProblem type)
    {
        super(getMessage(message, type), cause);

        m_FormatProblem = type;
    }

    /**
     * Create a new malformed message exception. 
     * 
     * @param cause
     *      the possible initial exception caught before this is thrown
     * @param type
     *      from the enum {@link FormatProblem}
     */
    public NovatelMessageException(final Throwable cause, final FormatProblem type)
    {
        super(type.toString(), cause);
        
        m_FormatProblem = type;
    }
    
    /**
     * Get the format problem that caused this exception.
     * 
     * @return 
     *      the enum which represents the problem encountered 
     */
    public FormatProblem getFormatProblem()
    {
        return m_FormatProblem;
    }
    
    /**
     * Construct a message from the base message and the format problem type.
     * 
     * @param message
     *      base exception message
     * @param type
     *      type of format problem to add to message
     * @return
     *      A string constructed from the base message and the format problem type
     */
    private static String getMessage(final String message, final FormatProblem type)
    {
        return message + ": " + type;
    }
    
    /**
     * This enum defines the types of problems that may occur while parsing ins messages.
     */
    public enum FormatProblem
    {
        /** UTC Offset not known. */
        UTC_OFFSET_UNKNOWN,

        /** Incomplete INSPVAA message. */
        INCOMPLETE_INS_MESSAGE,

        /** Incomplete TIMEA message. */
        INCOMPLETE_TIME_MESSAGE,

        /** INS solution not good. */
        INS_STATUS_NOT_GOOD,

        /** Time offset not reliable. */
        TIME_RELIABILITY,
        
        /** When a numeric value within a message cannot be properly parsed. */
        PARSING_ERROR,
        
        /** Condition other than described by other values. */
        OTHER;

    }
}
