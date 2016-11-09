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

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;


/**
 * Implementation of the {@link CommsLayerLinkModel} interface.  Class is used to instantiate link layer attributes
 * in a comms stack.   
 * @author bachmakm
 *
 */
public class CommsLayerLinkModelImpl extends AbstractFactoryBaseModel implements 
    CommsLayerLinkModel
{
    /**
     * The reference to the comms layers type manager.
     */
    final private CommsLayerTypesMgr m_CommsLayerTypesMgr;
    
    /**
     * Holds the FQCN of the object that this model represents.
     */
    final private String m_ClazzName;
    
    /**
     * Flag corresponding to whether or not the link layer has been activated.  
     * Flag is true if layer is active. Flag is false if layer is not active.
     * Flag is null if the value has not been set.  
     */
    private Boolean m_Activated;
    
    /**
     * Status of the link layer.
     */
    private LinkLayer.LinkStatus m_Status;
    
    /**
     * The reference to the comms image class.
     */
    private final CommsImage m_CommsImageInterface;
    
    /**
     * Constructor.
     * @param controllerId
     *      ID of the controller.
     * @param uuid
     *      UUID of the communications layer.
     * @param pid
     *      PID of the communications layer.
     * @param clazzName
     *      the FQCN that this model represents
     * @param factMgr
     *      the factory object's manager
     * @param mgr
     *      the reference to the {@link CommsLayerTypesMgr}
     * @param wrapper
     *      Reference to the system configuration manager bean. 
     * @param imgInterface
     *      the image display interface to use
     */
    public CommsLayerLinkModelImpl(final int controllerId, final UUID uuid, final String pid, final String clazzName,
           final FactoryObjMgr factMgr, final CommsLayerTypesMgr mgr, 
           final ConfigurationWrapper wrapper, final CommsImage imgInterface)
    {
        super(controllerId, uuid, pid, clazzName, factMgr, wrapper);
        
        m_ClazzName = clazzName;
        
        m_CommsLayerTypesMgr = mgr;
        
        m_CommsImageInterface = imgInterface;
    }
    
    /**
     * Function returns the FQCN of the class that this object represents.
     * @return
     *  a string representation of the FQCN for the object that this class represents
     */
    public String getCommsClazz()
    {
        return m_ClazzName;
    }
    
    @Override
    public void setActivated(final boolean active)
    {
        m_Activated = active;
    }
    
    @Override
    public void setStatus(final LinkLayer.LinkStatus status)
    {
        m_Status = status;
    }
    
    @Override
    public LinkLayer.LinkStatus getStatus()
    {
        return m_Status;
    }
    
    @Override
    public Boolean isActivated()
    {
        return m_Activated;
    }

    @Override
    public String getStatusString()
    {
        if (m_Status == null)
        {
            return "Unknown";
        }
        return m_Status.toString();
    }

    @Override
    public boolean isMetadataComplete()
    {
        return m_Activated != null && m_Status != null;
    }
    
    /*
     * (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.comms.CommsLayerLinkModel#getImage()
     */
    @Override
    public String getImage()
    {
        return m_CommsImageInterface.getLinkLayerImage((LinkLayerCapabilities)m_CommsLayerTypesMgr.
                getCapabilities(super.getControllerId(), m_ClazzName));
    }
    
    //check if the activated flag and the status have been set for the link layer
    @Override
    public boolean isComplete()
    {
        //check if the activated flag was set
        if (m_Activated == null)
        {
            return false;
        }
        //check if the status was set
        if (m_Status == null)
        {
            return false;
        }
        
        //check to make sure the abstract factory base model is complete
        if (!super.isComplete())
        {
            return false;
        }
        
        return true;
    }
}
