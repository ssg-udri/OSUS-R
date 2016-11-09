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

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mock internal interactions with the datastore.
 * @author allenchl
 *
 */
public final class FactoryObjectDataManagerMocker
{
    //object PIDs, keyed by UUID
    private static Map<UUID, String> m_ObjectPids = 
            Collections.synchronizedMap(new HashMap<UUID, String>());
    //object names, keyed by UUID
    private static Map<UUID, String> m_ObjectNames = 
            Collections.synchronizedMap(new HashMap<UUID, String>());
    
    /**
     * Create a factory object data manager mocking implementation.
     * @param manager
     *      the mock manager to handle implementation concerns for
     */
    public static void createMockFactoryObjectDataManager(final FactoryObjectDataManager manager) 
        throws IllegalArgumentException, FactoryObjectInformationException
    {
        //clear any previous entries
        m_ObjectNames.clear();
        m_ObjectPids.clear();
        
        //when the data manager is asked to get the PID for an object pull the PID from the map of PIDs
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                final UUID objUuid = (UUID)invocation.getArguments()[0];
                return m_ObjectPids.get(objUuid);
            }
        }).when(manager).getPid(Mockito.any(UUID.class));

        //when asked to store the PID of an object, put the PID in the PID map
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                final UUID objUuid = (UUID)invocation.getArguments()[0];
                final String pid = (String)invocation.getArguments()[1];
                
                return m_ObjectPids.put(objUuid, pid);
            }
        }).when(manager).setPid(Mockito.any(UUID.class), anyString());
        
        //remove the PID from the lookup
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                final UUID objUuid = (UUID)invocation.getArguments()[0];

                return m_ObjectPids.remove(objUuid);
            }
        }).when(manager).clearPid(Mockito.any(UUID.class));
        
        //when the data manager is asked to get the name of an object, pull the name from the map of names keyed by UUID
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                final UUID objUuid = (UUID)invocation.getArguments()[0];
                return m_ObjectNames.get(objUuid);
            }
        }).when(manager).getName(Mockito.any(UUID.class));

        //create an entry for the object's name in the Map of names
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                //UUID of the object
                final UUID objUuid = (UUID)invocation.getArguments()[0];
                //the name
                final String name = (String)invocation.getArguments()[2];

                return m_ObjectNames.put(objUuid, name);
            }
        }).when(manager)
            .persistNewObjectData(Mockito.any(UUID.class), Mockito.any(FactoryDescriptor.class), anyString());
        
        //replace or add the name in the map object names
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                final UUID objUuid = (UUID)invocation.getArguments()[0];
                final String name = (String)invocation.getArguments()[1];

                return m_ObjectNames.put(objUuid, name);
            }
        }).when(manager).setName(Mockito.any(UUID.class), anyString());
    }
}