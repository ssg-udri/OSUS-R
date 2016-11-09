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
package mil.dod.th.core.remote.objectconverter;


/**
 * This exception signals that some error has occurred in the process of converting a JAXB object into a Google
 * Protocol Buffer message.
 * 
 * @author cweisenborn
 */
public class ObjectConverterException extends Exception
{
    /**
     * UID for serialization.
     */
    private static final long serialVersionUID = 2958897121582737760L;

    /**
     * Constructor method that accepts a string as a parameter. The string is the message detailing the exception.
     * 
     * @param message
     *          The message detailing the specifics of the exception.
     */
    public ObjectConverterException(final String message)
    {
        super(message);
    }

    /**
     * Constructor method that accepts a throwable as a parameter. 
     * 
     * @param cause
     *          The exception responsible for the ObjectConvertException being thrown.
     */
    public ObjectConverterException(final Throwable cause)
    {
        super(cause);
    }
    
    
    /**
     * Constructor method that accepts a string and a throwable as parameters. The string details the reason the 
     * exception was thrown and the throwable is the exception responsible for causing the ObjectConverterException.
     * 
     * @param message
     *          The message detailing the specifics of the exception.
     * @param cause
     *          The exception responsible for the ObjectConvertException being thrown.
     */
    public ObjectConverterException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
