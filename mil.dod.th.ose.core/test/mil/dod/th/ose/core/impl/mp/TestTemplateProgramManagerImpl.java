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
package mil.dod.th.ose.core.impl.mp;

import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.utils.FileService;
import mil.dod.th.ose.utils.xml.XmlUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

/**
 * Test the template program manager implementation.
 * @author callen
 *
 */
public class TestTemplateProgramManagerImpl 
{
    private TemplateProgramManagerImpl m_SUT;
    private PersistentDataStore m_PersistentDataStore;
    private Validator m_MissionProgramValidator;
    private FileService m_FileService;
    private XmlUnmarshalService m_UnmarshalService;
    
    @Mock private BundleContext context;
    @Mock private File templateDirectory;
    
    @Before
    public void setUp() throws ValidationFailedException, MalformedURLException
    {
        MockitoAnnotations.initMocks(this);
        
        //mock services
        m_SUT = new TemplateProgramManagerImpl();
        LoggingService logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(logging);
        m_PersistentDataStore = mock(PersistentDataStore.class);
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        m_MissionProgramValidator = mock(Validator.class);
        m_SUT.setMissionProgramValidator(m_MissionProgramValidator);
        m_FileService = mock(FileService.class);
        m_SUT.setFileService(m_FileService);
        m_UnmarshalService = mock(XmlUnmarshalService.class);
        m_SUT.setXMLUnmarshalService(m_UnmarshalService);
        
        //does exist, false so we don't look in the file
        when(templateDirectory.exists()).thenReturn(false);
        when(context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY)).thenReturn("data-dir");
        when(m_FileService.getFile(new File("data-dir"), "templates")).thenReturn(templateDirectory);
    }
    
    /*
     * Mock templates that are in the {@link PersistentDataStore}.
     */
    private void mockTemplateProgramManagerTemplates()
    {
        // variable metadata
        MissionVariableMetaData varMeta =  new MissionVariableMetaData().withName("assetX").withType(
                MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta2 =  new MissionVariableMetaData().withName("assetZ").withType(
                MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta3 =  new MissionVariableMetaData().withName("tl7").withType(
                    MissionVariableTypesEnum.TRANSPORT_LAYER);
        MissionVariableMetaData varMeta5 =  new MissionVariableMetaData().withName("saved-program5").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData varMeta6 =  new MissionVariableMetaData().withName("pl5").withType(
                    MissionVariableTypesEnum.PHYSICAL_LINK);
        MissionVariableMetaData varMeta9 =  new MissionVariableMetaData().withName("asset3").withType(
                MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta10 =  new MissionVariableMetaData().withName("bogus-program").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData varMeta11 =  new MissionVariableMetaData().withName("ll1").
            withType(MissionVariableTypesEnum.LINK_LAYER);
        MissionVariableMetaData varMeta12 =  new MissionVariableMetaData().withName("saved-program3").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        
        //mission templates
        MissionProgramTemplate programData1 = new MissionProgramTemplate().withSource("test").
            withVariableMetaData(varMeta, varMeta2, varMeta3, varMeta5 ).withName("saved-program1");
           
        MissionProgramTemplate programData2 = new MissionProgramTemplate().withSource("blah").
            withName("saved-program2").withVariableMetaData(varMeta6, varMeta11);
        
        MissionProgramTemplate programData3 = new MissionProgramTemplate().withSource("something").
            withName("saved-program3").withVariableMetaData(varMeta9);
        
        MissionProgramTemplate programData4 = new MissionProgramTemplate().withSource("blahblah").
            withName("saved-program4").withVariableMetaData(varMeta10);
        
        MissionProgramTemplate programData5 = new MissionProgramTemplate().withSource("huh").
            withName("saved-program5").withVariableMetaData(varMeta12);
        
        //mock the template data
        List<PersistentData> perData = new ArrayList<PersistentData>(); 
        PersistentData dataTemplate1 = mock(PersistentData.class);
        PersistentData dataTemplate2 = mock(PersistentData.class);
        PersistentData dataTemplate3 = mock(PersistentData.class);
        PersistentData dataTemplate4 = mock(PersistentData.class);
        PersistentData dataTemplate5 = mock(PersistentData.class);
                
        when(dataTemplate1.getDescription()).thenReturn("saved-program1");
        when(dataTemplate2.getDescription()).thenReturn("saved-program2");
        when(dataTemplate3.getDescription()).thenReturn("saved-program3");
        when(dataTemplate4.getDescription()).thenReturn("saved-program4");
        when(dataTemplate5.getDescription()).thenReturn("saved-program5");
        
        when((byte[])dataTemplate1.getEntity()).thenReturn(XmlUtils.toXML(programData1, true));
        when((byte[])dataTemplate2.getEntity()).thenReturn(XmlUtils.toXML(programData2, true));
        when((byte[])dataTemplate3.getEntity()).thenReturn(XmlUtils.toXML(programData3, true));
        when((byte[])dataTemplate4.getEntity()).thenReturn(XmlUtils.toXML(programData4, true));
        when((byte[])dataTemplate5.getEntity()).thenReturn(XmlUtils.toXML(programData5, true));
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        UUID uuid5 = UUID.randomUUID();
        
        when(dataTemplate1.getUUID()).thenReturn(uuid1);
        when(dataTemplate2.getUUID()).thenReturn(uuid2);
        when(dataTemplate3.getUUID()).thenReturn(uuid3);
        when(dataTemplate4.getUUID()).thenReturn(uuid4);
        when(dataTemplate5.getUUID()).thenReturn(uuid5);
        
        perData.add(dataTemplate1);
        perData.add(dataTemplate2);
        perData.add(dataTemplate3);
        perData.add(dataTemplate4);
        perData.add(dataTemplate5);
        
        //mocked response when data store is queried 
        doReturn(perData).when(m_PersistentDataStore).query(TemplateProgramManagerImpl.class);
        when(m_PersistentDataStore.find(uuid1)).thenReturn(dataTemplate1);
        when(m_PersistentDataStore.find(uuid2)).thenReturn(dataTemplate2);
        when(m_PersistentDataStore.find(uuid3)).thenReturn(dataTemplate3);
        when(m_PersistentDataStore.find(uuid4)).thenReturn(dataTemplate4);
        when(m_PersistentDataStore.find(uuid5)).thenReturn(dataTemplate5);
    }
    
    /**
     * Mock templates that would be loaded from a directory.
     */
    private void mockTemplatesFromDir() throws IOException, UnmarshalException, URISyntaxException
    {           
        // variable metadata
        MissionVariableMetaData varMeta =  new MissionVariableMetaData().withName("asset").withType(
                MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta1 =  new MissionVariableMetaData().withName("progs").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData varMeta2 =  new MissionVariableMetaData().withName("phys").withType(
            MissionVariableTypesEnum.PHYSICAL_LINK);
        MissionVariableMetaData varMeta4 =  new MissionVariableMetaData().withName("link").
            withType(MissionVariableTypesEnum.LINK_LAYER);
        MissionVariableMetaData varMeta5 =  new MissionVariableMetaData().withName("pros2").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        
        //mission templates
        MissionProgramTemplate template1 = new MissionProgramTemplate().withSource("SourceMcSporty").
            withVariableMetaData(varMeta, varMeta1, varMeta5).withName("template-from-dir1");
        
        MissionProgramTemplate template2 = new MissionProgramTemplate().withSource("LinkingLog").
            withName("template-from-dir2").withVariableMetaData(varMeta2, varMeta4);
        
        // return true so directory is scanned
        when(templateDirectory.exists()).thenReturn(true);
        
        //child files
        File childFile1 = mock(File.class);
        URL childUrl1 = new URL("file:asdfs1");
        when(childFile1.toURI()).thenReturn(childUrl1.toURI());
        
        File childFile2 = mock(File.class);
        URL childUrl2 = new URL("file:asdfs2");
        when(childFile2.toURI()).thenReturn(childUrl2.toURI());
        
        //directory behavior
        File[] children = new File[]{childFile1, childFile2};
        
        when(templateDirectory.listFiles()).thenReturn(children);
        when(m_UnmarshalService.getXmlObject(MissionProgramTemplate.class, childUrl1)).thenReturn(template1);
        when(m_UnmarshalService.getXmlObject(MissionProgramTemplate.class, childUrl2)).thenReturn(template2);
    }
    
    /**
     * Test activation of the TemplateProgramManager.
     */
    @Test
    public void testActivate()
    {
        mockTemplateProgramManagerTemplates();
        m_SUT.activate(context);
        
        assertThat(m_SUT.getMissionTemplateNames(), hasItem("saved-program5"));
                
        //verify the template contents
        MissionProgramTemplate program1 = m_SUT.getTemplate("saved-program1");
        assertThat(program1.getSource(), is("test"));
        List<MissionVariableMetaData> progData = program1.getVariableMetaData();
        assertThat(progData.isEmpty(), is(false));
        //the template had four var metadata entries
        assertThat(progData.size(), is(4));
        assertThat(progData.get(0).getType(), is(MissionVariableTypesEnum.ASSET));
        assertThat(progData.get(2).getType(), is(MissionVariableTypesEnum.TRANSPORT_LAYER));
    }
    
    /**
     * Test the return of all template names.
     */
    @Test
    public void testGetTemplateNames() throws IllegalArgumentException, PersistenceFailedException
    {
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        m_SUT.activate(context);
    
        Set<String> names = m_SUT.getMissionTemplateNames();
        //check that all templates are here
        assertThat(names, hasItem("saved-program1"));
        assertThat(names, hasItem("saved-program2"));
        assertThat(names, hasItem("saved-program3"));
        assertThat(names, hasItem("saved-program4"));
        assertThat(names, hasItem("saved-program5"));
        
        MissionProgramTemplate template = new MissionProgramTemplate().withDescription("I'm a newb").
            withName("RockStar").withSource("wellOfCourse");
        m_SUT.loadMissionTemplate(template);
        verify(m_PersistentDataStore).persist(eq(TemplateProgramManagerImpl.class), Mockito.any(UUID.class), 
            eq("RockStar"), (Serializable)Mockito.any());
        
        //verify new template added and that its name is in the list
        names = m_SUT.getMissionTemplateNames();
        assertThat(names, hasItem("RockStar"));
    }
    
    /**
     * Test getting a template from the manager.
     * 
     */
    @Test
    public void testGetTemplate()
    {
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        m_SUT.activate(context);
        
        // mock data store
        MissionProgramTemplate template = m_SUT.getTemplate("saved-program2");
        assertThat(template.getName(), is("saved-program2"));
        //make sure that this is the correct template and that it is complete
        assertThat(template.getVariableMetaData().size(), is(2));
        
        //test error if template does not exist
        try
        {
            m_SUT.getTemplate("BubonicPlague");
            fail("Expected Exception");
        }
        catch(IllegalArgumentException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Testing the removal of a mission template.
     */
    @Test
    public void testRemoveTemplate() throws IllegalArgumentException, PersistenceFailedException
    {
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        m_SUT.activate(context);
        
        //test a template that does not exist
        try
        {
            m_SUT.removeTemplate("Ninja");
            fail("expecting an exception");
        }
        catch(IllegalArgumentException exception)
        {
             //expected exception because this template does not exist
        }
        
        MissionProgramTemplate template = new MissionProgramTemplate().withName("George").withSource("dragon");
        m_SUT.loadMissionTemplate(template);
        //need to capture the UUID so it is known for removal
        ArgumentCaptor<UUID> uuid = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<byte[]> templateCap = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentDataStore).persist(eq(TemplateProgramManagerImpl.class), uuid.capture(), 
            eq("George"), templateCap.capture());
        
        // mock data store
        PersistentData foundData = mock(PersistentData.class);
        when(m_PersistentDataStore.find((UUID)Mockito.any())).thenReturn(foundData);
        when(foundData.getEntity()).thenReturn(templateCap.getValue());
        
        template = m_SUT.getTemplate("George");
        assertThat(template, is(notNullValue()));
        m_SUT.removeTemplate("George");
        
        MissionProgramTemplate templateToRemove = (MissionProgramTemplate)XmlUtils.fromXML(templateCap.getValue(), 
                MissionProgramTemplate.class);
        assertThat(templateToRemove.getName(), is(template.getName()));
        
        //verify removed from data store
        verify(m_PersistentDataStore).remove(uuid.getValue());
        
        assertThat(m_SUT.getMissionTemplateNames(), not(hasItem("George")));
    }
    
    /**
     * Test that templates are persisted.
     */
    @Test
    public void testPersistTemplate() throws IllegalArgumentException, PersistenceFailedException
    {
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        m_SUT.activate(context);
        
        MissionProgramTemplate template = new MissionProgramTemplate().withName("MyTemplate").withSource("Source");
        m_SUT.loadMissionTemplate(template);
        //need to capture the UUID so it is known for removal
        ArgumentCaptor<UUID> uuid = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<byte[]> templateCap = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentDataStore).persist(eq(TemplateProgramManagerImpl.class), uuid.capture(), 
            eq("MyTemplate"), templateCap.capture());
        
        MissionProgramTemplate template2 = (MissionProgramTemplate)XmlUtils.fromXML(templateCap.getValue(), 
                MissionProgramTemplate.class);
        //check that the persisted template is the template expected
        assertThat(template2, is(template));
        //check that the new template is managed
        assertThat(m_SUT.getMissionTemplateNames(), hasItem("MyTemplate"));
        PersistentData foundData = mock(PersistentData.class);
        when(m_PersistentDataStore.find((UUID)Mockito.any())).thenReturn(foundData);
        when(foundData.getEntity()).thenReturn(templateCap.getValue());
        template = m_SUT.getTemplate("MyTemplate");
                
        m_SUT.removeTemplate("MyTemplate");
        //verify the data store removes the template
        verify(m_PersistentDataStore).remove(uuid.getValue());
        assertThat(m_SUT.getMissionTemplateNames(), not(hasItem("MyTemplate")));
    }
    
    /**
     * Test the loading of templates. Also testing that the mission program manager can activate without
     * templates to restore.
     */
    @Test
    public void testLoadMissionTemplate() throws IllegalArgumentException, PersistenceFailedException
    {
        m_SUT.activate(context);
        
        //template to add
        MissionProgramTemplate template = new MissionProgramTemplate().withName("MyTemplate").withSource("Source");
        m_SUT.loadMissionTemplate(template);
        ArgumentCaptor<byte[]> templateCap = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentDataStore).persist(eq(TemplateProgramManagerImpl.class), Mockito.any(UUID.class), 
            eq("MyTemplate"), templateCap.capture());
         
        //verify
        MissionProgramTemplate templateAgain = (MissionProgramTemplate)XmlUtils.fromXML(templateCap.getValue(), 
            MissionProgramTemplate.class);
        assertThat(templateAgain.getSource(), is("Source"));
        
        //change source, but not name
        templateAgain.setSource("DifferentSource");
        //try adding the template again
        m_SUT.loadMissionTemplate(templateAgain);
        
        //verify
        verify(m_PersistentDataStore, times(2)).persist(eq(TemplateProgramManagerImpl.class), Mockito.any(UUID.class), 
            eq("MyTemplate"), templateCap.capture());
        
        //verify that template was updated
        template = (MissionProgramTemplate)XmlUtils.fromXML(templateCap.getValue(), 
            MissionProgramTemplate.class);
        assertThat(template.getSource(), is("DifferentSource"));
    }
    
    /**
     * Verify loading from file and overwriting behavior.
     * TODO: TH-805, support syncing of templates, need to be able to preserve unique names,
     * but also allow for templates to be update if wanted.
     */
    @Test
    public void testLoadFromFile() throws IOException, IllegalArgumentException, PersistenceFailedException, 
        UnmarshalException, URISyntaxException
    {
        //activate the manager
        m_SUT.activate(context);
        
        //create a template
        MissionProgramTemplate localTemplate = new MissionProgramTemplate().withName("SpiderOne").
            withSource("ASpiderBite");
        URL url1 = templateUrl(localTemplate);
        
        //load the template
        m_SUT.loadFromFile(url1, true);

        //verify
        ArgumentCaptor<byte[]> dataByte = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore, times(1)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
            anyString(), Mockito.any(Serializable.class));
        PersistentData data1 = mock(PersistentData.class);
        byte[] tmpBytes = XmlUtils.toXML(localTemplate, false);
        when((byte[])data1.getEntity()).thenReturn(tmpBytes);
        when(m_PersistentDataStore.find(uuidCaptor.getValue())).thenReturn(data1);
        
        // verify the template was loaded from the file, and overwrote the previously loaded one
        assertThat(m_SUT.getTemplates().size(), is(1));
        
        //change the template
        MissionProgramTemplate localTemplate2 = new MissionProgramTemplate().withName("SpiderTwo").
            withSource("ILikeSpecialChars && @ <return>");
        URL url2 = templateUrl(localTemplate2);
        
        //load the template
        m_SUT.loadFromFile(url2, true);
        
        //mocking and verifying
        verify(m_PersistentDataStore, times(2)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
                anyString(), Mockito.any(Serializable.class));
        PersistentData data2 = mock(PersistentData.class);
        final byte[] tmpBytes2 = XmlUtils.toXML(localTemplate2, false);
        when((byte[])data2.getEntity()).thenReturn(tmpBytes2);
        when(m_PersistentDataStore.find(uuidCaptor.getValue())).thenReturn(data2);
        
        // verify the template was loaded from the file and that the template was overwritten
        Set<MissionProgramTemplate> templates = m_SUT.getTemplates();
        assertThat(templates.size(), is(2));
        
        //Lists are not place specific, check that the template added is within the templates in the list
        boolean foundFlag = false;
        for (MissionProgramTemplate template : templates)
        {
            if (template.getName().equals(localTemplate.getName()))
            {
                foundFlag = true;
                assertThat(template.getSource(), is(localTemplate.getSource()));             
            }
        }
        
        //check that found flag is true
        assertThat(foundFlag, is(true));
        
        //try to add a file that is not meant to overwrite the template with the same name
        MissionProgramTemplate localTemplate3 = new MissionProgramTemplate().withName("SpiderOne").
                withSource("I will get in");
        URL url3 = templateUrl(localTemplate3);
        
        //load the template, with overwrite as false
        m_SUT.loadFromFile(url3, false);

        //mocking and verifying
        verify(m_PersistentDataStore, times(3)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
                anyString(), dataByte.capture());
        PersistentData data3 = mock(PersistentData.class);
        final byte[] tmpBytes3 = XmlUtils.toXML(localTemplate3, false);
        when((byte[])data3.getEntity()).thenReturn(tmpBytes3);
        when(m_PersistentDataStore.find(uuidCaptor.getValue())).thenReturn(data3);

        //recap
        templates = m_SUT.getTemplates();
        assertThat(templates.size(), is(2));
        //verify
        for (MissionProgramTemplate template : templates)
        {
            if (template.getName().contains("SpiderOne"))
            {
                assertThat(template.getSource(), is("I will get in"));
            }
        }
    }

    /**
     * Test loading from file an XML object that is NOT well-formed.
     */
    @Test
    public void testBadLoadFromFile() throws IOException, IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException, UnmarshalException
    {
        //activate the manager
        m_SUT.activate(context);
        
        //mock URL
        URL url = new URL("file:asdf");
        
        //mock behavior
        when(m_UnmarshalService.getXmlObject(MissionProgramTemplate.class, url)).thenThrow(
                new UnmarshalException("Bad template!!")); 
        
        try
        {
            m_SUT.loadFromFile(url, false);
            fail("Expected Exception");
        }
        catch (UnmarshalException e)
        {
            
        }
    }
    
    /**
     * Verify that if a template fails validation that the template is not loaded.
     */
    @Test
    public void testFailedValidationLoadFromFile() throws ValidationFailedException, MalformedURLException, 
        UnmarshalException, PersistenceFailedException, URISyntaxException
    {
        //activate the manager
        m_SUT.activate(context);

        //create a template
        MissionProgramTemplate localTemplate = new MissionProgramTemplate().withName("Spider").
            withSource("ASpiderBite");
        URL url = templateUrl(localTemplate);

        //mock behavior of validator
        doThrow(new ValidationFailedException()).when(m_MissionProgramValidator)
           .validate(Mockito.any(MissionProgramTemplate.class));
        
        try
        {
            m_SUT.loadFromFile(url, true);
            fail("Expected exception, as the model should fail validation!");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that if a template fails marshalling that the template is not loaded.
     */
    @Test
    public void testIllegalStateLoadFromFile() throws ValidationFailedException, MalformedURLException, 
        UnmarshalException, PersistenceFailedException, URISyntaxException
    {
        //activate the manager
        m_SUT.activate(context);

        //create a template
        MissionProgramTemplate localTemplate = new MissionProgramTemplate().withName("Spider").
            withSource("ASpiderBite");
        URL url = templateUrl(localTemplate);

        //mock behavior of validator
        doThrow(new IllegalStateException()).when(m_MissionProgramValidator)
           .validate(Mockito.any(MissionProgramTemplate.class));
        
        try
        {
            m_SUT.loadFromFile(url, true);
            fail("Expected exception, as the model should fail validation!");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Test that a list of all templates is returned.
     */
    @Test
    public void testGetMissionTemplates() throws IllegalArgumentException, PersistenceFailedException, IOException, 
        UnmarshalException, URISyntaxException
    {    
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        mockTemplatesFromDir();
        m_SUT.activate(context);
        
        //make a list of uuids, and byte data assigned to each template from the template directory
        ArgumentCaptor<byte[]> dataByte = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore, times(2)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
                anyString(), dataByte.capture());
        
        //mock behavior of look-up
        PersistentData data = mock(PersistentData.class);
        PersistentData data1 = mock(PersistentData.class);
        when((byte[])data.getEntity()).thenReturn(dataByte.getAllValues().get(0));
        when(m_PersistentDataStore.find(uuidCaptor.getAllValues().get(0))).thenReturn(data);
        when((byte[])data1.getEntity()).thenReturn(dataByte.getAllValues().get(1));
        when(m_PersistentDataStore.find(uuidCaptor.getAllValues().get(1))).thenReturn(data1);
        
        //get all templates known, should be 7, as five templates are mocked, 2 are from the template directory
        assertThat(m_SUT.getTemplates().size(), is(7));
    }
    
    /**
     * Test the LoadFromStream method.
     * Verify that the template is loaded appropriately. 
     */
    @Test
    public void testLoadFromStream() throws IOException, UnmarshalException, IllegalArgumentException, 
        PersistenceFailedException, URISyntaxException
    {
        //mock the stored templates
        mockTemplateProgramManagerTemplates();
        mockTemplatesFromDir();
        //activate the manager
        m_SUT.activate(context);
        
        //make a list of uuids, and byte data assigned to each template from the template directory
        ArgumentCaptor<byte[]> dataByte = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore, times(2)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
                anyString(), dataByte.capture());
        assertThat(dataByte.getAllValues().size(), is(2));
            
        
        //mock behavior of look-up, the order and data for each isn't particularly important at this point
        PersistentData data = mock(PersistentData.class);
        PersistentData data1 = mock(PersistentData.class);
        when((byte[])data.getEntity()).thenReturn(dataByte.getAllValues().get(0));
        when(m_PersistentDataStore.find(uuidCaptor.getAllValues().get(0))).thenReturn(data);
        when((byte[])data1.getEntity()).thenReturn(dataByte.getAllValues().get(1));
        when(m_PersistentDataStore.find(uuidCaptor.getAllValues().get(1))).thenReturn(data1);
        
        //get all templates known, should be 7, as five templates are mocked, 2 are from the template directory
        assertThat(m_SUT.getTemplates().size(), is(7));
        
        //create a template and save it to the file
        MissionProgramTemplate localTemplate = new MissionProgramTemplate().withName("TestStreamMission").
            withSource("ScriptSourceCode");
        final byte[] tmpBytes = XmlUtils.toXML(localTemplate, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(tmpBytes);
        
        //mock behavior
        when(m_UnmarshalService.getXmlObject(MissionProgramTemplate.class, bais)).thenReturn(localTemplate);
        
        //load the template
        m_SUT.loadFromStream(bais, true);

        //mock behavior
        verify(m_PersistentDataStore, times(3)).persist(eq(TemplateProgramManagerImpl.class), uuidCaptor.capture(), 
            anyString(), Mockito.any(Serializable.class));
        PersistentData data3 = mock(PersistentData.class);
        when((byte[])data3.getEntity()).thenReturn(tmpBytes);
        when(m_PersistentDataStore.find(uuidCaptor.getValue())).thenReturn(data3);
        
        // verify the template was loaded from the file
        assertThat(m_SUT.getTemplates().size(), is(8));
    }
    
    /**
     * Mock out a file to template interaction.
     */
    private URL templateUrl(final MissionProgramTemplate template) throws MalformedURLException, UnmarshalException, 
        URISyntaxException
    {
        File localFile = mock(File.class);
        URL url = new URL("file:asdfs3");
        
        //mocking behavior
        when(m_FileService.getFile("file:hidy")).thenReturn(localFile);
        when(localFile.toURI()).thenReturn(url.toURI());
        when(m_UnmarshalService.getXmlObject(MissionProgramTemplate.class, url)).thenReturn(template);
        return url;
    }
}
