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
package mil.dod.th.ose.gui.webapp.comms;

import java.util.UUID;

import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;

/**
 * Class needed to instantiate instances of transport and physical layers.  
 * @author bachmakm
 *
 */
public class CommsLayerBaseModel extends AbstractFactoryBaseModel
{
    /**
     * Holds the FQCN of the object that this model represents.
     */
    final private String m_ClazzName; // TD: this could be merged with m_Type in AssetModel and handled by base class
    
    /**
     * The type of the comms object that this model represents.
     */
    final private CommType m_Type;
    
    /**
     * The reference to the {@link CommsLayerTypesMgr}.
     */
    final private CommsLayerTypesMgr m_CommsLayerTypesMgr;
    
    /**
     * Reference to the comms image class.
     */
    private final CommsImage m_CommsImage;
    
    /**
     * Constructor for the base layer model.  
     * @param controllerId
     *     the system id to which this base layer model corresponds
     * @param uuid
     *     the UUID of the base layer which this model represents
     * @param pid
     *     the PID of the base layer
     * @param clazzName
     *     the FQCN that this model represents
     * @param type
     *     the {@link CommType} of the object this model represents
     * @param factMgr
     *     the factory object's manage
     * @param mgr
     *     the {@link CommsLayerTypesMgr} reference
     * @param configWrapper
     *      reference to the system configuration manager bean
     * @param imgInterface
     *     the image display interface that is to be used.
     */
    public CommsLayerBaseModel(final int controllerId, final UUID uuid, 
            final String pid, final String clazzName, final CommType type, 
            final FactoryObjMgr factMgr, final CommsLayerTypesMgr mgr, 
            final ConfigurationWrapper configWrapper, final CommsImage imgInterface)
    {
        super(controllerId, uuid, pid, clazzName, factMgr, configWrapper);
        
        m_ClazzName = clazzName;
        
        m_Type = type;
        
        m_CommsLayerTypesMgr = mgr;
        m_CommsImage = imgInterface;
    }

    /**
     * Function returns the FQCN that this object represents.
     * @return
     *  a string representation of the FQCN for the object that this class represents
     */
    public String getCommsClazz()
    {
        return m_ClazzName;
    }
    
    /**
     * Function returns that {@link CommType} of the object this model represents.
     * @return
     *  the {@link CommType} of this object
     */
    public CommType getType()
    {
        return m_Type;
    }
    
    /**
     * Get the image that pertains to a comms layer base.
     * @return
     *      the string URL of the comms layer base image
     */
    @Override
    public String getImage()
    {
        if (m_Type.equals(CommType.TransportLayer))
        {
            return m_CommsImage.getTransportImage((TransportLayerCapabilities)
                    m_CommsLayerTypesMgr.getCapabilities(super.getControllerId(), m_ClazzName));
        }
        else if (m_Type.equals(CommType.PhysicalLink))
        {
            return m_CommsImage.getPhysicalLinkImage();
        }
        
        return m_CommsImage.getImage();
    }
}
