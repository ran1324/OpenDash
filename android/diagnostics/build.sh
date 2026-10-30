#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-$SDK/build-tools/34.0.0}"
PLATFORM="${ANDROID_PLATFORM_JAR:-$SDK/platforms/android-27/android.jar}"
JAVAC="${JAVAC:-/opt/homebrew/opt/openjdk@17/bin/javac}"
KEYTOOL="${KEYTOOL:-/opt/homebrew/opt/openjdk@17/bin/keytool}"
mkdir -p build/classes build/dex build/release
"$JAVAC" --release 8 -classpath "$PLATFORM" -d build/classes src/org/opendash/diagnostics/*.java
"$BUILD_TOOLS/d8" --min-api 27 --output build/dex $(find build/classes -name '*.class' -print)
"$BUILD_TOOLS/aapt2" link -o build/base.apk --manifest AndroidManifest.xml -I "$PLATFORM" --min-sdk-version 27 --target-sdk-version 27
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [[ ! -f build/debug.keystore ]]; then
  "$KEYTOOL" -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias opendash-diagnostics -keyalg RSA -keysize 3072 -validity 3650 -dname 'CN=OpenDash Diagnostics Local Build' -noprompt >/dev/null 2>&1
fi
"$BUILD_TOOLS/apksigner" sign --ks build/debug.keystore --ks-key-alias opendash-diagnostics --ks-pass pass:android --key-pass pass:android --out build/release/opendash-diagnostics-debug.apk build/aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose build/release/opendash-diagnostics-debug.apk
echo "APK: $(pwd)/build/release/opendash-diagnostics-debug.apk"
