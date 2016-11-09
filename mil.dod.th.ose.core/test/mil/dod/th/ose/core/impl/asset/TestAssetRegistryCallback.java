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
package mil.dod.th.ose.core.impl.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author dhumeniuk
 *
 */
public class TestAssetRegistryCallback
{
    private AssetRegistryCallback m_SUT;
    
    @Mock private FactoryServiceContext<AssetInternal> factoryServiceContext;
    @Mock private FactoryRegistry<AssetInternal> registry;
    
    @Before
    public void setUp()
    {
        // mock
        MockitoAnnotations.initMocks(this);

        // stub
        when(factoryServiceContext.getRegistry()).thenReturn(registry);

        // setup
        m_SUT = new AssetRegistryCallback(factoryServiceContext);
    }
    
    /**
     * Verify checking if a new object should be activated at startup.
     */
    @Test
    public void testPostObjectInitialize() throws FactoryException, AssetException
    {
        AssetInternal mockAsset = mock(AssetInternal.class);
        
        AssetAttributes attrs = mock(AssetAttributes.class);
        when(mockAsset.getConfig()).thenReturn(attrs);
        when(attrs.activateOnStartup()).thenReturn(true);
        String name = "name";
        when(mockAsset.getName()).thenReturn(name);
        when(registry.isObjectCreated(name)).thenReturn(false);
        when(mockAsset.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        
        m_SUT.postObjectInitialize(mockAsset);

        verify(mockAsset).activateAsync();
    }
    
    /**
     * Verify there are no registry dependencies.
     */
    @Test
    public void testRetrieveRegistryDependencies()
    {
        assertThat(m_SUT.retrieveRegistryDependencies().size(), is(0));
    }
}
