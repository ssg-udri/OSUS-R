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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;

import com.google.common.base.CaseFormat;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.ProtocolMessageEnum;

import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.EnumConverter;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen.MissionProgramSchedule;

/**
 * Utility class responsible for handling the conversion of JAXB objects to Google Protocol Buffer messages.
 * 
 * @author cweisenborn
 */
public final class JaxbConverter
{
    /**
     * Regex string expression for ignore casing. Shows up more than once throughout the program.
     */
    private static final String IGNORECASE = "(?i)";
    
    /**
     * String representation of underscore character. Used multiple times in regex expressions.
     */
    private static final String UNDERSCORE = "_";

    /**
     * Hidden constructor to avoid instantiation.
     */
    private JaxbConverter()
    {
        //hidden on purpose
    }
    
    /**
     * The method responsible for handling the conversion of the JAXB object to the Protocol {@link Message}.
     * 
     * @param object
     *          The JAXB object to be converted to a Protocol {@link Message}.
     * @return
     *          The {@link Message} that represents the JAXB object.
     * @throws ObjectConverterException 
     *          When the JAXB object is unable to be converted to a @{link Message}.
     */
    public static Message convertToProto(final Object object)
            throws ObjectConverterException
    {   
        final Class<? extends Message> messageClass = findProtoClass(object.getClass());
        final Message.Builder protoBuilder = getBuilder(messageClass);
        
        //fill in the fields for the particular message using the values from the jaxb class
        for (FieldDescriptor fieldDesc : protoBuilder.getDescriptorForType().getFields())
        {
            processField(object, protoBuilder, messageClass, fieldDesc);
        }
        
        return protoBuilder.build();
    }
    
    /**
     * Fill out and build a message representing the passed jaxb object.
     * @param object
     *      the object to make a proto message of
     * @param refclass
     *      the proto message class of the object
     * @return
     *      the message representing the jaxb object passed
     * @throws ObjectConverterException
     *      if one of the message fields cannot be set
     */
    private static Message buildProto(final Object object, final Class<? extends Message> refclass) 
            throws ObjectConverterException
    {
        //get the builder for the referenced proto message class
        final Message.Builder protoBuilder = getBuilder(refclass);
        for (FieldDescriptor fieldDesc : protoBuilder.getDescriptorForType().getFields())
        {
            processField(object, protoBuilder, refclass, fieldDesc);
        }
        return protoBuilder.build();
    }
    
    /**
     * Process the given field and add the value to the message builder.
     * @param jaxbObject
     *      the jaxb object containing the field's value
     * @param protoBuilder
     *      the message to add the given field's value to
     * @param messageClass
     *      the message class of the builder
     * @param fieldDesc
     *      the field to process
     * @throws ObjectConverterException
     *      if the field cannot be set 
     */
    private static void processField(final Object jaxbObject, final Builder protoBuilder, 
            final Class<? extends Message> messageClass, final FieldDescriptor fieldDesc) 
                    throws ObjectConverterException
    {
        final String protoFieldName = fieldDesc.getName();

        //if the field name is '_base' it is required and a type that is extended
        //by it's jaxb class
        if ("_base".equals(protoFieldName))
        {
            //retrieve the field type and build that proto message and set the field
            final Class<? extends Message> fieldType = findClass(protoFieldName.replace(UNDERSCORE, ""), messageClass);
            protoBuilder.setField(fieldDesc, buildProto(jaxbObject, fieldType));
            return;
        }
          // Don't check the isSet method for fields that belong to the UUID class. UUID is a java.util type and not 
          // an XSD generated class therefore the isSet method won't exist for fields in that class.
        else if (!(UUID.class.getSimpleName().toLowerCase().equals(protoFieldName)
                || "leastSignificantBits".equals(protoFieldName)
                || "mostSignificantBits".equals(protoFieldName)))
        {
            final boolean isSet = ConverterUtilities.isSet(jaxbObject, protoFieldName);
    
            if (!isSet)
            {
                return;
            }
        }

        final Object value = ConverterUtilities.getValue(jaxbObject, protoFieldName, fieldDesc.getType());

        if (value == null)
        {
            return;
        }

        if (fieldDesc.isRepeated())
        {
            // Raw type since the type of values in the list could vary depending on the object being converted.
            @SuppressWarnings("rawtypes")
            final List list = (List)value;
            for (Object item : list)
            {
                protoBuilder.addRepeatedField(fieldDesc,
                        checkValueForMessage(fieldDesc, item, protoFieldName, messageClass));
            }
        }
        else
        { 
            // pass parent object
            protoBuilder.setField(fieldDesc, checkValueForMessage(fieldDesc, value, protoFieldName, messageClass));
        }
    } 

