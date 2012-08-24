package hudson.plugins.performance;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public final class PerformanceProjectAction implements Action {

    private static final String CONFIGURE_LINK = "configure";
    private static final String TRENDREPORT_LINK = "trendReport";
    private static final String TESTSUITE_LINK = "testsuiteReport";

    private static final String PLUGIN_NAME = "performance";

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(PerformanceProjectAction.class.getName());

    public final AbstractProject<?, ?> project;

    private transient List<String> performanceReportList;

    public String getDisplayName() {
        return Messages.ProjectAction_DisplayName();
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return PLUGIN_NAME;
    }

    public PerformanceProjectAction(AbstractProject project) {
        this.project = project;
    }

    private JFreeChart createErrorsChart(CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createLineChart(
                Messages.ProjectAction_PercentageOfErrors(), // chart title
                null, // unused
                "%", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
                );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        final CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperBound(100);
        rangeAxis.setLowerBound(0);

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(4.0f));
        ColorPalette.apply(renderer);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

        return chart;
    }

    protected static JFreeChart createRespondingTimeChart(CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createLineChart(
                Messages.ProjectAction_RespondingTime(), // charttitle
                null, // unused
                "ms", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
                );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        final CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(4.0f));
        ColorPalette.apply(renderer);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

        return chart;
    }

    protected static JFreeChart createSummarizerChart(CategoryDataset dataset, String yAxis, String chartTitle) {

        final JFreeChart chart = ChartFactory.createBarChart(
                chartTitle, // chart title
                null, // unused
                yAxis, // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                true // urls
                );

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBaseStroke(new BasicStroke(4.0f));
        renderer.setItemMargin(0);
        renderer.setMaximumBarWidth(0.05);

        return chart;
    }

    public void doErrorsGraph(StaplerRequest request, StaplerResponse response)
            throws IOException {
        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
            if (getPerformanceReportList().size() == 1) {
                performanceReportNameFile = getPerformanceReportList().get(0);
            } else {
                return;
            }
        }
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        final List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (final AbstractBuild<?, ?> currentBuild : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {

                if (!buildsLimits.includedByStep(currentBuild.number)) {
                    continue;
                }

                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
                final PerformanceBuildAction performanceBuildAction = currentBuild
                        .getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                final PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap()
                        .getPerformanceReport(
                                performanceReportNameFile);
                if (performanceReport == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }
                dataSetBuilderErrors.add(performanceReport.errorPercent(),
                        Messages.ProjectAction_Errors(), label);
            }
            nbBuildsToAnalyze--;
        }
        ChartUtil.generateGraph(request, response,
                createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
    }

    public void doRespondingTimeGraphPerTestCaseMode(StaplerRequest request,
            StaplerResponse response) throws IOException {
        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
            if (getPerformanceReportList().size() == 1) {
                performanceReportNameFile = getPerformanceReportList().get(0);
            } else {
                return;
            }
        }
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        final List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();

        for (final AbstractBuild<?, ?> build : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {
                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);

                if (!buildsLimits.includedByStep(build.number)) {
                    continue;
                }
                final PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                final PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap()
                        .getPerformanceReport(
                                performanceReportNameFile);
                if (performanceReport == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }

                final List<HttpSample> allSamples = new ArrayList<HttpSample>();
                for (final UriReport currentReport : performanceReport.getUriReportMap().values()) {
                    allSamples.addAll(currentReport.getHttpSampleList());
                }
                Collections.sort(allSamples);
                for (final HttpSample sample : allSamples) {
                    if (sample.hasError()) {
                        // we set duration as 0 for failed tests
                        dataSetBuilderAverage.add(0,
                                sample.getUri(), label);
                    }
                    else {
                        dataSetBuilderAverage.add(sample.getDuration(),
                                sample.getUri(), label);
                    }
                }

            }
            nbBuildsToAnalyze--;
        }
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(dataSetBuilderAverage.build()), 600, 200);

    }

    public void doRespondingTimeGraph(StaplerRequest request,
            StaplerResponse response) throws IOException {
        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
            if (getPerformanceReportList().size() == 1) {
                performanceReportNameFile = getPerformanceReportList().get(0);
            } else {
                return;
            }
        }
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        final List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (final AbstractBuild<?, ?> build : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {

                if (!buildsLimits.includedByStep(build.number)) {
                    continue;
                }

                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
                final PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                final PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap()
                        .getPerformanceReport(
                                performanceReportNameFile);
                if (performanceReport == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }
                dataSetBuilderAverage.add(performanceReport.getMedian(),
                        Messages.ProjectAction_Median(), label);
                dataSetBuilderAverage.add(performanceReport.getAverage(),
                        Messages.ProjectAction_Average(), label);
                dataSetBuilderAverage.add(performanceReport.get90Line(),
                        Messages.ProjectAction_Line90(), label);
            }
            nbBuildsToAnalyze--;
            continue;
        }
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(dataSetBuilderAverage.build()), 400, 200);
    }

    public void doSummarizerGraph(StaplerRequest request,
            StaplerResponse response) throws IOException {

        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        if (performanceReportNameFile == null) {
            if (getPerformanceReportList().size() == 1) {
                performanceReportNameFile = getPerformanceReportList().get(0);
            } else {
                return;
            }
        }
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            // response.sendRedirect2(request.getContextPath() +
            // "/images/headless.png");
            return;
        }
        final DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizer = new DataSetBuilder<NumberOnlyBuildLabel, String>();
        final DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizerErrors = new DataSetBuilder<NumberOnlyBuildLabel, String>();

        final List<?> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (final Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
            final AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
            if (buildsLimits.in(nbBuildsToAnalyze)) {
                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
                final PerformanceBuildAction performanceBuildAction = currentBuild
                        .getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                final PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap()
                        .getPerformanceReport(
                                performanceReportNameFile);

                if (performanceReport == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }

                for (final String key : performanceReport.getUriReportMap().keySet()) {
                    final Long methodAvg = performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0)
                            .getDuration();
                    final float methodErrors = performanceReport.getUriReportMap().get(key).getHttpSampleList().get(0)
                            .getSummarizerErrors();
                    dataSetBuilderSummarizer.add(methodAvg, label, key);
                    dataSetBuilderSummarizerErrors.add(methodErrors, label, key);
                }
            }

            nbBuildsToAnalyze--;
        }

        final String summarizerReportType = performanceReportPosition.getSummarizerReportType();
        if (summarizerReportType != null) {
            ChartUtil.generateGraph(
                    request,
                    response,
                    createSummarizerChart(dataSetBuilderSummarizerErrors.build(), "%",
                            Messages.ProjectAction_PercentageOfErrors()), 400, 200);
        }
        else {
            ChartUtil.generateGraph(
                    request,
                    response,
                    createSummarizerChart(dataSetBuilderSummarizer.build(), "ms",
                            Messages.ProjectAction_RespondingTime()), 400, 200);
        }

    }

    /**
     * <p>
     * give a list of two Integer : the smallest build to use and the biggest.
     * </p>
     * 
     * @param request
     * @param builds
     * @return outList
     */
    private Range getFirstAndLastBuild(StaplerRequest request, List<?> builds) {
        final GraphConfigurationDetail graphConf = (GraphConfigurationDetail) createUserConfiguration(request);

        if (graphConf.isNone()) {
            return all(builds);
        }

        if (graphConf.isBuildCount()) {
            if (graphConf.getBuildCount() <= 0) {
                return all(builds);
            }
            final int first = builds.size() - graphConf.getBuildCount();
            return new Range(first > 0 ? first + 1 : 1,
                    builds.size());
        } else if (graphConf.isBuildNth()) {
            if (graphConf.getBuildStep() <= 0) {
                return all(builds);
            }
            return new Range(1, builds.size(), graphConf.getBuildStep());
        } else if (graphConf.isDate()) {
            if (graphConf.isDefaultDates()) {
                return all(builds);
            }
            int firstBuild = -1;
            int lastBuild = -1;
            int var = builds.size();
            GregorianCalendar firstDate = null;
            GregorianCalendar lastDate = null;
            try {
                firstDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getFirstDayCount());
                lastDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getLastDayCount());
                lastDate.set(GregorianCalendar.HOUR_OF_DAY, 23);
                lastDate.set(GregorianCalendar.MINUTE, 59);
                lastDate.set(GregorianCalendar.SECOND, 59);

                for (final Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
                    final AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
                    final GregorianCalendar buildDate = new GregorianCalendar();
                    buildDate.setTime(currentBuild.getTimestamp().getTime());
                    if (firstDate.getTime().before(buildDate.getTime())) {
                        firstBuild = var;
                    }
                    if (lastBuild < 0 && lastDate.getTime().after(buildDate.getTime())) {
                        lastBuild = var;
                    }
                    var--;
                }
            } catch (final ParseException e) {
                LOGGER.log(Level.SEVERE, "Error during the manage of the Calendar", e);
            }

            return new Range(firstBuild, lastBuild);
        }
        throw new IllegalArgumentException("unsupported configType + " + graphConf.getConfigType());
    }

    public Range all(List<?> builds) {
        return new Range(1, builds.size());
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public List<String> getPerformanceReportList() {
        this.performanceReportList = new ArrayList<String>(0);
        if (null == this.project) {
            return performanceReportList;
        }
        if (null == this.project.getSomeBuildWithWorkspace()) {
            return performanceReportList;
        }
        final File file = new File(this.project.getSomeBuildWithWorkspace().getRootDir(),
                PerformanceReportMap.getPerformanceReportDirRelativePath());
        if (!file.isDirectory()) {
            return performanceReportList;
        }

        for (final File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                for (final File e : entry.listFiles()) {
                    this.performanceReportList.add(e.getName());
                }
            } else {
                this.performanceReportList.add(entry.getName());
            }

        }

        Collections.sort(performanceReportList);

        return this.performanceReportList;
    }

    public void setPerformanceReportList(List<String> performanceReportList) {
        this.performanceReportList = performanceReportList;
    }

    public boolean isTrendVisibleOnProjectDashboard() {
        final List<String> reportList = getPerformanceReportList();
        return reportList != null && reportList.size() == 1;
    }

    /**
     * Returns the graph configuration for this project.
     * 
     * @param link
     *            not used
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of the analysis (detail page).
     */
    public Object getDynamic(final String link, final StaplerRequest request,
            final StaplerResponse response) {
        if (CONFIGURE_LINK.equals(link)) {
            return createUserConfiguration(request);
        } else if (TRENDREPORT_LINK.equals(link)) {
            return createTrendReport(request);
        } else if (TESTSUITE_LINK.equals(link)) {
            return createTestsuiteReport(request, response);
        } else {
            return null;
        }
    }

    /**
     * Creates a view to configure the trend graph for the current user.
     * 
     * @param request
     *            Stapler request
     * @return a view to configure the trend graph for the current user
     */
    private Object createUserConfiguration(final StaplerRequest request) {
        final GraphConfigurationDetail graph = new GraphConfigurationDetail(project,
                PLUGIN_NAME, request);
        return graph;
    }

    /**
     * Creates a view to configure the trend graph for the current user.
     * 
     * @param request
     *            Stapler request
     * @return a view to configure the trend graph for the current user
     */
    private Object createTrendReport(final StaplerRequest request) {
        final String filename = getTrendReportFilename(request);
        final CategoryDataset dataSet = getTrendReportData(request, filename).build();
        final TrendReportDetail report = new TrendReportDetail(project, PLUGIN_NAME,
                request, filename, dataSet);
        return report;
    }

    private Object createTestsuiteReport(final StaplerRequest request, final StaplerResponse response) {
        final String filename = getTestSuiteReportFilename(request);

        final List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        final TestSuiteReportDetail report = new TestSuiteReportDetail(project, PLUGIN_NAME,
                request, filename, buildsLimits);

        return report;
    }

    private String getTrendReportFilename(final StaplerRequest request) {
        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        return performanceReportPosition.getPerformanceReportPosition();
    }

    private String getTestSuiteReportFilename(final StaplerRequest request) {
        final PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        return performanceReportPosition.getPerformanceReportPosition();
    }

    private DataSetBuilder getTrendReportData(final StaplerRequest request,
            String performanceReportNameFile) {

        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSet = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        final List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        final Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (final AbstractBuild<?, ?> currentBuild : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {
                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
                final PerformanceBuildAction performanceBuildAction = currentBuild
                        .getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                PerformanceReport report = null;
                report = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
                        performanceReportNameFile);
                if (report == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }
                dataSet.add(Math.round(report.getAverage()),
                        Messages.ProjectAction_Average(), label);
                dataSet.add(Math.round(report.getMedian()),
                        Messages.ProjectAction_Median(), label);
                dataSet.add(Math.round(report.get90Line()),
                        Messages.ProjectAction_Line90(), label);
                dataSet.add(Math.round(report.getMin()),
                        Messages.ProjectAction_Minimum(), label);
                dataSet.add(Math.round(report.getMax()),
                        Messages.ProjectAction_Maximum(), label);
                dataSet.add(Math.round(report.errorPercent()),
                        Messages.ProjectAction_PercentageOfErrors(), label);
                dataSet.add(Math.round(report.countErrors()),
                        Messages.ProjectAction_Errors(), label);
            }
            nbBuildsToAnalyze--;
        }
        return dataSet;
    }

    public boolean ifSummarizerParserUsed(String filename) {

        boolean b = false;
        String fileExt = "";

        final List<PerformanceReportParser> list = project.getPublishersList().get(PerformancePublisher.class)
                .getParsers();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getDescriptor().getDisplayName() == "JmeterSummarizer") {
                fileExt = list.get(i).glob;
                final String parts[] = fileExt.split("\\s*[;:,]+\\s*");
                for (final String path : parts) {
                    if (filename.endsWith(path.substring(5))) {
                        b = true;
                    }
                }
            }
        }

        return b;
    }

    public boolean ifModePerformancePerTestCaseUsed() {
        return project.getPublishersList().get(PerformancePublisher.class).isModePerformancePerTestCase();
    }

    public static class Range {

        public int first;

        public int last;

        public int step;

        public Range(int first, int last) {
            this.first = first;
            this.last = last;
            this.step = 1;
        }

        public Range(int first, int last, int step) {
            this(first, last);
            this.step = step;
        }

        public boolean in(int nbBuildsToAnalyze) {
            return nbBuildsToAnalyze <= last
                    && first <= nbBuildsToAnalyze;
        }

        public boolean includedByStep(int buildNumber) {
            if (buildNumber % step == 0) {
                return true;
            }
            return false;
        }

    }
}
