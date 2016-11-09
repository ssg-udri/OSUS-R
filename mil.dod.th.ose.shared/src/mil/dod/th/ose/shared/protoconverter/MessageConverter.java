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

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.EnumConverter;

/**
 * Utility class responsible for handling the conversion of Google Protocol Buffer messages to JAXB objects.
 * 
 * @author cweisenborn
 */
public final class MessageConverter
{
    private static final String PACKAGE_SEPARATOR = ".";
    
    /**
     * Private constructor so that the class cannot be instantiated.
     */
    private MessageConverter()
    {
        
    }
    
    /**
     * The method responsible for handling the conversion of a Google Protocol Buffer message to an object.
     * 
     * @param protoMsg
     *          The {@link Message} being converted to an object.
     * @return
     *          The converted message as an object.
     * @throws ObjectConverterException
     *          When the Google Protocol Buffer message is unable to be converted to an object.
     */
    public static Object convertToJaxbRec(final Message protoMsg)
            throws ObjectConverterException
    {
        //UUID is not an XSD generated class and does not have a standard no arguments constructor and therefore
        //must be handled separately.
        if (protoMsg.getClass().getSimpleName().equals(UUID.class.getSimpleName()))
        {
            return handleUUID((SharedMessages.UUID)protoMsg); 
        }
        //The Multitype protocol class does not have a corresponding JAXB class and therefore must have a special case
        //to handle the conversion. In JAXB classes the multitype is represented by a generic Object.
        else if (protoMsg.getClass().getSimpleName().equals(Multitype.class.getSimpleName()))
        {
            return handleMultitype((Multitype)protoMsg);
        }
        final Object jaxbObj = createEmptyJaxb(protoMsg);
        for (FieldDescriptor fieldDesc : protoMsg.getDescriptorForType().getFields())
        {
            if (fieldDesc.isRepeated())
            {
                handleRepeatedField(jaxbObj, protoMsg, fieldDesc); 
            }
            else
            { 
                handleStandardField(jaxbObj, protoMsg, fieldDesc);
            }
        }
        return jaxbObj;
    }
    
    /**
     * This method is responsible for handling any repeated fields within the Google Protocol message being converted
     * to an object. Repeated fields in a protocol message is turned into a java.util.List.
     * 
     * @param jaxbObject
     *          The JAXB object being created by the converter.
     * @param protoMsg
     *          The {@link Message} being converted to an object.
     * @param fieldDesc
     *          The {@link FieldDescriptor} of the repeated field being converted.
     *          converted.
     * @throws ObjectConverterException
     *          When the messages repeated fields cannot be converted to a list.
     */
    private static void handleRepeatedField(final Object jaxbObject, final Message protoMsg, 
            final FieldDescriptor fieldDesc) throws ObjectConverterException
    {   
        if (protoMsg.getRepeatedFieldCount(fieldDesc) == 0)
        {
            return;
        }
        
        final String protoFieldName = fieldDesc.getName();
        final int numFields = protoMsg.getRepeatedFieldCount(fieldDesc);
        final Object valuesList = ConverterUtilities.getValue(jaxbObject, protoFieldName, fieldDesc.getType());

        @SuppressWarnings("unchecked")
        final List<Object> list = (List<Object>)valuesList;
        
        for (int i = 0; i < numFields; i++)
        {
            final Object protoFieldValue = protoMsg.getRepeatedField(fieldDesc, i);
            list.add(getJaxbValueForProtoField(fieldDesc.getType(), protoFieldValue));
        } 
    }
    
    /**
     * This method is responsible for handling any field not of the repeated type in a Google Protocol message being
     * converted to an object.
     *  
     * @param jaxbObject
     *          The JAXB object being created by the converter.
     * @param protoMsg
     *          The {@link Message} being converted to an object.
     * @param fieldDesc
     *          The {@link FieldDescriptor} of the repeated field being converted.
     * @throws ObjectConverterException
     *          When the messages field cannot be converted to an object.
     */
    private static void handleStandardField(final Object jaxbObject, final Message protoMsg, 
            final FieldDescriptor fieldDesc) throws ObjectConverterException
    {
        if  (!protoMsg.hasField(fieldDesc))
        {
            return;
        }
        final String protoFieldName = fieldDesc.getName();
        final Object protoFieldValue = protoMsg.getField(fieldDesc);
        if ("_base".equals(protoFieldName))
        {
            handleExtendedMsgField(jaxbObject, protoMsg, fieldDesc);
            return;
        }
        final Object jaxbValue = getJaxbValueForProtoField(fieldDesc.getType(), protoFieldValue);
        ConverterUtilities.setNonRepeatedValue(jaxbObject, jaxbValue, protoFieldName, fieldDesc.getType());
    }
    
