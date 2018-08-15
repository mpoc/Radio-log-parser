package application;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.collections.ObservableList;

public class Parsing {
	public static ArrayList<Song> songs = new ArrayList<Song>(); //ArrayList of all Song objects
	public static ArrayList<String> songList = new ArrayList<String>(); //ArrayList of all filepaths of songs (which are in the log files)
	public static File logfileSongsDirectory = new File("E:\\Sandëlys\\"); //The path of the folder in which the songs are located (program only accepts files from a single folder)
	public static File albumDataFile = new File("Album data.xlsx"); //The path of the file, which contains album data

	public static String startParsing(ObservableList<File> files) {
		//Clearing the song lists, because otherwise repeated parsing without closing the program would return results of songs being played more times, than they actually did 
		songs.clear();
		songList.clear();
		
		String invalidLogFiles = ""; //A string containing paths of invalid log files
		
		Iterator<File> itr = files.iterator();
		while (itr.hasNext()) {
			File inputFile = itr.next();
			StringBuilder inputString = parseFileToString(inputFile);
			//If there was an error and the file could not be find, inputSrting will be null.
			//Thus, this file is no longer processed
			if (inputString == null) {
				continue;
			}
			//If the log file turned out to be invalid, add it to the invalidLogFiles string
			if (!splitToSongsAndProcess(inputString)) {
				invalidLogFiles += inputFile.getAbsolutePath() + "\n";
			}
		}

		Collections.sort(songs, new SongArtistComparator());
		
		return invalidLogFiles;
	}

	//Parses all the lines from a log file into one string
	public static StringBuilder parseFileToString(File inputFile) {
		Scanner inputRead = null;
		try {
			inputRead = new Scanner(inputFile);
		}
		catch (Exception ex) {
			SampleController.showExceptionAlert(ex);
			return null;
		}
		
		StringBuilder inputString = new StringBuilder();
		
		while (inputRead.hasNextLine()) {
			inputString.append(inputRead.nextLine() + "\n");
		}
		
		inputRead.close();
		
		return inputString;
	}

	//Splits the string of whole log file into separate song segments using regex. Adds the songs to global ArrayLists songs and songList. Returns true if a valid file was passed as an argument, false if an invalid
	public static boolean splitToSongsAndProcess(StringBuilder inputString) {
		//This regex matches the first line [Files] and every song segment in the log file 
		//Original regex is (?:.+\n)+
		String regexToSplitIntoSongs = "(?:.+\\n)+";
		Pattern patternToSplitIntoSongs = Pattern.compile(regexToSplitIntoSongs); 
		Matcher matcherToSplitIntoSongs = patternToSplitIntoSongs.matcher(inputString);

		//Checks the validity of the file using the first match (it must be [Files]) and whether the file matches the given regex
		boolean validity = true;
		if (matcherToSplitIntoSongs.find()) {
			if (!matcherToSplitIntoSongs.group().equals("[Files]\n")) {
				//The first match does not equal [Files], which means that the log file is invalid
				validity = false;
			}
		}
		else {
			//Regex was unable to parse this file, which means that it is invalid
			validity = false;
		}
		
		//If file is invalid, exit from the method and return false
		if (!validity) {
			return validity;
		}

		boolean validSongsFound = false; //Were there any song found, that are from the path logfileSongsDirectory
		
		//If this point is reached, the log file is valid, thus further parsing is done 
		while(matcherToSplitIntoSongs.find()) {
			String songPlay = matcherToSplitIntoSongs.group(); //One song segment from the log file
			
			System.out.println("Song found " + matcherToSplitIntoSongs.start() + "-" + matcherToSplitIntoSongs.end());
			
			ArtistTitle artistTitleData = getArtistTitle(songPlay);
			Time startTimeData = getStartTime(songPlay);
			Time stopTimeData = getStopTime(songPlay);	
			boolean timeFound = startTimeData.found & startTimeData.found; //Both start and stop time being found
			long playingTime = 0; //The playing time of the song from the song segment

			if (timeFound && startTimeData.time.before(stopTimeData.time)) {
				playingTime = (stopTimeData.time.getTime() - startTimeData.time.getTime()) / 1000; //In seconds
				System.out.println("Song played for " + playingTime + " seconds");
			}
			else {
				System.out.println("Error: stop time is earlier than start time");
				timeFound = false;
			}
			
			if (artistTitleData.found & timeFound) {
				addSong(artistTitleData.songFilepath, artistTitleData.artist, artistTitleData.title, playingTime);
				validSongsFound = true;
			}
			else {
				System.out.println("The song artist and title, start or stop of playing time was not found. Song is not added to the list.");
			}
			System.out.println();
		}
		
		//If no valid songs were found, that means that the whole file is invalid
		if (!validSongsFound) {
			validity = false;
		}
		
		return validity;
	}
	
