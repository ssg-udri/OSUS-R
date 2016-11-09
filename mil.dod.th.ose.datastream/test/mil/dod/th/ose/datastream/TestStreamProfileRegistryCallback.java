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
package mil.dod.th.ose.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author jmiller
 *
 */
public class TestStreamProfileRegistryCallback
{
    private StreamProfileRegistryCallback m_SUT;
    
    @Mock private FactoryServiceContext<StreamProfileInternal> m_FactoryServiceContext;
    @Mock private FactoryRegistry<StreamProfileInternal> m_Registry;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        when(m_FactoryServiceContext.getRegistry()).thenReturn(m_Registry);
        
        m_SUT = new StreamProfileRegistryCallback();       
    }
    
    /**
     * Verify there are no registry dependencies.
     */
    @Test
    public void testRetrieveRegistryDependencies()
    {
        assertThat(m_SUT.retrieveRegistryDependencies().size(), is(0));
    }
    
    @Test
    public void testPostObjectInitialize() throws FactoryException
    {
        StreamProfileInternal mockStreamProfile = mock(StreamProfileInternal.class);
        String name = "name";
        when(mockStreamProfile.getName()).thenReturn(name);
        when(m_Registry.isObjectCreated(name)).thenReturn(false);
        
        m_SUT.postObjectInitialize(mockStreamProfile);        
    }
}
