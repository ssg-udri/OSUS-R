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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerImage;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.mp.MissionModel.MissionTemplateLocation;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.context.RequestContext;
import org.primefaces.model.StreamedContent;

/**
 * Test class for {@link MissionRequestImpl}.
 * 
 * @author cweisenborn
 */
public class TestMissionRequestImpl
{
    private MissionRequestImpl m_SUT;
    private TemplateProgramManager m_Tpm;
    private FacesContextUtil m_FacesContextUtil;
    private FacesContext m_FacesContext;
    private RequestContext m_RequestContext;
    private MissionProgramMgr m_MissionProgramMgr;
    private ActiveController m_ActiveController;
    private ControllerImage m_ControllerImageInterface;
    
    @Before
    public void setup() throws Exception
    {
        m_FacesContext = mock(FacesContext.class);
        m_RequestContext = mock(RequestContext.class);
        m_FacesContextUtil = mock(FacesContextUtil.class);
        m_Tpm = mock(TemplateProgramManager.class);
        m_MissionProgramMgr = mock(MissionProgramMgr.class);
        m_ActiveController = mock(ActiveController.class);
        m_ControllerImageInterface = mock(ControllerImage.class);
        
        when(m_FacesContextUtil.getFacesContext()).thenReturn(m_FacesContext);
        when(m_FacesContextUtil.getRequestContext()).thenReturn(m_RequestContext);
        
        m_SUT = new MissionRequestImpl();
        m_SUT.setTemplateProgramManager(m_Tpm);
        m_SUT.setFacesContextUtil(m_FacesContextUtil);
        m_SUT.setMissionProgMgr(m_MissionProgramMgr);
        m_SUT.setActiveController(m_ActiveController);
    }
    
    @Test
    public void testGetMissions()
    {
        //Mock class and create real data needed for the primary and secondary images.
        MissionProgramTemplate template = mock(MissionProgramTemplate.class);
        Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        templates.add(template);
        DigitalMedia image = mock(DigitalMedia.class);
        MissionVariableMetaData variableData = mock(MissionVariableMetaData.class);
        Set<String> testNames = new HashSet<String>();
        testNames.add("Name 1");
        List<DigitalMedia> imageList = new ArrayList<DigitalMedia>();
        imageList.add(image);
        List<MissionVariableMetaData> variableList = new ArrayList<MissionVariableMetaData>();
        variableList.add(variableData);
        byte[] someByteArr = {10, 12, 15, 25, 35};
    
        //Mocked methods.
        when(m_Tpm.getTemplates()).thenReturn(templates);
        when(template.getName()).thenReturn("Name 1");
        when(template.getDescription()).thenReturn("Some description about the mission.");
        when(template.getPrimaryImage()).thenReturn(image);
        when(template.getSecondaryImages()).thenReturn(imageList);
        when(template.getVariableMetaData()).thenReturn(variableList);
        when(image.getEncoding()).thenReturn("image/some encoding");
        when(image.getValue()).thenReturn(someByteArr);
        when(variableData.getName()).thenReturn("Awesome Variable!");
        when(variableData.getDefaultValue()).thenReturn("Default");
        when(variableData.getType()).thenReturn(MissionVariableTypesEnum.STRING);
        
        List<String> opValues = new ArrayList<String>();
        opValues.add("one");
        opValues.add("two");
        when(variableData.getOptionValues()).thenReturn(opValues);
        
        //active controller behavior
        ControllerModel contModel = new ControllerModel(911, m_ControllerImageInterface);
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(contModel);
        
        //mission program mgr behavior
        when(m_MissionProgramMgr.getRemoteTemplateNames(911)).thenReturn(new ArrayList<String>());
        
        //Verify that the mission is retrieved and all data is correct.
        List<MissionModel> models = m_SUT.getMissions();
        assertThat(models.get(0).getName(), equalTo("Name 1"));
        assertThat(models.get(0).getDescription(), equalTo("Some description about the mission."));
        assertThat(models.get(0).getArguments().get(0).getName(), equalTo("Awesome Variable!"));
        assertThat((String)models.get(0).getArguments().get(0).getCurrentValue(), equalTo("Default"));
        assertThat(models.get(0).getLocation(), is(MissionTemplateLocation.LOCAL));
    }
    
