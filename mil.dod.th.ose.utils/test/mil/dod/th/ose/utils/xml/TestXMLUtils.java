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
package mil.dod.th.ose.utils.xml;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field; // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.hamcrest.Matchers.*;

public class TestXMLUtils
{
    @XmlRootElement
    static class JaxbExample
    {
        
    }
    
    @After
    public void tearDown() throws Exception
    {
        //ensure that we have a blank class map
        Field classMap = XmlUtils.class.getDeclaredField("serMap");
        
        classMap.setAccessible(true);
        
        Map<Class<?>, JAXBContext> map = new HashMap<Class<?>, JAXBContext>();
        
        classMap.set(null, map);
    }
    
    /**
     * Create an example with the just the base required properties set.
     */
    private JaxbExample createExample()
    {
        final JaxbExample example = new JaxbExample();
        return example;
    }
    
    @Test
    public void testToXMLRootElementSpecified()
    {
        JaxbExample obs = createExample(); 
        
        byte[] receivedArray = XmlUtils.toXML(obs, true, "JaxbExample");
        
        JAXBContext context;
        try
        {
            context = JAXBContext.newInstance(JaxbExample.class);
            final Marshaller marshaller = context.createMarshaller();
            
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            
            final JAXBElement<JaxbExample> jaxbElement = new JAXBElement<JaxbExample>(new QName("JaxbExample"), 
                    JaxbExample.class, obs);
            
            marshaller.marshal(jaxbElement, byteStream);
            
            String expected = new String(byteStream.toByteArray());
            
            String received = new String(receivedArray);
            
            //make sure strings are same length and are exactly the same
            assertThat(received.length(), is(expected.length()));
            assertThat(received, is(expected));            
        }
        catch (JAXBException e)
        {
            fail("A JAXBException has occured.");
        }
    }
    
    @Test
    public void testToXML()
    {
        JaxbExample obs = createExample(); 
        
        byte[] bArray = XmlUtils.toXML(obs, true);
        
        JAXBContext context;
        try
        {
            context = JAXBContext.newInstance(JaxbExample.class);
            final Marshaller marshaller = context.createMarshaller();
            
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            
            marshaller.marshal(obs, byteStream);
            
            String expected = new String(byteStream.toByteArray());
            
            String received = new String(bArray);
            
            //make sure strings are same length and are exactly the same
            assertThat(received.length(), is(expected.length()));
            assertThat(received, is(expected));            
        }
        catch (JAXBException e)
        {
            fail("A JAXBException has occured.");
        }
    }
    
    @Test
    public void testToXMLElement()
    {
        JaxbExample obs = createExample(); 
        
        Element theElement = XmlUtils.toXMLElement(obs);
        
        assertThat(theElement, notNullValue());
        
        assertThat(theElement.getLocalName(), is("jaxbExample"));     
    }
    
    @Test (expected= IllegalStateException.class)
    public void testToXMLElementError()
    {
        //wrong value...should be an object
        XmlUtils.toXMLElement(JaxbExample.class);
    }
    
    /**
     * Verify fromXML creates an object from XML data.
     */
    @Test
    public void testFromXML() throws JAXBException
    {
        JaxbExample expectedObs = createExample(); 
        
        JAXBContext context = JAXBContext.newInstance(JaxbExample.class);
        final Marshaller marshaller = context.createMarshaller();
        
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        marshaller.marshal(expectedObs, byteStream);
        
        byte[] bArray = byteStream.toByteArray();
        assertThat(bArray, notNullValue());
        
        JaxbExample receivedObs = (JaxbExample)XmlUtils.fromXML(bArray, JaxbExample.class);
        
        assertThat(receivedObs, notNullValue());
    }
}
