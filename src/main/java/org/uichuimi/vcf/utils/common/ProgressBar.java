package org.uichuimi.vcf.utils.common;

import java.util.Timer;
import java.util.TimerTask;

public class ProgressBar {

	private static final int MILLISECONDS_PER_HOUR = 3_600_000;
	private static final int MILLISECONDS_PER_MINUTE = 60_000;

	private long start = -1;

	private final Timer timer = new Timer(true);
	private double progress;
	private String message = "";


	public ProgressBar() {
	}

	public long start() {
		start = System.nanoTime();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				print();
			}
		}, 250, 250);
		return start;
	}

	private void print() {
		final String time = formatTime(System.nanoTime() - start);
		System.out.printf("\r%s %.0f%% %s", time, progress * 100, message);
	}

	public void update(double progress, String message) {
		if (start < 0) start();
		this.progress = progress;
		this.message = message;
	}

	public void stop() {
		stop(message);
	}

	public void stop(String message) {
		this.message = message;
		timer.cancel();
		print();
		System.out.println();
	}

	private static String formatTime(long nanos) {
		long millis = nanos / 1_000_000;
		final long hours = millis / MILLISECONDS_PER_HOUR;  // 60 * 60 * 1000
		millis -= hours * MILLISECONDS_PER_HOUR;
		final long minutes = millis / MILLISECONDS_PER_MINUTE;
		millis -= minutes * MILLISECONDS_PER_MINUTE;
		final long seconds = millis / 1000;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}
