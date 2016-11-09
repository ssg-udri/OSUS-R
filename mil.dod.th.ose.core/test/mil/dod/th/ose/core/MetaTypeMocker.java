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
package mil.dod.th.ose.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.core.factory.api.FactoryInternal;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author dhumeniuk
 *
 */
public class MetaTypeMocker
{
    private static Map<Bundle, MetaTypeInformation> m_MetaInfo = new HashMap<Bundle, MetaTypeInformation>();
    private static Map<Bundle, String[]> m_Pids = new HashMap<Bundle, String[]>();

    public static MetaTypeService createMockMetaType()
    {
        m_MetaInfo.clear();
        m_Pids.clear();

        MetaTypeService service = mock(MetaTypeService.class);
        
        when(service.getMetaTypeInformation(Mockito.any(Bundle.class))).thenAnswer(new Answer<MetaTypeInformation>()
        {
            @Override
            public MetaTypeInformation answer(InvocationOnMock invocation) throws Throwable
            {
                Bundle bundle = (Bundle)invocation.getArguments()[0];
                return m_MetaInfo.get(bundle);
            }
        });
        
        return service;
    }

    public static void registerMetaTypeProvider(Bundle bundle, String factoryPid, MetaTypeProvider provider)
    {
        MetaTypeInformation info = m_MetaInfo.get(bundle);
        if (info == null)
        {
            info = mock(MetaTypeInformation.class);
            m_MetaInfo.put(bundle, info);
            m_Pids.put(bundle, new String[] {factoryPid});
        }
        
        ObjectClassDefinition ocd = provider.getObjectClassDefinition(factoryPid, null);
        when(ocd.getDescription()).thenReturn(ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION);
        when(info.getObjectClassDefinition(factoryPid, null)).thenReturn(ocd);
        when(info.getPids()).thenReturn(m_Pids.get(bundle));
    }
    
    /**
     * Mock {@link MetaTypeService} to return the given attributes when requested attributes for the given factory.
     */
    public static void registerMetaTypeAttributes(Bundle bundle, FactoryInternal factory, 
            AttributeDefinition[] attrsReq, AttributeDefinition[] attrsOpt, AttributeDefinition[] attrsAll)
    {
        String factoryPid = factory.getPid();
        
        registerMetaTypeAttributes(bundle, factoryPid, attrsReq, attrsOpt, attrsAll);
    }
    
    public static MetaTypeInformation registerMetaTypeAttributes(Bundle bundle, String factoryPid, 
            AttributeDefinition[] attrsReq, AttributeDefinition[] attrsOpt, AttributeDefinition[] attrsAll)
    {
        MetaTypeInformation info = m_MetaInfo.get(bundle);
        if (info == null)
        {
            info = mock(MetaTypeInformation.class);
            m_MetaInfo.put(bundle, info);
            m_Pids.put(bundle, new String[] {factoryPid});
        }
        
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrsAll);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)).thenReturn(attrsReq);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL)).thenReturn(attrsOpt);
        when(info.getObjectClassDefinition(factoryPid, null)).thenReturn(ocd);
        when(ocd.getDescription()).thenReturn(ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION);
        when(info.getPids()).thenReturn(m_Pids.get(bundle));
        
        Logging.log(LogService.LOG_DEBUG, "Registered metatype attributes for factory pid=%s", factoryPid);
        
        return info;
    }
    
    public static void registerMetaTypeAttributesNoOcdDescription(
            Bundle bundle, String factoryPid, AttributeDefinition[] attrs)
    {
        MetaTypeInformation info = m_MetaInfo.get(bundle);
        if (info == null)
        {
            info = mock(MetaTypeInformation.class);
            m_MetaInfo.put(bundle, info);
            m_Pids.put(bundle, new String[] {factoryPid});
        }
        
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrs);
        when(ocd.getDescription()).thenReturn("");
        when(info.getObjectClassDefinition(factoryPid, null)).thenReturn(ocd);
        when(info.getPids()).thenReturn(m_Pids.get(bundle));
        
        Logging.log(LogService.LOG_DEBUG, "Registered metatype attributes for factory pid=%s", factoryPid);
    }
}
