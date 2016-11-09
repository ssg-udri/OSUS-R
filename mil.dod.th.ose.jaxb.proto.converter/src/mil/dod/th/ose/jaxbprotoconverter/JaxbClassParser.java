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

package mil.dod.th.ose.jaxbprotoconverter;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoElement;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoEnum;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.ProtoType;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoFile;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoMessage;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdNamespace;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

/**
 * This class has the capability to convert generated JAXB classes and convert them into protocol buffer proto 
 * files which can then be used by the protocol buffer compiler to generate data access classes. These data
 * access classes can be used to send and receive protocol buffer messages.
 * 
 * @author cweisenborn
 */
public class JaxbClassParser
{       
    /**
     * Map that contains all available non-generated messages for import. The key is a String that represents the name
     * of the message and the value represents the import statement that needs to be added to a proto file in order to
     * have access to the message.
     */
    private Map<String, String> m_ImportableMessages = new HashMap<String, String>();
    
    private ProtoModel m_ProtoModel;
    private XsdModel m_XsdModel;
    private File m_XsdFilesDir;
    
    /**
     * Creates a {@link ProtoModel} based on the JAXB class information stored within the specified {@link XsdModel}.
     * 
     * @param xsdModel
     *      XSD model that contains information on the JAXB classes to be parsed.
     * @param importableMessages
     *      Map of non-generated messages that can be imported.
     * @param xsdFilesDir
     *      Base directory where all XSD schema files are located.
     * @return
     *      Proto model that represents the parsed JAXB classes.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs parsing a class. 
     */
    public ProtoModel parseJaxbClasses(final XsdModel xsdModel, final Map<String, String> importableMessages,
            final File xsdFilesDir) throws JaxbProtoConvertException
    {
        m_ImportableMessages = importableMessages;
        m_XsdFilesDir = xsdFilesDir;
        m_ProtoModel = new ProtoModel();
        m_XsdModel = xsdModel;
        for (XsdNamespace namespace: m_XsdModel.getNamespacesMap().values())
        {
            for (Class<?> clazz: namespace.getTypesMap().keySet())
            {
                parseClass(clazz, namespace);
            }
        }
        
        return m_ProtoModel;
    }
    
    /**
     * Method that parses the specified JAXB class and creates the needed models to represent the class as a protocol
     * buffer.
     * 
     * @param jaxbClass
     *      The JAXB class to be parsed.
     * @param xsdNamespace
     *      The XSD namespace that contains the JAXB class.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs parsing the class. 
     */
    private void parseClass(final Class<?> jaxbClass, final XsdNamespace xsdNamespace) throws JaxbProtoConvertException
    {
        final XsdType xsdType = xsdNamespace.getTypesMap().get(jaxbClass);
        
        if (jaxbClass.isEnum())
        {
            handleEnum(jaxbClass, xsdType);
        }
        else
        {
            handleMessage(jaxbClass, xsdType);
        }
    }
    
    /**
     * Method that handles a JAXB class that is an enumeration.
     * 
     * @param jaxbClass
     *      JAXB enumeration class to be parsed.
     * @param xsdType
     *      XSD type that represents the JAXB enumeration class.
     */
    private void handleEnum(final Class<?> jaxbClass, final XsdType xsdType)
    {
        final ProtoEnum protoEnum = getOrCreateEnum(jaxbClass, xsdType);

        final List<String> values = new ArrayList<>();
        for (final Object value : jaxbClass.getEnumConstants())
        {
            values.add(value.toString());
        }
        
        protoEnum.setValues(values);
        m_ProtoModel.getEnums().add(protoEnum);
        protoEnum.setProcessed(true);
    }
    
    /**
     * Method that creates a model that represents a protocol buffer message for the JAXB class specified.
     * 
     * @param jaxbClass
     *      The JAXB class to crate protocol buffer model for.
     * @param xsdType
     *      The XSD type that represents the JAXB class.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs handling a message.
     */
    private void handleMessage(final Class<?> jaxbClass, final XsdType xsdType) throws JaxbProtoConvertException
    {
        final ProtoMessage protoMessage = getOrCreateMessage(jaxbClass.getSimpleName(), jaxbClass, xsdType);
        
        handleFields(protoMessage, jaxbClass);
        if (xsdType.getBaseType() != null)
        {
            final XsdType baseType = xsdType.getBaseType();
            final Class<?> baseClazz = baseType.getJaxbType();
            final ProtoMessage baseMessage = getOrCreateMessage(baseClazz.getSimpleName(), baseClazz, baseType);
            
            protoMessage.setBaseMessage(baseMessage);
            handleBaseTypeImport(protoMessage);
        }
        protoMessage.setProcessed(true);
    }
    
