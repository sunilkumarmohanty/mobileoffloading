package fi.aalto.mobileoffloading.models;

public class OcrEntry {
    private String text;
    private String createdAt;
    private long processingTime;
    private String original;
    private String thumbnail;

    public OcrEntry(String text) {
        this.text = text;
    }

    public OcrEntry(String text, String createdAt, long processingTime, String original, String thumbnail) {
        this.text = text;
        this.createdAt = createdAt;
        this.processingTime = processingTime;
        this.original = original;
        this.thumbnail = thumbnail;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
