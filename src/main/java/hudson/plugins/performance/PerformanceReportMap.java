package hudson.plugins.performance;

import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Root object of a performance report.
 */
@SuppressWarnings("hiding")
public class PerformanceReportMap implements ModelObject {

    /**
     * The {@link PerformanceBuildAction} that this report belongs to.
     */
    private transient PerformanceBuildAction buildAction;
    /**
     * {@link PerformanceReport}s are keyed by
     * {@link PerformanceReport#reportFileName}
     * 
     * Test names are arbitrary human-readable and URL-safe string that
     * identifies an individual report.
     */
    private Map<String, PerformanceReport> performanceReportMap = new LinkedHashMap<String, PerformanceReport>();
    private static final String PERFORMANCE_REPORTS_DIRECTORY = "performance-reports";

    private static AbstractBuild<?, ?> currentBuild = null;

    /**
     * Parses the reports and build a {@link PerformanceReportMap}.
     * 
     * @throws IOException
     *             If a report fails to parse.
     */
    PerformanceReportMap(final PerformanceBuildAction buildAction, TaskListener listener)
            throws IOException {
        this.buildAction = buildAction;
        parseReports(getBuild(), listener, new PerformanceReportCollector() {

            public void addAll(Collection<PerformanceReport> reports) {
                for (final PerformanceReport r : reports) {
                    r.setBuildAction(buildAction);
                    performanceReportMap.put(r.getReportFileName(), r);
                }
            }
        }, null);
    }

    private void addAll(Collection<PerformanceReport> reports) {
        for (final PerformanceReport r : reports) {
            r.setBuildAction(buildAction);
            performanceReportMap.put(r.getReportFileName(), r);
        }
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

    public List<PerformanceReport> getPerformanceListOrdered() {
        final List<PerformanceReport> listPerformance = new ArrayList<PerformanceReport>(
                getPerformanceReportMap().values());
        Collections.sort(listPerformance);
        return listPerformance;
    }

    public Map<String, PerformanceReport> getPerformanceReportMap() {
        return performanceReportMap;
    }

    /**
     * <p>
     * Give the Performance report with the parameter for name in Bean
     * </p>
     * 
     * @param performanceReportName
     * @return
     */
    public PerformanceReport getPerformanceReport(String performanceReportName) {
        return performanceReportMap.get(performanceReportName);
    }

    /**
     * Get a URI report within a Performance report file
     * 
     * @param uriReport
     *            "Performance report file name";"URI name"
     * @return
     */
    public UriReport getUriReport(String uriReport) {
        if (uriReport != null) {
            String uriReportDecoded;
            try {
                uriReportDecoded = URLDecoder.decode(uriReport.replace(
                        UriReport.END_PERFORMANCE_PARAMETER, ""), "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            final StringTokenizer st = new StringTokenizer(uriReportDecoded,
                    GraphConfigurationDetail.SEPARATOR);
            return getPerformanceReportMap().get(st.nextToken()).getUriReportMap().get(
                    st.nextToken());
        }
        return null;
    }

    public String getUrlName() {
        return "performanceReportList";
    }

    void setBuildAction(PerformanceBuildAction buildAction) {
        this.buildAction = buildAction;
    }

    public void setPerformanceReportMap(
            Map<String, PerformanceReport> performanceReportMap) {
        this.performanceReportMap = performanceReportMap;
    }

    public static String getPerformanceReportFileRelativePath(
            String parserDisplayName, String reportFileName) {
        return getRelativePath(parserDisplayName, reportFileName);
    }

    public static String getPerformanceReportDirRelativePath() {
        return getRelativePath();
    }

    private static String getRelativePath(String... suffixes) {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(PERFORMANCE_REPORTS_DIRECTORY);
        for (final String suffix : suffixes) {
            sb.append(File.separator).append(suffix);
        }
        return sb.toString();
    }

    /**
     * <p>
     * Verify if the PerformanceReport exist the performanceReportName must to
     * be like it is in the build
     * </p>
     * 
     * @param performanceReportName
     * @return boolean
     */
    public boolean isFailed(String performanceReportName) {
        return getPerformanceReport(performanceReportName) == null;
    }

    public void doRespondingTimeGraph(StaplerRequest request,
            StaplerResponse response) throws IOException {
        final String parameter = request.getParameter("performanceReportPosition");
        AbstractBuild<?, ?> previousBuild = getBuild();
        final Map<AbstractBuild<?, ?>, Map<String, PerformanceReport>> buildReports = new LinkedHashMap<AbstractBuild<?, ?>, Map<String, PerformanceReport>>();
        while (previousBuild != null) {
            final AbstractBuild<?, ?> currentBuild = previousBuild;
            parseReports(currentBuild, TaskListener.NULL, new PerformanceReportCollector() {

                public void addAll(Collection<PerformanceReport> parse) {
                    for (final PerformanceReport performanceReport : parse) {
                        if (buildReports.get(currentBuild) == null) {
                            final Map<String, PerformanceReport> map = new LinkedHashMap<String, PerformanceReport>();
                            buildReports.put(currentBuild, map);
                        }
                        buildReports.get(currentBuild).put(performanceReport.getReportFileName(), performanceReport);
                    }
                }
            }, parameter);
            previousBuild = previousBuild.getPreviousBuild();
        }
        // Now we should have the data necessary to generate the graphs!
        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        for (final AbstractBuild<?, ?> currentBuild : buildReports.keySet()) {
            final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
            final PerformanceReport report = buildReports.get(currentBuild).get(parameter);
            dataSetBuilderAverage.add(report.getAverage(), Messages.ProjectAction_Average(), label);
        }
        ChartUtil.generateGraph(request, response,
                PerformanceProjectAction.createRespondingTimeChart(dataSetBuilderAverage.build()), 400, 200);
    }

    public void doSummarizerGraph(StaplerRequest request,
            StaplerResponse response) throws IOException {
        final String parameter = request.getParameter("performanceReportPosition");
        AbstractBuild<?, ?> previousBuild = getBuild();
        final Map<AbstractBuild<?, ?>, Map<String, PerformanceReport>> buildReports = new LinkedHashMap<AbstractBuild<?, ?>, Map<String, PerformanceReport>>();

        while (previousBuild != null) {
            final AbstractBuild<?, ?> currentBuild = previousBuild;
            parseReports(currentBuild, TaskListener.NULL, new PerformanceReportCollector() {

                public void addAll(Collection<PerformanceReport> parse) {
                    for (final PerformanceReport performanceReport : parse) {
                        if (buildReports.get(currentBuild) == null) {
                            final Map<String, PerformanceReport> map = new LinkedHashMap<String, PerformanceReport>();
                            buildReports.put(currentBuild, map);
                        }
                        buildReports.get(currentBuild).put(performanceReport.getReportFileName(), performanceReport);
                    }
                }
            }, parameter);
            previousBuild = previousBuild.getPreviousBuild();
        }
        final DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizer = new DataSetBuilder<NumberOnlyBuildLabel, String>();
        for (final AbstractBuild<?, ?> currentBuild : buildReports.keySet()) {
            final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
            final PerformanceReport report = buildReports.get(currentBuild).get(parameter);

            // Now we should have the data necessary to generate the graphs!
            for (final String key : report.getUriReportMap().keySet()) {
                final Long methodAvg = report.getUriReportMap().get(key).getHttpSampleList().get(0).getDuration();
                dataSetBuilderSummarizer.add(methodAvg, label, key);
            }
        }
        ChartUtil.generateGraph(
                request,
                response,
                PerformanceProjectAction.createSummarizerChart(dataSetBuilderSummarizer.build(), "ms",
                        Messages.ProjectAction_RespondingTime()), 400, 200);
    }

    private void parseReports(AbstractBuild<?, ?> build, TaskListener listener, PerformanceReportCollector collector,
            final String filename) throws IOException {
        final File repo = new File(build.getRootDir(),
                PerformanceReportMap.getPerformanceReportDirRelativePath());

        // files directly under the directory are for JMeter, for compatibility
        // reasons.
        final File[] files = repo.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return !f.isDirectory();
            }
        });
        // this may fail, if the build itself failed, we need to recover
        // gracefully
        if (files != null) {
            addAll(new JMeterParser("").parse(build,
                    Arrays.asList(files), listener));
        }

