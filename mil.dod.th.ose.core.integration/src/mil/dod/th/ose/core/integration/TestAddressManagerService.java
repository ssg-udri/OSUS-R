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

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import example.ccomms.EchoTransport;
import example.ccomms.ExampleAddress;
import example.ccomms.ExampleLinkLayer;
import example.ccomms.FakeAddress;

/**
 * @author dhumeniuk
 *
 */
public class TestAddressManagerService extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @Override
    public void setUp() throws Exception
    {
        AddressManagerService addressMgr = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleAddress.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleLinkLayer.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, EchoTransport.class, 5000);
        
        addressMgr.flush();
    }
    
    @Override
    public void tearDown()
    {
        AddressManagerService addressMgr = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        addressMgr.flush();
    }

    /**
     * Verify new addresses can be created using the method that accepts a property dictionary.
     * 
     * Verify invalid prefixes will cause an exception to be thrown.
     */
    public void testAddressCreation() 
        throws InterruptedException, CCommException, IllegalArgumentException, FactoryException
    {
        AddressManagerService addressManagerService = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        // Create the properties for a new address
        Map<String, Object> propsAddress1 = new HashMap<String, Object>();
        propsAddress1.put("a", 600);
        // Should create an address
        addressManagerService.getOrCreateAddress(ExampleAddress.class.getName(), "Addr1", propsAddress1);    
        propsAddress1 = null; // delete this variable so it can't be used again on accident
        
        // Create a new set of properties for a new address, testing that the link is null
        Map<String, Object> propsAddr2 = new HashMap<String, Object>();
        propsAddr2.put("a", 700);

        addressManagerService.getOrCreateAddress(ExampleAddress.class.getName(), "Addr2", propsAddr2);
        propsAddr2 = null; // again, delete variable so it can't be used again on accident
    
        // Need to test createAddress, checkAddressAlreadyExists for the other implementation of createAddress
        // property "a" must exist and equal "a", for success, in ExampleAddress.
        try{
            addressManagerService.getOrCreateAddress("Examplee:10"); // Invalid name
            // Using no parameters will generally fail by design in a real factory, but here it doesn't matter 
            fail("Expected to fail - invalid name - an extra character is on the end");
        }catch(CCommException e){    }
        
        try{
            addressManagerService.getOrCreateAddress("example:10"); // Case sensitivity - invalid name
            fail("Expected to fail - invalid name - currently case-sensitive");
        }catch(CCommException e){    }
        
        // Check with a valid prefix
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:12345"),is(false));
        
        // Create the same address that was just checked
        Address a = addressManagerService.getOrCreateAddress("Example:12345");
        assertThat(a, not(nullValue()));
        
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:12345"),is(true));
    }
    
    /**
     * Ensure that an existing address that can be found is not duplicated. The service should return the found
     * address. 
     */
    public void testGetOrCreateAddressNoDuplication() throws Exception
    {
        // wait until all factories have been registered, at that point all layers must be restored
        AddressManagerService addressMgrSvc = ServiceUtils.getService(m_Context, AddressManagerService.class);
            
        // Create a new address
        Address a1 = addressMgrSvc.getOrCreateAddress("Example:5");        
        
        // Ensure the address is now in the service
        List<String> addressStringsAfterAdding = addressMgrSvc.getAddressDescriptiveStrings();
        assertThat(addressStringsAfterAdding, hasItem("Example:5"));
        
        // Now request the address again. No new address should be created.
        Address a2 = addressMgrSvc.getOrCreateAddress("Example:5");        
        assertThat(a2.getUuid(), is(a1.getUuid()));
        // List should be the same size
        assertThat(addressMgrSvc.getAddressDescriptiveStrings().size(), is(addressStringsAfterAdding.size()));
        
        // Request another address that is different.  New address should be created.
        Address a3 = addressMgrSvc.getOrCreateAddress("Example:10");
        assertThat(a2.getUuid(), is(not(a3.getUuid())));
        // List should increase by one
        assertThat(addressMgrSvc.getAddressDescriptiveStrings().size(), is(addressStringsAfterAdding.size() + 1));
    }
    
    /**
     * Verify that only address of the same type are checked against when creating a new address 
     * or checking if one already exists. Tests both getOrCreateAddress() and checkAddressAlreadyExists()
     */
    public void testSamePropertiesDifferentAddressType() 
            throws CCommException, IllegalArgumentException
    {
        AddressManagerService addressManagerService = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        //Create a common property to use for both addresses
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("a",42);
        
        //Create a new address of type "Fake", with the common properties
        Address fakeAddress1 = 
                addressManagerService.getOrCreateAddress(FakeAddress.class.getName(),"Addr1",props);
        
        //Create a new address of type "Example", with the common properties
        Address exampleAddress1 = 
                addressManagerService.getOrCreateAddress(ExampleAddress.class.getName(),"Addr2",props);
    
        assertThat(fakeAddress1.getUuid(), is(not(exampleAddress1.getUuid())));
    
        //Ensure that the address now exist
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:42"),is(true));
        assertThat(addressManagerService.checkAddressAlreadyExists("Fake:42"),is(true));
        
        //Try to get or create new addresses with existing types and properties
        Address fakeAddress2 = addressManagerService.getOrCreateAddress("Fake:42");
        Address exampleAddress2 = addressManagerService.getOrCreateAddress("Example:42");
        
        //Check to make sure returns the already existing address
        assertThat(fakeAddress2, is(fakeAddress1));
        assertThat(exampleAddress2, is(exampleAddress1));
    }
    /**
     * Verify new addresses can be created and named.
     */
    public void testAddressCreationWithName() 
        throws InterruptedException, CCommException, IllegalArgumentException, FactoryException
    {
        AddressManagerService addressManagerService = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        // Create the properties for a new address
        Address addr1 = addressManagerService.getOrCreateAddress("Example:100", "UniqueName");
        assertThat(addressManagerService.getAddressByName("UniqueName"), is(addr1));
        
        Address addr2 = addressManagerService.getOrCreateAddress("Example:200", "DoNotCopy");
        assertThat(addressManagerService.getAddressByName("DoNotCopy"), is(addr2));

        // Verify address names/descriptors can be returned
        assertThat(addressManagerService.getAddressNames(), hasItems("UniqueName", "DoNotCopy"));
        Map<String, String> nameMap = addressManagerService.getAddressNamesWithDescriptor();
        assertThat(nameMap.size(), is(2));
        assertThat(nameMap.keySet(), hasItems("UniqueName", "DoNotCopy"));
        assertThat(nameMap.values(), hasItems("Example:100", "Example:200"));

        // Try to create an address with a duplicate name and verify it was not created.
        int size = addressManagerService.getAddressDescriptiveStrings().size();
        try
        {
            addressManagerService.getOrCreateAddress("Example:300", "DoNotCopy");
        }
        catch (Exception e)
        {
            assertThat(e.getCause().getMessage(), containsString("Duplicate name: [DoNotCopy]"));
        }
        
        //check no new addresses were added
        assertThat(addressManagerService.getAddressDescriptiveStrings().size(), is(size));
        
        //check first and second addresses were created
        assertThat(addr1.isManaged(), is(true));
        assertThat(addr2.isManaged(), is(true));
    }
    
    /**
     * Verify a plug-in providing the same prefix is not registered.
     */
    public void testDuplicateAddressPrefix() throws Exception
    {
        URL url = m_Context.getBundle().getResource("example.ccomm.duplicates.jar");
        Bundle dupBundle = m_Context.installBundle(url.toString());
        
        try
        {
            // should be null as service should not register descriptor for plug-in with a duplicate prefix
            ServiceReference<FactoryDescriptor> descriptor = 
                    FactoryUtils.getFactoryDescriptorReference(m_Context, 
                            "example.ccomms.duplicates.ExampleAddressDuplicate", 5000);
            assertThat(descriptor, is(nullValue()));
            
            // now make sure we have the right name of the product type by finding the actual class resource with the 
            // same package and class name, if the class is ever moved or renamed, this test will fail here, signaling 
            // the reference above to be updated as well
            URL productTypeFile = dupBundle.getResource("example/ccomms/duplicates/ExampleAddressDuplicate.class");
            assertThat(productTypeFile, is(notNullValue()));
            
            // Ensure address is the correct product type, not duplicate type
            AddressManagerService addressManagerService = 
                    ServiceUtils.getService(m_Context, AddressManagerService.class);
            Address addr = addressManagerService.getOrCreateAddress("Example:600", "NonDuplicateProductTypeAddr");
            assertThat(addr.getFactory().getProductType(), is(ExampleAddress.class.getName()));
        }
        finally
        {
            dupBundle.uninstall();
        }
    }
}
