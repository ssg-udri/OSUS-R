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
package mil.dod.th.ose.remote;

/**
 * An exception to handle issues with the Remote Interface.
 * @author jgold
 *
 */
public class RemoteInterfaceException extends Exception 
{
    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -7688924330746856055L;

    /**
     * The exception message.
     * @param msg the message describing this exception.
     */
    public RemoteInterfaceException(final String msg) 
    {
        super(msg);
    }
}
