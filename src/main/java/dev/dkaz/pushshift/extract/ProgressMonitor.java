package dev.dkaz.pushshift.extract;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressMonitor implements Runnable {
    public static final AtomicLong fileSize = new AtomicLong();
    public static final AtomicLong numBytesRead = new AtomicLong();
    public static final AtomicLong numMatchedLines = new AtomicLong();
    public static final AtomicLong numFailedLines = new AtomicLong();
    public static final AtomicBoolean isFinished = new AtomicBoolean(false);

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (true) {
            long _elapsedTime = System.currentTimeMillis() - startTime;
            String elapsedTime = String.format("%02d:%02d", _elapsedTime / 60000L, (_elapsedTime % 60000L) / 1000L);

            long _fileSize = fileSize.get();
            long _bytesRead = numBytesRead.get();
            String progress = String.format("%s/%s", humanReadableByteCountBin(_bytesRead), humanReadableByteCountBin(_fileSize));
            String percentage = String.format("%.1f%%", _fileSize == 0 ? 0 : (double) _bytesRead / _fileSize * 100.0);

            String matchedLines = String.format("%,d matched lines", numMatchedLines.get());
            String failedLines = String.format("%,d failed lines", numFailedLines.get());

            String status = String.format("%s  %s (%s)  %s  %s", elapsedTime, progress, percentage, matchedLines, failedLines);
            // use carriage return to overwrite the same line and emulate a live progress indicator
            System.out.printf("\r%-100s", status);

            if (isFinished.get()) return;

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
        }
    }

    // https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    private static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
}
