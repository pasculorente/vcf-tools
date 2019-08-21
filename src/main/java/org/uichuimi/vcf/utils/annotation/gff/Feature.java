package org.uichuimi.vcf.utils.annotation.gff;

import java.util.List;

class Feature implements Comparable<Feature> {

	private final String id;
	private final String type;
	private final String biotype;
	private final Integer start;
	private final Integer end;

	Feature(String id, String type, String biotype, Integer start, Integer end) {
		this.id = id;
		this.type = type;
		this.biotype = biotype;
		this.start = start;
		this.end = end;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getBiotype() {
		return biotype;
	}

	public Integer getStart() {
		return start;
	}

	public Integer getEnd() {
		return end;
	}

	@Override
	public int compareTo(Feature other) {
		if (other == this) return 0;
		final int compare = Integer.compare(start, other.start);
		if (compare != 0) return compare;
		return Integer.compare(end, other.end);
	}

	static <T extends Feature> T binarySearch(List<T> list, int position) {
		return binarySearch(list, 0, list.size() - 1, position);
	}

	private static <T extends Feature> T binarySearch(List<T> list, int start, int end, int position) {
		if (end >= start) {
			int mid = start + (end - start) / 2;
			final T feature = list.get(mid);
			final int compare = feature.compareTo(position);
			if (compare == 0) return feature;
			if (compare < 0) return binarySearch(list, start, mid - 1, position);
			else return binarySearch(list, mid + 1, end, position);
		}
		return null;
	}

	int compareTo(int position) {
		if (position < start) return -1;
		if (position > end) return 1;
		return 0;
	}

	boolean contains(int position) {
		return start <= position && position <= end;
	}

}
