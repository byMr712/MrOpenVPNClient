# vpnLib Module

## Source Version

This module built from: https://github.com/schwabe/ics-openvpn/tree/v0.7.64

## LICENSE

This project is under GPLv3 LICENSE. It mean if you use this project or a part of this project in your project it must be open source.

This project use another open source project as library detail bellow.
* [**OpenVPN for Android**](https://github.com/schwabe/ics-openvpn) under GPLv2 LICENSE (https://github.com/schwabe/ics-openvpn/blob/master/doc/LICENSE.txt)

## How to use this library (AAR file)

This project builds an Android Archive (`.aar`) file, which is a compiled library package containing the code, resources, and native libraries (like `.so` files). You can integrate this `.aar` file into your Android application project by following these steps:

### Option 1: Add as a local module (Recommended for quick integration)

1.  **Copy the `.aar` file:**
    *   Locate the generated `.aar` file after a successful build. It is typically found in `build/outputs/aar/your-library-name-release.aar`. For this project, it would be `vpnlib-release.aar` (or similar depending on your build configuration).
    *   In your Android application project, create a new directory named `libs` in the root of your app module (e.g., `app/libs`).
    *   Copy the `vpnlib-release.aar` file into this `app/libs` directory.

2.  **Add to your `build.gradle`:**
    *   Open the `build.gradle` file of your application module (e.g., `app/build.gradle`).
    *   Ensure that your `build.gradle` has `flatDir` for local AARs. Add the following to your `repositories` block (if not already present):
        ```gradle
        repositories {
            flatDir {
                dirs 'libs'
            }
            // other repositories like google(), mavenCentral()
        }
        ```
    *   Add the `.aar` file as a dependency within the `dependencies` block:
        ```gradle
        dependencies {
            // ... other dependencies
            implementation files('libs/vpnlib-release.aar')
            // If the AAR contains transitive dependencies that need to be explicitly declared,
            // you might need to add them here as well, e.g., appcompat, core-ktx, security-crypto.
            // Example:
            // implementation 'androidx.appcompat:appcompat:1.7.1'
            // implementation 'androidx.core:core-ktx:1.17.0'
            // implementation 'androidx.security:security-crypto:1.1.0'
        }
        ```

3.  **Synchronize Gradle:**
    *   Synchronize your project with Gradle files in Android Studio (File > Sync Project with Gradle Files).

### Option 2: Publish to a Maven repository (Recommended for shared libraries)

For more robust dependency management, especially in team environments or for distributing your library, you can publish the `.aar` file to a Maven repository (e.g., Maven Central, JitPack, or a private repository). Once published, other projects can easily consume your library by adding a simple Gradle dependency:

```gradle
dependencies {
    implementation 'com.example.yourlibrarygroupid:yourlibraryartifactid:1.0.0' // Replace with actual GAV coordinates
}
```

This method simplifies version management and distribution, as consumers only need to declare the dependency in their `build.gradle` file.