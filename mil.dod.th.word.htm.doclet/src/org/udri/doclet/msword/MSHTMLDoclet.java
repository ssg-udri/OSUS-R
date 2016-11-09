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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.udri.util.XSDAppender;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;
import com.sun.javadoc.WildcardType;
import com.sun.tools.doclets.Taglet;
import com.sun.tools.doclets.formats.html.ConfigurationImpl;
import com.sun.tools.doclets.internal.toolkit.util.TextTag;
import com.sun.tools.doclets.internal.toolkit.util.Util;

/**
 * Generate an HTML Javadoc compatible with MS Word 2003 and above.
 * 
 * @author Josh
 * 
 */
public final class MSHTMLDoclet extends Doclet {

    private static final String TH_MARKER_START = "INSERT_API";
    private static final String TH_MARKER_END = "END_API";

    private static final String PRIVATE = "private";
    private static final String PROTECTED = "protected";
    private static final String PUBLIC = "public";
    private static final String PKG_PRIVATE = "package_private";

    private static final String[] IGNOREABLE_ANNOTATIONS = { "javax.xml.bind.annotation" };

    private static final int LINK_MAX_LEN = 39;

    /**
     * Some Taglets need to reference the current ClassDoc, i.e., inline links.
     * Provide it here for all to consume.
     */
    private static ClassDoc CURRENT_CLASSDOC;

    public static ClassDoc getCurrentClassDoc() {
        return CURRENT_CLASSDOC;
    }

    /**
     * The set of intra-API links created while the API was generated. By
     * comparing this to the doc anchors created, we can determine if there are
     * bad references out there.
     */
    private static Map<String, Doc> _docLinkRefs = new HashMap<String, Doc>();

    /**
     * The set of doc anchors (reference points) created in the API. This
     * consists of classes, methods, and fields.
     */
    private static Set<String> _docLinks = new HashSet<String>();

    /**
     * All references are handled by the @link item of the {@link Tags} enum.
     */
    public static void addLinkRef(String docLinkRef, Doc indicator) {
        _docLinkRefs.put(docLinkRef, indicator);
    }

    private static PrintWriter _msHTM;
    private static Options _options = null;

    /**
     * Starting point for the JavaDoc documentation process.
     * 
     * @see com.sun.javadoc.Doclet#start(RootDoc)
     * 
     * @param root
     *            The root of the documentation tree.
     * 
     * @return <code>true</code> if success
     */
    public static boolean start(RootDoc root) {
        // Create the MS Word compatible *.htm file
        try {
            // Initialize writer and write out beginning HTML and CSS from
            // template
            _msHTM = new PrintWriter(new File(_options.getFilename()));

            if (_options.getInputFile() == null) {
                throw new FileNotFoundException(
                        "Could not find input file param.  Please add -inputFile param to Javadoc task.");
            }
            File inputFile = new File(_options.getInputFile());
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(TH_MARKER_START)) {
                    break;
                }

                _msHTM.println(line);
            }
            _msHTM.flush();

            // Continue to read the existing API until we get to the end of the
            // API Section
            while ((line = br.readLine()) != null) {
                if (line.contains(TH_MARKER_END)) {
                    break;
                }
            }

            // Now write out new API
            startClassDocs(root);

            // Look for XSD
            while ((line = br.readLine()) != null) {
                if (line.contains(XSDAppender.TH_XSD_MARKER_START)) {
                    break;
                }

                _msHTM.println(line);
            }

            // Continue to read the existing API until we get to the end of the
            // XSD section
            while ((line = br.readLine()) != null) {
                if (line.contains(XSDAppender.TH_XSD_MARKER_END)) {
                    break;
                }
            }

            // write XSDs
            if (_options.getXsdDirectory() != null) {
                File resourcesDir = new File(_options.getXsdDirectory());
                XSDAppender xsd = new XSDAppender(_msHTM);
                xsd.writeXSDs(resourcesDir);
            }

            // Finish writing wrapper HTML
            while ((line = br.readLine()) != null) {
                _msHTM.println(line);
            }
            _msHTM.flush();

            br.close();
            _msHTM.close();

