package dev.dkaz.pushshift.extract.filter;

public interface FilterStrategy extends AutoCloseable {
    // true to allow, false to deny
    public boolean filterLine(String line);

    public void close();
}
