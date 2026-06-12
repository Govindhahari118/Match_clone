# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
# BCrypt
-keep class org.mindrot.** { *; }
# Keep data classes (parcelable/room)
-keep class com.match.app.data.local.entity.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
# kotlinx-serialization
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * { @kotlinx.serialization.* *; }
# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class com.match.app.data.remote.** { *; }
# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
# Razorpay
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
# ExoPlayer / Media3
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
# WorkManager + HiltWorker
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
