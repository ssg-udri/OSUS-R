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

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.ExtensionRegistry;

import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.StreamProfileObjectData;
import mil.dod.th.ose.utils.CoverageIgnore;

/**
 * StreamProfile-specific implementation of {@link mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager}.
 * 
 * @author jmiller
 *
 */
@Component
public class StreamProfileFactoryObjectDataManagerImpl extends BaseFactoryObjectDataManager implements
        StreamProfileFactoryObjectDataManager
{
    
    /**
     * Extension registry for StreamProfile-specific information.
     */
    private ExtensionRegistry m_Registry;
    
    @Override
    @Reference
    @CoverageIgnore
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        //set in the super class
        super.setPersistentDataStore(persistentDataStore);
    }
    
    /**
     * Activate the component.
     */
    @Activate
    public void activate()
    {
        m_Registry = ExtensionRegistry.newInstance();
        m_Registry.add(StreamProfileObjectData.streamPort);
    }
    
    @Override
    public URI getStreamPort(final UUID uuid) throws FactoryObjectInformationException
    {
        final FactoryObjectData factData = tryGetMessage(uuid);
        if (factData != null && factData.hasExtension(StreamProfileObjectData.streamPort))
        {

            try
            {
                return new URI(factData.getExtension(StreamProfileObjectData.streamPort));
            }
            catch (final URISyntaxException e)
            {
                throw new FactoryObjectInformationException(
                        String.format("Stream port information for stream profile with UUID %s is not covertible.",
                                uuid.toString()), e);
            }

        }
        return null;
    }
    
    @Override
    public void setStreamPort(final UUID uuid, final URI streamPort) throws FactoryObjectInformationException
    {
        //persist new stream port
        final FactoryObjectData factDataMessage = getMessage(uuid);
        final FactoryObjectData.Builder updated = factDataMessage.toBuilder();

        updated.setExtension(StreamProfileObjectData.streamPort, streamPort.toString());
        
        //merge the updated entity
        mergeEntity(uuid, updated);

    }

    @Override
    protected Class<? extends FactoryObject> getServiceObjectType()
    {
        return StreamProfile.class;
    }
    
    @Override
    public ExtensionRegistry getRegistry()
    {
        return m_Registry;
    }
    
}
