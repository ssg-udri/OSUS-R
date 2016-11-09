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
package mil.dod.th.ose.gui.webapp.advanced;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.proto.BundleMessages;

import org.osgi.framework.Bundle;

/**
 * Class which stores a bundles information.
 * 
 *@author cweisenborn
 */
public class BundleModel
{
    /**
     * String representation of the bundle's symbolic name.
     */
    private String m_SymbolicName;
    
    /**
     * String representation of the bundle vendor.
     */
    private String m_BundleVendor;
    
    /**
     * String representation of the bundle name.
     */
    private String m_BundleName;
    
    /**
     * String representation of the bundle description.
     */
    private String m_Description;
    
    /**
     * String representation of the bundle version number.
     */
    private String m_Version;
    
    /**
     * String representation of the file path location of the bundle on the active controller.
     */
    private String m_Location;
    
    /**
     * List of strings which represents the packages imported by this bundle.
     */
    private List<String> m_ImportPackages;
    
    /**
     * List of strings which represents the packages exported by this bundle.
     */
    private List<String> m_ExportPackages;
    
    /**
     * ID of the bundle.
     */
    private long m_BundleId;
    
    /**
     * String that represents the state of the bundle.
     */
    private String m_State;
    
    /**
     * Constructor method that accepts a GetBundleInfoResponse message to build a bundle model.
     * 
     * @param bundleInfo
     *          The GetBundleInfoResponseData message that contains the information used to construct a bundle
     *          model.
     */
    public BundleModel(final BundleMessages.BundleInfoType bundleInfo)
    {   
        createOrUpdateBundle(bundleInfo);
    }
    
    /**
     * Method used to update a bundle's information.
     * 
     * @param updatedBundle
     *          The bundle containing the updated information.
     */
    public void updateBundle(final BundleMessages.BundleInfoType updatedBundle)
    {        
        createOrUpdateBundle(updatedBundle);
    }
    
    /**
     * Method called when instantiating or updating a bundle.
     * 
     * @param bundleInfo
     *          Bundle info type message containing the bundle data to be set.
     */
    private void createOrUpdateBundle(final BundleMessages.BundleInfoType bundleInfo)
    {
        m_BundleId = bundleInfo.getBundleId();
        m_SymbolicName = bundleInfo.getBundleSymbolicName();
        m_Version = bundleInfo.getBundleVersion();
        m_Location = bundleInfo.getBundleLocation();
        m_Description = bundleInfo.getBundleDescription();
        m_BundleVendor = bundleInfo.getBundleVendor();
        m_BundleName = bundleInfo.getBundleName();
        
        calculateState(bundleInfo.getBundleState());
        setImports(bundleInfo.getPackageImportList());
        setExports(bundleInfo.getPackageExportList()); 
    }
    
    /**
     * Set the bundle's state. This method converts the integer to a string. The integer value should be the enumeration
     * which represents the current state.
     * 
     * @param state
     *          Integer which represents the enumeration of the bundle's current state.
     */
    public void setState(final int state)
    {
        calculateState(state);
    }
    
    /**
     * Retrieve the bundle's symbolic name.
     * 
     * @return
     *          String which represents the bundle's symbolic name.
     */
    public String getSymbolicName()
    {
        return m_SymbolicName;
    }
    
    /**
     * Retrieve the bundle's vendor name.
     * 
     * @return
     *          String which represents the bundle's symbolic name.
     */
    public String getBundleVendor()
    {
        return m_BundleVendor;
    }
    
    /**
     * Retrieve the bundle's name.
     * 
     * @return
     *          String which represents the bundle's name.
     */
    public String getBundleName()
    {
        return m_BundleName;
    }
    
    /**
     * Retrieve the bundle's description.
     * 
     * @return
     *          String which represents the bundle's description.
     */
    public String getDescription()
    {
        return m_Description;
    }
    
    /**
     * Retrieve the bundle's current state.
     * 
     * @return
     *          String which represents the bundle's current state.
     */
    public String getState()
    {
        return m_State;
    }
    
    /**
     * Retrieve the bundle's version number.
     * 
     * @return
     *          String which represents the bundle's version number.
     */
    public String getVersion()
    {
        return m_Version;
    }
    
    /**
     * Retrieve the bundle's location.
     * 
     * @return
     *          String which represents the bundle's location.
     */
    public String getLocation()
    {
        return m_Location;
    }
    
    /**
     * Retrieve the list of strings which represents all packages imported by this bundle.
     * 
     * @return
     *          List of strings which represents the packages imported by this bundle.
     */
    public List<String> getImportPackages()
    {
        return m_ImportPackages;
    }
    
    /**
     * Retrieve the list of strings which represents all packages exported by this bundle.
     * 
     * @return
     *          List of strings which represents the packages exported by this bundle.
     */
    public List<String> getExportPackages()
    {
        return m_ExportPackages;
    }
    
    /**
     * Retrieve the bundle's ID.
     * 
     * @return
     *          Returns a long which represents the bundle's ID.
     */
    public long getBundleId()
    {
        return m_BundleId;
    }
    
    /**
     * Method used to convert the integer value of the bundle state enumeration to a string.
     * 
     * @param state
     *          Enumeration representing the state of the bundle.
     */
    private void calculateState(final int state)
    {
        switch (state)
        {
            case Bundle.INSTALLED:
                m_State = "Installed";
                break;
            case Bundle.RESOLVED:
                m_State = "Resolved";
                break;
            case Bundle.STARTING:
                m_State = "Starting";
                break;
            case Bundle.STOPPING:
                m_State = "Stopping";
                break;
            case Bundle.ACTIVE:
                m_State = "Active";
                break;
            case Bundle.UNINSTALLED:
                m_State = "Uninstalled";
                break;
            default:
                m_State = "State Could Not Be Determined";
                break;
        }
    }
    
    /**
     * Method used to set the list of imported packages when the bundle model is initially created.
     * 
     * @param importList
     *          List of strings which contains all packages imported by the bundle.
     */
    private void setImports(final List<String> importList)
    {   
        if (importList.isEmpty())
        {  
            m_ImportPackages = new ArrayList<String>();
            m_ImportPackages.add("This bundle does not import any packages.");
        }
        else
        {
            m_ImportPackages = new ArrayList<String>(importList);
        }
    }
    
    /**
     * Method used to set the list of exported packages when the bundle model is initially created.
     * 
     * @param exportList
     *          List of strings which contains all packages exported by the bundle.
     */
    private void setExports(final List<String> exportList)
    { 
        if (exportList.isEmpty())
        {
            m_ExportPackages = new ArrayList<String>();
            m_ExportPackages.add("This bundle does not export any packages.");
        }
        else
        {
            m_ExportPackages = new ArrayList<String>(exportList);
        }
    }
}
