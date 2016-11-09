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
package mil.dod.th.ose.core.impl.asset.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.AssetObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.ElevationDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HeadingDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test the asset implementation of the
 * {@link mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager}.
 * @author callen
 *
 */
public class TestAssetFactoryObjectDataManagerImpl
{
    private AssetFactoryObjectDataManagerImpl m_SUT;
    private PersistentDataStore m_PersistentDataStore;
    private PersistentData m_PersistentData;
    private FactoryObjectData m_FactData;
    private JaxbProtoObjectConverter m_JaxbConverter;
    private UUID m_UUID = UUID.randomUUID();
    private Coordinates m_Coordinates;
    private SpatialTypesGen.Coordinates m_ProtoCoors;
    private Orientation m_Orientation;
    private SpatialTypesGen.Orientation m_ProtoOrien;
    
    @Before
    public void setUp() throws ObjectConverterException
    {
        m_SUT = new AssetFactoryObjectDataManagerImpl();
        m_PersistentDataStore = mock(PersistentDataStore.class);
        m_PersistentData = mock(PersistentData.class);
        m_JaxbConverter = mock(JaxbProtoObjectConverter.class);
        
        m_Coordinates = new Coordinates();
        m_Orientation = new Orientation();
        
        m_ProtoCoors = SpatialTypesGen.Coordinates.newBuilder().
                setLatitude(LatitudeWgsDegrees.newBuilder().setValue(23).build()).
                setLongitude(LongitudeWgsDegrees.newBuilder().setValue(40).build())
                                .build();
        m_ProtoOrien = SpatialTypesGen.Orientation.newBuilder().
                setElevation(ElevationDegrees.newBuilder().setValue(19).build()).
                setHeading(HeadingDegrees.newBuilder().setValue(21).build()).build();
    
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        m_SUT.setJaxbProtoObjectConverter(m_JaxbConverter);
    }
    
    /**
     * Verify activate creates registry.
     */
    @Test
    public void testActivate()
    {
        m_SUT.activate();
        
        //per documentation this will return null if the extension is not found
        assertThat(
            m_SUT.getRegistry().findExtensionByName(AssetObjectData.coordinates.getDescriptor().getFullName()), 
                is(notNullValue()));
    }
    
    /**
     * Verify factory object service type is asset.
     */
    @Test
    public void testGetServiceObjectType()
    {
        assertThat(m_SUT.getServiceObjectType().getName(), is(Asset.class.getName()));
    }
    
    /**
     * Verify setting of coordinates for an asset.
     */
    @Test
    public void testSetCoordinates() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Coordinates)).thenReturn(m_ProtoCoors);
        
        m_SUT.setCoordinates(m_UUID, m_Coordinates);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        SpatialTypesGen.Coordinates coordCapt = captMessage.getExtension(AssetObjectData.coordinates);
        assertThat(coordCapt, is(m_ProtoCoors));
    }
    
    /**
     * Verify setting of orientation for an asset.
     * Verify exception if orientation cannot be converted.
     */
    @Test
    public void testSetOrientationException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Orientation)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.setOrientation(m_UUID, m_Orientation);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify setting of orientation for an asset.
     */
    @Test
    public void testSetOrientation() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Orientation)).thenReturn(m_ProtoOrien);
        
        m_SUT.setOrientation(m_UUID, m_Orientation);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        SpatialTypesGen.Orientation orienCapt = captMessage.getExtension(AssetObjectData.orientation);
        assertThat(orienCapt, is(m_ProtoOrien));
    }
    
    /**
     * Verify setting of coordinates for an asset.
     * Verify exception if coordinates cannot be converted.
     */
    @Test
    public void testSetCoordinatesException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Coordinates)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.setCoordinates(m_UUID, m_Coordinates);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }
     
    /**
     * Verify getting of coordinates for an asset.
     */
    @Test
    public void testGetCoordinates() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.coordinates, m_ProtoCoors).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors)).thenReturn(m_Coordinates);
        
        Coordinates newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(m_Coordinates));
    }
    
    /**
     * Verify getting of orientation for an asset.
     */
    @Test
    public void testGetOrientation() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.orientation, m_ProtoOrien).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien)).thenReturn(m_Orientation);
        
        Orientation newOrien = m_SUT.getOrientation(m_UUID);
        
        //verify orientation is returned
        assertThat(newOrien, is(m_Orientation));
    }
    
    /**
     * Verify getting of orientation for an asset.
     * Verify exception if the orientation cannot be converted.
     */
    @Test
    public void testGetOrientationException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.orientation, m_ProtoOrien).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.getOrientation(m_UUID);
            fail("Expected exception from the conversion of the orientation");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected an exception
        }
    }
    
    /**
     * Verify getting of coordinates for an asset.
     * Verify exception if coordinates cannot be converted.
     */
    @Test
    public void testGetCoordinatesException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.coordinates, m_ProtoCoors).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors)).
            thenThrow(new ObjectConverterException("Converter fail"));
        
        try
        {
            m_SUT.getCoordinates(m_UUID);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify getting of coordinates for an asset.
     */
    @Test
    public void testGetCoordinatesNoData() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(null);
        
        Coordinates newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(nullValue()));
    }
    
    /**
     * Verify getting of orientation for an asset when there is not data information.
     */
    @Test
    public void testGetOrientationNoData() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(null);
        
        Orientation newOrien = m_SUT.getOrientation(m_UUID);
        
        //verify
        assertThat(newOrien, is(nullValue()));
    }
    
    /**
     * Verify getting the coordinates for an asset when there is no coordinate information.
     */
    @Test
    public void testGetCoordinatesNoCords() throws FactoryObjectInformationException
    {        
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        Coordinates newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(nullValue()));
    }
    
    /**
     * Verify getting the orientation for an asset when there is no orientation information.
     */
    @Test
    public void testGetOrientationsNoOrient() throws FactoryObjectInformationException
    {        
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        Orientation newOrient = m_SUT.getOrientation(m_UUID);
        
        //verify
        assertThat(newOrient, is(nullValue()));
    }
}
