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
package mil.dod.th.ose.gui.webapp.asset;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;

import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;

/**
 * Implementation of the {@link AssetPageDisplayHelper} class.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "assetPageDisplayHelper")
@SessionScoped
public class AssetPageDisplayHelperImpl implements AssetPageDisplayHelper
{
    
    /**
     * Map used to stored the index of the open command tab for each asset.
     */
    private final Map<Integer, Map<UUID, ActiveCommandIndexHolder>> m_AccordionIndex = 
            Collections.synchronizedMap(new HashMap<Integer, Map<UUID, ActiveCommandIndexHolder>>());
    
    /**
     * Variable used to store the current tab on the assets page.
     */
    private Integer m_AssetPageIndex = 0;
    
    /**
     * Variable to hold the active index of the tabs for the Observation tools accordion panel.
     */
    private Integer m_ObservationToolsActiveIndex = -1;
    
    /**
     * Variable used to store the name of the current tab on the assets page.
     */
    private String m_AssetPageTabName;
    
    @Override
    public void setAssetPageIndex(final int index)
    {
        m_AssetPageIndex = index;
    }
    
    @Override
    public int getAssetPageIndex()
    {
        return m_AssetPageIndex;
    }
    
    @Override
    public String getAssetPageTabName()
    {
        return m_AssetPageTabName;
    }
    
    @Override
    public void setObservationToolsActiveIndex(final int index)
    {
        //nothing to do because the value doesn't need to be actually set
    }
    
    @Override
    public int getObservationToolsActiveIndex()
    {
        return m_ObservationToolsActiveIndex;
    }
    
    @Override
    public ActiveCommandIndexHolder getAssetCommandIndexHolder(final int controllerId, final UUID uuid)
    {
        if (m_AccordionIndex.containsKey(controllerId) && m_AccordionIndex.get(controllerId).containsKey(uuid))
        {
            return m_AccordionIndex.get(controllerId).get(uuid);
        }
        else
        {
            return createAssetCommandIndex(controllerId, uuid);
        }
    }
    
    /**
     * Method that creates the {@link ActiveCommandIndexHolder} which represents the currently active components 
     * for an asset.
     * @param controllerId
     *              ID of the controller where the asset is located.
     * @param uuid
     *              UUID of the asset to retrieve the {@link ActiveIndexHolder} object
     * @return
     *              {@link ActiveCommandIndexHolder} which holds the current active 
     *              indexes of components for that asset.
     */
    private ActiveCommandIndexHolder createAssetCommandIndex(final int controllerId, final UUID uuid)
    {
        final ActiveCommandIndexHolder commandIndexHolder = new ActiveCommandIndexHolder();  
        if (!m_AccordionIndex.containsKey(controllerId)) //NOPMD - avoid x!=y - arranging order would be less readable
        {
            m_AccordionIndex.put(controllerId, new HashMap<UUID, ActiveCommandIndexHolder>());
            m_AccordionIndex.get(controllerId).put(uuid, commandIndexHolder);
        }
        else if (!m_AccordionIndex.get(controllerId).containsKey(uuid))
        {   
            m_AccordionIndex.get(controllerId).put(uuid, commandIndexHolder);
        }
        return m_AccordionIndex.get(controllerId).get(uuid);
    }
    
    @Override
    public void assetTabViewChange(final TabChangeEvent event)
    {
        final String title = event.getTab().getTitle();
        final Integer listSize = event.getComponent().getChildCount();
        //exclude first element in sublist as it pertains to all accordion tabs being closed
        final List<UIComponent> tabList = 
                event.getComponent().getChildren().subList(1, listSize);
        
        for (int i = 0; i < tabList.size(); i++)
        {
            final Tab tabComp = (Tab)tabList.get(i);
            if (tabComp.getTitle().equals(title))
            {
                m_AssetPageIndex = i;
                m_AssetPageTabName = tabComp.getTitle();
                return;
            }
        }
    }
    
    @Override
    public void observationToolsTabChange(final TabChangeEvent event)
    {
        final String title = event.getTab().getTitle();
        final List<UIComponent> tabList = event.getComponent().getChildren();
        
        for (int i = 0; i < tabList.size(); i++)
        {
            final Tab tabComp = (Tab)tabList.get(i);
            if (tabComp.getTitle().equals(title))
            {
                m_ObservationToolsActiveIndex = i;
                return;
            }
        }
    }
    
}
