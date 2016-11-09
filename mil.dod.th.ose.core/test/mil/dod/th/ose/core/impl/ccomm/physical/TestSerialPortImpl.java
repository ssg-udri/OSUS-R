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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Hashtable;
import java.util.UUID;

import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.ccomm.physical.SerialPortProxy;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.EventAdmin;

/**
 * Test class for the serial port implementation.
 * @author allenchl
 *
 */
public class TestSerialPortImpl
{
    private SerialPortImpl m_SUT;
    private SerialPortProxy m_SerialProxy;
    private FactoryRegistry<?> m_FactReg;
    private FactoryInternal m_PhysicalLinkFactoryInternal;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private EventAdmin m_EventAdmin;
    private UUID m_Uuid = UUID.randomUUID();
    private String m_Name = "name";
    private String m_Pid = "pid1";
    private String m_BaseType = "baseType";
    private PhysicalLinkCapabilities m_Caps;
    private PowerManagerInternal m_PowManInternal;
    private Configuration m_Config;
    interface MySerialPortProxyAttributes extends SerialPortAttributes{}
    interface MySerialPortProxy extends SerialPortProxy{}
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new SerialPortImpl();
        
        //mocks
        m_SerialProxy = mock(SerialPortProxy.class, withSettings().extraInterfaces(FactoryObjectProxy.class));
        m_FactReg = mock(FactoryRegistry.class);
        m_PhysicalLinkFactoryInternal = mock(FactoryInternal.class);
        m_ConfigurationAdmin = ConfigurationAdminMocker.createMockConfigAdmin();
        m_EventAdmin = mock(EventAdmin.class);
        m_Caps = mock(PhysicalLinkCapabilities.class);
        m_PowManInternal = mock(PowerManagerInternal.class);

        when(m_PhysicalLinkFactoryInternal.getCapabilities()).thenReturn(m_Caps);
        doReturn(MySerialPortProxy.class.getName()).when(m_PhysicalLinkFactoryInternal).getProductType();
        
        m_SUT.initialize(m_FactReg, m_SerialProxy, m_PhysicalLinkFactoryInternal, m_ConfigurationAdmin, m_EventAdmin, 
                m_PowManInternal, m_Uuid, m_Name, m_Pid, m_BaseType);
        
        m_Config = ConfigurationAdminMocker.addMockConfiguration(m_SUT);
        when(m_FactReg.createConfiguration(eq(m_Uuid), anyString(), eq(m_SUT))).thenReturn(m_Config);
    }
    
    /**
     * Verify that a serial port's properties can be set and retrieved.
     */
    @Test
    public final void testSetSerialPortProperties() 
        throws PhysicalLinkException, IOException, InterruptedException, IllegalArgumentException, 
            IllegalStateException, ConfigurationException, FactoryException
    {
        m_SUT.setSerialPortProperties(57600, 8, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, FlowControlEnum.NONE);
        
        //verify
        assertThat(m_SUT.getConfig().baudRate(), is(57600));
        assertThat(m_SUT.getConfig().dataBits(), is(8));
        assertThat(m_SUT.getConfig().parity(), is(ParityEnum.NONE));
        assertThat(m_SUT.getConfig().stopBits(), is(StopBitsEnum.ONE_STOP_BIT));
        assertThat(m_SUT.getConfig().flowControl(), is(FlowControlEnum.NONE));
        
        //replay
        m_SUT.setSerialPortProperties(9600, 7, ParityEnum.EVEN, StopBitsEnum.TWO_STOP_BITS, 
                FlowControlEnum.XON_XOFF);
        
        //verify
        assertThat(m_SUT.getConfig().baudRate(), is(9600));
        assertThat(m_SUT.getConfig().dataBits(), is(7));
        assertThat(m_SUT.getConfig().parity(), is(ParityEnum.EVEN));
        assertThat(m_SUT.getConfig().stopBits(), is(StopBitsEnum.TWO_STOP_BITS));
        assertThat(m_SUT.getConfig().flowControl(), is(FlowControlEnum.XON_XOFF));
    }
    
    /**
     * Verify physical link exception if set properties fails.
     */
    @Test
    public void testSetPropertySetPropError() throws Exception
    {
        m_ConfigurationAdmin = mock(ConfigurationAdmin.class);
        
        //reset up
        String pid = "pid";
        m_SUT.initialize(m_FactReg, m_SerialProxy, m_PhysicalLinkFactoryInternal, m_ConfigurationAdmin, m_EventAdmin, 
                m_PowManInternal, m_Uuid, m_Name, pid, m_BaseType);
        
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(new Hashtable<String, Object>());
        when(m_ConfigurationAdmin.listConfigurations(anyString()))
            .thenReturn(new Configuration[] {config})
            .thenThrow(new IOException("Exception"));
        
        try
        {
            m_SUT.setSerialPortProperties(115200, 7, ParityEnum.EVEN, StopBitsEnum.TWO_STOP_BITS, 
                    FlowControlEnum.XON_XOFF);
            fail("Expecting Exception, because the configuration will throw exception.");
        }
        catch (PhysicalLinkException e)
        {
            //expecting exception
        }
    }
    
    /**
     * Verify call to proxy when DTR is set.
     */
    @Test
    public void testSetDtr()
    {
        //act
        m_SUT.setDTR(false);
        
        verify(m_SerialProxy).setDTR(false);
    }
}
