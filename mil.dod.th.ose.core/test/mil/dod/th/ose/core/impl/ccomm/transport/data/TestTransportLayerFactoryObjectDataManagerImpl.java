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
package mil.dod.th.ose.core.impl.ccomm.transport.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.persistence.PersistentDataStore;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the transport layer implementation of the 
 * {@link mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager}.
 * @author callen
 *
 */
public class TestTransportLayerFactoryObjectDataManagerImpl
{
    private TransportLayerFactoryObjectDataManagerImpl m_SUT;
    private PersistentDataStore m_PersistentDataStore;
    
    @Before
    public void setUp()
    {
        m_SUT = new TransportLayerFactoryObjectDataManagerImpl();
        m_PersistentDataStore = mock(PersistentDataStore.class);
    
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
    }
    
    /**
     * Verify registry.
     */
    @Test
    public void testRegistry()
    {
        //should be empty
        assertThat(m_SUT.getRegistry(), is(notNullValue()));
    }
    
    /**
     * Verify factory object service type is transport layer.
     */
    @Test
    public void testGetServiceObjectType()
    {
        assertThat(m_SUT.getServiceObjectType().getName(), is(TransportLayer.class.getName()));
    }
}
