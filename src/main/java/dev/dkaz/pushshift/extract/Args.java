package dev.dkaz.pushshift.extract;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Args {
    public static Path inputPath;
    public static Path outputPath;
    public static Source jsScript;
    public static Source pyScript;
    public static Pattern regex;

    public static void parse(String[] args) throws Exception {
        if (args.length < 2 || args.length > 6 || args.length % 2 == 1) {
            invalidArgsExit();
        }

        for (int i = 0; i < args.length; ) {
            switch (args[i++].toLowerCase()) {
                case "-i": {
                    if (inputPath != null) invalidArgsExit();
                    inputPath = Paths.get(args[i++]);
                    break;
                }
                case "-o": {
                    if (outputPath != null) invalidArgsExit();
                    outputPath = Paths.get(args[i++]);
                    break;
                }
                case "-j": {
                    if (jsScript != null) invalidArgsExit();
                    String script = Files.readString(Paths.get(args[i++]));
                    jsScript = Source.create("js", String.format("(function outer(){%s})", script));
                    // validation
                    try (Context context = Context.newBuilder("js").engine(Main.ENGINE).allowAllAccess(true).build()) {
                        context.parse(jsScript);
                    }
                    break;
                }
                case "-p": {
                    if (pyScript != null) invalidArgsExit();
                    pyScript = Source.newBuilder("python", Paths.get(args[i++]).toFile()).build();
                    // validation
                    try (Context context = Context.newBuilder("python").engine(Main.ENGINE).allowAllAccess(true).build()) {
                        context.parse(pyScript);
                    }
                    break;
                }
                case "-r": {
                    if (regex != null) invalidArgsExit();
                    regex = Pattern.compile(args[i++]);
                    break;
                }
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

        if (Stream.of(jsScript, pyScript, regex).filter(Objects::nonNull).count() > 1) {
            invalidArgsExit();
        }
    }

    private static void invalidArgsExit() {
        System.err.println("Invalid args. Usage: -i inputPath [-o outputPath] [-j jsScriptPath | -p pyScriptPath | -r regex]");
        System.exit(1);
    }
}
