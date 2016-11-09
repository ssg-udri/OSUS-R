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
 * Class used to store information on parsed XSD files including namespaces, types, and their fields.
 * 
 * @author cweisenborn
 */
public class XsdModel
{
    /**
     * Map of XSD namespaces. The key is the string which represents the namespace and the model that contains all 
     * XSD types within the namespace.
     */
    private final Map<String, XsdNamespace> m_NamespacesMap = new NonReplacingHashMap<String, XsdNamespace>();
    
    /**
     * Get the map of {@link XsdNamespace}s.
     * 
     * @return
     *      The map of XSD namespaces where the key is the string that represents the namespace and the value is the
     *      model that contains all {@link XsdType}s within the namespace.
     */
    public Map<String, XsdNamespace> getNamespacesMap()
    {
        return m_NamespacesMap;
    }
}
