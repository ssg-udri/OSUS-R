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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * Primary, top level thread used to manage the scanning process.
 */
public class ScannerManagerThread implements Runnable // TD: Executor should be used instead of low level threads
// The use of executors would also help introduce Future objects that could be used to report errors back from scanners
{
    /**
     * Asset scanners that should be processed for this instance.
     */
    private final Set<AssetScanner> m_ScannerList;

    /**
     * Reference to the asset directory service.
     */
    private final AssetDirectoryService m_AssetDirectoryService;

    /**
     * Scanner manager listener used to report scanning progress.
     */
    private final ScannerManagerListener m_ScanListener;

    /**
     * The semaphore that is updated by the scanner manager listener and causes this object to wait until all scanning
     * threads have completed.
     */
    private Semaphore m_Semaphore;

    /**
     * Constructor used for creating new scanner manager threads.
     * 
     * @param scannerList
     *            Asset scanners to be processed by this thread
     * @param assetDirService
     *            Reference to the asset directory service
     * @param scannerManagerListener
     *            Listener callback used to notify upon completion of scanning events
     */
    public ScannerManagerThread(final Set<AssetScanner> scannerList, final AssetDirectoryService assetDirService,
            final ScannerManagerListener scannerManagerListener)
    {
        m_ScannerList = scannerList;
        m_AssetDirectoryService = assetDirService;
        m_ScanListener = scannerManagerListener;
    }

    @Override
    public void run()
    {
        Logging.log(LogService.LOG_INFO, "Started thread for scanner manager");

        final List<Thread> threads = new ArrayList<>();

        m_Semaphore = new Semaphore(0);
        final ScannerListener listener = new ScannerListener()
        {
            @Override
            public void scannerStartedScanning(final Class<? extends AssetProxy> productType)
            {
                // pass event on up to listener
                m_ScanListener.scannerStartedScanning(productType);
            }

            @Override
            public void scanCompleteForType(final Class<? extends AssetProxy> productType,
                    final List<ScanResultsData> scanResults)
            {
                // pass event on up to listener
                m_ScanListener.scanCompleteForType(productType, scanResults);
                m_Semaphore.release();
            }
        };

        for (AssetScanner scanner : m_ScannerList)
        {
            // kick off thread to scan for new Assets
            final Scanner runnable = new Scanner(scanner, m_AssetDirectoryService, listener);
            final Thread thread = new Thread(runnable);

            thread.setName(scanner.getClass().getName());
            thread.start();
            threads.add(thread);
        }

        // if semaphore was not successfully acquired then some threads are still running. Therefore, must go through
        // list and find the ones that are and interrupt them.
        if (!runCheckForScanning(threads.size()))
        {
            for (Thread thread : threads)
            {
                if (thread.isAlive())
                {
                    thread.interrupt();
                    Logging.log(LogService.LOG_WARNING, "Had to stop thread scanning for Asset Type %s",
                            thread.getName());
                }
            }
        }
    }

    /**
     * Checks to see if threads are still scanning for assets.
     * 
     * @param numThreads
     *            Number of scanning threads that were created
     * 
     * @return True if semaphore was successfully acquired; false otherwise.
     */
    private boolean runCheckForScanning(final int numThreads)
    {
        boolean acquiredSemaphore = false;
        try
        {
            final int TIMEOUT = 15;
            // wait for each scanner thread as each will release a permit
            acquiredSemaphore = m_Semaphore.tryAcquire(numThreads, TIMEOUT, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Scanning for assets interrupted.");
        }

        // if did not acquire semaphore then not completed scanning within amount of time.
        if (!acquiredSemaphore)
        {
            Logging.log(LogService.LOG_WARNING, "Scanning for assets did not complete. Stopping scanning now.");
        }

        // whether all scans completed successfully or not, the scanning operation is complete.
        m_ScanListener.allScannersCompleted();

        return acquiredSemaphore;
    }
}
