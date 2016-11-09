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
package mil.dod.th.ose.jaxbprotoconverter;

/**
 * This exception signals that there has been some sort of error during the process of converting between JAXB and
 * Google Protocol Buffer files.
 * 
 * @author cweisenborn
 */
@SuppressWarnings("serial")
public class JaxbProtoConvertException extends Exception
{
    /**
     * Reference to a string that is used to store information passed to the exception.
     */
    private final String m_Message;
    
    /**
     * Default no argument constructor for the exception. Calls the super classes no argument constructor.
     */
    public JaxbProtoConvertException()
    {
        super();
        m_Message = null;
    }
    
    /**
     * Constructor method that accepts a string as the argument which is used to display error information. This class 
     * calls the super classes constructor that accepts a string as an argument.
     * 
     * @param error
     *          String containing information to be displayed about the error.
     */
    public JaxbProtoConvertException(final String error)
    {
        super(error);
        m_Message = error;
    }
    
    /**
     * Constructor method accepting a {@link Throwable} as an argument.
     * @param cause
     *          The cause which is saved for later retrieval.
     */
    public JaxbProtoConvertException(final Throwable cause)
    {
        super(cause);
        m_Message = null;
    }
    
    /**
     * Constructor method accepting a string and {@link Throwable} as an argument.
     * 
     * @param error
     *          The string containing information to be displayed about the error.
     * @param cause
     *          The cause of the error which is saved for later retrieval.
     */
    public JaxbProtoConvertException(final String error, final Throwable cause)
    {
        super(error, cause);
        m_Message = error;
    }
    
    @Override
    public String getMessage()
    {
        return m_Message;
    }
}
