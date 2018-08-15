package application;

public class Song {
	private String artist;
	private String title;
	private long playingTime;
	private int timesPlayed;
	
	public Song(String artist, String title) {
		setArtist(artist);
		setTitle(title);
		setTimesPlayed(1);
	}
	
	public Song(String artist, String title, long playingTime) {
		setArtist(artist);
		setTitle(title);
		setPlayingTime(playingTime);
		setTimesPlayed(1);
	}
	
	public String getArtist() {
		return artist;
	}
	
	public String getTitle() {
		return title;
	}
	
	public long getPlayingTime() {
		return playingTime;
	}
	
	public int getTimesPlayed() {
		return timesPlayed;
	}
	
	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public void setPlayingTime(long playingTime) {
		this.playingTime = playingTime;
	}
	
	public void setTimesPlayed(int timesPlayed) {
		this.timesPlayed = timesPlayed;
	}
	
	public void addTimesPlayed() {
		setTimesPlayed(getTimesPlayed() + 1);
	}
	
	public void addPlayingTime(long playingTime) {
		setPlayingTime(getPlayingTime() + playingTime);
	}
}

