package org.opendash.diagnostics;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.InputDevice;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Xml;
import android.view.Display;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.xmlpull.v1.XmlPullParser;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

final class Diagnostics {
    private Diagnostics() { }

    static JSONObject collect(Activity activity, boolean testRoot) {
        JSONObject report = new JSONObject();
        put(report, "schemaVersion", 4);
        put(report,"reportId",java.util.UUID.randomUUID().toString());
        put(report,"toolVersion","0.4");
        long started = android.os.SystemClock.elapsedRealtime();
        put(report, "generatedAtUtc", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US) {{ setTimeZone(java.util.TimeZone.getTimeZone("UTC")); }}.format(new java.util.Date()));
        put(report, "scope", "Read-only app observations. Missing and denied values are not negative findings. No firmware image or personal app list is collected.");
        JSONObject build = section(report, "build");
        field(build, "manufacturer", Build.MANUFACTURER, "Build.MANUFACTURER");
        field(build, "brand", Build.BRAND, "Build.BRAND");
        field(build, "model", Build.MODEL, "Build.MODEL");
        field(build, "device", Build.DEVICE, "Build.DEVICE");
        field(build, "board", Build.BOARD, "Build.BOARD");
        field(build, "hardware", Build.HARDWARE, "Build.HARDWARE");
        field(build, "product", Build.PRODUCT, "Build.PRODUCT");
        field(build, "fingerprint", Build.FINGERPRINT, "Build.FINGERPRINT");
        field(build, "androidRelease", Build.VERSION.RELEASE, "Build.VERSION.RELEASE");
        field(build, "sdkInt", Build.VERSION.SDK_INT, "Build.VERSION.SDK_INT");
        field(build, "securityPatch", Build.VERSION.SECURITY_PATCH, "Build.VERSION.SECURITY_PATCH");
        field(build, "supportedAbis", new JSONArray(Arrays.asList(Build.SUPPORTED_ABIS)), "Build.SUPPORTED_ABIS");
        field(build, "kernelRelease", System.getProperty("os.version"), "os.version");

