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
package mil.dod.th.ose.gui.webapp.mp;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;

/**
 * Class that is used to store the variable metadata from the mission template.
 * 
 * @author cweisenborn
 */
public class MissionArgumentModel
{
    /**
     * String representation of the variable's name.
     */
    private String m_Name;
    
    /**
     * String representation of the variable's description.
     */
    private String m_Description;
    
    /**
     * {@link MissionVariableTypesEnum} which represents the variable's class type.
     */
    private MissionVariableTypesEnum m_Type;
    
    /**
     * String representation of the variable's value.
     */
    private Object m_Value;
    
    /**
     * List of strings which represents all option values of the variable.
     */
    private List<String> m_OptionValues;
    
    /**
     * String representation of the variable's minimum value.
     */
    private String m_MinValue;
    
    /**
     * String representation of the variable's maximum value.
     */
    private String m_MaxValue;
    
    /**
     * Method that returns the variable's name.
     * 
     * @return
     *          String that represents the variable's name.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Method that sets the variable's name.
     * 
     * @param name
     *          The name to be set.
     */
    public void setName(final String name)
    {
        m_Name = name;
    }
    
    /**
     * Method that returns the variable's description.
     * 
     * @return
     *          String that represents the variable description.
     */
    public String getDescription()
    {
        return m_Description;
    }
    
    /**
     * Method that sets the variable's description.
     * 
     * @param description
     *          The description to be set.
     */
    public void setDescription(final String description)
    {
        m_Description = description;
    }
    
    /**
     * Method that returns the variable's class type.
     * 
     * @return
     *          {@link MissionVariableTypesEnum} that represents the variables class type.
     */
    public MissionVariableTypesEnum getType()
    {
        return m_Type;
    }
    
    /**
     * Method that sets the variable's class type.
     * 
     * @param type
     *          {@link MissionVariableTypesEnum} to be set for the class type.
     */
    public void setType(final MissionVariableTypesEnum type)
    {
        m_Type = type;
    }
    
    /**
     * Method that returns the variable's current value.
     * 
     * @return
     *          Object that represents the variable's current value.
     */
    public Object getCurrentValue()
    {
        return m_Value;
    }
    
    /**
     * Method that sets the variable's value.
     * 
     * @param value
     *          Value to be set for the variable.
     */
    public void setCurrentValue(final Object value)
    {
        //it is expected that the value is really a string, but the getter/setter types must match
        if (value == null || ((String)value).isEmpty())
        {
            //don't really care about an empty string or null, only care about actual inputs
            //that may represent a type like a number
            m_Value = value;
        }
        else
        {
            m_Value = convertToActualType((String)value);
        }
    }
    
    /**
     * Method that returns a list of all option values for the variable.
     * 
     * @return
     *          {@link List} of strings that represents all option values for the variable.
     */
    public List<String> getOptionValues()
    {
        if (m_OptionValues == null)
        {
            m_OptionValues = new ArrayList<String>();
        }
        return m_OptionValues;
    }
    
    /**
     * Method that sets the option values for the variable.
     * 
     * @param optionValues
     *          {@link List} of strings to be set as the variable's option values.
     */
    public void setOptionValues(final List<String> optionValues)
    {
        m_OptionValues = optionValues;
    }
    
    /**
     * Method that returns the minimum value for the variable.
     * 
     * @return
     *          String representation of the variable's minimum value.
     */
    public String getMinValue()
    {
        return m_MinValue;
    }
    
    /**
     * Method that sets the variable's minimum value.
     * 
     * @param minValue
     *          The minimum value to be set.
     */
    public void setMinValue(final String minValue)
    {
        m_MinValue = minValue;
    }
    
    /**
     * Method that returns the maximum value for the variable.
     * 
     * @return
     *          String representation of the variable's maximum value.
     */
    public String getMaxValue()
    {
        return m_MaxValue;
    }
    
    /**
     * Method that sets the maximum value for the variable.
     * 
     * @param maxValue
     *          The maximum value to be set.
     */
    public void setMaxValue(final String maxValue)
    {
        m_MaxValue = maxValue;
    }
    
