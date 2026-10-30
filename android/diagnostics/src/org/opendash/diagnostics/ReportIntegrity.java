package org.opendash.diagnostics;

import java.io.InputStream;
import java.io.IOException;

final class ReportIntegrity {
    static boolean matches(InputStream input,byte[] expected) throws IOException {
        byte[] buffer=new byte[8192];int offset=0,n;
        while((n=input.read(buffer))!=-1) {
            if(n>expected.length-offset)return false;
            for(int i=0;i<n;i++)if(buffer[i]!=expected[offset+i])return false;
            offset+=n;
        }
        return offset==expected.length;
    }
}
