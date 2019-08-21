package org.uichuimi.vcf.utils.annotation.common;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ProgressBar {

	private static final int TOTAL_BARS = 30;
	private static final String SOLID_BLOCK = "■";
	private static final String EMPTY_BLOCK = "□";
	private static final String PROGRESS_BLOCK = "▣";

	private long start = -1;

	private final Timer timer = new Timer(true);
	private double progress;
	private String message = "";


	public ProgressBar() {
	}

	public void start() {
		start = System.nanoTime();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				print();
			}
		}, 250, 400);
	}

	private void print() {
		final String time = formatTime(System.nanoTime() - start);
		final int bars = (int) (progress * TOTAL_BARS);
		final int indicator = bars < TOTAL_BARS ? 1 : 0;
		final String brs =
				SOLID_BLOCK.repeat(bars) +
				PROGRESS_BLOCK.repeat(indicator) +
				EMPTY_BLOCK.repeat(TOTAL_BARS - bars - indicator);
		System.out.printf("\r%s %3.0f%% %s %s", time, progress * 100, brs, message);
	}

	public void update(double progress, String message) {
		if (start < 0) start();
		this.progress = progress;
		this.message = message;
	}

	public void stop() {
		timer.cancel();
		print();
		System.out.println();
	}

	private static String formatTime(long nanos) {
		return String.format("%02d:%02d:%02d", TimeUnit.NANOSECONDS.toHours(nanos),
				TimeUnit.NANOSECONDS.toMinutes(nanos) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.NANOSECONDS.toSeconds(nanos) % TimeUnit.MINUTES.toSeconds(1));
	}

	public long getElapsedNanos() {
		return System.nanoTime() - start;
	}
}
