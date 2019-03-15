package de.uni_jena.cs.fusion.abecto.processor.progress;

import java.util.ArrayList;
import java.util.List;

public class SubtaskProgressManager {

	private class SubtaskProgressListener implements ProgressListener {

		private SubtaskProgressManager manager;
		private int id;

		protected SubtaskProgressListener(SubtaskProgressManager manager, int id) {
			this.manager = manager;
			this.id = id;
		}

		@Override
		public void onFailure(Throwable throwable) {
			this.manager.onFailure(throwable);
		}

		@Override
		public void onProgress(long current, long total) {
			this.manager.onProgress(this.id, current, total);

		}

		@Override
		public void onSuccess() {
			this.manager.onSuccess(this.id);
		}

	}

	private ProgressListener listener;
	private List<SubtaskProgressListener> subtasks = new ArrayList<>();
	private List<Long> current = new ArrayList<>();
	private List<Long> currentNormed = new ArrayList<>();
	private List<Long> total = new ArrayList<>();
	private long totalLCM = 1;
	private List<Boolean> success = new ArrayList<>();

	private int subtaskCount;

	public SubtaskProgressManager(ProgressListener listener, int subtaskCount) {
		this.listener = listener;
		this.subtaskCount = subtaskCount;
	}

	public synchronized void addSubtask() {
		this.subtaskCount++;
	}

	public synchronized void addSubtasks(int i) {
		this.subtaskCount += i;
	}

	public synchronized SubtaskProgressListener getSubtaskProgressListener() {
		SubtaskProgressListener subtask = new SubtaskProgressListener(this, subtasks.size());
		this.subtasks.add(subtask);
		this.current.add(0L);
		this.currentNormed.add(0L);
		this.total.add(1L);
		this.success.add(false);
		if (this.subtaskCount < this.subtasks.size()) {
			this.subtaskCount = this.subtasks.size();
			this.notifyListener();
		}
		return subtask;
	}

	private void notifyListener() {
		this.listener.onProgress(this.currentNormed.stream().mapToLong(Long::longValue).sum(),
				this.totalLCM * this.subtaskCount);
	}

	public synchronized void onFailure(Throwable throwable) {
		this.listener.onFailure(throwable);
	}

	protected synchronized void onProgress(int id, long current, long total) {
		if (!this.success.get(id)) {
			boolean notify = false;
			if (!this.total.get(id).equals(total)) {
				this.total.set(id, total);
				this.updateTotalLCM();
				this.updateCurrentNormed();
				notify = true;
			}
			if (!this.current.get(id).equals(current)) {
				this.current.set(id, current);
				this.updateCurrentNormed(id);
				notify = true;
			}
			if (notify) {
				this.notifyListener();
			}
		}
	}

	public synchronized void onSuccess() {
		this.listener.onSuccess();
	}

	protected synchronized void onSuccess(int id) {
		if (!this.success.get(id)) {
			this.success.set(id, true);
			this.current.set(id, this.total.get(id));
			this.currentNormed.set(id, this.totalLCM);
			this.notifyListener();
		}
	}

	private void updateCurrentNormed() {
		for (int id = 0; id < this.current.size(); id++) {
			this.updateCurrentNormed(id);
		}
	}

	private void updateCurrentNormed(int id) {
		this.currentNormed.set(id, this.current.get(id) * (this.totalLCM / this.total.get(id)));
	}

	private void updateTotalLCM() {
		// update LCM of totals
		this.totalLCM = this.total.stream().reduce((a, b) -> {
			// calculate gcd
			long gcd = a;
			long x = b;
			while (x > 0) {
				long temp = x;
				x = gcd % x;
				gcd = temp;
			}
			// calculate lcm
			return a * (b / gcd);
		}).orElseThrow();
	}

}
