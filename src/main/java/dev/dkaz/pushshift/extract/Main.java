package dev.dkaz.pushshift.extract;

import dev.dkaz.pushshift.extract.filter.FilterFactory;
import dev.dkaz.pushshift.extract.filter.FilterRunner;
import dev.dkaz.pushshift.extract.filter.JavascriptFilter;
import dev.dkaz.pushshift.extract.filter.NoFilter;
import dev.dkaz.pushshift.extract.filter.PythonFilter;
import dev.dkaz.pushshift.extract.filter.RegexFilter;
import org.graalvm.polyglot.Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main {
    // create a unique, non-cached instance for equality comparison (==)
    public static final String EOF = new String("EOF");
    public static final ThreadGroup FILTER_THREAD_GROUP = new ThreadGroup("FILTER_THREAD_GROUP");
    public static final Engine ENGINE = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    public static BlockingQueue<LineEntry> FILTER_QUEUE;
    public static BlockingQueue<String> WRITER_QUEUE;

    public static void main(String[] args) {
        try {
            Args.parse(args);

            int numThreads = Runtime.getRuntime().availableProcessors();
            if (numThreads < 1) numThreads = 1;

            FILTER_QUEUE = new ArrayBlockingQueue<>(numThreads * 10);
            WRITER_QUEUE = new ArrayBlockingQueue<>(numThreads * 10);

            List<Thread> threads = new ArrayList<>();
            threads.add(new Thread(new LineReader()));

            FilterFactory filterFactory = null;
            if (Args.jsScript != null) {
                filterFactory = JavascriptFilter.FILTER_FACTORY;
            } else if (Args.pyScript != null) {
                filterFactory = PythonFilter.FILTER_FACTORY;
            } else if (Args.regex != null) {
                filterFactory = RegexFilter.FILTER_FACTORY;
            } else {
                filterFactory = NoFilter.FILTER_FACTORY;
            }
            for (int i = 0; i < numThreads; i++) {
                threads.add(new Thread(FILTER_THREAD_GROUP, new FilterRunner(filterFactory)));
            }

            threads.add(new Thread(new LineWriter()));
            threads.add(new Thread(new ProgressMonitor()));
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            ENGINE.close();
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}