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
package mil.dod.th.ose.gui.webapp.observation;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

/**
 * Implementation of the {@link RelatedObservationView} class.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "relatedObs")
@ViewScoped
public class RelatedObservationViewImpl implements RelatedObservationView
{
    /**
     * Reference to observation managed bean.
     */
    @ManagedProperty(value = "#{obsMgr}")
    private ObservationMgr obsMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Linked list responsible for storing the history of related observations that have been viewed.
     */
    private final List<UUID> m_History = new LinkedList<UUID>();
    
    /**
     * Integer that represents the current index of the observation being viewed with respect to the history linked 
     * list.
     */
    private int m_CurrentIndex;
    
    /**
     * Reference to the current related observation being displayed.
     */
    private GuiObservation m_CurrentObs;
    
    /**
     * Method used to set the observation manager to be used.
     * 
     * @param observationMgr
     *      The observation manager instance.
     */
    public void setObsMgr(final ObservationMgr observationMgr)
    {
        obsMgr = observationMgr;
    }
    
    @Override
    public void back()
    {
        if (canMoveBack())
        {
            m_CurrentIndex--;
            m_CurrentObs = obsMgr.getObservation(m_History.get(m_CurrentIndex));
        }
    }

    @Override
    public void forward()
    {
        if (canMoveForward())
        {
            m_CurrentIndex++;
            m_CurrentObs = obsMgr.getObservation(m_History.get(m_CurrentIndex));
        }
    }

    @Override
    public boolean canMoveBack()
    {
        if (m_History.isEmpty())
        {
            return false;
        }
        return m_CurrentIndex > 0;
    }

    @Override
    public boolean canMoveForward()
    {
        if (m_History.isEmpty())
        {
            return false;
        }
        return m_CurrentIndex < m_History.size() - 1;
    }

    @Override
    public void initialize(final UUID initialNodeUuid)
    {
        m_History.clear();
        m_History.add(initialNodeUuid);
        m_CurrentObs = obsMgr.getObservation(initialNodeUuid);
        m_CurrentIndex = 0;
    }

    @Override
    public void setCurrentNode(final UUID currentNodeUuid)
    {
        if (canMoveForward())
        {
            m_History.subList(m_CurrentIndex + 1, m_History.size()).clear();
        }
 
        m_History.add(currentNodeUuid);
        m_CurrentIndex++;
        m_CurrentObs = obsMgr.getObservation(currentNodeUuid);
    }

    @Override
    public GuiObservation getObservation()
    {
        return m_CurrentObs;
    }
}
