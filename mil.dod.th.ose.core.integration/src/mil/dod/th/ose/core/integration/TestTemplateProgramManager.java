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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import junit.framework.TestCase;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;

/**
 * Tests the ability of the {@link TemplateProgramManager} to manage {@link MissionProgramTemplate}s.
 * 
 * @author callen
 *
 */
public class TestTemplateProgramManager extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private TemplateProgramManager m_TemplateProgramManager;

    @Override
    public void setUp()
    {
        m_TemplateProgramManager = ServiceUtils.getService(m_Context, TemplateProgramManager.class);
        assertThat(m_TemplateProgramManager, is(notNullValue()));
        
        GeneralUtils.assertSetupHasRun(m_Context);
    }

    /**
     * Verify ability to load and get templates.
     */
    public void testLoadMissionTemplate() throws IllegalArgumentException, PersistenceFailedException
    {
        MissionProgramTemplate progTemplate = createTestTemplate("TemplateTest", 
                "To be or not to be, that is the question.");
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("TemplateTest"));
        
        //get the template made already
        MissionProgramTemplate template = m_TemplateProgramManager.getTemplate("TemplateTest");
        assertThat(template.getSource(), is("To be or not to be, that is the question."));
        
        //get a template that doesn't exist
        try
        {
            template = m_TemplateProgramManager.getTemplate("ImagineryTemplate");
            fail("Expected Exception");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception the template requested does not exist.       
        }
    }
    
    /**
     * Verify ability to get the names of templates loaded on a controller.
     */
    public void testGetTemplateNames() throws IllegalArgumentException, PersistenceFailedException
    {
        String source = "This is good Source";
        //add a few templates
        MissionProgramTemplate progTemplate = createTestTemplate("TemplateTest1", source);
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);  
        progTemplate = createTestTemplate("TemplateTest2", source);
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        progTemplate = createTestTemplate("TemplateTest3", source);
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        progTemplate = createTestTemplate("TemplateTest4", source);
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        
        //verify
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("TemplateTest1"));
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("TemplateTest2"));
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("TemplateTest3"));
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("TemplateTest4"));
    }
    
    /**
     * Verify the ability to delete/remove previously loaded mission program templates.
     */
    public void testRemoveTemplate() throws IllegalArgumentException, PersistenceFailedException
    {
        //add templates
        MissionProgramTemplate progTemplate = createTestTemplate("TemplateTest1", "source");
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);  
        progTemplate = createTestTemplate("TemplateTest2", "source");
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        
        //remove templates
        m_TemplateProgramManager.removeTemplate("TemplateTest1");
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), not(hasItem("TemplateTest1")));
        m_TemplateProgramManager.removeTemplate("TemplateTest2");
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), not(hasItem("TemplateTest2")));
        
        //try to remove template that is already gone
        try
        {
            m_TemplateProgramManager.removeTemplate("TemplateTest2");
            fail("Expected Exception");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception as the template was already removed. 
        }
    }
    
    /**
     * Inside the template directory there are templates prefixed with 'bad'. These templates
     * specifically check that even with different types of invalid templates that all other templates in the 
     * directory are still loaded. 
     */
    public void testBadTemplate()
    {
        //get the template that has ill-formed XML
        try
        {
            m_TemplateProgramManager.getTemplate("bad-template");
            fail("Expected exception the template should not be in the lookup");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
        //try to get the template that is missing a required element
        try
        {
            m_TemplateProgramManager.getTemplate("bad-template-missing-element");
            fail("Expected exception the template should not be in the lookup");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
        //try to get the template with an added element not define in the mission program schemas
        try
        {
            m_TemplateProgramManager.getTemplate("bad-template-added-element");
            fail("Expected exception the template should not be in the lookup");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
        //verify that the other two templates from the directory are loaded
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("timed-data-capture-loop"));
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("triggered-data-captured"));
    }
    
    /**
     * Create a test template.
     */
    private MissionProgramTemplate createTestTemplate(final String name, final String source)
    {
        return new MissionProgramTemplate().withSource(source).withName(name);
    }
}
