package com.zerogc.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileLogWriter extends BaseLogWriter {
    private String name;

    public FileLogWriter(String name) {
        this.name = name;
        this.immediateFlush = true;
    }

    @Override
    public void open() throws IOException {
        // Keep some history
        for (int i=9; i>0;) {
            Path to = Paths.get(name + "_" + i + ".log");
            i--;
            Path from = Paths.get(name + (i > 0 ? "_" + i : "") + ".log");
            try {
                if (Files.exists(to)) {
                    Files.delete(to);
                }
                Files.move(from, to);
            } catch (IOException e)
            {
            }
        }
        stream = new FileOutputStream(name + ".log");
    }
}
