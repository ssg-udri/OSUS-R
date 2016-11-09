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
package mil.dod.th.ose.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObjectContext;

/**
 * Class that handles mocking {@link AssetContext}s.
 * 
 * @author cweisenborn
 */
public class FactoryObjectContextMocker 
{
    /**
     * Creates a mocked {@link AssetContext} that returns the specified properties.
     * 
     * @param name
     *      Name of the asset to be returned when {@link AssetContext#getName()}.
     * @param props
     *      Configuration properties to be returned when {@link AssetContext#getProperties()} or 
     *      {@link AssetContext#getProperties()} is called.
     * @return
     *      The mocked asset context.
     */
    public static AssetContext mockAssetContext(final String name, final Map<String, Object> props)
    {
        final AssetContext context = mock(AssetContext.class);
        final AssetFactory factory = mock(AssetFactory.class);
        
        mockContext(context, factory, name, props);
        
        return context;
    }
    
    /**
     * Creates a mocked {@link PhysicalLinkContext} that returns the specified properties.
     * 
     * @param name
     *      Name of the asset to be returned when {@link PhysicalLinkContext#getName()}.
     * @param props
     *      Configuration properties to be returned when {@link PhysicalLinkContext#getProperties()} or 
     *      {@link PhysicalLinkContext#getProperties()} is called.
     * @return
     *      The mocked asset context.
     */
    public static PhysicalLinkContext mockPhysicalLinkContext(final String name, final Map<String, Object> props)
    {
        final PhysicalLinkContext context = mock(PhysicalLinkContext.class);
        final PhysicalLinkFactory factory = mock(PhysicalLinkFactory.class);
        
        mockContext(context, factory, name, props);
        
        return context;
    }
    
    /**
     * Method used to mock the context methods.
     * 
     * @param context
     *      Context to be mocked.
     * @param factory
     *      Factory the context should return when getFactory is called.
     * @param name
     *      Name to be returned when getName is called.
     * @param props
     *      Properties to be returned when getProperties is called.
     */
    private static void mockContext(final FactoryObjectContext context, 
        final FactoryDescriptor factory, final String name, final Map<String, Object> props)
    {
        when(context.getName()).thenReturn(name);
        when(context.getFactory()).thenReturn(factory);
        when(context.getProperties()).thenReturn(props);
    }
}
