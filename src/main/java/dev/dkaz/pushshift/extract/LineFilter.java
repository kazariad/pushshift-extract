package dev.dkaz.pushshift.extract;

import java.util.concurrent.Future;

public class LineFilter implements Runnable {
    private final String line;
    private final Future<?> previousFuture;

    public LineFilter(String line, Future<?> previousFuture) {
        this.line = line;
        this.previousFuture = previousFuture;
    }

    @Override
    public void run() {
        try {
            if (line == LineReader.EOF || Args.regex.matcher(line).matches()) {
                previousFuture.get();
                LineWriter.WRITER_QUEUE.put(line);
            } else {
                previousFuture.get();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
