package org.uichuimi.vcf.utils.annotation;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class VcfAnnotator implements VariantConsumer {

	private final VariantReader reader;
	private final List<ColumnSpec> columnSpecs;

	public VcfAnnotator(File file, List<ColumnSpec> columnSpecs) throws IOException {
		this(FileUtils.getInputStream(file), columnSpecs);
	}

	public VcfAnnotator(InputStream inputStream, List<ColumnSpec> columnSpecs) throws IOException {
		reader = new VariantReader(inputStream);
		this.columnSpecs = columnSpecs;
	}


	@Override
	public void start(VcfHeader header) {
		// Clone header lines
		for (ColumnSpec spec : columnSpecs) {
			final InfoHeaderLine infoHeader = reader.getHeader().getInfoHeader(spec.getSourceColumn());
			if (infoHeader != null) {
				final InfoHeaderLine targetInfo = new InfoHeaderLine(spec.getTargetColumn(), infoHeader.getNumber(), infoHeader.getTypeName(), infoHeader.getDescription());
				header.addHeaderLine(targetInfo);
			}
		}
		System.out.println("Header processed");
	}

	@Override
	public void accept(Variant variant) {
		final Variant sourceVariant = reader.next(variant.getCoordinate());
		if (sourceVariant == null) return;
		for (ColumnSpec spec : columnSpecs) {
			final Object info = sourceVariant.getInfo(spec.getSourceColumn());
			if (info != null) variant.setInfo(spec.getTargetColumn(), info);
		}
	}

	@Override
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<ColumnSpec> getColumnSpecs() {
		return columnSpecs;
	}
}
