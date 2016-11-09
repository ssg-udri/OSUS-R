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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link PhysicalLinkRegistryCallback}.
 * @author allenchl
 *
 */
public class TestPhysicalLinkRegistryCallback
{
    private PhysicalLinkRegistryCallback m_SUT;
    private LoggingService m_Log;
    
    @Before
    public void setup()
    {
        m_Log = LoggingServiceMocker.createMock();
        m_SUT = new PhysicalLinkRegistryCallback(m_Log);
    }
    
    /**
     * Verify no reg deps are returned when retrieve reg deps is called.
     */
    @Test
    public void testRetrieveRegistryDependencies()
    {
        assertThat(m_SUT.retrieveRegistryDependencies().size(), is(0));
    }
    
    /**
     * Verify when an phys is removed that the link is closed if open.
     */
    @Test
    public void testOnRemovedObjectOpen() throws PhysicalLinkException
    {
        PhysicalLinkInternal phys =  mock(PhysicalLinkInternal.class);
        when(phys.isOpen()).thenReturn(true);
        
        m_SUT.onRemovedObject(phys);
        verify(phys).close();
    }
    
    /**
     * Verify when an phys is removed that the link is attempted to be closed.
     * Verify if unable to close that no exception is thrown from the method call.
     */
    @Test
    public void testOnRemovedObjectOpenException() throws PhysicalLinkException
    {
        PhysicalLinkInternal phys =  mock(PhysicalLinkInternal.class);
        when(phys.isOpen()).thenReturn(true);
        doThrow(new PhysicalLinkException("uh oh")).when(phys).close();
        
        m_SUT.onRemovedObject(phys);
    }
    
    /**
     * Verify when an phys is removed that the link is closed only if open.
     */
    @Test
    public void testOnRemovedObjectClosed() throws PhysicalLinkException
    {
        PhysicalLinkInternal phys =  mock(PhysicalLinkInternal.class);
        when(phys.isOpen()).thenReturn(false);
        
        m_SUT.onRemovedObject(phys);
        verify(phys, never()).close();
    }
}
