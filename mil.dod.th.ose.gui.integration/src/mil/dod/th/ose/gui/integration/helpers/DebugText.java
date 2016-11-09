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

import java.util.Stack;

import org.openqa.selenium.JavascriptExecutor;

/**
 * @author dhumeniuk
 *
 */
public class DebugText
{
    private static Stack<String> m_DebugStack = new Stack<>();
    
    public static void pushText(String text, Object ...args)
    {
        m_DebugStack.push(String.format(text, args));        
        updateDebugText();
    }

    public static void popText()
    {
        m_DebugStack.pop();  
        updateDebugText();
    }
    
    public static void replaceText(String text)
    {
        m_DebugStack.pop();
        m_DebugStack.push(text);
        updateDebugText();
    }
    
    /**
     * Set debug text based on top stack item.
     */
    static void updateDebugText()
    {
        String msg = m_DebugStack.empty() ? "" : m_DebugStack.peek();
        
        JavascriptExecutor js = (JavascriptExecutor)WebDriverFactory.retrieveWebDriver(); 
        js.executeScript("$(\"[id='debugOutput']\").text(arguments[0]);", msg); 
    }
}
