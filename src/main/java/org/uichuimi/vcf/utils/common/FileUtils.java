package org.uichuimi.vcf.utils.common;

import java.io.*;
import java.util.zip.*;

public class FileUtils {

	public static BufferedReader getBufferedReader(File file) throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(file)));
	}

	public static InputStream getInputStream(File file) throws IOException {
		if (file.getName().endsWith(".zip")) {
			final ZipFile zipFile = new ZipFile(file);
			final ZipEntry zipEntry = zipFile.entries().nextElement();
			return zipFile.getInputStream(zipEntry);
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

	public static OutputStream getOutputStream(File file) throws IOException {
		if (file.getName().endsWith(".zip")) {
			return new ZipOutputStream(new FileOutputStream(file));
		} else if (file.getName().endsWith(".gz")) {
			return new GZIPOutputStream(new FileOutputStream(file));
		} else return new FileOutputStream(file);
	}
}
