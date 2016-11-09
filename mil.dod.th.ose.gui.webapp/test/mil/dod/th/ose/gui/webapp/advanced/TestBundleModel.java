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
package mil.dod.th.ose.gui.webapp.advanced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test class for the {@link BundleModel} class.
 * 
 * @author cweisenborn
 */
public class TestBundleModel
{
    private BundleModel m_SUT;
    
    @Before
    public void setup()
    {
        BundleInfoType.Builder type = BundleInfoType.newBuilder();
        type.setBundleId(1L).setBundleDescription("Test Description").
            setBundleLocation("Somewhere").setBundleName("Name").
            setBundleState(Bundle.INSTALLED).setBundleSymbolicName("Symbolic Name").
            setBundleVendor("Nobody").setBundleVersion("-1000.1.2");
        
        BundleInfoType infoType = type.build();
        
        List<BundleInfoType> bInfo = new ArrayList<BundleInfoType>();
        bInfo.add(infoType);
        
        //Create the bundle model by passing the mocked message to the constructor.
        m_SUT = new BundleModel(infoType);
    }
    
    /**
     * Test getting the bundles symbolic name.
     * Verify the correct name was returned.
     */
    @Test
    public void testGetSymbolicName()
    {
        assertThat(m_SUT.getSymbolicName(), is("Symbolic Name"));
    }
    
    /**
     * Test getting the bundle vendor name.
     * Verify the correct vendor name was returned.
     */
    @Test
    public void testGetBundleVendor()
    {
        assertThat(m_SUT.getBundleVendor(), is("Nobody"));
    }
    
    /**
     * Test getting the bundle name.
     * Verify the correct name is returned for the bundle.
     */
    @Test
    public void testGetBundleName()
    {
        assertThat(m_SUT.getBundleName(), is("Name"));
    }
    
    /**
     * Test getting the bundle description.
     * Verify the description is returned correctly.
     */
    @Test
    public void testGetDescription()
    {
        assertThat(m_SUT.getDescription(), is("Test Description"));
    }   
    
    /**
     * Test getting the bundle version.
     * Verify the correct version string is returned.
     */
    @Test
    public void testGetVersion()
    {
        assertThat(m_SUT.getVersion(), is("-1000.1.2"));
    }   
    
    /**
     * Test getting the ID number of the bundle.
     * Verify that correct long representing the bundle ID is returned.
     */
    @Test
    public void testGetBundleId()
    {
        assertThat(m_SUT.getBundleId(), is(Long.valueOf(1)));
    }
    
    /**
     * Test getting the location of the bundle.
     * Verify the correct location for the bundle is returned.
     */
    @Test
    public void getSetLocation()
    {
        assertThat(m_SUT.getLocation(), is("Somewhere"));
    }
    
    /**
     * Test getting the current state of the bundle.
     * Verify the correct state of the bundle was returned.
     */
    @Test
    public void testBundleState()
    {
        assertThat(m_SUT.getState(), is("Installed"));
        
        m_SUT.setState(Bundle.RESOLVED);
        assertThat(m_SUT.getState(), is("Resolved"));
        
        m_SUT.setState(Bundle.STARTING);
        assertThat(m_SUT.getState(), is("Starting"));
        
        m_SUT.setState(Bundle.STOPPING);
        assertThat(m_SUT.getState(), is("Stopping"));
        
        m_SUT.setState(Bundle.ACTIVE);
        assertThat(m_SUT.getState(), is("Active"));
        
        m_SUT.setState(Bundle.UNINSTALLED);
        assertThat(m_SUT.getState(), is("Uninstalled"));
        
        m_SUT.setState(10000);
        assertThat(m_SUT.getState(), is("State Could Not Be Determined"));
    }
    
    /**
     * Test getting the packages imported by the bundle.
     * Verify that the correct list of packages is returned.
     */
    @Test
    public void testGetImportPackages()
    {
        List<String> imports = new ArrayList<String>();
        imports.add("This bundle does not import any packages.");
        assertThat(m_SUT.getImportPackages(), is(imports));
    }
    
    /**
     * Test getting the packages exported by the bundle.
     * Verify that the correct list of packages is returned.
     */
    @Test
    public void testGetExportPackages()
    {
        List<String> exports = new ArrayList<String>();
        exports.add("This bundle does not export any packages.");
        assertThat(m_SUT.getExportPackages(), is(exports));
    }
    
    /**
     * Test updating the bundle.
     * Verify that all the fields within the bundle are updated accordingly.
     */
    @Test
    public void testUpdateBundle()
    {
        //New bundle information.
        long id = 1L;
        String desc = "New Description";
        String loc = "New Location";
        String name = "New Name";
        String symbName = "New Symbolic Name";
        String vendor = "New Vendor";
        String version = "3.0";
        
        
        BundleInfoType.Builder builderType = BundleInfoType.newBuilder();
        
        builderType.setBundleId(id);
        builderType.setBundleDescription(desc);
        builderType.setBundleLocation(loc);
        builderType.setBundleName(name);
        builderType.setBundleState(Bundle.ACTIVE);
        builderType.setBundleSymbolicName(symbName);
        builderType.setBundleVendor(vendor);
        builderType.setBundleVersion(version);
        
        List<String> exports = new ArrayList<String>();
        exports.add("Export 1");
        exports.add("Export 2");
        
        builderType.addAllPackageExport(exports);
        
        List<String> imports = new ArrayList<String>();
        imports.add("Import 1");
        imports.add("Import 2");
        
        builderType.addAllPackageImport(imports);
        
        //Replay
        m_SUT.updateBundle(builderType.build());
        
        //Verify that the fields within the bundle are set correctly.
        assertThat(m_SUT.getBundleId(), is(id));
        assertThat(m_SUT.getDescription(), is(desc));
        assertThat(m_SUT.getLocation(), is(loc));
        assertThat(m_SUT.getBundleName(), is(name));
        assertThat(m_SUT.getState(), is("Active"));
        assertThat(m_SUT.getSymbolicName(), is(symbName));
        assertThat(m_SUT.getBundleVendor(), is(vendor));
        assertThat(m_SUT.getVersion(), is(version));
        assertThat(m_SUT.getImportPackages(), is(imports));
        assertThat(m_SUT.getExportPackages(), is(exports));
    }
}
