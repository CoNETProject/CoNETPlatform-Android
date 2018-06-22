package ca.conettech.conetplatformandroid;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.Intent;
import java.net.*;
import java.io.*;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    //We just want one instance of node running in the background.
    public static boolean _startedNodeAlready = false;
    public static int copyCpunt = 6000;
    public static int totalFile = 6400;
    public static int waiting = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!this.isTaskRoot() && getIntent() != null) {
            String action = getIntent().getAction();
            if ( getIntent().hasCategory( Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals( action )) {
                finish();
                return;
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView textViewVersions = (TextView) findViewById(R.id.textView112);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar4);
        progressBar.setProgress(0);
        //progressBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

        textViewVersions.setText( "Start up!"  );
        if( !_startedNodeAlready ) {
            _startedNodeAlready = true;
            copyCpunt = 0;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //The path where we expect the node project to be at runtime.
                    String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
                    if (wasAPKUpdated()) {
                        //Recursively delete any existing nodejs-project.
                        File nodeDirReference = new File(nodeDir);
                        if (nodeDirReference.exists()) {
                            deleteFolderRecursively(new File(nodeDir));
                        }
                        //Copy the node project from assets into the application's data path.
                        copyAssetFolder(progressBar, getApplicationContext().getAssets(), "nodejs-project", nodeDir);

                        //saveLastUpdateTime();
                    }
                    startNodeWithArguments(new String[]{"node",
                            nodeDir + "/main.js", nodeDir
                    });


                }
            }).start();
        }
        waitingOpenBrowser ( textViewVersions );




    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native Integer startNodeWithArguments(String[] arguments);



    private void waitingOpenBrowser (final TextView text) {
        waiting += 1;


        text.setText( "try time:[" + waiting + "], total copy [" + copyCpunt +"]");
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    new AsyncTask<Void,Void,String>() {
                        boolean openBrowser = true;
                        @Override
                        protected String doInBackground(Void... params) {
                            String nodeResponse="";

                            try {
                                URL localNodeServer = new URL("http://localhost:3000/");
                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(localNodeServer.openStream()));
                                String inputLine;
                                while ((inputLine = in.readLine()) != null)
                                    nodeResponse=nodeResponse+inputLine;
                                in.close();


                            } catch (Exception ex) {
                                openBrowser = false;
                            }

                            return nodeResponse;
                        }
                        @Override
                        protected void onPostExecute(String result) {
                            if (!openBrowser) {
                                waitingOpenBrowser ( text );
                            } else {
                                final Intent browserIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://localhost:3000/"));
                                startActivity(browserIntent);
                                finish();
                            }

                        }
                    }.execute();

                }
            },5000 );
    }


    private void checkNodeJSReady () {
        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String nodeResponse="";
                try {
                    URL localNodeServer = new URL("http://localhost:3000/");
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(localNodeServer.openStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        nodeResponse=nodeResponse+inputLine;
                    in.close();
                } catch (Exception ex) {
                    nodeResponse=ex.toString();
                }
                return nodeResponse;
            }
            @Override
            protected void onPostExecute(String result) {

            }
        }.execute();
    }

    private boolean wasAPKUpdated() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        long previousLastUpdateTime = prefs.getLong("NODEJS_MOBILE_APK_LastUpdateTime", 0);
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (lastUpdateTime != previousLastUpdateTime);
    }

    private void saveLastUpdateTime() {
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("NODEJS_MOBILE_APK_LastUpdateTime", lastUpdateTime);
        editor.commit();
    }

    private static boolean deleteFolderRecursively(File file) {
        try {
            boolean res=true;
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    res &= deleteFolderRecursively(childFile);
                } else {
                    res &= childFile.delete();
                }
            }
            res &= file.delete();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAssetFolder(final ProgressBar progressBar, AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            boolean res = true;

            if (files.length==0) {

                //If it's a file, it won't have any assets "inside" it.
                copyCpunt += 1;
                final int uu = Math.round(copyCpunt*100/totalFile);
                progressBar.setProgress(uu);
                res &= copyAsset(assetManager,
                        fromAssetPath,
                        toPath);
            } else {
                new File(toPath).mkdirs();
                for (String file : files)
                    res &= copyAssetFolder(progressBar, assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    final static Pattern pp = Pattern.compile(".(js$|json$)",Pattern.CASE_INSENSITIVE);

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

}
