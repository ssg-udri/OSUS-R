package ${package};

import java.util.Set;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService.ScanResults;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.factory.ProductType;

/**
 * ${task}: Optional class. Implement the scanForNewAssets method or remove this class if scanning is not supported.
 *
 * ${description} scanner implementation.
 *
 * @author ${author}
 */
@Component
@ProductType(${class}.class)
public class ${class}Scanner implements AssetScanner
{
    @Override
    public void scanForNewAssets(final ScanResults results, final Set<Asset> existing) throws AssetException 
    {
        // ${task}: Handle scanning for assets. This example only creates a new
        //       asset by scan if no instances already exists   
        // if (existing.isEmpty())
        // {
        //    // retrieve that attributes interface for the default properties
        //    final ${class}Attributes attributes = 
        //        Configurable.createConfigurable(${class}Attributes.class, new HashMap<String, Object>());
        //    // retrieve the default serial port from the attributes interface
        //    final String defaultPort = attributes.serialPort();
        //
        //    // create a dictionary with the properties for the found asset
        //    final Map<String, Object> newAssetProps = new HashMap<String, Object>();
        //    newAssetProps.put("serial.port", defaultPort);
        //    results.found("ExampleAsset-" + defaultPort, newAssetProps);
        // }
        // else
        // {
        //    Logging.log(LogService.LOG_INFO, "Nothing new found");
        // }
    }
}
