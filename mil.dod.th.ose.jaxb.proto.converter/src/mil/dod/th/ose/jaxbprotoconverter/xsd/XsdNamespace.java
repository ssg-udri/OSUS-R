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
package mil.dod.th.ose.jaxbprotoconverter.xsd;

import java.util.Map;

import mil.dod.th.ose.jaxbprotoconverter.NonReplacingHashMap;

/**
 * Model used to store information about all types within a XSD namespace.
 * 
 * @author cweisenborn
 */
public class XsdNamespace
{
    /**
     * Reference to the XSD model that XSD namespace is associated with.
     */
    private final XsdModel m_XsdModel;
    
    /**
     * Map of XSD types within the namespace. The key is the JAXB class and the value is the model the represents the 
     * XSD type for the JAXB class.
     */
    private final Map<Class<?>, XsdType> m_TypesMap = new NonReplacingHashMap<Class<?>, XsdType>();
    
    /**
     * Constructor that sets the XSD model that namespace is associated with.
     * 
     * @param xsdModel
     *      The {@link XsdModel} the namespace is associated with.
     */
    public XsdNamespace(final XsdModel xsdModel)
    {
        m_XsdModel = xsdModel;
    }
    
    /**
     * Get the XSD model that the namespace is associated with.
     * 
     * @return
     *      The XSD model the namespace is associated with.
     */
    public XsdModel getXsdModel()
    {
        return m_XsdModel;
    }
    
    /**
     * Get the map of {@link XsdType}s.
     * 
     * @return
     *      The map of XSD types where the key is the JAXB class and the value is the model that represents the XSD 
     *      schema for the JAXB class.
     */
    public Map<Class<?>, XsdType> getTypesMap()
    {
        return m_TypesMap;
    }
}
