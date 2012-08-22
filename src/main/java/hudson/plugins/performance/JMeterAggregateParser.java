package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

public class JMeterAggregateParser extends PerformanceReportParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JMeterAggregate";
        }
    }

    @DataBoundConstructor
    public JMeterAggregateParser(String glob) {
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
        final Pattern lineDelimeterPattern = Pattern.compile(",");
        for (final File f : reports) {
            try {
                final AggregatePerformanceReport aggregateReport = new AggregatePerformanceReport();
                aggregateReport.setReportFileName(f.getName());
                logger.println("Performance: Parsing JMeterAggregate report file " + f.getName());

                final Scanner s = new Scanner(f);
                String line;
                while (s.hasNextLine()) {
                    line = s.nextLine();

                    if (!line.startsWith("sampler_label")) {
                        final boolean isTotal = line.startsWith("TOTAL");

                        final Scanner scanner = new Scanner(line);
                        scanner.useDelimiter(lineDelimeterPattern);

                        final String uri = scanner.next();

                        final int samplesCount = scanner.nextInt();
                        final long averageTime = scanner.nextLong();
                        final long aggregateMedian = scanner.nextLong();
                        final long aggregate90Percentile = scanner.nextLong();
                        final long minTime = scanner.nextLong();
                        final long maxTime = scanner.nextLong();
                        final float errorPercentage = Float.valueOf(scanner.next());

                        if (isTotal) {
                            aggregateReport.setSamplesCount(samplesCount);
                            aggregateReport.setAverageTime(averageTime);
                            aggregateReport.setAggregateMedian(aggregateMedian);
                            aggregateReport.setAggregate90Percentile(aggregate90Percentile);
                            aggregateReport.setMinTime(minTime);
                            aggregateReport.setMaxTime(maxTime);
                            aggregateReport.setErrorPercentage(errorPercentage);
                        } else {
                            final AggregateUriReport uriReport = new AggregateUriReport(aggregateReport, uri);
                            uriReport.setSamplesCount(samplesCount);
                            uriReport.setAverageTime(averageTime);
                            uriReport.setAggregateMedian(aggregateMedian);
                            uriReport.setAggregate90Percentile(aggregate90Percentile);
                            uriReport.setMinTime(minTime);
                            uriReport.setMaxTime(maxTime);
                            uriReport.setErrorPercentage(errorPercentage);
                            aggregateReport.addUriReport(uriReport);
                        }
                    }
                }

                result.add(aggregateReport);
            } catch (final FileNotFoundException e) {
                logger.println("File not found" + e.getMessage());
            }
        }

        return result;

    }

}
