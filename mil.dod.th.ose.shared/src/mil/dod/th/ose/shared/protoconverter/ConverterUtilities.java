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
package mil.dod.th.ose.shared.protoconverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

import mil.dod.th.core.remote.objectconverter.ObjectConverterException;

/**
 * Utility class that contains methods used by both the MessageConverter and JAXBConverter classes.
 * 
 * @author cweisenborn
 */
public final class ConverterUtilities
{
    /**
     * Constructor is declared to prevent instantiation of the class.
     */
    private ConverterUtilities()
    {
        
    }
    
    /**
     * This method is responsible for returning the value associated with the specified field within the object.
     * 
     * @param parentJaxbObject
     *          Object that contains the field with the value to be retrieved.
     * @param protoFieldName
     *          The name of the field within the object contains the value to be retrieved.
     * @param protoFieldType
     *          The enumeration that represents the specified field's class type
     * @return
     *          The value of the specified field within the object.
     * @throws ObjectConverterException
     *          When the value is not able to be retrieved from the specified field within the object.
     */
    public static Object getValue(final Object parentJaxbObject, final String protoFieldName,
            final FieldDescriptor.Type protoFieldType) throws ObjectConverterException
    {
        Method getMethod;//NOCHECKSTYLE will get assigned in try/catch
        try
        {
            getMethod = parentJaxbObject.getClass().getMethod(getGetAccessorName(protoFieldName, protoFieldType));
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ObjectConverterException(exception);
        }

        try
        {
            return getMethod.invoke(parentJaxbObject);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ObjectConverterException(exception);
        }
    }
    
    /**
     * This method sets the value for the specified field in the object.  Does not apply if the field is repeated (i.e.,
     * List in JAXB)
     * 
     * @param parentJaxbObject
     *          The object with the field that is being set.
     * @param fieldValue
     *          The value of the field to be set.
     * @param protoFieldName
     *          The name of the field being set.
     * @param protoFieldType
     *          type of the proto field
     * @throws ObjectConverterException
     *          When specified field cannot be set.
     */
    public static void setNonRepeatedValue(final Object parentJaxbObject, final Object fieldValue, 
            final String protoFieldName, final FieldDescriptor.Type protoFieldType) throws ObjectConverterException
    {
        final Class<?> fieldClass = getJaxbTypeForField(parentJaxbObject, protoFieldName, protoFieldType);
        Method setMethod;//NOCHECKSTYLE will get assigned in try/catch
        try
        {
            setMethod = parentJaxbObject.getClass().getMethod(getSetAccessorName(protoFieldName), fieldClass);
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ObjectConverterException(String.format("Unable to set value for field [%s] for [%s]", 
                    protoFieldName, parentJaxbObject), exception);
        }
        
        try
        {
            setMethod.invoke(parentJaxbObject, fieldValue);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ObjectConverterException(exception);
        }
    }
    
    /**
     * This method checks to see if the specified field in the object is set. If the field is set the the method
     * returns true otherwise it returns false.
     * 
     * @param parentJaxbObject
     *          The object with the field that is being checked.
     * @param protoFieldName
     *          The name of the field within the object that is being checked.
     * @return
     *          True or false depending on whether the specified field is set or not.
     * @throws ObjectConverterException
     *          When the method is unable to determine if the field is set or not.
     */
    public static boolean isSet(final Object parentJaxbObject, final String protoFieldName) 
            throws ObjectConverterException
    {
        Method isSetMethod;//NOCHECKSTYLE will get assigned in try/catch

        try
        {
            isSetMethod = parentJaxbObject.getClass().getMethod(getIsSetAccessorName(protoFieldName));
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ObjectConverterException(exception);
        }
        
        try
        {
            return (Boolean)isSetMethod.invoke(parentJaxbObject);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ObjectConverterException(exception);
        }
    }

    /**
     * This method is responsible for determining the prefix for an accessor method. If the 
     * proto type is a boolean it uses the "is" accessor, otherwise the method uses the "get" accessor prefix.
     * 
     * @param protoFieldName
     *          Name of the protocol field that is being accessed and needs the accessor prefix determined.
     * @param protoFieldType
     *          The proto type of the protocol field that is being accessed.
     * @return
     *          Returns the the string representation of the accessor method for the specified protocol field.
     */
    private static String getGetAccessorName(final String protoFieldName, final FieldDescriptor.Type protoFieldType)
    {
        if (protoFieldType == Type.BOOL)
        {
            return "is" + Character.toUpperCase(protoFieldName.charAt(0)) + protoFieldName.substring(1);
        }
        else
        {
            return "get" + Character.toUpperCase(protoFieldName.charAt(0)) + protoFieldName.substring(1);
        }
    }
    
    /**
     * Method is responsible for returning the string representation of the specified fields setter method.
     *  
     * @param protoFieldName
     *          Name of the field that a string representation of the set method is being created for.
     * @return
     *          The string representation of the set method for the specified field.
     */
    public static String getSetAccessorName(final String protoFieldName)
    {
        return "set" + Character.toUpperCase(protoFieldName.charAt(0)) + protoFieldName.substring(1);
    }
    
    /**
     * This method is responsible for returning the string representation of the "isSet" method for the specified 
     * protocol field.
     * 
     * @param protoFieldName
     *          String representation of the protocol field name.
     * @return
     *          The string representation of the "isSet" method for the specified protocol field.
     */
    private static String getIsSetAccessorName(final String protoFieldName)
    {
        return "isSet" + Character.toUpperCase(protoFieldName.charAt(0)) + protoFieldName.substring(1);
    }
    
    /**
     * This method is responsible for determining the JAXB class type of the corresponding field in the JAXB parent 
     * object from the protocol message field.  If the field is repeated, the List class will not be returned, but the
     * type of list will be returned instead.
     * 
     * @param parentJaxbObject
     *          The JAXB object that is being created from a {@link com.google.protobuf.Message}.
     * @param protoFieldName
     *          The name of the field being converted.
     * @param protoFieldType
     *          Type of the proto field
     * @return
     *          The class type of the field or the generic type if the type is a List
     * @throws ObjectConverterException
     *          if unable to find a get method for the field to determine the class type
     */
    public static Class<?> getJaxbTypeForField(final Object parentJaxbObject, final String protoFieldName, 
            final FieldDescriptor.Type protoFieldType) throws ObjectConverterException
    {
        Method getMethod;//NOCHECKSTYLE will get assigned in try/catch
        try
        {
            getMethod = parentJaxbObject.getClass().getMethod(getGetAccessorName(protoFieldName, protoFieldType));
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ObjectConverterException(exception);
        }
        
        final Class<?> returnType = getMethod.getReturnType();
        
        // List type, so get the generic type inside it
        if (returnType.equals(List.class))
        {
            // All List types in a JAXB class must have a parameterized type with exactly 1 actual type which is the
            // type we need
            final ParameterizedType paramType = (ParameterizedType) getMethod.getGenericReturnType();
            return (Class<?>)paramType.getActualTypeArguments()[0];
        }
        
        return returnType;
    }
}
