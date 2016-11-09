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
package mil.dod.th.core.pm;

/**
 * General exception class for the power management package.
 * 
 * @author dhumeniuk
 *
 */
public class PowerManagerException extends Exception
{

    /**
     * UID for serialization.  Update when changes dictate.
     */
    private static final long serialVersionUID = 9198175225347210458L;

    /**
     * Default constructor.
     * 
     * @param cause
     *      cause of this exception
     */
    public PowerManagerException(final Exception cause)
    {
        super(cause);
    }

}
