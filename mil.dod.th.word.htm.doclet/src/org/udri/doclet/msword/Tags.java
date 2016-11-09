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
package org.udri.doclet.msword;

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
        public String toMSHTML(Tag tag)
        {
            String text = tag.text();
            if (text.startsWith("#"))
            {
                text = text.substring(1);
            }

            String linkText = text;
            int index = linkText.indexOf('(');
            if (index > 0)
            {
                linkText = linkText.substring(0, index);
            }
            String className = MSHTMLDoclet.getCurrentClassDoc().name();

            StringBuilder sb = new StringBuilder(
                    "<p class=MsoNormal style='margin-top:10.0pt;margin-right:0in;margin-bottom:");
            sb.append("12.5pt;margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>See Also:</b><span");
            sb.append("style='mso-tab-count:1'>&nbsp;&nbsp;&nbsp; </span><a href=\"#").append(className).append(".").append(
                    linkText).append("\">");
            sb.append(text).append("</a></p>");

            return sb.toString();
        }

    },

    DEPRECATED("deprecated", false)
    {

        @Override
        public String toMSHTML(Tag tag)
        {
            StringBuilder sb = new StringBuilder(
                    "<p class=MsoNormal style='margin-top:10.0pt;margin-right:0in;margin-bottom:");
            sb.append("12.5pt;margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>Deprecated:</b><span");
            sb.append("style='mso-tab-count:1'>&nbsp;&nbsp;&nbsp; </span><span class=SpellE><span class=GramE>").append(
                    tag.text());
            sb.append("</span></span></p>");

            return sb.toString();
        }

    },

    CODE("@code", true)
    {

        @Override
        public String toString(Tag tag)
        {
            return "<code><![CDATA[" + tag.text() + "]]></code>";
        }

        @Override
        public String toMSHTML(Tag tag)
        {
            return "<span style='font-family:Courier'>" + tag.text() + "</span>";
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
                StringBuilder sb = new StringBuilder("inherited from <a href=\"#");

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
                    sb.append(inherited).append(".").append(mDoc.name()).append("\">");
                    sb.append(inherited).append(".").append(mDoc.name()).append("()</a>");
                    return sb.toString();
                }
                else
                {
                    return "<div class=\"inherited\">" + tag.text() + "</div>";
                }
            }
            else
            {
                return "<div class=\"inherited\">" + tag.text() + "</div>";
            }
        }

        @Override
        public String toMSHTML(Tag tag)
        {
            return toString(tag);
        }

    },

    LINK("@link", true)
    {
        @Override
        public String toString(Tag tag)
        {
            // Find out if we are dealing with just a class or a class and
            // method
            StringBuilder sb = new StringBuilder();
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

                boolean intraClassLink = false;
                if (className.length() == 0)
                {
                    intraClassLink = true;
                    className = MSHTMLDoclet.getCurrentClassDoc().name(); // tag.holder().name();
                }

                sb.append("<a href=\"#");
                sb.append(className);

                String method;
                if ((index = link[1].indexOf('(')) > -1)
                {
                    method = link[1].substring(0, index);
                }
                else
                {
                    method = link[1].trim();
                }
                String methodWithParms;
                if ((index = link[1].indexOf(')')) > -1)
                {
                    methodWithParms = link[1].substring(0, index + 1);
                }
                else
                {
                    methodWithParms = link[1];
                }

                sb.append(".").append(method);
                sb.append("\">");

                if (intraClassLink || tag.holder() == null)
                {
                    sb.append(methodWithParms);
                }
                else
                {
                    sb.append(className).append(".").append(methodWithParms);
                }

                sb.append("</a>");

                if (shouldMakeActive)
                {
                    //Add to document links list
                    MSHTMLDoclet.addLinkRef(className + "." + method, tag.holder());
                }
                else
                {
                    //Clear out everything else and just leave the text
                    sb.setLength(0);
                    sb.append(className).append(".").append(methodWithParms);                    
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

                sb.append("<a href=\"#").append(className).append("\">");

                sb.append(className);

                sb.append("</a>");

                if (shouldMakeActive)
                {
                    //Add to document links list
                    MSHTMLDoclet.addLinkRef(className, tag.holder());
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
        public String toMSHTML(Tag tag)
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

    public abstract String toMSHTML(Tag tag);

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
