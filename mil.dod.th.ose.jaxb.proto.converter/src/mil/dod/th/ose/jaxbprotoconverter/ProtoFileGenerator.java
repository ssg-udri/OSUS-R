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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.inject.Inject;

import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoElement;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoEnum;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.ProtoType;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoFile;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoMessage;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdField;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;
import mil.dod.th.ose.utils.FileService;

/**
 * This class is capable of taking a {@link ProtoModel} class and converting into a Google Protocol Buffer compatible 
 * proto file or outputting the converted JAXB class to any specified output.
 * 
 * @author Dave Humeniuk
 */
public class ProtoFileGenerator
{
    private static final String PACKAGE_SEPARATOR = ".";
    
    /**
     * Integer that represents the index reserved for a nested base message.
     */
    private static final int BASE_MESSAGE_INDEX = 50;
    
    /**
     * Reference to the service used to handle file operations.
     */
    private final FileService m_FileService;
    
    /**
     * Base Constructor.
     * 
     * @param fileService
     *      File service used to handle file operations.
     */
    @Inject
    public ProtoFileGenerator(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Generates all proto files based on the specified {@link ProtoModel}.
     * 
     * @param protoModel
     *      The proto model that contains the information on the proto files to be generated.
     * @param xsdFilesDir
     *      Base directory in which all XSD schema files are located.
     * @param outputDir
     *      The file that represents the directory the proto files should be generated to.
     * @param baseJavaPackageName
     *      The base package name used for Java package proto file option.
     * @throws JaxbProtoConvertException
     *      Thrown if an exception occurs creating a proto file.
     * @throws IOException
     *      Thrown if an exception occurs writing a proto file.
     */
    public void generateProtoFiles(final ProtoModel protoModel, final File xsdFilesDir, final File outputDir, 
            final String baseJavaPackageName) throws JaxbProtoConvertException, IOException
    {
        for (ProtoFile protoFile: protoModel.getProtoFileMap().values())
        {
            generateProtoFile(protoFile, xsdFilesDir, outputDir, baseJavaPackageName);
        }
    }
    
    /**
     * Method that generates proto file based on the {@link ProtoFile} model.
     * 
     * @param protoFile
     *      The proto file model that represents the proto file to be generated.
     * @param xsdFilesDir
     *      Base directory in which all XSD schema files are located.
     * @param outputDir
     *      File that represents the directory the proto file should be generated to.
     * @param baseJavaPackageName
     *      Base package name used for Java package proto file option.
     * @throws JaxbProtoConvertException
     *      Thrown if an exception occurs creating the proto file.
     * @throws IOException
     *      Thrown if an exception occurs writing to the proto file.
     */
    public void generateProtoFile(final ProtoFile protoFile, final File xsdFilesDir, final File outputDir, 
            final String baseJavaPackageName) throws JaxbProtoConvertException, IOException
    {
        final File xsdFile = protoFile.getXsdFile();
        //Name of the file without any extension.
        final String baseName = xsdFile.getName().replace(".xsd", "");
        //Name of the Java class when compiled.
        final String javaClassName = baseName + "Gen";
        //Name of the proto file to be generated.
        final String protoFileName = baseName + ".proto";
        //Build the relative path the proto file should be generated to based on the XSD path.
        final Path relativePath = PathUtils.getRelativePath(xsdFile.getParentFile(), xsdFilesDir);
        //Get the file that the proto file should be generated to.
        final File protoFileDir = new File(outputDir.getAbsolutePath() + File.separator + relativePath.toString());
        //Create any directories that don't exist.
        if (!protoFileDir.exists() && !protoFileDir.mkdirs())
        {
            throw new JaxbProtoConvertException(String.format("Couldn't create proto file output directory: %s", 
                    protoFileDir.getAbsolutePath()));
        }
        //Build the Java package the proto file belongs to when compiled.
        final String javaPackage = baseJavaPackageName + PACKAGE_SEPARATOR 
                + relativePath.toString().replace(File.separator, PACKAGE_SEPARATOR);
        
        //Get the print stream used to write the proto file.
        final PrintStream outStream = m_FileService.createPrintStream(new File(protoFileDir, protoFileName));
         
        final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss\n", Locale.US);
        final long currentDate = new Date().getTime();
        
        //Output the date the proto file was generated on, Java package the compiled proto belongs to, and the class 
        //name of the compiled Java file.
        outStream.format("//Generated By: JAXB to Proto Converter on %s%n", dateFormat.format(currentDate));
        outStream.format("option java_package = \"%s\";%n", javaPackage);
        outStream.format("option java_outer_classname = \"%s\";%n", javaClassName);
        outStream.println();
        
        //Output all import statements needed by the proto file.
        for (String importStatement: protoFile.getImports())
        {
            outStream.format("import \"%s\";%n", importStatement);
        }
        outStream.println();
        
        //Output all messages contained within the proto file.
        for (ProtoMessage message: protoFile.getMessageMap().values())
        {
            printMessage(outStream, message);
        }
    }

    /**
     * This method is responsible for taking a specified {@link ProtoEnum} and writing it to the provided
     * {@link PrintStream}.
     * 
     * @param outStream
     *      The output stream the {@link ProtoEnum} will be written to.
     * @param enumeration
     *      The {@link ProtoEnum} that will be written to the {@link PrintStream}.
     * @throws JaxbProtoConvertException
     *      Thrown if the enumeration contains no values.
     */
    private void printEnum(final PrintStream outStream, final ProtoEnum enumeration) throws JaxbProtoConvertException
    {
        outStream.format("  enum %s {%n", enumeration.getName());

        if (enumeration.getValues().isEmpty())
        {
            throw new JaxbProtoConvertException(String.format("Empty enumeration %s encountered while printing "
                    + "message %s", enumeration.getType().getSimpleName(), enumeration.getProtoMessage().getName()));
        }
        
        int index = 1;
        for (String value : enumeration.getValues())
        {
            outStream.format("    %s = %d;%n", value, index++); // NOCHECKSTYLE: Duplicate string is more readable
        }

        outStream.println("  }"); // NOCHECKSTYLE: Duplicate string is more readable
    }

    /**
     * This method is responsible for taking a specified {@link ProtoMessage} and writing it to the provided 
     * {@link PrintStream}. 
     * 
     * @param outStream
     *      The output stream the {@link ProtoMessage} will be written to.
     * @param message
     *      The {@link ProtoMessage} that will be written to the {@link PrintStream}.
     * @throws JaxbProtoConvertException
     *      If the {@link ProtoMessage} cannot successfully be written to the specified {@link PrintStream}.
     * @throws JaxbProtoConvertException
     *      If the proto message is empty.
     */
    private void printMessage(final PrintStream outStream, final ProtoMessage message) 
            throws JaxbProtoConvertException
    {
        final String prefix = "message %s {%n";
        
        outStream.format(prefix, message.getName());
        
        printMessageContents(outStream, message);

        outStream.println("}");
        outStream.println();
    }
    
    /**
     * Method that prints the contents of a {@link ProtoMessage}.
     * 
     * @param outStream
     *      The output stream the {@link ProtoMessage} contents will be written to.
     * @param message
     *      The {@link ProtoMessage} whose contents will be written to the {@link PrintStream}.
     * @throws JaxbProtoConvertException
     *      If the proto message is empty.
     */
    private void printMessageContents(final PrintStream outStream, final ProtoMessage message) 
            throws JaxbProtoConvertException
    {
        final XsdType xsdType = message.getXsdType();
        final ProtoMessage baseMessage = message.getBaseMessage();
        if (baseMessage != null && xsdType.isComplexRestriction())
        {
            printMessageContents(outStream, baseMessage);
        }
        else if (baseMessage != null && !xsdType.isComplexRestriction())
        {
            outStream.format("  required %s _base = %d;%n", baseMessage.getName(), BASE_MESSAGE_INDEX);
        }
        
        final ProtoEnum nestedEnum = message.getEnumeration();
        if (nestedEnum != null)
        {
            printEnum(outStream, nestedEnum);
        }
        
        if (message.getFields().isEmpty() 
                && nestedEnum == null 
                && xsdType.getBaseType() == null)
        {
            throw new JaxbProtoConvertException(String.format("Message %s of type %s is empty, this should not occur", 
                    message.getName(), message.getClass().getName()));
        }
        printAllFields(message, xsdType, outStream);
    }
    
    /**
     * Prints all fields for the specified message.
     * 
     * @param message
     *      The message to containing the fields to be printed.
     * @param xsdType
     *      The XSD type that represents the message.
     * @param outStream
     *      Print stream the message should be printed to.
     * @throws JaxbProtoConvertException
     *      Thrown if a field cannot be printed.
     */
    private void printAllFields(final ProtoMessage message, final XsdType xsdType, final PrintStream outStream) 
            throws JaxbProtoConvertException
    {
        for (String fieldName: xsdType.getFieldsMap().keySet())
        {
            //TD: May want to update converter to have generated proto files use the prtoobuf deprecated flag for
            //deprecated fields instead of not generating them. Also need to determine if the deprecated prefix and 
            //handling of deprecated fields is needed since renaming a field by prefixing it with "_DEPRECATED_" 
            //technically is an API breaking change.
            if (fieldName.contains("_DEPRECATED_"))
            {
                continue;
            }
            final ProtoField field = message.getFields().get(fieldName);
            final XsdField xsdField = xsdType.getFieldsMap().get(fieldName);
            //Check if the field index is the same as the index reserved for base messages. If it is then 
            //increment it by 1.
            final int fieldIndex = xsdField.getIndex() >= BASE_MESSAGE_INDEX 
                    ? xsdField.getIndex() + 1 : xsdField.getIndex();
            final String fieldString = buildFieldString(field, fieldIndex);
            try
            {
                printField(outStream, fieldString, xsdField.isIndexOverridden());
            }
            catch (final Exception e)
            {
                throw new JaxbProtoConvertException("Unable to print field " + field.getName() + " for message " 
                        + message.getName(), e);
            }            
        }   
    }

    /**
     * This method is responsible for taking a specified {@link ProtoField} and creating the string representation of
     * that field.
     * 
     * @param field
     *      The {@link ProtoField} to be converted to its string representation.
     * @param index
     *      The index value to be printed for the field.
     * @return
     *      The string representation of the field.
     */
    private String buildFieldString(final ProtoField field, final int index)
    {
        final StringBuilder fieldBuilder = new StringBuilder("  ");
        
        final String packString = appendRule(field, fieldBuilder);
        
        switch (field.getType())
        {
            case Bytes:
                fieldBuilder.append("bytes");
                break;
            case Int32:
                fieldBuilder.append("int32");
                break;
            case Int64:
                fieldBuilder.append("int64");
                break;
            case Reference:
                fieldBuilder.append(field.getTypeRef().getName());
                break;
            case Enum:
                final ProtoElement element = field.getTypeRef();
                fieldBuilder.append(element.getType().getSimpleName().replace(ProtoEnum.ENUM, PACKAGE_SEPARATOR 
                        + element.getName()));
                break;
            case String:
                fieldBuilder.append("string");
                break;
            case UInt32:
                fieldBuilder.append("uint32");
                break;
            case UInt64:
                fieldBuilder.append("uint64");
                break;
            case Double:
                fieldBuilder.append("double");
                break;
            case Float:
                fieldBuilder.append("float");
                break;
            case Boolean:
                fieldBuilder.append("bool");
                break;
            default:
                throw new IllegalStateException("Invalid field type: " + field.getType()); 
        }
        
        fieldBuilder.append(String.format(" %s = %d%s;%n", field.getName(), index, packString));

        return fieldBuilder.toString();
    }
    
    /**
     * Method that prints the string that represents a field to the specified {@link PrintStream}.
     * 
     * @param outStream
     *      The print stream the field string should be printed to.
     * @param fieldString
     *      The string which represents the field to be printed.
     * @param indexOverridden
     *      Whether or not the field has an index which has been overridden.
     */
    private void printField(final PrintStream outStream, final String fieldString, final boolean indexOverridden)
    {
        if (indexOverridden)
        {
            final String overrideWarning = "  //------|Index Overridden|------//";
            outStream.format("%n%s%n%s%s%n%n", overrideWarning, fieldString, overrideWarning);
        }
        else
        {
            outStream.print(fieldString);
        }
    }
    
    /**
     * This method is responsible for appending the {@link mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule} 
     * type for each field.
     * 
     * @param field
     *      The {@link ProtoField} with the {@link mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule} to be 
     *      appended.
     * @param fieldBuilder
     *      The {@link StringBuilder} the {@link mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule} type is to 
     *      be appended to.
     * @return packString
     *      A string is returned that is not empty if a {@link mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule} 
     *      is of the repeated type and is not of the {@link ProtoType} type of reference. 
     */
    private String appendRule(final ProtoField field, final StringBuilder fieldBuilder)
    {
        String packString = "";
        
        switch (field.getRule())
        {
            case Optional:
                fieldBuilder.append("optional ");
                break;
                
            case Repeated:
                fieldBuilder.append("repeated ");
                if (field.getType() != ProtoType.Reference 
                        && field.getType() != ProtoType.Enum 
                        && field.getType() != ProtoType.String)
                {
                    packString = " [packed=true]";
                }
                break;
                
            case Required:
                fieldBuilder.append("required ");
                break;
        
            default:
                throw new IllegalStateException("Invalid field qualifier: " + field.getRule());    
        }
        
        return packString;
    }
}
