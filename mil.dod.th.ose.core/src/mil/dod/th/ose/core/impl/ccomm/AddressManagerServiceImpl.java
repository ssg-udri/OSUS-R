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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.AddressTranslator;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.utils.SingleComponent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * This interface serves as an Address Manager Service. Addresses may be looked up using specific parameters.
 * If an address meeting that specification is not found, a new one is created. 
 */
@Component
public class AddressManagerServiceImpl extends DirectoryService implements AddressManagerService
{
    /**
     * Factory service context.
     */
    private FactoryServiceContext<AddressInternal> m_FactoryServiceContext; 
    
    /**
     * Reference to the service proxy for addresses.
     */
    private FactoryServiceProxy<AddressInternal> m_FactoryServiceProxy; 
    
    /**
     * Reference to the {@link AddressTranslatorManager}.
     */
    private AddressTranslatorManager m_TranslatorManager;

    /**
     * Component wrapper for the {@link FactoryServiceContext} instance.
     */
    private SingleComponent<FactoryServiceContext<AddressInternal>> m_FactServiceContextComp;

    /**
     * Wake lock used for address manager service operations.
     */
    private WakeLock m_WakeLock;

    /**
     * Bind the factory for creating {@link FactoryServiceContext} instances.
     * 
     * @param serviceContextFactory
     *      factory that creates {@link FactoryServiceContext} instances
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryServiceContext.FACTORY_NAME + ")")
    public void setFactoryServiceContextFactory(final ComponentFactory serviceContextFactory)
    {
        m_FactServiceContextComp = new SingleComponent<FactoryServiceContext<AddressInternal>>(serviceContextFactory);
    }

    /**
     * Bind the translator manager.
     * @param manager
     *      the translator manager to use
     */
    @Reference
    public void setAddressTranslatorManager(final AddressTranslatorManager manager)
    {
        m_TranslatorManager = manager;
    }
    
    /**
     * Activate the Address Manager Service component.
     * 
     * @param context
     *      context for the bundle containing this component
     * @throws InvalidSyntaxException
     *      if {@link FactoryServiceContext} provides an invalid filter
     */
    @Activate
    public void activate(final BundleContext context) throws InvalidSyntaxException
    {
        m_FactoryServiceContext = m_FactServiceContextComp.newInstance(null);
        m_FactoryServiceContext.initialize(context, m_FactoryServiceProxy, this);
        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreAddrManagerService");
    }
    
    /**
     * Deactivate the component by disposing of created component factory instances.
     */
    @Deactivate
    public void deactivate()
    {
        m_FactServiceContextComp.tryDispose();
        m_WakeLock.delete();
    }

