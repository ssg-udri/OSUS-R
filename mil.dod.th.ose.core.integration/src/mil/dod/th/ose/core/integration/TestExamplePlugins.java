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

package mil.dod.th.ose.core.integration;

import java.util.List;

import junit.framework.TestCase;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.ImmutableList;

import example.asset.ExampleAsset;
import example.asset.exception.ExampleExceptionAsset;
import example.asset.exception.ExampleObsExAsset;
import example.asset.exception.ExampleSlowUpdateAsset;
import example.asset.gui.ExampleUpdateAsset;
import example.asset.lexicon.ExampleCommandAsset;
import example.asset.lexicon.ExampleObservationsAsset;
import example.asset.lexicon.ExampleRelatedObsAsset;

/**
 * Verifies example plug-ins work. Just an easy way to test without having to run all the different tests like GUI 
 * and remote integration.
 * 
 * @author dhumeniuk
 *
 */
public class TestExamplePlugins extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    private List<String> expectedAssetTypes = ImmutableList.of(
            ExampleExceptionAsset.class.getName(),
            ExampleAsset.class.getName(),
            ExampleSlowUpdateAsset.class.getName(),
            ExampleUpdateAsset.class.getName(),
            ExampleCommandAsset.class.getName(),
            ExampleObservationsAsset.class.getName(),
            ExampleRelatedObsAsset.class.getName(),
            ExampleObsExAsset.class.getName());

    @Override
    public void tearDown() throws Exception
    {
        AssetUtils.deleteAllAssets(m_Context);
    }
    
    /**
     * Verify plug-ins successfully registered.
     */
    public void testAssetsAvailable()
    {
        for (String expectedAssetType : expectedAssetTypes)
        {
            FactoryUtils.assertFactoryDescriptorAvailable(m_Context, expectedAssetType, 3000);
        }
    }
}
