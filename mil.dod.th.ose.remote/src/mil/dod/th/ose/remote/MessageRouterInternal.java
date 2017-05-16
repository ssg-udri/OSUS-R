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

import mil.dod.th.core.remote.RemoteChannelLookup;

/**
 * Contains internal functions available to the {@link mil.dod.th.core.remote.messaging.MessageRouter} implementation.
 * Includes functions that should not be part of the {@link mil.dod.th.core.remote.messaging.MessageRouter} interface 
 * available to outside consumers, but is needed internally.
 * 
 * @author cweisenborn
 */
public interface MessageRouterInternal
{
    /**
     * Bind a message service with the router for sending messages.  Router will send all messages where the namespace
     * is {@link MessageService#getNamespace()}.
     * 
     * @param messageService
     *      service that will handle messages
     */
    void bindMessageService(MessageService messageService);
    
    /**
     * Unbind a message service with the router for sending messages.  Router will no longer handle message for the 
     * namespace {@link MessageService#getNamespace()}.
     * 
     * @param messageService
     *      service that will no longer handle messages
     */
    void unbindMessageService(MessageService messageService);
    
    /**
     * Binds the remote channel lookup with the router for sending messages. Router will use the remote channel lookup
     * to attempt to forward any messages that don't belong to the system.
     * 
     * @param lookup
     *      remote channel lookup service to be bound
     */
    void bindRemoteChannelLookup(RemoteChannelLookup lookup);
    
    /**
     * Unbinds the remote channel lookup with the router for sending messages. Router will no longer attempt to foward
     * messages that don't belong to the system.
     * 
     * @param lookup
     *      remote channel lookup service to be unbound
     */
    void unbindRemoteChannelLookup(RemoteChannelLookup lookup);
}
