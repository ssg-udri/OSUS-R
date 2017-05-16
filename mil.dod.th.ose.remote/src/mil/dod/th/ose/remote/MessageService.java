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

import java.io.IOException;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Handle a message for given namespace. This interface shall be implemented for each namespace.
 * 
 * @author Dave Humeniuk
 */
@ProviderType
public interface MessageService
{
    /**
     * Get the namespace for this service.
     * 
     * @return
     *      What namespace the service handles
     */
    Namespace getNamespace();
    
    /**
     * Handle the message for a particular namespace.
     * 
     * Each message service will handle messages differently, but at a minimum must post an event for the remote 
     * message.  The event will contain the inner message (e.g., BaseNamespace and message type).
     * 
     * @param message
     *      message that was received and routed for the particular namespace
     * @param payload
     *      payload that was received
     * @param channel
     *      channel the message was received on
     * @throws IOException
     *      if the message cannot be handled properly
     * @throws ObjectConverterException
     *      if an object could not be converted between JAXB and protocol buffer format
     * @throws UnmarshalException
     *      if an object could not be converted from XML into JAXB
     */
    void handleMessage(TerraHarvestMessage message, TerraHarvestPayload payload,  RemoteChannel channel) 
            throws IOException, ObjectConverterException, UnmarshalException;

}
