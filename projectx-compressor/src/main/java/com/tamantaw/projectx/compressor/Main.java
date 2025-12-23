package com.tamantaw.projectx.compressor;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public final class Main {

	private static final Logger log = LogManager.getLogger(Main.class);

	private static final String STATIC_PATH = "src/main/resources/static";

	private Main() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			throw new IllegalArgumentException(
					"Usage: java -jar projectx-compressor.jar <project-root> <module-name>"
			);
		}

		Path projectRoot = Paths.get(args[0]).toAbsolutePath().normalize();
		String moduleName = args[1];

		Path moduleRoot = projectRoot.resolve(moduleName);
		Path sourceRoot = moduleRoot.resolve(STATIC_PATH);

		// ✅ output inside module target
		Path outputRoot = moduleRoot.resolve("target/static");

		log.info("==================================================");
		log.info(" Static Minify (ONLY .min for css/js)");
		log.info(" Module   : {}", moduleName);
		log.info(" Source   : {}", sourceRoot);
		log.info(" Output   : {}", outputRoot);
		log.info("==================================================");

		if (!Files.isDirectory(sourceRoot)) {
			throw new IllegalStateException("Static directory not found: " + sourceRoot);
		}

		// Clean output
		FileUtils.deleteQuietly(outputRoot.toFile());
		Files.createDirectories(outputRoot);

		// ✅ 1) Copy non-css/js assets only
		copyNonCssJs(sourceRoot, outputRoot);

		// ✅ 2) Produce ONLY min css/js into output
		produceMinCssJsOnly(sourceRoot, outputRoot);

		log.info("Done. Output contains only: non-css/js + *.min.css + *.min.js");
	}

	private static void copyNonCssJs(Path sourceRoot, Path outputRoot) throws IOException {
		try (Stream<Path> paths = Files.walk(sourceRoot)) {
			paths.filter(Files::isRegularFile)
					.filter(p -> !endsWithIgnoreCase(p, ".css"))
					.filter(p -> !endsWithIgnoreCase(p, ".js"))
					.forEach(p -> copyFile(sourceRoot, outputRoot, p));
		}
		log.info("Copied non-css/js assets.");
	}

	private static void produceMinCssJsOnly(Path sourceRoot, Path outputRoot) throws IOException {
		try (Stream<Path> paths = Files.walk(sourceRoot)) {
			paths.filter(Files::isRegularFile)
					.filter(p -> endsWithIgnoreCase(p, ".css") || endsWithIgnoreCase(p, ".js"))
					.forEach(p -> {
						// If already minified => copy as-is
						if (endsWithIgnoreCase(p, ".min.css") || endsWithIgnoreCase(p, ".min.js")) {
							copyFile(sourceRoot, outputRoot, p);
							return;
						}
						// Otherwise => generate .min.*
						runEsbuildToMin(sourceRoot, outputRoot, p);
					});
		}
		log.info("Generated/collected .min.css/.min.js only (no original css/js copied).");
	}

	private static void runEsbuildToMin(Path sourceRoot, Path outputRoot, Path inputFile) {
		try {
			Path relative = sourceRoot.relativize(inputFile);
			String rel = relative.toString();

			String minRel;
			if (endsWithIgnoreCase(inputFile, ".js")) {
				minRel = rel.substring(0, rel.length() - 3) + ".min.js";
			}
			else if (endsWithIgnoreCase(inputFile, ".css")) {
				minRel = rel.substring(0, rel.length() - 4) + ".min.css";
			}
			else {
				return;
			}

			Path target = outputRoot.resolve(minRel);
			Files.createDirectories(target.getParent());

			ProcessBuilder pb = new ProcessBuilder(
					"cmd", "/c",
					"npx", "esbuild",
					inputFile.toAbsolutePath().toString(),
					"--minify",
					"--outfile=" + target.toAbsolutePath()
			);

			pb.inheritIO();
			Process process = pb.start();
			int exit = process.waitFor();

			if (exit != 0) {
				throw new RuntimeException("esbuild failed for " + inputFile);
			}

			log.info("Minified: {} -> {}", relative, outputRoot.relativize(target));
		}
		catch (Exception e) {
			throw new RuntimeException("Minification failed for " + inputFile, e);
		}
	}

	private static void copyFile(Path sourceRoot, Path outputRoot, Path file) {
		try {
			Path relative = sourceRoot.relativize(file);
			Path target = outputRoot.resolve(relative);

			Files.createDirectories(target.getParent());
			Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed copying file: " + file, e);
		}
	}

	private static boolean endsWithIgnoreCase(Path p, String suffix) {
		return p.toString().toLowerCase().endsWith(suffix.toLowerCase());
	}
}
