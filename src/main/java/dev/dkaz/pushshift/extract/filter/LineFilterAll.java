package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Main;
import dev.dkaz.pushshift.extract.ProgressMonitor;

public class LineFilterAll implements Runnable {
    @Override
    public void run() {
        LineEntry lineEntry = null;
        while (true) {
            try {
                lineEntry = Main.FILTER_QUEUE.take();

                lineEntry.getPreviousFuture().get();
                Main.WRITER_QUEUE.put(lineEntry.getLine());

                if (lineEntry.getLine() == Main.EOF) {
                    Main.FILTER_THREAD_GROUP.interrupt();
                    return;
                } else {
                    ProgressMonitor.numMatchedLines.incrementAndGet();
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
