package dev.dkaz.pushshift.extract;

import com.github.luben.zstd.ZstdBufferDecompressingStream;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class LineReader implements Runnable {
    @Override
    public void run() {
        ByteBuffer inBuf = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer zstBuf = ByteBuffer.allocate(1024 * 1024 * 16);
        // wrap instead of allocating so we can access the array directly
        CharBuffer chBuf = CharBuffer.wrap(new char[1024 * 1024 * 16]);

        CharsetDecoder csd = StandardCharsets.UTF_8.newDecoder();
        StringBuilder sb = new StringBuilder();

        // use a chain of futures to write filtered lines in correct order
        CompletableFuture<Void> currentFuture = new CompletableFuture<>();
        currentFuture.complete(null);

        try (
                FileChannel inChannel = FileChannel.open(Args.inputPath, StandardOpenOption.READ);
                ZstdBufferDecompressingStream zstd = new ZstdBufferDecompressingStream(inBuf);
        ) {
            ProgressMonitor.fileSize.set(inChannel.size());

            while (true) {
                int numBytesRead = inChannel.read(inBuf);
                if (numBytesRead == -1 && inBuf.position() == 0 && zstBuf.position() == 0) break;
                if (numBytesRead > 0) ProgressMonitor.numBytesRead.addAndGet(numBytesRead);

                inBuf.flip();
                zstd.read(zstBuf);
                inBuf.compact();

                zstBuf.flip();
                csd.decode(zstBuf, chBuf, numBytesRead == -1 && inBuf.position() == 0);
                // zstBuf may not be fully processed if a multi-byte UTF-8 character was split across the buffer boundary
                // and couldn't be decoded, i.e. there can be up to 3 bytes remaining
                zstBuf.compact();

                chBuf.flip();
                for (int i = 0, j = 0; true; ) {
                    if (i == chBuf.limit()) {
                        sb.append(chBuf.array(), j, i - j);
                        break;
                    }

                    if (chBuf.get(i++) == '\n') {
                        sb.append(chBuf.array(), j, i - j - 1);
                        j = i;
                        String line = sb.toString();
                        sb.setLength(0);

                        LineEntry lineEntry = new LineEntry(line, currentFuture);
                        currentFuture = lineEntry.getCurrentFuture();
                        Main.FILTER_QUEUE.put(lineEntry);
                    }
                }
                chBuf.clear();
            }

            if (!sb.isEmpty()) {
                String line = sb.toString();
                LineEntry lineEntry = new LineEntry(line, currentFuture);
                currentFuture = lineEntry.getCurrentFuture();
                Main.FILTER_QUEUE.put(lineEntry);
            }

            Main.FILTER_QUEUE.put(new LineEntry(Main.EOF, currentFuture));
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}
