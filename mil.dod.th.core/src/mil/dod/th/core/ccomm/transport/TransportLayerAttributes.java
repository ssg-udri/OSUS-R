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
package mil.dod.th.core.ccomm.transport;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link TransportLayer}s. Retrieve properties using {@link 
 * TransportLayer#getConfig()}.
 * 
 * @author dhumeniuk
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface TransportLayerAttributes
{
    /** Configuration property key for the transport layer timeout. */
    String CONFIG_PROP_READ_TIMEOUT_MS = FactoryObject.TH_PROP_PREFIX + ".read.timeout.ms";
    
    /** Configuration property key for the name of the transport layer's link layer name. */
    String CONFIG_PROP_LINK_LAYER_NAME = FactoryObject.TH_PROP_PREFIX + ".link.layer.name";
    
    /**
     * Configuration property for the timeout in milliseconds during read calls.
     * 
     * @return timeout in milliseconds for timeout or 0 for block forever
     */
    @AD(required = false, deflt = "0", id = CONFIG_PROP_READ_TIMEOUT_MS,
        description = "Timeout value for read calls in milliseconds")
    int readTimeoutMs();
    
    /**
     * Configuration property for the name of the {@link mil.dod.th.core.ccomm.link.LinkLayer} used by this 
     * {@link TransportLayer}.
     * 
     * @return name of the {@link mil.dod.th.core.ccomm.link.LinkLayer}
     */
    @AD(required = false, deflt = "", id = CONFIG_PROP_LINK_LAYER_NAME,
        description = "Name of the link layer used by the transport layer")
    String linkLayerName();
}
