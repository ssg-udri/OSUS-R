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
// This file contains the PhysicalLinkException class.  Generated from 
// Enterprise Architect.  The Enterprise Architect model should be updated for 
// all non-implementation changes (function names, arguments, notes, etc.) and 
// re-synced with the code.
//
//==============================================================================
package mil.dod.th.core.ccomm.physical;

/**
 * Base exception for PhysicalLinks.  It by itself will be thrown or a subclass of it.
 * 
 * @author dhumeniuk
 */
public class PhysicalLinkException extends Exception
{
    /**
     * Unique id for this exception.
     */
    private static final long serialVersionUID = 1050950782421446509L;

    /**
     * Constructor to throw exception when no other exception caused it.
     * 
     * @param message
     *            message to associate with the exception
     */
    public PhysicalLinkException(final String message)
    {
        super(message);
    }

    /**
     * Used to throw PhysicalLinkException that was caused by some other
     * exception.
     * 
     * @param message
     *            message to associate with the exception
     * @param cause
     *            what underlying error caused the exception
     */
    public PhysicalLinkException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
