package org.opendash.diagnostics;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.security.MessageDigest;

/** Only app-owned report files. No raw hardware files or firmware writes. */
final class ReportFiles {
    private static final int LIMIT=8*1024*1024;
    static synchronized void save(Context context,String name,String json) throws IOException {
        byte[] data=json.getBytes("UTF-8");
        if(data.length>LIMIT) throw new IOException("Report exceeds 8 MiB storage limit");
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),name));
        FileOutputStream out=null;
        try {out=file.startWrite();out.write(data);file.finishWrite(out);}
        catch(IOException e){if(out!=null) file.failWrite(out);throw e;}
    }
    static synchronized String load(Context context,String name) throws IOException {
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),name));
        try(InputStream in=file.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192]; int n;
            while((n=in.read(b))!=-1) {if(out.size()+n>LIMIT) throw new IOException("Report exceeds limit");out.write(b,0,n);}
            return out.toString("UTF-8");
        }
    }
    static String digest(byte[] bytes) throws Exception {
        byte[] sum=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder hex=new StringBuilder();
        for(byte b:sum) hex.append(String.format(java.util.Locale.US,"%02x",b&255));
        return hex.toString();
    }
}
