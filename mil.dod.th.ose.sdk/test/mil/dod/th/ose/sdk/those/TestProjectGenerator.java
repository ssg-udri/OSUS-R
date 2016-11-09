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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.test.FileMocker;
import mil.dod.th.ose.test.FileServiceMocker;
import mil.dod.th.ose.test.MockFileMatcher;
import mil.dod.th.ose.utils.FileService;

/**
 * @author Admin
 * 
 */
public class TestProjectGenerator
{
    private ProjectGenerator m_SUT;
    private File m_ProjectDir;
    private Configuration m_Config;
    private Template m_BuildTemplate;
    private Template m_XmlTemplate;
    private Template m_LocalTemplate;
    private Template m_BundleTemplate;
    private Template m_ProjectTemplate;
    private Template m_ClassTemplate;
    private Template m_FactoryObjectJavaTemplate;
    private Template m_AttributesJavaTemplate;
    private Template m_ScannerJavaTemplate;
    private Template m_EclipseJdtCorePrefsTemplate;

    private final String bndTemplateName = "bnd.bnd.ftl";
    private FileService m_FileService;
    private FileOutputStream m_FileOutputStream;

    @Before
    public void setUp() throws IOException
    {
        m_ProjectDir = FileMocker.mockIt("base-path");

        m_Config = mock(Configuration.class);
        m_BuildTemplate = mock(Template.class);
        m_XmlTemplate = mock(Template.class);
        m_LocalTemplate = mock(Template.class);
        m_BundleTemplate = mock(Template.class);
        m_ProjectTemplate = mock(Template.class);
        m_ClassTemplate = mock(Template.class);
        m_FactoryObjectJavaTemplate = mock(Template.class);
        m_AttributesJavaTemplate = mock(Template.class);
        m_ScannerJavaTemplate = mock(Template.class);
        m_EclipseJdtCorePrefsTemplate = mock(Template.class);

        // common stubbing
        when(m_Config.getTemplate("build.properties.ftl")).thenReturn(m_BuildTemplate);
        when(m_Config.getTemplate("build.xml.ftl")).thenReturn(m_XmlTemplate);
        when(m_Config.getTemplate("local.properties.ftl")).thenReturn(m_LocalTemplate);
        when(m_Config.getTemplate(".project.ftl")).thenReturn(m_ProjectTemplate);
        when(m_Config.getTemplate(".classpath.ftl")).thenReturn(m_ClassTemplate);
        when(m_Config.getTemplate("ExampleAttributes.java.ftl")).thenReturn(m_AttributesJavaTemplate);
        when(m_Config.getTemplate("org.eclipse.jdt.core.prefs.ftl")).thenReturn(m_EclipseJdtCorePrefsTemplate);

        m_FileService = FileServiceMocker.mockIt();
        FileWriter fileWriter = mock(FileWriter.class);
        when(m_FileService.createFileWriter(Mockito.any(File.class))).thenReturn(fileWriter);
        m_FileOutputStream = mock(FileOutputStream.class);
        when(m_FileService.createFileOutputStream(Mockito.any(File.class))).thenReturn(m_FileOutputStream);
        m_SUT = new ProjectGenerator(m_FileService);
    }

