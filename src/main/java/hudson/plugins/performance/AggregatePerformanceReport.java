package hudson.plugins.performance;

public class AggregatePerformanceReport extends PerformanceReport {

    private int samplesCount;

    private long averageTime;

    private long aggregateMedian;

    private long aggregate90Percentile;

    private long minTime;

    private long maxTime;

    private double errorPercentage;

    public void addUriReport(UriReport uriReport) {
        uriReportMap.put(uriReport.getStaplerUri(), uriReport);
    }

    @Override
    public int countErrors() {
        return (int) (samplesCount * errorPercentage);
    }

    @Override
    public double errorPercent() {
        return errorPercentage * 100;
    }

    @Override
    public long getAverage() {
        return averageTime;
    }

    @Override
    public long get90Line() {
        return aggregate90Percentile;
    }

    @Override
    public long getMedian() {
        return aggregateMedian;
    }

    @Override
    public long getPercentileLine(int percentile) {
        return -1;
    }

    @Override
    public long getMax() {
        return maxTime;
    }

    @Override
    public long getMin() {
        return minTime;
    }

    @Override
    public int size() {
        return samplesCount;
    }

    public void setSamplesCount(int samplesCount) {
        this.samplesCount = samplesCount;
    }

    public void setAverageTime(long averageTime) {
        this.averageTime = averageTime;
    }

    public void setAggregateMedian(long aggregateMedian) {
        this.aggregateMedian = aggregateMedian;
    }

    public void setAggregate90Percentile(long aggregate90Percentile) {
        this.aggregate90Percentile = aggregate90Percentile;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public void setErrorPercentage(double errorPercentage) {
        this.errorPercentage = errorPercentage;
    }

}
