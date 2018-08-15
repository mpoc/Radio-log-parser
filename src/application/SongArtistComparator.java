package application;

import java.util.Comparator;

//Code adapted from https://stackoverflow.com/questions/2784514/sort-arraylist-of-custom-objects-by-property
public class SongArtistComparator implements Comparator<Song> {
    public int compare(Song song1, Song song2) {
        return song1.getTitle().compareTo(song2.getTitle());
    }
}