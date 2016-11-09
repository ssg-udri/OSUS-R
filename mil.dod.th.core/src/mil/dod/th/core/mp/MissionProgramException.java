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
package mil.dod.th.core.mp;

/**
 * Exception class for the {@link MissionProgramManager} service.
 * 
 * @author dhumeniuk
 *
 */
public class MissionProgramException extends Exception
{

    /**
     * Generated identifier, update when serialization interface changes.
     */
    private static final long serialVersionUID = -7548877173685133930L;

    /**
     * Default constructor where exception is based on cause of another exception.
     * 
     * @param cause
     *      cause of the exception
     */
    public MissionProgramException(final Throwable cause)
    {
        super(cause);
    }

}
