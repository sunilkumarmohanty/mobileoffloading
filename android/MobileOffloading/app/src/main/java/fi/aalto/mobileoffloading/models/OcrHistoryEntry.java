package fi.aalto.mobileoffloading.models;

import java.util.List;

public class OcrHistoryEntry {
    private String text;
    private String createdAt;
    private long processingTime;
    private List<String> originals;
    private List<String> thumbnails;

    public OcrHistoryEntry(String text) {
        this.text = text;
    }

    public OcrHistoryEntry(String text, String createdAt, long processingTime, List<String> originals, List<String> thumbnails) {
        this.text = text;
        this.createdAt = createdAt;
        this.processingTime = processingTime;
        this.originals = originals;
        this.thumbnails = thumbnails;
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

    public List<String> getOriginals() {
        return originals;
    }

    public void setOriginals(List<String> originals) {
        this.originals = originals;
    }

    public List<String> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }
}