    @Test
    public void testGetStreamPrimaryImage() throws Exception
    {
        //Real data to be checked for in verification.
        String encoding = "image/something";
        byte[] byteArr = {25, 15, 10, 35, 45};
        
        //Mock the image and mission template to be used.
        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put("primeImageMissionName", "TestMissionName");
        ExternalContext testExtContext = mock(ExternalContext.class);
        MissionProgramTemplate template = mock(MissionProgramTemplate.class);
        DigitalMedia image = mock(DigitalMedia.class);
        
        //Mocked methods.
        when(m_FacesContext.getExternalContext()).thenReturn(testExtContext);
        when(testExtContext.getRequestParameterMap()).thenReturn(paramsMap);
        when(m_Tpm.getTemplate("TestMissionName")).thenReturn(template);
        when(template.getPrimaryImage()).thenReturn(image);
        when(image.getEncoding()).thenReturn(encoding);
        when(image.getValue()).thenReturn(byteArr);
        
        //Call to method being tested. Should return a StreamedContent object.
        StreamedContent content =  m_SUT.getStreamPrimaryImage();
        
        //Verify the StreamedContent has the appropriate type set.
        assertThat(content.getContentType(), is("image/something"));
        
        int streamByte;
        int index = 0;
        //Verify that all the bytes within the StreamedContent are appropriate and in the correct order.
        while ((streamByte = content.getStream().read()) != -1)
        {
            assertThat((byte)streamByte, is(byteArr[index]));
            index++;
        }
    }
    
    /**
     * Verify that if a mission name cannot be found an empty default streamed content object 
     * is returned.
     */
    @Test
    public void testGetStreamPrimaryImageWithNullMissionName()
    {
        //Mock the image and mission template to be used.
        Map<String, String> paramsMap = new HashMap<String, String>();
        ExternalContext testExtContext = mock(ExternalContext.class);
        
        //Mocked methods.
        when(m_FacesContext.getExternalContext()).thenReturn(testExtContext);
        when(testExtContext.getRequestParameterMap()).thenReturn(paramsMap);
        
        //Call to method being tested. Should return an empty StreamedContent object.
        StreamedContent content =  m_SUT.getStreamPrimaryImage();
        
        assertThat(content.getContentType(), nullValue());
        assertThat(content.getStream(), nullValue());
    }
    
    @Test
    public void testGetStreamSecondaryImage() throws IOException
    {
        //Real data to be checked for in verification
        String encoding = "image/something";
        byte[] byteArr = {25, 15, 10, 35, 45};
        
        //Mock the image and external context.
        ExternalContext testExtContext = mock(ExternalContext.class);
        DigitalMedia image = mock(DigitalMedia.class);
        List<DigitalMedia> secondaryImages = new ArrayList<DigitalMedia>();
        secondaryImages.add(image);
        //MissionModel testMission = new MissionModel();
        MissionProgramTemplate testMission = mock(MissionProgramTemplate.class);
        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put("secondImageMissionName", "testName");
        paramsMap.put("imageId", "0");
    
        //Mocked methods.
        when(m_Tpm.getTemplate("testName")).thenReturn(testMission);
        when(testMission.getSecondaryImages()).thenReturn(secondaryImages);
        when(m_FacesContext.getExternalContext()).thenReturn(testExtContext);
        when(testExtContext.getRequestParameterMap()).thenReturn(paramsMap);
        when(image.getEncoding()).thenReturn(encoding);
        when(image.getValue()).thenReturn(byteArr);
        
        //Call to method being tested. Should return a StreamedContent object.
        StreamedContent content = m_SUT.getStreamSecondaryImage();
        
        //Verify the StreamedContent has the appropriate type set.
        assertThat(content.getContentType(), is("image/something"));
        
        int streamByte;
        int index = 0;
        //Verify that all the bytes within the StreamedContent are appropriate and in the correct order.
        while ((streamByte = content.getStream().read()) != -1)
        {
            assertThat((byte)streamByte, is(byteArr[index]));
            index++;
        }
    }
    
    /**
     * Verify that if a mission name cannot be found an empty default streamed content object 
     * is returned.
     */
    @Test
    public void testGetStreamSecondaryImageWithNoMissionName()
    {
      //Mock the image and external context.
        ExternalContext testExtContext = mock(ExternalContext.class);
        DigitalMedia image = mock(DigitalMedia.class);
        List<DigitalMedia> secondaryImages = new ArrayList<DigitalMedia>();
        secondaryImages.add(image);
        //MissionModel testMission = new MissionModel();
        MissionProgramTemplate testMission = mock(MissionProgramTemplate.class);
        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put("imageId", "0");
    
        //Mocked methods.
        when(m_Tpm.getTemplate("testName")).thenReturn(testMission);
        when(testMission.getSecondaryImages()).thenReturn(secondaryImages);
        when(m_FacesContext.getExternalContext()).thenReturn(testExtContext);
        when(testExtContext.getRequestParameterMap()).thenReturn(paramsMap);
        
        StreamedContent content = m_SUT.getStreamSecondaryImage();
        
        assertThat(content.getContentType(), nullValue());
        assertThat(content.getStream(), nullValue());
    }
    
