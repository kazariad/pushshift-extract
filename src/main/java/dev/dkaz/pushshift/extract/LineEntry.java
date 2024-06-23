package dev.dkaz.pushshift.extract;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class LineEntry {
    private final String line;

    private final Future<Void> previousFuture;

    private final CompletableFuture<Void> currentFuture;

    public LineEntry(String line, Future<Void> previousFuture) {
        this.line = line;
        this.previousFuture = previousFuture;
        this.currentFuture = new CompletableFuture<>();
    }

    public String getLine() {
        return line;
    }

    public Future<Void> getPreviousFuture() {
        return previousFuture;
    }

    public CompletableFuture<Void> getCurrentFuture() {
        return currentFuture;
    }
}
