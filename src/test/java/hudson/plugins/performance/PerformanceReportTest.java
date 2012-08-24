package hudson.plugins.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("hiding")
public class PerformanceReportTest {

    private PerformanceReport performanceReport;

    @Before
    public void setUp() {
        final PerformanceBuildAction buildAction = EasyMock
                .createMock(PerformanceBuildAction.class);
        performanceReport = new PerformanceReport();
        performanceReport.setBuildAction(buildAction);
    }

    @Test
    public void testAddSample() throws Exception {
        final PrintStream printStream = EasyMock.createMock(PrintStream.class);
        EasyMock.expect(
                performanceReport.getBuildAction().getHudsonConsoleWriter())
                .andReturn(printStream);
        printStream
                .println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
        EasyMock.replay(printStream);
        EasyMock.replay(performanceReport.getBuildAction());

        final HttpSample sample1 = new HttpSample();
        performanceReport.addSample(sample1);

        sample1.setUri("invalidCharacter/");
        performanceReport.addSample(sample1);
        UriReport uriReport = performanceReport.getUriReportMap().get(
                "invalidCharacter_");
        assertNotNull(uriReport);

        final String uri = "uri";
        sample1.setUri(uri);
        performanceReport.addSample(sample1);
        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        uriReport = uriReportMap.get(uri);
        assertNotNull(uriReport);
        final List<HttpSample> httpSampleList = uriReport.getHttpSampleList();
        assertEquals(1, httpSampleList.size());
        assertEquals(sample1, httpSampleList.get(0));
    }

    /*
     * @Test public void testCountError() throws SAXException { HttpSample
     * sample1 = new HttpSample(); sample1.setSuccessful(false);
     * sample1.setUri("sample1"); performanceReport.addSample(sample1);
     * 
     * HttpSample sample2 = new HttpSample(); sample2.setSuccessful(true);
     * sample2.setUri("sample2"); performanceReport.addSample(sample2);
     * assertEquals(1, performanceReport.countErrors()); }
     */
    @Test
    public void testPerformanceReport() throws IOException {
        final PerformanceReport performanceReport = parseOneJMeter(new File(
                "src/test/resources/JMeterResults.jtl"));
        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(2, uriReportMap.size());
        final String loginUri = "Home";
        final UriReport firstUriReport = uriReportMap.get(loginUri);
        final HttpSample firstHttpSample = firstUriReport.getHttpSampleList().get(0);
        assertEquals(loginUri, firstHttpSample.getUri());
        assertEquals(14720, firstHttpSample.getDuration());
        assertEquals(new Date(1296846793179L), firstHttpSample.getDate());
        assertTrue(firstHttpSample.isSuccessful());
        final String logoutUri = "Workgroup";
        final UriReport secondUriReport = uriReportMap.get(logoutUri);
        final HttpSample secondHttpSample = secondUriReport.getHttpSampleList()
                .get(0);
        assertEquals(logoutUri, secondHttpSample.getUri());
        assertEquals(278, secondHttpSample.getDuration());
        assertEquals(new Date(1296846847952L), secondHttpSample.getDate());
        assertTrue(secondHttpSample.isSuccessful());
    }

    private PerformanceReport parseOneJMeter(File f) throws IOException {
        return new JMeterParser("").parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out)).iterator().next();
    }

    private PerformanceReport parseOneJUnit(File f) throws IOException {
        return new JUnitParser("").parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out)).iterator().next();
    }

    @Test
    public void testPerformanceNonHTTPSamplesMultiThread() throws IOException {
        final PerformanceReport performanceReport = parseOneJMeter(new File(
                "src/test/resources/JMeterResultsMultiThread.jtl"));

        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(1, uriReportMap.size());

        final String uri = "WebService(SOAP) Request";
        final UriReport report = uriReportMap.get(uri);
        assertNotNull(report);

        final int[] expectedDurations = { 894, 1508, 1384, 1581, 996 };
        for (int i = 0; i < expectedDurations.length; i++) {
            final HttpSample sample = report.getHttpSampleList().get(i);
            assertEquals(expectedDurations[i], sample.getDuration());
        }
    }

    @Test
    public void testPerformanceReportJUnit() throws IOException {
        final PerformanceReport performanceReport = parseOneJUnit(new File(
                "src/test/resources/TEST-JUnitResults.xml"));
        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(5, uriReportMap.size());
        final String firstUri = "testGetMin";
        final UriReport firstUriReport = uriReportMap.get(firstUri);
        final HttpSample firstHttpSample = firstUriReport.getHttpSampleList().get(0);
        assertEquals(firstUri, firstHttpSample.getUri());
        assertEquals(31, firstHttpSample.getDuration());
        assertEquals(new Date(0L), firstHttpSample.getDate());
        assertTrue(firstHttpSample.isSuccessful());
        final String lastUri = "testGetMax";
        final UriReport secondUriReport = uriReportMap.get(lastUri);
        final HttpSample secondHttpSample = secondUriReport.getHttpSampleList()
                .get(0);
        assertEquals(lastUri, secondHttpSample.getUri());
        assertEquals(26, secondHttpSample.getDuration());
        assertEquals(new Date(0L), secondHttpSample.getDate());
        assertFalse(secondHttpSample.isSuccessful());
    }

    @Test
    public void testIssue5571() throws IOException {
        final PerformanceReport performanceReport = parseOneJUnit(new File(
                "src/test/resources/jUnitIssue5571.xml"));
        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(1, uriReportMap.size());
        final String uri = "unknown";
        final UriReport report = uriReportMap.get(uri);
        final HttpSample firstHttpSample = report.getHttpSampleList().get(0);
        assertEquals(uri, firstHttpSample.getUri());
        assertEquals(890, firstHttpSample.getDuration());
        assertEquals(new Date(0L), firstHttpSample.getDate());
        assertTrue(firstHttpSample.isSuccessful());

        final HttpSample secondHttpSample = report.getHttpSampleList().get(1);
        assertEquals(uri, secondHttpSample.getUri());
        assertEquals(50, secondHttpSample.getDuration());
        assertEquals(new Date(0L), secondHttpSample.getDate());
        assertTrue(secondHttpSample.isSuccessful());

        assertEquals(33, report.getMedian());
    }

    @Test
    public void testPerformanceReportMultiLevel() throws IOException {
        final PerformanceReport performanceReport = parseOneJMeter(new File(
                "src/test/resources/JMeterResultsMultiLevel.jtl"));
        final Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(2, uriReportMap.size());
        final UriReport report = uriReportMap.get("Home");
        assertNotNull(report);
    }
}
