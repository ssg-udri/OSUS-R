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

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

/**
 * Interface for getting all the transport layer types.
 * @author bachmakm
 *
 */
public interface CommsLayerTypesMgr
{
    /** Event topic prefix for {@link CommsLayerTypesMgr} events. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/comms/CommsLayerTypesMgr/";
    
    /** Topic used to specify that new comms layer types have been received.
     */
    String TOPIC_COMMS_TYPES_UPDATED = TOPIC_PREFIX + "COMMS_TYPES_UPDATED";   
    
    /**
     * Retrieves a list of all transport layer types available on a particular controller.
     * Type is represented by the fully qualified class name of the layer.
     * @param systemId
     *      ID of the controller containing the transport layer types
     * @return
     *      A list of all transport layer types on the active controller.
     */
    List<String> getTransportLayerTypes(int systemId);
    
    /**
     * Method which returns a list of all link layer types for a particular controller.
     * Type is represented by the fully qualified class name of the layer.
     * @param systemId
     *      ID of the controller containing the link layer types
     * @return
     *      List of link layer types for an active controller.
     */
    List<String> getLinkLayerTypes(int systemId);

    /**
     * Function to retrieve whether a link layer requires a physical link.
     * 
     * @param systemId
     *      the id of the system that the link layer resides on
     * @param clazzName
     *      the name of the link layer class
     * @return
     *      true if the link layer requires a physical link
     */
    boolean getLinkLayerRequiresPhysical(int systemId, String clazzName);

    /**
     * Method which returns a list of all physical layer classes for a particular controller.
     * Type is represented by the fully qualified class name of the layer.
     * @param systemId
     *      ID of the controller containing the physical layer types
     * @return
     *      List of physical layer types for an active controller.
     */
    List<String> getPhysicalLinkClasses(int systemId);
    
    /**
     * Get a list of available physical link types.
     * 
     * @param systemId
     *      which controller to get the list of types
     * @return
     *      list of link types
     */
    List<PhysicalLinkTypeEnum> getPhysicalLinkTypes(int systemId);
     
    /**
     * Function to retrieve a capabilities object for the specified system and class name.
     * @param systemId
     *      the id of the system that the capabilities object resides on
     * @param clazzName
     *      the name of the class which the capabilities object pertains to
     * @return
     *      the capabilities object that matches the system id and the class name; if 
     *      no match exists then null is returned
     */
    Object getCapabilities(final int systemId, final String clazzName);
    
    /**
     * Send request for new layer types belonging to a particular controller.  
     * Responses are received in the implementation.  
     * @param systemId
     *      ID of the controller.
     */
    void requestLayerTypesUpdate(int systemId);
    
    /**
     * Get the image that pertains to a certain comms type.
     * @param commsType
     *      the comms type to get the image
     * @param controllerId
     *      the controller id that the comm type belongs to
     * @return
     *      the string URL of the image
     */
    String getImage(String commsType, int controllerId);
    
    /**
     * Get the physical link class for the physical link with the specified type.
     * 
     * @param systemId
     *      the id of the system that the physical link resides on
     * @param type
     *      type of physical link to lookup
     * @return
     *      fully qualified class name of the plug-in providing the given link type 
     */
    String getPhysicalLinkClassByType(final int systemId, final PhysicalLinkTypeEnum type);
}
