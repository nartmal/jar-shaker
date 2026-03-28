package com.github.narmtal.jarshaker.testing;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds an in-memory JAR from class definitions declared in code.
 *
 * <p>Classes are defined using a fluent builder, compiled in-memory via {@code javax.tools},
 * and packed into a JAR byte array — no temp files, no subprocesses.
 *
 * <pre>{@code
 * InMemoryJar jar = InMemoryJar.builder()
 *     .addClass("com.example.App")
 *         .field("com.example.Service", "service")
 *         .done()
 *     .addClass("com.example.Service")
 *         .done()
 *     .build();
 *
 * ReachabilityAnalyzer.analyze(List.of(jar.toInputStream()), List.of());
 * }</pre>
 */
public final class InMemoryJar {

    private final byte[] bytes;

    private InMemoryJar(byte[] bytes) {
        this.bytes = bytes;
    }

    public InputStream toInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {
        private final List<ClassSpec> specs = new ArrayList<>();

        public ClassBuilder addClass(String fullyQualifiedName) {
            return new ClassBuilder(this, fullyQualifiedName);
        }

        Builder registerClass(ClassSpec spec) {
            specs.add(spec);
            return this;
        }

        public InMemoryJar build() throws IOException {
            Map<String, JavaFile> sources = new LinkedHashMap<>();
            for (ClassSpec spec : specs) {
                sources.put(spec.fqn(), spec.toJavaFile());
            }
            return new InMemoryJar(packJar(compile(sources)));
        }
    }

    public static final class ClassBuilder {
        private final Builder parent;
        private final String fqn;
        private final List<FieldSpec> fields = new ArrayList<>();

        ClassBuilder(Builder parent, String fqn) {
            this.parent = parent;
            this.fqn = fqn;
        }

        /** Adds a field of the given type, creating a bytecode-level dependency on that type. */
        public ClassBuilder field(String typeName, String fieldName) {
            fields.add(FieldSpec.builder(ClassName.bestGuess(typeName), fieldName).build());
            return this;
        }

        public Builder done() {
            return parent.registerClass(new ClassSpec(fqn, List.copyOf(fields)));
        }
    }

    // -------------------------------------------------------------------------
    // Internal model
    // -------------------------------------------------------------------------

    record ClassSpec(String fqn, List<FieldSpec> fields) {
        JavaFile toJavaFile() {
            int lastDot = fqn.lastIndexOf('.');
            String pkg    = lastDot >= 0 ? fqn.substring(0, lastDot) : "";
            String simple = lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;

            TypeSpec.Builder type = TypeSpec.classBuilder(simple).addModifiers(Modifier.PUBLIC);
            fields.forEach(type::addField);

            return JavaFile.builder(pkg, type.build()).build();
        }
    }

    // -------------------------------------------------------------------------
    // Compilation
    // -------------------------------------------------------------------------

    private static Map<String, byte[]> compile(Map<String, JavaFile> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "javax.tools.JavaCompiler not available — tests must run with a JDK, not a JRE.");
        }

        List<JavaFileObject> sourceFiles = new ArrayList<>();
        for (Map.Entry<String, JavaFile> entry : sources.entrySet()) {
            StringWriter sw = new StringWriter();
            entry.getValue().writeTo(sw);
            sourceFiles.add(new StringSource(entry.getKey(), sw.toString()));
        }

        Map<String, byte[]> output = new LinkedHashMap<>();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager std = compiler.getStandardFileManager(null, null, null);
             MemoryFileManager fm = new MemoryFileManager(std, output)) {

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fm, diagnostics, null, null, sourceFiles);

            if (!task.call()) {
                StringBuilder msg = new StringBuilder("Compilation failed:\n");
                diagnostics.getDiagnostics().forEach(d -> msg.append("  ").append(d).append('\n'));
                throw new IllegalStateException(msg.toString());
            }
        }

        return output;
    }

    // -------------------------------------------------------------------------
    // JAR packing
    // -------------------------------------------------------------------------

    private static byte[] packJar(Map<String, byte[]> classes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // javax.tools plumbing
    // -------------------------------------------------------------------------

    private static final class StringSource extends SimpleJavaFileObject {
        private final String src;

        StringSource(String className, String src) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.src = src;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return src;
        }
    }

    private static final class MemoryFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final Map<String, byte[]> output;

        MemoryFileManager(StandardJavaFileManager delegate, Map<String, byte[]> output) {
            super(delegate);
            this.output = output;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            return new ClassSink(className, output);
        }
    }

    private static final class ClassSink extends SimpleJavaFileObject {
        private final String className;
        private final Map<String, byte[]> output;

        ClassSink(String className, Map<String, byte[]> output) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension),
                    Kind.CLASS);
            this.className = className;
            this.output = output;
        }

        @Override
        public OutputStream openOutputStream() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return new FilterOutputStream(baos) {
                @Override
                public void close() throws IOException {
                    super.close();
                    output.put(className, baos.toByteArray());
                }
            };
        }
    }
}
