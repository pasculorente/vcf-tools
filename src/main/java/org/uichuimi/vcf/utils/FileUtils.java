package org.uichuimi.vcf.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class FileUtils {

	public static BufferedReader getBufferedReader(File file) throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(file)));
	}

	public static InputStream getInputStream(File file) throws IOException {
		if (file.getName().endsWith(".zip")) {
			return new ZipInputStream(new FileInputStream(file));
		} else if (file.getName().endsWith(".gz")) {
			return new GZIPInputStream(new FileInputStream(file));
		} else return new FileInputStream(file);
	}

	public static long countLines(File file) {
		try (final BufferedReader reader = getBufferedReader(file)) {
			return reader.lines().count();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
