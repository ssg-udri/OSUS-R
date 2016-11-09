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
package mil.dod.th.ose.sdk.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.File;
import java.util.Map;

import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableMap;

/**
 * Utility class used for SDK tests.
 * 
 * @author dhumeniuk
 *
 */
public class SdkUtils
{
    /** Where generated files from the build go. */
    public static final String TARGET_DIR_PROP = "target.dir";
    
    public static final String SDK_ASSET_NAME = "SdkAsset";
    public static final String SDK_SPI_NAME = "SdkSpi";
    public static final String SDK_I2C_NAME = "SdkI2C";
    public static final String SDK_SERIAL_PORT_NAME = "SdkSerialPort";
    public static final String SDK_GPIO_NAME = "SdkGpio";
    public static final String SDK_LINK_LAYER_NAME = "SdkLinkLayer";
    public static final String SDK_TRANS_LAYER_NAME = "SdkTransLayer";

    public static final Map<String, String> PLUGIN_MAP = new ImmutableMap.Builder<String, String>()
        .put(SDK_ASSET_NAME, "example.sdk.asset")
        .put(SDK_SPI_NAME, "example.sdk.spi")
        .put(SDK_I2C_NAME, "example.sdk.i2c")
        .put(SDK_SERIAL_PORT_NAME, "example.sdk.serialport")
        .put(SDK_GPIO_NAME, "example.sdk.gpio")
        .put(SDK_LINK_LAYER_NAME, "example.sdk.link")
        .put(SDK_TRANS_LAYER_NAME, "example.sdk.transport")
        .build();

    public static File getTargetDir(BundleContext context)
    {
        return new File(context.getProperty(SdkUtils.TARGET_DIR_PROP));
    }
    
    public static File getWorkspaceDir(BundleContext context)
    {
        // up one folder from target to get integration project folder, up one more for the workspace
        return new File(getTargetDir(context), "../../");
    }
    
    public static File getPluginDir(BundleContext context, String name)
    {
        File pluginDir = new File(getTargetDir(context), PLUGIN_MAP.get(name));
        assertThat("Must run 'setup' build target to generate SDK plug-ins", pluginDir, isDirectory());
        
        return pluginDir;
    }
    
    /**
     * Get the product type given the simple class name.
     */
    public static String getProductType(String name)
    {
        return PLUGIN_MAP.get(name) + "." + name;
    }
}
