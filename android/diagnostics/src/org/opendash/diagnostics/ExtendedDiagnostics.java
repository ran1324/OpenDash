package org.opendash.diagnostics;

import android.content.Context;
import android.content.pm.*;
import android.hardware.camera2.*;
import android.hardware.usb.*;
import android.location.LocationManager;
import android.media.*;
import android.system.Os;
import org.json.*;
import java.io.File;
import java.util.*;

/** Bounded inventories only: never opens a device, configures hardware or activates a sensor. */
final class ExtendedDiagnostics {
    static void collect(Context context, JSONObject report) {
        JSONObject drivers = Diagnostics.section(report,"kernelDrivers");
        for (String path:new String[]{"/proc/modules","/proc/devices","/proc/filesystems","/proc/bus/input/devices","/proc/asound/cards","/proc/asound/pcm","/proc/tty/drivers"})
            Diagnostics.readField(drivers,path,path,32768);
        for (String path:new String[]{"/sys/module","/vendor/lib/modules","/system/lib/modules","/vendor/lib/hw","/system/lib/hw","/vendor/lib64/hw","/system/lib64/hw","/vendor/bin/hw","/vendor/etc/vintf/manifest","/system/etc/vintf/manifest"})
            Diagnostics.listMetadata(drivers,path);
        JSONObject buses = Diagnostics.section(report,"driverBindings");
        for (String bus:new String[]{"platform","i2c","spi","usb","sdio","pci"}) {
            Diagnostics.listMetadata(buses,"/sys/bus/"+bus+"/drivers");
            devices(buses,"/sys/bus/"+bus+"/devices",new String[]{"modalias"},64);
        }
        JSONObject sys = Diagnostics.section(report,"deviceMetadata");
        devices(sys,"/sys/class/input",new String[]{"name","device/name"},32);
        devices(sys,"/sys/class/thermal",new String[]{"type","temp"},32);
        devices(sys,"/sys/class/backlight",new String[]{"max_brightness","actual_brightness"},8);
        devices(sys,"/sys/class/graphics",new String[]{"name","virtual_size","bits_per_pixel"},8);
        devices(sys,"/sys/class/video4linux",new String[]{"name"},16);
        devices(sys,"/sys/devices/system/cpu/cpufreq",new String[]{"scaling_driver","scaling_governor","cpuinfo_max_freq","cpuinfo_min_freq","scaling_available_frequencies"},16);
        devices(sys,"/sys/block",new String[]{"size","queue/logical_block_size","device/name","device/type","device/manfid","device/oemid","device/fwrev"},32);
        devices(sys,"/sys/class/net",new String[]{"type","operstate"},16);
        Diagnostics.readField(sys,"selinuxEnforce","/sys/fs/selinux/enforce",32);
        JSONObject services = Diagnostics.section(report,"services");
        command(services,"binderServices",new String[]{"/system/bin/service","list"});
        command(services,"dumpServiceNames",new String[]{"/system/bin/dumpsys","-l"});
        packages(context,services);
        cameras(context,Diagnostics.section(report,"cameras"));
        usb(context,Diagnostics.section(report,"usb"));
        codecs(Diagnostics.section(report,"mediaCodecs"));
        JSONObject location = Diagnostics.section(report,"location");
        try {
            LocationManager lm=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            Diagnostics.field(location,"providers",new JSONArray(lm.getAllProviders()),"LocationManager; no location requested; not a GPS fix test");
        } catch(Exception e) { Diagnostics.error(location,"providers",e); }
        JSONObject coverage = Diagnostics.section(report,"coverage");
        Diagnostics.field(coverage,"evidenceLevels","Declared features, enumerated components and readable metadata are inventory evidence. OpenGL context creation and optional su identity are the only active probes.","Collector design");
        for(String item:new String[]{"MCU firmware/protocol","CAN box communication","reverse-camera trigger/video","radio reception","steering-wheel controls","Bluetooth pairing/calls","audio playback/microphone","GPS fix","touch accuracy","suspend/resume and ignition power","kernel driver correctness"})
            Diagnostics.status(coverage,item,"not_tested","Requires device-specific integration or a supervised functional test; absence from app-visible inventories is inconclusive");
        Diagnostics.field(coverage,"limits","No recursive filesystem scan. Each bus: 64 devices; lists: 256 entries; attribute: 4 KiB; commands: 3 seconds/128 KiB. No root inventory. Built-in drivers need not appear in /proc/modules. OEM camera/radio/CAN paths may bypass Android APIs.","Scan limits");
    }
    private static void devices(JSONObject out,String path,String[] attrs,int cap) {
        JSONObject inventory=new JSONObject();
        File[] files=new File(path).listFiles();
        if(files==null){ Diagnostics.status(out,path,"unavailable","Directory absent or listing denied"); return; }
        Arrays.sort(files,(a,b)->a.getName().compareTo(b.getName()));
        for(int i=0;i<Math.min(cap,files.length);i++) {
            File f=files[i]; JSONObject device=new JSONObject();
            try { Diagnostics.field(device,"driver",Os.readlink(f.getPath()+"/driver"),"sysfs driver symlink; binding does not prove function"); }
            catch(Exception e){ Diagnostics.status(device,"driver","unavailable","No readable driver symlink: "+e.getClass().getSimpleName()); }
            for(String attr:attrs) Diagnostics.readField(device,attr,f.getPath()+"/"+attr,4096);
            Diagnostics.put(inventory,f.getName(),device);
        }
        Diagnostics.field(out,path,inventory,"sysfs metadata; selected attributes only");
        Diagnostics.field(out,path+":totalEntries",files.length,"Directory enumeration");
        if(files.length>cap) Diagnostics.status(out,path+":limit","partial","Only first "+cap+" entries inspected");
    }
    private static void command(JSONObject out,String name,String[] args) {
        BoundedCommand.Result r=BoundedCommand.run(args,3000,131072);
        if(!r.ok()) { Diagnostics.status(out,name,r.timedOut?"timeout":"unavailable","Read-only service inventory failed; exit="+r.exitCode+"; "+r.error); return; }
        if(r.output.toLowerCase(Locale.US).contains("permission denial") || r.output.toLowerCase(Locale.US).contains("permission denied")) {
            Diagnostics.status(out,name,"permission_denied","Service inventory access denied"); return;
        }
        Diagnostics.field(out,name,r.output,"Read-only command: "+Arrays.toString(args));
        if(r.truncated) Diagnostics.status(out,name+":limit","partial","Command output capped at 128 KiB");
    }
    private static void packages(Context c,JSONObject out) {
        try {
            List<PackageInfo> packages=c.getPackageManager().getInstalledPackages(PackageManager.GET_SERVICES);
            JSONArray system=new JSONArray(); int total=0;
            for(PackageInfo p:packages) {
                if(p.applicationInfo==null || (p.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==0) continue;
                total++; if(system.length()>=300) continue;
                JSONObject entry=new JSONObject(); Diagnostics.put(entry,"package",p.packageName);
                Diagnostics.put(entry,"version",p.versionName); Diagnostics.put(entry,"sourceDir",p.applicationInfo.sourceDir);
                Diagnostics.put(entry,"enabled",p.applicationInfo.enabled);
                JSONArray declared=new JSONArray();
                if(p.services!=null) for(int i=0;i<Math.min(p.services.length,64);i++) declared.put(p.services[i].name);
                Diagnostics.put(entry,"declaredServices",declared);
                Diagnostics.put(entry,"serviceListTruncated",p.services!=null && p.services.length>64);
                system.put(entry);
            }
            Diagnostics.field(out,"systemPackages",system,"PackageManager system apps including vendor apps under /system; declarations do not prove running services");
            Diagnostics.field(out,"systemPackageCount",total,"PackageManager visible inventory");
            if(total>300) Diagnostics.status(out,"systemPackages:limit","partial","First 300 system packages only");
        } catch(Exception e){ Diagnostics.error(out,"systemPackages",e); }
    }
    private static void cameras(Context c,JSONObject out) {
        try {
            CameraManager cm=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids=cm.getCameraIdList();
            if(ids.length>16)Diagnostics.status(out,"characteristicsLimit","partial","Characteristics limited to first 16 cameras");
            Diagnostics.field(out,"ids",new JSONArray(Arrays.asList(ids)),"CameraManager; cameras are not opened; OEM reverse camera may be separate");
            for(int i=0;i<Math.min(ids.length,16);i++) try {
                CameraCharacteristics cc=cm.getCameraCharacteristics(ids[i]); JSONObject info=new JSONObject();
                Diagnostics.field(info,"hardwareLevel",cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL),"CameraCharacteristics");
                Diagnostics.field(info,"lensFacing",cc.get(CameraCharacteristics.LENS_FACING),"CameraCharacteristics");
                Diagnostics.put(out,ids[i],info);
            } catch(Exception e){ Diagnostics.error(out,ids[i],e); }
        } catch(Exception e){ Diagnostics.error(out,"inventory",e); }
    }
    private static void usb(Context c,JSONObject out) {
        try {
            UsbManager um=(UsbManager)c.getSystemService(Context.USB_SERVICE); JSONArray devices=new JSONArray();
            Map<String,UsbDevice> visible=um.getDeviceList();
            if(visible.size()>64)Diagnostics.status(out,"limit","partial","First 64 USB devices only");
            for(UsbDevice d:visible.values()) {
                if(devices.length()>=64) break;
                JSONObject item=new JSONObject(); Diagnostics.put(item,"vendorId",d.getVendorId()); Diagnostics.put(item,"productId",d.getProductId());
                Diagnostics.put(item,"deviceClass",d.getDeviceClass()); Diagnostics.put(item,"interfaceCount",d.getInterfaceCount()); devices.put(item);
            }
            Diagnostics.field(out,"hostDevices",devices,"UsbManager; enumeration only, no permission request or connection opened");
        } catch(Exception e){ Diagnostics.error(out,"hostDevices",e); }
    }
    private static void codecs(JSONObject out) {
        try {
            MediaCodecInfo[] codecs=new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos(); JSONArray entries=new JSONArray();
            for(int i=0;i<Math.min(codecs.length,128);i++) {
                MediaCodecInfo codec=codecs[i]; JSONObject item=new JSONObject();
                Diagnostics.put(item,"name",codec.getName()); Diagnostics.put(item,"encoder",codec.isEncoder());
                Diagnostics.put(item,"mimeTypes",new JSONArray(Arrays.asList(codec.getSupportedTypes()))); entries.put(item);
            }
            Diagnostics.field(out,"advertisedCodecs",entries,"MediaCodecList; no encode/decode benchmark or hardware acceleration claim");
            if(codecs.length>128) Diagnostics.status(out,"limit","partial","First 128 codecs only");
        } catch(Exception e){ Diagnostics.error(out,"advertisedCodecs",e); }
    }
}
