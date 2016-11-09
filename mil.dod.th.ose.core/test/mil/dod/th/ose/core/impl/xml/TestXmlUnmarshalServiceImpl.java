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
package mil.dod.th.ose.core.impl.xml;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

import mil.dod.th.ose.core.xml.JAXBContextFactory;
import mil.dod.th.ose.utils.xml.XmlService;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author bachmakm
 *
 */
public class TestXmlUnmarshalServiceImpl
{

    private XmlUnmarshalServiceImpl m_SUT;   
    private Bundle m_bunMock;
    private BundleContext m_bunContextMock;
    private TestClass m_TestData;
    private Schema m_Schema;
    private SchemaFactory m_SchemaFactory;
    private JAXBContextFactory m_JaxbContextFactory;
    private JAXBContext m_JaxbContext;
    
    interface TestClass {}
    
    @Before
    public void setUp() throws Exception
    { 
        m_SUT = new XmlUnmarshalServiceImpl();
        m_bunMock = mock(Bundle.class);   
        m_bunContextMock = mock(BundleContext.class);
        when(m_bunContextMock.getBundle()).thenReturn(m_bunMock);
        
        m_SUT.activate(m_bunContextMock);
        
        Unmarshaller unmarshalMock = mock(Unmarshaller.class);
        m_JaxbContext = mock(JAXBContext.class);
        m_JaxbContextFactory = mock(JAXBContextFactory.class);
        m_Schema = mock(Schema.class); 
        m_TestData = mock(TestClass.class);
        
        //mocked objects corresponding to sequence of events in getXMLObject method
        m_SUT.setJAXBContextFactory(m_JaxbContextFactory);
        when(m_JaxbContextFactory.getContext(TestClass.class)).thenReturn(m_JaxbContext);
        when(m_JaxbContextFactory.getContext(getClass())).thenReturn(m_JaxbContext);
        when(m_JaxbContext.createUnmarshaller()).thenReturn(unmarshalMock);
        XmlService xmlService = mock(XmlService.class);
        m_SUT.setXmlService(xmlService);
        m_SchemaFactory = mock(SchemaFactory.class);
        when(xmlService.createSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI)).thenReturn(m_SchemaFactory);
        
        // by default, have bundle return some URL
        URL schemaUrl = new URL("file:schema.xsd");
        when(m_bunMock.getResource(anyString())).thenReturn(schemaUrl);

        when(m_SchemaFactory.newSchema(schemaUrl)).thenReturn(m_Schema);      
        when(unmarshalMock.unmarshal(Mockito.any(InputStream.class))).thenReturn(m_TestData);
        URL objectUrl = new URL("file:object.xml");
        when(unmarshalMock.unmarshal(objectUrl)).thenReturn(m_TestData);
        
        //needed to prevent actual setSchema method from executing - otherwise will throw NullPointerException
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                return "called with args "+args;
            }
        }).when(unmarshalMock).setSchema(Mockito.any(Schema.class));       
    }

    /**
     * Verify the method will retrieve a capabilities object given a valid URL.
     * 
     * Verify correct schema is used based on the class.
     */
    @Test
    public void testGetXmlObjectURL() throws UnmarshalException, SAXException, JAXBException, MalformedURLException
    {
        TestClass data = m_SUT.getXmlObject(TestClass.class, new URL("file:object.xml"));
        
        // verify schema, should be the fully qualified class name, replacing . with /, mil.dod.th with scheamas and 
        // change .java to .xsd
        verify(m_bunMock).getResource("schemas/ose/core/impl/xml/TestClass.xsd");
        
        assertThat(data, is(m_TestData));
    }
    
    /**
     * Verify the method will retrieve a capabilities object given the input stream.
     */
    @Test
    public void testGetXmlObjectStream() throws UnmarshalException, SAXException, JAXBException
    {
        InputStream is = mock(InputStream.class);
        TestClass data = m_SUT.getXmlObject(TestClass.class, is);
        assertThat(data, is(m_TestData));
    }
    
    /**
     * Verify the method will retrieve a capabilities object given the array.
     */
    @Test
    public void testGetXmlObjectArray() throws UnmarshalException, SAXException, JAXBException
    {
        byte[] array = { 0x1 };
        TestClass data = m_SUT.getXmlObject(TestClass.class, array);
        assertThat(data, is(m_TestData));
    }
    
    /**
     * Verify exception will be thrown if trying to get object of an invalid type (no schema)
     */
    @Test
    public void testGetXmlObjectNoSchema() throws SAXException, JAXBException
    {
        byte[] array = { 0x1 };
        
        // mock bundle to return null
        when(m_bunMock.getResource(anyString())).thenReturn(null);
        
        try
        {
            m_SUT.getXmlObject(getClass(), array);
            fail("Expecting exception as class does not have schema");
        }
        catch (IllegalStateException e)
        {
        }      
    }
    
    @Test
    public void testGetXMLResource() throws UnmarshalException, MalformedURLException
    {
        Bundle bundle = mock(Bundle.class);
        URL expectedUrl = new URL("file:blah");
        when(bundle.getEntry(Mockito.anyString())).thenReturn(expectedUrl);
        
        URL url = m_SUT.getXmlResource(bundle, "file:///my-xml-folder", "my.class.name");
        assertThat(url, is(expectedUrl));
        
        // verify invalid URL throws an exception
        when(bundle.getEntry(Mockito.anyString())).thenReturn(null);
        
        try
        {
            m_SUT.getXmlResource(bundle, "file:///my-xml-folder", "my.class.name");
            fail("Expecting exception as URL could not be found");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
}
