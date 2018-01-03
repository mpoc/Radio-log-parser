package application;

import java.util.Date;

public class Play {
	String title;
	Date startTime;
	Date stopTime;
	long playingTime;
	int timesPlayed;

	public Play(String title, Date startTime, Date stopTime) {
		this.title = title;
		this.startTime = startTime;
		this.stopTime = stopTime;
		calculatePlayingTime();
		setTimesPlayed(1);
	}
	
	private void calculatePlayingTime() {
		playingTime = (stopTime.getTime() - startTime.getTime())/1000;
	}
	
	public int getTimesPlayed() {
		return timesPlayed;
	}
	
	public long getPlayingTime() {
		return playingTime;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public Date getStopTime() {
		return stopTime;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	public void setStopTime(Date stopTime) {
		this.stopTime = stopTime;
	}
	
	public void setPlayingTime(long playingTime) {
		this.playingTime = playingTime;
	}
	
	public void setTimesPlayed(int timesPlayed) {
		this.timesPlayed = timesPlayed;
	}
	
	public String toString() {
//		return getTimesPlayed() + ": " + getPlayingTime() + " seconds: " + title;
		String csvDelimiter = ";";
		if (title.contains(" - ")) {
			String[] parts = title.split(" - ", 2);
//			return getTimesPlayed() + csvDelimiter + getPlayingTime() + csvDelimiter + parts[0] + csvDelimiter + parts[1];
//			return parts[0] + csvDelimiter + parts[1] + csvDelimiter + getTimesPlayed() + csvDelimiter + getPlayingTime() + csvDelimiter + (((getPlayingTime() - (getPlayingTime() % 60)) / 60) + ":" + (getPlayingTime() % 60));
			return parts[0] + csvDelimiter + parts[1] + csvDelimiter + getTimesPlayed() + csvDelimiter + getPlayingTime() + csvDelimiter + (((getPlayingTime() - (getPlayingTime() % 60)) / 60) + csvDelimiter + (getPlayingTime() % 60));
		}
		else {
//			return getTimesPlayed() + csvDelimiter + getPlayingTime() + csvDelimiter + title + csvDelimiter;
			return title + csvDelimiter + csvDelimiter + getTimesPlayed() + csvDelimiter + getPlayingTime() + csvDelimiter + (((getPlayingTime() - (getPlayingTime() % 60)) / 60) + csvDelimiter + (getPlayingTime() % 60));
		}
	}
}

