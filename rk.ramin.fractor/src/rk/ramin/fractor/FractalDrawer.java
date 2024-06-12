package rk.ramin.fractor;

import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * This is pretty much the core of this application, 
 * but as you can see it isn't complicated at all :D
 * 
 * So much code wrapped around this ...
 */
public class FractalDrawer {
	
	/** data[count of duplicators] [data] 
	 * data: 
	 * 0: xPos
	 * 1: yPos
	 * 2: scale
	 * 3: rotation */
	private double[][] data;
	private int length;
	
	private double size;
	
	/**
	 * But before anything works you have to call updatePoints()!
	 */
	public FractalDrawer(double atomSize) {
		size = atomSize;
	}
	
	public void updatePoints(ArrayList<Knob> knobs) {
		int s = knobs.size();
		double[][] d = new double[s][];
		int i = 0;
		for (i = 0; i < s; i++) {
			d[i] = knobs.get(i).getDataForFractalDrawer();
		}
		data = d;
		length = data.length;
	}
	
	/**
	 * 0 Steps: one starting atom, length^0 
	 * 1 Step: length atoms, 1 time was calculated, length^1
	 * 2 Steps: length^2
	 * ...
	 */
	public void startCalculation(double x, double y, double scale, double rotation, int maxSteps, int highlight, GraphicsContext graphics) {
		graphics.setFill(Color.DARKGRAY);
		calculate(x,y,scale,rotation,0,maxSteps,highlight,-1,graphics);
	}
	
	private void calculate(double x, double y, double s, double r, int step, int maxStep, int highlight, int firstOccurrence, GraphicsContext gc) {
		if (step >= maxStep) {
		//if (s < 1.3) {
			onAtomGenerated(x, y, firstOccurrence, gc);
		} else {
			for (int i = 0; i < length; i++) {
				double newX = data[i][0]*s, newY = data[i][1]*s;
				//calculate(x+newX,y+newY,s*scl,r+rot, step+1, maxStep, gc); Without rotation
				calculate(x+(Math.cos(r)*newX)-(Math.sin(r)*newY),y+(Math.sin(r)*newX)+(Math.cos(r)*newY),s*data[i][2],r+data[i][3], step+1, maxStep, highlight, (firstOccurrence == -1 && i==highlight ? step : firstOccurrence), gc);
			}
		}
	}
	
	private void onAtomGenerated(double x, double y, int firstOccurrence, GraphicsContext gc) {
		gc.setFill(firstOccurrence == 0 ? Color.SKYBLUE : Color.DIMGRAY);
		gc.fillRect(x, y, size, size);
	}
}