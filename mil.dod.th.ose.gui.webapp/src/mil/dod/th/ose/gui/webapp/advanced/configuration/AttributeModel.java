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

import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;

/**
 * Model that represents all information stored in a meta type attribute.
 * 
 * @author cweisenborn
 */
public class AttributeModel
{
    /**
     * ID (key) of the attribute.
     */
    private final String m_Key;
    
    /**
     * Name of the attribute.
     */
    private final String m_Name;
    
    /**
     * Description of the attribute.
     */
    private final String m_Description;
    
    /**
     * Class type of the attribute.
     */
    private final int m_Type;
    
    /**
     * Cardinality of the attribute.
     */
    private final int m_Cardinality;
    
    /**
     * Default values for the attribute.
     */
    private final List<String> m_DefaultValues;
    
    /**
     * Option labels for the attribute.
     */
    private final List<String> m_OptionLabels;
    
    /**
     * Option values for the attribute.
     */
    private final List<String> m_OptionValues;
    
    /**
     * Whether the attribute it required or optional.
     */
    private final boolean m_Required;

    /**
     * Constructor method that creates an attribute model. This model accepts the ID, name, description, type,
     * cardinality, list of default values, and a list of option values as parameters.
     * 
     * @param attributeDef
     *      definition to base the model on
     */
    public AttributeModel(final AttributeDefinitionType attributeDef)
    {
        m_Key = attributeDef.getId();
        m_Name = attributeDef.getName();
        m_Description = attributeDef.getDescription();
        m_Type = attributeDef.getAttributeType();
        m_Cardinality = attributeDef.getCardinality();
        m_DefaultValues = new ArrayList<>(attributeDef.getDefaultValueList());
        m_OptionLabels = new ArrayList<>(attributeDef.getOptionLabelList());
        m_OptionValues = new ArrayList<>(attributeDef.getOptionValueList());
        m_Required = attributeDef.getRequired();
    }
    
    /**
     * Retrieves the ID of the attribute.
     * 
     * @return
     *          ID (Key) of the attribute.
     */
    public String getId()
    {
        return m_Key;
    }
    
    /**
     * Retrieves the name of the attribute.
     * 
     * @return
     *          Name of the attribute.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Retrieves the description of the attribute.
     * 
     * @return
     *          Description of the attribute.
     */
    public String getDescription()
    {
        return m_Description;
    }
    
    /**
     * Retrieves the class type of the attribute.
     * 
     * @return
     *          Class type of the attribute.
     */
    public int getType()
    {
        return m_Type;
    }
    
    /**
     * Retrieves the cardinality of the attribute.
     * 
     * @return
     *          Cardinality of the attribute.
     */
    public int getCardinality()
    {
        return m_Cardinality;
    }
    
    /**
     * Retrieves the list of default values for the attribute.
     * 
     * @return
     *          List of default values for the attribute.
     */
    public List<String> getDefaultValues()
    {
        return new ArrayList<String>(m_DefaultValues);
    }
    
    /**
     * Retrieves the list of option labels for the attribute.
     * 
     * @return
     *          List of option labels for the attribute.
     */
    public List<String> getOptionLabels()
    {
        return new ArrayList<String>(m_OptionLabels);
    }

    /**
     * Retrieves the list of option values for the attribute.
     * 
     * @return
     *          List of option values for the attribute.
     */
    public List<String> getOptionValues()
    {
        return new ArrayList<String>(m_OptionValues);
    }
    
    /**
     * Returns whether the attribute it required.
     * 
     * @return
     *      True if the attribute is required, false otherwise.
     */
    public boolean isRequired()
    {
        return m_Required;
    }
}
