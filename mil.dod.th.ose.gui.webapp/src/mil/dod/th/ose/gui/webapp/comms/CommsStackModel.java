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

import java.util.List;

import mil.dod.th.ose.gui.webapp.CompletableModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

/**
 * Interface containing the different communications layers available in a given comms stack.
 * @author bachmakm
 *
 */
public interface CommsStackModel extends CompletableModel
{
    /**
     * String representation of the top-most layer name in the comms stack.
     * @return
     *      string representation of the comms stack name.
     */
    String getCommsTopLayerName();
    
    /**
     * Transport layer available in a comms stack object.
     * @return
     *      the available transport layer in a comms stack object.
     */
    FactoryBaseModel getTransport();
    
    /**
     * Set the transport layer of the comms stack.
     * @param tPort
     *      transport layer of the comms stack.
     */
    void setTransport(FactoryBaseModel tPort);

    /**
     * Link layer available in a comms stack object.
     * @return
     *      the available link layer in a comms stack object.
     */
    CommsLayerLinkModel getLink();
    
    /**
     * Set the link layer of the comms stack.
     * @param link
     *      link layer of the comms stack.
     */
    void setLink(CommsLayerLinkModel link);

    /**
     * Physical layer available in a comms stack object.
     * @return
     *      the available physical layer in a comms stack object.
     */
    FactoryBaseModel getPhysical();
    
    /**
     * Set the physical layer of the comms stack.
     * @param phys
     *      physical layer of the comms stack.
     */
    void setPhysical(FactoryBaseModel phys);

    /**
     * Returns a list of all available communication layers in a comms stack object.
     * @return
     *      list of all communication layers in a comms stack object.
     */
    List<FactoryBaseModel> getStackLayers();
    
    /**
     * Sets stackComplete flag to true, indicating that all layers for a stack have been set.  
     */
    void setStackComplete();
}
