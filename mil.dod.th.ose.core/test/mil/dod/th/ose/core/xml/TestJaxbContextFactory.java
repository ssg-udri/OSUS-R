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
package mil.dod.th.ose.core.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Admin
 *
 */
public class TestJaxbContextFactory
{
    private JAXBContextFactoryImpl m_SUT;   

    @Before
    public void setUp() throws Exception
    {         
        m_SUT = new JAXBContextFactoryImpl();

    }

    @Test
    public void testGetContext() throws JAXBException
    {
        JAXBContext jaxbContext = m_SUT.getContext(AssetCapabilities.class);
        assertThat(jaxbContext, is(notNullValue()));
        assertThat(jaxbContext, is(instanceOf(JAXBContext.class)));
    }

    /**
     * Verify getting the context multiple times for the same class with return the same context.
     */
    @Test
    public void testGetMuliptleContexts() throws JAXBException
    {
        JAXBContext jaxbContext1 = m_SUT.getContext(AssetCapabilities.class);
        JAXBContext jaxbContext2 = m_SUT.getContext(AssetCapabilities.class);
        JAXBContext jaxbContext3 = m_SUT.getContext(LinkLayerCapabilities.class);
        
        // different classes should have different contexts
        assertThat(jaxbContext1, is(not(jaxbContext3)));
        
        // same class should have the same context
        assertThat(jaxbContext1, is(jaxbContext2));
    }
}