    /**
     * Test that if a template name is found to belong to a remote controller that it is 
     * represented as a synced template.
     * 
     * Verify that the template is marked as synced.
     */
    @Test
    public void testGetMissionsSynced()
    {
        //Mock class and create real data needed for the primary and secondary images.
        MissionProgramTemplate template = mock(MissionProgramTemplate.class);
        Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        templates.add(template);
        
        //mock that the mission program Mgr returns a list that contains the added template's name
        List<String> remoteName = new ArrayList<String>();
        remoteName.add("Name 1");
        when(m_MissionProgramMgr.getRemoteTemplateNames(123)).thenReturn(remoteName);
        
        DigitalMedia image = mock(DigitalMedia.class);
        MissionVariableMetaData variableData = mock(MissionVariableMetaData.class);
        Set<String> testNames = new HashSet<String>();
        testNames.add("Name 1");
        List<DigitalMedia> imageList = new ArrayList<DigitalMedia>();
        imageList.add(image);
        List<MissionVariableMetaData> variableList = new ArrayList<MissionVariableMetaData>();
        variableList.add(variableData);
        byte[] someByteArr = {10, 12, 15, 25, 35};
    
        //Mocked methods.
        when(m_Tpm.getTemplates()).thenReturn(templates);
        when(template.getName()).thenReturn("Name 1");
        when(template.getDescription()).thenReturn("Some description about the mission.");
        when(template.getPrimaryImage()).thenReturn(image);
        when(template.getSecondaryImages()).thenReturn(imageList);
        when(template.getVariableMetaData()).thenReturn(variableList);
        when(image.getEncoding()).thenReturn("image/some encoding");
        when(image.getValue()).thenReturn(someByteArr);
        when(variableData.getName()).thenReturn("Awesome Variable!");
        when(variableData.getDefaultValue()).thenReturn("Default");
        when(variableData.getType()).thenReturn(MissionVariableTypesEnum.STRING);

        //active controller behavior
        ControllerModel contModel = new ControllerModel(911, m_ControllerImageInterface);
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(contModel);
        
        //mission program mgr behavior
        List<String> names = new ArrayList<String>();
        names.add("Name 1");
        when(m_MissionProgramMgr.getRemoteTemplateNames(911)).thenReturn(names);

        //Verify that the mission is retrieved and all data is correct.
        m_SUT.getMissions();
        assertThat(m_SUT.getMissions().get(0).getName(), equalTo("Name 1"));
        assertThat(m_SUT.getMissions().get(0).getDescription(), equalTo("Some description about the mission."));
        assertThat(m_SUT.getMissions().get(0).getArguments().get(0).getName(), equalTo("Awesome Variable!"));
        assertThat((String)m_SUT.getMissions().get(0).getArguments().get(0).getCurrentValue(), equalTo("Default"));
        assertThat(m_SUT.getMissions().get(0).getLocation(), is(MissionTemplateLocation.SYNCED));
    }

    /**
     * Test get mission when there is not an active controller set.
     * 
     * Verify template is denoted as local.
     */
    @Test
    public void testGetMissionsNoActiveController()
    {
        //Mock class and create real data needed for the primary and secondary images.
        MissionProgramTemplate template = mock(MissionProgramTemplate.class);
        Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        templates.add(template);
        DigitalMedia image = mock(DigitalMedia.class);
        MissionVariableMetaData variableData = mock(MissionVariableMetaData.class);
        Set<String> testNames = new HashSet<String>();
        testNames.add("Name 1");
        List<DigitalMedia> imageList = new ArrayList<DigitalMedia>();
        imageList.add(image);
        List<MissionVariableMetaData> variableList = new ArrayList<MissionVariableMetaData>();
        variableList.add(variableData);
        byte[] someByteArr = {10, 12, 15, 25, 35};
    
        //Mocked methods.
        when(m_Tpm.getTemplates()).thenReturn(templates);
        when(template.getName()).thenReturn("Name 1");
        when(template.getDescription()).thenReturn("Some description about the mission.");
        when(template.getPrimaryImage()).thenReturn(image);
        when(template.getSecondaryImages()).thenReturn(imageList);
        when(template.getVariableMetaData()).thenReturn(variableList);
        when(image.getEncoding()).thenReturn("image/some encoding");
        when(image.getValue()).thenReturn(someByteArr);
        when(variableData.getName()).thenReturn("Awesome Variable!");
        when(variableData.getDefaultValue()).thenReturn("Default");
        when(variableData.getType()).thenReturn(MissionVariableTypesEnum.STRING);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(false);
        
        //Verify that the mission is retrieved and all data is correct.
        List<MissionModel> models = m_SUT.getMissions();
        assertThat(models.get(0).getName(), equalTo("Name 1"));
        assertThat(models.get(0).getDescription(), equalTo("Some description about the mission."));
        assertThat(models.get(0).getArguments().get(0).getName(), equalTo("Awesome Variable!"));
        assertThat((String)models.get(0).getArguments().get(0).getCurrentValue(), equalTo("Default"));
        assertThat(models.get(0).getLocation(), is(MissionTemplateLocation.LOCAL));
    }
}