    @Override
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        super.setLoggingService(logging);
    }

    @Override
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }

    @Override
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Bind the service. Each service type will provide a service proxy, restrict to the address one.
     * 
     * @param factoryServiceProxy
     *      service to bind
     */
    @Reference(target = "(" + AddressInternal.SERVICE_TYPE_PAIR + ")")
    public void setFactoryServiceProxy(final FactoryServiceProxy<AddressInternal> factoryServiceProxy)
    {
        m_FactoryServiceProxy = factoryServiceProxy;
    }

    @Override
    public Address getOrCreateAddress(final String addressDescription) throws CCommException
    {
        return getOrCreateAddress(addressDescription, null);
    }
    
    @Override
    public Address getOrCreateAddress(final String addressDescription, final String name) throws CCommException
    {   
        final AddressFactory factory = getFactoryByString(addressDescription);
        
        // Check to make sure a factory was found
        if (null == factory)
        {
            throw new CCommException("No factory exists for description: " + addressDescription, FormatProblem.OTHER);
        }
        final AddressTranslator translator = m_TranslatorManager.getAddressTranslator(factory.getProductType());
        final String[] descriptionSplit = splitDescription(addressDescription);
        final Map<String, Object> properties = translator.getAddressPropsFromString(descriptionSplit[1]);
        return this.getOrCreateAddress(factory.getProductType(), name, properties);
    }

    @Override
    public boolean checkAddressAlreadyExists(final String addressDescription)
    {
        try
        {
            final AddressFactory factory = getFactoryByString(addressDescription);
            
            if (null == factory)
            {
                return false;
            }
            
            final String[] descriptionSplit = splitDescription(addressDescription);
            final AddressTranslator translator = m_TranslatorManager.getAddressTranslator(factory.getProductType());
            
            final Map<String, Object> properties =
                    translator.getAddressPropsFromString(descriptionSplit[1]);

            for (Address tempAddress : m_FactoryServiceContext.getRegistry().
                    getObjectsByProductType(factory.getProductType()))
            {
                // Results for equalProperties different from equals. equalProperties is correct here.
                if (tempAddress.equalProperties(properties))
                {
                    return true;
                }
            }
            return false;
        }
        catch (final CCommException e)
        {
            return false;
        }
    }

    @Override
    public void flush()
    {
        m_Logging.info("Flushing all entries from AddressManagerService.");
        for (Address address : m_FactoryServiceContext.getRegistry().getObjects())
        {
            try
            {
                address.delete();
            }
            catch (final Exception e)
            {
                m_Logging.warning(e, "Unable to remove an address [%s] while flushing", address);
            }
        }
    }

    @Override
    public List<String> getAddressDescriptiveStrings()
    {
        final List<String> descriptors  = new ArrayList<String>();
        for (AddressInternal address : m_FactoryServiceContext.getRegistry().getObjects())
        {
            final String messageAddress;
            try
            {
                messageAddress = address.getDescription();
            }
            catch (final Exception e)
            {
                m_Logging.warning(e, "Unable to get address description of type [%s]", 
                        address.getFactory().getProductType());
                continue;
            }

            descriptors.add(messageAddress);
        }

        return descriptors;
    }
    
    @Override
    public void printDeep(final PrintStream printStream)
    {
        printStream.println("****************Begin print deep****************\n");

        for (Address address : m_FactoryServiceContext.getRegistry().getObjects())
        {
            printStream.format("%s: %s (%s)%n", address.getFactory().getProductType(),
                    // must call toString() here, otherwise would be called in format() and cause deadlock
                    address.toString(), 
                    address.getName());
        }

        printStream.println("\n****************End print deep****************");
    }
    
    @Override
    public Address getOrCreateAddress(final String addressType, final String name, final Map<String, Object> properties)
            throws CCommException
    {   
        Preconditions.checkNotNull(properties);

        // check to see if the address is currently in the list of known addresses
        // TD: should probably store of map of addresses by descriptive string or something as this method could be
        // called often
        for (Address aAddress : m_FactoryServiceContext.getRegistry().getObjectsByProductType(addressType))
        {
            if (aAddress.equalProperties(properties))
            {
                return aAddress;
            }
        }

        // not in list, add to listing
        final FactoryInternal factory = getFactoryByType(addressType);
        m_Logging.log(LogService.LOG_INFO, "Creating an Address of type [%s] (name='%s').", 
                factory.getProductType(), name);
        try
        {
            m_WakeLock.activate();

            return m_FactoryServiceContext.getRegistry().createNewObject(factory, name, properties);
        }
        catch (final Exception ex)
        {
            throw new CCommException("Error creating new Address from getOrCreateAddress.", ex, FormatProblem.OTHER);
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public Address getAddressByName(final String name) throws IllegalArgumentException
    {
        for (Address aAddress : m_FactoryServiceContext.getRegistry().getObjects())
        {
            if (aAddress.getName().equals(name))
            {
                return aAddress;
            }
        }

        throw new IllegalArgumentException(String.format("Address with name %s not found", name));
    }

    @Override
    public List<String> getAddressNames()
    {
        final List<String> names  = new ArrayList<>();
        for (Address address : m_FactoryServiceContext.getRegistry().getObjects())
        {
            names.add(address.getName());
        }

        return names;
    }

    @Override
    public Map<String, String> getAddressNamesWithDescriptor()
    {
        final Map<String, String> namesWithDescMap = new HashMap<>();
        for (Address address : m_FactoryServiceContext.getRegistry().getObjects())
        {
            String messageAddress;
            try
            {
                messageAddress = address.getDescription();
            }
            catch (final Exception e)
            {
                messageAddress = null;
            }

            namesWithDescMap.put(address.getName(), messageAddress);
        }

        return namesWithDescMap;
    }

    /**
     * Split the address string into a prefix and everything else.
     * @param addressDescription
     *          the address that needs to be split
     * @return
     *          the string, split into two parts
     * @throws IllegalArgumentException
     *         if the address description is null or incomplete
     */
    private String[] splitDescription(final String addressDescription) throws IllegalArgumentException
    {
        Preconditions.checkNotNull(addressDescription);
        
        final String splitter = ":";
        
        final String[] addressParts = addressDescription.split(splitter, 2);

        final String errorMessage = String.format(
                "The address description [%s] is invalid as it does not contain a prefix and a suffix",
                addressDescription);
        
        if (addressParts.length != 2)
        {
            throw new IllegalArgumentException(errorMessage);
        }
        
        final String prefix = addressParts[0];
        final String suffix = addressParts[1];
        
        if (prefix.isEmpty() || suffix.isEmpty())
        {
            throw new IllegalArgumentException(errorMessage);
        }
        
        return addressParts;
    }
    
    /**
     * Get an {@link AddressFactory} by its {@link AddressFactory#getProductType()}. Throw an exception if no factory
     * is found.
     * 
     * @param addressType
     *      type of address product type
     * @return
     *      factory that produces the given address type
     * @throws CCommException
     *      if the given type is not known by the manager service 
     */
    private FactoryInternal getFactoryByType(final String addressType) throws CCommException
    {
        final FactoryInternal factory = m_FactoryServiceContext.getFactories().get(addressType);
        
        if (factory == null)
        {
            throw new CCommException("Unable to find requested Address type: " + addressType, FormatProblem.OTHER);
        }
        
        return factory;
    }
    
    /**
     * Get an {@link AddressFactory} by its {@link mil.dod.th.core.ccomm.capability.AddressCapabilities#getPrefix()}.
     * Return null if no factory is found.
     * 
     * @param addressDescription
     *      description of the address - may not be null
     * @return factory that produces the given address type or null
     * @throws IllegalArgumentException
     *      if the address description is incomplete
     */
    private AddressFactory getFactoryByString(final String addressDescription) throws IllegalArgumentException
    {
        final String[] addrDescAfterSplit = splitDescription(addressDescription);
        final String prefix = addrDescAfterSplit[0];
        return getFactoryByPrefix(prefix);
    }

    /**
     * Get a factory by prefix.
     * @param prefix
     *     the address factory's prefix to search for
     * @return
     *     the factory with the matching prefix or null if not found
     */
    AddressFactory getFactoryByPrefix(final String prefix)
    {
        final Map<String, FactoryInternal> factories = m_FactoryServiceContext.getFactories();
        
        synchronized (factories)
        {
            for (FactoryInternal factory : factories.values())
            {
                if (factory.getAddressCapabilities().getPrefix().equals(prefix))
                {
                    return factory;
                }
            }
        }
        
        return null;
    }
}
