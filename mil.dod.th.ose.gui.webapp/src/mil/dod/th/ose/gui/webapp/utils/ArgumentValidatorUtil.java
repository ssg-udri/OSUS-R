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
package mil.dod.th.ose.gui.webapp.utils;

/**
 * Utility class used to validate mission parameters.
 * 
 * @author cweisenborn
 */
public final class ArgumentValidatorUtil
{    
    /**
     * Private constructor so that the object cannot be instantiated.
     */
    private ArgumentValidatorUtil()
    {
        
    }
    
    /**
     * Method that returns a boolean value of whether the value passed to the method is a valid value.
     * 
     * @param value
     *          The string representation of the value to be checked.
     * @param minValue
     *          String representation of the minimum value that can be set for the value being checked.
     * @param maxValue
     *          String representation of the maximum value that can be set for the value being checked.
     * @param classType
     *          Class type of the value being validated.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum isValidValue(final String value, final String minValue, //NOCHECKSTYLE:
        final String maxValue, final Class<?> classType) //high cyclomatic complexity, need to handle all simple types
    {
        try
        {
            if (classType == Integer.class || classType == int.class)
            {
                final Integer parsedValue = Integer.parseInt(value);
                return checkInteger(parsedValue, minValue, maxValue);
            }
            else if (classType == Double.class || classType == double.class)
            {
                final Double parsedValue = Double.parseDouble(value);
                return checkDouble(parsedValue, minValue, maxValue);
            }
            else if (classType == Float.class || classType == float.class)
            {
                final Float parsedValue = Float.parseFloat(value);
                return checkFloat(parsedValue, minValue, maxValue);
            }
            else if (classType == Long.class || classType == long.class)
            {
                final Long parsedValue = Long.parseLong(value);
                return checkLong(parsedValue, minValue, maxValue);
            }
            else if (classType == Short.class || classType == short.class)
            {
                final Short parsedValue = Short.parseShort(value);
                return checkShort(parsedValue, minValue, maxValue);
            }
            else if (classType == Byte.class || classType == byte.class)
            {
                final Byte parsedValue = Byte.parseByte(value);
                return checkByte(parsedValue, minValue, maxValue);
            }
            else if (classType == Boolean.class || classType == boolean.class)
            {
                if (value.equalsIgnoreCase(Boolean.TRUE.toString()))
                {
                    return ValidationEnum.PASSED;
                }
                else if (value.equalsIgnoreCase(Boolean.FALSE.toString()))
                {
                    return ValidationEnum.PASSED;
                }
                return ValidationEnum.PARSING_FAILURE;
            }
            else 
            {
                return ValidationEnum.PASSED;
            }
        }
        catch (final NumberFormatException exception)
        {
            return ValidationEnum.PARSING_FAILURE;
        }
    }
    
    /**
     * Method used to check if the value is a valid integer.
     *  
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum integer the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum integer the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkInteger(final Integer value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Integer.parseInt(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Integer.parseInt(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }  

    /**
     * Method used to check if the value is a valid double.
     * 
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum double the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum double the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkDouble(final Double value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Double.parseDouble(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Double.parseDouble(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }
    
    /**
     * Method used to check if the value is a valid float.
     * 
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum float the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum float the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkFloat(final Float value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Float.parseFloat(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Float.parseFloat(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }
    
    /**
     * Method used to check if the value is a valid long.
     * 
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum long the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum long the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkLong(final Long value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Long.parseLong(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Long.parseLong(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }
    
    /**
     * Method used to check if the value is a valid short.
     * 
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum short the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum short the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkShort(final Short value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Short.parseShort(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Short.parseShort(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }
    
    /**
     * Method used to check if the value is a valid byte.
     * 
     * @param value
     *          String value to be checked.
     * @param minValue
     *          String that represents the minimum byte the value being checked can be set to.
     * @param maxValue
     *          String that represents the maximum byte the value being checked can be set to.
     * @return
     *          Enumeration which represents whether or not the value is valid and for what reason if it is not a valid
     *          value.
     */
    public static ValidationEnum checkByte(final Byte value, final String minValue, final String maxValue)
    {
        if (minValue != null && value < Byte.parseByte(minValue))
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        if (maxValue != null && value > Byte.parseByte(maxValue))
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        return ValidationEnum.PASSED;
    }
    
    /**
     * Method that converts a string to its actual value type. Used to convert values being returned from an XHTML page
     * since that all values being returned are of the String type regardless of what the actual value is.
     * 
     * @param value
     *          Value to be converted from a string to its actual type.
     * @param type
     *          Class type the string value is to be converted to.
     * @return
     *          Object that represents the converted value.
     */
    public static Object convertToActualType(final String value, final Class<?> type) //NOCHECKSTYLE: High cyclomatic 
    { //complexity. Need to handle all base types. 
        if (type == Double.class || type == double.class)
        {
            return Double.parseDouble(value);
        }
        else if (type == Float.class || type == float.class)
        {
            return Float.parseFloat(value);
        }
        else if (type == Integer.class || type == int.class)
        {
            return Integer.parseInt(value);
        }
        else if (type == Long.class || type == long.class)
        {
            return Long.parseLong(value);
        }
        else if (type == Short.class || type == short.class)
        {
            return Short.parseShort(value);
        }
        else if (type == Byte.class || type == byte.class)
        {
            return Byte.parseByte(value);
        }
        else if (type == Boolean.class || type == boolean.class)
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
        else if (type == Character.class || type == char.class)
        {
            return value.charAt(0);
        }
        else
        {
            return value;
        }
    }
    
    /**
     * Enumeration used to determine the outcome of the object being validated by the {@link ArgumentValidatorUtil} 
     * class.
     */
    public enum ValidationEnum
    {
        /**
         * If the value passes validation.
         */
        PASSED("Value Passed Validation!"),
        
        /**
         * If the value is less than the minimum value allowed.
         */
        LESS_THAN_MIN("Value Below Min!"),
        
        /**
         * If the value is greater than the maximum value allowed.
         */
        GREATER_THAN_MAX("Value Above Max!"),
        
        /**
         * If the value cannot be parsed to the appropriate type.
         */
        PARSING_FAILURE("Value Invalid Type!");
        
        /**
         * String representation of the currently set enumeration.
         */
        private String m_Value;
        
        /**
         * Method that sets the string value of the current enumeration.
         * 
         * @param valueStr
         *          String value to be set for the current enumeration.
         */
        ValidationEnum(final String valueStr)
        {
            m_Value = valueStr;
        }
        
        @Override
        public String toString()
        {
            return m_Value;
        }
    }
}
