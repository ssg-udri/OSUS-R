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

import mil.dod.th.core.mp.EventHandlerHelper;

import org.apache.felix.service.command.Descriptor;

/**
 * Event handler helper commands.
 * 
 * @author cweisenborn
 */
@Component(provide = EventHandlerHelperCommands.class, properties = {"osgi.command.scope=ehh", 
        "osgi.command.function=unregisterAllHandlers" })
public class EventHandlerHelperCommands
{
    /**
     * Reference to the service.
     */
    private EventHandlerHelper m_EventHandlerHelper;
    
    /**
     * Sets the EventHandlerHelper service.
     * 
     * @param eventHandlerHelper
     *              service to be set
     */
    @Reference
    public void setEventHandlerHelper(final EventHandlerHelper eventHandlerHelper)
    {
        m_EventHandlerHelper = eventHandlerHelper;
    }
    
    /**
     * Unregister all previously registered handlers.
     */
    @Descriptor("Unregister all previously registered handlers.")
    public void unregisterAllHandlers()
    {
        m_EventHandlerHelper.unregisterAllHandlers();
    }
}
