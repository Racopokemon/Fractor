package rk.ramin.fractor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;

/**
 * This field shows the inserted number rounded to 6 decimal places.
 * If you request with getValue you might get a number that has much more places than shown in the field.
 * If the user inserts more than the 6 numbers they will be saved internally but shown rounded to 6 decimal places.
 * 
 * Internally we simply store the value as a double variable.
 * To have not a endless number of numbers or stuff like 2.34E-24 we String.format() them. 
 */
public class TextFieldDouble extends TextField {

	private double min, max, value, officialValue;
	private OfficialValueEventHandler changeHandler;
	
	private static DecimalFormat formatter;
	
	static {formatter = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.US));}
	
	public TextFieldDouble(double startValue, double minValue, double maxValue) {
		super(String.valueOf(startValue));
		min = minValue;
		max = maxValue;
		value = startValue;
		officialValue = startValue;
		
		this.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observ, String oldS, String newS) {
				if (newS.equals("") || newS.equals("-") || newS.equals("-.") || newS.equals(".")) {
					value = 0;
				} else {
					try {
						value = Double.parseDouble(newS); 
						//pressing first time enter will keep all additional decimal places and save them. 
						//pressing again reads out the number in the text field again (or reading the value-value) and writes a rounded number as official.
					} catch (Exception e) {
						setText(oldS);
					}
				}
			}
		});
		this.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observ, Boolean oldB, Boolean newB) {
				if (!newB) {
					makeOfficial();
				}
			}
		});
		this.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ae) {
				makeOfficial();
			}
		});
	}
	
	private String format(double value) {
		/*try {
		return String.format(Locale.US, "%.6f", value);
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR";
		}
		Because the formatter supports decimal places but cannot remove useless zeros -.- ...
		... I had to use the DecimalFormat.
		*/
		return formatter.format(value);
	}
	
	private void makeOfficial() {
		double old = officialValue;
		if (value < min) {
			setValue(min);
		} else if (value > max) {
			setValue(max);
		}
		officialValue = value;
		setText(format(value));
		if (changeHandler != null && old != officialValue) {
			changeHandler.handle();
		}
	}

	/**
	 * The value has to be in bounds or you get an exception in your face.
	 * Setting the value on this way, no officialValue etc. event is fired.
	 */
	public void setValue(double v) {
		if (v < min || v > max) {
			throw new RuntimeException("TextFieldDouble: "+v+" is not in bounds!");
		}
		this.setText(format(v));
		officialValue = v;
		//value = v;
	}
	
	/**
	 * Returns the last typed, finished and bounds-checked value
	 */
	public double getValue() {
		return officialValue;
	}
	
	/**
	 * Changes the bounds of this textField. If the last (official) value is not in this new bounds,
	 * it is set to the new minimum value. 
	 * Regardless of the official value gets changed there will be no officialValueEvent thrown - 
	 * provision is made for inserting a new value right after changing the bounds. 
	 */
	public void changeInterval(double min, double max) {
		this.min = min;
		this.max = max;
		if (officialValue < min || officialValue > max) {
			setValue(min);
		}
	}
	
	public void setOnOfficialValueChanged(OfficialValueEventHandler oveh) {
		changeHandler = oveh;
	}
	
	/**
	   	public TextFieldDouble(double startValue, double minValue, double maxValue) {
		super(String.valueOf(startValue));
		min = minValue;
		max = maxValue;
		value = startValue;
		officialValue = startValue;
		
		this.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observ, String oldS, String newS) {
				if (newS.equals("") || newS.equals("-") || newS.equals("-.") || newS.equals(".")) {
					value = 0;
				} else {
					try {
						value = Double.parseDouble(newS);
					} catch (Exception e) {
						setText(oldS);
					}
				}
			}
		});
		this.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observ, Boolean oldB, Boolean newB) {
				if (!newB) {
					makeofficial();
				}
			}
		});
		this.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ae) {
				makeofficial();
			}
		});
	}

	 */
}
