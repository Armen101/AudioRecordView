**AudioRecordView**

*Audio visualizer that can be used during sound recording*

<a href="https://imgflip.com/gif/3keacb"><img src="https://i.imgflip.com/3keacb.gif" title="made at imgflip.com"/></a>

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
  implementation 'com.github.Armen101:AudioRecordView:1.0.2'
}
```
Or Maven
```
<dependency>
  <groupId>com.github.Armen101</groupId>
  <artifactId>AudioRecordView</artifactId>
  <version>1.0.2</version>
</dependency>
```

**How do I use AudioRecordView?**

in XML 

```xml
<com.visualizer.amplitude.AudioRecordView
        android:id="@+id/audioRecordView"
        android:layout_width="256dp"
        android:layout_height="64dp"
        app:chunkAlignTo="bottom"                                  
        app:chunkRoundedCorners="true"
        app:chunkSoftTransition="true"                                  
        app:chunkColor="@color/app_style_blue"
        app:chunkSpace="1dp"
        app:chunkWidth="2dp"
        app:chunkMaxHeight="48dp"
        app:chunkMinHeight="2dp"/>
```
Drawing

You can execute this code in a timer, for example, every 100 milliseconds

```kotlin
 val audioRecordView: AudioRecordView = findViewById(R.id.audioRecordView)
 
 // in the timer
 val currentMaxAmplitude = getMediaRecorder().getMaxAmplitude()
 audioRecordView.update(currentMaxAmplitude)   //redraw view
```

At the end or before reuse
```
audioRecordView.recreate()
```
**Compatibility**

Minimum Android SDK: AudioRecordView requires a minimum API level of 16.

**Also you can see**

[Sample project](https://github.com/Armen101/AudioRecordViewSample) in github

[Tutorial](https://medium.com/@gevorgyanweb/audiorecordview-or-simplest-and-best-audio-visualizer-for-android-4fcec59608) in Medium

**License**

Apache 2.0. See the [LICENSE](https://github.com/Armen101/AudioRecordView/blob/master/LICENSE). file for details.

**Author**
Armen Gevorgyan
