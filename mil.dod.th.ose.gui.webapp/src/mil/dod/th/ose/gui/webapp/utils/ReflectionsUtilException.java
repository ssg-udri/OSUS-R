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
package mil.dod.th.ose.gui.webapp.utils;

/**
 * This exception signals that there has been some sort of error while using a Java Reflections method within the 
 * {@link ReflectionsUtil}.
 * 
 * @author cweisenborn
 */
public class ReflectionsUtilException extends Exception
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 8108167838359000777L;
    
    /**
     * Default no argument constructor for the exception. Calls the super classes no argument constructor.
     */
    public ReflectionsUtilException()
    {
        super();
    }
    
    /**
     * Constructor method that accepts a string as the argument which is used to display error information. This class 
     * calls the super classes constructor that accepts a string as an argument.
     * 
     * @param error
     *          String containing information to be displayed about the error.
     */
    public ReflectionsUtilException(final String error)
    {
        super(error);
    }
    
    /**
     * Constructor method accepting a {@link Throwable} as an argument.
     * @param cause
     *          The cause which is saved for later retrieval.
     */
    public ReflectionsUtilException(final Throwable cause)
    {
        super(cause);
    }
    
    /**
     * Constructor method accepting a string and {@link Throwable} as an argument.
     * 
     * @param error
     *          The string containing information to be displayed about the error.
     * @param cause
     *          The cause of the error which is saved for later retrieval.
     */
    public ReflectionsUtilException(final String error, final Throwable cause)
    {
        super(error, cause);
    }
}
