# MediaPicker SDK ProGuard Rules

# Keep all public classes and methods in the MediaPicker SDK
-keep public class com.mediapicker.** { *; }

# Keep the main activity class that might be referenced externally
-keep class com.mediapicker.gallery.presentation.activity.MediaGalleryActivity { *; }

# Keep all fragment classes to prevent issues with fragment transactions
-keep class com.mediapicker.gallery.presentation.fragments.** { *; }

# Keep ViewModels to prevent issues with ViewModelProvider
-keep class com.mediapicker.gallery.presentation.viewmodels.** { *; }

# Keep custom views and their constructors
-keep class com.mediapicker.gallery.presentation.carousalview.** {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# Keep domain entities for proper serialization/deserialization
-keep class com.mediapicker.gallery.domain.entity.** { *; }

# Keep listener interfaces
-keep interface com.mediapicker.gallery.presentation.carousalview.CarousalActionListener { *; }
-keep interface com.mediapicker.gallery.presentation.carousalview.** { *; }

# Keep any classes that might be used via reflection
-keepclassmembers class com.mediapicker.** {
    public <init>(...);
}


# Keep annotations
-keepattributes *Annotation*

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep generic signatures
-keepattributes Signature

# If using Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Prevent obfuscation of classes that extend Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.view.View
-keep public class * extends androidx.lifecycle.ViewModel