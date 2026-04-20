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

        String jobId = UUID.randomUUID().toString();
        Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "judge_" + jobId);

        try {
            Files.createDirectories(workDir);
            Path sourceFile = workDir.resolve("Main.java");
            Files.writeString(sourceFile, req.code);


            ProcessBuilder compileBuilder = new ProcessBuilder("javac", "Main.java")
                .directory(workDir.toFile())  
                .redirectErrorStream(false);

            Process compile = compileBuilder.start();

          
            String compileErrors = new String(compile.getErrorStream().readAllBytes());

            
            boolean compiledInTime = compile.waitFor(10, TimeUnit.SECONDS);
            if (!compiledInTime) {
                compile.destroyForcibly();
                return "Error: Compilation timed out.";
            }
            if (!compileErrors.isEmpty()) {
                return compileErrors;
            }

          
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", workDir.toString(), "Main")
                .directory(workDir.toFile());  

            Process run = runBuilder.start();

       \
            try (BufferedWriter stdin = new BufferedWriter(
                    new OutputStreamWriter(run.getOutputStream()))) {
                if (req.input != null && !req.input.isEmpty()) {
                    stdin.write(req.input);
                    stdin.newLine();
                }
            } 
            String stdout = new String(run.getInputStream().readAllBytes());
            String stderr = new String(run.getErrorStream().readAllBytes());

 
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
            try {
                Files.walk(workDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
            } catch (IOException ignored) {}
        }
}
}
