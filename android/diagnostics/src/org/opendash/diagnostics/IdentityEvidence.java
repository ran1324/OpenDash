package org.opendash.diagnostics;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Pure parsing/correlation: software declarations never certify a physical board. */
final class IdentityEvidence {
    static final class Item {
        final String value,source,family;
        Item(String v,String s,String f){value=v;source=s;family=f;}
    }
    static final class Finding {
        final String state;
        final Map<String,List<Item>> candidates;
        Finding(String s,Map<String,List<Item>> c){state=s;candidates=c;}
    }
    static List<String> strings(byte[] bytes) {
        List<String> values=new ArrayList<>();
        for(byte b:bytes) {int n=b&255;if(n!=0 && n!=9 && n!=10 && n!=13 && (n<32 || n>126))return values;}
        for(String v:new String(bytes,StandardCharsets.UTF_8).split("\u0000"))if(!v.trim().isEmpty())values.add(v.trim());
        return values;
    }
    static List<Map<String,String>> cpuRecords(String text) {
        List<Map<String,String>> records=new ArrayList<>();Map<String,String> current=new LinkedHashMap<>();
        for(String line:text.split("\n")) {
            if(line.trim().isEmpty()){if(!current.isEmpty()){records.add(current);current=new LinkedHashMap<>();}continue;}
            int i=line.indexOf(':');if(i<0)continue;
            String key=line.substring(0,i).trim(),value=line.substring(i+1).trim();
            if(key.equals("processor")&&current.containsKey("processor")){records.add(current);current=new LinkedHashMap<>();}
            if(!key.equalsIgnoreCase("Serial"))current.put(key,value);
        }
        if(!current.isEmpty())records.add(current);return records;
    }
    static String armCore(String implementer,String part) {
        try {if(Long.decode(implementer)==0x41 && Long.decode(part)==0xc07)return "cortex-a7";}catch(Exception ignored){}
        return null; // Other parts remain unclassified instead of guessing.
    }
    static String midrCore(String value) {
        try {long midr=Long.decode(value.trim());return armCore("0x"+Long.toHexString((midr>>>24)&255),"0x"+Long.toHexString((midr>>>4)&4095));}
        catch(Exception e){return null;}
    }
    static List<String> platformTokens(String value) {
        List<String> out=new ArrayList<>();
        Matcher m=Pattern.compile("(?i)(?<![a-z0-9])(?:ac|mt)[0-9]{4}[a-z0-9]*(?![a-z0-9])").matcher(value);
        while(m.find())if(!out.contains(m.group().toLowerCase(Locale.US)))out.add(m.group().toLowerCase(Locale.US));
        return out;
    }
    static Finding correlate(List<Item> items,boolean differingIsConflict) {
        Map<String,List<Item>> candidates=new TreeMap<>();
        for(Item i:items) {
            String key=i.value.trim().toLowerCase(Locale.US);
            if(key.isEmpty() || key.equals("unknown"))continue;
            if(!candidates.containsKey(key))candidates.put(key,new ArrayList<>());
            candidates.get(key).add(i);
        }
        if(candidates.isEmpty())return new Finding("insufficient_evidence",candidates);
        if(candidates.size()>1)return new Finding(differingIsConflict?"conflicting_identifiers":"multiple_labels_or_core_types",candidates);
        Set<String> families=new HashSet<>();for(Item i:candidates.values().iterator().next())families.add(i.family);
        return new Finding(families.size()>1?"agreement_across_interfaces":"single_source_family",candidates);
    }
    static String rootValue(String output,String path) {
        String marker="@@OD:"+path+"\n";int start=output.indexOf(marker);
        if(start<0)return null;
        int next=output.indexOf("\n@@OD:",start+marker.length());
        String body=output.substring(start+marker.length(),next<0?output.length():next);
        int rc=body.lastIndexOf("@@RC:");if(rc<0)return null;
        if(!body.substring(rc+5).trim().equals("0"))return null;
        if(rc==0 || body.charAt(rc-1)!='\n')return null;
        return body.substring(0,rc-1);
    }
}
