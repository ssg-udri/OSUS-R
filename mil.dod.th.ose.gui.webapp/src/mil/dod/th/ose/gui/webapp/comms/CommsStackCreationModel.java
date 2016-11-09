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

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;


/**
 * This class contains the needed information to complete a request to create a new comms stack.
 * @author allenchl
 *
 */
public class CommsStackCreationModel 
{
    /**
     * String corresponding to the selected link type.
     * The type is the fully qualified class name of the link object.
     */
    private String m_SelectedLinkType;
    
    /**
     * String corresponding to the selected transport type.
     * The type is the fully qualified class name of the transport object.
     */
    private String m_SelectedTransportType;
    
    /**
     * String corresponding to the name of the physical link selected for use.
     */
    private String m_SelectedPhysicalLink;

    /**
     * Name of the link layer to be created.
     */
    private String m_NewLinkName;
    
    /**
     * Name of the transport layer to be created. 
     */
    private String m_NewTransportName;
    
    /**
     * Name of the physical link to be created.
     * This is to only be used when a controller does not have physical links pre-loaded.
     */
    private String m_NewPhysicalName;
    
    /**
     * Type of physical link selected.
     */
    private PhysicalLinkTypeEnum m_SelectedPhysicalType;

    /**
     * Boolean value indication whether or not a physical link
     * needs to be force-added to the system.
     */
    private boolean m_ForceAddPhysicalLink;

    /**
     * Returns the selected link layer type.
     * Type is represented by the fully qualified class name of the layer.
     * @return
     *      returns the selected link layer type.
     */
    public String getSelectedLinkLayerType()
    {
        return m_SelectedLinkType;
    }
    
    /**
     * Method which sets the type of the link layer to be created according to a user's selection.
     * Type is represented by the fully qualified class name of the layer.
     * @param linkType
     *      type of the link layer to be created.
     */
    public void setSelectedLinkLayerType(final String linkType)
    {
        m_SelectedLinkType = linkType;
    }

    /**
     * Returns the name of the new link layer being created.
     * @return
     *      Name of the new link layer being created.
     */
    public String getNewLinkName()
    {
        return m_NewLinkName;
      
    }
    
    /**
     * Sets the name for the new link layer to be created.
     * @param linkName
     *      Name of the new link layer being created.
     */
    public void setNewLinkName(final String linkName)
    {
        m_NewLinkName = linkName == null ? "" : linkName.trim();
    }

    /**
     * Returns the selected type of the transport layer to be created.
     * Type is represented by the fully qualified class name of the layer.
     * @return
     *      The selected type of the transport layer to be created. 
     */
    public String getSelectedTransportLayerType()
    {
        return m_SelectedTransportType;
    }

    /**
     * Sets the selected type of the transport layer to be created.
     * Type is represented by the fully qualified class name of the layer.
     * @param selectedTransport
     *      The selected type of the transport layer to be created.
     */
    public void setSelectedTransportLayerType(final String selectedTransport)
    {
        m_SelectedTransportType = selectedTransport;
    }

    /**
     * Returns the name of the new transport layer to be created.
     * @return
     *      The name of the new transport layer.
     */
    public String getNewTransportName()
    {
        return m_NewTransportName;
    }

    /**
     * Sets the name of the new transport layer to be created.
     * @param transportName
     *      The name of the new transport layer.
     */
    public void setNewTransportName(final String transportName)
    {
        m_NewTransportName = transportName == null ? "" : transportName.trim();
    }

    /**
     * Returns the name of the user-selected physical link.
     * @return
     *      The name of the user-selected physical link.
     */
    public String getSelectedPhysicalLink()
    {
        return m_SelectedPhysicalLink;
    }

    /**
     * Sets the selected name for the physical link to be used in the new comms stack.
     * @param physicalLink
     *      The selected name of the physical link.
     */
    public void setSelectedPhysicalLink(final String physicalLink)
    {
        m_SelectedPhysicalLink = physicalLink;
    }

    /**
     * Returns the name of the physical link to be created.
     * Should only be used in instances where physical links are
     * not pre-loaded on a controller.
     * @return
     *      name of the physical link to be created
     */
    public String getNewPhysicalName()
    {
        return m_NewPhysicalName;
    }
    
    /**
     * Sets the name of the new physical link to be created.
     * Should only be used in instances where physical links are
     * not pre-loaded on a controller.
     * @param physicalName
     *      Name of the physical link.
     */
    public void setNewPhysicalName(final String physicalName)
    {
        m_NewPhysicalName = physicalName == null ? "" : physicalName.trim();
    }

    /**
     * Returns the selected physical link type.
     * Type is represented by the fully qualified class name of the layer.
     * Should only be used in instances where physical links are
     * not pre-loaded on a controller.
     * @return
     *      Fully qualified class name of the selected physical link type
     */
    public PhysicalLinkTypeEnum getSelectedPhysicalType()
    {
        return m_SelectedPhysicalType;
    }
    
    /**
     * Sets the type of the physical link to be created.
     * Type is represented by the fully qualified class name of the layer.
     * Should only be used in instances where physical links are
     * not pre-loaded on a controller.
     * 
     * @param physicalLinkType
     *      Type of physical link selected
     *      
     */
    public void setSelectedPhysicalType(final PhysicalLinkTypeEnum physicalLinkType)
    {
        m_SelectedPhysicalType = physicalLinkType;
    }
    
    /**
     * Sets the value indicating whether or not a physical link
     * needs to be force-added to the system. <code>true</code>
     * indicates that a physical link needs to be force-added.
     * <code>false</code> otherwise.
     * @param forceAddPhysicalLink
     *      <code>true</code> indicates that a physical link
     *      needs to be force-added. <code>false</code> otherwise. 
     */
    public void setForceAdd(final boolean forceAddPhysicalLink)
    {
        m_ForceAddPhysicalLink = forceAddPhysicalLink;
    }
    
    /**
     * Returns the value indicating whether or not a physical link
     * needs to be force-added to the system. <code>true</code>
     * indicates that a physical link needs to be force-added.
     * <code>false</code> otherwise.
     * @return
     *      <code>true</code> indicates that a physical link
     *      needs to be force-added. <code>false</code> otherwise. 
     */
    public boolean isForceAdd()
    {
        return m_ForceAddPhysicalLink;
    }
}
