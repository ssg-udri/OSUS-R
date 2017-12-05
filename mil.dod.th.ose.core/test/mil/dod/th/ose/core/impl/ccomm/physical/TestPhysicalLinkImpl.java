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
//
// DESCRIPTION:
// JUnit test class for the CommPortService abstract class
//
//==============================================================================
package mil.dod.th.ose.core.impl.ccomm.physical;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkAttributes;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkProxy;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.EventAdmin;

/**
 * @author dhumeniuk
 *
 */
public class TestPhysicalLinkImpl
{
    private PhysicalLinkImpl m_SUT;
    private PhysicalLinkProxy m_PhysLinkProxy;
    private FactoryRegistry<?> m_FactReg;
    private FactoryInternal m_PhysicalLinkFactoryInternal;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private EventAdmin m_EventAdmin;
    private UUID m_Uuid = UUID.randomUUID();
    private String m_Name = "name";
    private String m_Pid;
    private String m_BaseType = "baseType";
    private PhysicalLinkCapabilities m_Caps;
    private PowerManagerInternal m_PowManInternal;
    private WakeLock m_WakeLock;
    private Configuration m_Configuration;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new PhysicalLinkImpl();
        
        //mocks
        m_PhysLinkProxy = mock(PhysicalLinkProxy.class, withSettings().extraInterfaces(FactoryObjectProxy.class));
        m_FactReg = mock(FactoryRegistry.class);
        m_PhysicalLinkFactoryInternal = mock(FactoryInternal.class);
        m_ConfigurationAdmin = ConfigurationAdminMocker.createMockConfigAdmin();
        m_EventAdmin = mock(EventAdmin.class);
        m_Caps = mock(PhysicalLinkCapabilities.class);
        m_PowManInternal = mock(PowerManagerInternal.class);
        m_WakeLock = mock(WakeLock.class);

        when(m_PhysicalLinkFactoryInternal.getCapabilities()).thenReturn(m_Caps);
        doReturn(PhysicalLink.class.getName()).when(m_PhysicalLinkFactoryInternal).getProductType();
        when(m_PowManInternal.createWakeLock(m_PhysLinkProxy.getClass(), m_SUT, "coreFactoryObject")).thenReturn(
                m_WakeLock);
        when(m_PowManInternal.createWakeLock(m_PhysLinkProxy.getClass(), m_SUT, "corePhyLink")).thenReturn(m_WakeLock);

        m_Configuration = mock(Configuration.class);

        //add a random number to the PID so that the order of tests does not depend on if the configuration has already
        //been mocked or not
        m_Pid = "pid" + Math.random();

        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.initialize(m_FactReg, m_PhysLinkProxy, m_PhysicalLinkFactoryInternal, m_ConfigurationAdmin, 
                m_EventAdmin, m_PowManInternal, m_Uuid, m_Name, m_Pid, m_BaseType);
        m_SUT.postCreation();

