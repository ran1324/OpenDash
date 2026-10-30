package org.opendash.diagnostics;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.OutputStream;

public final class MainActivity extends Activity {
    private static final int CREATE_JSON = 101;
    private JSONObject report;
    private TextView details;
    private Button export, scan, rootTest;
    private static final java.util.concurrent.atomic.AtomicBoolean BUSY = new java.util.concurrent.atomic.AtomicBoolean();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(18));
        root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title = new TextView(this);
        title.setText("OpenDash Diagnostics"); title.setTextSize(24); title.setTypeface(null,Typeface.BOLD);
        title.setTextColor(Color.rgb(25,35,45)); root.addView(title);
        TextView intro = new TextView(this);
        intro.setText("Read-only device report for NAM5210-A9 research. Results are observations, not a firmware compatibility verdict. Review the JSON before sharing; it can contain device identifiers and package names.");
        intro.setTextSize(15); intro.setPadding(0,dp(8),0,dp(12)); root.addView(intro);
        scan = button("Scan capabilities & drivers"); root.addView(scan);
        rootTest = button("Test root (su -c id)"); root.addView(rootTest);
        export = button("Export JSON…"); export.setEnabled(false); root.addView(export);
        ScrollView scroll = new ScrollView(this); details = new TextView(this);
        details.setText("Tap Collect diagnostics. No root request is made during collection.");
        details.setTextIsSelectable(true); details.setTextSize(13); details.setTypeface(Typeface.MONOSPACE);
        details.setPadding(dp(8),dp(12),dp(8),dp(12)); scroll.addView(details);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        scan.setOnClickListener(v -> collect(false));
        rootTest.setOnClickListener(v -> collect(true));
        export.setOnClickListener(v -> {
            if (report == null) return;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE,"opendash-diagnostics.json");
            try { startActivityForResult(intent,CREATE_JSON); }
            catch (android.content.ActivityNotFoundException e) { Toast.makeText(this,"System file picker is unavailable on this unit",Toast.LENGTH_LONG).show(); }
        });
    }
    private void collect(boolean testRoot) {
        if (!BUSY.compareAndSet(false,true)) return;
        scan.setEnabled(false); rootTest.setEnabled(false); export.setEnabled(false);
        details.setText("Scanning capabilities, drivers and services… This does not test vehicle functions.");
        new Thread(() -> {
            JSONObject result = null; String failure = null;
            try { result = Diagnostics.collect(this,testRoot); }
            catch (Exception e) { failure = e.getClass().getSimpleName(); }
            finally { BUSY.set(false); }
            final JSONObject completed=result; final String error=failure;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                scan.setEnabled(true); rootTest.setEnabled(true);
                if(completed==null) { details.setText("Collection failed: "+error); return; }
                report = completed; export.setEnabled(true);
                String text;
                try { text=completed.toString(2); } catch(Exception e) { text=completed.toString(); }
                if(text.length()>65536) text=text.substring(0,65536)+"\n[Screen preview limited; export JSON for the full report.]";
                details.setText(text);
            });
        },"opendash-scan").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request != CREATE_JSON || result != RESULT_OK || data == null || data.getData() == null || report == null) return;
        Uri uri = data.getData();
        final String json=report.toString();
        export.setEnabled(false);
        new Thread(() -> {
            String message;
            try (OutputStream out = getContentResolver().openOutputStream(uri,"w")) {
                if (out == null) throw new IllegalStateException("No output stream");
                out.write(json.getBytes("UTF-8")); out.flush(); message="Report saved";
            } catch (Exception e) { message="Export failed: " + e.getClass().getSimpleName(); }
            final String status=message;
            runOnUiThread(() -> { if(!isDestroyed()) {export.setEnabled(!BUSY.get()); Toast.makeText(this,status,Toast.LENGTH_LONG).show();} });
        },"opendash-export").start();
    }

    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); return b; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
