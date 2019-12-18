**AudioRecordView**

*Audio visualizer that can be used during sound recording*

**How to include?**

Add the repository to your project build.gradle:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
And add the library to your module build.gradle:
```
dependencies {
  implementation 'com.github.Armen101:AudioRecordView:1.0.0'
}
```
Or Maven
```
<dependency>
  <groupId>com.github.Armen101</groupId>
  <artifactId>AudioRecordView</artifactId>
  <version>1.0.0</version>
</dependency>
```

**How do I use AudioRecordView?**

in XML 

```xml
<com.visualizer.amplitude.AudioRecordView
        android:id="@+id/audioRecordView"
        android:layout_width="200dp"
        android:layout_height="50dp"
        app:chunkColor="@color/app_style_blue"
        app:chunkSpace="1dp"
        app:chunkWidth="2dp"
        app:chunkMaxHeight="45dp"
        app:chunkMinHeight="2dp"/>
```
Drawing

In the timer you can do this code, for example, every 100 milliseconds

```java
 val audioRecordView: AudioRecordView = findViewById(R.id.audioRecordView)
 
 // in the timer
 val currentMaxAmplitude = getMediaRecorder().getMaxAmplitude();
 audioRecordView.update(currentMaxAmplitude); //redraw view
```

At the end or before reuse
```
audioRecordView.recreate();
```
**Compatibility**

Minimum Android SDK: AudioRecordView requires a minimum API level of 16.

**Author**
Armen Gevorgyan
