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
package mil.dod.th.ose.core.impl.ccomm.link;

import mil.dod.th.core.ccomm.link.LinkLayerContext;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Contains internal functions available to the {@link mil.dod.th.core.ccomm.link.LinkLayer} implementation. Includes 
 * functions that should not be part of the {@link mil.dod.th.core.ccomm.link.LinkLayer} interface available to outside 
 * consumers, but is needed internally.
 */
public interface LinkLayerInternal extends LinkLayerContext, FactoryObjectInternal
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal";
    
    /**
     * Key value pair to denote a {@link mil.dod.th.core.ccomm.link.LinkLayer} service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=LinkLayer";
    
    /**
     * sets the PhysicalLink to be used. The PhysicalLink passed must already be requested from the CustomComms Service
     * 
     * @param physicalLink
     *            The PhysicalLink that this LinkLayer should use.
     */
    void setPhysicalLink(PhysicalLink physicalLink);
}
