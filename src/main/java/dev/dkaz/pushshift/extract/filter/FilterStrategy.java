package dev.dkaz.pushshift.extract.filter;

public interface FilterStrategy extends AutoCloseable {
    public boolean isAllowed(String line);

    public void close();
}
