package application;

public class ArtistTitle {
	String artist;
	String title;
	boolean found;
	String songFilepath;
	
	public ArtistTitle(String artist, String title, boolean found, String songFilepath) {
		this.artist = artist;
		this.title = title;
		this.found = found;
		this.songFilepath = songFilepath;
	}
}
