package rk.ramin.fractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class IOManager {

	private ListView<String> list;
	private TextField file;
	private Button loadButton;
	private Button saveButton;
	private Button deleteButton;
	private Button directoryButton;

	private File currentDir;
	private File[] momentaryListedFiles = new File[0];

	private Main app;
	private Window owner;

	private DirectoryChooser directory;

	private boolean ignoreInputs = false;

	public IOManager(ListView<String> list, TextField fileName, Button load, Button save, Button delete, Button chDir, Region pane, Window owner, Main app) {
		this.list = list;
		file = fileName;
		saveButton = save;
		loadButton = load;
		deleteButton = delete;
		directoryButton = chDir;
		
		this.app = app;
		this.owner = owner;
		
		directory = new DirectoryChooser();
		directory.setTitle("Select new directory");
		File finalDir = null;
		File dir = this.getJarLocation();
		if (dir != null) {
			File fracDir = new File(dir.getAbsolutePath()+"\\Fractals");
			if (fracDir.exists()) {
				finalDir = fracDir;
			}
		}
		if (finalDir == null) {
			currentDir = new File(System.getenv("USERPROFILE"));
		} else {
			currentDir = finalDir;
		}
		
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				save();
			}
		});
		
		loadButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				loadSelected();
			}
		});
		
		directoryButton.setOnAction(new EventHandler<ActionEvent> () {
			@Override
			public void handle(ActionEvent event) {
				directory.setInitialDirectory(currentDir);
				File newDir = directory.showDialog(owner);
				if (newDir != null) {
					currentDir = newDir;
				}
				updateFiles(false);
				list.requestFocus();
				//We should always update, even if the directory has not changed - maybe the user wants the listView to update by doing so 
			}
		});
		
		deleteButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (list.getSelectionModel().isEmpty()) {
					return;
				}
				Alert confirm = new Alert(AlertType.CONFIRMATION, "Are you sure you want to delete "+list.getSelectionModel().getSelectedItem()+".fractor?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
				confirm.setHeaderText(null);
				confirm.setTitle("Confirm");
				Optional<ButtonType> ret = confirm.showAndWait();
				if (ret.isPresent() && ret.get() == ButtonType.YES) {
					try {
						Files.delete(Paths.get(currentDir+"\\"+list.getSelectionModel().getSelectedItem()+".fractor"));
					} catch (IOException e) {
						e.printStackTrace();
						showAlert("A bit embarrassing - error during deletion: \n"+e.getMessage());
					} finally {
						updateFiles(false);
					}
				}
			}
		});
		
		list.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getClickCount() == 2) {
					loadSelected();
				}
			}
		});
		list.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					loadSelected();
				}
			}
		});
		list.getSelectionModel().selectedItemProperty().addListener(new ListSelectionHandler());
		file.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				save();
			}
		});
		file.textProperty().addListener(new TextFieldHandler());
		
		pane.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				updateFiles(true);
			}
		});
		
		updateFiles(false);
	}

	// http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
	private File getJarLocation() {
		try {
			return new File(IOManager.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Checks whether something has changed in the folder (or in the new folder)
	 * If the content of the folder changed in any way,
	 */
	private void updateFiles(boolean checkBefore) {
		String lastFile = (String) list.getSelectionModel().getSelectedItem();
		ArrayList<String> chosen = new ArrayList<String>();
		File[] fls = currentDir.listFiles();
		for (int i = 0; i < fls.length; i++) {
			if (isFractorFile(fls[i])) {
				String name = fls[i].getName();
				chosen.add(name.substring(0, name.length() - 8));
			}
		}
		boolean update;
		if (checkBefore) {
			ObservableList<String> items = list.getItems();
			if (items.size() == chosen.size()) {
				update = false;
				for (String s : chosen) {
					if (!items.contains(s)) {
						update = true;
						break;
					}
				}
			} else {
				update = true;
			}
		} else {
			update = true;
		}
		if (update) {
			ignoreInputs = true;
			ObservableList<String> items = list.getItems();
			list.getSelectionModel().clearSelection();
			items.clear();
			items.addAll(chosen);
			items.sort(StringComparator.INSTANCE);
			if (lastFile != null) {
				if (items.contains(lastFile)) {
					list.getSelectionModel().select(lastFile);
				} else {
					file.setText("");
					updateSaveButton();
				}
			}
			ignoreInputs = false;
		}
	}

	/**
	 * If nothing is selected, simply nothing happens.
	 */
	private void loadSelected() {
		if (list.getSelectionModel().isEmpty()) {
			return;
		}
		File f = new File(currentDir.getPath() + "\\"
				+ list.getSelectionModel().getSelectedItem() + ".fractor");
		if (f.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				app.setFileContent(FileManager.readFile(fis));
			} catch (Exception e) {
				e.printStackTrace();
				showAlert("Error during loading process: \n" + e.getMessage());
			} finally {
				try {
					if (fis != null) fis.close();
				} catch (IOException e) {e.printStackTrace();}
			}
		} else {
			showAlert(f + " does not exist. \nUpdating files list.");
			updateFiles(false);
		}
	}

	/**
	 * Saves the current fractal in a file named with the text of the textField.
	 * If the file already exists, it will be overwritten, if the file name is
	 * empty, nothing happens, and if the file name is invalid, also nothing
	 * happens.
	 */
	private void save() {
		if (saveButton.disabledProperty().get()) {
			// The save button is always up to date - there is no need to check
			// the file name again
			return;
		}
		OutputStream fos = null;
		try {
			fos = Files.newOutputStream(Paths.get(currentDir + "\\" + 
					file.getText() + ".fractor"));
			FileManager.writeFile(fos, app.getFileContent());
		} catch (Exception e) {
			e.printStackTrace();
			showAlert("Error during saving process: \n" + e.getMessage());
			//Maybe I should delete the file afterwards ... 
		} finally {
			try {
				if (fos != null) fos.close();
			} catch (Exception e) {e.printStackTrace();}
			updateFiles(false);
		}
	}

	private boolean isFractorFile(File f) {
		if (!f.isFile()) {
			return false;
		} else {
			String name = f.getName();
			int lastIndex = name.lastIndexOf(".");
			if (lastIndex == -1) {
				return false;
			}
			String extension = name.substring(lastIndex + 1);
			return extension.equals("fractor");
			/*
			 * Yes. Only the lower case extension ".fractor" is accepted by this
			 * software. This is not a bug but the best solution to get this
			 * software to support also case-sensitive operation systems:
			 * Because it is smarter and easier for the user, the software
			 * leaves out the file extensions. In case-sensitive operation
			 * systems, there could be more than one file with the same filename
			 * and only an case-different extension. Of course it would still
			 * work by e.g. also showing the extensions or having another list
			 * that is not shown in mind or something, but in my opinion this
			 * solution is still the best I could do. (I would even have more
			 * trouble down in the TextFieldHandler by finding out whether the
			 * typed file fits to one (or even more) existing files with a
			 * any-case extension ... D:)
			 */
		}
	}

	// Thanks to
	// http://stackoverflow.com/questions/893977/java-how-to-find-out-whether-a-file-name-is-valid
	private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t',
			'\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

	private boolean isFileNameValid(String name) {
		boolean invalid = true;
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) != ' ') { // Anti whitespace ... just stupid to
											// have empty file names in the
											// list.
				invalid = false; // Maybe I should even force that the first
									// character in general must not be a space
									// ... this file names are also ridiculous,
									// aren't they?
				break;
			}
		}
		if (invalid) {
			return false;
		} else {
			for (int i = 0; i < ILLEGAL_CHARACTERS.length; i++) {
				if (name.indexOf((ILLEGAL_CHARACTERS[i])) != -1) {
					return false;
				}
			}
		}
		return true;

	}

	private void showAlert(String text) {
		Alert a = new Alert(AlertType.ERROR, text, ButtonType.OK);
		a.setTitle("Error");
		a.setHeaderText(null); // <- I don't like these header lines
		a.showAndWait();
	}

	private void updateSaveButton() {
		String name = file.getText();
		if (name.equals("")) {
			saveButton.setDisable(true);
			saveButton.setText("No name");
		} else if (isFileNameValid(name)) {
			File ex = new File(currentDir.getPath() + "\\" + name + ".fractor");
			saveButton.setDisable(false);
			if (ex.exists()) {
				saveButton.setText("Overwrite");
			} else {
				saveButton.setText("Save");
			}
		} else {
			saveButton.setDisable(true);
			saveButton.setText("Invalid");
		}
	}

	private static class StringComparator implements Comparator<String> {
		private static StringComparator INSTANCE = new StringComparator();

		@Override
		public int compare(String s1, String s2) {
			return s2.compareTo(s2);
		}
	}

	private class ListSelectionHandler implements ChangeListener<String> {
		@Override
		public void changed(ObservableValue<? extends String> observable,
				String oldValue, String newValue) {
			if (!ignoreInputs) {
				// So in here there are only events caused by the user
				ignoreInputs = true;
				file.setText(newValue);
				ignoreInputs = false;
				updateSaveButton();
			}
		}
	}

	private class TextFieldHandler implements ChangeListener<String> {
		@Override
		public void changed(ObservableValue<? extends String> observable,
				String oldValue, String newValue) {
			if (!ignoreInputs) {
				ignoreInputs = true;
				// Yes ... this is veeery circumstantial ... but what other
				// solution is there to make sure the program also run on
				// Linux-systems etc. ?
				File f = new File(currentDir.getPath() + "\\" + newValue
						+ ".fractor");
				if (f.exists()) {
					if (list.getItems().contains(newValue)) {
						list.getSelectionModel().select(newValue);
					} else {
						// The file exists and we assume this list to be up to
						// date. Anyway we could not find newValue as file name.
						// It follows that we are on a case-insensitive file
						// system and the file newValue exists - in another
						// case.
						// We have to compare using equalsIgnoreCase.
						for (String s : list.getItems()) {
							if (s.equalsIgnoreCase(newValue)) {
								list.getSelectionModel().select(s);
								break;
							}
						}
					}
				} else {
					list.getSelectionModel().clearSelection();
				}
				ignoreInputs = false;
				updateSaveButton();
			}
		}
	}
}
