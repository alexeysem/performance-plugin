package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

/**
 * Created by IntelliJ IDEA. User: Agoley Date: 06.02.2012 Time: 12:45:24 To
 * change this template use File | Settings | File Templates.
 */
public class JmeterSummarizerParser extends PerformanceReportParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JmeterSummarizer";
        }
    }

    @DataBoundConstructor
    public JmeterSummarizerParser(String glob) {
        super(glob);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.log";
    }

    @Override
    public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
            Collection<File> reports, TaskListener listener) {
        final List<PerformanceReport> result = new ArrayList<PerformanceReport>();

        final PrintStream logger = listener.getLogger();
        for (final File f : reports) {
            try {
                final PerformanceReport r = new PerformanceReport();
                r.setReportFileName(f.getName());
                logger.println("Performance: Parsing JMeterSummarizer report file " + f.getName());

                final Scanner s = new Scanner(f);
                final Map<String, HttpSample> map = new HashMap<String, HttpSample>();
                String key;
                String line;
                while (s.hasNextLine()) {
                    line = s.nextLine().replaceAll("=", " ");

                    if (!line.contains("+")) {
                        final Scanner scanner = new Scanner(line);
                        final HttpSample sample = new HttpSample();

                        // set Date !!!! stub. not Ffrom log
                        sample.setDate(new Date(Long.valueOf("1296876799179")));

                        scanner.findInLine("jmeter.reporters.Summariser:");
                        key = scanner.next();

                        // set SamplesCount
                        scanner.findInLine(key);
                        sample.setSummarizerSamples(scanner.nextLong());
                        // set response time
                        scanner.findInLine("Avg:");
                        sample.setDuration(scanner.nextLong());
                        sample.setSuccessful(true);
                        // set MIN
                        scanner.findInLine("Min:");
                        sample.setSummarizerMin(scanner.nextLong());
                        // set MAX
                        scanner.findInLine("Max:");
                        sample.setSummarizerMax(scanner.nextLong());
                        // set errors count
                        scanner.findInLine("Err:");
                        scanner.nextInt();
                        sample.setSummarizerErrors(Float.valueOf(scanner.next().replaceAll("[()%]", "")));
                        // sample.setSummarizerErrors(Long.valueOf(scanner.next()));

                        sample.setUri(key);
                        map.put(key, sample);
                    }
                }
                for (final String method : map.keySet()) {
                    r.addSample(map.get(method));
                }

                result.add(r);

            } catch (final FileNotFoundException e) {
                logger.println("File not found" + e.getMessage());
            } catch (final SAXException e) {
                logger.println(e.getMessage());
            }
        }

        return result;

    }

}
