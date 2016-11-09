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
package org.udri.doclet.confluence;

/**
 * Handles all of the Javadoc Tags as defined in the documentation.
 * See download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html
 * 
 * NOTE: If there is an inline or block tag not listed, refer to XMLDoclet's implementation to
 * see if logic has already been written for THOSE
 * 
 * @author Josh Gold
 * 
 */

import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

public enum Tags implements Taglet
{

    SEE("see", false)
    {
        @Override
        public String tagString(Tag tag)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("*See Also:* ");
            if (!tag.toString().contains("http"))
            {
                // Find out if we are dealing with just a class or a class and method
                String[] link = tag.text().split("#");
                // if the class is not part of the API, leave the
                // the package name, but DO NOT make it a link.
                boolean shouldMakeActive = true;
                if (link.length == 2)
                {
                    String className = link[0].trim();
                    int index;
                    if ((index = link[0].lastIndexOf('.')) > -1)
                    {
                        //By Java coding standards, package names are lower case and
                        //class names always start with an uppercase letter.  Let's
                        //check for that.
                        if (Character.isLowerCase(link[0].charAt(0)) && !link[0].startsWith("mil.dod.th.core"))
                        {
                            shouldMakeActive = false;
                        }
                    }

                    if (className.length() == 0)
                    {
                        className = ConfluenceDoclet.getCurrentClassDoc().name();
                    }

                    // Get the method name
                    String method = "";
                    if ((index = link[1].indexOf('(')) > -1)
                    {
                        method = link[1].substring(0, index);
                    }
                    else
                    {
                        method = link[1].trim();
                    }

                    // Get the method parameters
                    String params;
                    if ((index = link[1].indexOf(')')) > -1 && link[1].indexOf('(') != (link[1].indexOf(')') - 1)) {
                        params = link[1].substring(link[1].indexOf('('), link[1].indexOf(')') + 1);
                    } else {
                        params = "";
                    }

                    sb.append("[" + method + params + "|" + ConfluenceDoclet.getPageName(className) + "#" + method + "]");

                    if (shouldMakeActive)
                    {
                        //Add to document links list
                        ConfluenceDoclet.addLinkRef(className + "#" + method, tag.holder());
                    }
                    else
                    {
                        //Clear out everything else and just leave the text
                        sb.setLength(0);
                        sb.append(className).append(".").append(method + params);                    
                    }
                }
                else
                {
                    String className = link[0].trim();
                    if (className.lastIndexOf('.') > -1)
                    {
                        //By Java coding standards, package names are lower case and
                        //class names always start with an uppercase letter.  Let's
                        //check for that.  However, if it is a TH core class, then
                        //include it (and lose the package name)
                        if (Character.isLowerCase(className.charAt(0)))
                        {
                            if (className.startsWith("mil.dod.th.core"))
                            {
                                className = className.substring(className.lastIndexOf('.') + 1);
                            }
                            else
                            {
                                shouldMakeActive = false;
                            }
                        }
                    }
                    sb.append("[").append(className).append("|").append(ConfluenceDoclet.getPageName(className)).append("]");

                    if (shouldMakeActive)
                    {
                        //Add to document links list
                        ConfluenceDoclet.addLinkRef(className, tag.holder());
                    }
                    else
                    {
                        sb.setLength(0);
                        sb.append(className);
                    }
                }
            }
            else
            {
                sb.append(tag.toString().replaceAll("@see:", ""));
            }

            return sb.toString() + "\n";
        }
    },

    DEPRECATED("deprecated", false)
    {

        @Override
        public String tagString(Tag tag)
        {
            StringBuilder sb = new StringBuilder("");
            sb.append("*Deprecated:* ");
            sb.append(tag.text());

            return sb.toString();
        }

    },

    CODE("@code", true)
    {

        @Override
        public String toString(Tag tag)
        {
            return "{{" + tag.text() + "}}";
        }

        @Override
        public String tagString(Tag tag)
        {
            return "{{" + tag.text() + "}}";
        }

    },

    INHERITDOC("@inheritDoc", true)
    {

        @Override
        public String toString(Tag tag)
        {
            if (tag.holder() instanceof MethodDoc)
            {
                MethodDoc mDoc = (MethodDoc)tag.holder();
                ClassDoc cDoc = mDoc.containingClass();
                StringBuilder sb = new StringBuilder("inherited from [");

                // Find the method in a superclass or interface
                String inherited = null;
                for (MethodDoc superM : cDoc.superclass().methods())
                {
                    if (superM.name().equals(mDoc.name()))
                    {
                        inherited = cDoc.superclass().name();
                        break;
                    }
                }
                if (inherited == null)
                {
                    for (ClassDoc iface : cDoc.interfaces())
                    {
                        for (MethodDoc implM : iface.methods())
                        {
                            if (implM.name().equals(mDoc.name()))
                            {
                                inherited = iface.name();
                                break;
                            }
                        }
                        if (inherited != null)
                            break;
                    }
                }
                if (inherited != null)
                {
                    sb.append(mDoc.name() + "()").append("|" + ConfluenceDoclet.getPageName(inherited) + "#").append(mDoc.name()).append("]");
                    return sb.toString();
                }
                else
                {
                    return tag.text();
                }
            }
            else
            {
                return tag.text();
            }
        }

        @Override
        public String tagString(Tag tag)
        {
            return toString(tag);
        }

    },

    LINK("@link", true)
    {
        @Override
        public String toString(Tag tag)
        {
            StringBuilder sb = new StringBuilder();
            // Find out if we are dealing with just a class or a class and method
            String[] link = tag.text().split("#");
            // if the class is not part of the API, leave the
            // the package name, but DO NOT make it a link.
            boolean shouldMakeActive = true;
            if (link.length == 2)
            {
                String className = link[0].trim();
                int index;
                if ((index = link[0].lastIndexOf('.')) > -1)
                {
                    //By Java coding standards, package names are lower case and
                    //class names always start with an uppercase letter.  Let's
                    //check for that.
                    if (Character.isLowerCase(link[0].charAt(0)) && !link[0].startsWith("mil.dod.th.core"))
                    {
                        shouldMakeActive = false;
                    }
                }

                if (className.length() == 0)
                {
                    className = ConfluenceDoclet.getCurrentClassDoc().name();
                }

                // Get the method name
                String method = "";
                if ((index = link[1].indexOf('(')) > -1)
                {
                    method = link[1].substring(0, index);
                }
                else
                {
                    method = link[1].trim();
                }

                // Get the method parameters
                String params;
                if ((index = link[1].indexOf(')')) > -1 && link[1].indexOf('(') != (link[1].indexOf(')') - 1)) 
                {
                    params = link[1].substring(link[1].indexOf('('), link[1].indexOf(')') + 1);
                } 
                else 
                {
                    params = "";
                }

                sb.append("[" + method + params + "|" + ConfluenceDoclet.getPageName(className) + "#" + method + "]");

                if (shouldMakeActive)
                {
                    //Add to document links list
                    ConfluenceDoclet.addLinkRef(className + "#" + method, tag.holder());
                }
                else
                {
                    //Clear out everything else and just leave the text
                    sb.setLength(0);
                    sb.append(className).append(".").append(method + params);                    
                }
            }
            else
            {
                String className = link[0].trim();
                if (className.lastIndexOf('.') > -1)
                {
                    //By Java coding standards, package names are lower case and
                    //class names always start with an uppercase letter.  Let's
                    //check for that.  However, if it is a TH core class, then
                    //include it (and lose the package name)
                    if (Character.isLowerCase(className.charAt(0)))
                    {
                        if (className.startsWith("mil.dod.th.core"))
                        {
                            className = className.substring(className.lastIndexOf('.') + 1);
                        }
                        else
                        {
                            shouldMakeActive = false;
                        }
                    }
                }

                sb.append("[").append(className).append("|").append(ConfluenceDoclet.getPageName(className)).append("]");

                if (shouldMakeActive)
                {
                    //Add to document links list
                    ConfluenceDoclet.addLinkRef(className, tag.holder());
                }
                else
                {
                    sb.setLength(0);
                    sb.append(className);
                }
            }

            return sb.toString();
        }

        @Override
        public String tagString(Tag tag)
        {
            return toString(tag);
        }
    };

    /**
     * The name of the tag
     */
    private final String _name;

    /**
     * Whether this Taglet is inline
     */
    private final boolean _isInline;

    Tags(String name, boolean isInline)
    {
        _name = name;
        _isInline = isInline;
    }

    public abstract String tagString(Tag tag);

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public boolean isInlineTag()
    {
        return _isInline;
    }

    @Override
    public boolean inConstructor()
    {
        return true;
    }

    @Override
    public boolean inField()
    {
        return true;
    }

    @Override
    public boolean inMethod()
    {
        return true;
    }

    @Override
    public boolean inOverview()
    {
        return true;
    }

    @Override
    public boolean inPackage()
    {
        return true;
    }

    @Override
    public boolean inType()
    {
        return true;
    }

    @Override
    public String toString(Tag tag)
    {
        return tag.text();
    }

    @Override
    public String toString(Tag[] tags)
    {
        StringBuilder out = new StringBuilder();
        for (Tag t : tags)
        {
            out.append(toString(t));
            if (!t.equals(tags[tags.length]))
                out.append(",");
        }
        return out.toString();
    }

    /**
     * Register the Taglets.
     * 
     * @param tagletMap
     *            the map to register this tag to.
     */
    public static void register(Map<String, Taglet> tagletMap)
    {
        for (Taglet tag : Tags.values())
        {
            tagletMap.put(tag.getName(), tag);
        }
    }
}
