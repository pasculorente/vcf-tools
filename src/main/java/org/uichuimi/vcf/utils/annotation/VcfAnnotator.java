package org.uichuimi.vcf.utils.annotation;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
				// Copy header lines changing target name
				final InfoHeaderLine targetInfo = new InfoHeaderLine(spec.getTargetColumn(),
						infoHeader.getNumber(), infoHeader.getTypeName(), infoHeader.getDescription());
				header.addHeaderLine(targetInfo);
			}
		}
	}

	@Override
	public void accept(Variant variant) {
		// We expect more than one line per coordinate
		Variant sourceVariant;
		while ((sourceVariant = reader.next(variant.getCoordinate())) != null) {
			// REF should only contain one allele
			if (!sourceVariant.getReferences().get(0).equals(variant.getReferences().get(0))) continue;
			// Check alternative alleles match
			for (int a = 0; a < sourceVariant.getAlternatives().size(); a++) {
				final String allele = sourceVariant.getAlternatives().get(a);
				final int index = variant.getAlternatives().indexOf(allele);
				if (index >= 0) {
					for (ColumnSpec spec : columnSpecs) {
						final InfoHeaderLine info = sourceVariant.getHeader().getInfoHeader(spec.getSourceColumn());
						if (info.getNumber().equals("A")) {
							final Object val = sourceVariant.getInfo(spec.getSourceColumn());
							if (val != null) {
								// Val must be an array or list
								final List<?> valList = (List<?>) val;
								setAlleleInfo(variant, index, spec.getTargetColumn(), valList.get(a));
							}
						}
					}
				}
			}
			for (ColumnSpec spec : columnSpecs) {
				final InfoHeaderLine info = sourceVariant.getHeader().getInfoHeader(spec.getSourceColumn());
				if (info == null || info.getNumber().equals("0") || info.getNumber().equals("1")) {
					final Object val = sourceVariant.getInfo(spec.getSourceColumn());
					if (val != null) variant.setInfo(spec.getTargetColumn(), val);
				}
			}
		}
	}

	private <T> void setAlleleInfo(Variant variant, int i, String key, T value) {
		if (value == null) return;
		// variant.getInfo returns unmodifiable list, must create new one
		final List<T> target = repeat(null, variant.getAlternatives().size());
		final List<T> field = variant.getInfo(key);
		if (field != null) {
			for (int j = 0; j < field.size(); j++) {
				target.set(j, field.get(j));
			}
		}
		target.set(i, value);
		variant.setInfo(key, target);
	}

	@NotNull
	private <T> List<T> repeat(T value, int n) {
		final List<T> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(value);
		return list;
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
