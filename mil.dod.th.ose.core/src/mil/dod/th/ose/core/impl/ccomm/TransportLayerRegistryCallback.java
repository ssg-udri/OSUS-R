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
package mil.dod.th.ose.core.impl.ccomm;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.RegistryDependency;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;

import org.osgi.service.cm.ConfigurationException;

/**
 * Callback registry implementation for {@link mil.dod.th.core.ccomm.transport.TransportLayer}s.
 * @author allenchl
 *
 */
public class TransportLayerRegistryCallback implements FactoryRegistryCallback<TransportLayerInternal>
{
    /**
     * Reference to comms service to acquire layers.
     */
    private final CustomCommsService m_CcomsService;
    
    /**
     * Create instance by passing comms service.
     * @param cComms
     *      service used to acquire layers through
     */
    public TransportLayerRegistryCallback(final CustomCommsService cComms)
    {
        m_CcomsService = cComms;
    }
    
    @Override
    public void preObjectInitialize(final TransportLayerInternal object) throws FactoryException,
            ConfigurationException
    {
        checkLinkLayer(object);
    }

    @Override
    public void postObjectInitialize(final TransportLayerInternal object)
    {
        // nothing to do after initializing the proxy
    }
    
    @Override
    public void preObjectUpdated(final TransportLayerInternal object) throws FactoryException
    {
        checkLinkLayer(object);
    }

    @Override
    public void onRemovedObject(final TransportLayerInternal object)
    {
        object.shutdown();
    }
    
    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        final List<RegistryDependency> listRegDep = new ArrayList<>();
        
        listRegDep.add(new RegistryDependency(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME, false)
        {
            @Override
            public Object findDependency(final String objectName)
            {
                return m_CcomsService.findLinkLayer(objectName);
            }
        });
        
        return listRegDep;
    }
    
    /** 
     * Check the link layer property and request the necessary object.
     * 
     * @param object
     *      object to check
     * @throws FactoryException
     *      if there is an error in accessing the link layer
     */
    private void checkLinkLayer(final TransportLayerInternal object) throws FactoryException
    {
        //will throw an exception if the property is not found
        final String linkLayerName = object.getConfig().linkLayerName();
        
        //if the name is not empty try to find the Link Layer
        if (!Strings.isNullOrEmpty(linkLayerName))
        {
            final LinkLayer linkLayer = m_CcomsService.findLinkLayer(linkLayerName);
            if (linkLayer == null)
            {
                throw new FactoryException(String.format("Link Layer [%s] was unable to be fetched.", 
                        linkLayerName));
            }
            else
            {
                object.setLinkLayer(linkLayer);
            }
        }
    }
}
