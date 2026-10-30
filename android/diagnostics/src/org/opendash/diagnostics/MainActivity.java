package org.opendash.diagnostics;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;

public final class MainActivity extends Activity {
    private static final int CREATE_JSON=101;
    private static final java.util.concurrent.atomic.AtomicBoolean BUSY=new java.util.concurrent.atomic.AtomicBoolean();
    private JSONObject report;
    private boolean exporting;
    private TextView details;
    private Button scan,rootTest,export,local;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(12));
        TextView title=new TextView(this);title.setText("OpenDash Diagnostics 0.3");title.setTextSize(22);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        TextView intro=new TextView(this);intro.setText("Read-only hardware inventory. Driver presence does not prove function. The last report is kept privately in this app. Review identifiers before sharing.");root.addView(intro);
        scan=button("Scan capabilities & drivers");root.addView(scan);
        rootTest=button("Scan + test root (su -c id)");root.addView(rootTest);
        export=button("Export JSON…");root.addView(export);
        local=button("Save local JSON copy (file-picker fallback)");root.addView(local);
        details=new TextView(this);details.setTextIsSelectable(true);details.setTextSize(13);details.setTypeface(Typeface.MONOSPACE);root.addView(details);
        ScrollView page=new ScrollView(this);page.addView(root);setContentView(page);
        setEnabled(false);details.setText("Loading last report…");
        scan.setOnClickListener(v->collect(false));rootTest.setOnClickListener(v->collect(true));
        export.setOnClickListener(v->prepareExport());local.setOnClickListener(v->saveLocal());
        new Thread(()->{
            JSONObject old=null;
            try {old=new JSONObject(ReportFiles.load(this,"last-report.json"));} catch(Exception ignored) { }
            final JSONObject restored=old;
            runOnUiThread(()->{if(isDestroyed())return;report=restored;showReport(restored,"No saved report. Tap Scan capabilities & drivers.");if(!exporting)setEnabled(true);});
        },"opendash-restore").start();
    }
    private void setEnabled(boolean enabled) {
        scan.setEnabled(enabled);rootTest.setEnabled(enabled);
        export.setEnabled(enabled && report!=null);local.setEnabled(enabled && report!=null);
    }
    private void showReport(JSONObject value,String empty) {
        if(value==null){details.setText(empty);return;}
        String text;try{text=value.toString(2);}catch(Exception e){text=value.toString();}
        if(text.length()>65536)text=text.substring(0,65536)+"\n[Preview limited; export contains the full collected report.]";
        details.setText(text);
    }
    private void collect(boolean root) {
        if(!BUSY.compareAndSet(false,true)){Toast.makeText(this,"A scan is already running",Toast.LENGTH_SHORT).show();return;}
        setEnabled(false);details.setText("Scanning… Vehicle functions are not activated.");
        new Thread(()->{
            JSONObject result=null;String note="";
            try {result=Diagnostics.collect(this,root);ReportFiles.save(this,"last-report.json",result.toString());}
            catch(Exception e){note="Collection or private report save failed: "+e.getClass().getSimpleName();}
            finally {BUSY.set(false);}
            final JSONObject done=result;final String message=note;
            runOnUiThread(()->{if(isDestroyed())return;if(done!=null)report=done;showReport(report,message);setEnabled(true);if(!message.isEmpty())Toast.makeText(this,message,Toast.LENGTH_LONG).show();});
        },"opendash-scan").start();
    }
    private String fileName(JSONObject value) {
        return "opendash-"+value.optString("reportId","report")+".json";
    }
    private void prepareExport() {
        if(report==null)return; final JSONObject selected=report;setEnabled(false);
        new Thread(()->{
            String error=null;
            try {ReportFiles.save(this,"pending-export.json",selected.toString());}catch(Exception e){error=e.getClass().getSimpleName();}
            final String failure=error;
            runOnUiThread(()->{
                if(isDestroyed())return;setEnabled(true);
                if(failure!=null){Toast.makeText(this,"Cannot prepare export: "+failure,Toast.LENGTH_LONG).show();return;}
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE,fileName(selected));
                try {startActivityForResult(intent,CREATE_JSON);}
                catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"No system file picker. Use Save local JSON copy.",Toast.LENGTH_LONG).show();}
            });
        },"opendash-prepare-export").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=CREATE_JSON || result!=RESULT_OK || data==null || data.getData()==null)return;
        final Uri uri=data.getData();exporting=true;setEnabled(false);
        new Thread(()->{
            String message;
            try {
                byte[] bytes=ReportFiles.load(this,"pending-export.json").getBytes("UTF-8");
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")) {
                    if(out==null)throw new IOException("No stream");out.write(bytes);out.flush();
                }
                String verification;
                try(InputStream in=getContentResolver().openInputStream(uri)) {
                    verification=in!=null && ReportIntegrity.matches(in,bytes)?"Read-back verified: exact byte match.":"WARNING: read-back differs from the report. Retry export.";
                }catch(Exception e){verification="Write completed; read-back unavailable: "+e.getClass().getSimpleName();}
                message=verification+"\n"+bytes.length+" bytes; SHA-256: "+ReportFiles.digest(bytes);
            }catch(Exception e){message="Export failed: "+e.getClass().getSimpleName()+". Use Save local JSON copy if needed.";}
            finishExport(message);
        },"opendash-export").start();
    }
    private void saveLocal() {
        if(report==null)return;final JSONObject selected=report;setEnabled(false);
        new Thread(()->{
            String message;
            try {
                File directory=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if(directory==null)throw new IOException("External app storage unavailable");
                File target=new File(directory,fileName(selected));byte[] bytes=selected.toString().getBytes("UTF-8");
                try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();}
                try(InputStream in=new FileInputStream(target)){if(!ReportIntegrity.matches(in,bytes))throw new IOException("Read-back mismatch");}
                message="Verified local copy: "+target.getAbsolutePath()+"\n"+bytes.length+" bytes; SHA-256: "+ReportFiles.digest(bytes)+"\nCopy with a file manager before uninstalling the app.";
            }catch(Exception e){message="Local copy failed: "+e.getClass().getSimpleName();}
            finishExport(message);
        },"opendash-local-export").start();
    }
    private void finishExport(String message) {
        runOnUiThread(()->{if(isDestroyed())return;exporting=false;setEnabled(true);details.setText(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();});
    }
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
