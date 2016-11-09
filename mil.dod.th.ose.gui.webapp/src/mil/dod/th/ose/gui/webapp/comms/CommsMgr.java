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
import java.util.UUID;

import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.RemoteCreateLinkLayerHandler;

/**
 * Interface for the comms bean.  
 * @author bachmakm
 *
 */
public interface CommsMgr
{
    
    /** Event topic prefix for all {@link CommsMgr} events. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/comms/CommsMgr/";
    
    /** Topic used to specify information on a remote comms stack was received and that the comms manager has updated
     * the information stored about this comms stack.
     */
    String TOPIC_COMMS_LAYER_UPDATED = TOPIC_PREFIX + "COMMS_LAYER_UPDATED";    
    
    /**
     * Returns a list of all known transport layers for a particular controller.
     * @param systemId
     *      ID of the controller.
     * @return
     *      list of all known transport layers.
     */
    List<CommsLayerBaseModel> getTransportsAsync(int systemId);
    
    /**
     * Returns a list of all known link layers for a particular controller.
     * @param systemId
     *      ID of the controller.
     * @return
     *      list of all known link layers.
     */
    List<CommsLayerLinkModelImpl> getLinksAsync(int systemId);
    
    /**
     * Returns a list of all known physical layers for a particular controller.
     * @param systemId
     *      ID of the controller.
     * @return
     *      list of all known physical layers.
     */
    List<CommsLayerBaseModel> getPhysicalsAsync(int systemId);

    /**
     * Returns a list of all known unused physical link names
     * for a particular controller.
     * @param systemId
     *      ID of the controller.
     * @return
     *      list of known unused physical link names. 
     */
    List<String> getUnusedPhysicalLinkNames(int systemId);

    /**
     * Method for creating an instance of the response handler used to potentially
     * send a CreateTransportLayer request upon receiving a CreateLinkLayer response. 
     * @param transportBuild
     *      Partially built CreateTransportLayer request.  Can be <code>null</code> if link layer
     *      does not have corresponding transport layer.  
     * @return  
     *      New instance of RemoteCreateLinkLayerHandler for listening to CreateLinkLayer responses.
     */
    RemoteCreateLinkLayerHandler createLinkLayerHandler(CreateTransportLayerRequestData.Builder transportBuild); 
    
    /**
     * Method for getting the UUID of a physical link based on the physical link's 
     * name. Needed for CreateLinkLayer requests.  
     * @param name
     *      name of the physical link
     * @param systemId
     *      ID of the controller to which the physical link belongs
     * @return
     *      UUID of the physical link
     */
    UUID getPhysicalUuidByName(String name, int systemId);

    /**
     * Method which updates the list of unused physical links.  
     * @param systemId
     *      ID to which the physical links belong
     * @param unusedPhysicalLinks
     *      updated list of unused physical links.
     */
    void setUnusedPhysicalLinks(int systemId, List<CommsLayerBaseModel> unusedPhysicalLinks);  
    
    /**
     * Given a SystemId and a Physical layer name, find the class name.
     * @param name physical layer name
     * @param systemId the system id
     * @return the class name, or null if not found.
     */
    String getPhysicalClazzByName(final String name, final int systemId);

}
