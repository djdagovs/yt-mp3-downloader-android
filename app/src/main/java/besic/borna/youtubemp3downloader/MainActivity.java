package besic.borna.youtubemp3downloader;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    String checkBaseUrl = "http://api.convert2mp3.cc/check.php?v=";

    TextView tv;
    Handler handler;
    ProgressDialog progressDialog;

    private final int STORAGE_PERMISSION_REQUEST = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.label);
        handler = new Handler(getMainLooper());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading MP3...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);

        if(!requestPermissions()){
            handleIntent();
        }
    }

    private void download(final String url){
        CheckTask checkTask = new CheckTask(new CheckCallback() {
            @Override
            public void done(String status) {
                String[] statusTokens = status.split("\\|");
                //for (String t : statusTokens) Log.w("TOKEN", t);

                Runnable reschedule = new Runnable() {
                    @Override
                    public void run() {
                        download(url);
                    }
                };

                switch (statusTokens[0]){
                    case "OK":
                        String server = statusTokens[1];
                        String id = statusTokens[2];
                        String name = statusTokens[3];
                        String downloadUrl = "http://dl"+server+".downloader.space/dl.php?id="+id;

                        final DownloadTask downloadTask = new DownloadTask(MainActivity.this, MainActivity.this, name+".mp3", progressDialog);

                        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                downloadTask.cancel(true);
                            }
                        });

                        downloadTask.execute(downloadUrl);
                        break;
                    case "ERROR":
                        if(statusTokens[1].compareTo("PENDING")==0){
                            MainActivity.this.handler.postDelayed(reschedule, 2000);
                            tv.setText(R.string.converting);
                        }
                        else MainActivity.this.finish();
                        break;
                    case "DOWNLOAD":
                        MainActivity.this.handler.postDelayed(reschedule, 2000);
                        tv.setText(R.string.converting);
                        break;
                }
            }
        });
        checkTask.execute(url);
    }

    void handleIntent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String youtubeUrlString = intent.getStringExtra(Intent.EXTRA_TEXT);
                String videoId = extractIdFromUrl(youtubeUrlString);
                if (youtubeUrlString != null) {
                    download(checkBaseUrl+videoId);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // permission granted

                    handleIntent();

                } else { // permission denied
                    this.finish();
                }
                return;
            }
        }
    }

    private boolean requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
            return true;

        }
        return false;
    }

    private String extractIdFromUrl(String urlString){

        Pattern pattern1 = Pattern.compile("youtube\\.com/watch\\?v\\=(.{11})");
        Pattern pattern2 = Pattern.compile("youtu\\.be/(.{11})");

        Matcher matcher1 = pattern1.matcher(urlString);
        Matcher matcher2 = pattern2.matcher(urlString);

        if(matcher1.find()){
            return matcher1.group(1);
        }
        else if(matcher2.find()){
            return matcher2.group(1);
        }

        return null;
    }
}
