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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.apache.commons.lang.WordUtils;

/**
 * Utility class used to simplify calling various reflection methods.
 * 
 * @author cweisenborn
 */
public final class ReflectionsUtil
{
    /**
     * Private constructor to prevent instantiation.
     */
    private ReflectionsUtil()
    {
        
    }
    
    /**
     * Method used to retrieve the getter method for a field within the specified object.
     * 
     * @param containingObject
     *      Object that contains the getter method to be retrieved.
     * @param fieldName
     *      Name of the field within the object the getter method pertains to.
     * @param fieldType
     *      Class type of the field the getter method pertains to. Used to determine if the getter method is for a 
     *      boolean time in which is the "is" prefix is used instead of the standard "get" prefix.
     * @return
     *      The {@link Method} that represents the specified getter method within the object.
     * @throws ReflectionsUtilException
     *      Thrown if an error occurs while using reflections to retrieve the get method. 
     */
    public static Method retrieveGetMethod(final Object containingObject, final String fieldName, 
            final Class<?> fieldType) throws ReflectionsUtilException
    {      
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class))
        {
            try
            {
                return containingObject.getClass().getMethod("is" + WordUtils.capitalize(fieldName), new Class<?>[] {});
            }
            catch (final SecurityException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final NoSuchMethodException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
        }
        else
        {
            try
            {
                return containingObject.getClass().getMethod(
                        "get" + WordUtils.capitalize(fieldName), new Class<?>[] {});
            }
            catch (final SecurityException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final NoSuchMethodException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
        }
    }
    
    /**
     * Method used to retrieve the setter method for a field within the specified object.
     * 
     * @param containingObject
     *      Object that contains the setter method to be retrieved.
     * @param fieldName
     *      Name of the field within the object the setter method pertains to.
     * @return
     *      The {@link Method} that represents the specified setter method within the object.
     */
    public static Method retrieveSetMethod(final Object containingObject, final String fieldName)
    {
        for (Method method: containingObject.getClass().getMethods())
        {
            if (method.getName().equals("set" + WordUtils.capitalize(fieldName)))
            {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Method used to retrieve the is set method for a field within the specified object. This field is present in JAXB
     * objects.
     * 
     * @param containingObject
     *      Object that contains the is set method to be retrieved.
     * @param fieldName
     *      Name of the field that has an is set method.
     * @return
     *      Return the is set method if found, otherwise return null.
     */
    public static Method retrieveIsSetMethod(final Object containingObject, final String fieldName)
    {
        for (Method method: containingObject.getClass().getMethods())
        {
            if (method.getName().equals("isSet" + WordUtils.capitalize(fieldName)))
            {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Method used to retrieve the unset method for a field within the specified object. This field is present in JAXB
     * objects.
     * 
     * @param containingObject
     *      Object that contains the is set method to be retrieved.
     * @param fieldName
     *      Name of the field that has an is set method.
     * @return
     *      Return the unset method if found, otherwise return null.
     */
    public static Method retrieveUnsetMethod(final Object containingObject, final String fieldName)
    {
        for (Method method: containingObject.getClass().getMethods())
        {
            if (method.getName().equals("unset" + WordUtils.capitalize(fieldName)))
            {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Method used to determine if a field within an object is set. The object that contains the field to be checked
     * must have a method called is(FieldName)Set. This method is automatically generated for most fields within a JAXB
     * class. Will return null if no isSet method can be found.
     * 
     * @param containingObject
     *      Object that contains the field to be checked.
     * @param fieldName
     *      Name of the field to be checked.
     * @return
     *      True if the value is set, False if the value is not set, and null if no isSet method can be found.
     * @throws ReflectionsUtilException 
     *      Thrown if an reflections exception occurs while trying to call the isSet method.
     */
    public static Boolean isFieldSet(final Object containingObject, final String fieldName) 
            throws ReflectionsUtilException
    {
        final Method isSetMethod = retrieveIsSetMethod(containingObject, fieldName);
        if (isSetMethod == null)
        {
            throw new ReflectionsUtilException(String.format("The method is%sSet does not exists for the " 
                    + "containing object: %s", fieldName, containingObject));
        }
        else
        {
            try
            {
                return (Boolean)isSetMethod.invoke(containingObject, new Object[] {});
            }
            catch (final IllegalArgumentException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final IllegalAccessException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final InvocationTargetException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
        }
    }
    
    /**
     * Method used to set a value back to null. This method attempts to use the classes unset method for the specified
     * field. If none exists then this method attempts to call the fields setter method by passing null to it. The unset
     * method is usually specified in JAXB generated classes for certain fields. Not all JAXB fields have an unset 
     * method.
     * 
     * @param containingObject
     *      Object that contains the field to be set to null.
     * @param fieldName
     *      Name of the field to be unset.
     * @throws ReflectionsUtilException 
     *      Thrown if an exception occurs while using reflections to call the unset method.
     */
    public static void unsetField(final Object containingObject, final String fieldName) throws ReflectionsUtilException
    {
        final Method unsetMethod = retrieveUnsetMethod(containingObject, fieldName);
        if (unsetMethod == null)
        {
            setInnerObject(containingObject, fieldName, null);
        }
        else
        {
            try
            {
                unsetMethod.invoke(containingObject, new Object[] {});
            }
            catch (final IllegalArgumentException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final IllegalAccessException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final InvocationTargetException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
        }
    }      
    
    /**
     * Method used to retrieve an inner object from within a reference object.
     * 
     * @param containingObject
     *      The object that contains the inner object to be retrieved.
     * @param fieldName
     *      Name of the field that represents the inner object.
     * @param fieldType
     *      Class that represents the class type of the inner object.
     * @return
     *      Object that is a reference to the inner object retrieved from the containing object.    
     * @throws ReflectionsUtilException
     *      Thrown if an error occurs while using reflections to retrieve the inner object.
     */
    public static Object retrieveInnerObject(final Object containingObject, final String fieldName, 
            final Class<?> fieldType) throws ReflectionsUtilException
    {   
        final Method getMethod = retrieveGetMethod(containingObject, fieldName, fieldType);
        try
        {
            return getMethod.invoke(containingObject, new Object[] {});
        }
        catch (final IllegalArgumentException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
    }
    
    /**
     * Method used to set the value of an inner object within a reference object.
     * 
     * @param containingObject
     *      Object that contains the inner object to be set.
     * @param fieldName
     *      Name of the field that represents the inner object to be set.
     * @param valueToBeSet
     *      Object that is the value to be set for the inner object.    
     * @throws ReflectionsUtilException
     *      Thrown if an error occurs while using reflections to set the inner object.
     */
    public static void setInnerObject(final Object containingObject, final String fieldName, 
            final Object valueToBeSet) throws ReflectionsUtilException
    {   
        final Method setMethod = retrieveSetMethod(containingObject, fieldName);
        if (setMethod == null)
        {
            throw new ReflectionsUtilException(String.format("The method set%s does not exist for the containing " 
                    + "object: %s", fieldName, containingObject));
        }
        else
        {
            try
            {
                setMethod.invoke(containingObject, valueToBeSet);
            }
            catch (final IllegalArgumentException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final IllegalAccessException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
            catch (final InvocationTargetException exception)
            {
                throw new ReflectionsUtilException(exception);
            }
        }
    }
    
    /**
     * Method used to retrieve the specified field for an object. This class checks super classes for the specified
     * field as well. Will return null if a field with the specified name cannot be found.
     * 
     * @param fieldName
     *      String the represents the name of the field to be retrieved.
     * @param containingObject
     *      Object that contains the field to be retrieved.
     * @return
     *      {@link Field} that represents the retrieved field or null if no field with the specified name can be
     *      found.
     */
    public static Field retrieveField(final String fieldName, final Object containingObject)
    {
        for (Field field: containingObject.getClass().getDeclaredFields())
        {
            if (field.getName().equals(fieldName))
            {
                return field;
            }
        }
        
        Class<?> superClass = containingObject.getClass().getSuperclass();
        while (superClass != null && !superClass.equals(Object.class))
        {
            for (Field field: superClass.getDeclaredFields())
            {
                if (field.getName().equals(fieldName))
                {
                    return field;
                }
            }
            superClass = superClass.getSuperclass();
        }
        
        return null;
    }
    
    /**
     * Method used to invoke the parseFrom method for a protocol buffer class. This is used to convert a byte string 
     * into the appropriate message object.
     * 
     * @param protoClass
     *      The protocol buffer class containing the parseFrom method to be invoked.
     * @param byteString
     *      The {@link ByteString} to be parsed into a message.
     * @return
     *      The parse {@link Message}.
     * @throws ReflectionsUtilException
     *      Thrown if an error occurs while using reflections to invoke the pareFrom method.
     */
    public static Message invokeProtobuffParseFrom(final Class<?> protoClass, final ByteString byteString) 
            throws ReflectionsUtilException
    {
        final Method parseFromMethod;
        try
        {
            parseFromMethod = protoClass.getMethod("parseFrom", ByteString.class);
        }
        catch (final SecurityException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        try
        {
            return (Message)parseFromMethod.invoke(null, byteString);
        }
        catch (final IllegalArgumentException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ReflectionsUtilException(exception);
        }
    }
    
    /**
     * Method used to determine whether or not a class is a primitive type. This check will return true for all wrapped
     * primitives, Strings, and Enumerations.
     * 
     * @param type
     *      The class type to be checked.
     * @return
     *      True if the class type is a primitive otherwise false is returned.
     */
    public static boolean isPrimitiveOrWrapper(final Class<?> type)
    {
        if (type.isPrimitive() 
                || type.equals(Boolean.class)
                || type.equals(Integer.class)
                || type.equals(Double.class)
                || type.equals(Float.class)
                || type.equals(Long.class)
                || type.equals(Short.class)
                || type.equals(Byte.class)
                || type.equals(Character.class)
                || type.equals(String.class)
                || type.isEnum())
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Method that checks if the field has an appropriate JAXB annotation. Fields without an appropriate annotation
     * should be ignored. An example would be the {@link javax.xml.bind.annotation.XmlTransient} annotation.
     * 
     * @param field
     *      {@link Field} with annotations to be checked.
     * @return
     *      True if the the field contains a {@link XmlAttribute}, {@link XmlValue}, or {@link XmlElement} annotation.
     *      False is returned otherwise.
     */
    public static boolean hasJaxbAnnotation(final Field field)
    {
        final Annotation[] annotationArray = field.getAnnotations();
        boolean found = false;
        for (Annotation annotation: annotationArray)
        {
            final Class<?> annotationClass = annotation.annotationType();
            if (annotationClass.equals(XmlAttribute.class)
                    || annotationClass.equals(XmlValue.class)
                    || annotationClass.equals(XmlElement.class))
            {
                found = true;
            }
        }
        return found;
    }
}
