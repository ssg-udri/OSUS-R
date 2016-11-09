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

import java.io.IOException;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import com.google.protobuf.ByteString;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BundleMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.service.log.LogService;
import org.primefaces.model.UploadedFile;

/**
 * Implementation of the {@link BundleImportMgr} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "bundleImportMgr")
@ViewScoped
public class BundleImportMgrImpl implements BundleImportMgr 
{
    /**
     * Bundle file to be updated or installed.
    */
    private UploadedFile m_BundleFile;

    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * Service used to display growl messages to the user.
    */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Acquire the bundle config service.
     */
    @ManagedProperty(value = "#{bundleConfigurationMgr}")
    private BundleConfigurationMgr bundleConfigurationMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Boolean used to determine if the bundle should be started when updated or
     * installed.
    */
    private boolean m_StartBundle;

    /*
     * (non-Javadoc)
     * 
     * @see
     * mil.dod.th.ose.gui.webapp.advanced.BundleImportMgr#setStartBundle(boolean
     * )
     */
    @Override
    public void setStartBundle(final boolean startBundle) 
    {
        m_StartBundle = startBundle;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleImportMgr#isStartBundle()
     */
    @Override
    public boolean isStartBundle()
    {
        return m_StartBundle;
    }

    /**
     * Sets the growl message utility.
     * 
     * @param growlUtil
     *            The growl message utility to be set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }

    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }

    /**
     * Sets the {@link BundleConfigurationMgr}.
     * 
     * @param bundleConfigMgr
     *            The service to be set
     */
    public void setBundleConfigurationMgr(final BundleConfigurationMgr bundleConfigMgr)
    {
        bundleConfigurationMgr = bundleConfigMgr;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see
     * mil.dod.th.ose.gui.webapp.advanced.BundleImportMgr#setFile(org.primefaces
     * .model.UploadedFile)
     */
    @Override
    public void setFile(final UploadedFile file)
    {
        m_BundleFile = file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleImportMgr#getFile()
     */
    @Override
    public UploadedFile getFile()
    {
        return m_BundleFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * mil.dod.th.ose.gui.webapp.advanced.BundleImportMgr#handleImportBundle()
     */
    @Override
    public void handleImportBundle(final int controllerId)
    {
        if (m_BundleFile == null)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN,
                    "No file selected!", "No file was selected to "
                    + "install/update! A valid bundle file must first be selected before one can be "
                    + "installed/updated.");
            return;
        }

        Logging.log(LogService.LOG_DEBUG, "Attempting to install/update [%s] (file size=%d)", 
                m_BundleFile.getFileName(), m_BundleFile.getSize());
        
        final ByteString byteString = ByteString.copyFrom(m_BundleFile.getContents());

        Long bundleId = null;

        try 
        {
            bundleId = determineBundleId();
        } 
        catch (final IOException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Bundle Error!", "The bundle: "
                    + m_BundleFile.getFileName()
                    + " you are trying to install/update is either not a valid jar or "
                    + "could not be properly read from.", exception);
            return;
        }

        if (bundleId == -1)
        {
            final BundleMessages.InstallRequestData installRequest = BundleMessages.InstallRequestData.
                    newBuilder().setBundleFile(byteString).
                    setBundleLocation(m_BundleFile.getFileName()).
                    setStartAtInstall(m_StartBundle).build();

            m_MessageFactory.createBundleMessage(BundleMessageType.InstallRequest, installRequest).
                queue(controllerId, null);
        }
        else
        {
            final BundleMessages.UpdateRequestData updateRequest = BundleMessages.UpdateRequestData.
                    newBuilder().setBundleFile(byteString).
                    setBundleId(bundleId).build();

            m_MessageFactory.createBundleMessage(BundleMessageType.UpdateRequest, updateRequest).
                queue(controllerId, null);
        }
    }

    /**
     * Used to determine the ID of bundle and whether or not it is already
     * currently installed. If the bundle is installed then this method returns
     * the ID of the bundle. If the bundle is not currently installed it will
     * return -1 as the bundle ID value.
     * 
     * @return A long which represents the bundle ID on the active controller or
     *         -1 if it is not currently installed on the active controller.
     * @throws IOException
     *             thrown if file is not a valid jar file.
     */
    private Long determineBundleId() throws IOException
    {
        // Create a jar input stream from the file.
        final JarInputStream jis = new JarInputStream(m_BundleFile.getInputstream());

        // Retrieve the manifest from the jar file.
        final Manifest manifest = jis.getManifest();

        // Close the jar input stream.
        jis.close();

        // If no manifest can be retrieved then file is not a valid jar. Will
        // throw null pointer exception later if not
        // handled now.
        if (manifest == null)
        {
            throw new IOException("The bundle does not contain a valid manifest file.");
        }

        // Retrieve the bundle name from the manifest file.
        final String bundleName = manifest.getMainAttributes().getValue(
                "Bundle-SymbolicName");

        final List<BundleModel> bundles = bundleConfigurationMgr.getBundles();

        Long bundleId = Long.valueOf(-1);
        for (BundleModel bundle : bundles)
        {
            if (bundle.getSymbolicName().equals(bundleName))
            {
                bundleId = bundle.getBundleId();
            }
        }

        return bundleId;
    }
}
