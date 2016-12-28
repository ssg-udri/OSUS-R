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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.ExtensionRegistry;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.AssetObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.CoordinatesEntry;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.OrientationEntry;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen;

/**
 * Asset specific implementation of the {@link BaseFactoryObjectDataManager}. This implementation includes support
 * for being able to persist asset specific information like the asset's location.
 * 
 * @author callen
 */
@Component
public class AssetFactoryObjectDataManagerImpl extends BaseFactoryObjectDataManager implements
        AssetFactoryObjectDataManager
{
    /**
     * Lexicon converter as data is persisted using the protocol buffer object.
     */
    private JaxbProtoObjectConverter m_JaxbProtoObjectConverter;
    
    /**
     * Extension registry for asset specific information.
     */
    private ExtensionRegistry m_Registry;
    
    @Override
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        //set in the super class
        super.setPersistentDataStore(persistentDataStore);
    }
    
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter jaxbProtoObjectConverter)
    {
        m_JaxbProtoObjectConverter = jaxbProtoObjectConverter;
    }

    /**
     * Activate the component.
     */
    @Activate
    public void activate()
    {
        m_Registry = ExtensionRegistry.newInstance();
        m_Registry.add(AssetObjectData.coordinates);
        m_Registry.add(AssetObjectData.orientation);
    }

    @Override
    public Map<String, Coordinates> getCoordinates(final UUID uuid) throws FactoryObjectInformationException
    {
        final FactoryObjectData factData = tryGetMessage(uuid);
        if (factData != null)
        {
            try
            {
                final List<CoordinatesEntry> coords = factData.getExtension(AssetObjectData.coordinates);

                final Map<String, Coordinates> coordsMap = new HashMap<>();
                for (CoordinatesEntry entry : coords)
                {
                    coordsMap.put(entry.getKey().isEmpty() ? null : entry.getKey(),
                            (Coordinates) m_JaxbProtoObjectConverter.convertToJaxb(entry.getValue()));
                }
                return coordsMap;
            }
            catch (final ObjectConverterException e)
            {
                throw new FactoryObjectInformationException(
                    String.format("Coordinate information for asset with UUID %s is not convertible.", 
                            uuid.toString()), e);
            }
        }
        return null;
    }

    @Override
    public Map<String, Orientation> getOrientation(final UUID uuid) throws FactoryObjectInformationException
    {
        final FactoryObjectData factData = tryGetMessage(uuid);
        
        if (factData != null)
        {
            try
            {
                final List<OrientationEntry> orientations = factData.getExtension(AssetObjectData.orientation);

                final Map<String, Orientation> orienMap = new HashMap<>();
                for (OrientationEntry entry : orientations)
                {
                    orienMap.put(entry.getKey().isEmpty() ? null : entry.getKey(),
                        (Orientation) m_JaxbProtoObjectConverter.convertToJaxb(entry.getValue()));
                }
                return orienMap;
            }
            catch (final ObjectConverterException e)
            {
                throw new FactoryObjectInformationException(
                    String.format("Orienation information for asset with UUID %s is not convertable.", 
                           uuid.toString()), e);
            }
        }
        return null;
    }

    @Override
    public void setCoordinates(final UUID uuid, final Map<String, Coordinates> coordsMap)
            throws FactoryObjectInformationException
    {
        //persist new coords
        final FactoryObjectData factDataMessage = getMessage(uuid);
        final FactoryObjectData.Builder updated = factDataMessage.toBuilder();
        try
        {
            final List<CoordinatesEntry> coords = new ArrayList<>();
            for (String key : coordsMap.keySet())
            {
                final CoordinatesEntry.Builder entry = CoordinatesEntry.newBuilder();
                if (key != null)
                {
                    entry.setKey(key);
                }

                entry.setValue(
                    (SpatialTypesGen.Coordinates) m_JaxbProtoObjectConverter.convertToProto(coordsMap.get(key)));

                coords.add(entry.build());
            }

            updated.setExtension(AssetObjectData.coordinates, coords);
        }
        catch (final ObjectConverterException e)
        {
            throw new FactoryObjectInformationException(String.format(
                    "Unable to update coordinates for asset with UUID %s, because the coordinates"
                            + " could not be converted to a persistable entity.", uuid.toString()), e); // NOCHECKSTYLE:
            // this error message is similar to another for consistency of information being portrayed
        }
        //merge the updated entity
        mergeEntity(uuid, updated);
    }

    @Override
    public void setOrientation(final UUID uuid, final Map<String, Orientation> orienMap)
            throws FactoryObjectInformationException
    {
        //persist new orientation
        final FactoryObjectData factDataMessage = getMessage(uuid);
        final FactoryObjectData.Builder updated = factDataMessage.toBuilder();
        try
        {
            final List<OrientationEntry> oriens = new ArrayList<>();
            for (String key : orienMap.keySet())
            {
                final OrientationEntry.Builder entry = OrientationEntry.newBuilder();
                if (key != null)
                {
                    entry.setKey(key);
                }

                entry.setValue(
                    (SpatialTypesGen.Orientation)m_JaxbProtoObjectConverter.convertToProto(orienMap.get(key)));

                oriens.add(entry.build());
            }

            updated.setExtension(AssetObjectData.orientation, oriens);
        }
        catch (final ObjectConverterException e)
        {
            throw new FactoryObjectInformationException(
                    String.format("Unable to update orientation for asset with UUID %s, because the orientation"
                        + " could not be converted to a persistable entity.", uuid.toString()), e);
        }
        //merge the updated entity
        mergeEntity(uuid, updated);
    }

    @Override
    protected Class<? extends FactoryObject> getServiceObjectType()
    {
        return Asset.class;
    }

    @Override
    public ExtensionRegistry getRegistry()
    {
        return m_Registry;
    }
}
