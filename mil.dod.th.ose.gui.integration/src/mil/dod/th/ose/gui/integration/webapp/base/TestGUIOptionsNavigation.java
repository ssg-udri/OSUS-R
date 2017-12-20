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
package mil.dod.th.ose.gui.integration.webapp.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.PageNameConstants;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TestGUIOptionsNavigation
{

    private static WebDriver m_Driver;

    @BeforeClass
    public static void beforeClass()
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();

        // navigate to the base URL
        NavigationHelper.openBaseUrl();
    }

    /*
     * Tests to see if Devices -> Assets button navigates to the assets_comms.xhtml page. Side note: All sections go by
     * how they are divided in the left scroll bar, but since the Assets button and Comms button currently share a page
     * they are split up so that navigation will not be to the same page twice
     */
    @Test
    public void testAssetButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);

        // this is needed to make sure that the tests do not end before the page has navigated to the
        // next page. Leaving the below code out will result in all subsequent tests failing because
        // they are a page behind.
        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_ASSET);
            }
        });

        assertThat(result, is(true));
    }

    /**
     * Tests to see if Mission Apps, Add Mission App button navigates to setup_mission.xhtml
     */
    @Test
    public void testAddMissionAppButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
       
        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_MISSION_SETUP);
            }
        });

        assertThat(result, is(true));
    }

    /**
     * Tests to see if Devices, Comms button navigates to assets_comms.xhtml
     */
    @Test
    public void testCommsButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS);

        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_COMMS);
            }
        });

        assertThat(result, is(true));
    }

    /*
     * Tests to see if Mission Apps -> Mission App Status button navigates to missions.xhtml
     */
    @Test  
    public void testMissionAppStatusButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);

        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_MISSION);
            }
        });

        assertThat(result, is(true));
    }

    /*
     * Tests to see if Advanced -> Power Management button navigates to power.xhtml
     */
    @Test
    @Ignore //Button disabled at this time.
    public void testPowerButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_POWER);
        
        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_POWER);
            }
        });

        assertThat(result, is(true));
    }

    /*
     * Tests to see if Advanced -> System Configuration button navigates to systemconfig.xhtml
     */
    @Test
    public void testSystemConfigButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);

        Boolean result = (new WebDriverWait(m_Driver, 5)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_SYS_CONFIG);
            }
        });

        assertThat(result, is(true));
    }

    /*
     * Tests to see if Advanced -> GUI Configuration button navigates to guiconfig.xhtml
     */
    @Test
    public void testGuiConfigButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        
        Boolean result = (new WebDriverWait(m_Driver, 10)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_GUI_CONFIG);
            }
        });

        assertThat(result, is(true));
    }

    /*
     * Tests to see if toolbar Channels button navigates to channels.xhtml
     */
    @Test
    public void testChannelsButton() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);

        Boolean result = (new WebDriverWait(m_Driver, 10)).until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver d)
            {
                return d.getCurrentUrl().contains(PageNameConstants.PAGECONST_PROP_CHANNELS);
            }
        });

        assertThat(result, is(true));
    }
}