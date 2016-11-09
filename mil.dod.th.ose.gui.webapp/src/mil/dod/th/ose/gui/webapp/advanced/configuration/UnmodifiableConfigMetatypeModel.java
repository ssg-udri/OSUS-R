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
 * Model that represents a configuration service and contains both configuration and meta type information. 
 * Can be used to display the configuration and retrieve information about the configuration.
 * 
 * @author cweisenborn
 */
public class UnmodifiableConfigMetatypeModel
{
    /**
     * PID of the service the model represents.
     */
    private final String m_Pid;
    
    /**
     * List of models that represents all the properties of the service.
     */
    private final List<UnmodifiablePropertyModel> m_Properties;
    
    /**
     * Constructor method. Accepts the PID to be set for the configuration display model as a parameter.
     * 
     * @param pid
     *          PID of the service the model represents.
     */
    public UnmodifiableConfigMetatypeModel(final String pid)
    {
        m_Pid = pid;
        m_Properties = new ArrayList<UnmodifiablePropertyModel>();
    }
    
    /**
     * Method that returns the pid.
     * 
     * @return
     *          PID of the service the model represents.
     */
    public String getPid()
    {
        return m_Pid;
    }
    
    /**
     * Method that retrieves a list of model that represents all properties for the service.
     * 
     * @return
     *          List of {@link ConfigPropModelImpl}s.
     */
    public List<UnmodifiablePropertyModel> getProperties()
    {
        return m_Properties;
    }
}
