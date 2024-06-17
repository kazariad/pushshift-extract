package dev.dkaz.pushshift.extract;

import com.github.luben.zstd.ZstdBufferDecompressingStream;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class LineReader implements Runnable {
    // create a unique, non-cached instance for equality comparison (==)
    public static final String EOF = new String("EOF");

    @Override
    public void run() {
        ByteBuffer inBuf = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer zstBuf = ByteBuffer.allocate(1024 * 1024 * 16);
        // wrap instead of allocating so we can access the array directly
        CharBuffer chBuf = CharBuffer.wrap(new char[1024 * 1024 * 16]);

        CharsetDecoder csd = StandardCharsets.UTF_8.newDecoder();
        StringBuilder sb = new StringBuilder();

        // use a chain of futures to write filtered lines in correct order
        Future<?> future = new CompletableFuture<>();
        ((CompletableFuture<?>) future).complete(null);

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
                        String line = sb.toString();
                        future = Main.EXECUTOR_SERVICE.submit(new LineFilter(line, future));
                        sb.setLength(0);
                        j = i;
                    }
                }
                chBuf.clear();
            }

            if (!sb.isEmpty()) {
                String line = sb.toString();
                future = Main.EXECUTOR_SERVICE.submit(new LineFilter(line, future));
            }

            Main.EXECUTOR_SERVICE.submit(new LineFilter(EOF, future));
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}
