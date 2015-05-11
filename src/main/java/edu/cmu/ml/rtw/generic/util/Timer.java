package edu.cmu.ml.rtw.generic.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class Timer {
	private class Clock {
		private boolean stopped;
		private long runTime;
		private long startTime;
		
		public Clock() {
			this.stopped = true;
			this.startTime = 0;
			this.runTime = 0;
		}
		
		public boolean isStopped() {
			return this.stopped;
		}
		
		public long getRunTimeInMillis() {
			return this.runTime + System.currentTimeMillis() - this.startTime;
		}
		
		public boolean stop() {
			if (isStopped())
				return false;
			
			this.stopped = true;
			this.runTime += System.currentTimeMillis() - this.startTime;
			return true;
		}
		
		public boolean start() {
			if (!isStopped())
				return false;
			this.stopped = false;
			this.startTime = System.currentTimeMillis();
			
			return true;
		}
		
		@Override
		public String toString() {
			if (!isStopped())
				return "[Not Stopped]";
			
			long hours = TimeUnit.MILLISECONDS.toHours(this.runTime);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(this.runTime) - TimeUnit.HOURS.toMinutes(hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(this.runTime) - TimeUnit.MILLISECONDS.toSeconds(minutes) - TimeUnit.MILLISECONDS.toSeconds(hours);
			return String.format("%d h %d m %d s", hours, minutes, seconds);		
		}
	}
	
	private Map<String, Clock> clocks;
	
	public Timer() {
		this.clocks = new HashMap<String, Clock>();
	}
	
	public synchronized boolean startClock(String clockName) {
		if (!this.clocks.containsKey(clockName))
			this.clocks.put(clockName, new Clock());
				
		return this.clocks.get(clockName).start();
	}
	
	public long getClockRunTimeInMillis(String clockName) {
		return this.clocks.get(clockName).getRunTimeInMillis();
	}
	
	public synchronized boolean stopClock(String clockName) {
		if (!this.clocks.containsKey(clockName))
			return false;
		return this.clocks.get(clockName).stop();
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		for (Entry<String, Clock> entry : this.clocks.entrySet()) {
			str.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");
		}
		
		return str.toString();
	}
}
