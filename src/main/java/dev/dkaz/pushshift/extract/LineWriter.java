package dev.dkaz.pushshift.extract;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class LineWriter implements Runnable {
    public static final BlockingQueue<String> WRITER_QUEUE = new ArrayBlockingQueue<>(1000);

    @Override
    public void run() {
        ByteBuffer outBuf = ByteBuffer.allocate(1024 * 1024 * 16);
        byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
        boolean isFirstLine = true;

        try (FileChannel outChannel = FileChannel.open(Args.outputPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            for (String line = WRITER_QUEUE.take(); line != LineReader.EOF; line = WRITER_QUEUE.take()) {
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
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }

        ProgressMonitor.isFinished.set(true);
    }
}
