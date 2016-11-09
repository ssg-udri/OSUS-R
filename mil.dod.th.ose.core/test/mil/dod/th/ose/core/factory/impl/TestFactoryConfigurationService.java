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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.ose.core.MetaTypeMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.utils.ConfigurationUtils;

public class TestFactoryConfigurationService
{
    private static final String PRODUCT_TYPE = "product-type";
    private FactoryConfigurationService m_SUT;
    private FactoryInternal m_Factory;
    private AttributeDefinition[] m_ServiceAttrsOpt;
    private AttributeDefinition[] m_ExtendedAttrsOpt;
    private AttributeDefinition[] m_ServiceAttrsReq;
    private AttributeDefinition[] m_ExtendedAttrsReq;
    @SuppressWarnings("rawtypes")
    private FactoryRegistry m_FactoryReg;
    
    @Mock private Bundle m_ApiBundle;
    
    @SuppressWarnings({"rawtypes"})
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_ServiceAttrsOpt = AttributeDefinitionMocker.mockArrayOptional();
        m_ExtendedAttrsOpt = AttributeDefinitionMocker.mockArrayOptional();
        m_ServiceAttrsReq = AttributeDefinitionMocker.mockArrayRequired();
        m_ExtendedAttrsReq = AttributeDefinitionMocker.mockArrayRequired();
        
        m_Factory = mock(FactoryInternal.class);
        when(m_Factory.getProductType()).thenReturn(PRODUCT_TYPE);
        when(m_Factory.getPid()).thenReturn(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX);
        
        FactoryServiceContext factoryServiceContext = mock(FactoryServiceContext.class);
        when(factoryServiceContext.getApiBundle()).thenReturn(m_ApiBundle);
        m_FactoryReg = mock(FactoryRegistry.class);
        when(factoryServiceContext.getBaseType()).thenReturn(FactoryObject.class);
        when(factoryServiceContext.getRegistry()).thenReturn(m_FactoryReg);
        MetaTypeService metaTypeService = MetaTypeMocker.createMockMetaType();
        when(factoryServiceContext.getMetaTypeService()).thenReturn(metaTypeService);
        when(factoryServiceContext.getExtendedServiceAttributeDefinitions(Mockito.any(FactoryInternal.class), 
                eq(ObjectClassDefinition.OPTIONAL))).thenReturn(m_ExtendedAttrsOpt);
        when(factoryServiceContext.getExtendedServiceAttributeDefinitions(Mockito.any(FactoryInternal.class), 
                eq(ObjectClassDefinition.REQUIRED))).thenReturn(m_ExtendedAttrsReq);
        