    /**
     * This method is responsible for determining the {@link Class} of the specified proto message field given 
     * the string representation of the fields name and the initial class the field was found in.
     * 
     * @param protoFieldName
     *          The string representation of the fields name that references another message class.
     * @param messageType
     *          The class in which field resides.
     * @return
     *          The {@link Class} that is class type of the referenced field.
     */
    private static Class<? extends Message> findClass(final String protoFieldName, final Class<?> messageType)
    {
        Class<?> importedClazz = null;
        final Field[] fields = messageType.getDeclaredFields();
        
        for (Field field: fields)
        {
            if (field.getName().matches(IGNORECASE + protoFieldName + UNDERSCORE))
            {
                importedClazz = field.getType();
                if (importedClazz.getSimpleName().equals("List"))
                {
                    final ParameterizedType listType = (ParameterizedType)field.getGenericType();
                    importedClazz = (Class<?>)listType.getActualTypeArguments()[0];
                    break;
                }
            }
        }
        @SuppressWarnings("unchecked")
        final Class<? extends Message> messageClass = (Class<? extends Message>)importedClazz;
        return messageClass; //NOPMD: Unnecessary Local Before Return, needed for suppression
    }
    
    /**
     * This method is responsible for determining the value to be set for the specified protocol field. If the value
     * is of type reference, then the {@link #buildProto(Object, Class)} method is called to convert the reference 
     * class to a {@link Message}. The created message for the reference field is then returned and set for the value 
     * of the specified protocol field.
     * 
     * @param field
     *          The protocol field that represents a field in the {@link Message}.
     * @param value
     *          The value that belongs to the corresponding protocol field.
     * @param protoFieldName
     *          The string representation of the protocol field.
     * @param messageType
     *          The class of the {@link Message} being created.
     * @return
     *          The value of the specified protocol field.
     * @throws ObjectConverterException
     *          When the field value is unable to be set for the specified field. 
     */
    private static Object checkValueForMessage(final FieldDescriptor field, final Object value, 
            final String protoFieldName, final Class<? extends Message> messageType) throws ObjectConverterException
    {
        if (field.getType() == Type.MESSAGE)
        {
            final Class<? extends Message> refClass = findClass(protoFieldName, messageType);
            if (refClass == Multitype.class)
            {
                return handleMultitype(value);
            }
            return buildProto(value, refClass);

        }
        else if (field.getType() == Type.ENUM)
        {
            final ProtocolMessageEnum enumObject = EnumConverter.convertJavaEnumToProto((Enum<?>)value);
            return enumObject.getValueDescriptor();
        }
        else
        {
            if (field.getType() == Type.BYTES)
            {
                return ByteString.copyFrom((byte[])value);
            }
            else
            {
                return value;
            }
        }
    }

    /**
     * This method handles converting a field in a JAXB class to a {@link Multitype} message.
     * 
     * @param value
     *          The value to be set for the multitype.
     * @return
     *          The multitpye message created from the provided value.        
     * @throws ObjectConverterException
     *          Thrown if the value is not able to be converted to a multitype.
     */
    private static Message handleMultitype(final Object value) throws ObjectConverterException 
    {
        Multitype convertedMultitype = null;
        if (SharedMessageUtils.isValueConvertableToMultitype(value))
        {
            convertedMultitype = SharedMessageUtils.convertObjectToMultitype(value);
        }
        else 
        {
            throw new ObjectConverterException("The value: " + value + " is not able to be converted to multitype!");
        }
        return convertedMultitype;
    }
    
