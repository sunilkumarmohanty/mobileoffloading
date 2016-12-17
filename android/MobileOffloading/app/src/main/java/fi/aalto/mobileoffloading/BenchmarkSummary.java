package fi.aalto.mobileoffloading;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.List;

import fi.aalto.mobileoffloading.models.BenchmarkResult;
import fi.aalto.mobileoffloading.models.BenchmarkResultContainer;

public class BenchmarkSummary extends AppCompatActivity {

    private List<BenchmarkResult> localBenchmarks;
    private List<BenchmarkResult> remoteBenchmarks;

    TextView numberOfImages;

    TextView localProcessingTime;
    TextView remoteProcessingTime;

    TextView localProcessingTimeMin;
    TextView remoteProcessingTimeMin;

    TextView localProcessingTimeMax;
    TextView remoteProcessingTimeMax;

    TextView avgLocalProcessingTime;
    TextView avgRemoteProcessingTime;

    TextView localProcessingTimeDev;
    TextView remoteProcessingTimeDev;

    TextView dataExchanged;
    TextView dataExchangedAvg;
    TextView dataExchangedDev;
    TextView dataExchangedMin;
    TextView dataExchangedMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark_summary);

        Gson gson = new Gson();
        String brContainerStr = getIntent().getStringExtra("benchmarkResults");
        BenchmarkResultContainer brContainer = gson.fromJson(brContainerStr, BenchmarkResultContainer.class);

        localBenchmarks = brContainer.getLocalBenchmarks();
        remoteBenchmarks = brContainer.getRemoteBenchmarks();

        long localTotalTime = 0;
        long remoteTotalTime = 0;

        long imageDataExchanged = 0;

        long localTimeMin = localBenchmarks.get(0).getProcessingTime();
        int localTimeMinIndex = 0;
        long localTimeMax = localBenchmarks.get(0).getProcessingTime();
        int localTimeMaxIndex = 0;

        long remoteTimeMin = remoteBenchmarks.get(0).getProcessingTime();
        int remoteTimeMinIndex = 0;
        long remoteTimeMax = remoteBenchmarks.get(0).getProcessingTime();
        int remoteTimeMaxIndex = 0;

        numberOfImages = (TextView) findViewById(R.id.number_of_images);

        localProcessingTime = (TextView) findViewById(R.id.local_processing_time);
        remoteProcessingTime = (TextView) findViewById(R.id.remote_processing_time);

        localProcessingTimeMin = (TextView) findViewById(R.id.local_processing_time_min);
        localProcessingTimeMax = (TextView) findViewById(R.id.local_processing_time_max);

        remoteProcessingTimeMin = (TextView) findViewById(R.id.remote_processing_time_min);
        remoteProcessingTimeMax = (TextView) findViewById(R.id.remote_processing_time_max);

        avgLocalProcessingTime = (TextView) findViewById(R.id.local_avg_processing_time);
        avgRemoteProcessingTime = (TextView) findViewById(R.id.remote_avg_processing_time);

        localProcessingTimeDev = (TextView) findViewById(R.id.local_processing_time_dev);
        remoteProcessingTimeDev = (TextView) findViewById(R.id.remote_processing_time_dev);

        dataExchanged = (TextView) findViewById(R.id.exchanged_data);
        dataExchangedAvg = (TextView) findViewById(R.id.avg_exchanged_data);
        dataExchangedDev = (TextView) findViewById(R.id.dev_exchanged_data);
        dataExchangedMin = (TextView) findViewById(R.id.exchanged_data_min);
        dataExchangedMax = (TextView) findViewById(R.id.exchanged_data_max);

        for (int i = 0; i < localBenchmarks.size(); i++) {
            BenchmarkResult currentResult = localBenchmarks.get(i);

            localTotalTime += currentResult.getProcessingTime();
            if (currentResult.getProcessingTime() < localTimeMin) {
                localTimeMin = currentResult.getProcessingTime();
                localTimeMinIndex = i;
            }
            if (currentResult.getProcessingTime() > localTimeMax) {
                localTimeMax = currentResult.getProcessingTime();
                localTimeMaxIndex = i;
            }
        }

        long exchangedDataMin = remoteBenchmarks.get(0).getImageSize();
        int exchangedDataMinIndex = 0;
        long exchangedDataMax = localBenchmarks.get(0).getImageSize();
        int exchangedDataMaxIndex = 0;

        for (int i = 0; i < remoteBenchmarks.size(); i++) {
            BenchmarkResult currentResult = remoteBenchmarks.get(i);

            remoteTotalTime += currentResult.getProcessingTime();
            imageDataExchanged += currentResult.getImageSize();

            if (currentResult.getProcessingTime() < remoteTimeMin) {
                remoteTimeMin = currentResult.getProcessingTime();
                remoteTimeMinIndex = i;
            }
            if (currentResult.getProcessingTime() > remoteTimeMax) {
                remoteTimeMax = currentResult.getProcessingTime();
                remoteTimeMaxIndex = i;
            }

            if (currentResult.getImageSize() < exchangedDataMin) {
                exchangedDataMin = currentResult.getImageSize();
                exchangedDataMinIndex = i;
            }
            if (currentResult.getImageSize() > exchangedDataMax) {
                exchangedDataMax = currentResult.getImageSize();
                exchangedDataMaxIndex = i;
            }
        }

        double avgLocalProcTime = (double) localTotalTime / localBenchmarks.size();
        double avgRemoteProcTime = (double) remoteTotalTime / remoteBenchmarks.size();
        double avgData = (double) imageDataExchanged / remoteBenchmarks.size();

        long localTimeStDev = 0;
        for (BenchmarkResult br : localBenchmarks) {
            localTimeStDev += Math.pow(br.getProcessingTime() - avgLocalProcTime, 2);
        }
        localTimeStDev = (long) Math.sqrt(localTimeStDev/ localBenchmarks.size());

        long remoteTimeStDev = 0;
        for (BenchmarkResult br : remoteBenchmarks) {
            remoteTimeStDev += Math.pow(br.getProcessingTime() - avgRemoteProcTime, 2);
        }
        remoteTimeStDev = (long) Math.sqrt(remoteTimeStDev/remoteBenchmarks.size());

        long dataStDev = 0;
        for (BenchmarkResult br : remoteBenchmarks) {
            dataStDev += Math.pow(br.getImageSize() - avgData, 2);
        }
        dataStDev = (long) Math.sqrt(dataStDev/remoteBenchmarks.size());

        numberOfImages.setText("" + localBenchmarks.size());

        localProcessingTime.setText("" + localTotalTime);
        remoteProcessingTime.setText("" + remoteTotalTime);

        localProcessingTimeMin.setText("" + localTimeMin + " (" + localTimeMinIndex + ")");
        localProcessingTimeMax.setText("" + localTimeMax + " (" + localTimeMaxIndex + ")");

        remoteProcessingTimeMin.setText("" + remoteTimeMin + " (" + remoteTimeMinIndex + ")");
        remoteProcessingTimeMax.setText("" + remoteTimeMax + " (" + remoteTimeMaxIndex + ")");

        avgLocalProcessingTime.setText(String.format("%.3f", avgLocalProcTime));
        avgRemoteProcessingTime.setText(String.format("%.3f", avgRemoteProcTime));

        localProcessingTimeDev.setText("" + localTimeStDev);
        remoteProcessingTimeDev.setText("" + remoteTimeStDev);

        dataExchanged.setText("" + imageDataExchanged);
        dataExchangedAvg.setText("" + avgData);
        dataExchangedDev.setText("" + dataStDev);
        dataExchangedMin.setText("" + exchangedDataMin + " (" + exchangedDataMinIndex + ")");
        dataExchangedMax.setText("" + exchangedDataMax + " (" + exchangedDataMaxIndex + ")");
    }
}
