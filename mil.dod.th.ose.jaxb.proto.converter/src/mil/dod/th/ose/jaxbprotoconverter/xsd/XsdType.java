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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to store information about an XSD type including fields contained in the type and the XSD file the 
 * type can be found in.
 * 
 * @author cweisenborn
 */
public class XsdType
{
    /**
     * Map of all fields contained in the XSD type. Key is the string which represents the field's name and the value is
     * the model that represents the XSD field. A linked hash map is used to maintain the order in which fields are 
     * added.
     */
    private final Map<String, XsdField> m_FieldsMap = new LinkedHashMap<String, XsdField>();
    
    /**
     * List of all overridden IDs.
     */
    private final List<Integer> m_OverriddenIds = new ArrayList<Integer>();
    
    /**
     * Reference to the XSD namespace that XSD type is associated with.
     */
    private XsdNamespace m_XsdNamespace;
    
    /**
     * Reference to the file the XSD type is located in.
     */
    private File m_XsdFile;
    
    /**
     * JAXB class this type represents.
     */
    private Class<?> m_JaxbType;
    
    /**
     * Type this XSD type extends.
     */
    private XsdType m_BaseType;
    
    /**
     * Boolean used to determine whether the XSD type represents a restriction on another type.
     */
    private boolean m_ComplexRestriction;

    private String m_OverriddenName;

    /**
     * Get the map of {@link XsdField}s.
     * 
     * @return
     *     The map of XSD fields contained in the XSD type.
     */
    public Map<String, XsdField> getFieldsMap()
    {
        return m_FieldsMap;
    }
    
    /**
     * Returns the a list of integers which represents all overridden IDs within the XSD type.
     * 
     * @return
     *      List of all overridden IDs within the XSD type.
     */
    public List<Integer> getOverriddenIds()
    {
        return m_OverriddenIds;
    }
    
    /**
     * Sets the XSD namespace the types is associated with.
     * 
     * @param xsdNamespace
     *      The XSD namespace the type is associated with.
     */
    public void setXsdNamespace(final XsdNamespace xsdNamespace)
    {
        m_XsdNamespace = xsdNamespace;
    }
    
    /**
     * Get the XSD namespace that the type is associated with.
     * 
     * @return
     *      The XSD namespace the type is associated with.
     */
    public XsdNamespace getXsdNamespace()
    {
        return m_XsdNamespace;
    }
    
    /**
     * Sets the XSD file the type is located in.
     * 
     * @param xsdFile
     *      The XSD file the type is located in.
     */
    public void setXsdFile(final File xsdFile)
    {
        m_XsdFile = xsdFile;
    }
    
    /**
     * Gets the file that represents the XSD file where the type is located.
     * 
     * @return
     *      File that represents where the XSD type is located.
     */
    public File getXsdFile()
    {
        return m_XsdFile;
    }
    
    /**
     * Sets the JAXB class the XSD type represents.
     * 
     * @param jaxbType
     *      The JAXB class the XSD type represents.
     */
    public void setJaxbType(final Class<?> jaxbType)
    {
        m_JaxbType = jaxbType;
    }
    
    /**
     * Gets the JAXB class the XSD type represents.
     * 
     * @return
     *      The JAXB class the XSD type represents.
     */
    public Class<?> getJaxbType()
    {
        return m_JaxbType;
    }
    
    /**
     * Sets the base type of the XSD type.
     * 
     * @param baseType
     *      Base type of the XSD type.
     */
    public void setBaseType(final XsdType baseType)
    {
        m_BaseType = baseType;
    }
    
    /**
     * Gets the base type the XSD type extend.
     * 
     * @return
     *      Base type of the XSD type or null if the XSD type does not extend another type.
     */
    public XsdType getBaseType()
    {
        return m_BaseType;
    }
    
    /**
     * Sets the boolean used to determine if the XSD type is a restriction on another type.
     *
     * @param restriction
     *      True if the XSD type represents an xs:restriction element.
     */
    public void setComplexRestriction(final boolean restriction)
    {
        m_ComplexRestriction = restriction;
    }
    
    /**
     * Gets the boolean that represents whether the XSD type is restriction on another type.
     * 
     * @return
     *      True if the XSD type represents is a restriction of another type and false otherwise.
     */
    public boolean isComplexRestriction()
    {
        return m_ComplexRestriction;
    }

    public void setOverriddenName(final String overriddenName)
    {
        m_OverriddenName = overriddenName;
    }

    public String getOverriddenName()
    {
        return m_OverriddenName;
    }
}
