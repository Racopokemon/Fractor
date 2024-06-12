package rk.ramin.fractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import sun.security.util.DisabledAlgorithmConstraints;

import com.sun.java.accessibility.util.java.awt.CheckboxTranslator;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Main extends Application {

	private static final DecimalFormat format = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.US));
	
	private KnobField knobsF;
	private PreviewField previewF;
	private IOManager ioManager;
	private TextFieldDouble[] dataFields = new TextFieldDouble[4];
	/** "Focussed" means here that this SensorCanvas is using the dataFields */
	private boolean knobFieldFocussed = true;
	private double[][] dataFieldBounds = {{KnobField.POS_MIN,KnobField.POS_MIN,KnobField.SCALE_MIN,-180},{KnobField.POS_MAX,KnobField.POS_MAX,KnobField.SCALE_MAX,180},
			{PreviewField.MIN_POS,PreviewField.MIN_POS,PreviewField.MIN_ZOOM,-180},{PreviewField.MAX_POS,PreviewField.MAX_POS,PreviewField.MAX_ZOOM,180}};
	private static final int MAX_ITERATIONS_MIN = 1000, MAX_ITERATIONS_MAX = 150000;
	public static final int IMAGE_SIZE_MIN = 10, IMAGE_SIZE_MAX = 16384;
	public static final int SAMPLING_MIN = 4, SAMPLING_MAX = 512;
	public static final double ATOM_SIZE_MIN = 0.05, ATOM_SIZE_MAX = 1;
	public static final double BIGGEST_FACTOR_MIN = 0.0005, BIGGEST_FACTOR_MAX = 0.1;
	
	private Label info1, info2;
	private TextFieldInteger imgW, imgH, sampling;
	private TextFieldDouble atomSize, maxSize;
	private CheckBox effects, showBounds;
	
	public static void main(String[] args) {
		Main.launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		/**
		 * The stage represents the window, scenes are different views shown on it. 
		 * The scenes determine the size of the window. 
		 */
		HBox root = new HBox(16);

			Label stats = new Label("Anything has gone wrong here.");
			Label statsScale = new Label("We need to be updated ...");
			info1 = stats;
			info2 = statsScale;
		
		KnobField knobs = new KnobField(451,451,this);
		knobsF = knobs;

		PreviewField rect2 = new PreviewField(400, 400, 30000, this, knobs.getKnobs());
		previewF = rect2;
		
		TextFieldInteger maxIterations = new TextFieldInteger(30000,MAX_ITERATIONS_MIN,MAX_ITERATIONS_MAX);
		maxIterations.setPrefColumnCount(4);
		maxIterations.setOnOfficalValueChanged(new OfficialValueEventHandler() {
			@Override
			public void handle() {
				previewF.setMaxIterations(maxIterations.getValue());
			}
		});
		
		Button resMore = new Button();
		resMore.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				int base = previewF.getBase();
				if (base >= 2) {
					double stepsNow = (int)(Math.log(maxIterations.getValue())/Math.log(base));
					double stepsNew = stepsNow+1.5;
					int iterations = (int) Math.min(Math.pow(base, stepsNew), MAX_ITERATIONS_MAX);
					maxIterations.setValue(iterations);
					previewF.setMaxIterations(iterations);
				}
			}
		});
		Button resLess = new Button();
		resLess.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				int base = previewF.getBase();
				if (base >= 2) {
					double stepsNow = (int)(Math.log(maxIterations.getValue())/Math.log(base));
					double stepsNew = stepsNow-0.5;
					int iterations = (int) Math.max(Math.pow(base, stepsNew), MAX_ITERATIONS_MIN);
					maxIterations.setValue(iterations);
					previewF.setMaxIterations(iterations);
				}
			}
		});
		VBox res = new VBox(resMore, resLess);
		HBox wholeIterations = new HBox(new Label("atmos at most: "), maxIterations, res);
		wholeIterations.setAlignment(Pos.CENTER_RIGHT);
		
		BorderPane statistics = new BorderPane();
		statistics.setLeft(stats);
		BorderPane.setAlignment(stats, Pos.CENTER_LEFT);
		statistics.setRight(wholeIterations);
		
		VBox preview = new VBox(4);
		preview.setAlignment(Pos.TOP_LEFT);
		preview.getChildren().addAll(statistics, rect2, statsScale);

		GridPane grid = new GridPane();
		grid.vgapProperty().set(4);
		grid.hgapProperty().set(4);
		
		OfficialValueEventHandler oveh = new OfficialValueEventHandler() {
			@Override
			public void handle() {
				if (knobFieldFocussed) {
					knobs.onValuesChanged(dataFields[0].getValue(), dataFields[1].getValue(), 
							dataFields[2].getValue(), dataFields[3].getValue());
				} else {
					previewF.onValuesChanged(dataFields[0].getValue(), dataFields[1].getValue(), 
							dataFields[2].getValue(), dataFields[3].getValue());
				}
			}
		};
		String[] properties = {"X pos.","Y pos.","scale","rotation"};
		double[] data = {0,0,.5,0};
		for (int i = 0; i < 4; i++) {
			Label l = new Label(properties[i]);
			grid.add(l, 1, 3+i);
			TextFieldDouble tfd = new TextFieldDouble(data[i],dataFieldBounds[0][i],dataFieldBounds[1][i]);
			tfd.setPrefColumnCount(8);
			tfd.setOnOfficialValueChanged(oveh);
			tfd.setDisable(true);
			dataFields[i] = tfd;
			grid.add(tfd, 0, 3+i);
		}
		
		/* During development I implemented a better solution for that
		Button open = new Button("Open ...");
		open.setMaxWidth(1000);
		Button save = new Button("Save as ...");
		save.setMaxWidth(1000);
		grid.add(open, 0, 0);
		GridPane.setColumnSpan(open, 2);
		grid.add(save, 0, 1);
		GridPane.setColumnSpan(save, 2);
		
		Separator sp = new Separator(Orientation.HORIZONTAL);
		sp.setMinHeight(16);
		grid.add(sp, 0, 2);
		GridPane.setColumnSpan(sp, 2);
		*/
		
		Separator sp = new Separator(Orientation.HORIZONTAL);
		sp.setMinHeight(16);
		grid.add(sp, 0, 7);
		GridPane.setColumnSpan(sp, 2);

		GridPane g = new GridPane();
		TitledPane wrap;
		{
			//GridPane g = new GridPane();
			g.setVgap(4);
			g.setHgap(4);
			
			imgW = new TextFieldInteger(960,IMAGE_SIZE_MIN,IMAGE_SIZE_MAX);
			imgW.setPrefColumnCount(3);
			g.add(imgW, 0, 0);
			g.add(new Label("image width"), 1, 0);
			
			imgH = new TextFieldInteger(600,IMAGE_SIZE_MIN,IMAGE_SIZE_MAX);
			imgH.setPrefColumnCount(3);
			g.add(imgH, 0, 1);
			g.add(new Label("image height"), 1, 1);
			
			showBounds = new CheckBox("Show bounds in preview");
			g.add(showBounds, 0, 2);
			GridPane.setColumnSpan(showBounds, 2);
			
			sp = new Separator(Orientation.HORIZONTAL);
			sp.setMinHeight(8);
			g.add(sp, 0, 3);
			GridPane.setColumnSpan(sp, 2);
			
			sampling = new TextFieldInteger(20,SAMPLING_MIN,SAMPLING_MAX);
			sampling.setPrefColumnCount(4);
			g.add(sampling, 0, 4);
			g.add(new Label("[sampling]"), 1, 4);
			
			atomSize = new TextFieldDouble(0.2,ATOM_SIZE_MIN,ATOM_SIZE_MAX);
			atomSize.setPrefColumnCount(3);
			g.add(atomSize, 0, 5);
			g.add(new Label("atom size"), 1, 5);

			maxSize = new TextFieldDouble(0.02,BIGGEST_FACTOR_MIN,BIGGEST_FACTOR_MAX);
			maxSize.setPrefColumnCount(3);
			g.add(maxSize, 0, 6);
			g.add(new Label("biggest factor"), 1, 6);
			
			effects = new CheckBox("Add some nice effects");
			//effects.allowIndeterminateProperty().set(false);
			effects.selectedProperty().set(true);
			g.add(effects, 0, 7);
			GridPane.setColumnSpan(effects, 2);
			
			ProgressBar prog = new ProgressBar(0);
			prog.setMaxWidth(1000);
			g.add(prog, 0, 8);
			GridPane.setColumnSpan(prog, 2);
			
			Button render = new Button("Render!");
			render.setMaxWidth(1000);
			g.add(render, 0, 9);
			GridPane.setColumnSpan(render, 2);
						
			wrap = new TitledPane("Render ...", g);
			//wrap.setAlignment(Pos.CENTER);
			//wrap.setAnimated(false);
			//wrap.expandedProperty().set(false);
		}
		
		TitledPane io;
		{
			GridPane ioElements = new GridPane();
			ioElements.setVgap(4);
			ioElements.setHgap(4);
			
			Button chDir = new Button("Change directory");
			chDir.setMaxWidth(1000);
			ioElements.add(chDir, 0, 3, 2, 1);
			 
			ListView<String> files = new ListView<String>();
			files.setPrefWidth(0);
			ioElements.add(files, 0, 1, 2, 1); 
			
			Button loadButton = new Button("Load file");
			loadButton.setMaxWidth(1000);
			loadButton.requestFocus();
			ioElements.add(loadButton, 0, 2);
			
			Button deleteButton = new Button("Delete file");
			deleteButton.setMaxWidth(1000);
			ioElements.add(deleteButton, 1, 2);
			
			//sp = new Separator(Orientation.HORIZONTAL);
			//sp.setMinHeight(8);
			//ioElements.add(sp, 0, 3, 2, 1);
			
			TextField newFile = new TextField("");
			newFile.setPrefColumnCount(5);
			ioElements.add(newFile, 0, 0);
			
			Button saveButton = new Button("No name");
			saveButton.setDisable(true);
			saveButton.setMaxWidth(1000);
			ioElements.add(saveButton, 1, 0);
			
			ioManager = new IOManager(files, newFile, loadButton, saveButton, deleteButton, chDir, ioElements, stage, this);
			
			io = new TitledPane("Files", ioElements);
			ioElements.setAlignment(Pos.CENTER);
		}
		
		Accordion acc = new Accordion();
		acc.getPanes().addAll(io, wrap);
		acc.setExpandedPane(wrap);
		
		grid.add(acc, 0, 8);
		GridPane.setColumnSpan(acc, 2);
		
		//open.requestFocus();
		
		root.getChildren().addAll(grid, knobs, preview);
		
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(8));
		
		Scene scn = new Scene(root);
		stage.sizeToScene(); //:o no exception ...
		
		stage.setScene(scn);
		stage.setResizable(false);
		stage.setTitle("Fraktor");
		stage.show();
		
		double s = maxIterations.getHeight()*0.5;
		resMore.setMinSize(s, s);
		resMore.setPrefSize(s, s);
		resLess.setMinSize(s, s);
		resLess.setPrefSize(s, s);
		knobs.redraw();
		rect2.redraw();
		acc.setExpandedPane(io);
		
		// Yes - I had big plans when I started Fractor ... hopefully I will find one day the time to add the essential render feature ... ;)
		Label msg = new Label("Rendering is not implemented yet :/");
		msg.setWrapText(true);
		msg.setMaxWidth(g.getWidth());
		g.add(msg, 0, 10);
		GridPane.setColumnSpan(msg, 2);
		
		g.setDisable(true);
	}

	public void setDataFields(double x, double y, double scale, double rotation, boolean isKnobField) {
		if (isKnobField != knobFieldFocussed) {
			if (knobFieldFocussed) {
				knobsF.unfocus();
			} else {
				previewF.unfocus();
			}
			changeDataFieldsBounds(isKnobField);
			knobFieldFocussed = isKnobField;
		}
		if (!Double.isNaN(x)) {dataFields[0].setValue(x);}
		if (!Double.isNaN(y)) {dataFields[1].setValue(y);}
		if (!Double.isNaN(scale)) {dataFields[2].setValue(scale);}
		if (!Double.isNaN(rotation)) {dataFields[3].setValue(rotation);}
	}
	
	/**
	 * Deactivates all data fields and writes some standard values into them.
	 */
	public void deactivateDataFields(boolean isKnobField) {
		for (TextFieldDouble d : dataFields) {
			d.setDisable(true);
		}
		setDataFields(0,0,0.5,0,isKnobField);
	}
	
	/**
	 * Activates all data fields.
	 */
	public void activateDataFields() {
		for (TextFieldDouble d : dataFields) {
			d.setDisable(false);
		}
	}
	
	/**
	 * Call here to redraw the fractal.
	 * If there are new knob values insert null.
	 */
	public void updateAndRedrawFractal(ArrayList<Knob> data, int highlightIndex) {
		if (data!=null) {previewF.setData(data);}
		previewF.redrawFractal(highlightIndex);
	}
	
	public void updateLabels(int atomCount, int steps, int base, double smallest, double biggest) {
		String text1 = atomCount + (atomCount == 1 ? " atom | " : " atoms | ") + steps + (steps==1 ?" step | ":" steps | ")+base+" base",
				text2 = "smallest scale: "+format.format(smallest)+" | biggest scale: "+format.format(biggest);
		info1.setText(text1);
		info2.setText(text2);
		//System.out.println("Upd8");
	}
	
	private void changeDataFieldsBounds(boolean forKnobField) {
		int min, max;
		if (forKnobField) {
			min = 0;
			max = 1;
		} else {
			min = 2;
			max = 3;
		}
		for (int i = 0; i < 4; i++) {
			dataFields[i].changeInterval(dataFieldBounds[min][i], dataFieldBounds[max][i]);
		}
	}
	
	public void setFileContent(FileContent fc) {
		imgW.setValue(fc.getImageSize()[0]);
		imgH.setValue(fc.getImageSize()[1]);
		sampling.setValue(fc.getSampling());
		effects.setSelected(fc.getEffects());
		atomSize.setValue(fc.getAtomSizes()[0]);
		maxSize.setValue(fc.getAtomSizes()[1]);
		deactivateDataFields(true);
		previewF.loadData(fc);
		knobsF.loadData(fc);
	}
	
	public FileContent getFileContent() {
		FileContent fc = new FileContent();
		fc.setRenderSettings(imgW.getValue(), imgH.getValue(), effects.isSelected(), sampling.getValue(), atomSize.getValue(), maxSize.getValue());
		previewF.saveData(fc);
		knobsF.saveData(fc);
		return fc;
	}
}
