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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import com.google.common.base.Preconditions;

import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;
import mil.dod.th.ose.utils.ConfigurationUtils;

/**
 * Model that represents a meta type attribute and configuration to be displayed on an XHTML page.
 * 
 * @author cweisenborn
 */
public class ConfigPropModelImpl implements ModifiablePropertyModel
{
    /**
     * The key of the property.
     */
    private final String m_Key;
    
    /**
     * The name of the property.
     */
    private final String m_Name;
    
    /**
     * The description of the property.
     */
    private final String m_Description;
    
    /**
     * The value of the property.
     */
    private Object m_Value;
    
    /**
     * List of default values for the property.
     */
    private final List<String> m_DefaultValues;
    
    /**
     * List of options for the property.
     */
    private final List<OptionModel> m_Options;
    
    /**
     * Class type of the property value.
     */
    private final Class<?> m_Type;
    
    /**
     * Cardinality of the property.
     */
    private final int m_Cardinality;
    
    /**
     * Whether the configuration property is required.
     */
    private final boolean m_Required;
  
    /**
     * Constructor that instantiates a ConfigPropModel object.
     * 
     * @param attributeModel
     *      use attribute properties for this configuration property model
     */
    public ConfigPropModelImpl(final AttributeModel attributeModel)
    {
        Preconditions.checkArgument(attributeModel.getOptionLabels().size() == attributeModel.getOptionValues().size(),
                "Option label and value list must be the same size");
        
        m_Key = attributeModel.getId();
        m_Name = attributeModel.getName();
        m_Description = attributeModel.getDescription();
        m_Type = ConfigurationUtils.convertToClassType(attributeModel.getType());
        m_Cardinality = attributeModel.getCardinality();
        m_DefaultValues = attributeModel.getDefaultValues();
        m_Options = new ArrayList<>();
        m_Required = attributeModel.isRequired();
        
        for (int i = 0; i < attributeModel.getOptionLabels().size(); i++)
        {
            m_Options.add(new OptionModel(attributeModel.getOptionLabels().get(i), 
                    attributeModel.getOptionValues().get(i)));
        }
    }
    
    /**
     * Copy constructor that instantiates a ConfigPropModel object.
     * 
     * @param model
     *  model to copy values from
     */
    public ConfigPropModelImpl(final ConfigPropModelImpl model)
    {
        m_Key = model.m_Key;
        m_Name = model.m_Name;
        m_Description = model.m_Description;
        m_Type = model.m_Type;
        m_Cardinality = model.m_Cardinality;
        m_DefaultValues = new ArrayList<>(model.m_DefaultValues);
        m_Options = new ArrayList<>(model.m_Options);
        m_Value = model.m_Value;
        m_Required = model.m_Required;
    }
    
    @Override
    public String getKey()
    {
        return m_Key;
    }
    
    @Override
    public String getName()
    {
        return m_Name;
    }
    
    @Override
    public String getDescription()
    {
        return m_Description;
    }
    
    @Override
    public void setValue(final Object value)
    {
        //it is expected that the value is really a string, but the getter/setter types must match
        if (value == null || (value.toString()).isEmpty())
        {
            //don't really care about an empty string or null, only care about actual inputs
            //that may represent a type like a number
            m_Value = value;
        }
        else
        {
            m_Value = ArgumentValidatorUtil.convertToActualType(value.toString(), m_Type);
        }
    }
    
    @Override
    public Object getValue()
    {       
        if (m_Value == null)
        {
            if (m_DefaultValues.size() == 1)
            {
                return ArgumentValidatorUtil.convertToActualType(m_DefaultValues.get(0), m_Type);
            }
            //no default values return empty string otherwise an array will be returned and cause
            //the page not to render
            else if (m_DefaultValues.isEmpty())
            {
                return "";
            }
            else
            {
                return m_DefaultValues;
            }
        }
        
        return m_Value;
    }
    
    @Override
    public List<String> getDefaultValues()
    {
        return m_DefaultValues;
    }
    
    @Override
    public List<OptionModel> getOptions()
    {
        return m_Options;
    }
    
    @Override
    public Class<?> getType()
    {
        return m_Type;
    }
    
    @Override
    public boolean isBooleanType()
    {
        return m_Type == Boolean.class;
    }
    
    @Override
    public int getCardinality()
    {
        return m_Cardinality;
    }
    
    @Override
    public void validateValue(final FacesContext context, final UIComponent component, final Object value) 
            throws ValidatorException
    {
        final ValidationEnum result = 
                ArgumentValidatorUtil.isValidValue((String)value, null, null, m_Type);
        if (result != ValidationEnum.PASSED)
        {
            final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, result.toString(), null);
            throw new ValidatorException(msg);
        }
    }
    
    @Override
    public boolean isRequired()
    {
        return m_Required;
    }
}
