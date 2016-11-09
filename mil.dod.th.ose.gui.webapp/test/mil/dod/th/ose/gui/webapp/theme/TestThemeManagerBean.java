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
package mil.dod.th.ose.gui.webapp.theme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class TestThemeManagerBean
{
    /*
     * Test initialization of ThemeManagerBean.
     */
    @Test
    public void testInitThemeBean()
    {
        ThemeManagerBean bean = new ThemeManagerBean();
        
        assertThat(bean, is(notNullValue()));

        assertThat(bean.getTheme(), is("TH-Day"));
    }

    /*
     * Tests setting a new theme aside from the default one.
     */
    @Test
    public void testSetTheme()
    {
        ThemeManagerBean bean = new ThemeManagerBean();

        bean.setTheme("star-day");

        assertThat(bean.getTheme(), is("star-day"));
    }

    /*
     * Tests setting the theme to null and verifies that it still has a value.
     */
    @Test
    public void testSetThemeNull()
    {
        ThemeManagerBean bean = new ThemeManagerBean();

        bean.setTheme(null);

        assertThat(bean.getTheme(), is("TH-Day"));
    }

}
