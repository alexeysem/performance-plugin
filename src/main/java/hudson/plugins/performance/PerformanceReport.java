package hudson.plugins.performance;

import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

/**
 * Represents a single performance report, which consists of multiple
 * {@link UriReport}s for different URLs that was tested.
 * 
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractReport implements
        Comparable<PerformanceReport> {

    private PerformanceBuildAction buildAction;

    private HttpSample httpSample;

    private String reportFileName = null;

    /**
     * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
     */
    protected final Map<String, UriReport> uriReportMap = new LinkedHashMap<String, UriReport>();

    private PerformanceReport lastBuildReport;

    public void addSample(HttpSample pHttpSample) throws SAXException {
        final String uri = pHttpSample.getUri();
        if (uri == null) {
            buildAction
                    .getHudsonConsoleWriter()
                    .println(
                            "label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
            return;
        }
        final String staplerUri = uri.replace("http:", "").replaceAll("/", "_");
        UriReport uriReport = uriReportMap.get(staplerUri);
        if (uriReport == null) {
            uriReport = new UriReport(this, staplerUri, uri);
            uriReportMap.put(staplerUri, uriReport);
        }
        uriReport.addHttpSample(pHttpSample);

    }

    public int compareTo(PerformanceReport jmReport) {
        if (this == jmReport) {
            return 0;
        }
        return getReportFileName().compareTo(jmReport.getReportFileName());
    }

    @Override
    public int countErrors() {
        int nbError = 0;
        for (final UriReport currentReport : uriReportMap.values()) {
            if (buildAction.getPerformanceReportMap().ifSummarizerParserUsed(reportFileName)) {
                nbError += currentReport.getHttpSampleList().get(0).getSummarizerErrors();
            } else {
                nbError += currentReport.countErrors();
            }
        }
        return nbError;
    }

    @Override
    public double errorPercent() {
        if (buildAction.getPerformanceReportMap().ifSummarizerParserUsed(reportFileName)) {
            return size() == 0 ? 0 : ((double) countErrors()) / size();
        } else {
            return size() == 0 ? 0 : ((double) countErrors()) / size() * 100;
        }
    }

    @Override
    public long getAverage() {
        long result = 0;
        final int size = size();
        if (size != 0) {
            long average = 0;
            for (final UriReport currentReport : uriReportMap.values()) {
                average += currentReport.getAverage() * currentReport.size();
            }
            final double test = average / size;
            result = (int) test;
        }
        return result;
    }

    @Override
    public long get90Line() {
        return getPercentileLine(90);
    }

    @Override
    public long getMedian() {
        return getPercentileLine(50);
    }

    @Override
    public long getPercentileLine(int percentile) {
        long result = 0;
        final int size = size();
        if (size != 0) {
            final List<HttpSample> allSamples = new ArrayList<HttpSample>();
            for (final UriReport currentReport : uriReportMap.values()) {
                allSamples.addAll(currentReport.getHttpSampleList());
            }
            Collections.sort(allSamples);
            result = allSamples.get((int) (allSamples.size() * (percentile / 100d))).getDuration();
        }
        return result;
    }

    @Override
    public String getHttpCode() {
        return "";
    }

    public AbstractBuild<?, ?> getBuild() {
        return buildAction.getBuild();
    }

    PerformanceBuildAction getBuildAction() {
        return buildAction;
    }

    public String getDisplayName() {
        return Messages.Report_DisplayName();
    }

    public UriReport getDynamic(String token) throws IOException {
        return getUriReportMap().get(token);
    }

    public HttpSample getHttpSample() {
        return httpSample;
    }

    @Override
    public long getMax() {
        long max = Long.MIN_VALUE;
        for (final UriReport currentReport : uriReportMap.values()) {
            max = Math.max(currentReport.getMax(), max);
        }
        return max;
    }

    @Override
    public long getMin() {
        long min = Long.MAX_VALUE;
        for (final UriReport currentReport : uriReportMap.values()) {
            min = Math.min(currentReport.getMin(), min);
        }
        return min;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public List<UriReport> getUriListOrdered() {
        final Collection<UriReport> uriCollection = getUriReportMap().values();
        final List<UriReport> UriReportList = new ArrayList<UriReport>(uriCollection);
        return UriReportList;
    }

    public Map<String, UriReport> getUriReportMap() {
        return uriReportMap;
    }

    void setBuildAction(PerformanceBuildAction buildAction) {
        this.buildAction = buildAction;
    }

    public void setHttpSample(HttpSample httpSample) {
        this.httpSample = httpSample;
    }

    public void setReportFileName(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    @Override
    public int size() {
        int size = 0;
        for (final UriReport currentReport : uriReportMap.values()) {
            size += currentReport.size();
        }
        return size;
    }

    public void setLastBuildReport(PerformanceReport lastBuildReport) {
        final Map<String, UriReport> lastBuildUriReportMap = lastBuildReport.getUriReportMap();
        for (final Map.Entry<String, UriReport> item : uriReportMap.entrySet()) {
            final UriReport lastBuildUri = lastBuildUriReportMap.get(item.getKey());
            if (lastBuildUri != null) {
                item.getValue().addLastBuildUriReport(lastBuildUri);
            } else {
            }
        }
        this.lastBuildReport = lastBuildReport;
    }

    @Override
    public long getAverageDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return getAverage() - lastBuildReport.getAverage();
    }

    @Override
    public long getMedianDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return getMedian() - lastBuildReport.getMedian();
    }

    @Override
    public double getErrorPercentDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return errorPercent() - lastBuildReport.errorPercent();
    }

    @Override
    public String getLastBuildHttpCodeIfChanged() {
        return "";
    }

    @Override
    public int getSizeDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return size() - lastBuildReport.size();
    }

}
