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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import aQute.bnd.osgi.Jar;

/**
 * @author dlandoll
 *
 */
public class TestProjectProperties
{
    private ProjectProperties m_SUT;
    private BndService m_BndService;

    @Before
    public void setUp() throws Exception
    {
        m_BndService = mock(BndService.class);

        m_SUT = new ProjectProperties(m_BndService);
        m_SUT.setSdkDirectory("temp");
    }

    @Test
    public void testGetProperties()
    {
        Map<String, Object> dataModel = m_SUT.getProperties();
        assertNotNull(dataModel);
        assertThat(dataModel, hasEntry("author", (Object)System.getProperty("user.name")));
        assertThat(dataModel, hasEntry("task", (Object)"TODO")); // this TO_DO is used as a string value
    }

    @Test
    public void testDefaultJavacTarget()
    {
        Map<String, Object> dataModel = m_SUT.getProperties();
        assertNotNull(dataModel);

        assertThat(m_SUT.getJavacTarget(), is("1.8"));
        assertThat(dataModel, hasEntry("javacTarget", (Object)"1.8"));
    }

    @Test
    public void testOverrideJavacTarget()
    {
        Map<String, Object> dataModel = m_SUT.getProperties();
        assertNotNull(dataModel);

        m_SUT.setJavacTarget("1.9");
        assertThat(m_SUT.getJavacTarget(), is("1.9"));
        assertThat(dataModel, hasEntry("javacTarget", (Object)"1.9"));
    }

    @Test
    public void testGetSdkDirectory()
    {
        String retValue = m_SUT.getSdkDirectory();
        assertThat(retValue, is(notNullValue()));

        String testValue = "temp2";
        m_SUT.setSdkDirectory(testValue);
        assertThat(m_SUT.getSdkDirectory(), is(testValue));
    }

    @Test
    public void testGetProjectName()
    {
        String retValue = m_SUT.getProjectName();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testProjName";
        m_SUT.setProjectName(testValue);
        assertThat(m_SUT.getProjectName(), is(testValue));
    }

    @Test
    public void testGetClassName()
    {
        String retValue = m_SUT.getClassName();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testClassName";
        m_SUT.setClassName(testValue);
        assertThat(m_SUT.getClassName(), is(testValue));
    }

    @Test
    public void testGetPackageName()
    {
        String retValue = m_SUT.getPackageName();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testPackageName";
        m_SUT.setPackageName(testValue);
        assertThat(m_SUT.getPackageName(), is(testValue));
    }

    /**
     * Verify attribute type can be correctly set and retrieved.
     */
    @Test
    public void testGetAttributeType()
    {
        String testValue = "testAttributeType";
        m_SUT.setAttributeType(testValue);
        assertThat(m_SUT.getProperties(), hasEntry(ProjectProperties.PROP_ATTR_TYPE, (Object)testValue));
    }

    /**
     * Verify proxy type can be correctly set and retrieved.
     */
    @Test
    public void testGetProxyType()
    {
        String testValue = "testProxyType";
        m_SUT.setProxyType(testValue);
        assertThat(m_SUT.getProperties(), hasEntry(ProjectProperties.PROP_PROXY_TYPE, (Object)testValue));
    }

    @Test
    public void testGetVendor()
    {
        String retValue = m_SUT.getVendor();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testVendor";
        m_SUT.setVendor(testValue);
        assertThat(m_SUT.getVendor(), is(testValue));
    }

    @Test
    public void testGetDescription()
    {
        String retValue = m_SUT.getDescription();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testDescription";
        m_SUT.setDescription(testValue);
        assertThat(m_SUT.getDescription(), is(testValue));
    }

    @Test
    public void testGetAuthor()
    {
        String retValue = m_SUT.getAuthor();
        assertThat(retValue, is(notNullValue()));

        String testValue = "testAuthor";
        m_SUT.setAuthor(testValue);
        assertThat(m_SUT.getAuthor(), is(testValue));
    }

    @Test
    public void testGetProjectType()
    {
        ProjectType retValue = m_SUT.getProjectType();
        assertThat(retValue, is(notNullValue()));

        ProjectType testValue = ProjectType.PROJECT_LINKLAYER;
        m_SUT.setProjectType(testValue);
        assertThat(m_SUT.getProjectType(), is(testValue));
    }

    /**
     * Verify the API version is retrieved from the bundle version for the core API bundle.
     */
    @Test
    public void testGetApiVersion() throws Exception
    {
        Jar jar = mock(Jar.class);
        when(jar.getVersion()).thenReturn("7.6");
        when(m_BndService.newJar(new File("temp/api/mil.dod.th.core.api.jar"))).thenReturn(jar);

        assertThat(m_SUT.getApiVersion(), is("7.6"));
    }

    /**
     * Verify setting the property will make it available from the map.
     */
    @Test
    public void testSetBasePackage()
    {
        m_SUT.setBasePackage("test.package");
        assertThat(m_SUT.getProperties(), hasEntry(ProjectProperties.PROP_BASE_PACKAGE, (Object)"test.package"));
    }

    /**
     * Verify setting the property will make it available from the map.
     */
    @Test
    public void testSetPhysicalLinkExtraCode()
    {
        m_SUT.setPhysicalLinkExtraCode("void someMethod();");
        assertThat(m_SUT.getProperties(), hasEntry(ProjectProperties.PROP_PL_EXTRA_CODE, (Object)"void someMethod();"));
    }
}
