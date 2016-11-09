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
package mil.dod.th.ose.gui.webapp.factory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for the {@link FactoryObjectDisplayHelper}.
 * @author callen
 *
 */
public abstract class AbstractFactoryObjectDisplayHelper implements FactoryObjectDisplayHelper 
{
    /**
     * The selected factory object.
     */
    private FactoryBaseModel m_SelectedObject; 

    @Override
    public List<? extends FactoryBaseModel> getFactoryObjectListAsync()
    {
        return new ArrayList<AbstractFactoryBaseModel>();
    }

    @Override
    public void setSelectedFactoryObject(final FactoryBaseModel model)
    {
        m_SelectedObject = model;
    }

    @Override
    public FactoryBaseModel getSelectedFactoryObject()
    {
        return m_SelectedObject;
    }

    @Override
    public boolean isSetSelectedObject()
    {
        return m_SelectedObject != null;
    }
}
