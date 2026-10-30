package org.opendash.diagnostics;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/** Drains output while waiting so a full OS pipe cannot deadlock a read-only probe. */
final class BoundedCommand {
    static final class Result {
        int exitCode=-1; String output="", error=""; boolean timedOut, truncated;
        boolean ok(){return exitCode==0 && !timedOut && error.isEmpty();}
    }
    static Result run(String[] args,long timeout,int limit) {
        Result result=new Result(); Process process=null;
        try {
            process=new ProcessBuilder(args).redirectErrorStream(true).start();
            final Process child=process; final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            Thread reader=new Thread(()->{
                try(InputStream input=child.getInputStream()) {
                    byte[] buffer=new byte[4096]; int n;
                    while((n=input.read(buffer))!=-1) {
                        int keep=Math.min(n,Math.max(0,limit-bytes.size()));
                        bytes.write(buffer,0,keep); if(keep<n) result.truncated=true;
                    }
                } catch(Exception e){ result.error=e.getClass().getSimpleName(); }
            },"opendash-command-reader");
            reader.setDaemon(true); reader.start();
            if(!process.waitFor(timeout,TimeUnit.MILLISECONDS)) {result.timedOut=true; process.destroyForcibly();}
            else result.exitCode=process.exitValue();
            reader.join(250);
            if(reader.isAlive()) {result.error="Output reader did not finish"; process.getInputStream().close();}
            else result.output=bytes.toString("UTF-8");
        } catch(Exception e){result.error=e.getClass().getSimpleName();}
        finally {if(process!=null) process.destroy();}
        return result;
    }
}
