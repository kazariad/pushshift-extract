package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Main;
import dev.dkaz.pushshift.extract.ProgressMonitor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

public class LineFilterJS implements Runnable {
    private static final String CODE = """
            (function myFun(line){
                const obj = JSON.parse(line);
                return obj.subreddit.toUpperCase() === 'NBA';
            })
            """;

    @Override
    public void run() {
        try (
                Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
                Context context = Context.newBuilder("js").engine(engine).build()
        ) {
            Value filterFunc = context.eval("js", CODE);

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

                    if (filterFunc.execute(lineEntry.getLine()).asBoolean()) {
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