        m_SUT = new FactoryConfigurationService(m_Factory, factoryServiceContext);
    }
    
    /**
     * Verify both service and extended optional attributes are returned.
     */
    @Test
    public void testGetServiceAttributeDefinitionsOptional()
    {
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                null, m_ServiceAttrsOpt, null);

        AttributeDefinition[] attrs = m_SUT.getServiceAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
        
        assertThat(Arrays.asList(attrs), hasItems(m_ServiceAttrsOpt)); 
        assertThat(Arrays.asList(attrs), hasItems(m_ExtendedAttrsOpt)); 
        assertThat(attrs.length, is(m_ServiceAttrsOpt.length + m_ExtendedAttrsOpt.length));  
    }
    
    /**
     * Verify both service and extended required attributes are returned.
     */
    @Test
    public void testGetServiceAttributeDefinitionsRequired()
    {                
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                m_ServiceAttrsReq, null, null);

        AttributeDefinition[] attrs = m_SUT.getServiceAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        
        assertThat(Arrays.asList(attrs), hasItems(m_ServiceAttrsReq)); 
        assertThat(Arrays.asList(attrs), hasItems(m_ExtendedAttrsReq)); 
        assertThat(attrs.length, is(m_ServiceAttrsReq.length + m_ExtendedAttrsReq.length));  
    }
    
    /**
     * Verify exception if the OCD does not have the description for being a partial def.
     */
    @Test
    public void testGetServiceAttributeDefinitionsNoOcdDescription()
    {
        MetaTypeMocker.registerMetaTypeAttributesNoOcdDescription(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                m_ServiceAttrsOpt);

        try
        {
            m_SUT.getServiceAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
            fail("expected because the OCD does not reflect it is a partial def.");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify no base service attributes defined is handled properly. 
     */
    @Test
    public void testNoBaseServiceAttributeDefinitions()
    {
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                null, null, null);
        
        AttributeDefinition[] optAttrs = m_SUT.getServiceAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
        
        // should only have extended attrs
        assertThat(Arrays.asList(optAttrs), hasItems(m_ExtendedAttrsOpt));
        assertThat(optAttrs.length, is(m_ExtendedAttrsOpt.length));
        
        AttributeDefinition[] reqAttrs = m_SUT.getServiceAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        
        // should only have extended attrs
        assertThat(Arrays.asList(reqAttrs), hasItems(m_ExtendedAttrsReq));
        assertThat(reqAttrs.length, is(m_ExtendedAttrsReq.length));
    }
    
    /**
     * Verify get an objects class definition, optional attributes.
     */
    @Test
    public void testGetOCDOptional()
    {
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                m_ServiceAttrsReq, m_ServiceAttrsOpt, null);

        ObjectClassDefinition ocd = m_SUT.getObjectClassDefinition(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX, null);
        
        assertThat(ocd, is(notNullValue()));
        assertThat(ocd.getDescription(), is(m_Factory.getProductDescription()));
        assertThat(ocd.getName(), is(m_Factory.getProductType()));
        assertThat(ocd.getID(), is(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX));
        
        //inspect optional attrs
        List<AttributeDefinition> optAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL));
        assertThat(optAttrs.size(), is(m_ServiceAttrsOpt.length + m_ExtendedAttrsOpt.length));
        assertThat(optAttrs, hasItems(m_ServiceAttrsOpt)); 
        assertThat(optAttrs, hasItems(m_ExtendedAttrsOpt));
        
        //inspect required attrs
        List<AttributeDefinition> reqAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED));
        assertThat(reqAttrs.size(), is(m_ServiceAttrsReq.length + m_ExtendedAttrsReq.length));
        assertThat(reqAttrs, hasItems(m_ServiceAttrsReq)); 
        assertThat(reqAttrs, hasItems(m_ExtendedAttrsReq));
    }
    
    /**
     * Verify get an objects class definition when the service attrs are null.
     */
    @Test
    public void testGetOCDServiceAttrsNull()
    {
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                null, null, null);

        ObjectClassDefinition ocd = m_SUT.getObjectClassDefinition(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX, null);
        
        assertThat(ocd, is(notNullValue()));
        assertThat(ocd.getDescription(), is(m_Factory.getProductDescription()));
        assertThat(ocd.getName(), is(m_Factory.getProductType()));
        assertThat(ocd.getID(), is(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX));
        
        //inspect optional attrs
        List<AttributeDefinition> optAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL));
        assertThat(optAttrs.size(), is(m_ExtendedAttrsOpt.length)); 
        assertThat(optAttrs, not(hasItems(m_ServiceAttrsOpt)));
        assertThat(optAttrs, hasItems(m_ExtendedAttrsOpt));
        
        //inspect required attrs
        List<AttributeDefinition> reqAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED));
        assertThat(reqAttrs.size(), is(m_ExtendedAttrsReq.length)); 
        assertThat(reqAttrs, not(hasItems(m_ServiceAttrsReq)));
        assertThat(reqAttrs, hasItems(m_ExtendedAttrsReq));
    }
    
    /**
     * Verify get an objects class definition when the service attrs are null.
     */
    @Test
    public void testGetOCDWithFactoryAttrs()
    {
        MetaTypeMocker.registerMetaTypeAttributes(m_ApiBundle, 
                FactoryObject.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, // service PID 
                null, null, null);

        AttributeDefinition def1 = mock(AttributeDefinition.class);
        AttributeDefinition def2 = mock(AttributeDefinition.class);
        
        when(m_Factory.getPluginAttributeDefinitions(ObjectClassDefinition.OPTIONAL)).
            thenReturn(new AttributeDefinition[]{def1});
        when(m_Factory.getPluginAttributeDefinitions(ObjectClassDefinition.REQUIRED)).
            thenReturn(new AttributeDefinition[]{def2});
        
        ObjectClassDefinition ocd = m_SUT.getObjectClassDefinition(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX, null);
        
        assertThat(ocd, is(notNullValue()));
        assertThat(ocd.getDescription(), is(m_Factory.getProductDescription()));
        assertThat(ocd.getName(), is(m_Factory.getProductType()));
        assertThat(ocd.getID(), is(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX));
        
        //inspect optional attrs
        List<AttributeDefinition> optAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL));
        //plus 1 which is the plugin specific attr
        assertThat(optAttrs.size(), is(m_ExtendedAttrsOpt.length + 1));
        assertThat(optAttrs, not(hasItems(m_ServiceAttrsOpt)));
        assertThat(optAttrs, hasItems(m_ExtendedAttrsOpt));
        assertThat(optAttrs, hasItem(def1));
        
        //inspect required attrs
        List<AttributeDefinition> reqAttrs = Arrays.asList(
                ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED));
        //plus 1 which is the plug-in specific attr
        assertThat(reqAttrs.size(), is(m_ExtendedAttrsReq.length + 1));
        assertThat(reqAttrs, not(hasItems(m_ServiceAttrsReq)));
        assertThat(reqAttrs, hasItems(m_ExtendedAttrsReq));
        assertThat(reqAttrs, hasItem(def2));
    }
    
    /**
     * Verify exception if the passed PID is unknown.
     */
    @Test
    public void testGetOCDException()
    {
        try
        {
            m_SUT.getObjectClassDefinition("newCar", null);
            fail("Expected because pid isn't known.");
        }
        catch (IllegalStateException e)
        {
            //expected as the pid isn't known.
        }
    }
    
    /**
     * Verify updated actually updates configuration properties.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdated() throws FactoryException, ConfigurationException
    {
        FactoryObjectInternal obj = mock(FactoryObjectInternal.class);
        String pid = "oldCar";
        when(m_FactoryReg.getObjectByPid(pid)).thenReturn(obj);
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("k", "l");
        
        //act
        m_SUT.updated(pid, props);
        
        //verify
        verify(m_FactoryReg).handleUpdated(obj);
        verify(obj).blockingPropsUpdate(ConfigurationUtils.convertDictionaryPropsToMap(props));
    }
    
    /**
     * Verify removal of pid from registry if a configuration is deleted thus the pid is no longer
     * valid.
     */
    @Test
    public void testDeleted() throws IllegalArgumentException, FactoryObjectInformationException
    {
        FactoryObjectInternal obj = mock(FactoryObjectInternal.class);
        String pid = "oldCar";
        when(m_FactoryReg.getObjectByPid(pid)).thenReturn(obj);
        
        m_SUT.deleted(pid);
        
        //verify
        verify(m_FactoryReg).unAssignPidForObj(obj);
    }
}
