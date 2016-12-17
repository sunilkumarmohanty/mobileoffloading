package fi.aalto.mobileoffloading.models;

import java.util.List;

public class BenchmarkResultContainer {
    private List<BenchmarkResult> localBenchmarks;
    private List<BenchmarkResult> remoteBenchmarks;

    public BenchmarkResultContainer(List<BenchmarkResult> localBenchmarks, List<BenchmarkResult> remoteBenchmarks) {
        this.localBenchmarks = localBenchmarks;
        this.remoteBenchmarks = remoteBenchmarks;
    }

    public List<BenchmarkResult> getLocalBenchmarks() {
        return localBenchmarks;
    }

    public void setLocalBenchmarks(List<BenchmarkResult> localBenchmarks) {
        this.localBenchmarks = localBenchmarks;
    }

    public List<BenchmarkResult> getRemoteBenchmarks() {
        return remoteBenchmarks;
    }

    public void setRemoteBenchmarks(List<BenchmarkResult> remoteBenchmarks) {
        this.remoteBenchmarks = remoteBenchmarks;
    }
}
