package dev.dkaz.pushshift.extract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) {
        try {
            Args.parse(args);
            EXECUTOR_SERVICE.submit(new LineReader());
            EXECUTOR_SERVICE.submit(new LineWriter()).get();
            EXECUTOR_SERVICE.shutdown();
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}