	public static ArtistTitle getArtistTitle(String songPlay) {
		String artist = "";
		String title = "";
		boolean artistTitleFound = false;
		String songFilepath = "";
		//Regex finds the line, beginning with Path=, which has the about filepath
		//Original regex is Path=(.+)
		String regexToFindPath = "Path=(.+)";
		Pattern patternToFindPath = Pattern.compile(regexToFindPath);
		Matcher matcherToFindPath = patternToFindPath.matcher(songPlay);
		
		if (matcherToFindPath.find()) {
			songFilepath = matcherToFindPath.group(1);
			//Regex finds the artist and title from the filepath 
			//Original regex is E:\\Sandëlys\\(.+) - (.+)\..+
			//String regexToFindArtistSongname = "E:\\\\Sandëlys\\\\(.+) - (.+)\\..+";
			String regexToFindArtistSongname = logfileSongsDirectory.getPath().replaceAll("\\\\", "\\\\\\\\") + "\\\\(.+) - (.+)\\..+";
			Pattern patternToFindArtistSongname = Pattern.compile(regexToFindArtistSongname);
			Matcher matcherToFindArtistSongname = patternToFindArtistSongname.matcher(songFilepath);
			
			if (matcherToFindArtistSongname.find()) {
				artist = matcherToFindArtistSongname.group(1); //Artist
				title = matcherToFindArtistSongname.group(2); //Title
				artistTitleFound = true;
				
				System.out.println("Artist found: " + artist); 
				System.out.println("Title found: " + title); 
			}
			else {
				System.out.println("Artist and/or title not found, " + matcherToFindPath.group());
			}
		}
		else {
			System.out.println("Path not found");
		}
		
		return new ArtistTitle(artist, title, artistTitleFound, songFilepath);
	}
	
	public static Time getTime(String regex, String songPlay) {
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		boolean timeFound = false;
		Date time = new Date();
		
		Pattern patternToFindTime = Pattern.compile(regex);
		Matcher matcherToFindTime = patternToFindTime.matcher(songPlay);
					
		
		if (matcherToFindTime.find()) {
			System.out.print("Song time found: ");
			if (matcherToFindTime.group(3) == null) {
				try {
					time = df.parse(matcherToFindTime.group(2) + " 00:00:00");
				}
				catch (Exception ex){
					System.out.println(ex);
				}
			}
			else {
				try {
					time = df.parse(matcherToFindTime.group(1));
				}
				catch (Exception ex){
					System.out.println(ex);
				}
			}
			
			System.out.println(time);
			timeFound = true;
		}
		else {
			System.out.println("Time not found");
			System.out.println();
		}
		return new Time(time, timeFound);
	}
	
	public static Time getStartTime(String songPlay) {
		//Original regex is StartTime=((\d{4}\.\d{2}\.\d{2}) ?(\d{2}:\d{2}:\d{2})?)
		String regexToFindStartTime = "StartTime=((\\d{4}\\.\\d{2}\\.\\d{2}) ?(\\d{2}:\\d{2}:\\d{2})?)";
		return getTime(regexToFindStartTime, songPlay);
	}
	
	public static Time getStopTime(String songPlay) {
		//Original regex is StopTime=((\d{4}\.\d{2}\.\d{2}) ?(\d{2}:\d{2}:\d{2})?)
		String regexToFindStopTime = "StopTime=((\\d{4}\\.\\d{2}\\.\\d{2}) ?(\\d{2}:\\d{2}:\\d{2})?)";
		return getTime(regexToFindStopTime, songPlay);
	}

	public static void addSong(String songFilepath, String artist, String title, long playingTime) {
		if (songList.contains(songFilepath)) {
			songs.get(songList.indexOf(songFilepath)).addTimesPlayed();
			songs.get(songList.indexOf(songFilepath)).addPlayingTime(playingTime);;
		}
		else {
			songList.add(songFilepath);
			songs.add(new Song(artist, title, playingTime));
		}
	}

