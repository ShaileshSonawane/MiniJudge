package com.example.demo;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class CodeController {

    @PostMapping("/run")
    public String runCode(@RequestBody Request req) {
    	System.out.println("In Controller");
        // FIX 4: unique directory per request — no file collisions
        String jobId = UUID.randomUUID().toString();
        Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "judge_" + jobId);

        try {
            Files.createDirectories(workDir);
            Path sourceFile = workDir.resolve("Main.java");
            Files.writeString(sourceFile, req.code);

            // FIX 2: ProcessBuilder instead of deprecated exec(String)
            // COMPILE
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", "Main.java")
                .directory(workDir.toFile())   // FIX 6: explicit working directory
                .redirectErrorStream(false);

            Process compile = compileBuilder.start();

            // FIX 1: drain streams before waitFor to avoid deadlock
            String compileErrors = new String(compile.getErrorStream().readAllBytes());

            // FIX 3: timeout — compilation shouldn't take more than 10s
            boolean compiledInTime = compile.waitFor(10, TimeUnit.SECONDS);
            if (!compiledInTime) {
                compile.destroyForcibly();
                return "Error: Compilation timed out.";
            }
            if (!compileErrors.isEmpty()) {
                return compileErrors;
            }

            // RUN
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", workDir.toString(), "Main")
                .directory(workDir.toFile());  // FIX 6

            Process run = runBuilder.start();

            // Send input
            try (BufferedWriter stdin = new BufferedWriter(
                    new OutputStreamWriter(run.getOutputStream()))) {
                if (req.input != null && !req.input.isEmpty()) {
                    stdin.write(req.input);
                    stdin.newLine();
                }
            } // auto-closes, signals EOF to the running program

            // FIX 1: read ALL streams before waitFor
            String stdout = new String(run.getInputStream().readAllBytes());
            String stderr = new String(run.getErrorStream().readAllBytes());

            // FIX 3: timeout — kill infinite loops after 5s
            boolean finishedInTime = run.waitFor(5, TimeUnit.SECONDS);
            if (!finishedInTime) {
                run.destroyForcibly();
                return "Error: Time limit exceeded (5s).";
            }

            if (!stderr.isEmpty()) return stderr;
            return stdout.isEmpty() ? "(no output)" : stdout;

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "Error: " + sw.toString();
            } finally {
            // Cleanup temp directory after each run
            try {
                Files.walk(workDir)
                    .sorted((a, b) -> b.compareTo(a)) // files before dirs
                    .forEach(p -> p.toFile().delete());
            } catch (IOException ignored) {}
        }
}
}