        // otherwise subdirectory name designates the parser ID.
        final File[] dirs = repo.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        // this may fail, if the build itself failed, we need to recover
        // gracefully
        if (dirs != null) {
            for (final File dir : dirs) {
                final PerformanceReportParser p = buildAction.getParserByDisplayName(dir.getName());
                if (p != null) {
                    final File[] listFiles = dir.listFiles(new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            if (filename == null) {
                                return true;
                            }
                            if (name.equals(filename)) {
                                return true;
                            }
                            return false;
                        }
                    });
                    collector.addAll(p.parse(build, Arrays.asList(listFiles), listener));
                }
            }
        }

        addPreviousBuildReports();
    }

    private void addPreviousBuildReports() {

        // Avoid parsing all builds.
        if (PerformanceReportMap.currentBuild == null) {
            PerformanceReportMap.currentBuild = getBuild();
        } else {
            if (PerformanceReportMap.currentBuild != getBuild()) {
                PerformanceReportMap.currentBuild = null;
                return;
            }
        }

        final AbstractBuild<?, ?> previousBuild = getBuild().getPreviousBuild();
        if (previousBuild == null) {
            return;
        }

        final PerformanceBuildAction previousPerformanceAction = previousBuild.getAction(PerformanceBuildAction.class);
        if (previousPerformanceAction == null) {
            return;
        }

        final PerformanceReportMap previousPerformanceReportMap = previousPerformanceAction.getPerformanceReportMap();
        if (previousPerformanceReportMap == null) {
            return;
        }

        for (final Map.Entry<String, PerformanceReport> item : getPerformanceReportMap().entrySet()) {
            final PerformanceReport lastReport = previousPerformanceReportMap.getPerformanceReportMap().get(
                    item.getKey());
            if (lastReport != null) {
                item.getValue().setLastBuildReport(lastReport);
            }
        }
    }

    private interface PerformanceReportCollector {

        public void addAll(Collection<PerformanceReport> parse);
    }

    public boolean ifSummarizerParserUsed(String filename) {

        boolean b = false;
        String fileExt = "";

        final List<PerformanceReportParser> list = buildAction.getBuild().getProject().getPublishersList()
                .get(PerformancePublisher.class).getParsers();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getDescriptor().getDisplayName() == "JmeterSummarizer") {
                fileExt = list.get(i).glob;
                final String parts[] = fileExt.split("\\s*[;:,]+\\s*");
                for (final String path : parts) {
                    if (filename.endsWith(path.substring(5))) {
                        b = true;
                        return b;
                    }
                }
            }
        }

        return b;
    }

}
