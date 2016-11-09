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
package mil.dod.th.ose.gui.integration.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebElement;

/**
 * Class performs verification of growl messages.
 * 
 * @author nickmarcucci
 *
 */
public class GrowlVerifier
{
    /**
     * Function verifies that the first growl message shown(i.e. the one on the top) has the correct growl title and 
     * therefore verifies that the growl was shown. This method will return immediately once found. This method will 
     * only work while growl is visible.
     * 
     * @param waitTimeSecs
     *  the time in seconds to be set for the web driver wait
     * @param clickElement
     *  the web element to be clicked that will cause a growl message to be displayed.
     * @param growlTitles
     *  a list of titles of the growl messages that the action should cause to be displayed.
     */
    public static void verifyNoWait(final int waitTimeSecs, final WebElement clickElement, 
            final String ...growlTitles)
    {
        verify(GrowlAction.NO_WAIT, waitTimeSecs, clickElement, growlTitles);
    }
    
    /**
     * Function verifies that the first growl message shown(i.e. the one on the top) has the correct growl title and 
     * therefore verifies that the growl was shown. Then it waits until that growl message is no longer visible. This 
     * method will only work while growl is visible.
     * 
     * @param waitTimeSecs
     *  the time in seconds to be set for the web driver wait
     * @param clickElement
     *  the web element to be clicked that will cause a growl message to be displayed.
     * @param growlTitles
     *  a list of titles of the growl messages that the action should cause to be displayed.
     */
    public static void verifyAndWaitToDisappear(final int waitTimeSecs, final WebElement clickElement, 
            final String ...growlTitles)
    {
        verify(GrowlAction.WAIT_TO_DISAPPEAR, waitTimeSecs, clickElement, growlTitles);
    }
    
    private static void verify(final GrowlAction growlAction, final int waitTimeSecs, final WebElement clickElement, 
            final String[] growlTitles)
    {
        assertThat("Must have at least one title to verify", growlTitles.length, is(greaterThan(0)));
        
        final ExecutorService executor = Executors.newFixedThreadPool(growlTitles.length);
        List<FutureTask<Boolean>> resultsList = new ArrayList<FutureTask<Boolean>>();
        //Create a growl checker for each growl message that should appear when the action is performed.
        //Each instance of the growl checker is ran on a separate thread using a future task and the executor service.
        for (String title: growlTitles)
        {
            final FutureTask<Boolean> findMessage = new FutureTask<Boolean>(
                    new GrowlChecker(title, growlAction, waitTimeSecs));
            resultsList.add(findMessage);
            executor.execute(findMessage);
        }
        
        //Perform the action that will cause a growl message to be displayed.
        GeneralHelper.safeClick(clickElement);
  
        //Verify that all expected growl messages were displayed.
        for (int i = 0; i < resultsList.size(); i++)
        {
            final FutureTask<Boolean> result = resultsList.get(i);
            final String title = growlTitles[i];
            boolean foundGrowl;
            try
            {
                if (growlAction == GrowlAction.WAIT_TO_DISAPPEAR)
                {
                    DebugText.pushText("Waiting for growl message [%s] for [%d] seconds, wait for growl to disappear", 
                            title, waitTimeSecs);
                }
                else
                {
                    DebugText.pushText("Waiting for growl message [%s] for [%d] seconds, no wait", title, waitTimeSecs);
                }
                // the underlying thread is the one that is actually using the timeout in a wait, just need to make sure
                // we wait long enough for those timeouts to occur in the thread, the actual time in this wait may be 
                // more than the actual timeout, but only enough time for overhead
                foundGrowl = result.get(waitTimeSecs * 2, TimeUnit.SECONDS);
            }
            catch (TimeoutException | ExecutionException | InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
            finally
            {
                DebugText.popText();
            }
            assertThat("Growl message with title: " + title + " could not be found!", foundGrowl, is(true));
        }
        //Shutdown the executor service.
        executor.shutdown();
    }
    
    public enum GrowlAction
    {
        /**
         * Used when waiting for the growl message to disappear before continuing.
         */
        WAIT_TO_DISAPPEAR, 
        
        /**
         * Used when waiting for the growl message, but stop waiting immediately after that.
         */
        NO_WAIT
    }
}
