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
package mil.dod.th.ose.core.impl.asset;

import java.util.HashMap;
import java.util.Map;

/**
 * This class that contains the name and properties needed to create an asset found while scanning.
 * 
 * @author cweisenborn
 */
public class ScanResultsData
{
    /**
     * Name of the asset found.
     */
    private final String m_Name;
    
    /**
     * Properties of the asset found.
     */
    private final Map<String, Object> m_Properties;
    
    /**
     * Constructor that accepts the name and the properties of the asset found while scanning.
     * 
     * @param name
     *      Name of the asset or null if the asset found should use a default name.
     * @param properties
     *      Properties of the asset or null if no properties.
     */
    public ScanResultsData(final String name, final Map<String, Object> properties)
    {
        m_Name = name;
        m_Properties = properties;
    }
    
    /**
     * Returns the name of the found asset.
     * 
     * @return
     *      Name of the asset or null if a default name has been specified.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Returns the properties for the found asset.
     * 
     * @return
     *      Properties for the found asset.
     */
    public Map<String, Object> getProperties()
    {
        if (m_Properties == null)
        {
            return new HashMap<String, Object>();
        }
        return m_Properties;
    }
}
