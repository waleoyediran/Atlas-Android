## Add Layer Atlas

1. Copy `layer-atlas` to the root of your AndroidStudio project:

	```
	/MyApplication/layer-atlas
	```
	
2. Add Layer's GitHub Maven repo to your root `build.gradle` (e.g. `/MyApplication/build.gradle`):

	``` groovy
	allprojects {
    	repositories {
        	maven { url "https://raw.githubusercontent.com/layerhq/releases-android/master/releases/" }
	    }
	}
	```

3. Add `layer-atlas` project reference to your app's `build.gradle` (e.g. `/MyApplication/app/build.gradle`):

	``` groovy
	dependencies {
    	compile project(':layer-atlas')
	}
	```

4. Add `:layer-atlas` module to your project's root `settings.gradle` (e.g. `/MyApplication/settings.gradle`):

	``` groovy
	include ':app', ':layer-atlas'
	```

5. Click "Sync Project with Gradle Files" in Android Studio

## Test with Atlas Messenger
1. Add **Layer Atlas** to your project (see above).

2. Copy `layer-atlas-messenger` to the root of your AndroidStudio project:

	```
	/MyApplication/layer-atlas-messenger
	```

3. Add `:layer-atlas-messenger` module to your project's root `settings.gradle` (e.g. `/MyApplication/settings.gradle`):

	``` groovy
	include ':app', ':layer-atlas', ':layer-atlas-messenger'
	```

4. Click "Sync Project with Gradle Files" in Android Studio