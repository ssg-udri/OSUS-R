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
package mil.dod.th.ose.gui.integration.by;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;

import com.google.common.collect.Iterables;

/**
 * A series of {@link By} objects chained together to find an element. By chains can be used instead of {@link 
 * WebElement#findElement(By)} which can lead to errors if the base element is no longer valid. By chains will started
 * from the beginning of the chain each time an element is needed.
 * 
 * @author dhumeniuk
 *
 */
public abstract class ByChain
{
    protected List<ByElement> m_Chain;
    
    /**
     * Create a chain with a given list of elements.
     */
    public ByChain(List<ByElement> chain)
    {
        m_Chain = chain;
    }

    public static EmptyByChainBuilder newBuilder()
    {
        return new EmptyByChainBuilder();
    }
    
    /**
     * Internal methods that will return either a single {@link WebElement} object or a list of them depending on the 
     * last element in the chain.
     */
    protected Object findElementOrElements()
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        return new FluentWait<WebDriver>(driver)
            .withTimeout(30, TimeUnit.SECONDS)
            .pollingEvery(1, TimeUnit.SECONDS)
            .ignoring(StaleElementReferenceException.class)
            .until(new ExpectedCondition<Object>()
            {
                @Override
                public Object apply(WebDriver driver)
                {   
                    WebElement element = null;
                    List<WebElement> elements = null;
                    boolean firstElement = true;
                    for (ByElement byElement : m_Chain)
                    {
                        if (firstElement)
                        {
                            switch (byElement.getType())
                            {
                                case SINGLE:
                                    element = driver.findElement(byElement.getSelector()); 
                                    elements = null; // make sure next part of chain uses element
                                    break;
                                    
                                case MULTIPLE:
                                    elements = driver.findElements(byElement.getSelector());
                                    element = null; // make sure next part of chain uses elements
                                    break;
                                    
                                case INDEX:
                                    throw new IllegalStateException("Index can't be the first element");
                            }
                            firstElement = false;
                        }
                        else
                        {
                            switch (byElement.getType())
                            {
                                case SINGLE:
                                    element = element.findElement(byElement.getSelector()); 
                                    elements = null; // make sure next part of chain uses element
                                    break;
                                    
                                case MULTIPLE:
                                    elements = element.findElements(byElement.getSelector());
                                    element = null; // make sure next part of chain uses elements
                                    break;
                                    
                                case INDEX:
                                    element = elements.get(byElement.getIndex());
                                    elements = null; // make sure next part of chain uses elment
                                    break;
                            }
                        }
                    }
                    
                    if (Iterables.getLast(m_Chain).getType() == ByType.MULTIPLE)
                    {
                        return elements;
                    }
                    else
                    {
                        return element;
                    }
                }
            });
    }
    
    public static class SingleByChain extends ByChain
    {
        public SingleByChain(List<ByElement> chain)
        {
            super(chain);
        }

        /**
         * Find elements given the chain of selectors.
         */
        public WebElement findElement()
        {
            return (WebElement)findElementOrElements();
        }
        
        public SingleByChainBuilder extendChain()
        {
            return new SingleByChainBuilder(m_Chain);
        }
    }
     
    public static class MultipleByChain extends ByChain
    {
        public MultipleByChain(List<ByElement> chain)
        {
            super(chain);
        }

        /**
         * Find elements given the chain of selectors.
         */
        @SuppressWarnings("unchecked")
        public List<WebElement> findElements()
        {
            return (List<WebElement>)findElementOrElements();
        }
        
        public MultipleByChainBuilder extendChain()
        {
            return new MultipleByChainBuilder(m_Chain);
        }
    }
    
    public static abstract class ByChainBuilder
    {
        protected List<ByElement> m_Chain;
        
        /**
         * Construct a new chain builder with existing elements.
         */
        public ByChainBuilder(List<ByElement> chain)
        {
            m_Chain = new ArrayList<>(chain);
        }

        /**
         * Construct a new chain builder with no elements.
         */
        public ByChainBuilder()
        {
            m_Chain = new ArrayList<>();
        }
    }
    
    public static class EmptyByChainBuilder extends ByChainBuilder
    {
        public EmptyByChainBuilder single(By selector)
        {
            m_Chain.add(new ByElement(selector, ByType.SINGLE));
            return this;
        }
        
        public MultipleByChainBuilder multiple(By selector)
        {
            m_Chain.add(new ByElement(selector, ByType.MULTIPLE));
            return new MultipleByChainBuilder(m_Chain);
        }
    }
    
    public static class SingleByChainBuilder extends EmptyByChainBuilder
    {
        public SingleByChainBuilder(List<ByElement> chain)
        {
            m_Chain = new ArrayList<>(chain);
        }

        public SingleByChain build()
        {
            return new SingleByChain(m_Chain);
        }
    }
    
    public static class MultipleByChainBuilder extends ByChainBuilder
    {
        public MultipleByChainBuilder(List<ByElement> chain)
        {
            m_Chain = new ArrayList<>(chain);
        }

        public SingleByChainBuilder index(int index)
        {
            m_Chain.add(new ByElement(index));
            return new SingleByChainBuilder(m_Chain);
        }

        public MultipleByChain build()
        {
            return new MultipleByChain(m_Chain);
        }
    }

    private static class ByElement
    {
        private ByType m_Type;
        private By m_Selector;
        private Integer m_Index;

        ByElement(By selector, ByType type)
        {
            m_Type = type;
            m_Selector = selector;
        }

        ByElement(int index)
        {
            m_Type = ByType.INDEX;
            m_Index = index;
        }

        public int getIndex()
        {
            if (m_Type == ByType.INDEX)
            {
                return m_Index;
            }
            else
            {
                throw new IllegalStateException("Can't call method on non-index element");
            }
        }

        public ByType getType()
        {
            return m_Type;
        }

        public By getSelector()
        {
            if (m_Type == ByType.INDEX)
            {
                throw new IllegalStateException("Can't call method on index element");
            }
            else
            {
                return m_Selector;
            }
        }
    }
    
    private enum ByType
    {
        SINGLE, MULTIPLE, INDEX
    }
}
