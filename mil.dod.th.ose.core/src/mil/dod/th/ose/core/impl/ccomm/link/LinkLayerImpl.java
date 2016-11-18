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
package mil.dod.th.ose.core.impl.ccomm.link;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.base.Preconditions;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * Basic implementation of a link layer.
 * @author allenchl
 *
 */
@Component(factory = LinkLayerInternal.COMPONENT_FACTORY_REG_ID)
public class LinkLayerImpl extends AbstractFactoryObject implements LinkLayerInternal
{
    /**
     * Flag denoting whether this layer is activated or not.
     */
    private boolean m_IsActivated;
    
    /**
     * This variable holds the PhysicalLink object referenced to this LinkLayer.
     */
    private PhysicalLink m_PhysicalLink;
    
    /**
     * True if the link layer is performing the built-in test (BIT).
     */
    private Boolean m_IsPerformingBIT = false;

    /**
     * The current status of the link layer.
     */
    private LinkStatus m_Status = LinkStatus.LOST;
    
    /**
     * Internal layer representation of the link layer.
     */
    private LinkLayerProxy m_LinkProxy;
    
    /**
     * Reference to internal power management for factory objects.
     */
    private PowerManagerInternal m_PowInternal;

    /**
     * Wake lock used for link layer operations.
     */
    private WakeLock m_WakeLock;

    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, //NOPMD:
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin,
            final PowerManagerInternal powInternal, final UUID uuid, final String name, 
            final String pid, final String baseType) throws IllegalStateException
            // ExcessiveParameterList, needed to provide this class with access to necessary services.
    {
        super.initialize(registry, proxy, factory, configAdmin, eventAdmin, powInternal, uuid, name, pid, baseType);
        m_LinkProxy = (LinkLayerProxy)proxy;
        m_PowInternal = powInternal;
        m_WakeLock = powInternal.createWakeLock(m_LinkProxy.getClass(), this, "coreLinkLayer");
    }
    
    @Override
    public void activateLayer()
    {
        try
        {
            m_WakeLock.activate();

            m_LinkProxy.onActivate();
            m_IsActivated = true;
            postEvent(TOPIC_ACTIVATED, new HashMap<String, Object>());
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public void deactivateLayer()
    {
        try
        {
            m_WakeLock.activate();

            m_LinkProxy.onDeactivate();
            m_IsActivated = false;
            postEvent(TOPIC_DEACTIVATED, new HashMap<String, Object>());
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public PhysicalLink getPhysicalLink()
    {
        return m_PhysicalLink;
    }

    @Override
    public LinkStatus getLinkStatus()
    {
        return m_Status;
    }

    @Override
    public boolean isAvailable(final Address address)
    {
        return m_LinkProxy.isAvailable(address);
    }

    @Override
    public boolean isActivated()
    {
        return m_IsActivated;
    }

    @Override
    public boolean isPerformingBit()
    {
        return m_IsPerformingBIT;
    }

    @Override
    public int send(final LinkFrame frame, final Address addr) throws CCommException
    {
        Preconditions.checkNotNull(frame, "Cannot send a [Null] frame.");
        
        if (!isActivated())
        {
            throw new CCommException(String.format(
                "LinkLayer [%s] is not activated. Unable to send frame.", getName()), 
                    FormatProblem.INACTIVE);
        }

        try
        {
            m_WakeLock.activate();

            final int toReturn = m_LinkProxy.send(frame, addr);
        
            final Map<String, Object> props = new HashMap<String, Object>();
            if (addr != null)
            {
                props.putAll(addr.getEventProperties(Address.EVENT_PROP_DEST_ADDRESS_PREFIX));
            }
            props.put(LinkLayer.EVENT_PROP_LINK_FRAME, frame);
            postEvent(LinkLayer.TOPIC_DATA_SENT, props);
            
            return toReturn;
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public LinkStatus performBit() throws CCommException
    {
        if (!getFactory().getLinkLayerCapabilities().isPerformBITSupported())
        {
            throw new CCommException(String.format("Link layer [%s] with UUID [%s] does not have a built-in-test",
                getName(), getUuid()), FormatProblem.OTHER);
        }
        
        m_IsPerformingBIT = true;

        try
        {
            m_WakeLock.activate();

            final LinkStatus status = m_LinkProxy.onPerformBit();
            Logging.log(LogService.LOG_DEBUG, "Performed BIT for link layer %s. Status: %s", getName(), status);

            setStatus(status);
        }
        catch (final CCommException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Failed to perform BIT for: %s", getName());

            setStatus(LinkStatus.LOST);
        }
        finally
        {
            m_WakeLock.cancel();
        }

        m_IsPerformingBIT = false;
        return getLinkStatus();
    }

    @Override
    public int getMtu()
    {
        final LinkLayerCapabilities caps = getFactory().getLinkLayerCapabilities();
        if (caps.isStaticMtu())
        {
            return caps.getMtu();
        }
        else
        {
            return m_LinkProxy.getDynamicMtu();
        }
    }

    @Override
    public LinkLayerAttributes getConfig() 
    {
        return Configurable.createConfigurable(LinkLayerAttributes.class, getProperties());
    };
    
    @Override
    public void setStatus(final LinkStatus status)
    {
        m_Status = status;
        
        Logging.log(LogService.LOG_INFO, "New status for [%s]: %s", getName(), m_Status);
        
        postEvent(TOPIC_STATUS_CHANGED, new HashMap<String, Object>());
    }

    @Override
    public void postReceiveEvent(final Address sourceAddress, final Address destAddress, final LinkFrame frame)
    {
        Preconditions.checkNotNull(frame, "The received LinkFrame is null; this is not allowed.");
        
        final Map<String, Object> props = new HashMap<String, Object>();
        if (sourceAddress != null)
        {
            props.putAll(sourceAddress.getEventProperties(Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX));
        }
        if (destAddress != null)
        {
            props.putAll(destAddress.getEventProperties(Address.EVENT_PROP_DEST_ADDRESS_PREFIX));
        }
        props.put(LinkLayer.EVENT_PROP_LINK_FRAME, frame);
        
        postEvent(TOPIC_DATA_RECEIVED, props);
    }

    @Override
    public void setPhysicalLink(final PhysicalLink physicalLink)
    {
        m_PhysicalLink = physicalLink;
    }
    
    @Override
    public void postEvent(final String topic, final Map<String, Object> props)
    {
        //add link status
        props.put(LinkLayer.EVENT_PROP_LINK_STATUS, getLinkStatus());
        
        //post
        super.postEvent(topic, props);
    }
    
    @Override
    public void delete() throws IllegalStateException
    {
        if (isActivated())
        {
            throw new IllegalStateException(String.format("Link Layer is activated, cannot remove Link Layer [%s]", 
                    getName()));
        }
 
        m_PowInternal.deleteWakeLock(m_WakeLock);

        super.delete();
    }
}
