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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Model that represents a service as stored by the meta type service.
 * 
 * @author cweisenborn
 */
public class MetaTypeModel
{
    /**
     * PID of the service that this model represents.
     */
    private final String m_Pid;
    
    /**
     * ID of the bundle where this service is located.
     */
    private final long m_BundleId;
    
    /**
     * List of all attributes that pertain to the service.
     */
    private List<AttributeModel> m_Attributes;
    
    /**
     * Constructor method that accepts the PID and bundle ID as parameters.
     * 
     * @param pid
     *          PID of the service.
     * @param bundleId
     *          ID of the bundle where the service is located.
     */
    public MetaTypeModel(final String pid, final Long bundleId)
    {
        m_Pid = pid;
        m_BundleId = bundleId;
    }
    
    /**
     * Method that retrieves the PID of the service the model represents.
     * 
     * @return
     *          PID of the service the model represents
     */
    public String getPid()
    {
        return m_Pid;
    }
    
    /**
     * Method that retrieves the bundle ID where the service is located.
     * 
     * @return
     *          ID of the bundle where the service is located.  
     */
    public long getBundleId()
    {
        return m_BundleId;
    }
    
    /**
     * Retrieves the list of all attributes associated with the service.
     * 
     * @return
     *          List of all {@link AttributeModel}s associated with the service.
     */
    public List<AttributeModel> getAttributes()
    {
        if (m_Attributes == null)
        {
            m_Attributes = new ArrayList<AttributeModel>();
        }
        return m_Attributes;
    }
}
