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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Generate a Javadoc compatible with Confluence Wiki.
 * 
 * @author Josh
 * 
 */
public final class ConfluenceDoclet extends Doclet 
{

    private static final String PRIVATE = "private";
    private static final String PROTECTED = "protected";
    private static final String PUBLIC = "public";
    private static final String PKG_PRIVATE = "package_private";
    
    private static final String[] IGNOREABLE_ANNOTATIONS = { "javax.xml.bind.annotation" };

    private static final int LINK_MAX_LEN = 39;
    
    private static final String BREAK = "\\\\";
    private static final String LINE = "----";

    private static ArrayList<String> confluencePages = new ArrayList<String>();
    private static Set<String> apiPages = new HashSet<String>();
    private static Map<String, String> linkMap = new HashMap<String, String>();

    /**
     * Some Taglets need to reference the current ClassDoc, i.e., inline links.
     * Provide it here for all to consume.
     */
    private static ClassDoc CURRENT_CLASSDOC;

    public static ClassDoc getCurrentClassDoc() 
    {
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
    public static void addLinkRef(final String docLinkRef, final Doc indicator) 
    {
        _docLinkRefs.put(docLinkRef, indicator);
    }
    
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
    public static boolean start(final RootDoc root)
    {
        try
        {
            // Retrieve current pages on confluence
            retrieveConfluencePageList();

            // Generate list of pages needed
            getApiPageList(root);            

            // Compare the two lists of pages, rename api pages if the name already exists in the space
            // and create a map that will store all the new api pages mapped to what the final page name actually is.
            createLinkMap();

            // Write out new API
            startClassDocs(root);

            // Report broken links within the API
            runLinkReport();
        }
        catch (final Throwable t)
        {
            t.printStackTrace();

            return false;
        }

        return true;
    }
    
    /**
     * Store a list of pages that are currently on the confluence space.
     * 
     * @throws IOException 
     *      Used to catch input exceptions from loading the file with the page list.
     */
    public static void retrieveConfluencePageList() throws IOException
    {
        final String thoseWorkspace = System.getenv("THOSE_WS");
        
        //TODO TH-1475 Tool should be independent of the actual API, don't hard code filename as it could come form anywhere.
        //TODO TH-1475 use option flag instead.
        FileInputStream inputStream = new FileInputStream(new File(thoseWorkspace + "/shared/core/generated/confluencePages.txt"));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) 
        {
            confluencePages.add(line);
        }
        reader.close();
    }
    
    /**
     *  Generate a list of pages the doclet would like to create for the api.
     *  
     * @param root
     *         The start of the document.
     */
    private static void getApiPageList(final RootDoc root) 
    {
        for (ClassDoc doc : root.classes()) 
        {
            apiPages.add(doc.name());
            apiPages.add(doc.containingPackage().name());
        }
    }

    /**
     * Check the list of pages the doclet would like to create for the api
     * against the list of pages that are currently on the confluence space.
     * 
     * If a page exists, rename it with underscores appended until the name
     * is no longer taken.
     * 
     * When the desired name is found, store the new name mapped to the old
     * name in order to correctly link to the new page throughout the document.
     */
    private static void createLinkMap() 
    {
        for (String link : apiPages)
        {
            String finalLink = link;
            while (confluencePages.contains(finalLink))
            {
                finalLink = finalLink + "_";
            }
            linkMap.put(link, finalLink);
        }
    }

    /**
     * Run the report and create the output file to write the invalid links report.
     */
    private static void runLinkReport() {
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

    private static void startClassDocs(RootDoc root) throws FileNotFoundException {
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
        //TODO TH-1475 Use List class with sort method rather than queue. May also need to change in WordHTMDoclet.
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
        }
    }

