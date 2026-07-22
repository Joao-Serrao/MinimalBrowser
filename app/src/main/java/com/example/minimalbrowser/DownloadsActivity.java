package com.example.minimalbrowser;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadsActivity extends AppCompatActivity {

    private static final long POLL_INTERVAL_MS = 500;

    private DownloadManager dm;
    private LinearLayout container;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Cleared in onDestroy so polling loops cannot outlive the activity. */
    private final AtomicBoolean active = new AtomicBoolean(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(container, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        loadDownloads();
    }

    private void loadDownloads() {
        int count = 0;

        // try-with-resources: the old code skipped close() on the break path
        // and whenever moveToFirst() returned false.
        try (Cursor c = dm.query(new DownloadManager.Query())) {
            if (c != null) {
                int idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
                int titleCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
                int statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);

                while (c.moveToNext()) {
                    addRow(c.getLong(idCol), c.getString(titleCol), c.getInt(statusCol));
                    count++;
                }
            }
        }

        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_downloads);
            empty.setGravity(Gravity.CENTER);
            container.addView(empty);
        }
    }

    private void addRow(long id, String title, int status) {
        TextView tv = new TextView(this);
        tv.setText(title == null ? "" : title);
        container.addView(tv);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        container.addView(pb);

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            pb.setProgress(100);
            bindOpenAction(tv, id);
        } else if (status == DownloadManager.STATUS_RUNNING
                || status == DownloadManager.STATUS_PENDING) {
            pollProgress(id, pb, tv);
        }
    }

    private void bindOpenAction(View row, long id) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor c = dm.query(query)) {
            if (c == null || !c.moveToFirst()) return;

            String uriStr = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            if (uriStr == null) return;

            Uri fileUri = Uri.parse(uriStr);
            // The row label is the click target; a ProgressBar is not one.
            row.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(fileUri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                } catch (Exception ignored) {
                    // No app can open this file type.
                }
            });
        }
    }

    /**
     * Polls on the main-thread Handler instead of a {@code while(true)} worker.
     * The old version span forever when the query returned nothing and held an
     * Activity reference well past destruction.
     */
    private void pollProgress(long id, ProgressBar pb, View row) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!active.get()) return;

                DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
                try (Cursor c = dm.query(query)) {
                    if (c == null || !c.moveToFirst()) return;

                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    long done = c.getLong(c.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long total = c.getLong(c.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    pb.setProgress(total > 0 ? (int) (done * 100L / total) : 0);

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        pb.setProgress(100);
                        bindOpenAction(row, id);
                        return;
                    }
                    if (status == DownloadManager.STATUS_FAILED) return;
                }
                handler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }, POLL_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        active.set(false);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