	public static void buildXLSX(ArrayList<Song> songs, File dest) {
		String[][] albumData = getAlbumData();
		
		Workbook workbook = new XSSFWorkbook();		
		Sheet sheet = workbook.createSheet("Song data");

		//Creating the style for the header row
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		//Cell style to show two digits after the decimal point for the column of "Playing time (in minutes)"
		CellStyle playingTimeMinutesCellStyle = workbook.createCellStyle();
		playingTimeMinutesCellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
		
		if (albumData != null) {
			//Creating the header row
			String[] columns = {"Title", "Album", "Artist", "Record label", "Times played", "Playing time (in seconds)", "Playing time (in minutes)", "ISRC identifier"};
			Row headerRow = sheet.createRow(0);
			for(int i = 0; i < columns.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(columns[i]);
				cell.setCellStyle(headerCellStyle);
			}
			
			//Adding the songs to the excel worksheet row-by-row
			int rowNum = 1;
			for(Song song: songs) {
				Row row = sheet.createRow(rowNum++);
				
				String title = song.getTitle();
				String artist = song.getArtist();
				boolean albumDataFound = false;
				int albumDataIndex = -1;
				
				for (int i = 2; i < albumData.length; i++) {
					if (albumData[i][3].equals(artist) && albumData[i][1].equals(title)) {
						albumDataFound = true;
						albumDataIndex = i;
					}
				}
				
				row.createCell(0).setCellValue(title);
				row.createCell(2).setCellValue(artist);
				row.createCell(4).setCellValue(song.getTimesPlayed());
				row.createCell(5).setCellValue(song.getPlayingTime());
				//Setting the cell style of "Playing time (in minutes)"
				Cell playingTimeMinutesCell = row.createCell(6);
				playingTimeMinutesCell.setCellValue((double)(song.getPlayingTime())/60);
				playingTimeMinutesCell.setCellStyle(playingTimeMinutesCellStyle);
				
				if (albumDataFound) {
					row.createCell(1).setCellValue(albumData[albumDataIndex][2]); //Album name
					row.createCell(3).setCellValue(albumData[albumDataIndex][4]); //Record label
					row.createCell(7).setCellValue(albumData[albumDataIndex][5]); //ISRC ID
				}
			}
			
			//Auto-sizing the columns to fit the contents
			for(int i = 0; i < columns.length; i++) {
				sheet.autoSizeColumn(i);
				if (sheet.getColumnWidthInPixels(i) > 257) {
					sheet.setColumnWidth(i, 8000);
				}
			}
		}
		else {
			//Creating the header row
			String[] columns = {"Title", "Artist", "Times played", "Playing time (in seconds)", "Playing time (in minutes)"};
			Row headerRow = sheet.createRow(0);
			for(int i = 0; i < columns.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(columns[i]);
				cell.setCellStyle(headerCellStyle);
			}
			
			//Adding the songs to the excel worksheet row-by-row
			int rowNum = 1;
			for(Song song: songs) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(song.getTitle());
				row.createCell(1).setCellValue(song.getArtist());
				row.createCell(2).setCellValue(song.getTimesPlayed());
				row.createCell(3).setCellValue(song.getPlayingTime());
				//Setting the cell style of "Playing time (in minutes)"
				Cell playingTimeMinutesCell = row.createCell(4);
				playingTimeMinutesCell.setCellValue((double)(song.getPlayingTime())/60);
				playingTimeMinutesCell.setCellStyle(playingTimeMinutesCellStyle);
			}
			
			//Auto-sizing the columns to fit the contents
			for(int i = 0; i < columns.length; i++) {
				sheet.autoSizeColumn(i);
			}
		}

		//Writing to the actual Excel file
		try {
			FileOutputStream output = new FileOutputStream(dest.getAbsolutePath());
			workbook.write(output);
			output.close();
			workbook.close();
		}
		catch (Exception ex) {
			SampleController.showExceptionAlert(ex);
		}
	}
	
	public static String[][] getAlbumData() {
		//Code adapted from https://www.callicoder.com/java-read-excel-file-apache-poi/
		Workbook workbook = null;
		
		//Checks if the input file exists and is an excel file
		try {
			workbook = WorkbookFactory.create(albumDataFile);
		}
		catch (Exception ex) {
			SampleController.showExceptionAlert(ex);
			return null;
		}
		
		Sheet sheet = workbook.getSheetAt(0);
        int numOfRows = sheet.getPhysicalNumberOfRows();
        String[][] albumData = new String[numOfRows][6]; //A two dimensional array which will contain all excel sheet data
        DataFormatter dataFormatter = new DataFormatter(); //Needed to get string data of cells
        int currentRowNumber = 0; //Needed for row parsing into two dimensional array
        
        //Goes through all rows of the excel sheet
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            Iterator<Cell> cellIterator = row.cellIterator();
            int currentCellNumber = 0;
            while (cellIterator.hasNext()) {
            	//Checks if there were more than 6 columns, which would mean an invalid album data file
            	if (currentCellNumber > 5) {
            		Exception ex = new Exception("Invalid number of columns in album data file");
            		SampleController.showExceptionAlert(ex);
            		return null;
            	}
            	else {
            		Cell cell = cellIterator.next();
                	String cellValue = dataFormatter.formatCellValue(cell);
                	albumData[currentRowNumber][currentCellNumber] = cellValue;
                	currentCellNumber++;
            	}
            }
            currentRowNumber++;
        }
        
		try {
			workbook.close();
		}
		catch (Exception ex) {
			SampleController.showExceptionAlert(ex);
			return null;
		}
		return albumData;
	}
}
