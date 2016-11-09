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
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.AddressTranslator;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.ProductType;

/**
 * @author allenchl
 *
 */
public class TestAddressTranslatorManager
{
    private AddressTranslatorManager m_SUT;
    private AddressTranslator m_AddrTrans1;
    private AddressTranslator m_AddrTrans2;
    private interface MyAddr extends AddressProxy {};
    @ProductType(MyAddr.class)
    private class MyAddrTrans implements AddressTranslator 
    {
        @Override
        public Map<String, Object> getAddressPropsFromString(String addressDescriptionSuffix) throws CCommException
        {
            Map<String, Object> props = new HashMap<>();
            props.put("Howdy", "Bye");
            return props;
        }
    };
    
    private class MyAddrTransBad implements AddressTranslator 
    {
        @Override
        public Map<String, Object> getAddressPropsFromString(String addressDescriptionSuffix) throws CCommException
        {
            Map<String, Object> props = new HashMap<>();
            props.put("Howdy", "Bye");
            return props;
        }
    };
    
    @Before
    public void setup()
    {
        m_SUT = new AddressTranslatorManager();
        m_AddrTrans1 = new MyAddrTrans();
        m_AddrTrans2 = new MyAddrTrans();
    }
    
    /**
     * Verify adding a new address translator.
     */
    @Test
    public void testSetAddressTranslator()
    {
        m_SUT.setAddressTranslator(m_AddrTrans1);
        
        assertThat(m_SUT.getAddressTranslator(MyAddr.class.getName()), is(m_AddrTrans1));
    }
    
    /**
     * Verify if a translator is already known for a type that it is not registered.
     */
    @Test
    public void testSetAddressTranslatorDuplicate()
    {
        //this should work
        m_SUT.setAddressTranslator(m_AddrTrans1);
        
        //this shouldn't error, but the translator won't actually be saved
        m_SUT.setAddressTranslator(m_AddrTrans2);
        
        assertThat(m_SUT.getAddressTranslator(MyAddr.class.getName()), is(m_AddrTrans1));
    }

    /**
     * Verify if the translator does not have the product type annotation that there is no error thrown.
     */
    @Test
    public void testSetAddressTranslatorNoProductType()
    {
        AddressTranslator trans = new MyAddrTransBad();
        m_SUT.setAddressTranslator(trans);      
    }
    
    /**
     * Verify that a translator can be unset and therefore no longer available.
     */
    @Test
    public void testUnSetAddressTranslator()
    {
        m_SUT.setAddressTranslator(m_AddrTrans1);
        
        assertThat(m_SUT.getAddressTranslator(MyAddr.class.getName()), is(m_AddrTrans1));
        
        m_SUT.unsetAddressTranslator(m_AddrTrans1);
        
        try
        {
            m_SUT.getAddressTranslator(MyAddr.class.getName());
            fail("Expected exception as the translator was just unset and therefore should no longer be available.");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }
}