    /**
     * Store the type of the object as the actual type expected.
     * @param value
     *     the string representation of the value to set for this argument
     * @return
     *     the object representing the correct value of this argument
     */
    private Object convertToActualType(final String value)
    {
        if (m_Type == MissionVariableTypesEnum.DOUBLE)
        {
            return Double.parseDouble(value);
        }
        else if (m_Type == MissionVariableTypesEnum.FLOAT)
        {
            return Float.parseFloat(value);
        }
        else if (m_Type == MissionVariableTypesEnum.INTEGER)
        {
            return Integer.parseInt(value);
        }
        else if (m_Type == MissionVariableTypesEnum.LONG)
        {
            return Long.parseLong(value);
        }
        else if (m_Type == MissionVariableTypesEnum.SHORT)
        {
            return Short.parseShort(value);
        }
        else if (m_Type == MissionVariableTypesEnum.BYTE)
        {
            return Byte.parseByte(value);
        }
        else if (m_Type == MissionVariableTypesEnum.BOOLEAN)
        {
            if (value.equalsIgnoreCase(Boolean.TRUE.toString()))
            {
                return true;
            }
            else if (value.equalsIgnoreCase(Boolean.FALSE.toString()))
            {
                return false;
            }
            else
            {
                throw new IllegalArgumentException(String.format("Value: %s cannot be parsed to a boolean value.", 
                        value));
            }
        }
        else
        {
            return checkStringTypes(value);
        }
    }
    
    /**
     * Method used while converting to actual type that is used to determine if the argument type should be represented 
     * by a string value. This method is called in the {@link #convertToActualType(String)} and is used to reduce
     * complexity of the method.
     *  
     * @param value
     *          Value to check if the argument type is of a type that should be represented by a string.
     * @return
     *          String value that was passed in if it matches the appropriate argument type. Otherwise it throws an
     *          illegal argument exception if the argument type is not an argument type that should be represented by
     *          a string.
     */
    private Object checkStringTypes(final String value)
    {
        if (m_Type == MissionVariableTypesEnum.STRING 
                || m_Type == MissionVariableTypesEnum.ASSET
                || m_Type == MissionVariableTypesEnum.LINK_LAYER
                || m_Type == MissionVariableTypesEnum.PHYSICAL_LINK
                || m_Type == MissionVariableTypesEnum.TRANSPORT_LAYER
                || m_Type == MissionVariableTypesEnum.PROGRAM_DEPENDENCIES)
        {
            return value;
        }
        else
        {
            throw new IllegalArgumentException(String.format("The argument type: %s is not a known argument type!", 
                    m_Type.toString()));
        }
    }
    
    /**
     * This method checks {@link MissionVariableTypesEnum} of the parameter and returns the corresponding class type if
     * it is able to. This class type is later used in validating the value.
     * 
     * @param type
     *          The {@link MissionVariableTypesEnum} to be converted to a class type.
     * @return
     *          Class type specified by the {@link MissionVariableTypesEnum} enumeration.
     */
    private Class<?> determineClassType(final MissionVariableTypesEnum type)
    {
        switch (type)
        {
            case INTEGER: 
                return Integer.class;
            case DOUBLE: 
                return Double.class;
            case FLOAT: 
                return Float.class;
            case LONG: 
                return Long.class;
            case SHORT: 
                return Short.class;
            case BYTE: 
                return Byte.class;
            default: 
                return null;
        }
    }
    
    /**
     * Method used to validate the mission argument value being set.
     * 
     * @param context
     *          The current faces context, method interface is defined by JSF so parameter is required even if not used
     * @param component
     *          The JSF component calling the validate method, method interface is defined by JSF so parameter is 
     *          required even if not used
     * @param value
     *          The value being validated.
     * @throws ValidatorException
     *          Thrown value passed is not valid.
     */
    public void validateValue(final FacesContext context, final UIComponent component, final Object value) 
            throws ValidatorException
    {
        final Class<?> classType = determineClassType(getType());
        final ValidationEnum result = 
                ArgumentValidatorUtil.isValidValue((String)value, getMinValue(), m_MaxValue, classType);
        if (result != ValidationEnum.PASSED)
        {
            final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, result.toString(), null);
            throw new ValidatorException(msg);
        }
    }
}
