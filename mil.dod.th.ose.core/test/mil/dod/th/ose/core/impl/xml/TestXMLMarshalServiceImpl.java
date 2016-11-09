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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;

import mil.dod.th.ose.core.xml.JAXBContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test class for {@link XmlMarshalServiceImpl}.
 * 
 * @author cweisenborn
 */
public class TestXMLMarshalServiceImpl
{
    private XmlMarshalServiceImpl m_SUT;
    private JAXBContextFactory m_ContextFactory;
    
    @Before
    public void setup()
    {
        m_SUT = new XmlMarshalServiceImpl();
        m_ContextFactory = mock(JAXBContextFactory.class);
        
        m_SUT.setJAXBContextFactory(m_ContextFactory);
    }
    
    /**
     * Test the getXMLFile method.
     * Verify that a file is returned and the name is correct.
     */
    @Test
    public void testCreateXMLByteArray() throws MarshalException, JAXBException
    {
        String sequence = "Test Sequence!";
        byte[] testArr = sequence.getBytes();
        Marshaller marshaller = mock(Marshaller.class);
        JAXBContext context = mock(JAXBContext.class);
        when(m_ContextFactory.getContext(String.class)).thenReturn(context);
        when(context.createMarshaller()).thenReturn(marshaller);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                String sequence = "Test Sequence!";
                ByteArrayOutputStream baos = (ByteArrayOutputStream)invocation.getArguments()[1];
                baos.write(sequence.getBytes());
                return null;
            }
        }).when(marshaller).marshal(eq(sequence), Mockito.any(OutputStream.class));
        
        assertThat(m_SUT.createXmlByteArray(sequence, true), is(testArr));
        verify(marshaller).setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        assertThat(m_SUT.createXmlByteArray(sequence, false), is(testArr));
        verify(marshaller).setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);       
    }
}
