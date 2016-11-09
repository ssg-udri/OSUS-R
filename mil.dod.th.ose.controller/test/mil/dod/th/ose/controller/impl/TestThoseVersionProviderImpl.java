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
package mil.dod.th.ose.controller.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Properties;

import mil.dod.th.ose.controller.api.ThoseVersionProvider;
import mil.dod.th.ose.test.PropertyRetrieverMocker;
import mil.dod.th.ose.utils.PropertyRetriever;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * @author dhumeniuk
 *
 */
public class TestThoseVersionProviderImpl
{
    private ThoseVersionProviderImpl m_SUT;
    private Properties m_TmpProps;
    private Properties m_InfoProps;
    private BundleContext m_Context;

    @Before
    public void setUp() throws IOException
    {
        m_SUT = new ThoseVersionProviderImpl();
        m_Context = mock(BundleContext.class);
        
        m_TmpProps = new Properties();
        m_InfoProps = new Properties();
        
        PropertyRetriever propRetriever = PropertyRetrieverMocker.mockIt(m_Context, 
                new String[] {ThoseVersionProvider.TMP_BUILD_FILE_NAME, ThoseVersionProvider.BUILD_INFO_FILE_NAME},
                new Properties[] {m_TmpProps, m_InfoProps});
        m_SUT.setPropertyRetriever(propRetriever);
    }
    
    /**
     * Test version for an individual build.
     */
    @Test
    public final void testGetVersionIndividual() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.INDIVIDUAL_BUILD_TYPE);
        m_TmpProps.put(ThoseVersionProvider.BUILD_NUM_PROP_NAME, 2);
        m_TmpProps.put(ThoseVersionProvider.BUILD_USER_PROP_NAME, "jdoe");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 5);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 3);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 6);
        
        assertThat(m_SUT.getVersion(), is("5.3.6-ind.jdoe.2"));
    }
    
    /**
     * Test version for a Jenkins build.
     */
    @Test
    public final void testGetVersionJenkins() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.AUTOMATED_BUILD_TYPE);
        m_TmpProps.put(ThoseVersionProvider.BUILD_GIT_HASH, "af223d523");
        m_TmpProps.put(ThoseVersionProvider.BUILD_BRANCH_PROP_NAME, "feature/TH-2343-this-is-a-branch");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 3);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 1);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 2);
        
        assertThat(m_SUT.getVersion(), is("3.1.2-feature/TH-2343-this-is-a-branch.af223d5"));
    }
    
    /**
     * Test version for a automated release build.
     */
    @Test
    public final void testGetVersionAutoRelease() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.AUTOMATED_BUILD_TYPE);
        m_TmpProps.put(ThoseVersionProvider.BUILD_BRANCH_PROP_NAME, "origin/release/TH-2343-this-is-a-branch");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 3);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 1);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 2);
        
        assertThat(m_SUT.getVersion(), is("3.1.2"));
    }
    
    /**
     * Test version for a automated release build no branch info.
     */
    @Test
    public final void testGetVersionAutoReleaseNoBranch() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.AUTOMATED_BUILD_TYPE);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 3);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 1);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 2);
        
        try
        {
            m_SUT.activate(m_Context);
            fail("Expected failure because automated build type must include branch information.");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
    }
    
    /**
     * Test version when metadata is supplied.
     */
    @Test
    public final void testGetVersionMetadata() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.AUTOMATED_BUILD_TYPE);
        m_TmpProps.put(ThoseVersionProvider.BUILD_GIT_HASH, "af223d5ff23");
        m_TmpProps.put(ThoseVersionProvider.BUILD_BRANCH_PROP_NAME, "origin/feature/TH-2343-this-is-a-branch");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 1);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 0);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 0);
        m_InfoProps.put(ThoseVersionProvider.METADATA_PROP_NAME, "some-metadata");
        
        assertThat(m_SUT.getVersion(), is("1.0.0-origin/feature/TH-2343-this-is-a-branch.af223d5+some-metadata"));
    }
    
    /**
     * Test version for an unknown build type.
     */
    @Test
    public final void testGetVersionUnknown() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, ThoseVersionProvider.UNKNOWN_BUILD_TYPE);
        m_TmpProps.put(ThoseVersionProvider.BUILD_NUM_PROP_NAME, 0);
        m_TmpProps.put(ThoseVersionProvider.BUILD_USER_PROP_NAME, "jdoe");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        m_InfoProps.put(ThoseVersionProvider.MAJOR_NUM_PROP_NAME, 3);
        m_InfoProps.put(ThoseVersionProvider.MINOR_NUM_PROP_NAME, 4);
        m_InfoProps.put(ThoseVersionProvider.PATCH_NUM_PROP_NAME, 7);
        
        assertThat(m_SUT.getVersion(), is("3.4.7-UNKNOWN.jdoe.0"));
    }
    
    /**
     * Test version throws exception if invalid type.
     */
    @Test
    public final void testGetVersionInvalidType() throws IOException
    {
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, "invalid-type");
        m_TmpProps.put(ThoseVersionProvider.BUILD_BRANCH_PROP_NAME, "origin/feature/TH-2343-this-is-a-branch");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        try
        {
            m_SUT.getVersion();
            fail("Expecting exception due to invalid type");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Test getting build info.
     */
    @Test
    public final void testGetBuildInfo() throws IOException
    {
        m_TmpProps.put("some.prop", "510");
        m_TmpProps.put("other.prop", "blah");
        m_TmpProps.put(ThoseVersionProvider.BUILD_TYPE_PROP_NAME, "sometype");
        
        //activate, sets build type and branch name, the latter of which only matters if an automated build
        m_SUT.activate(m_Context);
        
        assertThat(m_SUT.getBuildInfo(), hasEntry("some.prop", "510"));
        assertThat(m_SUT.getBuildInfo(), hasEntry("other.prop", "blah"));
    }
}
