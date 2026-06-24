# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

-keepattributes *Annotation*

-keep class javax.xml.stream.** { *; }
-keep class com.ctc.wstx.** { *; }
-keep class de.tadris.fitness.util.export.gpx.model.** { *; }
-keep class de.tadris.fitness.data.** { *; } # this includes several classes used in backup serialization

# StAX factory implementation from stax-api is loaded via service loader at runtime
# by Jackson XmlMapper — R8 would strip it without a direct code reference
-keep class com.bea.xml.stream.** { *; }
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-dontwarn org.codehaus.stax2.validation.XMLValidationSchemaFactory$*
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn com.ctc.wstx.shaded.msv_core.driver.textui.Driver
-dontwarn jakarta.xml.bind.DatatypeConverter
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.xml.bind.DatatypeConverter
-dontwarn org.osgi.framework.BundleActivator
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.ServiceRegistration