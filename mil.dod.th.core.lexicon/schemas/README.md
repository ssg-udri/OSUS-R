<!--
==============================================================================
 This software is part of the Open Standard for Unattended Sensors (OSUS)
 reference implementation (OSUS-R).

 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along
 with this software. If not, see
 <http://creativecommons.org/publicdomain/zero/1.0/>.
==============================================================================
-->

This folder contains all the XSD files (schemas) for the core API and other schemas to represent the data model. All 
schemas will be compiled by XJC to JAXB annotated Java classes. The directory structure is based on the namespace of 
the schema which will then match the package name of the generated Java class. For instance, a namespace of 
asset.core.th.dod.mil equals a package name of mil.dod.th.core.asset (as a namespace is the reverse of the package, this
is just what XJC does). For a package name of mil.dod.th.core.asset, the folder will be core/asset under this folder. 
The mil.dod.th part of the package is the same for all namespace so it is excluded in the directory structure. In 
addition, the filename of the XSD file should match the name of the root element in the XSD. If not, services like 
XmlUnmarshal will not be able to find the XSD file.