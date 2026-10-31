package org.opendash.diagnostics;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;

public final class MainActivity extends Activity {
    private static final int CREATE_JSON=101, CHOOSE_FOLDER=102;
    private static final java.util.concurrent.atomic.AtomicBoolean BUSY=new java.util.concurrent.atomic.AtomicBoolean();
    private JSONObject report;
    private boolean exporting;
    private TextView details;
    private Button scan,rootTest,export,usb,local;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(12));
        TextView title=new TextView(this);title.setText("OpenDash Diagnostics 0.4");title.setTextSize(22);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        TextView intro=new TextView(this);intro.setText("Read-only hardware inventory. Driver presence does not prove function. The last report is kept privately in this app. Review identifiers before sharing.");root.addView(intro);
        scan=button("Scan capabilities & drivers");root.addView(scan);
        rootTest=button("Scan + optional root identity reads");root.addView(rootTest);
        export=button("Export JSON…");root.addView(export);
        usb=button("Save JSON to USB / folder…");root.addView(usb);
        local=button("Save local copy for file manager");root.addView(local);
        details=new TextView(this);details.setTextIsSelectable(true);details.setTextSize(13);details.setTypeface(Typeface.MONOSPACE);root.addView(details);
        ScrollView page=new ScrollView(this);page.addView(root);setContentView(page);
        setEnabled(false);details.setText("Loading last report…");
        scan.setOnClickListener(v->collect(false));rootTest.setOnClickListener(v->collect(true));
        export.setOnClickListener(v->prepareExport(false));usb.setOnClickListener(v->prepareExport(true));local.setOnClickListener(v->saveLocal());
        new Thread(()->{
            JSONObject old=null;
            try {old=new JSONObject(ReportFiles.load(this,"last-report.json"));} catch(Exception ignored) { }
            final JSONObject restored=old;
            runOnUiThread(()->{if(isDestroyed())return;report=restored;showReport(restored,"No saved report. Tap Scan capabilities & drivers.");if(!exporting)setEnabled(true);});
        },"opendash-restore").start();
    }
    private void setEnabled(boolean enabled) {
        scan.setEnabled(enabled);rootTest.setEnabled(enabled);
        export.setEnabled(enabled && report!=null);usb.setEnabled(enabled && report!=null);local.setEnabled(enabled && report!=null);
    }
    private void showReport(JSONObject value,String empty) {
        if(value==null){details.setText(empty);return;}
        String text;try{text=value.toString(2);}catch(Exception e){text=value.toString();}
        if(text.length()>65536)text=text.substring(0,65536)+"\n[Preview limited; export contains the full collected report.]";
        JSONObject identity=value.optJSONObject("hardwareIdentity");
        if(identity!=null)try{text="CPU / BOARD CROSS-CHECKS\n"+identity.optJSONObject("crossChecks").toString(2)+"\n\nCOLLECTED REPORT\n"+text;}catch(Exception ignored){}
        if(text.length()>65536)text=text.substring(0,65536)+"\n[Preview limited; export the full report.]";
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
    private void prepareExport(boolean folder) {
        if(report==null)return; final JSONObject selected=report;setEnabled(false);
        new Thread(()->{
            String error=null;
            try {ReportFiles.save(this,"pending-export.json",selected.toString());}catch(Exception e){error=e.getClass().getSimpleName();}
            final String failure=error;
            runOnUiThread(()->{
                if(isDestroyed())return;setEnabled(true);
                if(failure!=null){Toast.makeText(this,"Cannot prepare export: "+failure,Toast.LENGTH_LONG).show();return;}
                Intent intent;
                if(folder)intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                else intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,fileName(selected));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {startActivityForResult(intent,folder?CHOOSE_FOLDER:CREATE_JSON);}
                catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"No compatible picker. Save a local copy and copy it to USB with your file manager.",Toast.LENGTH_LONG).show();}
            });
        },"opendash-prepare-export").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if((request!=CREATE_JSON && request!=CHOOSE_FOLDER) || result!=RESULT_OK || data==null || data.getData()==null)return;
        final Uri selectedUri=data.getData();final boolean folder=request==CHOOSE_FOLDER;exporting=true;setEnabled(false);
        new Thread(()->{
            String message;
            try {
                String snapshot=ReportFiles.load(this,"pending-export.json");
                byte[] bytes=snapshot.getBytes("UTF-8");
                Uri uri=selectedUri;
                if(folder) {
                    Uri parent=DocumentsContract.buildDocumentUriUsingTree(selectedUri,DocumentsContract.getTreeDocumentId(selectedUri));
                    uri=DocumentsContract.createDocument(getContentResolver(),parent,"application/json",fileName(new JSONObject(snapshot)));
                    if(uri==null)throw new IOException("Folder provider did not create the file");
                }
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
