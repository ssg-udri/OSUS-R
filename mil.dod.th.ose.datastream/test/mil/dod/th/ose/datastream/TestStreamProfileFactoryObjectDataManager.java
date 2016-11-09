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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.StreamProfileObjectData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author jmiller
 *
 */
public class TestStreamProfileFactoryObjectDataManager
{
    @Mock private PersistentDataStore m_PersistentDataStore;
    @Mock private PersistentData m_PersistentData;
    
    private StreamProfileFactoryObjectDataManagerImpl m_SUT;
    private UUID m_UUID = UUID.randomUUID();
    private URI m_StreamPort;
    private FactoryObjectData m_FactData;
        
    @Before
    public void setUp() throws URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        m_StreamPort = new URI("//226.1.2.3:20000");
        
        m_SUT = new StreamProfileFactoryObjectDataManagerImpl();
        m_SUT.setPersistentDataStore(m_PersistentDataStore);        
    }
    
    @Test
    public void testGetServiceObjectType()
    {
        assertThat(m_SUT.getServiceObjectType().getName(), is(StreamProfile.class.getName()));
    }
    
    @Test
    public void testActivate()
    {
        m_SUT.activate();
        assertThat(m_SUT.getRegistry().findExtensionByName(StreamProfileObjectData.streamPort.getDescriptor()
                .getFullName()), is(notNullValue()));
    }
    
    @Test
    public void testGetStreamPort() throws Exception
    {
        m_SUT.activate(); 
        
        m_FactData = FactoryObjectData.newBuilder().
                setName("factObjData").
                setExtension(StreamProfileObjectData.streamPort, m_StreamPort.toString()).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        URI streamPort = m_SUT.getStreamPort(m_UUID);

        assertThat(streamPort, is(m_StreamPort));       
    }
    
    @Test
    public void testSetStreamPort() throws Exception
    {
        m_SUT.activate();
        
        m_FactData = FactoryObjectData.newBuilder().
                setName("factObjData").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        m_SUT.setStreamPort(m_UUID, m_StreamPort);
        
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        String streamPortAsString = captMessage.getExtension(StreamProfileObjectData.streamPort);
        assertThat(streamPortAsString, is(m_StreamPort.toString()));       
    }
}
