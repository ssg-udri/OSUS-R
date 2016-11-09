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

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;


/**
 * Implementation for the {@link CommsStackModel} interface.
 * @author bachmakm
 *
 */
public class CommsStackModelImpl implements CommsStackModel
{
    /**
     * Reference to the comms image class.
     */
    private final CommsImage m_CommsImage;
    
    /**
     * Boolean value used to determine if all necessary layers have been set for a stack.  
     */
    private boolean m_IsStackComplete;

    /**
     * The transport layer of a particular comms stack.
     */
    private FactoryBaseModel m_Transport;

    /**
     * The link layer of a particular comms stack.
     */
    private CommsLayerLinkModel m_Link;

    /**
     * The physical layer of a particular comms stack.
     */
    private FactoryBaseModel m_Physical;
    
    /**
     * Constructor method.
     * @param imgInterface
     *  the image display interface to use.
     */
    public CommsStackModelImpl(final CommsImage imgInterface)
    {
        m_IsStackComplete = false;
        m_CommsImage = imgInterface;
    }
    
    @Override
    public String getCommsTopLayerName()
    {        
        if (m_Transport == null)
        {
            if (m_Link == null)
            {
                if (m_Physical == null)
                {
                    return "";
                }
                return m_Physical.getName();
            }
            else
            {
                return m_Link.getName();
            }
        }
        else
        {
            return m_Transport.getName();
        }
    }

    @Override
    public void setTransport(final FactoryBaseModel tPort)
    {
        m_Transport = tPort;
    }

    @Override
    public FactoryBaseModel getTransport()
    {
        return m_Transport;
    }

    @Override
    public void setLink(final CommsLayerLinkModel link)
    {
        m_Link = link;
    }

    @Override
    public CommsLayerLinkModel getLink()
    {
        return m_Link;
    }

    @Override
    public void setPhysical(final FactoryBaseModel phys)
    {
        m_Physical = phys;
    }

    @Override
    public FactoryBaseModel getPhysical()
    {
        return m_Physical;
    }

    @Override
    public List<FactoryBaseModel> getStackLayers()
    {
        final List<FactoryBaseModel> models = new ArrayList<FactoryBaseModel>();

        if (m_Transport != null)
        {
            models.add(m_Transport);
        }

        if (m_Link != null)
        {
            models.add(m_Link);
        }

        if (m_Physical != null)
        {
            models.add(m_Physical);
        }
        return models;
    }

    @Override
    public void setStackComplete()
    {
        m_IsStackComplete = true;
    }

    @Override
    public boolean isComplete()
    {
        return m_IsStackComplete;
    }
    
    /**
     * Get the image that pertains to the comms stack model.
     * @return
     *      the string URL of the image
     */
    public String getImage()
    {
        //if comms stack has a transport layer
        if (getTransport() != null)
        {
            return getTransport().getImage();
        }
        
        //if comms stack has a link layer
        if (getTransport() == null && getLink() != null)
        {
            return getLink().getImage();
        }
        
        //if comms stack doesn't have a transport layer and link layer or has both
        if (getPhysical() != null)
        {
            return getPhysical().getImage();
        }
        
        return m_CommsImage.getImage();
    }
}
