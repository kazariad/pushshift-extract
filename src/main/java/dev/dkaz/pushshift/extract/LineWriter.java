package dev.dkaz.pushshift.extract;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class LineWriter implements Runnable {
    @Override
    public void run() {
        try {
            ByteBuffer outBuf = ByteBuffer.allocate(1024 * 1024 * 16);
            byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
            boolean isFirstLine = true;

            try (FileChannel outChannel = FileChannel.open(Args.outputPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                for (String line = Main.WRITER_QUEUE.take(); line != Main.EOF; line = Main.WRITER_QUEUE.take()) {
                    if (isFirstLine) {
                        isFirstLine = false;
                    } else {
                        outBuf.put(LF);
                    }
                    outBuf.put(line.getBytes(StandardCharsets.UTF_8));

                    outBuf.flip();
                    while (outBuf.hasRemaining()) {
                        outChannel.write(outBuf);
                    }
                    outBuf.clear();
                }
            }

            if (isFirstLine) Files.delete(Args.outputPath);

            ProgressMonitor.isFinished.set(true);
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}
