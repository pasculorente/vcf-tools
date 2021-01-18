package org.uichuimi.vcf.utils.common;

import htsjdk.samtools.util.BlockCompressedOutputStream;

import java.io.*;
import java.util.zip.*;

public class FileUtils {

	/**
	 * Interprets the file type by its extension and generates the proper BufferedInputStream.
	 * Currently, it can open text files and gzipped and zipped text files.
	 *
	 * @param file file to read
	 * @throws IOException if file does not exist or is not readable
	 */
	public static BufferedReader getBufferedReader(File file) throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(file)));
	}

	public static BufferedWriter getBufferedWriter(File file) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(getOutputStream(file)));
	}

	/**
	 * Interprets the file type by its extension and generates the proper InputStream. Currently,
	 * it can open text files and gzipped and zipped text files.
	 *
	 * @param file file to read
	 * @throws IOException if file does not exist or is not readable
	 */
	public static InputStream getInputStream(File file) throws IOException {
		if (file.getName().endsWith(".zip")) {
			final ZipFile zipFile = new ZipFile(file);
			final ZipEntry zipEntry = zipFile.entries().nextElement();
			return zipFile.getInputStream(zipEntry);
		} else if (file.getName().endsWith(".gz") || file.getName().endsWith(".bgz")) {
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

	/**
	 * Interprets the file type by its extension and generates the proper OutputStream. Currently,
	 * it can open text files and gzipped and zipped text files.
	 *
	 * @param file file to write
	 * @throws IOException if file does not exist or is not writable
	 */
	public static OutputStream getOutputStream(File file) throws IOException {
		if (file.getName().endsWith(".zip")) {
			return new ZipOutputStream(new FileOutputStream(file));
		} else if (file.getName().endsWith(".vcf.gz")) {
			return new BlockCompressedOutputStream(file);
		} else if (file.getName().endsWith(".gz")) {
			return new GZIPOutputStream(new FileOutputStream(file));
		} else return new FileOutputStream(file);
	}
}