    /**
     * This method is responsible for determining what XML annotation a field uses and the calls the appropriate method
     * for handling a field with that annotation type.
     * 
     * @param message
     *      The {@link ProtoMessage} the converted fields will belong to.
     * @param clazz
     *      The class the with fields that are being converted to proto message fields.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs handling a field. 
     */
    private void handleFields(final ProtoMessage message, final Class<?> clazz) 
            throws JaxbProtoConvertException
    {
        final Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields)
        {
            //If the field is deprecated then do not generate it.
            if (field.getName().contains("_DEPRECATED_"))
            {
                continue;
            }
            ProtoField entry = null; 
            for (Annotation annotation : field.getAnnotations())
            {
                if (annotation.annotationType().equals(XmlElement.class))
                {
                    entry = handleXmlElement(message, field);
                    break;
                }
                else if (annotation.annotationType().equals(XmlAttribute.class))
                {
                    entry = handleXmlAttribute(message, field);
                    break;
                }
                else if (annotation.annotationType().equals(XmlValue.class))
                {
                    entry = handleXmlValue(message, field);
                    break;
                }
            }
            
            if (entry != null)
            {
                message.getFields().put(entry.getName(), entry);
            }
        }
    }
    
    /**
     * Method for converting a JAXB class field annotated with an XML element annotation into a protocol buffer
     * field.
     *
     * @param message
     *      Proto message the field resides in.
     * @param field
     *      Field that contains information about the XML element.
     * @return
     *      The created protocol buffer field based off the JAXB class field.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs handling the XML element.
     */
    private ProtoField handleXmlElement(final ProtoMessage message, final Field field) throws JaxbProtoConvertException
    {
        final XmlElement element = field.getAnnotation(XmlElement.class);
        final ProtoField entry = new ProtoField(message);
        if (element.name().equals("##default"))
        {
            entry.setName(field.getName());
        }
        else
        {
            entry.setName(element.name());
        }
        
        //Fields of the Protocol Buffer type repeated are compiled into Java lists.
        if (field.getType().equals(List.class))
        {
            handleListType(entry, field);
        }
        else
        {
            entry.setType(getBasicType(field.getType()));
            if (entry.getType() == null)
            {
                handleReference(field.getType(), entry);    
            }
            final XmlElement att = field.getAnnotation(XmlElement.class);
            entry.setRule(att.required() ? ProtoField.Rule.Required : ProtoField.Rule.Optional);
        }
        
        return entry;
    }
    
    /**
     * Method for converting a JAXB class field annotated with the XML attribute annotation into a protocol buffer 
     * field.
     * 
     * @param message
     *      Proto message the field resides in.
     * @param field
     *      Field that contains information about the XML attribute.
     * @return
     *      The created proto field based off the JAXB class field. 
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs handling the XML attribute.
     */
    private ProtoField handleXmlAttribute(final ProtoMessage message, final Field field) 
            throws JaxbProtoConvertException
    {
        final ProtoField entry = new ProtoField(message);
        final XmlAttribute att = field.getAnnotation(XmlAttribute.class);
        entry.setName(att.name());
        
        //Fields of the protocol buffer type repeated are compiled into Java lists.
        if (field.getType().equals(List.class))
        {
            handleListType(entry, field);
        }
        else
        {
            entry.setType(getBasicType(field.getType()));
            if (entry.getType() == null)
            {
                handleReference(field.getType(), entry);
            }
            entry.setRule(att.required() ? ProtoField.Rule.Required : ProtoField.Rule.Optional);
        }
        
        return entry;
    }
    
    /**
     * Method for converting a JAXB class field annotated with the XML value annotations into a protocol buffer field.
     * 
     * @param message
     *      Proto message the field resides in.
     * @param field
     *      Field that contains information about the XML value.
     * @return
     *      The proto field based off the JAXB class field.
     * @throws JaxbProtoConvertException
     *      Thrown if an error occurs handling the XML value.
     */
    private ProtoField handleXmlValue(final ProtoMessage message, final Field field) throws JaxbProtoConvertException
    {
        final ProtoField entry = new ProtoField(message);
        entry.setName(field.getName());
        entry.setRule(ProtoField.Rule.Required);
        
        entry.setType(getBasicType(field.getType()));
        if (entry.getType() == null)
        {
            handleReference(field.getType(), entry);
        }
        
        return entry;
    }
    
    /**
     * This method is responsible for converting any fields within a JAXB class of the type {@link List} into a
     * protocol buffer compatible field.
     * 
     * @param entry
     *      The {@link ProtoField} the converted field is to be saved to.
     * @param field
     *      The field to be converted from a {@link List} to protocol buffer field type.
     * @throws JaxbProtoConvertException 
     *      Thrown if an error occurs handling the list type.
     */
    private void handleListType(final ProtoField entry, final Field field) throws JaxbProtoConvertException
    {
        entry.setRule(ProtoField.Rule.Repeated);
        if (field.getGenericType() instanceof ParameterizedType) 
        {  
            final ParameterizedType pt = (ParameterizedType) field.getGenericType();
            if (pt.getActualTypeArguments().length != 1)
            {
                throw new IllegalArgumentException(
                        String.format("Field %s is a list, expecting one type arguement, found %d", 
                                field.getName(), pt.getActualTypeArguments().length));
            }
            final Class<?> type = (Class<?>)pt.getActualTypeArguments()[0];
            entry.setType(getBasicType(type));
            if (entry.getType() == null)
            {
                handleReference(type, entry);
            }
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Field %s is a list so must be parameterized", field.getName()));
        }
    }
    
    /**
     * Method that handles a field that references another message or enumeration.
     *  
     * @param refClass
     *      Class type of the reference message or enumeration.
     * @param field
     *      Model that represents the reference message or enumeration.
     * @throws JaxbProtoConvertException
     *      Thrown if reference type is not known.
     */
    private void handleReference(final Class<?> refClass, final ProtoField field) throws JaxbProtoConvertException
    {
        final XsdType refXsdType = findXsdType(refClass);
        final ProtoElement refElement;
        if (refXsdType == null)
        {
            refElement = getNonGeneratedType(refClass);
            field.setType(ProtoType.Reference);
        }
        else if (refClass.isEnum())
        {
            refElement = getOrCreateEnum(refClass, refXsdType);
            field.setType(ProtoType.Enum);
        }
        else
        {
            refElement = getOrCreateMessage(refClass.getSimpleName(), refClass, refXsdType);
            field.setType(ProtoType.Reference);
        }
        field.setTypeRef(refElement);
        handleRefImport(field, refClass);
    }
    
    /**
     * Method that is used to handle a reference to a non-generated method or enumeration.
     * 
     * @param refClass
     *      The class of the non-generated type being referenced.
     * @return
     *      Element that represents the non-generated class.
     */
    private ProtoElement getNonGeneratedType(final Class<?> refClass)
    {
        final String elementName;
        if (refClass.equals(Object.class))
        {
            elementName = "Multitype";
        }
        else
        {
            elementName = refClass.getSimpleName();
        }
        
        if (m_ImportableMessages.containsKey(elementName))
        {
            if (refClass.isEnum())
            {
                final ProtoEnum protoEnum = new ProtoEnum(null, null, elementName, refClass, null);
                return protoEnum;
            }
            final ProtoMessage protoMsg = new ProtoMessage(null, elementName, refClass, null);
            return protoMsg;
        }
        throw new IllegalArgumentException(String.format("No non-generated message with name %s available for import", 
                elementName));
    }
    
    /**
     * Method that either retrieves the message with specified name or creates a new one.
     * 
     * @param messageName
     *      The name of the message to be retrieved or created.
     * @param jaxbClass
     *      The JAXB class type the message represents.
     * @param xsdType
     *      The XSD type used to retrieve the XSD file the message belongs to.
     * @return
     *      The retrieved or created message.
     */
    private ProtoMessage getOrCreateMessage(final String messageName, final Class<?> jaxbClass,
            final XsdType xsdType)
    {
        ProtoFile protoFile = m_ProtoModel.getProtoFileMap().get(xsdType.getXsdFile());
        
        if (protoFile == null)
        {
            protoFile = new ProtoFile(m_ProtoModel, xsdType.getXsdFile());
            m_ProtoModel.getProtoFileMap().put(xsdType.getXsdFile(), protoFile);
        }
        
        ProtoMessage protoMessage = protoFile.getMessageMap().get(messageName);
        if (protoMessage == null)
        {
            protoMessage = new ProtoMessage(protoFile, messageName, jaxbClass, xsdType);
            protoFile.getMessageMap().put(messageName, protoMessage);
        }
        return protoMessage;
    }
    
    /**
     * Method that either retrieves an enumeration of the specified type or creates a new.
     * 
     * @param jaxbClass
     *      The JAXB class that represents the enumeration.
     * @param xsdType
     *      XSD type that represents the reference JAXB type.
     * @return
     *      The retrieved or created enumeration.
     */
    private ProtoEnum getOrCreateEnum(final Class<?> jaxbClass, final XsdType xsdType)
    {
        final Class<?> baseEnumClass = findEnumBaseClass(jaxbClass);
        final ProtoMessage protoMessage;
        if (baseEnumClass != null)
        {
            final XsdType enumBaseXsdType = findXsdType(baseEnumClass);
            protoMessage = getOrCreateMessage(baseEnumClass.getSimpleName(), baseEnumClass, enumBaseXsdType);
        }
        else
        {
            final String messageName = jaxbClass.getSimpleName().replace(ProtoEnum.ENUM, "");
            protoMessage = getOrCreateMessage(messageName, null, xsdType);
        }
        
        ProtoEnum protoEnum = protoMessage.getEnumeration();
        if (protoEnum == null)
        {
            protoEnum = new ProtoEnum(protoMessage, protoMessage.getProtoFile(), ProtoEnum.ENUM, jaxbClass, xsdType);
            protoMessage.setEnumeration(protoEnum);
        }
        
        return protoEnum;
    }
    
    /**
     * Method used to determine if an import statement needs to be added for the reference type field.
     * 
     * @param field
     *      Model that represents the field that is a reference to another message.
     * @param refClass
     *      Class type of the reference class.
     * @throws JaxbProtoConvertException
     *      Thrown if the reference type cannot be found. 
     */
    private void handleRefImport(final ProtoField field, final Class<?> refClass) throws JaxbProtoConvertException  
    {
        //Get the proto message the field is within.
        final ProtoMessage protoMsg = field.getProtoMessage();
        //Get XSD type that represents the proto message the field is within.
        final XsdType msgXsdType = protoMsg.getXsdType();
        final ProtoFile protoFile = protoMsg.getProtoFile();
        final Set<String> imports = protoFile.getImports();
        
        final XsdType refXsdType;
        if (refClass.isEnum())
        {
            //If the reference type is an enumeration then attempt to retrieve the XSD type that represents the 
            //reference type based off the enumeration base type. Else, if there is no base enumeration type then
            //use the reference type to retrieve the XSD type that represents the enumeration.
            final Class<?> enumBaseClass = findEnumBaseClass(refClass);
            refXsdType = enumBaseClass != null ? findXsdType(enumBaseClass) : findXsdType(refClass);
        }
        else
        {
            //Get the XSD type that represents the reference type.
            refXsdType = findXsdType(refClass);
        }
        
        //If the reference XSD type is null then the field must reference a non-generated type.
        if (refXsdType == null)
        {
            final String elementName = field.getTypeRef().getName();
            //Check if there is a non-generated type that can be imported and add the appropriate import statement, 
            //otherwise throw an exception.
            if (m_ImportableMessages.containsKey(elementName))
            {
                imports.add(m_ImportableMessages.get(elementName));
            }
            else
            {
                throw new JaxbProtoConvertException(String.format("Reference type %s for message %s is not a known "
                        + "generated or non-generated type", refClass.getName(), protoMsg.getName()));
            }
        }
        //If the reference XSD type exists, then check to see if the message and reference type are located in the
        //different XSD files. If they are in different XSD files then an import statement is added.
        else if (!msgXsdType.getXsdFile().equals(refXsdType.getXsdFile()))
        {
            final String importStatement = buildImportStatement(refXsdType.getXsdFile());
            imports.add(importStatement);
        }
    }
    
    /**
     * Method that handles adding needed import statement for a message that extends another type.
     * 
     * @param message
     *      Message that extends another type.
     */
    private void handleBaseTypeImport(final ProtoMessage message)
    {
        final XsdType xsdType = message.getXsdType();
        final XsdType baseType = xsdType.getBaseType();
        final ProtoFile protoFile = message.getProtoFile();
        final Set<String> imports = protoFile.getImports();
            
        if (!xsdType.isComplexRestriction() && !xsdType.getXsdFile().equals(baseType.getXsdFile()))
        {
            final String importStatement = buildImportStatement(baseType.getXsdFile());
            imports.add(importStatement);
        }
    }
    
    /**
     * Method that builds an import statement for the specified XSD file.
     * 
     * @param importFile
     *      File that represents the XSD file to be imported.
     * @return
     *      String that represents the import statement.
     */
    private String buildImportStatement(final File importFile)
    {
        final Path fileDirPath = importFile.toPath();
        final Path baseDirPath = m_XsdFilesDir.toPath();
        final Path relativePath = baseDirPath.relativize(fileDirPath);
        
        final String replacedSeparator = relativePath.toString().replace(File.separator, "/");
        final String importStatement = replacedSeparator.replace(".xsd", ".proto");
        return importStatement;
    }
    
    /**
     * The method responsible determining a generated protocol buffer field's type from the JAXB class field type.
     *   
     * @param clazz
     *      The class type of the JAXB class's field.
     * @return
     *      The protocol buffer field type or null if the type should be treated as a reference
     */
    private ProtoType getBasicType(final Class<?> clazz)//NOCHECKSTYLE method is fairly straight forward but could be
                                                        //broken up slightly. However this has been avoided due to a
                                                        //checkstyle error with the conditional logic check.
    {
        if (clazz.equals(Integer.class) || clazz.equals(int.class))
        {
            return ProtoType.Int32;
        }
        else if (clazz.equals(String.class))
        {
            return ProtoType.String;
        }
        else if (clazz.equals(Long.class) || clazz.equals(long.class))
        {
            return ProtoType.Int64;
        }
        else if (clazz.equals(Double.class) || clazz.equals(double.class))
        {
            return ProtoType.Double;
        }
        else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class))
        {
            return ProtoType.Boolean;
        }
        else if (clazz.equals(byte[].class))
        {
            return ProtoType.Bytes;
        }
        else if (clazz.equals(Float.class) || clazz.equals(float.class))
        {
            return ProtoType.Float;
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Find the XSD type for the specified JAXB class.
     * 
     * @param clazz
     *      The JAXB class to find the XSD type for.
     * @return
     *      The XSD type that represents the JAXB class or null if there is no such XSD type.
     */
    private XsdType findXsdType(final Class<?> clazz)
    {
        for (XsdNamespace namespace: m_XsdModel.getNamespacesMap().values())
        {
            if (namespace.getTypesMap().containsKey(clazz))
            {
                return namespace.getTypesMap().get(clazz);
            }
        }
        return null;
    }
    
    /**
     * Method used to find the base class for a enumeration.
     * 
     * @param enumClass
     *          Enumeration class to find the associated base class for.
     * @return
     *          The base class for the enumeration or null if one does not exist.
     */
    private Class<?> findEnumBaseClass(final Class<?> enumClass)
    {
        try
        {
            return Class.forName(enumClass.getName().replace(ProtoEnum.ENUM, ""));
        }
        catch (final ClassNotFoundException ex)
        {   //NOCHECKSTYLE
            //Class might not exist.
        }
        
        //Array of all JAXB packages. Used to check for enumeration base classes.
        //TODO TH-3097: Remove this array and associated logic.
        final String[] packages = 
        {
            "mil.dod.th.core.asset.capability",
            "mil.dod.th.core.asset.commands",
            "mil.dod.th.core.asset.types.ccbase",
            "mil.dod.th.core.capability",
            "mil.dod.th.core.ccomm.link.capability",
            "mil.dod.th.ocre.ccomm.physical.capability",
            "mil.dod.th.core.ccomm.transport.capability",
            "mil.dod.th.core.controller.capability",
            "mil.dod.th.core.observation.types",
            "mil.dod.th.core.types",
            "mil.dod.th.core.types.spatial"
        };
        
        final String simpleClassName = enumClass.getSimpleName().replace(ProtoEnum.ENUM, "");
        for (String packageName: packages)
        {
            try
            {
                return Class.forName(packageName + "." + simpleClassName); 
            }
            catch (final ClassNotFoundException ex)
            {   //NOCHECKSTYLE
                //Class might not exist.
            }
        }
        return null;
    }
}

