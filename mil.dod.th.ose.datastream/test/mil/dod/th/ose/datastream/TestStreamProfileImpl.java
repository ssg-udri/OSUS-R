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

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.datastream.StreamProfileAttributes;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.transcoder.TranscoderException;
import mil.dod.th.core.transcoder.TranscoderService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
public class TestStreamProfileImpl
{
    private static final String PRODUCT_TYPE = "product type";
    private static UUID OBJ_UUID = UUID.randomUUID();
    private static String OBJ_NAME = "StreamProfile1";
    private static String OBJ_PID = "StreamProfileConfig";
    private static final String OBJ_BASETYPE = "ObjBaseType";
    private static String ASSET_NAME = "Test Asset";
    private static double BITRATE_KBPS = 50.0;
    private static String DATA_SOURCE_STRING = "http://1.2.3.4:54321";
    private static String FORMAT = "video/mpeg";
    private static String SENSOR_ID = "sensor1";
    private URI m_StreamPort;

    private StreamProfileImpl m_SUT;

    @SuppressWarnings("rawtypes")
    @Mock private FactoryRegistry m_FactoryRegistry;
    @Mock private StreamProfileProxy m_StreamProfileProxy;
    @Mock private ConfigurationAdmin m_ConfigAdmin;
    @Mock private EventAdmin m_EventAdmin;
    @Mock private Configuration m_Config;
    @Mock private TranscoderService m_TranscoderService;
    @Mock private Asset m_Asset;
    @Mock private FactoryInternal m_FactoryInternal;
    @Mock private PowerManagerInternal m_PowerInternal;
    @Mock private LoggingService m_LoggingService;
    @Mock private StreamProfileFactoryObjectDataManager m_StreamProfileFactoryObjectDataManager;
    @Mock private WakeLock m_WakeLock;
  
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        m_StreamPort = new URI(null, null, "225.1.2.3", 20000, null, null, null);

        when(m_FactoryInternal.getProductType()).thenReturn(PRODUCT_TYPE);

        m_SUT = new StreamProfileImpl();

        when(m_PowerInternal.createWakeLock(m_StreamProfileProxy.getClass(), m_SUT, "coreStreamProfile")).thenReturn(
                m_WakeLock);

        m_SUT.setLoggingService(m_LoggingService);
        m_SUT.setTranscoderService(m_TranscoderService);
        m_SUT.setFactoryObjectDataManager(m_StreamProfileFactoryObjectDataManager);
        m_SUT.setStreamPort(m_StreamPort);

        Dictionary<String, Object> table = new Hashtable<>();
        table.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, ASSET_NAME);
        table.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, BITRATE_KBPS);
        table.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, new URI(DATA_SOURCE_STRING));
        table.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, FORMAT);
        table.put(StreamProfileAttributes.CONFIG_PROP_SENSOR_ID, SENSOR_ID);

        when(m_Config.getProperties()).thenReturn(table);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Dictionary<String, Object> dictionary = (Dictionary<String, Object>)invocation.getArguments()[0];
                m_SUT.blockingPropsUpdate(ConfigurationUtils.convertDictionaryPropsToMap(dictionary));

                return null;
            }
        }).when(m_Config).update(Mockito.any(Dictionary.class));
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {m_Config});

        m_SUT.initialize(m_FactoryRegistry, m_StreamProfileProxy, m_FactoryInternal, 
                m_ConfigAdmin, m_EventAdmin, m_PowerInternal, OBJ_UUID, OBJ_NAME, OBJ_PID, OBJ_BASETYPE);
    }
    
    @Test
    public void testIsEnabled()
    {
        //False by default
        assertThat(m_SUT.isEnabled(), is(false));
    }

    @Test
    public void testSetEnabledTrueWhenAlreadyEnabled()
    {
        Whitebox.setInternalState(m_SUT, "m_Enabled", true);
        m_SUT.setEnabled(true);
        verify(m_LoggingService).warning(anyString(), anyVararg());
        verify(m_WakeLock, never()).activate();
    }

    @Test
    public void testSetEnabledTrue() throws StreamProfileException, IllegalStateException, TranscoderException
    {
        m_SUT.setEnabled(true);

        verify(m_StreamProfileProxy).onEnabled();
        verify(m_TranscoderService).start(eq(OBJ_UUID.toString()), Mockito.<URI>anyObject(), 
                Mockito.<URI>anyObject(), Matchers.<Map<String,Object>>any());
        verify(m_WakeLock).activate();
    }
    
    @Test
    public void testSetEnabledFalseWhenAlreadyDisabled() 
    {
        m_SUT.setEnabled(false);
        verify(m_LoggingService).warning(anyString(), anyVararg());
        verify(m_WakeLock, never()).cancel();
    }
    
    @Test
    public void testSetEnabledFalse() throws StreamProfileException
    {
        Whitebox.setInternalState(m_SUT, "m_Enabled", true);
        m_SUT.setEnabled(false);
        
        verify(m_StreamProfileProxy).onDisabled();
        verify(m_TranscoderService).stop(eq(OBJ_UUID.toString()));
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testDelete()
    {
        m_SUT.delete();

        verify(m_WakeLock).delete();
    }
}
