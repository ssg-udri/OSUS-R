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
package mil.dod.th.ose.gui.webapp.asset;

import mil.dod.th.core.asset.capability.AssetCapabilities;

/**
 * Model for asset factories.
 * @author callen
 *
 */
public class AssetFactoryModel
{
    /**
     * The fully qualified name of the asset type this factory creates.
     */
    private final String m_FullyQualifiedAssetType;
    
    /**
     * The reference to the asset image class.
     */
    private final AssetImage m_AssetImage;
    
    /**
     * The human readable name of the products this factory produces.
     */
    private final String m_ProductName;
    
    /**
     * The default capabilities that of the products this factory produces.
     */
    private AssetCapabilities m_FactoryCaps;
    
 
    /**
     * The public constructor for the factory model.
     * @param productType
     *     runtime type of the product that the asset factory shall produce
     * @param imageInterface
     *     the image display interface to use.
     */
    public AssetFactoryModel(final String productType, final AssetImage imageInterface)
    {
        super();
        m_FullyQualifiedAssetType = productType;
        m_ProductName = "";
        m_AssetImage = imageInterface;
    }
    
    /**
     * Get the fully qualified name of the products this represented factory creates.
     * @return
     *     runtime type of the product that the asset factory shall produce
     *     
     */
    public String getFullyQualifiedAssetType()
    {
        return m_FullyQualifiedAssetType;
    }
    
    /**
     * Get the human readable name of the product this factory produces.
     * @return
     *     the human readable name of the product this factory produces
     */
    public String getProductName()
    {
        return m_ProductName;
    }
    
    /**
     * Function to set the default capabilities of the product this factory
     * produces.
     * @param capabilities
     *  the default capabilities for this factory
     */
    public void setFactoryCaps(final AssetCapabilities capabilities)
    {
        m_FactoryCaps = capabilities;
    }
    
    /**
     * Get the default capabilities of the product this factory produces.
     * @return
     *   the default capabilities if set. If capabilities have not been
     *   set null is returned.
     */
    public AssetCapabilities getFactoryCaps()
    {
        return m_FactoryCaps;
    }
    
    /**
     * Get the simple name of the product type that this factory creates.
     * @return
     *     simple name rendition of the FQN 
     */
    public String getSimpleType()
    {
        //the last entry should be the simple name of the produce type
        final String[] subStrings = m_FullyQualifiedAssetType.split("\\.");
        return subStrings[subStrings.length - 1];
    }
    
    /**
     * Get the image for the asset factory.
     * @return
     *      the URL of the image
     */
    public String getImage()
    {
        return m_AssetImage.getImage(getFactoryCaps());
    }
}
