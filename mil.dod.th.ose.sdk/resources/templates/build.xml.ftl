<!--
================================================================================
 Terra Harvest Plug-in Ant Build File

 DESCRIPTION:
 Build and test Terra Harvest plug-ins.

================================================================================
-->
<project name="${name}" default="help">

    <!-- Import properties used to define build behavior and output -->
    <property file="local.properties" />
    <property file="build.properties" />

    <!-- Validate that key properties have been properly defined and fail the build otherwise -->

    <fail message="The th.sdk.dir property is undefined, it should be set in local.properties" 
          unless="th.sdk.dir" />

    <!-- Import the standard build rules from the SDK -->
    <import file="${r"${th.sdk.dir}"}/tools/th_build_rules.xml" />

    <!-- Any of the standard build rules in ${r"${th.sdk.dir}"}/tools/th_build_rules.xml 
         can be overridden here
    -->

</project>
