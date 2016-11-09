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
package mil.dod.th.ose.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import com.google.common.base.Preconditions;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Utilities to make it easier to work with configurations from the ConfigurationAdmin service.
 */
public final class ConfigurationUtils
{
    /**
     * Private constructor to prevent instantiation.
     */
    private ConfigurationUtils()
    {
        // do nothing
    }

    /**
     * Converts String attribute value (from a metatype attribute definition) to what it is specified to be.
     * 
     * @param type
     *            the {@link AttributeDefinition} type
     * @param value
     *            the value that this attribute represents
     * @return parsed string of the desired type
     */
    public static Object parseStringByType(final int type, final String value)
    {
        switch (type)
        {
            case AttributeDefinition.BOOLEAN:
                return Boolean.parseBoolean(value);

            case AttributeDefinition.BYTE:
                return Byte.parseByte(value);

            case AttributeDefinition.CHARACTER:
                return value.charAt(0);

            case AttributeDefinition.DOUBLE:
                return Double.parseDouble(value);

            case AttributeDefinition.FLOAT:
                return Float.parseFloat(value);

            case AttributeDefinition.INTEGER:
                return Integer.parseInt(value);

            case AttributeDefinition.LONG:
                return Long.parseLong(value);

            case AttributeDefinition.SHORT:
                return Short.parseShort(value);

            case AttributeDefinition.STRING:
                return value;

            default:
                throw new IllegalArgumentException("Invalid type argument: " + type);
        }

    }
    
    /**
     * Method that converts the integer type stored in an attribute definition to a the appropriate class type.
     * 
     * @param type
     *          integer that represents the class type of the attribute definition.
     * @return
     *          the class type the integer represents.
     */
    public static Class<?> convertToClassType(final int type)
    {
        switch (type)
        {
            case AttributeDefinition.BOOLEAN:
                return Boolean.class;
            case AttributeDefinition.BYTE:
                return Byte.class;
            case AttributeDefinition.CHARACTER:
                return Character.class;
            case AttributeDefinition.DOUBLE:
                return Double.class;
            case AttributeDefinition.FLOAT:
                return Float.class;
            case AttributeDefinition.INTEGER:
                return Integer.class;
            case AttributeDefinition.LONG:
                return Long.class;
            case AttributeDefinition.SHORT:
                return Short.class;
            case AttributeDefinition.STRING:
                return String.class;
            default:
                return null;
        }
    }
    
    /**
     * Converts String[] attribute values (from MetaType attribute definitions) to what it is specified to be.
     * 
     * See {@link AttributeDefinition#getCardinality()} for information.
     * 
     * @param type
     *            the {@link AttributeDefinition} type
     * @param cardinality
     *            the {@link AttributeDefinition} cardinality
     * @param values
     *            the value that this attribute represents
     * @return parsed string of the desired type
     */
    public static Object parseStringsByType(final int type, final int cardinality, final String[] values)
    {
        Preconditions.checkNotNull(values);
        
        if (cardinality > 0)
        {
            // Return an array of objects
            final Object[] array = new Object[values.length];
            for (int i = 0; i < cardinality; ++i)
            {
                array[i] = parseStringByType(type, values[i]);
            }

            return array;
        }
        else if (cardinality < 0)
        {
            // Return a Vector of objects
            final Vector<Object> vector = new Vector<Object>(); // NOPMD: Use of Vector is required by MetaTypeService
            for (int i = 0; i < values.length; ++i)
            {
                vector.add(parseStringByType(type, values[i]));
            }

            return vector;
        }
        else
        {
            return parseStringByType(type, values[0]);
        }
    }

    /**
     * Utility method for realizing whether a property map contains configuration properties or only default 
     * component properties. This method is intended to check property maps for service components ONLY.
     * @param properties
     *     the property map to check for configuration values
     * @return
     *     true if the property map contains configuration values in addition to default component properties
     * @throws IllegalArgumentException
     *     thrown in the event the map evaluated does not contain the expected component name and id keys
     */
    public static boolean configPropertiesSet(final Map<String, Object> properties) throws IllegalArgumentException
    {
        if (properties.containsKey(ComponentConstants.COMPONENT_NAME) 
                && properties.containsKey(ComponentConstants.COMPONENT_ID))
        {
            //if there are only two properties then no config properties are also in the map
            if (properties.size() == 2)
            {
                return false;
            }
            //the map contains component properties and additional properties
            return true;
        }
        else
        {
            throw new IllegalArgumentException(
                "Property map is not from a component or the map is otherwise invalid");
        }
    }
    
    /**
     * Create a {@link Map} from the incoming {@link Dictionary}.
     * 
     * @param <K>
     *      type of the key for this Dictionary
     * @param <V>
     *      type of the value for this Dictionary
     * @param props
     *      dictionary of properties to convert
     * @return
     *      map of converted from the dictionary
     */
    public static <K, V> Map<K, V> convertDictionaryPropsToMap(final Dictionary<K, V> props)
    {
        final Map<K, V> map = new HashMap<>();
        
        final Enumeration<K> keys = props.keys();
        while (keys.hasMoreElements())
        {
            final K key = keys.nextElement();
            map.put(key, props.get(key));
        }
        
        return map;
    }
    
    /**
     * Create a clone of the incoming {@link Dictionary}.  Elements are the same reference, but the dictionary itself 
     * is a new instance.
     * 
     * @param props
     *      dictionary of properties to clone
     * @return
     *      clone of the dictionary
     */
    public static Dictionary<String, Object> cloneDictionary(final Dictionary<String, Object> props)
    {
        final Dictionary<String, Object> clone = new Hashtable<String, Object>();
        
        final Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements())
        {
            final String key = keys.nextElement();
            clone.put(key, props.get(key));
        }
        
        return clone;
    }
}
