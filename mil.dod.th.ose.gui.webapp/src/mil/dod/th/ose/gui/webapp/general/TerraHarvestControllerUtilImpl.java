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
package mil.dod.th.ose.gui.webapp.general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of {@link TerraHarvestControllerUtil}.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean (name = "terraHarvestSystemUtil")
@ViewScoped
public class TerraHarvestControllerUtilImpl implements TerraHarvestControllerUtil
{
    /**
     * Variable which holds the name of the {@link TerraHarvestController} system.
     */
    private String m_SystemName;
    
    /**
     * Variable which holds the id of the {@link TerraHarvestController} system.
     */
    private int m_SystemId;
    
    /**
     * Service for this instance of a Terra Harvest system.
     */
    @Inject @OSGiService
    private TerraHarvestController m_TerraHarvestController;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Post Constructor.
     */
    @PostConstruct
    public void init()
    {
        m_SystemName = m_TerraHarvestController.getName();
        m_SystemId = m_TerraHarvestController.getId();
    }
    
    /**
     * Set the TerraHarvestController service.
     * @param controller
     *  the current TerraHarvestController service object for this instance
     */
    public void setTerraHarvestController(final TerraHarvestController controller)
    {
        m_TerraHarvestController = controller;
    }
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#setSystemName(java.lang.String)
     */
    @Override
    public void setSystemName(final String name)
    {
        m_SystemName = name;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#getSystemName()
     */
    @Override
    public String getSystemName()
    {
        return m_SystemName;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#setSystemId(int)
     */
    @Override
    public void setSystemId(final int systemId)
    {
        m_SystemId = systemId;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#getSystemId()
     */
    @Override
    public int getSystemId()
    {
        return m_SystemId;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#getSystemVersion()
     */
    @Override
    public String getSystemVersion()
    {
        return m_TerraHarvestController.getVersion();
    }

    @Override
    public Map<String, String> getSystemBuildInformation()
    {
        return new HashMap<String, String>(m_TerraHarvestController.getBuildInfo());
    }
    
    @Override
    public List<String> getSystemBuildInformationKeys()
    {
        return new ArrayList<String>(m_TerraHarvestController.getBuildInfo().keySet());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.TerraHarvestSystemUtil#setSystemNameAndId()
     */
    @Override
    public void setSystemNameAndId()
    {
        m_TerraHarvestController.setName(m_SystemName);
        m_TerraHarvestController.setId(m_SystemId);
        
        m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Updated System Information", 
                "System information has been updated.");
    }
}
