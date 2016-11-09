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
package mil.dod.th.ose.shell;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryObject;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestFactoryObjectCommands
{
    private FactoryObjectCommands m_SUT;

    @Mock
    private FactoryObject m_TestObject;

    @Mock
    private Extension<Object> m_Extension;
    
    @Mock
    private Object m_ExtensionObject;

    @Mock
    private CommandSession m_CmdSession;
    
    @Mock
    private PrintStream m_PrintStream;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(m_Extension.getObject()).thenReturn(m_ExtensionObject);

        Set<Class<?>> extTypes = new HashSet<>();
        extTypes.add(m_Extension.getClass());
        when(m_TestObject.getExtensionTypes()).thenReturn(extTypes);
        when(m_TestObject.getName()).thenReturn("FactoryObject");

        when(m_CmdSession.getConsole()).thenReturn(m_PrintStream);

        m_SUT = new FactoryObjectCommands();
    }

    /**
     * Verify that an extension is returned for a valid extension type.
     */
    @Test
    public void testGetExtension()
    {
        // mock
        doReturn(m_Extension.getObject()).when(m_TestObject).getExtension(m_Extension.getClass());

        // replay
        Object ext = m_SUT.getExtension(m_TestObject, m_Extension.getClass().getName());

        // verify
        assertEquals(m_ExtensionObject, ext);
    }

    /**
     * Verify that an unknown extension throws an exception.
     */
    @Test
    public void testGetUnknownExtension()
    {
        // replay
        try
        {
            m_SUT.getExtension(m_TestObject, "UnknownExtension");
            fail("Expected an exception");
        }
        catch (IllegalArgumentException e)
        {
            // verify
        }
    }

    /**
     * Verify that the expected set of extension type strings is printed to the console stream.
     */
    @Test
    public void testGetExtensions()
    {
        // replay
        m_SUT.getExtensions(m_CmdSession, m_TestObject);

        // verify
        verify(m_PrintStream).println("FactoryObject Extensions:");
        verify(m_PrintStream, times(2)).println(anyString());
    }

    /**
     * Verify that an empty set of extension type strings is printed when none exist for the factory object.
     */
    @Test
    public void testGetEmptyExtensions()
    {
        // mock
        Set<Class<?>> extTypes = new HashSet<>();
        when(m_TestObject.getExtensionTypes()).thenReturn(extTypes);

        // replay
        m_SUT.getExtensions(m_CmdSession, m_TestObject);

        // verify
        verify(m_PrintStream).println("FactoryObject Extensions:");
        verify(m_PrintStream).println("None");
    }
}
