package com.example.minimalbrowser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class DownloadsActivity extends AppCompatActivity {

    private DownloadManager dm;
    private LinearLayout container;
    private HashMap<Long, ProgressBar> progressBars = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        setContentView(container);

        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        loadDownloads();
    }

    private void loadDownloads() {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor c = dm.query(query);

        if (c != null) {
            while (c.moveToNext()) {
                @SuppressLint("Range") long id = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID));
                @SuppressLint("Range") String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                @SuppressLint("Range") int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                TextView tv = new TextView(this);
                tv.setText(title);
                container.addView(tv);

                ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                pb.setMax(100);
                container.addView(pb);
                progressBars.put(id, pb);

                updateProgress(id, pb, status);
            }
            c.close();
        }
    }

    private void updateProgress(long id, ProgressBar pb, int status) {
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            pb.setProgress(100);
            // Optionally set click listener to open file
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor c = dm.query(query);
            if (c.moveToFirst()) {
                @SuppressLint("Range") String uriStr = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Uri fileUri = Uri.parse(uriStr);
                pb.setOnClickListener(v -> startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .setData(fileUri)
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)));
            }
            c.close();
        } else if (status == DownloadManager.STATUS_RUNNING) {
            // Poll progress periodically
            new Thread(() -> {
                while (true) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(id);
                    Cursor c = dm.query(q);
                    if (c != null && c.moveToFirst()) {
                        @SuppressLint("Range") int bytesDownloaded = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        @SuppressLint("Range") int bytesTotal = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final int progress = (bytesTotal > 0) ? (int) (bytesDownloaded * 100L / bytesTotal) : 0;

                        runOnUiThread(() -> pb.setProgress(progress));

                        @SuppressLint("Range") int downloadStatus = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (downloadStatus != DownloadManager.STATUS_RUNNING) break;
                        c.close();
                    }
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }).start();
        }
    }
}
