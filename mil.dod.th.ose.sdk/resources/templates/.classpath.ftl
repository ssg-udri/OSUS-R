<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="src"/>
	<classpathentry kind="src" output="test-bin" path="test"/>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
	<classpathentry kind="lib" path="${sdk}/lib/biz.aQute.bnd.annotation-3.1.0.jar"/>
    <classpathentry kind="lib" path="${sdk}/lib/hamcrest-all-1.3.jar"/>
    <classpathentry kind="lib" path="${sdk}/lib/mockito-core-1.9.5.jar"/>
    <classpathentry kind="lib" path="${sdk}/lib/com.springsource.org.objenesis-1.0.0.jar"/>
    <classpathentry kind="lib" path="${sdk}/lib/junit-4.11.jar"/>
	<classpathentry kind="lib" path="${sdk}/api/osgi.core-5.0.0.jar"/>
	<classpathentry kind="lib" path="${sdk}/api/mil.dod.th.core.api.jar">
		<attributes>
			<attribute name="javadoc_location" value="jar:file:/${sdk}/core/api/mil.dod.th.core-javadoc.zip!/"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="lib" path="${sdk}/api/org.apache.felix.gogo.runtime-0.12.1.jar"/>
	<classpathentry kind="lib" path="${sdk}/api/osgi.cmpn-5.0.0.jar"/>
	<classpathentry kind="output" path="bin"/>
</classpath>
