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
package mil.dod.th.ose.sdk.those;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Map;

import com.google.inject.Inject;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.Gpio;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.utils.FileService;

/**
 * Handles the generation of a Terra Harvest plug-in project.
 * 
 * @author dlandoll
 */
public class ProjectGenerator
{
    private static final String SETTINGS_DIR = ".settings";

    /**
     * Template configuration.
     */
    private Configuration m_TemplateConfig;

    /**
     * Console output stream.
     */
    private PrintStream m_OutStream;

    /**
     * Service for accessing file related info using an interface.
     */
    private FileService m_FileService;

    /**
     * Default constructor.
     * 
     * @param fileService
     *            service to access files
     */
    @Inject
    public ProjectGenerator(final FileService fileService)
    {
        m_FileService = fileService;
    }

    /**
     * Initializes the project generator.
     * 
     * @param templateConfig
     *            Freemarker template configuration
     * @param outStream
     *            Print stream to use for console output
     */
    public void initialize(final Configuration templateConfig, final PrintStream outStream)
    {
        m_TemplateConfig = templateConfig;
        m_OutStream = outStream;
    }

    /**
     * Creates a project using the given properties in the desired directory.
     * 
     * @param projectProps
     *            Project properties used for the file templates
     * @param projectDir
     *            Directory to create the project in
     * @throws IOException
     *             if there is a file I/O error
     * @throws TemplateException
     *             if there is an error processing the template
     */
    public void create(final ProjectProperties projectProps, final File projectDir)
            throws IOException, TemplateException
    {
        preProcess(projectProps);

        // Create project directory if needed
        if (!projectDir.exists())
        {
            final boolean created = projectDir.mkdir();
            if (created)
            {
                m_OutStream.println("Created project directory: " + projectDir.getPath());
            }
            else
            {
                m_OutStream.println("Failed to create project directory: " + projectDir.getPath());
            }
        }

        // Create project directory layout
        File newDir = m_FileService.getFile(projectDir, "bin");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, "lib");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, "reports");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, "src");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, "test");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, "test-bin");
        newDir.mkdir();

        newDir = m_FileService.getFile(projectDir, SETTINGS_DIR);
        newDir.mkdir();

        // Generate files at the root of the project
        generateFileFromTemplate("build.properties.ftl", projectProps, projectDir);
        generateFileFromTemplate("build.xml.ftl", projectProps, projectDir);
        generateFileFromTemplate("local.properties.ftl", projectProps, projectDir);
        generateFileFromTemplate(".project.ftl", projectProps, projectDir);
        generateFileFromTemplate(".classpath.ftl", projectProps, projectDir);
        generateFileFromTemplate("bnd.bnd.ftl", projectProps, projectDir);

        // Create directories needed under src/ and test/
        final String packagePath = projectProps.getPackageName().replace('.', File.separatorChar);

        final File sourceDir = m_FileService.getFile(projectDir, "src/" + packagePath);
        sourceDir.mkdirs();

        final File testDir = m_FileService.getFile(projectDir, "test/" + packagePath);
        testDir.mkdirs();

        final File settingsDir = m_FileService.getFile(projectDir, SETTINGS_DIR);
        settingsDir.mkdirs();
        generateFileFromTemplate("org.eclipse.jdt.core.prefs.ftl", projectProps, settingsDir);

        generateFileFromTemplate("ExampleAttributes.java.ftl", projectProps, sourceDir);

        // Create files based on the project type
        switch (projectProps.getProjectType())
        {
            case PROJECT_ASSET:
                generateFileFromTemplate("ExampleAsset.java.ftl", projectProps, sourceDir);
                generateFileFromTemplate("ExampleAssetScanner.java.ftl", projectProps, sourceDir);
                generateCapabilitiesXml(projectProps, projectDir, ProjectType.PROJECT_ASSET);
                break;

            case PROJECT_LINKLAYER:
                generateFileFromTemplate("ExampleLinkLayer.java.ftl", projectProps, sourceDir);
                generateCapabilitiesXml(projectProps, projectDir, ProjectType.PROJECT_LINKLAYER);
                break;

            case PROJECT_PHYLINK:
                generateFileFromTemplate("ExamplePhysicalLink.java.ftl", projectProps, sourceDir);
                generateCapabilitiesXml(projectProps, projectDir, ProjectType.PROJECT_PHYLINK);
                break;

            case PROJECT_TRANSPORTLAYER:
                generateFileFromTemplate("ExampleTransportLayer.java.ftl", projectProps, sourceDir);
                generateCapabilitiesXml(projectProps, projectDir, ProjectType.PROJECT_TRANSPORTLAYER);
                break;

            default:
                throw new IllegalArgumentException("Invalid project type is set for create()");
        }
    }

    /**
     * Perform any pre-processing of the properties before processing the template files.
     * 
     * @param props
     *            properties of the project
     */
    private void preProcess(final ProjectProperties props)
    {
        props.setAttributeType(determineAttributeType(props.getProjectType(), props.getPhysicalLinkTypeEnum()));
        props.setProxyType(determineProxyType(props.getProjectType(), props.getPhysicalLinkTypeEnum()));

        if (props.getPhysicalLinkTypeEnum() == PhysicalLinkTypeEnum.SERIAL_PORT)
        {
            props.setPhysicalLinkExtraCode("@Override\n"
                    + "    public void setDTR(final boolean high) throws UnsupportedOperationException\n" + "    {\n"
                    + "        // ${task}: Method should adjust the data transmit ready pin based on the input. If\n"
                    + "        // operation is supported, remove the throw below. If not supported, keep method as "
                    + "is.\n\n" // NOCHECKSTYLE: repeated string literal, not worried about other copy keeping in sync
                    + "        throw new UnsupportedOperationException(\"Platform does not support setting DTR\");\n"
                    + "    }\n");
        }
        else
        {
            props.setPhysicalLinkExtraCode("");
        }
    }

    /**
     * Generates a project file from the given template.
     * 
     * @param templateFileName
     *            Template file name
     * @param projectProps
     *            Project properties containing variables used for replacement in the templates
     * @param projectDir
     *            Directory to create project file in
     * @throws IOException
     *             if there is a file I/O error
     * @throws TemplateException
     *             if there is an error processing the template
     */
    private void generateFileFromTemplate(final String templateFileName, final ProjectProperties projectProps,
            final File projectDir) throws IOException, TemplateException
    {
        // Determine the output file name
        final int ftlExtLength = ".ftl".length();
        String outFileName = templateFileName.substring(0, templateFileName.length() - ftlExtLength);
        if (outFileName.endsWith(".java"))
        {
            outFileName = convertTemplateNameToJavaName(outFileName, projectProps.getClassName(),
                    projectProps.getProjectType());
        }

        generateFileFromTemplate(templateFileName, projectProps, projectDir, outFileName);
    }

    /**
     * Generates a project file from the given template.
     * 
     * @param templateFileName
     *            Template file name
     * @param projectProps
     *            Project properties containing variables used for replacement in the templates
     * @param projectDir
     *            Directory to create project file in
     * @param outFileName
     *            Filename to use for the created file
     * @throws IOException
     *             if there is a file I/O error
     * @throws TemplateException
     *             if there is an error processing the template
     */
    private void generateFileFromTemplate(final String templateFileName, final ProjectProperties projectProps,
            final File projectDir, final String outFileName) throws IOException, TemplateException
    {
        // Get the template
        final Template template = m_TemplateConfig.getTemplate(templateFileName);

        // Generate the file based on the template and project properties
        final File outFile = m_FileService.getFile(projectDir, outFileName);
        final Writer outWriter = m_FileService.createFileWriter(outFile);
        final Map<String, Object> dataModel = projectProps.getProperties();
        m_OutStream.println("Generating file: " + outFile);
        template.process(dataModel, outWriter);
        outWriter.flush();
        outWriter.close();
    }

    /**
     * Converts a Java template file name to match up with the proper class name.
     * 
     * @param templateFileName
     *            name of the template file
     * @param className
     *            name of the Java class
     * @param projectType
     *            type of project
     * @return file name based on the Java class name
     */
    private String convertTemplateNameToJavaName(final String templateFileName, final String className,
            final ProjectType projectType)
    {
        final String javaFileName;

        if (templateFileName.equals("ExampleAttributes.java"))
        {
            // attribute file is used for all types
            return String.format("%sAttributes.java", className);
        }

        switch (projectType)
        {
            case PROJECT_ASSET:
                javaFileName = templateFileName.replaceAll("ExampleAsset", className);
                break;

            case PROJECT_LINKLAYER:
                javaFileName = templateFileName.replaceAll("ExampleLinkLayer", className);
                break;

            case PROJECT_PHYLINK:
                javaFileName = templateFileName.replaceAll("ExamplePhysicalLink", className);
                break;

            case PROJECT_TRANSPORTLAYER:
                javaFileName = templateFileName.replaceAll("ExampleTransportLayer", className);
                break;

            default:
                throw new IllegalArgumentException("Invalid project type is set for convertTemplateNameToJavaName()");
        }

        return javaFileName;
    }

    /**
     * Generates an xml document for asset plug-in capabilities.
     * 
     * @param projectProps
     *            Project properties containing variables used for creation of the xml file
     * @param projectDir
     *            the directory in which the asset plug-in will reside
     * @param projectType
     *            type of project
     * @throws IOException
     *             if there is an error creating or writing to the xml file
     */
    private void generateCapabilitiesXml(final ProjectProperties projectProps, final File projectDir,
            final ProjectType projectType) throws IOException
    {
        // make the capabilities XML directory
        final File capabilitiesDir = m_FileService.getFile(projectDir, FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME);
        capabilitiesDir.mkdir();

        // construct the file name
        final String fileName = projectProps.getPackageName() + "." + projectProps.getClassName() + ".xml";

        // get the capabilities byte array and write to file
        final byte[] capByte;
        switch (projectType)
        {
            case PROJECT_ASSET:
                capByte = ProjectAssetCapabilities.getCapabilities();
                break;

            case PROJECT_LINKLAYER:
                capByte = ProjectLinkLayerCapabilities.getCapabilities();
                break;

            case PROJECT_PHYLINK:
                capByte = ProjectPhysicalLinkCapabilities.getCapabilities(projectProps.getPhysicalLinkTypeEnum());
                break;

            case PROJECT_TRANSPORTLAYER:
                capByte = ProjectTransportLayerCapabilities.getCapabilities();
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid project type is set to use for 'projectType'.getCapabilities().");
        }

        final File outFile = m_FileService.getFile(capabilitiesDir, fileName);

        try (FileOutputStream fos = m_FileService.createFileOutputStream(outFile))
        {
            fos.write(capByte);
        }
    }

    /**
     * Determine the correct proxy type class for a plug-in based on a project type. If project type is of a physical
     * link type then a physical link type must be passed as well.
     * 
     * @param projType
     *            the type of plug-in project that is to be created
     * @param plType
     *            the physical link type of the project. set only if the project type is a physical link project
     * @return the string value of the proxy class needed for the given
     */
    private static String determineProxyType(final ProjectType projType, final PhysicalLinkTypeEnum plType)
    {
        switch (projType)
        {
            case PROJECT_ASSET:
                return Asset.class.getSimpleName();
            case PROJECT_LINKLAYER:
                return LinkLayer.class.getSimpleName();
            case PROJECT_PHYLINK:
                switch (plType)
                {
                    case SERIAL_PORT:
                        return plType.value();
                    default:
                        return PhysicalLink.class.getSimpleName();
                }

            case PROJECT_TRANSPORTLAYER:
                return TransportLayer.class.getSimpleName();
            default:
                return null;
        }
    }

    /**
     * Determines the attribute type for the plug-in project.
     * 
     * @param projType
     *            the type of plug-in project
     * @param plType
     *            the type of physical link being created.
     * @return the string base type that is required for the given type of physical link
     */
    private static String determineAttributeType(final ProjectType projType, final PhysicalLinkTypeEnum plType)
    {
        switch (projType)
        {
            case PROJECT_ASSET:
                return Asset.class.getSimpleName();
            case PROJECT_LINKLAYER:
                return LinkLayer.class.getSimpleName();
            case PROJECT_PHYLINK:
                switch (plType)
                {
                    case SERIAL_PORT:
                        return plType.value();
                    case GPIO:
                        return Gpio.class.getSimpleName();
                    default:
                        return PhysicalLink.class.getSimpleName();
                }

            case PROJECT_TRANSPORTLAYER:
                return TransportLayer.class.getSimpleName();
            default:
                return null;
        }
    }
}
