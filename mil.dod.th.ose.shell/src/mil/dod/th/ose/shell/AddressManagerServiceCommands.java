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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Address Lookup Commands.
 * 
 * @author cweisenborn
 *
 */
@Component(provide = AddressManagerServiceCommands.class, properties = {"osgi.command.scope=thaddr", 
        "osgi.command.function=getOrCreateAddress|printDeep" })
public class AddressManagerServiceCommands
{
    /**
     * Reference to the AddressManagerService service.
     */
    private AddressManagerService m_AddressManagerService;
    
    
    /**
     * Set the address lookup service to use.
     * 
     * @param AddressManagerService the m_AddressManagerService to set
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService AddressManagerService)
    {
        m_AddressManagerService = AddressManagerService;
    }
    
    /**
     * Creates and adds an address to the lookup table.
     * 
     * @param addressDescription
     *                      string representation of an address
     * @return created address
     * @throws CCommException
     *                operational error related to the address      
     */
    @Descriptor("Returns an address or creates one if none is found.")
    public Address getOrCreateAddress(
            @Descriptor("String representation of an address.")
            final String addressDescription) throws CCommException
    {
        return m_AddressManagerService.getOrCreateAddress(addressDescription);
    }
    
    /**
     * Print status of service to console.
     * 
     * @param session
     *      command session containing the console 
     */
    @Descriptor("Display status of the service console.")
    public void printDeep(final CommandSession session)
    {
        m_AddressManagerService.printDeep(session.getConsole());
    }
}
