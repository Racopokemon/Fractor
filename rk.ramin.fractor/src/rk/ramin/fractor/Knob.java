package rk.ramin.fractor;

public class Knob {

	private double x, y, r, s;
	private static final double MOVEMENT_FACTOR = 1.5, INNER_FACTOR = 0.4f, TRANS_MID_FACTOR = 1.7f, TRANS_MAX_FACTOR = TRANS_MID_FACTOR+0.8f;
	private static double radius, movementRange, crossRange, hoverRangeX, hoverRangeY;
	private boolean ignored = false;
	
	public static void setRadiusOnField(double radiusOnField) {
		radius = radiusOnField;
		movementRange = radius*MOVEMENT_FACTOR;
		crossRange = radius*INNER_FACTOR;
		hoverRangeX = radius*2.7;
		hoverRangeY = radius*0.9;
	}
	
	public Knob() {
		this(0,0,0.5,0);
	}
	
	public Knob(double x, double y, double s, double r, boolean ignored) {
		this.x = x;
		this.y = y;
		this.r = r;
		this.s = s;
		this.ignored = ignored;
	}
	
	public Knob(double x, double y, double s, double r) {
		this(x,y,s,r,false);
	}
	
	public Knob(Knob k) {
		this(k.x,k.y,k.s,k.r,k.ignored);
	}
	
	/**
	 * Checks whether the mouse position is in range of this knob (or of the scaling / rotation rings around)
	 * 0 = doesn't touch mouse at all
	 * 1 = scaling ring
	 * 2 = rotation ring
	 * 3 = knob itself (for movement)
	 * 4 = inner of the knob (movement)
	 */
	public byte touchesMouse(double x, double y) {
		double dist = distance(x-this.x,y-this.y);
		if (dist < crossRange) {
			return 4;
		} else if (dist < movementRange) {
			return 3;
		} else if (Math.abs(x-this.x)<hoverRangeX && Math.abs(y-this.y)<hoverRangeY) {
			return this.x > x ? (byte)1 : (byte)2;
		}
		return 0;
	}
	
	/**
	 * Returns all values written into a double array.
	 * The values may be converted or changed to fit to the data types that the FractalDrawer accepts.
	 */
	public double[] getDataForFractalDrawer() {
		double[] d = {x,-y,s,r*Math.PI/180};
		return d;
	}
	
	public static double distance(double x, double y) {
		return Math.sqrt(x*x+y*y);
	}
	
	public double getXPosition() {
		return x;
	}
	
	public double getYPosition() {
		return y;
	}
	
	public double getScale() {
		return s;
	}
	
	/**
	 * Knob rotation in degrees
	 */
	public double getRotation() {
		return r;
	}
	
	/**
	 * Returns the knob rotation in radians
	 */
	public double getRotationInRadians() {
		return r*Math.PI/180;
	}
	
	public boolean getIgnored() {
		return ignored;
	}
	
	public void setNewValues(double x, double y, double s, double r) {
		this.x = x;
		this.y = y;
		this.s = s;
		this.r = r;
	}
	
	public void setNewPosition(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setRotation(double rotation) {
		r = rotation;
	}
	
	public void setScale(double scale) {
		s = scale;
	}
	
	public void setIgnored(boolean i) {
		ignored = i;
	}
	
	/**
	 * Returns the new value for ignored
	 */
	public boolean toggleIgnored() {
		ignored = !ignored;
		return ignored;
	}
	
	/**
	 * Checks (and corrects if necessary) the knob for position, scale and rotation values, that are not in bounds. (KnobField.POS_MIN etc.)
	 */
	public void verifyValues() {
		setToBounds(x, KnobField.POS_MIN, KnobField.POS_MAX);
		setToBounds(y, KnobField.POS_MIN, KnobField.POS_MAX);
		setToBounds(s, KnobField.SCALE_MIN, KnobField.SCALE_MAX);
		setToBounds(r, -180, 180);
	}
	private double setToBounds(double value, double min, double max) 
	{	if (value < min) {return min;} 
		else if (value > max) {return max;} 
		else {return value;}}

}
