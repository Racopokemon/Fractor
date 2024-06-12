package rk.ramin.fractor;

public abstract class MouseJob {

	public MouseJob() {}
	
	/**
	 * When the first mouse button is pressed we initialize.
	 * The first update() event is called after another mouse event.
	 */
	public abstract void initialize();
	
	/**
	 * Called during the job if mouse positions, buttons or keys change.
	 */
	public abstract void update();
	
	/**
	 * Called when there is no mouse button pressed anymore. 
	 * This will exit the job and we have the possibility to do the final steps of our task. 
	 */
	public abstract void finish();

}
