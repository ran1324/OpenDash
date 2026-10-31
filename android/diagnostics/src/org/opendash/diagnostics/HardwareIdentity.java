package org.opendash.diagnostics;

import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import org.json.*;
import java.io.*;
import java.util.*;

/** Fixed identity paths and bounded metadata enumeration; no device nodes or firmware dumps. */
final class HardwareIdentity {
    private final JSONObject evidence=new JSONObject(),parsed=new JSONObject();
    private final List<IdentityEvidence.Item> chips=new ArrayList<>(),cores=new ArrayList<>(),boards=new ArrayList<>();
    private int bytesRead;
    private static final int FILE_LIMIT=65536, TOTAL_LIMIT=2097152;
    private static final String[] ROOT_PATHS={"/proc/cpuinfo","/sys/firmware/devicetree/base/model","/sys/firmware/devicetree/base/compatible","/proc/device-tree/model","/proc/device-tree/compatible","/sys/devices/soc0/machine","/sys/devices/soc0/family","/sys/devices/soc0/soc_id","/sys/devices/soc0/revision"};
    static void collect(JSONObject report,Map<String,String> props,boolean rootConfirmed) {
        HardwareIdentity scan=new HardwareIdentity();JSONObject section=Diagnostics.section(report,"hardwareIdentity");
        try {scan.run(props,rootConfirmed);}catch(Exception e){Diagnostics.error(scan.evidence,"collector",e);}
        Diagnostics.put(section,"evidence",scan.evidence);Diagnostics.put(section,"decoded",scan.parsed);
        JSONObject findings=Diagnostics.section(section,"crossChecks");
        scan.finding(findings,"platformIdentifiers",scan.chips,true);
        scan.finding(findings,"cpuCoreTypes",scan.cores,false);
        scan.finding(findings,"boardAndProductLabels",scan.boards,false);
        Diagnostics.status(findings,"physicalBoardRevision","not_verified","Software board, SoC and CPU revisions have different meanings; no PCB revision is inferred from them.");
        Diagnostics.status(findings,"exactSocPackage","not_verified","ac8227l/mt* identifiers are reported platform candidates, not proof of the package marking. No AC/MT aliases are assumed.");
        Diagnostics.field(section,"interpretation","Build fields, getprop and build.prop share one source family; both device-tree paths share another. Root access does not create an independent source. Agreement across software interfaces can still reflect shared firmware declarations. Cortex-A7 identifies a CPU core, not the complete SoC.","Correlation rules");
        Diagnostics.field(section,"limits","Identity files: 64 KiB each, 2 MiB total; up to 32 CPU directories and 32 device-tree CPU nodes, 8 SoC nodes. Root identity read: fixed allowlist only, 6 seconds / 256 KiB. Unknown or denied sources are retained as statuses. No recursive whole-filesystem scan.","Collection bounds");
        Diagnostics.field(section,"readBytes",scan.bytesRead,"Identity file payload bytes read, excluding optional root command");
    }
    private void run(Map<String,String> props,boolean rootConfirmed) {
        observe("Build.BOARD","android_properties",Build.BOARD,"board");
        observe("Build.HARDWARE","android_properties",Build.HARDWARE,"platform");
        observe("Build.MODEL","android_properties",Build.MODEL,"board");
        if(props==null)Diagnostics.status(evidence,"getprop","unavailable","Property retrieval failed");
        else for(Map.Entry<String,String> p:props.entrySet())if(identityProperty(p.getKey()))observe("getprop:"+p.getKey(),"android_properties",p.getValue(),propertyRole(p.getKey()));
        for(String path:new String[]{"/system/build.prop","/vendor/build.prop","/odm/build.prop","/product/build.prop","/system/vendor/build.prop"}) {
            byte[] raw=read(path,false);if(raw==null)continue;
            JSONObject selected=new JSONObject();
            for(String line:new String(raw,java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                int eq=line.indexOf('=');if(eq<=0)continue;String key=line.substring(0,eq).trim();
                if(identityProperty(key)){String value=line.substring(eq+1).trim();Diagnostics.put(selected,key,value);observe(path+":"+key,"android_properties",value,propertyRole(key));}
            }
            Diagnostics.field(evidence,path,selected,"Only allowlisted identity properties retained; complete file content discarded");
        }
        byte[] cpu=read("/proc/cpuinfo",false);if(cpu!=null)cpuInfo("/proc/cpuinfo",new String(cpu,java.nio.charset.StandardCharsets.UTF_8));
        for(String path:new String[]{"/sys/devices/system/cpu/possible","/sys/devices/system/cpu/present","/sys/devices/system/cpu/online","/sys/devices/system/cpu/offline"})read(path,true);
        for(File f:children("/sys/devices/system/cpu","cpu[0-9]+",32)) {
            for(String attr:new String[]{"online","topology/core_id","topology/physical_package_id","topology/thread_siblings_list","cpufreq/cpuinfo_max_freq","cpufreq/cpuinfo_min_freq","cpufreq/scaling_cur_freq","cpufreq/scaling_driver","cpufreq/scaling_governor","cpufreq/scaling_available_frequencies","regs/identification/midr_el1"}) {
                byte[] raw=read(f+"/"+attr,true);
                if(raw!=null&&attr.endsWith("midr_el1")){String core=IdentityEvidence.midrCore(new String(raw).trim());if(core!=null)cores.add(new IdentityEvidence.Item(core,f+"/"+attr,"kernel_cpu"));}
            }
        }
        for(String base:new String[]{"/sys/firmware/devicetree/base","/proc/device-tree"}) {
            for(String attr:new String[]{"model","compatible","name","revision","board-id","board_id","board-revision","hardware-revision"})readIdentity(base+"/"+attr,"device_tree",attr.equals("compatible")?"platform":"board");
            for(File f:children(base+"/cpus","cpu(@.*)?",32))for(String attr:new String[]{"compatible","device_type","reg","clock-frequency","enable-method","status"})readIdentity(f+"/"+attr,"device_tree",attr.equals("compatible")?"cpu":"metadata");
        }
        Set<String> socPaths=new LinkedHashSet<>(Arrays.asList("/sys/devices/soc0","/sys/devices/system/soc/soc0"));
        for(File f:children("/sys/bus/soc/devices",".*",8))socPaths.add(f.getPath());
        for(String base:socPaths)for(String attr:new String[]{"machine","family","soc_id","revision","process"})readIdentity(base+"/"+attr,"kernel_soc",attr.equals("machine")?"board":"platform");
        if(rootConfirmed)rootRead();else Diagnostics.status(evidence,"optionalRootIdentity","not_tested","Tap Scan + root identity reads and approve su if available to retry a fixed set of identity paths");
    }
    private static boolean identityProperty(String key) {
        return key.matches("ro\\.(product\\.(?:(?:vendor|odm|system|product|system_ext)\\.)?(?:brand|manufacturer|model|device|name|board|cpu\\.(?:abi|abi2|abilist|abilist32|abilist64))|hardware(?:\\.[a-z0-9_]+)?|board\\.platform|mediatek\\.platform|soc\\.(?:model|manufacturer)|boot\\.(?:hardware|hardware\\.revision|board_id|board_revision|revision|hwrev)|bootloader|revision)");
    }
    private static String propertyRole(String key){return key.contains("platform")||key.contains("hardware")||key.contains("soc.")?"platform":key.contains("cpu.")?"metadata":"board";}
    private File[] children(String path,String regex,int max) {
        File[] all;
        try {all=new File(path).listFiles(f->f.getName().matches(regex));}
        catch(SecurityException e){Diagnostics.status(evidence,path+":listing","permission_denied","Directory listing denied");return new File[0];}
        if(all==null){Diagnostics.status(evidence,path+":listing","unavailable","Directory absent or inaccessible");return new File[0];}
        Arrays.sort(all,(a,b)->a.getName().compareTo(b.getName()));
        Diagnostics.field(evidence,path+":listing",new JSONArray(Arrays.asList(Arrays.stream(all).map(File::getName).toArray(String[]::new))),"Matching directory names");
        if(all.length>max)Diagnostics.status(evidence,path+":limit","partial","Inspecting first "+max+" of "+all.length+" entries");
        return Arrays.copyOf(all,Math.min(max,all.length));
    }
    private byte[] read(String path,boolean retain) {
        if(bytesRead>=TOTAL_LIMIT){Diagnostics.status(evidence,path,"partial","Total identity byte budget reached");return null;}
        try {
            Os.stat(path); // distinguishes ENOENT from EACCES when the kernel exposes that distinction
            try(InputStream in=new FileInputStream(path);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] b=new byte[4096];int n;
                while((n=in.read(b,0,Math.min(b.length,FILE_LIMIT+1-out.size())))>0){out.write(b,0,n);bytesRead+=n;if(out.size()>FILE_LIMIT||bytesRead>TOTAL_LIMIT){Diagnostics.status(evidence,path,"too_large","Identity read budget exceeded; value omitted");return null;}}
                byte[] raw=out.toByteArray();if(retain)retain(path,raw);return raw;
            }
        }catch(ErrnoException e){Diagnostics.status(evidence,path,e.errno==OsConstants.EACCES||e.errno==OsConstants.EPERM?"permission_denied":e.errno==OsConstants.ENOENT?"missing":"unavailable","stat errno="+e.errno);}
        catch(Exception e){Diagnostics.status(evidence,path,"unreadable",e.getClass().getSimpleName()+"; app UID access");}
        return null;
    }
    private void retain(String source,byte[] bytes) {
        List<String> values=IdentityEvidence.strings(bytes);JSONObject value=new JSONObject();
        Diagnostics.put(value,"byteLength",bytes.length);
        if(!values.isEmpty())Diagnostics.put(value,"strings",new JSONArray(values));
        else {StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format(Locale.US,"%02x",b&255));Diagnostics.put(value,"hex",hex.toString());}
        Diagnostics.field(evidence,source,value,"File payload; NUL-delimited printable strings or uninterpreted hex");
    }
    private void readIdentity(String path,String family,String role) {
        byte[] raw=read(path,true);if(raw!=null)for(String s:IdentityEvidence.strings(raw))add(path,family,s,role);
    }
    private void observe(String source,String family,String value,String role) {
        Diagnostics.field(evidence,source,value,"Identity declaration; source family: "+family);if(value!=null)add(source,family,value,role);
    }
    private void add(String source,String family,String value,String role) {
        if(role.equals("platform")||role.equals("board"))for(String t:IdentityEvidence.platformTokens(value))chips.add(new IdentityEvidence.Item(t,source,family));
        if(role.equals("board")&&!value.trim().isEmpty())boards.add(new IdentityEvidence.Item(value,source,family));
        if(role.equals("cpu")) {
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)cortex-a[0-9]+").matcher(value);
            while(m.find())cores.add(new IdentityEvidence.Item(m.group().toLowerCase(Locale.US),source,family));
        }
    }
    private void cpuInfo(String source,String text) {
        JSONArray records=new JSONArray();int index=0;
        for(Map<String,String> r:IdentityEvidence.cpuRecords(text)) {
            JSONObject record=new JSONObject(r);String core=IdentityEvidence.armCore(r.get("CPU implementer"),r.get("CPU part"));
            if(core!=null){Diagnostics.put(record,"decodedCore",core);cores.add(new IdentityEvidence.Item(core,source+"#record"+index,"kernel_cpu"));}
            if(r.containsKey("Hardware"))add(source+"#Hardware","kernel_cpu",r.get("Hardware"),"board");
            if(r.containsKey("model name"))add(source+"#model name","kernel_cpu",r.get("model name"),"cpu");
            records.put(record);index++;
        }
        Diagnostics.field(parsed,source,records,"Parsed CPU records; serial omitted; ARM implementer 0x41 + part 0xc07 decodes to Cortex-A7 only");
        Diagnostics.field(evidence,source,"See decoded CPU records","/proc/cpuinfo; one kernel CPU source family");
    }
    private void finding(JSONObject out,String key,List<IdentityEvidence.Item> items,boolean conflict) {
        IdentityEvidence.Finding result=IdentityEvidence.correlate(items,conflict);JSONObject f=new JSONObject(),values=new JSONObject();
        Diagnostics.put(f,"status",result.state);
        for(Map.Entry<String,List<IdentityEvidence.Item>> entry:result.candidates.entrySet()) {
            JSONArray sources=new JSONArray();Set<String> families=new TreeSet<>();
            for(IdentityEvidence.Item i:entry.getValue()){JSONObject s=new JSONObject();Diagnostics.put(s,"source",i.source);Diagnostics.put(s,"family",i.family);sources.put(s);families.add(i.family);}
            JSONObject candidate=new JSONObject();Diagnostics.put(candidate,"sources",sources);Diagnostics.put(candidate,"sourceFamilies",new JSONArray(families));Diagnostics.put(values,entry.getKey(),candidate);
        }
        Diagnostics.put(f,"candidates",values);Diagnostics.put(f,"note",conflict?"Different tokens require review; they may be vendor aliases. No automatic equivalence or ROM recommendation.":"Different board labels can describe different layers; multiple CPU types can describe heterogeneous cores.");Diagnostics.put(out,key,f);
    }
    private void rootRead() {
        StringBuilder script=new StringBuilder();
        for(String path:ROOT_PATHS)script.append("printf '\\n@@OD:").append(path).append("\\n'; if [ -r '").append(path).append("' ]; then /system/bin/cat '").append(path).append("'; printf '\\n@@RC:%s\\n' \"$?\"; else printf '@@RC:unavailable\\n'; fi;\n");
        BoundedCommand.Result r=BoundedCommand.run(new String[]{"su","-c",script.toString()},6000,262144);
        if(!r.ok()||r.truncated){Diagnostics.status(evidence,"optionalRootIdentity",r.timedOut?"timeout":"unavailable","Fixed root read incomplete; output omitted. No permissions or settings changed.");return;}
        for(String path:ROOT_PATHS) {
            String source="root:"+path;
            String value=IdentityEvidence.rootValue(r.output,path);
            if(value==null){Diagnostics.status(evidence,source,"unavailable","File unavailable or read failed through root; no value inferred");continue;}
            if(path.equals("/proc/cpuinfo"))cpuInfo(source,value);
            else {retain(source,value.getBytes(java.nio.charset.StandardCharsets.UTF_8));for(String s:IdentityEvidence.strings(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))add(source,path.contains("device-tree")||path.contains("devicetree")?"device_tree":"kernel_soc",s,path.endsWith("model")||path.endsWith("machine")?"board":"platform");}
        }
    }
}
