package rk.ramin.fractor;

/**
 * Very cheap handler for the case the user changes the value in a Integer- or DoubleTextField.
 * Events are only fired after user interactions.
 */
public interface OfficialValueEventHandler {
	public void handle();
}
