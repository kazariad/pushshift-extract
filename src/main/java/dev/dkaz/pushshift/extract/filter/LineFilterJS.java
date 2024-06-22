package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import dev.dkaz.pushshift.extract.ProgressMonitor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class LineFilterJS implements Runnable {
    @Override
    public void run() {
        try (Context context = Context.newBuilder("js").engine(Main.ENGINE).allowAllAccess(true).build()) {
            Value bindings = context.getBindings("js");
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

                    bindings.putMember("line", lineEntry.getLine());
                    bindings.putMember("isMatch", Boolean.FALSE);
                    // https://github.com/oracle/graal/issues/5071
                    context.eval(Args.jsScript);
                    Value isMatch = bindings.getMember("isMatch");

                    if (isMatch.asBoolean()) {
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
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}
