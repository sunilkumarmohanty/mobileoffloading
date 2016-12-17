package fi.aalto.mobileoffloading;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.test.espresso.core.deps.guava.collect.Lists;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.aalto.mobileoffloading.adapters.OcrHistoryAdapter;
import fi.aalto.mobileoffloading.api.RemoteOcrClient;
import fi.aalto.mobileoffloading.api.RemoteOcrService;
import fi.aalto.mobileoffloading.models.OcrHistoryEntry;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity implements TessBaseAPI.ProgressNotifier {
    RadioGroup radioGroupMode;
    RadioButton rbLocal, rbRemote, rbBenchmark;

    private ListView historyListView;
    private TessBaseAPI baseAPI;
    private Uri imageUri;
    String datapath = "";
    private static final int REQUEST_GALLERY = 0;
    private static final int REQUEST_CAMERA = 1;
    private static final String TAG = "MAIN";
    ProgressBar progressBar;
    RadioGroup rgMode;
    ArrayList<Uri> mArrayUri = new ArrayList<>();
    ProgressDialog mProgressDialog;
    private final Map<String, byte[]> thumbnails = new HashMap<>();

    private boolean retake = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        baseAPI = new TessBaseAPI(this);
        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        baseAPI = new TessBaseAPI();
        Log.d("Datapath :", datapath);
        checkFile(new File(datapath + "tessdata/"));
        baseAPI.init(datapath, language);
        rgMode = (RadioGroup) findViewById(R.id.radiogroup_mode);

        radioGroupMode = (RadioGroup) findViewById(R.id.radiogroup_mode);
        rbLocal = (RadioButton) findViewById(R.id.radio_local);
        rbRemote = (RadioButton) findViewById(R.id.radio_remote);
        rbBenchmark = (RadioButton) findViewById(R.id.radio_benchmark);
        historyListView = (ListView) findViewById(R.id.history_list);

        if (!isNetworkAvailable()) {
            rbLocal.setChecked(true);
            rbBenchmark.setEnabled(false);
            rbRemote.setEnabled(false);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            retake = extras.getBoolean("RETAKE");
            OperatingMode operatingMode = OperatingMode.valueOf(extras.getString("OPERATINGMODE"));

            switch (operatingMode) {
                case LOCAL:
                    rbLocal.setChecked(true);
                    break;
                case REMOTE:
                    rbRemote.setChecked(true);
                    break;
            }

        }

        // Preparing and opening Intent for Camera
        findViewById(R.id.take_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = "OCR.jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });

        // Preparing and opening Intent for Gallery
        findViewById(R.id.select_existing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
            }
        });
    }

    private void loadHistory() {
        final Activity activity = this;

        if(isNetworkAvailable()) {

            final RemoteOcrService remoteOcrService = new RemoteOcrClient(getApplicationContext()).getRemoteOcrService();
            final Call<List<OcrHistoryEntry>> historyCall = remoteOcrService.history();
            historyCall.enqueue(new Callback<List<OcrHistoryEntry>>() {
                @Override
                public void onResponse(Call<List<OcrHistoryEntry>> call, Response<List<OcrHistoryEntry>> response) {
                    final List<OcrHistoryEntry> historyEntries = response.body();

                    if (historyEntries.size() == 0) {
                        hideDialog();
                    }

                    Gson gson = new Gson();
                    SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("history", gson.toJson(historyEntries));
                    editor.commit();


                    for (final OcrHistoryEntry historyEntry : historyEntries) {

                        Call<ResponseBody> thumbnailCall = remoteOcrService.downloadImage(
                                getApplicationContext().getResources().getString(R.string.server_url)
                                        + "/api/" + historyEntry.getThumbnails().get(0));

                        thumbnailCall.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    thumbnails.put(historyEntry.getThumbnails().get(0), response.body().bytes());

                                    SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    String thumbnailAsString =
                                            Base64.encodeToString(thumbnails.get(historyEntry.getThumbnails().get(0)), Base64.DEFAULT);
                                    editor.putString(historyEntry.getThumbnails().get(0), thumbnailAsString);
                                    editor.commit();

                                    if (thumbnails.size() == historyEntries.size()) {
                                        List<OcrHistoryEntry> historyEntriesReverse = Lists.reverse(historyEntries);
                                        OcrHistoryAdapter ocrHistoryAdapter = new OcrHistoryAdapter(activity, R.layout.history_item_view, historyEntriesReverse, thumbnails);
                                        historyListView.setAdapter(ocrHistoryAdapter);
                                        hideDialog();
                                    }
                                } catch (IOException e) {
                                    Log.e("IMAGE LOAD", e.getMessage(), e);
                                    Toast.makeText(getApplicationContext(), "Can't download thumbnail", Toast.LENGTH_SHORT).show();
                                    hideDialog();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("IMAGE LOAD", t.getMessage(), t);
                                Toast.makeText(getApplicationContext(), "Can't download history", Toast.LENGTH_SHORT).show();
                                hideDialog();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<List<OcrHistoryEntry>> call, Throwable t) {
                    Log.e("Error", t.toString());
                    hideDialog();
                }
            });
        } else {
            SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
            String history = sharedPref.getString("history", "[]");
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<OcrHistoryEntry>>(){}.getType();
            List<OcrHistoryEntry> historyEntries = gson.fromJson(history, listType);

            for (final OcrHistoryEntry historyEntry : historyEntries) {
                String thumbnailAsString = sharedPref.getString(historyEntry.getThumbnails().get(0), null);
                byte[] array = Base64.decode(thumbnailAsString, Base64.DEFAULT);
                thumbnails.put(historyEntry.getThumbnails().get(0), array);
            }

            List<OcrHistoryEntry> historyEntriesReverse = Lists.reverse(historyEntries);
            OcrHistoryAdapter ocrHistoryAdapter = new OcrHistoryAdapter(activity, R.layout.history_item_view, historyEntries, thumbnails);
            historyListView.setAdapter(ocrHistoryAdapter);
            hideDialog();
        }

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        if(!retake) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Update history");
            showDialog();
            loadHistory();
        } else {
            String filename = "OCR.jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent();
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private void inspectFromBitmap(Bitmap bitmap) {
        //progressBar.setVisibility(View.VISIBLE);
        Bitmap thumbnail = bitmap;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        baseAPI.setImage(bitmap);
        Log.d(TAG, "inside inspectFromBitmap");

        //new MyWorker(this,null,bitmap,baseAPI).execute();
        String text = baseAPI.getUTF8Text();

        Log.d(TAG, text);
        //Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        bitmap.recycle();
        //progressBar.setVisibility(View.GONE);
    }

    private void inspect(Uri uri) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 2;
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            inspectFromBitmap(bitmap);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public OperatingMode getOperatingMode() {
        int selectedId = radioGroupMode.getCheckedRadioButtonId();

        if (selectedId == rbLocal.getId()) {
            return OperatingMode.LOCAL;
        } else if (selectedId == rbRemote.getId()) {
            return OperatingMode.REMOTE;
        } else {
            return OperatingMode.BENCHMARK;
        }
    }

    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
        Log.d(TAG, progressValues.getPercent() + "");
        progressBar.setProgress(progressValues.getPercent());
    }

    public void showDialog() {

        if (mProgressDialog != null && !mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    public void hideDialog() {

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ResultCode : ", "" + resultCode);
        Log.d("RequestCode : ", "" + requestCode);
        retake = false;
        mArrayUri = new ArrayList<Uri>();
        switch (requestCode) {
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    //inspect(data.getData());
                    if (data.getData() != null) {
                        Uri mImageUri = data.getData();
                        mArrayUri.add(mImageUri);
                    } else {
                        if (data.getClipData() != null) {
                            ClipData mClipData = data.getClipData();
                            for (int i = 0; i < mClipData.getItemCount(); i++) {
                                ClipData.Item item = mClipData.getItemAt(i);
                                Uri uri = item.getUri();
                                mArrayUri.add(uri);
                                Log.d("MULTIPLE", uri.toString());
                            }
                        }
                    }
                    new OCRTask(this, getOperatingMode(), SourceMode.GALLERY).execute(mArrayUri);
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (imageUri != null) {
                        mArrayUri.add(imageUri);
                    }
                    new OCRTask(this, getOperatingMode(), SourceMode.CAMERA).execute(mArrayUri);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }

    private void checkFile(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                LoginManager.getInstance().logOut();
                SharedPreferences settings;
                SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                sharedPref.putString("TOKEN", "").apply();
                sharedPref.commit();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(intent.getFlags());
                startActivity(intent);
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
