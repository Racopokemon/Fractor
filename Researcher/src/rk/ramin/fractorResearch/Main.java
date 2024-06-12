package rk.ramin.fractorResearch;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class Main {

	private static final DecimalFormat format = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.GERMANY));
	
	private int cnt;
	private static final double STEP = 0.00001, START = STEP*800;
	private static final int IGNORE_COUNT = 1, TIME_STEP = 3000;
	private double[] knobs = new double[] {0.5, 0.5};
	
	public Main() {}
	
	public static void main(String[] args) {
		new Main().makeLine();
		//new Main().go(new double[] {0.4, 0.4});
	}
	
	public void makeLine() {
		double start = 0.01, step = 0.01, end = 0.88;
		int size = (int)((end-start)/step)+1;
		double[] fact = new double[size], pow = new double[size];
		int c = 0;
		for (double i = start; i < end; i+=step) {
			double[] reg = go(new double[] {0.7, i});
			fact[c] = reg[0];
			pow[c++] = reg[1];
		}
		System.out.println("Finished.\n\nValues for factor:");
		for (double d : fact) {
			System.out.println(d);
		}
		System.out.println("\n\nValues for power:");
		for (double d : pow) {
			System.out.println(d);
		}
	}
	
	public double[] go(double[] knbs) {
		knobs = knbs;
		ArrayList<Double> limits = new ArrayList<Double>();
		ArrayList<Integer> atoms = new ArrayList<Integer>();
		SimpleRegression reg = new SimpleRegression();
		int lastValue = -1;
		int ignore = IGNORE_COUNT;
		long lastTime = System.currentTimeMillis()+TIME_STEP;
		System.out.println("Started.");
		for (double b = START; b <= 1; b+=STEP) {
			cnt = 0;
			step(1, b, knobs);
			if (cnt != lastValue) {
				if (ignore > 0) {
					ignore--;
				} else {
					if (cnt < 0) {
						System.out.println("cnt is negative :o\n We will irgnore this value.");
					} else {
						limits.add(b-STEP);
						atoms.add(lastValue);
						reg.addData(Math.log(b-STEP), Math.log(lastValue));
					}
				}
				lastValue = cnt;
			}
			if (System.currentTimeMillis() > lastTime) {
				System.out.println("Progress: "+format.format(b*100));
				lastTime = System.currentTimeMillis()+TIME_STEP;
			}
		}
		double power = reg.getSlope(),
				factor = Math.exp(reg.getIntercept());
		
		//System.out.println("Progress: 100\nFinished!\n");
		/*
		System.out.println("Output for scale limit:");
		for (Double d : limits) {
			System.out.println(format.format(d.doubleValue()));
		}
		System.out.println("\nOutput for atom count:");
		for (Integer i : atoms) {
			System.out.println(format.format(i.doubleValue()));
		}
		System.out.println("\n--------------\nRegression results:\nFactor: "+factor+"\nPower: "+power);
		*/
		return new double[] {factor, power};
	}
	
	private void step(double factor, double min, double[] values) {
		if (factor >= min) {
			for (int i = 0; i < values.length; i++) {
				double f = factor*values[1];
				/*if (f == factor) {
					if (factor >= min) {
						System.out.println("We will never end this. \nWe exit here.");
						System.exit(0);
					}
				}*/
				step(factor*values[i], min, values);
			}
		} else {
			cnt++;
		}
	}

}

/**
 * UNGENAUIGKEITEN DURCH
 * Die scheiß Fließkommazahlen (um einen Schritt. Also die möglichst klein halten)
 * Die transformierte lineare Regression arbteitet nicht ganz optimal mit "konvertierten" Zahlen
 * 
 * 
 * ... Aber der GTR ist einfach zu langsam, obwohl es mit ihm besser laufen würde
 **/
