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
package org.udri.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Given an input file and a directory, insert all of the XSD documents.
 * 
 * @author UDRI
 * 
 */
public class XSDAppender {
    public static final String TH_XSD_MARKER_START = "INSERT_XSD_DOCS";
    public static final String TH_XSD_MARKER_END = "END_XSD_DOCS";

    private static final String[][] SCHEMAS = {
        { "observationSchema", "Observation Schemas"},
        { "capabilitySchema", "Capability Schemas"},
        { "commandSchema", "Command Schemas"},
        { "missionProgramSchema", "Mission Program Schemas"},
        { "sharedSchema", "Shared Schemas"},
        { "inventorySchema", "Inventory Schemas"},
        { "remoteChannelSchema", "Remote Channel Schemas"}};


    private PrintWriter _apiFile;

    public XSDAppender(PrintWriter apiFile) {
        _apiFile = apiFile;
    }

    /**
     * Write the XSDs for each XSD in the resource dir. This assumes that XSDs
     * are included in subfolders within the resourcesDir
     */
    public void writeXSDs(File resourcesDir) throws IOException {
        File[] subdirs = resourcesDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File arg0) {
                if (arg0.isDirectory()) {
                    return true;
                }

                return false;
            }
        });

        for (File subdir : subdirs) {
            // ignore hidden directories, like .svn, as well as non-directories
            if (subdir.getName().startsWith(".") || !subdir.isDirectory()) {
                continue;
            }

            // If the schema dir is in the predefined list of schemas, then
            // process it.
            for (String[] schemaDirs : SCHEMAS) {
                if (schemaDirs[0].equals(subdir.getName())) {
                    addXSDs(subdir, schemaDirs[1]);
                    break;
                }
            }
        }
    }

    /**
     * Write out XSD file to RandomAccessFile
     * 
     * @param name
     *            the header name
     */
    private void addXSDs(File xsdDir, String name) throws IOException {

        StringBuilder resourceStr = new StringBuilder(
                "<div style='mso-element:para-border-div;border-top:double windowtext 2.5pt;")
        .append("border-left:none;border-bottom:double windowtext 2.5pt;border-right:none;")
        .append("mso-border-top-alt:triple windowtext 2.5pt;mso-border-bottom-alt:triple windowtext 2.5pt;")
        .append("padding:1.0pt 0in 1.0pt 0in'>");

        resourceStr
        .append("<h1 style='mso-list:l5 level1 lfo1'><![if !supportLists]><span class=grame><span")
        .append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'>2<span")
        .append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
        .append("</span></span></span></span><![endif]><span class=grame><span style='mso-fareast-font-family:")
        .append("\"Times New Roman\"'>").append(name).append("<o:p></o:p></span></span></h1>")
        .append("</div>");

        _apiFile.println(resourceStr.toString());

        // Get all XSD files
        File[] xsds = xsdDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith("xsd")) {
                    return true;
                }

                return false;
            }
        });

        for (File xsd : xsds) {

            // Write header
            StringBuilder xsdStr = new StringBuilder(
                    "<div style='mso-element:para-border-div;border-top:double windowtext 1.5pt;;")
            .append("border-left:none;border-bottom:double windowtext 1.5pt;border-right:none;")
            .append("padding:1.0pt 0in 1.0pt 0in'>");

            // Add intra-document link point
            xsdStr.append("<h2 style='mso-list:l5 level2 lfo1'><![if !supportLists]><span class=grame><span")
            .append("style='mso-fareast-font-family:\"Times New Roman\"'><span style='mso-list:Ignore'><span")
            .append("style='font:7.0pt \"Times New Roman\"'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
            .append("</span></span></span></span><![endif]><span class=grame><span style='mso-fareast-font-family:")
            .append("\"Times New Roman\"'>").append(xsd.getName()).append("<o:p></o:p></span></span></h2>")
            .append("</div>");

            _apiFile.println(xsdStr.toString());

            // Write XSD file, using same font as for <code> comments.
            _apiFile.println("<p class=MsoNormal style='margin-left:50.0pt;'>");
            BufferedReader br = new BufferedReader(new FileReader(xsd));
            String line;
            while ((line = br.readLine()) != null) {
                _apiFile.print("<pre><span style='mso-bookmark:_Ref286137727'><span class=grame><span style='font-family:Courier'>");
                _apiFile.print(line.replaceAll("<", "&lt;").replaceAll(">",
                        "&gt;").replaceAll("\n", ""));
                _apiFile.println("</span></span></pre>");
            }
            br.close();
            _apiFile.println("</p>");

            _apiFile.flush();
        }
    }
}
