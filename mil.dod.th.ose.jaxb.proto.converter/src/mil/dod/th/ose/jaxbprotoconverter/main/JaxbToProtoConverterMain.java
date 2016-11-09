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
package mil.dod.th.ose.jaxbprotoconverter.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import mil.dod.th.ose.jaxbprotoconverter.EnumConverterFileGenerator;
import mil.dod.th.ose.jaxbprotoconverter.JaxbClassParser;
import mil.dod.th.ose.jaxbprotoconverter.JaxbProtoConvertException;
import mil.dod.th.ose.jaxbprotoconverter.ProtoFileGenerator;
import mil.dod.th.ose.jaxbprotoconverter.ProtoFileParser;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdParser;
import mil.dod.th.ose.utils.FileService;
import mil.dod.th.ose.utils.UtilInjectionModule;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 * Converts JAXB classes to proto files.
 * 
 * @author dhumeniuk
 *
 */
public class JaxbToProtoConverterMain
{
    /**
     * Format string used to build a file path for Java packages.
     */
    private static final String PKG_PATH_FORMAT = "%s" + File.separator + "%s"; // NOCHECKSTYLE: Duplicate string is more readable
    
    /**
     * Converter used to create the actual proto file.
     */
    private ProtoFileGenerator m_ProtoFileGenerator;

    /**
     * Used to generate enumeration converter classes.
     */
    private EnumConverterFileGenerator m_ProtoEnumToConverterFile;
    
    /**
     * Service that parses XSD files.
     */
    private XsdParser m_XsdParser;
    
    /**
     * Service used to handle file operations.
     */
    private FileService m_FileService;
    
    /**
     * Service used to parse JAXB classes.
     */
    private JaxbClassParser m_JaxbClassParser;
    
    /**
     * Service used to pare proto files.
     */
    private ProtoFileParser m_ProtoFileParser;
    
    /**
     * Base constructor.
     * 
     * @param protoFileGenerator
     *      converter for creating the proto file
     * @param xsdParser
     *      service used to parse XSD files
     * @param protoEnumToConverterFile
     *      converter for creating enumeration converter file
     * @param fileService
     *      service used to handle file operations
     * @param jaxbClassParser
     *      class used to parse JAXB classes
     * @param protoFileParser
     *      service used to parse proto files
     */
    @Inject
    public JaxbToProtoConverterMain(final ProtoFileGenerator protoFileGenerator, final XsdParser xsdParser,
            final EnumConverterFileGenerator protoEnumToConverterFile, final FileService fileService, 
            final JaxbClassParser jaxbClassParser, final ProtoFileParser protoFileParser)
    {
        m_ProtoFileGenerator = protoFileGenerator;
        m_ProtoEnumToConverterFile = protoEnumToConverterFile;
        m_XsdParser = xsdParser;
        m_FileService = fileService;
        m_JaxbClassParser = jaxbClassParser;
        m_ProtoFileParser = protoFileParser;
    }

