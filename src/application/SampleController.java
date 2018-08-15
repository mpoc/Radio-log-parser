package application;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class SampleController {
	//The list of items (log files)
	ObservableList<File> items = FXCollections.observableArrayList();

    @FXML ListView<File> listView = new ListView<File>();
    @FXML Label statusLabel;
    
    String[] labelStatuses = {"Waiting for log files or parsing start", //0
    		"Parsing log files", //1
    		"Adding log files", //2
    		"Removing log files", //3
    		"Clearing log file list", //4
    		"Adding dragged-and-dropped log files", //5
    		"Creating log summary file"}; //6
    
    public void setStatus(int a) {
		statusLabel.setText(labelStatuses[a]);
    }
    
    @FXML
    public void initialize(){
    	//Allows the selection of multiple items in the list
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		setStatus(0);
    }
    
	@FXML
	protected void parseLogs(ActionEvent event) {
		//Check if there are any files in the list
		if (!items.isEmpty()) {
			setStatus(6);
			
			//Open a dialog which allows to select the output file
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save file as");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HHmmss");
			Date date = new Date();
			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Microsoft Excel files (*.xlsx)", "*.xlsx");
		    fileChooser.getExtensionFilters().add(extFilter);
		    fileChooser.setInitialFileName("Log summary " + df.format(date) + ".xlsx");
			File dest = fileChooser.showSaveDialog(new Stage());
			
			//Checks if the destination has been picked and not cancelled
			if (dest != null) {
				Alert wait = new Alert(AlertType.INFORMATION);
				wait.setHeaderText("Please wait while log files are being read.");
				wait.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
				wait.show();

				setStatus(1);
				
				//Parses the log files present in the list and stores the failed logs in a string
				String failedLogs = "";
				try {
					failedLogs = Parsing.startParsing(items);
					wait.close();
				}
				catch (Exception ex) {
					wait.close();
					showExceptionAlert(ex);
				}
					
				//Checks if there were any invalid logs and if there were, displays an alert 
				if (!failedLogs.isEmpty()) {
					Alert failedLogsAlert = new Alert(AlertType.INFORMATION);
					failedLogsAlert.setHeaderText("The following log files were detected as invalid and were not used in parsing");
					failedLogsAlert.setContentText(failedLogs);
					failedLogsAlert.showAndWait();
				}
				
				//If there were valid songs, create log summary
				if (!Parsing.songList.isEmpty()) {
					Parsing.buildXLSX(Parsing.songs, dest);	
					
					//Shows a dialog, informing that the file has been created and can be opened
					Alert alert = new Alert(AlertType.CONFIRMATION);
					alert.setHeaderText("File written to " + dest.getAbsolutePath());
					ButtonType btnOpen = new ButtonType("Open");
					ButtonType btnOK = new ButtonType("OK", ButtonData.OK_DONE);
					alert.getButtonTypes().setAll(btnOpen, btnOK);
					Optional<ButtonType> result = alert.showAndWait();
					//Open the output file, if requested
					File file = new File(dest.getAbsolutePath());
					if (result.get() == btnOpen){
						try {
							Desktop.getDesktop().open(file);
						}
						catch (Exception ex) {
							showExceptionAlert(ex);
						}
					}
				}
				else {
					Alert noValidSongs = new Alert(AlertType.ERROR);
					noValidSongs.setHeaderText("No valid songs were found, log summary file was not created.");
					noValidSongs.showAndWait();
				}
			}

			setStatus(0);
		}
		else {
			Alert noSelect = new Alert(AlertType.ERROR);
			noSelect.setHeaderText("No log files have been added.");
			noSelect.showAndWait();
		}
    }
	
	@FXML
	protected void addLogs(ActionEvent event) {
		setStatus(2);
		
		//Open a file chooser dialog to pick log files to add
	    FileChooser chooser = new FileChooser();
	    chooser.setTitle("Add log files");
	    
	    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Log files (*.log)", "*.log");
	    chooser.getExtensionFilters().add(extFilter);
	    
	    List<File> fileList = chooser.showOpenMultipleDialog(new Stage());
	    
	    //Checks if any filed have been chosen and the selecting dialog was not cancelled
	    if (fileList != null) {
	    	//Creating a string for duplicate files
	    	String duplicates = "";
	    	boolean duplicatesExist = false;
	    	
			Iterator<File> itr = fileList.iterator();      
            while (itr.hasNext()) {
            	File temp = itr.next();
            	
            	//Only add the files if they are not already present in the list
            	if (!items.contains(temp)) {
            		items.add(temp);
            	}
            	else {
            		duplicates += temp.getAbsolutePath() + "\n";
            		duplicatesExist = true;
            	}
            }
            
            //If there were any duplicates, show an alert informing about them
            if (duplicatesExist) {
    			Alert duplicatesAlert = new Alert(AlertType.INFORMATION);
    			duplicatesAlert.setHeaderText("The following files were not added, because they are already in the list");
    			duplicatesAlert.setContentText(duplicates);
    			duplicatesAlert.showAndWait();
            }
	    	
            //Update ListView
		    listView.setItems(items);
	    }  
	    
	    setStatus(0);
    }
	
	@FXML
	protected void removeLogs(ActionEvent event) {
		//Get files that are selected in the ListView
		ObservableList<File> selectedItems = listView.getSelectionModel().getSelectedItems();
		
		//Check if any files are selected, then remove them
		if (!selectedItems.isEmpty()) {
			setStatus(3);
			
			Alert confirm = new Alert(AlertType.CONFIRMATION);
			confirm.setHeaderText("Are you sure you want to remove the selected items?");
           
            //Formatting a list of files that are about to be deleted
			Iterator<File> itr = selectedItems.iterator();
            String filenamesToRemove = "";            
            while (itr.hasNext()) {
            	filenamesToRemove += itr.next().getAbsolutePath() + "\n";
            }
            
            confirm.setContentText(filenamesToRemove);
			Optional<ButtonType> result = confirm.showAndWait();
			
			//If OK is clicked, then remove the selected files
			if (result.get() == ButtonType.OK){
				items.removeAll(selectedItems);
	            listView.setItems(items);
			}
			
			setStatus(0);
		}
		else {
			Alert noSelect = new Alert(AlertType.ERROR);
			noSelect.setHeaderText("No items have been selected to be removed.");
			noSelect.showAndWait();
		}
    }
	
	@FXML
	protected void clearLogs(ActionEvent event) {
		setStatus(4);
		
		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setHeaderText("Are you sure you want to clear all items?");
		Optional<ButtonType> result = confirm.showAndWait();
		
		//If OK is clicked, then remove all items
		if (result.get() == ButtonType.OK){
			items.clear();
			listView.setItems(items);
		}
		
		setStatus(0);
    }
	
	@FXML
	protected void showSongFolderSelect(ActionEvent event) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("The current song folder is " + Parsing.logfileSongsDirectory);
		alert.setTitle("Song folder");
		ButtonType btnChange = new ButtonType("Change");
		ButtonType btnManual = new ButtonType("Manual entry");
		ButtonType btnOK = new ButtonType("OK", ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(btnChange, btnManual, btnOK);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == btnChange){
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Song folder");
			//If the current directory exists, set it as the initial directory of the dialog
			if(Parsing.logfileSongsDirectory.exists()){
				chooser.setInitialDirectory(Parsing.logfileSongsDirectory);
			}
			File selectedDirectory = chooser.showDialog(new Stage());
			//If the folder selecting menu wasn't cancelled
			if (selectedDirectory != null) {
				Parsing.logfileSongsDirectory = selectedDirectory;
            	Alert changed = new Alert(AlertType.INFORMATION);
            	changed.setHeaderText("The new song folder was sucessfully set to " + selectedDirectory);
            	changed.showAndWait();
			}
		}
		else if (result.get() == btnManual) {
			TextInputDialog dialog = new TextInputDialog(Parsing.logfileSongsDirectory.getAbsolutePath());
			dialog.setHeaderText("Song folder");
			dialog.setContentText("Please enter song folder filepath:");

			Optional<String> filepath = dialog.showAndWait();
			if (filepath.isPresent()){
				File folderFilepath = new File(filepath.get());
//				if (folderFilepath.isDirectory()) {
					Parsing.logfileSongsDirectory = folderFilepath;
					Alert changed = new Alert(AlertType.INFORMATION);
	            	changed.setHeaderText("The new song folder was sucessfully set to " + folderFilepath);
	            	changed.showAndWait();
//				}
//				else {
//					Alert changed = new Alert(AlertType.ERROR);
//	            	changed.setHeaderText("Invalid folder, song folder path unchanged");
//	            	changed.showAndWait();
//				}

			}
		}
    }
	
	@FXML
	protected void showAlbumDataFileSelect(ActionEvent event) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("The album data file is " + Parsing.albumDataFile.getAbsolutePath());
		alert.setTitle("Album data file");
		ButtonType btnChange = new ButtonType("Change");
		ButtonType btnOK = new ButtonType("OK", ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(btnChange, btnOK);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == btnChange){
			FileChooser chooser = new FileChooser();
			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Microsoft Excel files (*.xlsx)", "*.xlsx");
		    chooser.getExtensionFilters().add(extFilter);
			chooser.setTitle("Album data file");
			//If the current file exists, set its parent as the initial directory of the dialog
		    if(Parsing.albumDataFile.exists()){
		    	chooser.setInitialDirectory(Parsing.albumDataFile.getAbsoluteFile().getParentFile());
		    }
			File selectedFile = chooser.showOpenDialog(new Stage());
			//If the folder selecting menu wasn't cancelled
			if (selectedFile != null) {
				Parsing.albumDataFile = selectedFile;
            	Alert changed = new Alert(AlertType.INFORMATION);
            	changed.setHeaderText("The new album data file was sucessfully set to " + selectedFile);
            	changed.showAndWait();
			}
		}
    }
	
	@FXML
	protected void selectAll(ActionEvent event) {
		listView.getSelectionModel().selectAll();
    }
	
	@FXML
	protected void unselectAll(ActionEvent event) {
		listView.getSelectionModel().clearSelection();
    }
	
	@FXML
	protected void about(ActionEvent event) {
    	Alert alert = new Alert(AlertType.INFORMATION);
    	alert.setTitle("About");
    	alert.setHeaderText("About RADIO player pro log parser");
    	alert.setContentText("Radio player pro log parser v1.0 by Marius Pocevièius\n\n"
    			+ "This is a program which takes log files of played songs from the program \"RADIO player pro\" and "
    			+ "outputs for how long and how many times a certain song has been played. The output is presented in "
    			+ "a formatted excel table together with the album name of the song, the record label and the ISRC ID. "
    			+ "These details are gathered from a customizable album data file.");
    	alert.showAndWait();
    }
	
	@FXML
	protected void quit(ActionEvent event) {
		Platform.exit();
    }

	@FXML
	public void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
        boolean success = false;
        
        //If any items were dragged-and-dropped in
        if (db.hasFiles()) {
        	setStatus(5);
            success = true;
            
            //String of files that are invalid: are directiories, not log files or duplicates 
            String invalidFiles = "";
            //Were there any invalid files
            boolean invalidFilesExist = false;
            
            for (File file : db.getFiles()) {  	
            	//If file is valid
            	if (!file.isDirectory() && fileExtension(file).equals("log") && !items.contains(file)) {
            		items.add(file);
            	}
            	else {
            		invalidFiles +=  file.getAbsolutePath() + "\n";
            		invalidFilesExist = true;
            	}
            }
            
            //Update ListView
            listView.setItems(items);
            
            //If there were any invalid files, show an alert with them
            if (invalidFilesExist) {
            	Alert alert = new Alert(AlertType.INFORMATION);
            	alert.setHeaderText("The following items were not added, because they were either a directory, not a log file or a duplicate of an already existing item");
            	alert.setContentText(invalidFiles);
            	alert.showAndWait();
            }
            
            setStatus(0);
        }
        event.setDropCompleted(success);
        event.consume();
	}

	@FXML
	public void onDragOver(DragEvent event) {
		 Dragboard db = event.getDragboard();
         if (db.hasFiles()) {
             event.acceptTransferModes(TransferMode.COPY);
         } else {
             event.consume();
         }
	}
	
	//Code for this method adapted from http://code.makery.ch/blog/javafx-dialogs-official/
	public static void showExceptionAlert(Exception ex) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setHeaderText("Program encountered an exception");

		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("The exception stacktrace was:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);

		alert.showAndWait();
	}
	
	//Gets file extension
	public String fileExtension(File file) {
		return file.getName().substring(file.getName().lastIndexOf(".") + 1);
	}
}
