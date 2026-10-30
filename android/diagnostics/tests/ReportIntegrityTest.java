package org.opendash.diagnostics;
import java.io.ByteArrayInputStream;
public final class ReportIntegrityTest {
    public static void main(String[] args)throws Exception {
        byte[] expected=new byte[20001];for(int i=0;i<expected.length;i++)expected[i]=(byte)i;
        check(ReportIntegrity.matches(new ByteArrayInputStream(expected),expected),"full report");
        check(!ReportIntegrity.matches(new ByteArrayInputStream(new byte[0]),expected),"empty export");
        check(!ReportIntegrity.matches(new ByteArrayInputStream(java.util.Arrays.copyOf(expected,20000)),expected),"truncated export");
        check(!ReportIntegrity.matches(new ByteArrayInputStream(java.util.Arrays.copyOf(expected,20002)),expected),"extra trailing byte");
        byte[] bad=expected.clone();bad[10000]^=1;
        check(!ReportIntegrity.matches(new ByteArrayInputStream(bad),expected),"changed byte");
        System.out.println("PASS: export equality, empty, truncated, extended and corrupted output");
    }
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
