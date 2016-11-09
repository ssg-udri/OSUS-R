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
package mil.dod.th.ose.core.factory.impl;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.MetaTypeProvider;

import com.google.common.base.Preconditions;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.MetaTypeMocker;

//class does not appear to be needed anymore, deprecated 11/14/14, remove in future if really no longer needed
@Deprecated 
public class FactoryServiceReferenceMocker
{
    @SuppressWarnings({ "rawtypes", "unchecked" })//Due to mocking and ongoing stubbing
    public static <T extends FactoryDescriptor> ServiceReference<T> mockFactoryServiceRef(T factory, 
            List<ServiceReference<T>> factoryRefs) 
        throws IOException
    {
        Preconditions.checkNotNull(factory);
        
        final String factoryPid = factory.getProductType() + "Config";
        ServiceReference ref = mock(ServiceReference.class);
        final BundleContext bundleContext = mock(BundleContext.class);
        final Bundle bundle = mock(Bundle.class);
        when(bundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(ref.getBundle()).thenReturn(bundle);
        
        final ServiceRegistration msfReg = mock(ServiceRegistration.class);
        final ServiceRegistration evtHdlReg = mock(ServiceRegistration.class);

        when(bundleContext.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(evtHdlReg);
        
        final String[] clazzes = {ManagedServiceFactory.class.getName(), MetaTypeProvider.class.getName()};
        when(bundleContext.registerService(eq(clazzes), anyObject(), Mockito.any(Dictionary.class)))
            .thenAnswer(new Answer() 
            {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    FactoryConfigurationService configService = 
                            (FactoryConfigurationService)invocation.getArguments()[1];
                    String factoryPid = configService.m_Factory.getProductType() + "Config";
                    ConfigurationAdminMocker.registerManagedServiceFactory(factoryPid, configService);
                    MetaTypeMocker.registerMetaTypeProvider(bundle, factoryPid, configService);
                    return msfReg;
                }
            });
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                ConfigurationAdminMocker.unregisterManagedServiceFactory(factoryPid);
                return null;
            }
        }).when(msfReg).unregister();
        
        final ServiceRegistration fdReg = mock(ServiceRegistration.class);

        when(bundleContext.registerService(eq(FactoryDescriptor.class), Mockito.any(FactoryDescriptor.class), 
                Mockito.any(Dictionary.class))).thenReturn(fdReg);

        when((T)bundleContext.getService(ref)).thenReturn(factory);
        
        if (factoryRefs != null) // list of refs is optional
        {
            factoryRefs.add(ref);
        }
        
        return ref;
    }
}
