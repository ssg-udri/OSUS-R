package ${package};

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.${proxyType}Proxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * ${description} implementation.
 * 
 * @author ${author}
 */
@Component(factory = PhysicalLink.FACTORY)
public class ${class} implements ${proxyType}Proxy
{
    /**
    * Reference to the context which provides this class with methods to interact with the rest of the system.
    */
    private PhysicalLinkContext m_Context;
    
    @Override
    public void initialize(final PhysicalLinkContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        
        // ${task}: Replace with custom handling of properties when physical link is created or restored. `config` 
        // object uses reflection to obtain property from map and therefore should not be used in processing intensive
        // code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // ${task}: Replace with custom handling of properties when they are updated. `config` object uses
        // reflection to obtain property from map and therefore should not be used in processing intensive code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }

    @Override
    public void open()
    {
        // ${task}: Open/initialize the hardware device

        Logging.log(LogService.LOG_INFO, "${description} opened");
    }

    @Override
    public void close()
    {
        // ${task}: Close the hardware device

        Logging.log(LogService.LOG_INFO, "${description} closed");
    }

    @Override
    public boolean isOpen()
    {
        // ${task}: Return whether the physical link is currently open or not
        return false;
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        // ${task}: Return the physical link input stream
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        // ${task}: Return the physical link output stream
        return null;
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        // ${task}: Modify so that the set returned contains any plug-in specific extensions. The use of extensions is 
        // discouraged, but can be used in cases where it is necessary to provide an extended API.
        
    	return new HashSet<Extension<?>>();
    }
    
    ${physicalLinkExtraCode}
}