    /**
     * Main method responsible for calling all methods and instantiating all objects needed to convert the various 
     * classes from JAXB classes to compatible Google Protocol Buffer proto files.
     * 
     * @param args
     *          The fully qualified class name of the class to be converted to a proto file.    
     * @throws Exception
     *          If the program failed during execution
     */
    public static void main(final String[] args) throws Exception
    {   
        try
        {
            final Injector injector = Guice.createInjector(new UtilInjectionModule(), new LocalInjectionModule());
            final JaxbToProtoConverterMain converter = injector.getInstance(JaxbToProtoConverterMain.class);
            converter.processCommandLine(System.out, args);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Process the command with the given command line arguments.
     * 
     * @param out 
     *      where to print out message to
     * @param args
     *      command line arguments from main(), see {@link #getUsage()} for details
     * @throws JaxbProtoConvertException
     *      if there is a failure in process the generation of proto files
     */
    public void processCommandLine(final PrintStream out, final String[] args) throws JaxbProtoConvertException
    {
        final int schemasLocation = 0;
        final int protoDestination = 1;
        final int javaDestination = 2;
        final int baseProtoPackageName = 3;
        final int enumConvertersPackageName = 4;
        final int importsFolder = 5;
        final int expectedNumArgs = 6;

        if (args.length < expectedNumArgs)
        {
            out.println(getUsage());
            return;
        }

        process(new File(args[schemasLocation]), new File(args[protoDestination]), new File(args[javaDestination]), 
                args[baseProtoPackageName], args[enumConvertersPackageName], args[importsFolder]);
    }

    /**
     * Process the JAXB classes and create proto files from them.
     * 
     * @param xsdFilesDir
     *      directory where XSD files that should be parsed are located.
     * @param protoDestPath
     *      where to put the generated proto files
     * @param javaDestPath
     *      where to put the generated Java files
     * @param baseProtoPackageName
     *      Java package name to use for the proto message files
     * @param enumConvertersPackageName
     *      Java package name to use for the enumeration converters
     * @param importableProtoPath
     *      path containing proto files that can be referenced by proto files being created
     * @throws JaxbProtoConvertException
     *      if there is a problem processing the JAXB classes
     */
    public void process(final File xsdFilesDir, final File protoDestPath, final File javaDestPath, 
            final String baseProtoPackageName, final String enumConvertersPackageName, 
            final String importableProtoPath) throws JaxbProtoConvertException
    {
        System.out.format("Generating proto files to %s%n", protoDestPath);
        System.out.format("Generating Java files to %s%n", javaDestPath);
        
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder;
        try
        {
            docBuilder = builderFactory.newDocumentBuilder();
        }
        catch (final ParserConfigurationException ex)
        {
            throw new JaxbProtoConvertException(ex);
        }
        
        final XsdModel xsdModel;
        try
        {
            xsdModel = m_XsdParser.parseXsdFiles(docBuilder, xsdFilesDir, createXsdFileList(xsdFilesDir));
        }
        catch (final ClassNotFoundException | ParserConfigurationException | SAXException | IOException ex)
        {
            throw new JaxbProtoConvertException(ex);
        }
        Map<String, String> imports = new HashMap<String, String>();
        try
        {
            imports = m_ProtoFileParser.parseProtoFilesForTypes(importableProtoPath);
        }
        catch (final IOException ex)
        {
            throw new JaxbProtoConvertException(ex);
        }
        final ProtoModel protoModel = 
                m_JaxbClassParser.parseJaxbClasses(xsdModel, imports, xsdFilesDir);
        try
        {
            m_ProtoFileGenerator.generateProtoFiles(protoModel, xsdFilesDir, protoDestPath, baseProtoPackageName);
        }
        catch (final IOException ex)
        {
            throw new JaxbProtoConvertException(ex);
        }

        final String enumConvertersPath = String.format(PKG_PATH_FORMAT, javaDestPath.getPath(),
                enumConvertersPackageName.replace('.', File.separatorChar));
        final File enumConvertersDir = m_FileService.getFile(enumConvertersPath);
        try
        {
            m_ProtoEnumToConverterFile.generateEnumConverters(protoModel, enumConvertersDir, xsdFilesDir, 
                    "EnumConverter", baseProtoPackageName, enumConvertersPackageName);
        }
        catch (final FileNotFoundException exception)
        {
            throw new JaxbProtoConvertException(exception);
        }
    }
    
    /**
     * Returns the application line usage help when no arguments are passed to the program.
     * 
     * @return command line usage info as a string    
     */
    public static String getUsage()
    {
        final StringBuilder usage = new StringBuilder(400);
        
        usage.append("Invalid number of arguments!\n"
                    + "Usage: java -jar mil.dod.th.ose.jaxbprotoconverter.jar "
                    + "<XSD Schemas Location>"
                    + "<Proto Destination> "
                    + "<Java Destination> "
                    + "<Shared Messages Package Name> "
                    + "<Enum Converters Package Name> "
                    + "<Folder With Importable Proto Files>");
        
        return usage.toString();
    }
    
    /**
     * Creates a list of all XSD files contained within the specified directory.
     * 
     * @param xsdFilesDir
     *      The directory where XSD files are located.
     * @return
     *      A list of files which represents all XSD files within the specified directory.
     */
    private List<File> createXsdFileList(final File xsdFilesDir)
    {
        if (!xsdFilesDir.exists() || !xsdFilesDir.isDirectory())
        {
            throw new IllegalArgumentException(String.format("Invalid path specified for XSD file location: %s", 
                    xsdFilesDir.getAbsolutePath()));
        }
        
        @SuppressWarnings("unchecked")
        final List<File> xsdFiles = new ArrayList<File>(FileUtils.listFiles(xsdFilesDir, new String[]{"xsd"}, true));
        
        return xsdFiles;
    }
}
