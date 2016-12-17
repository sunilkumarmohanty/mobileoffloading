package fi.aalto.mobileoffloading;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.facebook.login.LoginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import fi.aalto.mobileoffloading.api.RemoteOcrClient;
import fi.aalto.mobileoffloading.api.RemoteOcrService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/***
 * Shows the OCR result
 *
 */
public class OcrResultActivity extends AppCompatActivity {

    private SourceMode sourceMode;
    private OperatingMode operatingMode;
    ArrayList<Uri> mArrayUri;
    String ocrText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sourceMode = SourceMode.valueOf(extras.getString("SOURCEMODE"));
            operatingMode = OperatingMode.valueOf(extras.getString("OPERATINGMODE"));
            ocrText = extras.getString("OCRText");
            mArrayUri = (ArrayList<Uri>) extras.get("IMAGELIST");
            getSupportActionBar().setTitle(extras.getString("CREATIONTIME"));
            BuildImageGallery();
        }
        TextView tvOCRText = (TextView) findViewById(R.id.result_content);
        tvOCRText.setText(ocrText);
        tvOCRText.setMovementMethod(new ScrollingMovementMethod());
    }

    /***
     * Exports the file to filesystem
     */
    private void ExportFile() {
        String filename = "ocrtext.txt";
        if (isExternalStorageWritable()) {
            String extStorageDirectory
                    = Environment.getExternalStorageDirectory().toString();
            File file = new File(extStorageDirectory, "ocrtxt.txt");
            OutputStream outStream = null;
            try {
                outStream = new FileOutputStream(file);
                outStream.write(ocrText.getBytes());
                outStream.close();
                Log.d("Save", file.getAbsolutePath() + "##" + file.getAbsolutePath());
                Toast.makeText(OcrResultActivity.this, "File exported to " + filename, Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Toast.makeText(OcrResultActivity.this, "Error saving file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(OcrResultActivity.this, "Application does not have permission to save file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /***
     * Builds the image gallery using - daimajia.slider:library
     */
    private void BuildImageGallery() {
        final SliderLayout sliderShow = (SliderLayout) findViewById(R.id.slider);
        try {
            if(sourceMode != SourceMode.HISTORY) {
                for (final Uri uri : mArrayUri) {
                    TextSliderView textSliderView = new TextSliderView(this);
                    File file = new File(getPath(getApplicationContext(), uri));
                    Log.d("PATH", uri.toString() + "##" + uri.getPath());
                    textSliderView.image(file);
                    sliderShow.addSlider(textSliderView);

                }
            } else {
                if(isNetworkAvailable()) {
                    final RemoteOcrService remoteOcrService = new RemoteOcrClient(getApplicationContext()).getRemoteOcrService();
                    final Context context = this;

                    for (final Uri uri : mArrayUri) {

                        Call<ResponseBody> thumbnailCall = remoteOcrService.downloadImage(uri.toString());

                        thumbnailCall.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    TextSliderView textSliderView = new TextSliderView(context);

                                    File outputDir = context.getCacheDir();
                                    File tempFile = File.createTempFile("prefix", "extension", outputDir);

                                    FileOutputStream fos = new FileOutputStream(tempFile);
                                    fos.write(response.body().bytes());
                                    textSliderView.image(tempFile);
                                    sliderShow.addSlider(textSliderView);
                                } catch (Exception e) {
                                    Log.e("IMAGE LOAD", e.getMessage(), e);
                                    Toast.makeText(getApplicationContext(), "Can't download picture", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("IMAGE LOAD", t.getMessage(), t);
                                Toast.makeText(getApplicationContext(), "Can't download history", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        } catch (Exception ex) {
            Log.d("Error", ex.getMessage());
        }
    }

    /***
     * Gets absolute path of the file
     * @param context
     * @param uri
     * @return
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    // Gets the real path in Android
    public String getRealPathFromURI(Context context, Uri contentUri) {
        String filePath = "";
        Cursor cursor;
        try {

            String wholeID = DocumentsContract.getDocumentId(contentUri);
            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];
            String[] column = {MediaStore.Images.Media.DATA};
            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";
            cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{id}, null);
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
            Log.d("GETPATH", filePath);
            return filePath;
        } catch (Exception ex) {
            Log.d("Error", ex.getMessage());
        }
        try {

            cursor = getContentResolver().query(contentUri, null, null, null, null);
            if (cursor == null) { // Source is Dropbox or other similar local file path
                filePath = contentUri.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                filePath = cursor.getString(idx);
                cursor.close();
            }
            return filePath;
        } catch (Exception ex) {
            Log.d("Error", ex.getStackTrace().toString());
        }
        return "";
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ocrresult_menu, menu);
        if(sourceMode != SourceMode.CAMERA) {
            MenuItem retakeMenuItem = menu.findItem(R.id.action_retake);
            retakeMenuItem.setVisible(false);
        }
        return true;
    }

    /***
     * Creates the menu
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                LoginManager.getInstance().logOut();

                SharedPreferences settings;
                SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                sharedPref.putString("TOKEN", "").apply();
                sharedPref.commit();

                Intent intent = new Intent(OcrResultActivity.this, LoginActivity.class);
                intent.setFlags(intent.getFlags());
                startActivity(intent);
                finish();
                return true;
            case R.id.action_retake:
                Intent retakeIntent = new Intent(getApplicationContext(), MainActivity.class);
                retakeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                retakeIntent.putExtra("RETAKE", true);
                retakeIntent.putExtra("OPERATINGMODE", operatingMode.toString());
                startActivity(retakeIntent);
                return true;
            case R.id.action_export:
                ExportFile();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
