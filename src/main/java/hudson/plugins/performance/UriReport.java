package hudson.plugins.performance;

import hudson.model.ModelObject;
import hudson.model.AbstractBuild;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A report about a particular tested URI.
 * 
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements ModelObject,
        Comparable<UriReport> {

    public final static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

    /**
     * Individual HTTP invocations to this URI and how they went.
     */
    private final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

    /**
     * The parent object to which this object belongs.
     */
    private final PerformanceReport performanceReport;

    /**
     * Escaped {@link #uri} that doesn't contain any letters that cannot be used
     * as a token in URL.
     */
    private final String staplerUri;

    private UriReport lastBuildUriReport;

    private String uri;

    UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
        this.performanceReport = performanceReport;
        this.staplerUri = staplerUri;
        this.uri = uri;
    }

    public void addHttpSample(HttpSample httpSample) {
        httpSampleList.add(httpSample);
    }

    public int compareTo(UriReport uriReport) {
        if (uriReport == this) {
            return 0;
        }
        return uriReport.getUri().compareTo(this.getUri());
    }

    @Override
    public int countErrors() {
        int nbError = 0;
        for (final HttpSample currentSample : httpSampleList) {
            if (!currentSample.isSuccessful()) {
                nbError++;
            }
        }
        return nbError;
    }

    @Override
    public double errorPercent() {
        return ((double) countErrors()) / size() * 100;
    }

    @Override
    public long getAverage() {
        long average = 0;
        for (final HttpSample currentSample : httpSampleList) {
            average += currentSample.getDuration();
        }
        return average / size();
    }

    @Override
    public long get90Line() {
        long result = 0;
        Collections.sort(httpSampleList);
        if (httpSampleList.size() > 0) {
            result = httpSampleList.get((int) (httpSampleList.size() * .9)).getDuration();
        }
        return result;
    }

    @Override
    public long getPercentileLine(int percentile) {
        long result = 0;
        Collections.sort(httpSampleList);
        if (httpSampleList.size() > 0) {
            result = httpSampleList.get((int) (httpSampleList.size() * (percentile / 100d))).getDuration();
        }
        return result;
    }

    @Override
    public String getHttpCode() {
        String result = "";

        for (final HttpSample currentSample : httpSampleList) {
            if (!result.matches(".*" + currentSample.getHttpCode() + ".*")) {
                result += (result.length() > 1) ? "," + currentSample.getHttpCode() : currentSample.getHttpCode();
            }
        }

        return result;
    }

    @Override
    public long getMedian() {
        long result = 0;
        Collections.sort(httpSampleList);
        if (httpSampleList.size() > 0) {
            result = httpSampleList.get((int) (httpSampleList.size() * .5)).getDuration();
        }
        return result;
    }

    public AbstractBuild<?, ?> getBuild() {
        return performanceReport.getBuild();
    }

    public String getDisplayName() {
        return getUri();
    }

    public List<HttpSample> getHttpSampleList() {
        return httpSampleList;
    }

    public PerformanceReport getPerformanceReport() {
        return performanceReport;
    }

    @Override
    public long getMax() {
        long max = Long.MIN_VALUE;
        for (final HttpSample currentSample : httpSampleList) {
            max = Math.max(max, currentSample.getDuration());
        }
        return max;
    }

    @Override
    public long getMin() {
        long min = Long.MAX_VALUE;
        for (final HttpSample currentSample : httpSampleList) {
            min = Math.min(min, currentSample.getDuration());
        }
        return min;
    }

    public String getStaplerUri() {
        return staplerUri;
    }

    public String getUri() {
        return uri;
    }

    public String getShortUri() {
        if (uri.length() > 130) {
            return uri.substring(0, 129);
        }
        return uri;
    }

    public boolean isFailed() {
        return countErrors() != 0;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public int size() {
        return httpSampleList.size();
    }

    public String encodeUriReport() throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder(120);
        sb.append(performanceReport.getReportFileName()).append(
                GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(
                END_PERFORMANCE_PARAMETER);
        return URLEncoder.encode(sb.toString(), "UTF-8");
    }

    public void addLastBuildUriReport(UriReport report) {
        this.lastBuildUriReport = report;
    }

    @Override
    public long getAverageDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return getAverage() - lastBuildUriReport.getAverage();
    }

    @Override
    public long getMedianDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return getMedian() - lastBuildUriReport.getMedian();
    }

    @Override
    public double getErrorPercentDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return errorPercent() - lastBuildUriReport.errorPercent();
    }

    @Override
    public String getLastBuildHttpCodeIfChanged() {
        if (lastBuildUriReport == null) {
            return "";
        }

        if (lastBuildUriReport.getHttpCode().equals(getHttpCode())) {
            return "";
        }

        return lastBuildUriReport.getHttpCode();
    }

    @Override
    public int getSizeDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return size() - lastBuildUriReport.size();
    }

}
