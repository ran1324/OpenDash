package org.opendash.diagnostics;

/** Host tests for output pressure, failure and deadline handling; no device needed. */
public final class BoundedCommandTest {
    public static void main(String[] args) throws Exception {
        if(args.length>0) {
            if(args[0].equals("large")) { for(int i=0;i<200000;i++) System.out.print("abcdefghij"); }
            else if(args[0].equals("sleep")) Thread.sleep(10000);
            else if(args[0].equals("fail")) System.exit(7);
            else System.out.print("uid=0(root)");
            return;
        }
        BoundedCommand.Result normal=probe("normal",3000,1024);
        require(normal.ok() && normal.output.equals("uid=0(root)"),"normal output");
        BoundedCommand.Result large=probe("large",10000,8192);
        require(large.ok() && large.truncated && large.output.length()==8192,"large output drained and bounded");
        long start=System.nanoTime();
        BoundedCommand.Result timeout=probe("sleep",150,1024);
        require(timeout.timedOut && (System.nanoTime()-start)/1000000<2000,"deadline enforced");
        require(probe("fail",3000,1024).exitCode==7,"exit status retained");
        require(!BoundedCommand.run(new String[]{"/no-such-opendash-command"},100,1024).ok(),"missing executable handled");
        System.out.println("PASS: normal output, pipe pressure, output cap, timeout, nonzero exit, missing command");
    }
    private static BoundedCommand.Result probe(String mode,long timeout,int limit) {
        return BoundedCommand.run(new String[]{System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),BoundedCommandTest.class.getName(),mode},timeout,limit);
    }
    private static void require(boolean value,String name) { if(!value) throw new AssertionError(name); }
}