            //Report broken links within the API
            runLinkReport();

        } catch (Throwable t) {
            t.printStackTrace();

            if (_msHTM != null) {
                _msHTM.close();
            }

            return false;
        }

        return true;
    }

    /**
     * Run the report and create the output file to write the invalid links
     * report
     */
    private static void runLinkReport() {
        // Initialize writer and write out beginning HTML and CSS from template
        String brokenlinksreport = _options.getBrokenLinksReportFile();
        if (brokenlinksreport == null) {
            return;
        }

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(brokenlinksreport));
            pw.println("LINK NOT FOUND, Containing Element");

            for (String linkRef : _docLinkRefs.keySet()) {
                if (!_docLinks.contains(linkRef)) {
                    Doc doc = _docLinkRefs.get(linkRef);
                    String element = null;
                    if (doc.isClass()) {
                        ClassDoc c = (ClassDoc) doc;
                        element = c.qualifiedName();
                    } else if (doc.isConstructor() || doc.isMethod()
                            || doc.isField() || doc.isEnum()) {
                        MemberDoc m = (MemberDoc) doc;
                        element = m.containingClass().qualifiedName() + m.name();
                    } else {
                        element = "Unknown Link Holder JavaDoc type: "
                                + doc.toString();
                    }
                    pw.println(linkRef + "," + element);
                    System.err.println("Broken Link: Missing '" + linkRef
                            + "' in " + element);
                }
            }
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null)
                pw.close();
        }
    }

    /**
     * Part of Doclet API. Must be implemented.
     */
    public static boolean validOptions(String o[][], DocErrorReporter reporter) {
        _options = Options.toOptions(o, reporter);
        // OK if we could set up the options.
        return _options != null;
    }

    /**
     * Part of Doclet API. Must be implemented.
     */
    public static int optionLength(String option) {
        System.out.println(option);
        return Options.getLength(option);
    }

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    private static void startClassDocs(RootDoc root) {
        PackageDoc currentPkg = null;

        // Sort the packages. They do not come sorted in root.classes().
        Comparator<ClassDoc> classComparator = new Comparator<ClassDoc>(){
            @Override
            public int compare(ClassDoc o1, ClassDoc o2){
                // Sort by package first. There is at least one case (CompositeAsset) that will be wrong without this.
                int compareMe = o1.containingPackage().toString().compareToIgnoreCase(o2.containingPackage().toString());
                // Packages are the same
                if(0 == compareMe){
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
                return compareMe;
            }
        };
        PriorityQueue<ClassDoc> classQueue= new PriorityQueue<ClassDoc>(root.classes().length, classComparator);
        for (ClassDoc doc : root.classes()){
            classQueue.add(doc);
        }
        // Iterate over the classes. Note that Iterators, even for a PriorityQueue, do NOT guarantee an order.
        while (!classQueue.isEmpty()) {
            ClassDoc doc  = classQueue.poll();
            PackageDoc pkg = doc.containingPackage();
            if (currentPkg == null || !currentPkg.name().equals(pkg.name())){
                currentPkg = pkg;
                writePackage(pkg);
            }
            writeClass(doc);

            _msHTM.flush();
        }
    }

    /**
     * Write Package info
     */
    private static void writePackage(PackageDoc pkg) {
        StringBuilder pkgHTM = new StringBuilder(
                "<div style='mso-element:para-border-div;border-top:double windowtext 1.5pt;")
        .append("border-left:none;border-bottom:double windowtext 1.5pt;border-right:none;")
        .append("padding:1.0pt 0in 1.0pt 0in'>");

        // Add intra-document link point
        _docLinks.add(pkg.name());
        pkgHTM.append("<h2>")
        .append("<a name=\"")
        .append(pkg.name())
        .append("\"></a><![if !supportLists]><span style='mso-list:Ignore'>")
        .append("<span style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp;&nbsp;&nbsp; </span></span><![endif]>")
        .append(pkg.name()).append("</h2></div>");

        _msHTM.println(pkgHTM);

        writeAnnotations(pkg, pkg.annotations());
        writeComment(pkg);
        writeTags(pkg);
    }

    /**
     * 
     * @param doc
     *            The Doc object.
     */
    private static void writeComment(final Doc doc) {
        if (doc.commentText() == null || doc.commentText().length() == 0) {
            return;
        }

        final StringBuilder comment = new StringBuilder();

        // Analyze each token and produce comment node
        for (Tag t : doc.inlineTags()) {
            final Taglet taglet = _options.getTagletForName(t.name());
            if (taglet != null) {
                comment.append(taglet.toString(t));
            } else {
                comment.append(t.text());
            }
        }

        if (comment.length() > 0) {
            _msHTM.println("<div class=MsoNormal style='margin-left:50.0pt'>");
            _msHTM.println(comment.toString());
            _msHTM.println("</div>");
        }
    }

    /**
     * Write comment for overriding method
     */
    private static void writeMethodOverrideComment(MethodDoc method) {
        final StringBuilder comment = new StringBuilder();

        // Analyze each token and produce comment node
        for (Tag t : method.inlineTags()) {
            final Taglet taglet = _options.getTagletForName(t.name());
            if (taglet != null) {
                comment.append(taglet.toString(t));
            } else {
                comment.append(t.text());
            }
        }

        if (comment.length() > 0) {
            String classLink = "<a href=\"#" + method.containingClass().name()
                    + "\">" + method.containingClass().name() + "</a>";
            _msHTM.println("<div class=MsoNormal style='margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>");
            _msHTM.println("Description copied from: </b>");
            _msHTM.println(classLink);
            _msHTM.println(". <a href=\"#" + method.containingClass().name() + method.name() + "\">" + method.name() 
                    + "</a>");

            _msHTM.println("</div>");

            _msHTM.println("<div class=MsoNormal style='margin-left:50.0pt'>");
            _msHTM.println(comment.toString());
            _msHTM.println("</div>");
        }
    }

    private static void writeAnnotations(Doc doc, AnnotationDesc[] annotations) {
        if (annotations.length == 0) {
            return;
        }

        StringBuilder annotationStr = new StringBuilder("");
        for (AnnotationDesc annotation : annotations) {
            if (annotation == null)
                return;

            if (isIgnoreableAnno(annotation.annotationType().qualifiedName())) {
                continue;
            }

            String name = null;
            try {
                name = annotation.annotationType().name();
            } catch (ClassCastException cce) {
                System.err.println("Error retrieving Annotation Type: ");
                cce.printStackTrace(System.err);
            }

            // Rename aQute BND @Version annotation. --jkg
            boolean isBndVersionTag = false;
            if (name.equals("Version")) {
                name = "Package Version";
                isBndVersionTag = true;
            }

            // Change annotation to mimic Java
            if (name.equals("Override")) {
                name = "Overrides";

                if (doc.isMethod()) {
                    MethodDoc method = (MethodDoc) doc;

                    String methodName = "";
                    String className = "";
                    // If not overriding class, assume it's in an interface
                    if (method.overriddenClass() == null) {
                        method = findOverrideMethod(method.containingClass(), method);
                        className = method.containingClass().name();
                        methodName = method.name();
                        name = "Specified by";
                    } else {
                        className = method.overriddenClass().name();
                        methodName = method.overriddenMethod().name();
                    }

                    annotationStr
                    .append("<p class=MsoNormal style='margin-top:10.0pt;margin-right:0in;margin-bottom:")
                    .append("12.5pt;margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>")
                    .append(name)
                    .append("</b><span style='mso-tab-count:1'>&nbsp;&nbsp; </span>");

                    TextTag overrideMethodTag = new TextTag(null, className
                            + "#" + methodName);
                    TextTag overrideClassTag = new TextTag(null, className);
                    annotationStr.append(Tags.LINK.toMSHTML(overrideMethodTag))
                    .append(" in class ")
                    .append(Tags.LINK.toMSHTML(overrideClassTag));
                }
            }
            else
            {

                annotationStr
                .append("<p class=MsoNormal style='margin-top:10.0pt;margin-right:0in;margin-bottom:")
                .append("12.5pt;margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>")
                .append(name)
                .append("</b><span style='mso-tab-count:1'>&nbsp;&nbsp; </span>");

                // Get the value for the annotation. Currently, just append them
                // together
                for (ElementValuePair pair : annotation.elementValues()) {
                    String val = pair.value().value().toString();

                    // JKG - Add special case for the aQute BND @Version annotation.
                    if (isBndVersionTag) {
                        val = val.substring(0, val.lastIndexOf('.'));
                    }
                    annotationStr.append(val);
                }
            }

            annotationStr.append("</p>");
        }

        _msHTM.println(annotationStr);
    }

    /**
     * Tests an annotation against the list of ignoreable annotations.
     * 
     * @param annoName
     *            the qualified annotation name
     * @return True if the annotation should be ignored.
     */
    private static boolean isIgnoreableAnno(String annoName) {
        for (String ignoreableAnno : IGNOREABLE_ANNOTATIONS) {
            if (annoName.contains(ignoreableAnno)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Write the comment and any Tags, block or inline
     * 
     * @param doc
     *            the comment block
     */
    private static void writeTags(Doc doc) {
        if (doc.tags().length == 0) {
            return;
        }

        StringBuilder tagsStr = new StringBuilder("");
        for (Tag tag : doc.tags()) {
            Taglet taglet = _options
                    .getTagletForName(tag.name().length() > 1 ? tag.name()
                            .substring(1) : "");
            if ((taglet instanceof Tags)) {
                tagsStr.append(((Tags) taglet).toMSHTML(tag));
            }
        }

        _msHTM.println(tagsStr);
    }

    /**
     * Write Class data
     * 
     * @param classDoc
     *            ClassDoc
     */
    private static void writeClass(ClassDoc classDoc) {
        CURRENT_CLASSDOC = classDoc;

        StringBuilder classStr = new StringBuilder(
                "<div style='mso-element:para-border-div;border:none;border-bottom:double windowtext 1.5pt;");
        classStr.append("padding:0in 0in 1.0pt 0in'>");

        // Add intra-document link
        _docLinks.add(classDoc.name());
        classStr.append("<h3 style='mso-list:l5 level3 lfo1'>");
        classStr.append("<a name=\"").append(classDoc.name())
        .append("\"></a><![if !supportLists]><span ");
        classStr.append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'><span ");
        classStr.append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp;&nbsp; </span></span></span><![endif]><span ");
        classStr.append("style='mso-fareast-font-family:\"Times New Roman\"'>");
        classStr.append(getVisibility(classDoc)).append(" ");

        if (classDoc.isInterface()) {
            classStr.append("interface ");
        }
        if (classDoc.isEnum()) {
            classStr.append("enum ");
        }
        if (classDoc.isFinal() && !classDoc.isEnum()) {
            classStr.append("final ");
        }
        if (classDoc.isAbstract() && !classDoc.isInterface()) {
            classStr.append("abstract ");
        }
        if (classDoc.isClass() && !classDoc.isEnum()) {
            classStr.append("class ");
        }

        classStr.append("<span class=SpellE>").append(classDoc.name())
        .append("</span>");

        // Apply the default enum documentation.
        if (classDoc.isEnum()) {
            Util.setEnumDocumentation(new ConfigurationImpl(), classDoc);
        }

        if (classDoc.superclass() != null
                && !classDoc.superclass().name()
                .equals(Object.class.getSimpleName())) {
            classStr.append(" extends <span class=SpellE>")
            .append(classDoc.superclass().name()).append("</span>");
        }

        ClassDoc[] interfaces = classDoc.interfaces();
        if (interfaces.length > 0) {
            classStr.append(" implements <span class=SpellE>");
            for (ClassDoc i : interfaces) {
                classStr.append(i.name());
                // If there is more than one interface and it's not the last
                // one, add a comma
                if (interfaces.length > 1
                        && !i.equals(interfaces[interfaces.length - 1])) {
                    classStr.append(", ");
                }
            }
            classStr.append("</span>");
        }
        classStr.append("</span></h3></div>");

        _msHTM.println(classStr);

        writeAnnotations(classDoc, classDoc.annotations());
        writeComment(classDoc);
        writeTags(classDoc);

        if (classDoc.isEnum()) {
            writeEnums(classDoc.enumConstants());
        }
        writeFields(classDoc.fields());
        writeConstructors(classDoc.constructors());
        writeMethods(classDoc.methods(false));
    }

    /**
     * Write enum values.
     * 
     * @param enums
     *            the fields that are enums.
     */
    private static void writeEnums(final FieldDoc[] enums) {
        // Iterate over the fields
        for (FieldDoc field : enums) {
            StringBuilder enumStr = new StringBuilder(
                    "<h4 style='mso-list:l5 level4 lfo1'>");

            // Add intra-document link
            _docLinks.add(field.containingClass().name() + field.name());
            enumStr.append("<a name=\"").append(field.containingClass().name()).append(field.name());
            enumStr.append("\"></a><![if !supportLists]><span ");
            enumStr.append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'><span ");
            enumStr.append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp; </span></span></span><![endif]><span ");
            enumStr.append("style='mso-fareast-font-family:\"Times New Roman\"'>");
            enumStr.append(field.name()).append("<o:p></o:p></span></h4>");

            _msHTM.println(enumStr);

            writeComment(field);
            writeTags(field);
        }
    }

    /**
     * Write out Fields for a given Class
     */
    private static void writeFields(FieldDoc[] fields) {

        // First sort static methods
        List<FieldDoc> sortedStaticFields = new ArrayList<FieldDoc>();
        for (FieldDoc field : fields) {
            if (!field.isStatic())
                continue;
            for (int j = 0; j < sortedStaticFields.size(); j++) {
                if (field.name().charAt(0) < sortedStaticFields.get(j).name()
                        .charAt(0)) {
                    sortedStaticFields.add(j, field);
                    break;
                }
            }
            if (!sortedStaticFields.contains(field))
                sortedStaticFields.add(field);
        }
        // Next sort the other fields
        List<FieldDoc> sortedFields = new ArrayList<FieldDoc>();
        for (FieldDoc field : fields) {
            if (field.isStatic())
                continue;
            for (int j = 0; j < sortedFields.size(); j++) {
                if (field.name().charAt(0) < sortedFields.get(j).name()
                        .charAt(0)) {
                    if (field.isStatic())
                        sortedFields.add(j, field);
                    break;
                }
            }
            if (!sortedFields.contains(field))
                sortedFields.add(field);
        }

        // Combine the lists, putting static methods first
        sortedFields.addAll(0, sortedStaticFields);

        // Iterate over the fields
        for (FieldDoc field : sortedFields) {
            if (!isPublicOrProtected(field)) {
                continue;
            }

            writeField(field);
        }
    }

    /**
     * Write a field.
     */
    private static void writeField(FieldDoc fieldDoc) {
        StringBuilder fieldStr = new StringBuilder(
                "<h4 style='mso-list:l5 level4 lfo1'>");

        // Add intra-document link
        _docLinks
        .add(fieldDoc.containingClass().name() + fieldDoc.name());
        fieldStr.append("<a name=\"").append(fieldDoc.containingClass().name()).append(fieldDoc.name());
        fieldStr.append("\"></a><![if !supportLists]><span ");
        fieldStr.append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'><span ");
        fieldStr.append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp; </span></span></span><![endif]><span ");
        fieldStr.append("style='mso-fareast-font-family:\"Times New Roman\"'>");
        fieldStr.append(getVisibility(fieldDoc)).append(" ");

        if (fieldDoc.isVolatile()) {
            fieldStr.append("volatile ");
        }
        if (fieldDoc.isTransient()) {
            fieldStr.append("transient ");
        }
        if (fieldDoc.isStatic()) {
            fieldStr.append("static ");
        }
        if (fieldDoc.isFinal()) {
            fieldStr.append("final ");
        }

        fieldStr.append(fieldDoc.type().typeName()).append(" ")
        .append(fieldDoc.name());

        if (fieldDoc.constantValueExpression() != null
                && fieldDoc.constantValueExpression().length() > 0) {
            fieldStr.append(" = ").append(fieldDoc.constantValueExpression());
        }

        fieldStr.append("<o:p></o:p></span></h4>");

        _msHTM.println(fieldStr);

        writeComment(fieldDoc);
        writeTags(fieldDoc);
    }

    private static void writeConstructors(ConstructorDoc[] constructors) {
        for (ConstructorDoc ctor : constructors) {
            if (!isPublicOrProtected(ctor)) {
                continue;
            }
            writeExecutableMember(ctor);
        }
    }

    private static void writeMethods(MethodDoc[] methods) {
        // First sort static methods
        List<MethodDoc> sortedStaticMethods = new ArrayList<MethodDoc>();
        for (MethodDoc method : methods) {
            if (!method.isStatic())
                continue;
            for (int j = 0; j < sortedStaticMethods.size(); j++) {
                if (method.name().charAt(0) < sortedStaticMethods.get(j).name()
                        .charAt(0)) {
                    sortedStaticMethods.add(j, method);
                    break;
                }
            }
            if (!sortedStaticMethods.contains(method))
                sortedStaticMethods.add(method);
        }
        // Next sort the other methods.
        List<MethodDoc> sortedMethods = new ArrayList<MethodDoc>();
        for (MethodDoc method : methods) {
            if (method.isStatic())
                continue;
            for (int j = 0; j < sortedMethods.size(); j++) {
                if (method.name().charAt(0) < sortedMethods.get(j).name()
                        .charAt(0)) {
                    if (method.isStatic())
                        sortedMethods.add(j, method);
                    break;
                }
            }
            if (!sortedMethods.contains(method))
                sortedMethods.add(method);
        }

        // Combine the lists, putting static methods first
        sortedMethods.addAll(0, sortedStaticMethods);

        // Convert them to XMLNodes.
        for (MethodDoc method : sortedMethods) 
        {
            if (!isPublicOrProtected(method)) 
            {
                continue;
            }

            boolean isOverride = false;

            // Check to see if it has an Overrides annotation.
            if (method.commentText() == null
                    || method.commentText().length() == 0) 
            {
                for (AnnotationDesc anno : method.annotations()) 
                {
                    if (anno.annotationType().qualifiedTypeName()
                            .equals(Override.class.getCanonicalName())) 
                    {
                        isOverride = true;
                        ClassDoc oclass = method.overriddenClass();
                        if (oclass == null)
                        {
                            // For some reason, the overriddenClass() and
                            // overriddenMethod() calls return null sometimes,
                            // so I wrote my own.
                            MethodDoc overrideMethod = findOverrideMethod(
                                    method.containingClass(), method);

                            // If we didn't find it, then just write out this method
                            if (overrideMethod != null) {
                                // For comments in tags, we also need to use the
                                // overriding method's class
                                //Not sure why I did this.  Throws off the links.  Taking
                                //out for now.  --jkg 1/8/2012
                                // CURRENT_CLASSDOC = overrideMethod.containingClass();
                                method = overrideMethod;
                                // CURRENT_CLASSDOC = method.containingClass();
                            }
                        }
                        break;
                    } //end if
                } //end for
            } //end if

            writeMethod(method, isOverride);
        }

    }

    /**
     * Write out the method, taking into account special documentation to
     * identify overridden method.
     */
    private static void writeMethod(MethodDoc method, boolean isOverride) {
        writeExecutableMember(method, isOverride);
        writeAnnotations(method, method.annotations());
    }

    /**
     * Recurse up the inheritance tree for a method that has the override
     * annotation to reuse the comment. The search is breadth-first, checking
     * all interfaces of a containing class, then super class. If that is
     * fruitless, repeat with the super interfaces and then the super class (as
     * the containing class)
     * 
     * @param parentClass
     *            - the containing class of the methodDoc
     * @param method
     *            - the method with the <code>java.lang.Override</code>
     *            annotation
     * @return The overridden method. Null if not found.
     */
    private static MethodDoc findOverrideMethod(ClassDoc parentClass,
            MethodDoc method) {
        // First check all implementing interfaces for method
        for (ClassDoc iface : parentClass.interfaces()) {
            for (MethodDoc iFaceMethod : iface.methods()) {
                if (method.overrides(iFaceMethod)) {
                    return iFaceMethod;
                }
            }
        }

        // Then check super class, if there is one. Otherwise, we missed it.
        if (parentClass.superclass() != null) {
            for (MethodDoc superClassMethod : parentClass.superclass()
                    .methods()) {
                if (method.overrides(superClassMethod)) {
                    return superClassMethod;
                }
            }
        }

        // Restart the search with all interfaces
        for (ClassDoc iface : parentClass.interfaces()) {
            MethodDoc foundMethod = findOverrideMethod(iface, method);
            if (foundMethod != null) {
                return foundMethod;
            }
        }

        // No super class? Nowhere else to go, so return null.
        if (parentClass.superclass() == null) {
            return null;
        }

        // Nothing found. Restart with super class
        return findOverrideMethod(parentClass.superclass(), method);
    }

    /**
     * Write out a Constuctor or Method doc element
     */
    private static void writeExecutableMember(ExecutableMemberDoc member) {
        writeExecutableMember(member, false);
    }

    /**
     * Write out Ctor or Method doc element, adding documentation for overridden
     * methods.
     */
    private static void writeExecutableMember(ExecutableMemberDoc member,
            boolean isOverride) {

        StringBuilder memberStr = new StringBuilder(
                "<h4 style='mso-list:l5 level4 lfo1'>");

        // Add intra-document link
        // The current class could be a parent class or interface, so always get
        // the current class.
        //Also, it looks like links are capped at 40 characters. Methods are the most
        //likely culprit, so let's just put it here for now.  --jkg 1/5/2012
        String link = CURRENT_CLASSDOC.name() + member.name();
        if (link.length() > LINK_MAX_LEN)
        {
            link = link.substring(0,LINK_MAX_LEN);
        }
        _docLinks.add(link);
        memberStr.append("<a name=\"").append(link);
        memberStr.append("\"></a><![if !supportLists]><span ");
        memberStr
        .append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'><span ");
        memberStr
        .append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp; </span></span></span><![endif]><span ");
        memberStr
        .append("style='mso-fareast-font-family:\"Times New Roman\"'>");
        memberStr.append(getVisibility(member)).append(" ");

        if (member.isFinal()) {
            memberStr.append("final ");
        }
        if (member.isStatic()) {
            memberStr.append("static ");
        }
        if (member.isSynchronized()) {
            memberStr.append("synchronized ");
        }
        if (member.isSynthetic()) {
            memberStr.append("synthetic ");
        }
        if (member.isInterface()) {
            memberStr.append("interface ");
        }
        if (member.isMethod()) {
            MethodDoc method = (MethodDoc) member;

            if (method.isAbstract()) {
                memberStr.append("abstract ");
            }

            memberStr.append(
                    generateGenericTypeInfo(method.returnType(), false))
                    .append(" ");
        }

        memberStr.append(member.name()).append("(");

        // For each parameter, write it out, taking into account
        // a possible variable length argument as the last param.
        if (member.parameters().length > 0) {
            int i = 0;
            int len = member.parameters().length;
            for (Parameter param : member.parameters()) {
                i++;
                memberStr
                .append(generateGenericTypeInfo(param.type(),
                        member.isVarArgs() && (i == len))).append(" ")
                        .append(param.name());
                if (i < len) {
                    memberStr.append(", ");
                }
            }
        }
        memberStr.append(")<o:p></o:p></span></h4>");

        // If the method marked with the inherit tag, don't do these guys.
        boolean isInherited = false;
        for (Tag tag : member.inlineTags()) {
            if (tag.name().indexOf(Tags.INHERITDOC.getName()) > -1) {
                isInherited = true;
                break;
            }
        }

        _msHTM.println(memberStr);

        if (isOverride) {
            writeMethodOverrideComment((MethodDoc) member);
        } else {
            writeComment(member);
        }

        writeTags(member);

        if (!isInherited && !isOverride) {
            writeParameters(member.parameters(), member);

            Tag[] returnTags = member.tags("@return");
            if (returnTags.length > 0) {
                StringBuilder returnStr = new StringBuilder(
                        "<p class=MsoNormal style='margin-left:50.0pt'><span class=GramE><b style='mso-bidi-font-weight:normal'>");
                returnStr
                .append("Returns</b><span style='mso-tab-count:1'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; </span>");

                returnStr.append(getTagComment(returnTags[0]));
                returnStr.append("</p>");

                _msHTM.println(returnStr.toString());
            }

            writeExceptions(member);
        }
    }

    /**
     * Write out method and params
     */
    private static void writeParameters(Parameter[] params,
            ExecutableMemberDoc member) {
        if (params.length == 0) {
            return;
        }

        StringBuilder paramsStr = new StringBuilder(
                "<p class=MsoNormal style='margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>Arguments</b></p>");

        int i = 0;
        int len = member.parameters().length;
        for (Parameter parameter : params) {
            i++;
            paramsStr
            .append("<p class=MsoNormal style='margin-left:50.0pt'><span class=GramE><i style='mso-bidi-font-style:normal'>");

            if (member.isVarArgs() && (i == len)) {
                paramsStr
                .append(generateGenericTypeInfo(parameter.type(), true))
                .append(" ").append(parameter.name());
            } else {
                paramsStr.append(parameter.name());
            }

            paramsStr
            .append("</i></span>:<span style='mso-tab-count:1'>&nbsp;&nbsp; </span>");

            for (ParamTag tag : member.paramTags()) {
                if (tag.parameterName().equalsIgnoreCase(parameter.name())) {
                    paramsStr.append(getTagComment(tag));
                }
            }

            paramsStr.append("</p>");
        }

        _msHTM.println(paramsStr.toString());

    }

    /**
     * Write out any exceptions.
     */
    private static void writeExceptions(ExecutableMemberDoc member) {
        if (member.thrownExceptions().length == 0) {
            return;
        }

        StringBuilder exceptionsStr = new StringBuilder("");
        for (ClassDoc exception : member.thrownExceptions()) {
            exceptionsStr
            .append("<p class=MsoNormal style='margin-left:50.0pt'><b style='mso-bidi-font-weight:normal'>Throws</b></p>");
            exceptionsStr
            .append("<p class=MsoNormal style='margin-left:50.0pt'><i style='mso-bidi-font-style:normal'>");
            exceptionsStr.append(exception.typeName());
            exceptionsStr
            .append("</i>:<span style='mso-tab-count:1'>&nbsp;&nbsp;&nbsp; </span>");

            for (ThrowsTag tag : member.throwsTags()) {
                if (tag.exceptionName().equalsIgnoreCase(exception.typeName())) {
                    exceptionsStr.append(tag.exceptionComment());
                }
            }

            exceptionsStr.append("</p>");
        }

        _msHTM.println(exceptionsStr.toString());
    }

    /**
     * Generate comment text for each Tag
     */
    private static String getTagComment(Tag tag) {
        StringBuilder comment = new StringBuilder("");

        for (Tag t : tag.inlineTags()) {
            final Taglet taglet = _options.getTagletForName(t.name());
            if (taglet != null) {
                comment.append(taglet.toString(t));
            } else {
                comment.append(t.text());
            }
        }
        return comment.toString();
    }

    /**
     * 
     * @param member
     *            visibility keyword
     * @return the string equivalent matching the visibility type.
     */
    private static String getVisibility(ProgramElementDoc member) {
        if (member.isPrivate())
            return PRIVATE;
        if (member.isProtected())
            return PROTECTED;
        if (member.isPublic())
            return PUBLIC;
        if (member.isPackagePrivate())
            return PKG_PRIVATE;
        // Should never happen
        return null;
    }

    /**
     * @return True if the member is public or protected
     */
    private static boolean isPublicOrProtected(ProgramElementDoc member) {
        String visibility = getVisibility(member);
        return visibility.equals(PUBLIC) || visibility.equals(PROTECTED);
    }

    /**
     * Generate formatted names to handle generic types.
     * 
     * @param fulltype
     *            the fulltype of the parameter, containing the fully qualified
     *            name
     * @param isVarArgs
     *            True if this parameter is variable-length.
     * @return A simplified type name, taking into account wildcards and
     *         parameterized types.
     */
    private static String generateGenericTypeInfo(Type fulltype,
            boolean isVarArgs) {
        StringBuilder type = new StringBuilder("");
        if (fulltype instanceof WildcardType) {
            // Handle wild cards, i.e., <?>, <? extends java.util.Map>, <? super
            // java.lang.Integer>, etc.
            WildcardType wildCard = (WildcardType) fulltype;
            type.append(wildCard.simpleTypeName());
            Type[] extendsTypes = wildCard.extendsBounds();
            if (extendsTypes.length > 0) {
                type.append(" extends ");
                for (int i = 0; i < extendsTypes.length; i++) {
                    if (i > 0) {
                        type.append(", ");
                    }
                    type.append(generateGenericTypeInfo(extendsTypes[i],
                            isVarArgs));
                }

            }
            Type[] superTypes = wildCard.superBounds();
            if (superTypes.length > 0) {
                type.append(" super ");
                for (int i = 0; i < superTypes.length; i++) {
                    if (i > 0) {
                        type.append(", ");
                    }
                    type.append(generateGenericTypeInfo(superTypes[i],
                            isVarArgs));
                }
            }
        } else if (fulltype instanceof ParameterizedType) {
            // This is a parameterized type, so grab the type name and all of
            // its argument types.
            // Since there can be nested parameterized types, account for that
            // by recursing through this method.
            ParameterizedType paramType = (ParameterizedType) fulltype;
            type.append(paramType.simpleTypeName())
            .append(paramType.dimension()).append("&lt;");
            Type[] args = paramType.typeArguments();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    type.append(", ");
                }
                type.append(generateGenericTypeInfo(args[i], isVarArgs));
            }
            type.append("&gt;");
        } else {
            // if t's just a regular type, print the unqualified name and
            // dimension.
            type.append(fulltype.typeName());
            if (isVarArgs) {
                type.append("...");
            } else {
                type.append(fulltype.dimension());
            }
        }
        return type.toString();
    }

}
