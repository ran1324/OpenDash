package org.opendash.diagnostics;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class IdentityEvidenceTest {
    static IdentityEvidence.Item item(String v,String s,String f){return new IdentityEvidence.Item(v,s,f);}
    static void check(boolean v,String name){if(!v)throw new AssertionError(name);}
    public static void main(String[] args) {
        check("cortex-a7".equals(IdentityEvidence.armCore("0x41","0xc07")),"A7 core decode");
        check(IdentityEvidence.armCore("0x42","0xc07")==null,"implementer matters");
        check(IdentityEvidence.armCore("0x41","0xdead")==null,"unknown not guessed");
        check("cortex-a7".equals(IdentityEvidence.midrCore("0x410fc075")),"MIDR decode");
        check(IdentityEvidence.strings("vendor,board\0mediatek,mt8127\0".getBytes(StandardCharsets.UTF_8)).size()==2,"DT string list");
        check(IdentityEvidence.strings(new byte[]{0,0,12,7}).isEmpty(),"binary cells not treated as text");
        List<Map<String,String>> records=IdentityEvidence.cpuRecords("processor : 0\nCPU implementer : 0x41\nCPU part : 0xc07\nprocessor : 1\nCPU implementer : 0x41\nCPU part : 0xc07\nSerial : PRIVATE\n");
        check(records.size()==2&&!records.get(1).containsKey("Serial"),"per-core records and serial exclusion");
        check(IdentityEvidence.correlate(Collections.emptyList(),true).state.equals("insufficient_evidence"),"unknown evidence");
        List<IdentityEvidence.Item> repeated=Arrays.asList(item("AC8227L","Build.HARDWARE","android_properties"),item("ac8227l","getprop","android_properties"));
        check(IdentityEvidence.correlate(repeated,true).state.equals("single_source_family"),"correlated declarations not independent");
        List<IdentityEvidence.Item> cross=new ArrayList<>(repeated);cross.add(item("ac8227l","DT","device_tree"));
        check(IdentityEvidence.correlate(cross,true).state.equals("agreement_across_interfaces"),"cross-interface agreement");
        cross.add(item("mt8127","soc","kernel_soc"));
        check(IdentityEvidence.correlate(cross,true).state.equals("conflicting_identifiers"),"AC and MT aliases never assumed");
        check(IdentityEvidence.correlate(cross,false).state.equals("multiple_labels_or_core_types"),"unlike board labels are not automatically hardware conflicts");
        check(IdentityEvidence.platformTokens("mediatek,mt8127 AC8227L").equals(Arrays.asList("mt8127","ac8227l")),"platform candidates");
        check(IdentityEvidence.platformTokens("Cortex-A7 1118MHz Mali-450").isEmpty(),"core/GPU names are not SoC IDs");
        check("a\0b\0".equals(IdentityEvidence.rootValue("\n@@OD:/test\na\0b\0\n@@RC:0\n","/test")),"root DT framing");
        check(IdentityEvidence.rootValue("@@OD:/test\n@@RC:unavailable\n","/test")==null,"root unavailable");
        check(IdentityEvidence.rootValue("@@OD:/test\nfailed\n@@RC:1\n","/test")==null,"root nonzero read");
        String mixed="\n@@OD:/missing\n@@RC:unavailable\n\n@@OD:/valid\nAC8227L\n@@RC:0\n";
        check(IdentityEvidence.rootValue(mixed,"/missing")==null,"missing root file cannot borrow next file result");
        check("AC8227L".equals(IdentityEvidence.rootValue(mixed,"/valid")),"next root file remains readable");
        check(IdentityEvidence.correlate(Arrays.asList(item("ac8227l","props","android_properties"),item(" unknown ","soc","kernel_soc")),true).state.equals("single_source_family"),"unknown is not corroboration");
        System.out.println("PASS: CPU/MIDR, DT binary/text, core records, source dependence, agreements, conflicts, unknowns, root framing");
    }
}