    /**
     * Find the proto class associated with the given jaxb class.
     * <br>
     * <br>
     * <b>NOTE: This method is not able to find all jaxb classes. Specifically
     * classes that are defined in a parent xGen class that does not match the jaxb objects simple
     * name, for example the proto message class for mil.dod.th.core.asset.capability.CommandCapabilities
     * will not be found without being in a mil.dod.th.core.asset.capability.AssetCapabilities jaxb
     * object. This is because there is currently no way to know that the 
     * mil.dod.th.core.asset.capability.CommandCapabilities proto message
     * equivalent is in mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen.CommandCapabilities.
     * </b>
     * 
     * @param jaxbClass
     *      the jaxb class to find the proto equivalent of
     * @return
     *      the proto message class
     * @throws ObjectConverterException
     *      if the class cannot be found
     */
    private static Class<? extends Message> findProtoClass(final Class<?> jaxbClass) throws ObjectConverterException
    {
        //split on the core package
        final String jaxbPackageFragment = jaxbClass.getName().split("mil.dod.th.core")[1];

        final String clazz = jaxbClass.getSimpleName();
        //the ending part of the jaxb package that matches the proto package
        final String splitPackage = jaxbPackageFragment.split("[A-Z].")[0];

        //Types package partition.
        final String typePackage = ".types.";
        //Remote package.
        final String remotePackage = "mil.dod.th.remote.lexicon";
        //Inner class designator.
        final String innerPackageDesignator = "$";
        //Generated proto file suffix.
        final String GEN_SUFFIX = "Gen";
        
        //the proto fqn class that is to found
        final String protoEquivFullyQualifiedClass;
        
        //the 'types' package contents are defined by the SharedTypes.xsd, this then becomes the SharedTypesGen,
        //from just the package and the jaxb class (xsd complex/simple types) there is not an easy way to derive the
        //package and outer class.
        if (typePackage.equals(splitPackage))
        {
            // The core.types package is defined in the SharedTypes.xsd, thus the proto derivative below.
            protoEquivFullyQualifiedClass = remotePackage + splitPackage + "SharedTypes"
                    + GEN_SUFFIX + innerPackageDesignator + clazz;
        }
        else if (splitPackage.startsWith(typePackage))
        {
            // the 'types' package contents are defined by a Xtypes.xsd, this then becomes the xTypesGen,
            // by transposing the last two package partitions the xsd encompassing type and the proto Gen type can
            // be derived.
            // TD: this is not infallible if there are further packages within the packages currently defined this
            // will break. There is not an way to get the XSD from which the jaxb classes came, but an improvement
            // might be making the XSD class known somehow.
            final String[] splitPackageUp = splitPackage.split("\\.");
            final StringBuffer switchPackOutterClass = new StringBuffer();
            // reverse the package sections, and you get the type, i.e. ...types.monkey can be expected to be derived
            // from MonkeyTypes.xsd and thus MonkeyTypesGen for proto concerns.
            for (int i = splitPackageUp.length - 1; i > 0; i--)
            {
                switchPackOutterClass.append(
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, splitPackageUp[i]));
            }
            protoEquivFullyQualifiedClass = remotePackage + splitPackage + switchPackOutterClass
                    + GEN_SUFFIX + innerPackageDesignator + clazz;
        }
        //mission package doesn't match the types structure, so has to be handled differently
        // TD should be able to look up XSD parent
        else if (MissionProgramSchedule.class.getSimpleName().equals(clazz))
        {
            //The schedule is defined in the parameters xsd, thus the proto derivative below.
            protoEquivFullyQualifiedClass = remotePackage + splitPackage + "MissionProgramParameters"
                        + GEN_SUFFIX + innerPackageDesignator + clazz;
        }
        else
        {
            protoEquivFullyQualifiedClass = remotePackage + jaxbPackageFragment 
                    + GEN_SUFFIX + innerPackageDesignator + clazz;
        }
        
        try
        {
            @SuppressWarnings("unchecked")
            final Class<? extends Message> messageClassToReturn = (Class<? extends Message>)
                    Class.forName(protoEquivFullyQualifiedClass, true, 
                    mil.dod.th.remote.lexicon.observation.types.ObservationGen.Observation.class.getClassLoader());
            //return here so suppression can be used on var
            return messageClassToReturn;
        }
        catch (final ClassNotFoundException e)
        {
            throw new ObjectConverterException(e);
        }
    }
    
    /**
     * This method is responsible for instantiating a {@link com.google.protobuf.Message.Builder} for the specified 
     * class.
     * 
     * @param messageType
     *          The class that a {@link com.google.protobuf.Message.Builder} is to be instantiated for.
     * @return
     *          The instantiated {@link com.google.protobuf.Message.Builder}.
     * @throws ObjectConverterException
     *          When the builder cannot be instantiated.
     */
    public static Message.Builder getBuilder(final Class<?> messageType) throws ObjectConverterException
    {
        try
        {
            return (Message.Builder)messageType.getMethod("newBuilder").invoke(null);
        }
        catch (final IllegalAccessException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final InvocationTargetException exception)
        {
            throw new ObjectConverterException(exception);
        }
        catch (final NoSuchMethodException exception)
        {
            throw new ObjectConverterException(exception);
        }
    }
}