    /**
     * Write Package info
     */
    private static void writePackage(PackageDoc pkg) throws FileNotFoundException {
        StringBuilder pkgSb = new StringBuilder();
        
        String linkName = getPageName(pkg.name());
        
        File outFile = new File(_options.getFilename(), linkName + ".package");
        PrintWriter pw = new PrintWriter(outFile);
        System.out.println("Writing package to " + outFile);

        // Add intra-document link point
        _docLinks.add(linkName);
        pkgSb.append("h2. ").append(pkg.name());

        pw.println(htmlToWiki(pkgSb.toString()));

        writeAnnotations(pw, pkg, pkg.annotations());
        writeComment(pw, pkg);
        writeTags(pw, pkg);
        
        pw.close();
    }

    /**
     * 
     * @param doc
     *            The Doc object.
     */
    private static void writeComment(final PrintWriter pw, final Doc doc) {
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
            pw.println(htmlToWiki(comment.toString()));
        }
    }

    /**
     * Write comment for overriding method
     */
    private static void writeMethodOverrideComment(PrintWriter pw, MethodDoc method) {
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
            String classLink = method.containingClass().name();
            pw.println("*Description copied from:* ");
            pw.println("[" + classLink + "." + method.name() + "|" + getPageName(classLink) + "#" + method.name() + "]" + "\n");            
            
            pw.println(htmlToWiki(comment.toString()));
        }
    }

    private static void writeAnnotations(PrintWriter pw, Doc doc, AnnotationDesc[] annotations) {
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
                name = "*Overrides* ";

                if (doc.isMethod()) {
                    MethodDoc method = (MethodDoc) doc;

                    String methodName = "";
                    String className = "";
                    // If not overriding class, assume it's in an interface
                    if (method.overriddenClass() == null) {
                        method = findOverrideMethod(method.containingClass(), method);
                        className = method.containingClass().name();
                        methodName = method.name();
                        name = "*Specified by* ";
                    } else {
                        className = method.overriddenClass().name();
                        methodName = method.overriddenMethod().name();
                    }

                    annotationStr.append(name);

                    TextTag overrideMethodTag = new TextTag(null, className
                            + "#" + methodName);
                    TextTag overrideClassTag = new TextTag(null, className);
                    annotationStr.append(Tags.LINK.tagString(overrideMethodTag))
                            .append(" in class ")
                            .append(Tags.LINK.tagString(overrideClassTag));
                }
            }
            else
            {
    
                annotationStr.append(" *" + name + "* ");
    
                // Get the value for the annotation. Currently, just append them together
                for (ElementValuePair pair : annotation.elementValues()) {
                    String val = pair.value().value().toString();
    
                    // JKG - Add special case for the aQute BND @Version annotation.
                    if (isBndVersionTag) {
                        val = val.substring(0, val.lastIndexOf('.'));
                    }
                    annotationStr.append(val);
                }
            }
        }

        pw.println(htmlToWiki(annotationStr.toString()));
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
    private static void writeTags(PrintWriter pw, Doc doc) {
        if (doc.tags().length == 0) {
            return;
        }

        StringBuilder tagsStr = new StringBuilder("");
        for (Tag tag : doc.tags()) {
            Taglet taglet = _options
                    .getTagletForName(tag.name().length() > 1 ? tag.name()
                            .substring(1) : "");
            if ((taglet instanceof Tags)) {
                tagsStr.append(((Tags) taglet).tagString(tag));
            }
        }

        pw.println(htmlToWiki(tagsStr.toString()));
    }

    /**
     * Write Class data
     * 
     * @param classDoc
     *            ClassDoc
     */
    private static void writeClass(ClassDoc classDoc) throws FileNotFoundException {
        CURRENT_CLASSDOC = classDoc;

        StringBuilder classStr = new StringBuilder();
        
        String linkName = getPageName(classDoc.name());

        File outFile = new File(_options.getFilename(), linkName + ".class." + classDoc.containingPackage());
        PrintWriter pw = new PrintWriter(outFile);
        System.out.println("Writing package to " + outFile);
        _docLinks.add(classDoc.name());
        pw.println("h1. " + classDoc.name());
        pw.println(LINE);
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

        classStr.append(classDoc.name());

        // Apply the default enum documentation.
        if (classDoc.isEnum()) {
            Util.setEnumDocumentation(new ConfigurationImpl(), classDoc);
        }

        if (classDoc.superclass() != null && !classDoc.superclass().name().equals(Object.class.getSimpleName())) {
            classStr.append(BREAK + "extends ").append("[" + classDoc.superclass().name() + "|" + getPageName(classDoc.superclass().name()) + "]");
        }

        ClassDoc[] interfaces = classDoc.interfaces();
        if (interfaces.length > 0) {
            classStr.append(BREAK + "implements ");
            for (ClassDoc i : interfaces) {
                classStr.append("[" + i.name() + "|" + getPageName(i.name()) + "]");
                // If there is more than one interface and it's not the last one, add a comma
                if (interfaces.length > 1
                        && !i.equals(interfaces[interfaces.length - 1])) {
                    classStr.append(", ");
                }
            }
        }        

        pw.println(htmlToWiki(classStr.toString()));
        pw.println(LINE);

        writeAnnotations(pw, classDoc, classDoc.annotations());
        writeComment(pw, classDoc);
        writeTags(pw, classDoc);

        pw.println(BREAK);
        
        if (classDoc.isEnum()) {
            writeEnums(pw, classDoc.enumConstants());
        }
        writeFields(pw, classDoc.fields());
        writeConstructors(pw, classDoc.constructors());
        writeMethods(pw, classDoc.methods(false));
        
        pw.close();
    }

    /**
     * Write enum values.
     * 
     * @param enums
     *            the fields that are enums.
     */
    private static void writeEnums(final PrintWriter pw, final FieldDoc[] enums) {
        // Iterate over the fields
        for (FieldDoc field : enums) {
            StringBuilder enumStr = new StringBuilder();
            // Add intra-document link
            _docLinks.add(field.containingClass().name() + field.name());
            enumStr.append("h4. " + field.name());

            pw.println(htmlToWiki(enumStr.toString()));

            writeComment(pw, field);
            writeTags(pw, field);
        }
    }

    /**
     * Write out Fields for a given Class
     */
    private static void writeFields(PrintWriter pw, FieldDoc[] fields) {
        
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

        if (sortedFields.size() > 0)
        {
            pw.println("{panel}");
            pw.println("h3. Fields");
            pw.println("{panel}");
            pw.println(LINE);
        }
        
        // Iterate over the fields
        for (FieldDoc field : sortedFields) {
            if (!isPublicOrProtected(field)) {
                continue;
            }

            writeField(pw, field);
            pw.println(LINE);
        }
    }

    /**
     * Write a field.
     */
    private static void writeField(PrintWriter pw, FieldDoc fieldDoc) {
        StringBuilder fieldStr = new StringBuilder();
        // Add intra-document link
        _docLinks.add(fieldDoc.containingClass().name() + fieldDoc.name());
        fieldStr.append("h4. ").append(fieldDoc.name());
        fieldStr.append("\n" + getVisibility(fieldDoc)).append(" ");

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

        pw.println(htmlToWiki(fieldStr.toString()));

        writeComment(pw, fieldDoc);
        writeTags(pw, fieldDoc);
    }

    private static void writeConstructors(PrintWriter pw, ConstructorDoc[] constructors) {
        if (constructors.length > 0)
        {
            pw.println("{panel}");
            pw.println("h3. Constructors");
            pw.println("{panel}");
            pw.println(LINE);
        }
        
        for (ConstructorDoc ctor : constructors) {
            if (!isPublicOrProtected(ctor)) {
                continue;
            }
            writeExecutableMember(pw, ctor);
            pw.println(LINE);
        }
    }

    private static void writeMethods(PrintWriter pw, MethodDoc[] methods) {
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

        if (sortedMethods.size() > 0)
        {
            pw.println("{panel}");
            pw.println("h3. Methods");
            pw.println("{panel}");
            pw.println(LINE);
        }
        
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
//                                CURRENT_CLASSDOC = overrideMethod.containingClass();
                                method = overrideMethod;
//                                CURRENT_CLASSDOC = method.containingClass();
                            }
                        }
                        break;
                    } //end if
                } //end for
            } //end if
            
            writeMethod(pw, method, isOverride);
            pw.println(LINE);
        }

    }

    /**
     * Write out the method, taking into account special documentation to
     * identify overridden method.
     */
    private static void writeMethod(PrintWriter pw, MethodDoc method, boolean isOverride) {
        writeExecutableMember(pw, method, isOverride);
        writeAnnotations(pw, method, method.annotations());
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
     * Write out a Constructor or Method doc element
     */
    private static void writeExecutableMember(final PrintWriter pw, ExecutableMemberDoc member) {
        writeExecutableMember(pw, member, false);
    }

    /**
     * Write out Ctor or Method doc element, adding documentation for overridden
     * methods.
     */
    private static void writeExecutableMember(PrintWriter pw, ExecutableMemberDoc member,
            boolean isOverride) {

        StringBuilder memberStr = new StringBuilder();
        
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
        memberStr.append("h4. ").append(member.name());
        memberStr.append("\n" + getVisibility(member)).append(" ");

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
        
        memberStr.append("\\)");
        // If the method marked with the inherit tag, don't do these guys.
        boolean isInherited = false;
        for (Tag tag : member.inlineTags()) {
            if (tag.name().indexOf(Tags.INHERITDOC.getName()) > -1) {
                isInherited = true;
                break;
            }
        }

        pw.println(htmlToWiki(memberStr.toString()));

        if (isOverride) {
            writeMethodOverrideComment(pw, (MethodDoc) member);
        } else {
            writeComment(pw, member);
        }

        writeTags(pw, member);
        
        if (!isInherited && !isOverride) {
            writeParameters(pw, member.parameters(), member);

            Tag[] returnTags = member.tags("@return");
            if (returnTags.length > 0) {
                StringBuilder returnStr = new StringBuilder();
                returnStr.append("*Returns:* ");
                
                returnStr.append(getTagComment(returnTags[0]));

                pw.println(htmlToWiki(returnStr.toString()));
            }

            writeExceptions(pw, member);
        }
    }

    /**
     * Write out method and params
     */
    private static void writeParameters(PrintWriter pw, Parameter[] params,
            ExecutableMemberDoc member) {
        if (params.length == 0) {
            return;
        }

        StringBuilder paramsStr = new StringBuilder();
        pw.println("*Parameters:* ");
        int i = 0;
        int len = member.parameters().length;
        for (Parameter parameter : params) {
            i++;
            paramsStr.append(" _");

            if (member.isVarArgs() && (i == len)) {
                paramsStr
                        .append(generateGenericTypeInfo(parameter.type(), true))
                        .append(" ").append(parameter.name());
            } else {
                paramsStr.append(parameter.name());
            }
            paramsStr.append("_ ");
            
            for (ParamTag tag : member.paramTags()) {
                if (tag.parameterName().equalsIgnoreCase(parameter.name())) {
                    paramsStr.append(getTagComment(tag));
                }
            }

            paramsStr.append(BREAK);
        }

        pw.println(htmlToWiki(paramsStr.toString()));

    }

    /**
     * Write out any exceptions.
     */
    private static void writeExceptions(final PrintWriter pw, ExecutableMemberDoc member) {
        if (member.thrownExceptions().length == 0) {
            return;
        }

        StringBuilder exceptionsStr = new StringBuilder("");
        for (ClassDoc exception : member.thrownExceptions()) {
            exceptionsStr.append(" *Throws:* ");
            exceptionsStr.append(" _" + exception.typeName() + "_ ");
            
            for (ThrowsTag tag : member.throwsTags()) {
                if (tag.exceptionName().equalsIgnoreCase(exception.typeName())) {
                    exceptionsStr.append(tag.exceptionComment());
                }
            }

            exceptionsStr.append(BREAK);
        }

        pw.println(htmlToWiki(exceptionsStr.toString()));
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
            // if it's just a regular type, print the unqualified name and
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
    
    /**
     * If a page has been renamed it will be stored in the link map so
     * when a link is created this method will take what the doclet
     * wanted to name the page and return what the name ended up being.
     * 
     * @param current
     *         The current name the doclet is expecting.
     * @return
     *         The page name that was finally given to the desired page.
     */
    public static String getPageName(String current)
    {
        String linkName = current;
        
        for (Entry<String, String> entry : linkMap.entrySet()){
            if (entry.getKey().equals(linkName)) {
                linkName = entry.getValue();
            }
        }
        
        return linkName;
    }
    
    /**
     * Convert to confluence wiki markup or remove all html tags in a string.
     * 
     * @param html
     *     The html string to convert.
     * @return
     *     The same string with all html removed or converted to confluence wiki.
     */
    public static String htmlToWiki(String html)
    {
        //TODO TH-1475 This would probably be more efficient using a StringBuilder 
        // to build the final string as it replaces html with wiki markup.
        
        // Convert links that make it past the tags structure to wiki links
        ArrayList<String> links = new ArrayList<String>();
        Pattern linkPattern = Pattern.compile("<a href.*?</a.*?>", Pattern.DOTALL);
        Matcher linkMatcher = linkPattern.matcher(html);
        while (linkMatcher.find())
        {
            links.add(linkMatcher.group());
        }
        for (String link : links)
        {
            String original = link;
            link = link.replaceAll("\\r\\n|\\r|\\n", " ");
            String alias = link.replaceAll("<a.*?>", "");
            alias = alias.replaceAll("</a>", "").trim();
            link = link.replaceAll("<a.*?\"", "["); 
            link = link.replaceAll("\".*?>.*?</a>", "]");           
            if (!link.contains("http"))
            {
                link = link.replaceAll("\\.", "#");
            }
            link = link.replace("[", "[" + alias + "|");
            html = html.replace(original, link);
        }
        
        // Convert ordered lists
        Pattern olPattern = Pattern.compile("<ol>.*?</ol>", Pattern.DOTALL);
        Matcher olMatcher = olPattern.matcher(html);
        while (olMatcher.find())
        {
            String original = olMatcher.group();
            String bullets = original.replaceAll("<li>", " # ");
            html = html.replaceAll(Pattern.quote(original), bullets);
        }
        
        // Convert unordered lists
        Pattern ulPattern = Pattern.compile("<ul>.*</ul>", Pattern.DOTALL);
        Matcher ulMatcher = ulPattern.matcher(html);
        while (ulMatcher.find())
        {
            String original = ulMatcher.group();
            String bullets = original.replaceAll("<li>", " - ");
            html = html.replaceAll(Pattern.quote(original), bullets);
        }
        
        // Convert code tags to monospace font
        html = html.replaceAll("<code( .*)?>", "{{");
        html = html.replaceAll("</code>", "}}");
        
        // Convert header tags
        html = html.replaceAll("<h1( .*)?>", "h1. ");
        html = html.replaceAll("<h2( .*)?>", "h2. ");
        html = html.replaceAll("<h3( .*)?>", "h3. ");
        html = html.replaceAll("<h4( .*)?>", "h4. ");
        html = html.replaceAll("<h5( .*)?>", "h5. ");
        
        // Convert italic tags
        html = html.replaceAll("<i( .*)?> ", " _");
        html = html.replaceAll("</i>", "_ ");
        
        // Convert bold tags
        html = html.replaceAll("<b( .*)?> ", " *");
        html = html.replaceAll("</b>", "* ");
        
        // Remove all remaining tags as a last resort, catch all.
        html = html.replaceAll("\\<.*?\\>", "");
        return html;
    }
}
