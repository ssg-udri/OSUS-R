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
package mil.dod.th.ose.shell;

import java.io.PrintStream;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.remote.api.RemoteEventRegistration;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;


/**
 * Class that supports access to remote event registrations.
 * @author callen
 *
 */
@Component(provide = RemoteEventAdminCommands.class, properties = {"osgi.command.scope=thremote", 
        "osgi.command.function=eventRegs" })
public class RemoteEventAdminCommands
{
    /**
     * Reference to the remote event admin service.
     */
    private RemoteEventAdmin m_RemoteEventAdmin;
    
    /**
     * Set the remote event admin service to use. 
     * 
     * @param remoteEventAdmin 
     *     the remote event admin service to use
     */
    @Reference
    public void setRemoteEventAdmin(final RemoteEventAdmin remoteEventAdmin)
    {
        m_RemoteEventAdmin = remoteEventAdmin;
    }
    
    
    /**
     * Prints all remote event registrations.
     * @param session
     *      provides access to the Gogo shell session
     */
    @Descriptor("Print all remote event admin registrations.")
    public void eventRegs(final CommandSession session)
    {
        final PrintStream out = session.getConsole();
        final Map<Integer, RemoteEventRegistration> regs = m_RemoteEventAdmin.getRemoteEventRegistrations();
        for (Integer regId : regs.keySet())
        {
            out.printf("Registration ID: %d\n%s", regId, regs.get(regId));
        }
    }
}
