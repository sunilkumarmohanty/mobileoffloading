package fi.aalto.mobileoffloading.models;

public class BenchmarkResult {
    private String resultText;
    private long processingTime;
    private long imageSize;

    public BenchmarkResult(String resultText, long processingTime) {
        this.resultText = resultText;
        this.processingTime = processingTime;
    }

    public BenchmarkResult(String resultText, long processingTime, long imageSize) {
        this.resultText = resultText;
        this.processingTime = processingTime;
        this.imageSize = imageSize;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public long getImageSize() {
        return imageSize;
    }

    public void setImageSize(long imageSize) {
        this.imageSize = imageSize;
    }
}