        JSONObject hardware = section(report, "hardware");
        readField(hardware, "cpuInfo", "/proc/cpuinfo", 32768);
        readField(hardware, "memoryInfo", "/proc/meminfo", 8192);
        readField(hardware, "kernelVersion", "/proc/version", 1024);
        readField(hardware, "kernelCommandLine", "/proc/cmdline", 8192);
        readField(hardware, "gpuDriver", "/proc/mali/version", 4096);
        readField(hardware, "gpuInfo", "/sys/class/misc/mali0/device/modalias", 1024);
        gpu(hardware);
        try {
            ActivityManager manager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); manager.getMemoryInfo(info);
            field(hardware,"totalRamBytes",info.totalMem,"ActivityManager.MemoryInfo");
            field(hardware,"availableRamBytes",info.availMem,"ActivityManager.MemoryInfo; snapshot");
            field(hardware,"lowMemory",info.lowMemory,"ActivityManager.MemoryInfo; snapshot");
            field(hardware,"cpuCoreCount",Runtime.getRuntime().availableProcessors(),"Runtime.availableProcessors; accessible logical cores");
        } catch (Exception e) { error(hardware,"memoryRuntime",e); }
        try {
            FeatureInfo[] features = activity.getPackageManager().getSystemAvailableFeatures();
            JSONArray names = new JSONArray();
            if (features != null) for (FeatureInfo f:features) if (f.name != null && names.length()<128) names.put(f.name);
            field(hardware,"declaredFeatures",names,"PackageManager; declared features, not proof of working hardware");
        } catch (Exception e) { error(hardware,"declaredFeatures",e); }
        try {
            SensorManager manager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
            JSONArray sensors = new JSONArray();
            for (Sensor s:manager.getSensorList(Sensor.TYPE_ALL)) {
                if (sensors.length()>=64) break;
                JSONObject item = new JSONObject(); put(item,"name",s.getName()); put(item,"vendor",s.getVendor()); put(item,"type",s.getType()); sensors.put(item);
            }
            field(hardware,"sensors",sensors,"SensorManager; advertised sensors only");
        } catch (Exception e) { error(hardware,"sensors",e); }
        try {
            JSONArray inputs = new JSONArray();
            int[] ids = InputDevice.getDeviceIds();
            for (int i=0;i<Math.min(ids.length,64);i++) {
                InputDevice device = InputDevice.getDevice(ids[i]); if (device==null) continue;
                JSONObject item = new JSONObject(); put(item,"name",device.getName()); put(item,"vendorId",device.getVendorId()); put(item,"productId",device.getProductId()); put(item,"sources",device.getSources()); inputs.put(item);
            }
            field(hardware,"inputDevices",inputs,"InputDevice; includes current touch and keys where exposed");
        } catch (Exception e) { error(hardware,"inputDevices",e); }
        try {
            AudioManager manager = (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
            JSONArray devices = new JSONArray();
            for (AudioDeviceInfo d:manager.getDevices(AudioManager.GET_DEVICES_ALL)) {
                if (devices.length()>=64) break;
                JSONObject item = new JSONObject(); put(item,"type",d.getType()); put(item,"productName",String.valueOf(d.getProductName())); put(item,"source",d.isSource()); put(item,"sink",d.isSink()); devices.put(item);
            }
            field(hardware,"audioDevices",devices,"AudioManager; currently exposed devices, not radio/MCU validation");
        } catch (Exception e) { error(hardware,"audioDevices",e); }
        JSONObject display = section(report, "display");
        try {
            Display d = activity.getWindowManager().getDefaultDisplay();
            Point real = new Point(); d.getRealSize(real);
            DisplayMetrics m = new DisplayMetrics(); d.getRealMetrics(m);
            field(display, "realPixels", real.x + "x" + real.y, "Display.getRealSize; active display only");
            field(display, "densityDpi", m.densityDpi, "Display.getRealMetrics");
            field(display, "refreshRateHz", d.getRefreshRate(), "Display.getRefreshRate");
        } catch (Exception e) { error(display, "activeDisplay", e); }
        JSONObject storage = section(report, "storage");
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            field(storage, "dataTotalBytes", s.getTotalBytes(), "StatFs /data");
            field(storage, "dataAvailableBytes", s.getAvailableBytes(), "StatFs /data");
        } catch (Exception e) { error(storage, "data", e); }

        JSONObject properties = section(report, "systemProperties");
        String[] keys = {"ro.build.display.id", "ro.build.version.release", "ro.build.version.sdk", "ro.build.version.security_patch", "ro.product.board", "ro.hardware", "ro.boot.hardware", "ro.board.platform", "ro.vendor.build.fingerprint", "ro.vendor.build.version.release", "ro.treble.enabled", "ro.vndk.version", "ro.vendor.build.version.sdk", "ro.boot.verifiedbootstate", "ro.boot.slot_suffix", "ro.build.ab_update", "ro.boot.dynamic_partitions", "ro.boot.super_partition", "ro.product.cpu.abi", "ro.sf.lcd_density"};
        Map<String,String> props = getProperties();
        for (String key : keys) {
            if (props == null) status(properties, key, "unavailable", "getprop failed or timed out");
            else if (!props.containsKey(key) || props.get(key).isEmpty()) status(properties, key, "missing", "Property is unset or not exposed");
            else field(properties, key, props.get(key), "getprop");
        }
        JSONObject treble = section(report, "trebleVintf");
        copy(properties, treble, "ro.treble.enabled");
        copy(properties, treble, "ro.vndk.version");
        for (String path : new String[]{"/vendor/etc/vintf/manifest.xml", "/system/etc/vintf/manifest.xml", "/vendor/manifest.xml", "/system/etc/vintf/compatibility_matrix.xml", "/vendor/etc/vintf/compatibility_matrix.xml"}) {
            fileMetadata(treble, path);
            if (path.endsWith("manifest.xml")) vintfHalNames(treble,path);
        }
        JSONObject partitions = section(report, "partitions");
        readField(partitions, "procPartitions", "/proc/partitions", 32768);
        readField(partitions, "mounts", "/proc/mounts", 32768);
        for (String path : new String[]{"/dev/block/by-name", "/dev/block/platform", "/dev/block/bootdevice/by-name"}) listMetadata(partitions, path);

        JSONObject packages = section(report, "oemPackages");
        try {
            List<PackageInfo> all = activity.getPackageManager().getInstalledPackages(0);
            JSONArray found = new JSONArray();
            for (PackageInfo p : all) {
                ApplicationInfo a = p.applicationInfo;
                if (a == null || (a.flags & ApplicationInfo.FLAG_SYSTEM) == 0) continue;
                String path = a.sourceDir == null ? "" : a.sourceDir;
                String name = p.packageName.toLowerCase(java.util.Locale.US);
                boolean candidate = path.startsWith("/vendor/") || path.startsWith("/oem/") || path.startsWith("/product/") || name.matches(".*(mcu|canbus|carservice|zxdz|nakamichi|radio|bluetooth|vehicle|hwcar|hiworld).*");
                if (!candidate) continue;
                JSONObject item = new JSONObject(); put(item, "package", p.packageName); put(item, "version", p.versionName); put(item, "sourceDir", path); found.put(item);
            }
            field(packages, "candidates", found, "PackageManager; system apps in vendor/oem/product or matching vehicle terms; heuristic, may omit OEM components");
            field(packages, "visiblePackageCount", all.size(), "PackageManager; count only");
        } catch (Exception e) { error(packages, "candidates", e); }

        JSONObject root = section(report, "rootCapability");
        field(root, "appUid", android.os.Process.myUid(), "Process.myUid");
        field(root, "suBinaryPaths", existingPaths(new String[]{"/system/xbin/su", "/system/bin/su", "/sbin/su", "/su/bin/su"}), "File.exists; presence does not prove usable root");
        if (testRoot) {
            CommandResult result = command(new String[]{"su", "-c", "id"}, 4000);
            if (result == null) status(root, "suId", "unavailable", "su could not start or timed out; permission prompt may need manual response");
            else if (result.exitCode == 0 && java.util.regex.Pattern.compile("(?m)^uid=0(?:\\(|\\s|$)").matcher(result.output).find()) field(root, "suId", "uid=0 confirmed", "su -c id; read-only probe");
            else status(root, "suId", "not_confirmed", "su exited " + result.exitCode + "; no uid=0 confirmation");
        } else status(root, "suId", "not_tested", "Tap Scan + optional root identity reads; su may show a permission prompt");
        ExtendedDiagnostics.collect(activity, report);
        HardwareIdentity.collect(report, props, "uid=0 confirmed".equals(root.optJSONObject("suId").optString("value")));
        field(section(report,"collection"),"elapsedMilliseconds",android.os.SystemClock.elapsedRealtime()-started,"Monotonic elapsed time; not a benchmark");
        return report;
    }

    static void readField(JSONObject section, String key, String path, int max) {
        File f = new File(path);
        if (!f.exists()) { status(section,key,"unavailable",path + " absent or inaccessible to this app"); return; }
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[max + 1]; int n = 0, r;
            while (n < b.length && (r = in.read(b,n,b.length-n)) > 0) n += r;
            if (n > max) { status(section,key,"too_large",path + " exceeds " + max + " bytes; omitted"); return; }
            field(section,key,new String(b,0,n,"UTF-8"),path);
        } catch (SecurityException e) { status(section,key,"permission_denied",path); }
          catch (Exception e) { status(section,key,"unreadable",path + ": " + e.getClass().getSimpleName()); }
    }
    private static void gpu(JSONObject section) {
        EGL10 egl = null; EGLDisplay display = null; EGLContext context = null; EGLSurface surface = null;
        try {
            egl = (EGL10) EGLContext.getEGL();
            display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (!egl.eglInitialize(display,version)) throw new IllegalStateException("eglInitialize");
            int[] attrs = {EGL10.EGL_RENDERABLE_TYPE,4,EGL10.EGL_SURFACE_TYPE,EGL10.EGL_PBUFFER_BIT,EGL10.EGL_NONE};
            EGLConfig[] configs = new EGLConfig[1]; int[] count = new int[1];
            if (!egl.eglChooseConfig(display,attrs,configs,1,count) || count[0]<1) throw new IllegalStateException("eglChooseConfig");
            context = egl.eglCreateContext(display,configs[0],EGL10.EGL_NO_CONTEXT,new int[]{0x3098,2,EGL10.EGL_NONE});
            surface = egl.eglCreatePbufferSurface(display,configs[0],new int[]{EGL10.EGL_WIDTH,1,EGL10.EGL_HEIGHT,1,EGL10.EGL_NONE});
            if (context == EGL10.EGL_NO_CONTEXT || surface == EGL10.EGL_NO_SURFACE || !egl.eglMakeCurrent(display,surface,surface,context)) throw new IllegalStateException("eglMakeCurrent");
            field(section,"glVendor",GLES20.glGetString(GLES20.GL_VENDOR),"OpenGL ES runtime");
            field(section,"glRenderer",GLES20.glGetString(GLES20.GL_RENDERER),"OpenGL ES runtime");
            field(section,"glVersion",GLES20.glGetString(GLES20.GL_VERSION),"OpenGL ES runtime");
        } catch (Exception e) { error(section,"glRuntime",e); }
        finally {
            if (egl != null && display != null && display != EGL10.EGL_NO_DISPLAY) {
                egl.eglMakeCurrent(display,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_CONTEXT);
                if (surface != null && surface != EGL10.EGL_NO_SURFACE) egl.eglDestroySurface(display,surface);
                if (context != null && context != EGL10.EGL_NO_CONTEXT) egl.eglDestroyContext(display,context);
                egl.eglTerminate(display);
            }
        }
    }
    private static void fileMetadata(JSONObject section, String path) {
        File f = new File(path);
        if (!f.exists()) { status(section,path,"unavailable","File absent or inaccessible to this app"); return; }
        try { if (!f.canRead()) status(section,path,"permission_denied","File exists but app cannot read it");
            else { JSONObject v = new JSONObject(); put(v,"bytes",f.length()); put(v,"readable",true); field(section,path,v,"File metadata; XML content not parsed"); }
        } catch (SecurityException e) { status(section,path,"permission_denied","File metadata denied"); }
    }
    private static void vintfHalNames(JSONObject section,String path) {
        File f = new File(path);
        if (!f.exists()) { status(section,path + ":halNames","unavailable","Manifest absent or inaccessible"); return; }
        if (f.length() > 262144) { status(section,path + ":halNames","too_large","Manifest exceeds 256 KiB"); return; }
        try (FileInputStream in = new FileInputStream(f)) {
            XmlPullParser parser = Xml.newPullParser(); parser.setInput(in,"UTF-8");
            JSONArray names = new JSONArray(); boolean inHal = false; int halDepth = -1;
            for (int event=parser.getEventType(); event!=XmlPullParser.END_DOCUMENT; event=parser.next()) {
                if (event==XmlPullParser.START_TAG && "hal".equals(parser.getName())) { inHal=true; halDepth=parser.getDepth(); }
                else if (event==XmlPullParser.START_TAG && inHal && parser.getDepth()==halDepth+1 && "name".equals(parser.getName())) {
                    if (names.length()<256) names.put(parser.nextText());
                } else if (event==XmlPullParser.END_TAG && inHal && parser.getDepth()==halDepth && "hal".equals(parser.getName())) inHal=false;
            }
            field(section,path + ":halNames",names,"VINTF manifest top-level HAL names; names alone do not prove working hardware");
        } catch (SecurityException e) { status(section,path + ":halNames","permission_denied","Manifest read denied"); }
          catch (Exception e) { status(section,path + ":halNames","unreadable",e.getClass().getSimpleName()); }
    }
    static void listMetadata(JSONObject section, String path) {
        File d = new File(path);
        if (!d.exists()) { status(section,path,"unavailable","Directory absent or inaccessible to this app"); return; }
        try {
            File[] files = d.listFiles();
            if (files == null) { status(section,path,"permission_denied","Directory listing unavailable"); return; }
            Arrays.sort(files, (a,b) -> a.getName().compareTo(b.getName()));
            JSONArray names = new JSONArray(); for (int i=0;i<Math.min(files.length,256);i++) names.put(files[i].getName());
            JSONObject v = new JSONObject(); put(v,"entries",names); put(v,"totalEntries",files.length);
            field(section,path,v,"Directory names only; no block devices opened");
            if(files.length>256) status(section,path+":limit","partial","First 256 entries only");
        } catch (SecurityException e) { status(section,path,"permission_denied","Directory listing denied"); }
    }
    private static JSONArray existingPaths(String[] paths) { JSONArray a = new JSONArray(); for (String p:paths) try { if (new File(p).exists()) a.put(p); } catch (SecurityException ignored) { } return a; }
    private static Map<String,String> getProperties() {
        CommandResult r = command(new String[]{"getprop"},3000);
        if (r == null || r.exitCode != 0) return null;
        Map<String,String> map = new TreeMap<>();
        for (String line:r.output.split("\n")) {
            int split = line.indexOf("]: [");
            if (line.startsWith("[") && split > 1 && line.endsWith("]")) map.put(line.substring(1,split),line.substring(split+4,line.length()-1));
        }
        return map;
    }
    private static final class CommandResult { final int exitCode; final String output; CommandResult(int code,String text){exitCode=code;output=text;} }
    private static CommandResult command(String[] args,long timeout) {
        BoundedCommand.Result result=BoundedCommand.run(args,timeout,131072);
        if(result.timedOut || !result.error.isEmpty() || result.truncated) return null;
        return new CommandResult(result.exitCode,result.output);
    }
    static JSONObject section(JSONObject report,String name) { JSONObject s=new JSONObject(); put(report,name,s); return s; }
    static void put(JSONObject o,String k,Object v) { try { o.put(k,v==null?JSONObject.NULL:v); } catch (Exception ignored) { } }
    static void field(JSONObject o,String k,Object v,String source) { JSONObject f=new JSONObject(); put(f,"status",v==null?"unavailable":"readable"); put(f,"value",v); put(f,"source",source); put(o,k,f); }
    static void status(JSONObject o,String k,String s,String note) { JSONObject f=new JSONObject(); put(f,"status",s); put(f,"note",note); put(o,k,f); }
    static void error(JSONObject o,String k,Exception e) { status(o,k,e instanceof SecurityException?"permission_denied":"unavailable",e.getClass().getSimpleName()); }
    private static void copy(JSONObject from,JSONObject to,String key) { put(to,key,from.optJSONObject(key)); }
}
