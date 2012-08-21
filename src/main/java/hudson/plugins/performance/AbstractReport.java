package hudson.plugins.performance;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.kohsuke.stapler.Stapler;

/**
 * Abstract class for classes with size, error, mean, average, 90 line, min and
 * max attributes
 */
public abstract class AbstractReport {

    private NumberFormat percentFormat;
    private NumberFormat dataFormat;

    abstract public int countErrors();

    abstract public double errorPercent();

    public AbstractReport() {
        if (Stapler.getCurrentRequest() != null) {
            Locale.setDefault(Stapler.getCurrentRequest().getLocale());
        }
        percentFormat = new DecimalFormat("0.0");
        dataFormat = new DecimalFormat("#,###");
    }

    public String errorPercentFormated() {
        Stapler.getCurrentRequest().getLocale();
        return percentFormat.format(errorPercent());
    }

    abstract public long getAverage();

    public String getAverageFormated() {
        return dataFormat.format(getAverage());
    }

    abstract public long getMedian();

    public String getMeanFormated() {
        return dataFormat.format(getMedian());
    }

    abstract public long get90Line();

    public String get90LineFormated() {
        return dataFormat.format(get90Line());
    }

    public abstract long getPercentileLine(int percentile);

    public String getPercentileLineFormated(int percentile) {
        return dataFormat.format(getPercentileLine(percentile));
    }

    abstract public long getMax();

    public String getMaxFormated() {
        return dataFormat.format(getMax());
    }

    abstract public long getMin();

    abstract public int size();

    abstract public String getHttpCode();

    abstract public long getAverageDiff();

    abstract public long getMedianDiff();

    abstract public double getErrorPercentDiff();

    abstract public String getLastBuildHttpCodeIfChanged();

    abstract public int getSizeDiff();
}
