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
//
// DESCRIPTION:
// This file contains the TimeoutException exception class.  Generated from  
// Enterprise Architect.  The Enterprise Architect model should be updated for  
// all non-implementation changes (function names, arguments, notes, etc.) and 
// re-synced with the code.
//
//==============================================================================
package mil.dod.th.core.ccomm.physical;

import java.io.IOException;

/**
 * Specific exception for a timeout for methods that throw IOException such as
 * InputStream.read().
 * 
 * @author dhumeniuk
 */
public class TimeoutException extends IOException
{
    /**
     * Unique id for this exception.
     */
    private static final long serialVersionUID = -4473169421943311783L;

    /**
     * Constructor used to throw exception.
     * 
     * @param message
     *            - message to associate with the exception.
     */
    public TimeoutException(final String message)
    {
        super(message);
    }
}
