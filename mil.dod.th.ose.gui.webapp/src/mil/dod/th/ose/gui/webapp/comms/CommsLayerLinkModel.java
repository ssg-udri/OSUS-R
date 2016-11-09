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

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

/**
 * Interface class containing methods to support the successful function of a link layer.    
 * @author bachmakm
 * 
 * TD: this interface should be removed and {@link CommsLayerLinkModelImpl} should be renamed to use this name to match
 * with {@link mil.dod.th.ose.gui.webapp.asset.AssetModel}, no need for models to have interfaces
 */
public interface CommsLayerLinkModel extends FactoryBaseModel
{    
    /**
     * Method which returns true if the link layer is active.  Returns false if the link layer is deactivated.
     * Returns  
     * @return
     *      true if the link layer is active. False otherwise. 
     */
    Boolean isActivated();
 
    /**
     * Returns a string representation of the active status of the link layer in a comms stack object.
     * @return
     *      string representation of the active status of the link layer in a comms stack object.
     */
    String getStatusString();
    
    /**
     * Returns the status of a link.
     * @return
     *      status of the link
     */
    LinkLayer.LinkStatus getStatus();
    
    /**
     * Sets the activated flag for the comms manager bean.
     * @param active
     *      Boolean value corresponding to the active state of the link layer.  
     */
    void setActivated(boolean active);
    
    /**
     * Sets the status of the link layer for the comms manager bean.
     * @param status
     *      Status of the link layer.
     */
    void setStatus(LinkLayer.LinkStatus status);

    /**
     * Used to determine if the status and activated flag have been set for the link layer.
     * @return
     *      true if both link status and isActivated flag have been set.  
     *      False if only one or neither values have been set.
     */
    boolean isMetadataComplete();

}
