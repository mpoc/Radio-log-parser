package application;
	
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class Main extends Application {
	
	public static ArrayList<String> paths = new ArrayList<String>();
	public static ArrayList<File> logFileObjects = new ArrayList<File>();
//	public static File logFile = new File("06_kovas_2017.log");
	//	public static File csvFile = new File("zz.csv");
//	public static Play[] songs;
	public static ArrayList<Play> songsA = new ArrayList<Play>();
//	static String csvFile = "zz.csv";
	//public static CSVWriter excelWriter = new CSVWriter(new FileWriter(csvFile));


		public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		public static Date date = new Date();
	
	@Override
	public void start(Stage primaryStage) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("Sample.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Radijo klubas");
			primaryStage.getIcons().add(new Image("file:icon.png"));
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static void outputToFile() throws IOException {
		PrintWriter output = new PrintWriter("playing_time_" + dateFormat.format(date) + ".csv");
		output.println("Artist;Song name;Times played;Time played (in seconds);Time played (in minutes only);Time played (in seconds only)");
//		String[] name = logFile.getName().split("\\.");
//		PrintWriter output = new PrintWriter(name[0] + "_playingtimes.csv");

		Map<String, Play> playMap = new HashMap<>();
		for (Play h : songsA) {
			Play current = playMap.get(h.getTitle());
			if (current == null) {
				playMap.put(h.getTitle(), h);
			}
			else {
				current.setPlayingTime(current.getPlayingTime() + h.getPlayingTime());
				current.setTimesPlayed(current.getTimesPlayed() + h.getTimesPlayed());
			}
		}
		Collection<Play> list = playMap.values();
		System.out.println(String.join("\n", list.stream().map(o -> o.toString()).collect(Collectors.toList())));
		output.println((String.join("\n", list.stream().map(o -> o.toString()).collect(Collectors.toList()))));
		System.out.println("uniques:" + list.size());
		output.close();
	}

	public static void parse(File file) throws IOException {
		//		Scanner inLog = new Scanner(file, "UTF-8");
		Scanner inLog = new Scanner(file);

		inLog.nextLine();
		inLog.nextLine();

		DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

		int count = 0;
		while (inLog.hasNextLine()) {
			if (inLog.nextLine().contains("Path=")) {
				count++;
			}
		}

		System.out.println("count:" + count);

		inLog.close();
		//		inLog = new Scanner(file, "UTF-8");
		inLog = new Scanner(file);

		inLog.nextLine();
		inLog.nextLine();

		int sandeliniai = 0;

		for (int i = 0; i < count; i++) {
			String currLine = "a";

			String title = "";
			Date startTimeD = new Date();
			Date stopTimeD = new Date();

			boolean isFromSandelys = false;

			while (!currLine.matches("[\\n\\r]*") && inLog.hasNextLine()) {
				currLine = inLog.nextLine();
				//System.out.println(currLine);
				if (currLine.contains("Path=") && currLine.contains("Sandëlys")) {
					isFromSandelys = true;
					//					System.out.println("cia is sandelio");
					sandeliniai++;
				}

				//isFromSandelys = true;	//Jei nori, kad imtu visus, ne tik is sandelio

				if (currLine.contains("Title=") && isFromSandelys && !currLine.contains("Tag_ID31") && !currLine.contains("Tag_ID32")) {
					String[] parts = currLine.split("=");
					title = parts[1];
				}

				if (currLine.contains("StartTime=") && isFromSandelys) {
					String[] parts = currLine.split("=");
					if (!parts[1].contains(":")) {
						parts[1] += " 00:00:00";
					} try {
						startTimeD = df.parse(parts[1]);
					}
					catch (Exception ex){
						System.out.println(ex);
					}

				}

				if (currLine.contains("StopTime=") && isFromSandelys) {
					String[] parts = currLine.split("=");
					if (!parts[1].contains(":")) {
						parts[1] += " 00:00:00";
					}
					try {
						stopTimeD = df.parse(parts[1]);
					}
					catch (Exception ex){
						System.out.println(ex);
					}
				}
			} // while (line) end
			if (isFromSandelys)
				songsA.add(new Play(title, startTimeD, stopTimeD));
			isFromSandelys = false;
		} // for (segment) end
		inLog.close();
		System.out.println("sandeliniai:" + sandeliniai);
	}// method parse() end
	
}
