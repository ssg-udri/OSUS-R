#
# Terra Harvest Plug-in BND Specification
#
Bundle-Version: 0.1
Bundle-Description: ${description}
Bundle-Name: ${description}
Bundle-SymbolicName: ${package}
Bundle-Vendor: ${vendor}

# Specify packages that should be included in the JAR file, but not accessible
# to other bundles.  Not needed unless new packages are added.
#Private-Package: ${package}.somepackage

# Specify packages that should be accessible by other bundles (separated by commas)
Export-Package: ${package}

# Specify files that should be included within the JAR file. The default value for
# "Include-Resource" will automatically include JARs found in the project lib/
# directory.
Include-Resource: ${r"${bnd.include.libs}"}, capabilities-xml=capabilities-xml

# Include BND service annotations found with the following packages.
Service-Component: ${package}.*

# Specify configuration interfaces annotated with OCD to be generated into metatype xml.
-metatype: *