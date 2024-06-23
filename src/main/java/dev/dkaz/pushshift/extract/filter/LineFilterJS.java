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
            Value script = context.eval(Args.jsScript);

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
                    script.execute();
                    boolean isMatch = bindings.getMember("isMatch").asBoolean();

                    lineEntry.getPreviousFuture().get();
                    if (isMatch) {
                        Main.WRITER_QUEUE.put(lineEntry.getLine());
                        ProgressMonitor.numMatchedLines.incrementAndGet();
                    }
                } catch (InterruptedException ie) {
                    lineEntry = null;
                    return;
                } catch (Exception e) {
                    lineEntry.getPreviousFuture().get();
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
