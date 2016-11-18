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
package mil.dod.th.ose.core.impl.ccomm.physical;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLinkAttributes;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkProxy;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;

/**
 * Basic physical link implementation.
 * @author allenchl
 *
 */
@Component(factory = PhysicalLinkInternal.COMPONENT_FACTORY_REG_ID)
public class PhysicalLinkImpl extends AbstractFactoryObject implements PhysicalLinkInternal
{
    /**
     * Boolean to hold if the {@link mil.dod.th.core.ccomm.physical.PhysicalLink} is in use.
     */
    private boolean m_InUse;

    /**
     * Owner of this physical link.
     */
    private LinkLayer m_Owner;
    
    /**
     * Physical link proxy.
     */
    private PhysicalLinkProxy m_PhysProxy;

    /**
     * Reference to the logging service.
     */
    private LoggingService m_Log;

    /**
     * Reference to internal power management for factory objects.
     */
    private PowerManagerInternal m_PowInternal;

    /**
     * Wake lock used for link layer operations.
     */
    private WakeLock m_WakeLock;

    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, //NOPMD:
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin,
            final PowerManagerInternal powInternal, final UUID uuid, final String name, 
            final String pid, final String baseType) throws IllegalStateException
            // ExcessiveParameterList, needed to provide this class with access to necessary services.
    {
        super.initialize(registry, proxy, factory, configAdmin, eventAdmin, powInternal, uuid, name, pid, baseType);
        m_PhysProxy = (PhysicalLinkProxy)proxy;
        m_PowInternal = powInternal;
        m_WakeLock = powInternal.createWakeLock(m_PhysProxy.getClass(), this, "corePhyLink");
    }
    
    @Override
    public void open() throws PhysicalLinkException
    {
        try
        {
            m_WakeLock.activate();

            m_PhysProxy.open();
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public void close() throws PhysicalLinkException
    {
        try
        {
            m_WakeLock.activate();

            m_PhysProxy.close();
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public boolean isInUse()
    {
        return m_InUse;
    }

    @Override
    public LinkLayer getOwner()
    {
        return m_Owner;
    }

    @Override
    public boolean isOpen()
    {
        return m_PhysProxy.isOpen();
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        return m_PhysProxy.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        return m_PhysProxy.getOutputStream();
    }

    @Override
    public void setReadTimeout(final int timeoutMS) throws IllegalArgumentException, FactoryException
    {
        final Map<String, Object> currentProps = getProperties();
        currentProps.put(PhysicalLinkAttributes.CONFIG_PROP_READ_TIMEOUT_MS, timeoutMS);
        setProperties(currentProps);       
    }

    @Override
    public void setDataBits(final int bits) throws IllegalArgumentException, FactoryException
    {
        if (bits < 1)
        {
            throw new IllegalArgumentException("Data bits value cannot be less than 1.");
        }

        final Map<String, Object> currentProps = getProperties();
        currentProps.put(PhysicalLinkAttributes.CONFIG_PROP_DATA_BITS, bits);
        setProperties(currentProps);
    }

    @Override
    public void setInUse(final boolean flag)
    {
        m_InUse = flag;
        
        m_Log.debug("Physical link [%s] in use? %b", getName(), m_InUse);
    }

    @Override
    public void setOwner(final LinkLayer linkLayer)
    {
        m_Owner = linkLayer;
    }
    
    @Override
    public PhysicalLinkAttributes getConfig()
    {
        return Configurable.createConfigurable(PhysicalLinkAttributes.class, getProperties());
    }

    @Override
    public void release() throws IllegalStateException
    {
        if (isOpen())
        {
            throw new IllegalStateException(String.format("Unable to release physical link [%s], it is open.", 
                    getName()));
        }
        else
        {
            if (isInUse())
            {
                setInUse(false);
                setOwner(null);
            }
            else
            {
                throw new IllegalStateException(String.format("Unable to release physical link [%s], it is not in use.",
                        getName()));
            }
        }
    }
    
    @Override
    public void delete() throws IllegalStateException
    {
        if (isInUse())
        {
            throw new IllegalStateException(String.format("Physical Link [%s] is in use, cannot delete.", getName()));
        }

        if (isOpen())
        {
            throw new IllegalStateException(String.format("Physical Link [%s] is open, cannot delete.", getName()));
        }

        m_PowInternal.deleteWakeLock(m_WakeLock);

        super.delete();
    }
}