    /**
     * Handles fields that are from an extended jaxb type going into the passed parent jaxb object. 
     * @param jaxbObject
     *      the object where the fields should be placed
     * @param protoMsg
     *      the proto msg containing the field
     * @param fieldDesc
     *      the field descriptor of the field to process
     * @throws ObjectConverterException
     *       if the child fields are not able to be set
     */
    private static void handleExtendedMsgField(final Object jaxbObject, final Message protoMsg, 
            final FieldDescriptor fieldDesc) throws ObjectConverterException
    {
        //want to apply fields to the jaxb object parent
        final Message protoFieldValue = (Message)protoMsg.getField(fieldDesc);
        for (FieldDescriptor field : protoFieldValue.getDescriptorForType().getFields())
        {
            if (field.isRepeated())
            {
                handleRepeatedField(jaxbObject, protoFieldValue, field); 
            }
            else
            { 
                handleStandardField(jaxbObject, protoFieldValue, field);
            }
        }
    }

    /**
     * This method is responsible for creating a UUID object instantiated with the values of the UUID field in the 
     * Google Protocol message being converted to an object.
     * 
     * @param uuidMsg
     *          The {@link Message} that is being converted.
     * @return
     *          An instantiated {@link UUID} object.
     */
    private static UUID handleUUID(final SharedMessages.UUID uuidMsg)
    {
        return new UUID(uuidMsg.getMostSignificantBits(), uuidMsg.getLeastSignificantBits());
    }
    
    /**
     * This method handles the conversion of the MapEntry. This method is needed since the inner Multitype protocol 
     * class does not have a corresponding JAXB class and therefore the conversion must be handled in a special case.
     * 
     * @param protoMsg
     *          The {@link Message} that is being converted.
     * @return
     *          A {@link mil.dod.th.core.types.MapEntry} instantiated and returned as a generic object.
     */
    private static Object handleMultitype(final Multitype protoMsg)
    {
        return SharedMessageUtils.convertMultitypeToObject(protoMsg);
    }
    
    /**
     * This method is responsible for determining the value to be set for the specified object field. If the value
     * is of type reference, then the convertToJAXBRec method is called to convert the reference class to a 
     * to an object. The created object for the reference field is then returned and set for the value of the 
     * specified object field.
     * 
     * @param field
     *          The protocol entry that represents a field in the {@link Message}.
     * @param protoFieldValue
     *          The value that belongs to the corresponding protocol field.
     * @return
     *          The JAXB value of the specified object field.
     * @throws ObjectConverterException
     *          When the field value is unable to be set for the specified field. 
     */
    private static Object getJaxbValueForProtoField(final FieldDescriptor.Type field, final Object protoFieldValue)
            throws ObjectConverterException
    {
        if (field == Type.MESSAGE)
        {
            final Message message = (Message)protoFieldValue;
            return convertToJaxbRec(message);
        }
        else if (field == Type.ENUM)
        {
            return EnumConverter.convertProtoEnumToJava((EnumValueDescriptor)protoFieldValue);
        }
        else if (field == Type.BYTES)
        {
            final ByteString bytes = (ByteString)protoFieldValue;
            return bytes.toByteArray();
        }
        else
        {
            return protoFieldValue;
        }
    }

    /**
     * Create an empty jaxb object representing the message.
     * @param protoMsg
     *      the proto message to base the jaxb object from
     * @return
     *      empty jaxb class
     * @throws ObjectConverterException
     *      if the jaxb class cannot be found
     */
    private static Object createEmptyJaxb(final Message protoMsg) throws ObjectConverterException
    {

        final Class<?> jaxbClass = findJaxbClassFromProtoClass(protoMsg.getClass());

        try
        {
            return jaxbClass.newInstance();
        }
        catch (final InstantiationException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ObjectConverterException(exception);
        }
    }
    
    /**
     * Find the proto class associated with the given jaxb class.
     * @param classToFind
     *      the class to find
     * @return
     *      the class object
     * @throws ObjectConverterException
     *      if the class cannot be found
     */
    private static Class<?> findJaxbClassFromProtoClass(final Class<?> classToFind) throws ObjectConverterException
    {
        final String jabClazz = classToFind.getName();
        //rename the package as we are going to the jaxb lexicon
        final String classString = jabClazz.replace("remote.lexicon", "core");
        
        //trim off parent message and Gen
        final Pattern pattern = Pattern.compile(".[A-Z]\\w*[\\$]");
        final Matcher match = pattern.matcher(classString);
        final String trimmedClass = match.replaceFirst(PACKAGE_SEPARATOR);
        try
        {
            return Class.forName(trimmedClass, true, 
                    mil.dod.th.core.observation.types.Observation.class.getClassLoader());
        }
        catch (final ClassNotFoundException e)
        {
            throw new ObjectConverterException(e);
        }
    }
}
