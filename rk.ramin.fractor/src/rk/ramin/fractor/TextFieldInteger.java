package rk.ramin.fractor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;

/**
 * TextField that accepts only integer numbers ... that are positive (cause I don't need negative numbers in this use)
 */
public class TextFieldInteger extends TextField {
	
	private int min, max, value, officalValue;
	private OfficialValueEventHandler changeHandler;
	
	public TextFieldInteger(int startValue, int minValue, int maxValue) {
		super(String.valueOf(startValue));
		min = minValue;
		max = maxValue;
		value = startValue;
		officalValue = startValue;
		
		this.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observ, String oldS, String newS) {
				if (newS.equals("")) {
					value = 0;
				} else {
					try {
						value = Integer.parseInt(newS);
					} catch (Exception e) {
						setText(oldS);
					}
				}
			}
		});
		this.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observ, Boolean oldB, Boolean newB) {
				if (!newB) {
					makeOffical();
				}
			}
		});
		this.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ae) {
				makeOffical();
			}
		});

	}
	
	private void makeOffical() {
		int old = officalValue;
		if (value < min) {
			setValue(min);
		} else if (value > max) {
			setValue(max);
		}
		officalValue = value;
		if (changeHandler != null && old != officalValue) {
			changeHandler.handle();
		}
	}

	/**
	 * The value has to be in bounds or you get an exception in your face.
	 * Setting the value on this way, no OfficalValue etc. event is fired.
	 */
	public void setValue(int v) {
		if (v < min || v > max) {
			throw new RuntimeException("TextFieldInteger: "+v+" is not in bounds!");
		}
		this.setText(String.valueOf(v));
		officalValue = v;
		value = v;
	}
	
	/**
	 * Returns the last typed, finished and bounds-checked value
	 */
	public int getValue() {
		return officalValue;
	}
	
	public void setOnOfficalValueChanged(OfficialValueEventHandler oveh) {
		changeHandler = oveh;
	}
}
