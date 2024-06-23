package dev.dkaz.pushshift.extract.filter;

public class NoFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        return new NoFilter();
    };

    @Override
    public boolean isAllowed(String line) {
        return true;
    }

    @Override
    public void close() {
    }
}
