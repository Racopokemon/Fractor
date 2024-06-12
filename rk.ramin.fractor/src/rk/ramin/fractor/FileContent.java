package rk.ramin.fractor;

import java.util.ArrayList;

public class FileContent {
	
	protected int[] imageSize;
	protected boolean effects;
	protected int sampling;
	protected double[] renderSettings;
	
	protected double knobFactor = 1;
	
	protected double[] pov;
	
	protected Knob[] knobs;
	
	public FileContent() {}
	
	public void setRenderSettings(int imgX, int imgH, boolean eff, int sampl, double atmSz, double mxSz) {
		imageSize = new int[] {imgX, imgH};
		effects = eff;
		sampling = sampl;
		renderSettings = new double[] {atmSz, mxSz};
	}
	
	public void setPointOfView(double x, double y, double scl, double rot) {
		pov = new double[] {x, y, scl, rot};
	}

	public void setKnobFactor(double factor) {
		
	}
	
	public void setKnobs(ArrayList<Knob> knobs) {
		this.knobs = new Knob[knobs.size()];
		knobs.toArray(this.knobs);
	}
	
	public void setKnobs(Knob[] knobs) {
		this.knobs = knobs;
	}
	
	public int[] getImageSize() {
		return imageSize;
	}
	
	public boolean getEffects() {
		return effects;
	}
	
	public int getSampling() {
		return sampling;
	}
	
	public double[] getAtomSizes() {
		return renderSettings;
	}
	
	public double[] getPointOfView() {
		return pov;
	}
	
	public Knob[] getKnobs() {
		return knobs;
	}
	
	/**
	 * Checks every value to be in bounds and changes values that are not in bounds. 
	 */
	public void verify() {
		imageSize[0] = setToBounds(imageSize[0],Main.IMAGE_SIZE_MIN,Main.IMAGE_SIZE_MAX);
		imageSize[1] = setToBounds(imageSize[1],Main.IMAGE_SIZE_MIN,Main.IMAGE_SIZE_MAX);
		sampling = setToBounds(sampling, Main.SAMPLING_MIN, Main.SAMPLING_MAX);
		renderSettings[0] = setToBounds(renderSettings[0], Main.ATOM_SIZE_MIN, Main.ATOM_SIZE_MAX);
		renderSettings[1] = setToBounds(renderSettings[1], Main.BIGGEST_FACTOR_MIN, Main.BIGGEST_FACTOR_MAX);
		
		pov[0] = setToBounds(pov[0], PreviewField.MIN_POS, PreviewField.MAX_POS); 
		pov[1] = setToBounds(pov[1], PreviewField.MIN_POS, PreviewField.MAX_POS); 
		pov[2] = setToBounds(pov[2], PreviewField.MIN_ZOOM, PreviewField.MAX_ZOOM); 
		pov[3] = setToBounds(pov[3], -180, 180);
		
		for (Knob k : knobs) {
			k.verifyValues();
		}
	}
	
	private int setToBounds(int value, int min, int max) {
		if (value < min) {
			return min;
		} else if (value > max) {
			return max;
		} else {
			return value;
		}
	}
	private double setToBounds(double value, double min, double max) {
		if (value < min) {
			return min;
		} else if (value > max) {
			return max;
		} else {
			return value;
		}
	}
	
	/**
	 * For debugging etc. 
	 * Writes all data into system.out
	 */
	public void printContent() {
		System.out.println(imageSize[0]);
		System.out.println(imageSize[1]);
		System.out.println(effects);
		System.out.println(sampling);
		System.out.println(renderSettings[0]);
		System.out.println(renderSettings[1]);
		System.out.println();
		System.out.println(pov[0]);
		System.out.println(pov[1]);
		System.out.println(pov[2]);
		System.out.println(pov[3]);
		System.out.println();
		for (Knob k : knobs) {
			System.out.println(k.getXPosition());
			System.out.println(k.getYPosition());
			System.out.println(k.getScale());
			System.out.println(k.getRotation());
			System.out.println();
		}
	}
}
