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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

/**
 * Base class for storing information about Google Protocol Buffer types.
 */
public abstract class ProtoElement
{
    /**
     * The proto file this element is contained within.
     */
    private final ProtoFile m_ProtoFile;
    
    /**
     * The name of the element.
     */
    private String m_Name;
    
    /**
     * The class type the element represents.
     */
    private Class<?> m_Type;
    
    /**
     * The XSD type associated with the element. 
     */
    private XsdType m_XsdType;
    
    /**
     * Boolean that is used to determine if the element has been processed by the converter or not.
     */
    private boolean m_Processed;
    
    /**
     * Constructor that accepts the proto file this proto element is contained within.
     * 
     * @param protoFile
     *      The proto file this proto element is contained within.
     * @param elementName
     *      Name of the proto element.
     * @param type
     *      Class type the element represents.
     * @param xsdType
     *      XSD type associated with the element.
     */
    public ProtoElement(final ProtoFile protoFile, final String elementName, final Class<?> type, 
            final XsdType xsdType)
    {
        m_ProtoFile = protoFile;
        m_Name = elementName;
        m_Type = type;
        m_XsdType = xsdType;
    }
    
    /**
     * Gets the proto file the proto message is associated with.
     * 
     * @return
     *      The proto file model the proto message is associated with.
     */
    public ProtoFile getProtoFile()
    {
        return m_ProtoFile;
    }
    
    /**
     * Sets the name of the element.
     * 
     * @param name
     *      The name of the element to be set.
     */
    public void setName(final String name)
    {
        m_Name = name;
    }
    
    /**
     * Gets the name of the element.
     * 
     * @return
     *      The name of the element.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Sets the type the element represents.
     * 
     * @param type
     *      The class type the element represents.
     */
    public void setType(final Class<?> type)
    {
        m_Type = type;
    }
    
    /**
     * Gets the type the element represents.
     * 
     * @return
     *      Class type the element represents.
     */
    public Class<?> getType()
    {
        return m_Type;
    }
    
    /**
     * Sets the XSD type the element is associated with.
     * 
     * @param xsdType
     *      The {@link XsdType} to be set.
     */
    public void setXsdType(final XsdType xsdType)
    {
        m_XsdType = xsdType;
    }
    
    /**
     * Gets the XSD type the element is associated with.
     * 
     * @return
     *      The XSD type the element is associated with.
     */
    public XsdType getXsdType()
    {
        return m_XsdType;
    }
    
    /**
     * Sets whether the element has been processed by the converter.
     * 
     * @param processed
     *      The boolean value to be set.
     */
    public void setProcessed(final boolean processed)
    {
        m_Processed = processed;
    }
    
    /**
     * Returns whether the element has been processed by the converter.
     * 
     * @return
     *      Boolean that represents if the element has been processed by the converter.
     */
    public boolean isProcessed()
    {
        return m_Processed;
    }
}