    /**
     * Test that creating an asset plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateAsset() throws IOException, TemplateException
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExampleAsset.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);
        when(m_Config.getTemplate("ExampleAssetScanner.java.ftl")).thenReturn(m_ScannerJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_ASSET);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectAssetCapabilities.getCapabilities());

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"Asset"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"Asset"));
    }

    /**
     * Test that creating a physical link plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateSerialPort() throws IOException, TemplateException
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExamplePhysicalLink.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);
        when(m_Config.getTemplate("ExampleAssetScanner.java.ftl")).thenReturn(m_ScannerJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_PHYLINK);
        projectProps.setPhysicalLinkTypeEnum(PhysicalLinkTypeEnum.SERIAL_PORT);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(
                ProjectPhysicalLinkCapabilities.getCapabilities(PhysicalLinkTypeEnum.SERIAL_PORT));

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"SerialPort"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"SerialPort"));
        String serialPortCode = "@Override\n"
                + "    public void setDTR(final boolean high) throws UnsupportedOperationException\n" + "    {\n"
                + "        // ${task}: Method should adjust the data transmit ready pin based on the input. If\n"
                + "        // operation is supported, remove the throw below. If not supported, keep method as is.\n"
                + "\n" // NOCHECKSTYLE: repeated string literal, not worried about other copy keeping in sync
                + "        throw new UnsupportedOperationException(\"Platform does not support setting DTR\");\n"
                + "    }\n";
        assertThat(projectProps.getProperties(),
                hasEntry(ProjectProperties.PROP_PL_EXTRA_CODE, (Object)serialPortCode));
    }

    /**
     * Test that creating a physical link plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateGpio() throws Exception
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExamplePhysicalLink.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_PHYLINK);
        projectProps.setPhysicalLinkTypeEnum(PhysicalLinkTypeEnum.GPIO);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectPhysicalLinkCapabilities.getCapabilities(PhysicalLinkTypeEnum.GPIO));

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"Gpio"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"PhysicalLink"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PL_EXTRA_CODE, (Object)""));
    }

    /**
     * Test that creating a physical link plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateSpi() throws Exception
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExamplePhysicalLink.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_PHYLINK);
        projectProps.setPhysicalLinkTypeEnum(PhysicalLinkTypeEnum.SPI);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectPhysicalLinkCapabilities.getCapabilities(PhysicalLinkTypeEnum.SPI));

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"PhysicalLink"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"PhysicalLink"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PL_EXTRA_CODE, (Object)""));
    }

    /**
     * Test that creating a physical link plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateI2C() throws Exception
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExamplePhysicalLink.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_PHYLINK);
        projectProps.setPhysicalLinkTypeEnum(PhysicalLinkTypeEnum.I_2_C);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectPhysicalLinkCapabilities.getCapabilities(PhysicalLinkTypeEnum.I_2_C));

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"PhysicalLink"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"PhysicalLink"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PL_EXTRA_CODE, (Object)""));
    }

    /**
     * Test that creating a link layer plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateLinkLayer() throws IOException, TemplateException
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExampleLinkLayer.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_LINKLAYER);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectLinkLayerCapabilities.getCapabilities());

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"LinkLayer"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"LinkLayer"));
    }

    /**
     * Test that creating a transport layer plug-in project will process the correct files.
     * 
     * Verify the data model is correctly passed in to the process call.
     * 
     * Verify the correct BND template file and Java templates are used.
     */
    @Test
    public void testCreateTransportLayer() throws IOException, TemplateException
    {
        // Stubbing
        when(m_Config.getTemplate(bndTemplateName)).thenReturn(m_BundleTemplate);
        when(m_Config.getTemplate("ExampleTransportLayer.java.ftl")).thenReturn(m_FactoryObjectJavaTemplate);

        // Instantiated objects
        ProjectProperties projectProps = new ProjectProperties(null);
        projectProps.setProjectType(ProjectType.PROJECT_TRANSPORTLAYER);
        final Map<String, Object> dataModel = projectProps.getProperties();
        PrintStream out = new PrintStream(System.out);
        ArgumentCaptor<Writer> outWriter = ArgumentCaptor.forClass(Writer.class);
        m_SUT.initialize(m_Config, out);

        // Method Call
        m_SUT.create(projectProps, m_ProjectDir);

        verifyCreation(dataModel, outWriter);
        verify(m_FileOutputStream).write(ProjectTransportLayerCapabilities.getCapabilities());

        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)"TransportLayer"));
        assertThat(projectProps.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)"TransportLayer"));
    }

    /**
     * Common verification methods.
     */
    private void verifyCreation(final Map<String, Object> dataModel, ArgumentCaptor<Writer> outWriter)
            throws TemplateException, IOException, FileNotFoundException
    {
        // Verification that the template.process method was called for .classpath template
        verify(m_ClassTemplate).process(eq(dataModel), outWriter.capture());
        verify(m_BuildTemplate).process(eq(dataModel), outWriter.capture());
        verify(m_BundleTemplate).process(eq(dataModel), outWriter.capture());
        verify(m_AttributesJavaTemplate).process(eq(dataModel), outWriter.capture());

        // verify capabilities XML file creation (file name and content)
        ArgumentCaptor<File> file = ArgumentCaptor.forClass(File.class);
        verify(m_FileService).createFileOutputStream(file.capture());

        assertThat(file.getValue(),
                MockFileMatcher.matches("base-path/capabilities-xml/example.project.ExampleClass.xml"));

        // verify creation of attributes file
        final int EXAMPLE_CLASS_ATTRIBUTE = 8;
        verify(m_FileService, atLeast(9)).createFileWriter(file.capture());
        assertThat(file.getAllValues().get(EXAMPLE_CLASS_ATTRIBUTE),
                MockFileMatcher.matches("base-path/src/example/project/ExampleClassAttributes.java"));

        // verify creation of org.eclipse.jdt.core.prefs file
        final int ECLIPSE_JDT_CORE_PREFS = 7;
        assertThat(file.getAllValues().get(ECLIPSE_JDT_CORE_PREFS),
                MockFileMatcher.matches("base-path/.settings/org.eclipse.jdt.core.prefs"));
    }
}
