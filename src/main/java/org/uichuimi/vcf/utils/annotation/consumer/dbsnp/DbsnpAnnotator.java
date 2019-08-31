package org.uichuimi.vcf.utils.annotation.consumer.dbsnp;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.FileUtils;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;

public class DbsnpAnnotator implements VariantConsumer {

	private final VariantReader reader;

	public DbsnpAnnotator(File dbsnp) throws IOException {
		reader = new VariantReader(FileUtils.getInputStream(dbsnp), Chromosome.Namespace.REFSEQ);
	}

	@Override
	public void start(VcfHeader header) {

	}

	@Override
	public void accept(Variant variant) {
		final Variant dbsnp = reader.next(variant.getCoordinate());
		if (dbsnp == null) return;
		variant.getIdentifiers().clear();
		variant.getIdentifiers().add(dbsnp.getIdentifiers().get(0));
	}

	@Override
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
