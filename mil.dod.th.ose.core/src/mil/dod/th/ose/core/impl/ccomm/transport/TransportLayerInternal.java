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
package mil.dod.th.ose.core.impl.ccomm.transport;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerContext;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Contains internal functions available to the {@link mil.dod.th.core.ccomm.transport.TransportLayer} implementation. 
 * Includes functions that should not be part of the {@link mil.dod.th.core.ccomm.transport.TransportLayer} interface 
 * available to outside consumers, but is needed internally.
 */
public interface TransportLayerInternal extends TransportLayerContext, FactoryObjectInternal
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.transport.link.TransportLayerInternal";
    
    /**
     * Key value pair to denote a {@link mil.dod.th.core.ccomm.transport.TransportLayer} service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=TransportLayer";
    
    /**
     * Set the link layer associated with this transport layer.
     * 
     * @param linkLayer
     *            the link layer.
     */
    void setLinkLayer(LinkLayer linkLayer);
}