        m_Configuration = ConfigurationAdminMocker.addMockConfiguration(m_SUT);
        when(m_FactReg.createConfiguration(eq(m_Uuid), anyString(), eq(m_SUT))).thenReturn(m_Configuration);
    }
    
    /**
     * Verify that inUse flag can be set and retrieved.
     */
    @Test
    public void testSetInUse()
    {
        m_SUT.setInUse(true);
        assertThat(m_SUT.isInUse(), is(true));

        m_SUT.setInUse(false);
        assertThat(m_SUT.isInUse(), is(false));
    }

    /**
     * Verify ability to set and retrieve the read timeout value.
     * Verify that the readtime value is validated.
     */
    @Test
    public final void testReadTimeout() throws IllegalArgumentException, FactoryException, 
        InterruptedException, IOException, IllegalStateException, ConfigurationException
    {
        m_SUT.setReadTimeout(100);
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(100));

        //verify 0 is ok
        m_SUT.setReadTimeout(0);
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(0));
        
        //Verify a negative number is ok
        m_SUT.setReadTimeout(-1);
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(-1));
    }
    
    /**
     * Verify being able to set and retrieve a physical links owner.
     */
    @Test
    public final void testSetGetOwner()
    {
        assertThat(m_SUT.getOwner(), is(nullValue()));
        
        LinkLayer linkLayer = mock(LinkLayer.class);
        m_SUT.setOwner(linkLayer);
        
        assertThat(m_SUT.getOwner(), is(linkLayer));
    }
    
    /**
     * Verify retrieving the input stream from proxy.
     */
    @Test
    public void testGetInputStream() throws PhysicalLinkException
    {
        //act
        m_SUT.getInputStream();
        
        verify(m_PhysLinkProxy).getInputStream();
    }
    
    /**
     * Verify retrieving the output stream from proxy.
     */
    @Test
    public void testGetOutputStream() throws PhysicalLinkException
    {
        //act
        m_SUT.getOutputStream();
        
        verify(m_PhysLinkProxy).getOutputStream();
    }
    
    /**
     * Verify calling open on the physical link proxy.
     */
    @Test
    public void testOpen() throws PhysicalLinkException
    {
        //act
        m_SUT.open();
        
        verify(m_WakeLock).activate();
        verify(m_PhysLinkProxy).open();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify calling close on the physical link proxy.
     */
    @Test
    public void testClose() throws PhysicalLinkException
    {
        //act
        m_SUT.close();
        
        verify(m_WakeLock).activate();
        verify(m_PhysLinkProxy).close();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify calling isopen on the physical link proxy.
     */
    @Test
    public void testIsOpen() throws PhysicalLinkException
    {
        //act
        m_SUT.isOpen();
        
        verify(m_PhysLinkProxy).isOpen();
    }
    
    /**
     * Verify ability to get physical link factory.
     */
    @Test
    public void testGetFactory()
    {
        assertThat(m_SUT.getFactory(), is(m_PhysicalLinkFactoryInternal));
    }
    
    /**
     * Verify ability to set databits property value.
     */
    @Test
    public final void testSetDataBits() throws IllegalArgumentException, FactoryException, 
        InterruptedException, IOException, IllegalStateException, ConfigurationException, PhysicalLinkException
    {
        //act
        m_SUT.setDataBits(100);
        
        //assert
        assertThat(m_SUT.getProperties(), hasEntry(PhysicalLinkAttributes.CONFIG_PROP_DATA_BITS, (Object)100));
    }
    
    /**
     * Verify exception if data bits value is less than 1.
     */
    @Test
    public void testSetDataBitsException() throws IOException, FactoryException
    {
        try
        {
            m_SUT.setDataBits(-1);
            fail("Exepcted exception becuase -1 is less than 1.");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
        
        verify(m_ConfigurationAdmin, never()).getConfiguration(m_Pid, null);
    }
    
    /**
     * Verify {@link PhysicalLinkImpl#getConfig()} will return an object with the default properties and set properties.
     */
    @Test
    public void testGetConfig()
    {
        Dictionary<String, Object> table = new Hashtable<>();
        when(m_Configuration.getProperties()).thenReturn(table);
        
        // verify defaults
        assertThat(m_SUT.getConfig().dataBits(), is(8));
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(-1));
        
        // test overrides
        table.put(PhysicalLinkAttributes.CONFIG_PROP_DATA_BITS, 7);
        table.put(PhysicalLinkAttributes.CONFIG_PROP_READ_TIMEOUT_MS, 500);
        assertThat(m_SUT.getConfig().dataBits(), is(7));
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(500));

    }
    
    /**
     * Verify a physical link can be released.
     */
    @Test
    public void testRelease() throws Exception 
    {
        //mocking
        LinkLayer linkLayer = mock(LinkLayer.class);
        when(m_PhysLinkProxy.isOpen()).thenReturn(false);
        
        //act
        m_SUT.setInUse(true);
        m_SUT.setOwner(linkLayer);
        m_SUT.release();
        
        assertThat(m_SUT.isInUse(), is(false));
        assertThat(m_SUT.getOwner(), is(nullValue()));
    }
    
    /**
     * Verify the a physical link cannot be released if still open.
     */
    @Test
    public void testRelease_StillOpen()
    {
        //mocking
        LinkLayer linkLayer = mock(LinkLayer.class);
        when(m_PhysLinkProxy.isOpen()).thenReturn(true);
        
        //act
        m_SUT.setInUse(true);
        m_SUT.setOwner(linkLayer);
        
        try
        {
            m_SUT.release();
            fail("Should not be able to release as still open");
        }
        catch (IllegalStateException e) { }
        
        assertThat(m_SUT.isInUse(), is(true));
        assertThat(m_SUT.getOwner(), is(linkLayer));
    }
    
    /**
     * Verify the a physical link cannot be released if not in use.
     */
    @Test
    public void testRelease_NotInUse()
    {
        //mocking
        LinkLayer linkLayer = mock(LinkLayer.class);
        when(m_PhysLinkProxy.isOpen()).thenReturn(false);
        
        //act
        m_SUT.setInUse(false);
        m_SUT.setOwner(linkLayer);
        
        try
        {
            m_SUT.release();
            fail("Should not be able to release as not in use");
        }
        catch (IllegalStateException e) { }
        
        assertThat(m_SUT.isInUse(), is(false));
        assertThat(m_SUT.getOwner(), is(linkLayer));
    }
    
    /**
     * Verify the registry is called to delete the object.
     */
    @Test
    public void testDelete() throws Exception
    {
        m_SUT.delete();
        verify(m_FactReg).delete(m_SUT);
        verify(m_PowManInternal, times(2)).deleteWakeLock(m_WakeLock);
    }
    
    /**
     * Verify the registry is not called if link is in use.
     */
    @Test
    public void testDelete_InUse() throws Exception
    {
        m_SUT.setInUse(true);
        
        try
        {
            m_SUT.delete();
            fail("Should fail to delete as link is in use");
        }
        catch (IllegalStateException e)
        {
            
        }
        verify(m_FactReg, never()).delete(Mockito.any(FactoryObjectInternal.class));
        verify(m_WakeLock, never()).delete();
    }
    
    /**
     * Verify the registry is not called if link is open.
     */
    @Test
    public void testDelete_IsOpen() throws Exception
    {
        when(m_PhysLinkProxy.isOpen()).thenReturn(true);
        
        try
        {
            m_SUT.delete();
            fail("Should fail to delete as link is open");
        }
        catch (IllegalStateException e)
        {
            
        }
        verify(m_FactReg, never()).delete(Mockito.any(FactoryObjectInternal.class));
        verify(m_WakeLock, never()).delete();
    }
}
