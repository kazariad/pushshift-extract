package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;

public class RegexFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        return new RegexFilter();
    };

    @Override
    public boolean filterLine(String line) {
        return Args.regex.matcher(line).matches();
    }

    @Override
    public void close() {
    }
}
