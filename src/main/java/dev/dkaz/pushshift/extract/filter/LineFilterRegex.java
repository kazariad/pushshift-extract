package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import dev.dkaz.pushshift.extract.ProgressMonitor;

public class LineFilterRegex implements Runnable {
    @Override
    public void run() {
        LineEntry lineEntry = null;
        while (true) {
            try {
                lineEntry = Main.FILTER_QUEUE.take();

                if (lineEntry.getLine() == Main.EOF) {
                    lineEntry.getPreviousFuture().get();
                    Main.WRITER_QUEUE.put(Main.EOF);
                    Main.FILTER_THREAD_GROUP.interrupt();
                    return;
                }

                if (Args.regex.matcher(lineEntry.getLine()).matches()) {
                    lineEntry.getPreviousFuture().get();
                    Main.WRITER_QUEUE.put(lineEntry.getLine());
                    ProgressMonitor.numMatchedLines.incrementAndGet();
                } else {
                    lineEntry.getPreviousFuture().get();
                }
            } catch (InterruptedException ie) {
                lineEntry = null;
                return;
            } catch (Exception e) {
                ProgressMonitor.numFailedLines.incrementAndGet();
            } finally {
                if (lineEntry != null) lineEntry.getCurrentFuture().complete(null);
            }
        }
    }
}
