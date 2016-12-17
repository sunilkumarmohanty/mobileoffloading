
package fi.aalto.mobileoffloading;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.aalto.mobileoffloading.api.RemoteOcrClient;
import fi.aalto.mobileoffloading.api.RemoteOcrService;
import fi.aalto.mobileoffloading.models.BenchmarkResult;
import fi.aalto.mobileoffloading.models.BenchmarkResultContainer;
import fi.aalto.mobileoffloading.models.OcrEntry;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Sunil on 26-11-2016.
 * This is to be called only in case of local processing
 * This is an async task
 */


public class OCRTask extends AsyncTask<ArrayList<Uri>, String, String> implements TessBaseAPI.ProgressNotifier {
    private final String TAG = "OCRTask";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private Context context;
    private ProgressDialog dialog;
    private String datapath = "";
    private TessBaseAPI baseAPI;
    private ArrayList<Uri> mArrayUri;
    private Date creationDate;
    private OperatingMode operatingMode = OperatingMode.LOCAL;
    private SourceMode sourceMode;
    private String stage = "";

    private List<BenchmarkResult> localBenchmarks;
    private List<BenchmarkResult> remoteBenchmarks;

    public OCRTask(Context ctx, OperatingMode operatingMode, SourceMode sourceMode) {
        this.context = ctx;
        this.operatingMode = operatingMode;
        this.sourceMode = sourceMode;
    }


    /**
     * Sets up the Dialog Box
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        stage = "Processing Images...";
        dialog.setMessage("Processing Images...");
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * Creates the Result Activity Intent and passes the necessary extra
     * @param result : OCR Text
     */
    @Override
    protected void onPostExecute(String result) {

        if(result == null) {
            Toast.makeText(context, "Can't run OCR due to an error", Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
            dialog.dismiss();
            return;
        }

        Log.d(TAG, result);
        super.onPostExecute(result);
        dialog.dismiss();

        switch (operatingMode) {
            case LOCAL:
            case REMOTE:
                Intent resultActivity = new Intent(this.context, OcrResultActivity.class);
                resultActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                resultActivity.putExtra("IMAGELIST", mArrayUri);
                resultActivity.putExtra("OCRText", result);
                resultActivity.putExtra("SOURCEMODE", this.sourceMode.toString());
                resultActivity.putExtra("OPERATINGMODE", this.operatingMode.toString());
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                resultActivity.putExtra("CREATIONTIME", df.format(creationDate));
                this.context.startActivity(resultActivity);
                break;
            case BENCHMARK:
                Gson gson = new Gson();
                Intent benchmarkActivity = new Intent(this.context, BenchmarkSummary.class);
                benchmarkActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                benchmarkActivity.putExtra("benchmarkResults",
                        gson.toJson(new BenchmarkResultContainer(localBenchmarks, remoteBenchmarks)));
                this.context.startActivity(benchmarkActivity);
                break;
        }
    }

    /**
     * Does the generation of OCR Text using TessBaseAPI
     * @return : OCR Text
     */
    private String processLocal() {
        Log.i(TAG, "OCR: starting");

        localBenchmarks = new ArrayList<>();

        String language = "eng";
        datapath = this.context.getFilesDir() + "/tesseract/";
        baseAPI = new TessBaseAPI(this);
        Log.d("Datapath :", datapath);
        checkFile(new File(datapath + "tessdata/"));
        baseAPI.init(datapath, language);
        // Use our camera provided data
        StringBuilder ocrText = new StringBuilder();
        int imgindx = 1;
        // Reads each image from the array and generates OCR and saves it in a string builder
        for (Uri uri : mArrayUri) {
            long startMillis = System.currentTimeMillis();
            stage = "Processing image : " + imgindx + "/" + mArrayUri.size();
            publishProgress(stage);
            String text = inspect(uri);
            ocrText.append(text);
            ocrText.append("\n");
            imgindx++;
            long endMillis = System.currentTimeMillis();
            long deltaMillis = endMillis - startMillis;
            localBenchmarks.add(new BenchmarkResult(text, deltaMillis));
        }

        creationDate = new Date();
        return ocrText.toString();
    }

    /**
     * Does the remote OCR
     * @return OCR Text
     */
    private String processRemote() {
        RemoteOcrService remoteOcrService = new RemoteOcrClient(this.context).getRemoteOcrService();
        remoteBenchmarks = new ArrayList<>();
        List<File> images = new ArrayList<>();
        List<MultipartBody.Part> files = new ArrayList<>();
        for (Uri uri : mArrayUri) {
            File file = FileUtils.getFile(context, uri);
            images.add(file);
            RequestBody requestFile =
                    RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), file);
            files.add(MultipartBody.Part.createFormData("images", file.getName(), requestFile));
        }

        Call<List<OcrEntry>> scanCall = remoteOcrService.scan(files);
        String resultText = "";
        try {
            Response<List<OcrEntry>> remoteOcrResult = scanCall.execute();
            List<OcrEntry> ocrEntries = remoteOcrResult.body();
            for (int i = 0; i < ocrEntries.size(); i++) {
                resultText += ocrEntries.get(i).getText();
                resultText += " ";
                remoteBenchmarks.add(new BenchmarkResult(ocrEntries.get(i).getText(),
                        ocrEntries.get(i).getProcessingTime(), images.get(i).length()));
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            creationDate = df.parse(ocrEntries.get(ocrEntries.size() - 1).getCreatedAt());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return resultText;
    }

    /**
     * OCR Text Processing
     * @param params Image array
     * @return OCR Text
     */
    protected String doInBackground(ArrayList<Uri>... params) {
        mArrayUri = params[0];
        String resultText = "";
        switch (this.operatingMode) {
            case LOCAL:
                resultText = processLocal();
                break;
            case REMOTE:
                resultText = processRemote();
                break;
            case BENCHMARK:
                processLocal();
                processRemote();
                break;
        }
        return resultText;
    }

    /**
     * Updates the Dialog Box with the progress status
     * @param progress message
     */
    @Override
    protected void onProgressUpdate(String... progress) {

        super.onProgressUpdate(progress);
        try {
            dialog.setMessage(progress[0].toString());
        } catch (Exception ex) {
        }
    }

    /**
     * Checks if the necessary files are present for Tesseract
     * @param dir : Directory
     */
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

    /**
     * Copies Tesseract File to required location if not present
     */
    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = this.context.getAssets();

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

    private void inspectFromBitmap(Bitmap bitmap) {
        //progressBar.setVisibility(View.VISIBLE);
        //progressBar.setVisibility(View.GONE);

    }

    /**
     * Does the OCR Conversion using TessBaseAPI
     * @param uri - the path of the image
     * @return OCR Text
     */
    private String inspect(Uri uri) {
        InputStream is = null;
        String text = "";
        try {
            is = this.context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 2;
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            baseAPI.setImage(bitmap);
            text = baseAPI.getUTF8Text();
            Log.d(TAG, text);
            bitmap.recycle();

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
        return text;
    }

    /**
     * Callback which gives % completion of OCR conversion
     * @param progressValues Progress status
     */
    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
        Log.d("Progress", progressValues.getPercent() + "");
        publishProgress(stage + " - " + progressValues.getPercent() + "%");
    }
}