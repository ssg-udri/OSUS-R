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
package mil.dod.th.ose.core.impl.ccomm.physical;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Contains internal functions available to the {@link mil.dod.th.core.ccomm.physical.PhysicalLink} implementation. 
 * Includes functions that should not be part of the {@link mil.dod.th.core.ccomm.physical.PhysicalLink} interface 
 * available to outside consumers, but is needed internally.
 */
public interface PhysicalLinkInternal extends PhysicalLinkContext, FactoryObjectInternal
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal";
    
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_SERIAL_PORT_FACTORY_REG_ID = 
            "mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal_SerialPort";
    
    /**
     * Key value pair to denote a {@link mil.dod.th.core.ccomm.physical.PhysicalLink} service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=PhysicalLink";
    
    /**
     * Set the usage variable.
     * 
     * @param flag
     *            true if the physical link is in use.
     */
    void setInUse(boolean flag);
    
    /**
     * Set which link layer owns the physical link.
     * 
     * @param linkLayer
     *      which link layer owns the physical link or null if not in use or in use by something other than a link layer
     */
    void setOwner(LinkLayer linkLayer);
}
