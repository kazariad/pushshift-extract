package dev.dkaz.pushshift.extract;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Args {
    public static Path inputPath;
    public static Path outputPath;
    public static Pattern regex;

    public static void parse(String[] args) {
        if (args.length < 2 || args.length > 6 || args.length % 2 == 1) {
            invalidArgsExit();
        }

        for (int i = 0; i < args.length; ) {
            switch (args[i++].toLowerCase()) {
                case "-i":
                    inputPath = Paths.get(args[i++]);
                    break;
                case "-o":
                    outputPath = Paths.get(args[i++]);
                    break;
                case "-r":
                    regex = Pattern.compile(args[i++]);
                    break;
                default:
                    invalidArgsExit();
            }
        }

        if (inputPath == null) {
            invalidArgsExit();
        }

        if (outputPath == null) {
            outputPath = inputPath.resolveSibling(inputPath.getFileName().toString().split("\\.")[0] + ".ndjson");
        }

        if (regex == null) {
            regex = Pattern.compile(".*");
        }
    }

    private static void invalidArgsExit() {
        System.err.println("Invalid args. Usage: -i inputPath [-o outputPath] [-r lineFilterRegex]");
        System.exit(1);
    }
}
