package rk.ramin.fractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.sun.corba.se.impl.orbutil.graph.Graph;

import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.Reflection;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * We don't check this, but KnobFields have to be squares (width == height).
 * 
 * In hindsight it was a quite stupid idea to use two different coordinate systems.
 * It would have been better to use only the "inner" system and transform the canvas to it with scale().
 */
public class KnobField extends SensorCanvas {
	
	private ArrayList<Knob> knobs = new ArrayList<Knob>(), visibleKnobs = new ArrayList<Knob>();
	private Knob selected, hover, parent, lastHover;
	private byte hoverType = 0;
	public static final double SCALE = 1.6, RIM_TOP = 1.25, RIM_SIDES = 1.45, KNOB_RADIUS = 12, KNOB_DIAMETER = KNOB_RADIUS*2;
	private static final double ROTATION_FACTOR = 1.5, SCALE_FACTOR = 0.005;
	
	private boolean showLettering = true;
	
	private double mid = this.widthProperty().get()*0.5, scaleFactor;
	private double[] gridSections;
	private Color[] gridColors;
	private Color[][] knobColors = {{Color.GHOSTWHITE,Color.GRAY,Color.LIGHTGRAY, KnobField.changeColorAlpha(Color.LIGHTGRAY, 0.25)},
			{Color.AZURE,Color.DODGERBLUE,Color.DEEPSKYBLUE, KnobField.changeColorAlpha(Color.DEEPSKYBLUE, 0.25)}};
	private Main app;
	
	/**
	 * The job indicates what action we are doing with our mouse.
	 * The first mouse button that is pressed indicates what kind of action we will do.
	 * (E.g. if I start by pressing the right mouse button I will delete everything at my mouse. )
	 * A job ends when no mouse button is pressed anymore.
	 */
	private MouseJob job = null;
	private double downX, downY, initialX, initialY, transformBarOffset;
	private boolean ddStarted, ddPrecise, ddDuplicate, ddRasterize, inversedTransformBar;
	private boolean outOfBoundsMessage = false, mouseAtMessage = false;
	private boolean needsRedraw = false, updateFractal = false, updateFractalData = false;
	private final double oobMessageHideValue = this.heightProperty().get()-35;
	private Color oobMessageMouseColor = new Color(Color.DARKGRAY.getRed(), Color.DARKGRAY.getGreen(), Color.DARKGRAY.getBlue(), 0.2);
	private static final double[] scalePointsOfInterest, scaleSegments;
	private ArrayList<Knob> ddLastKnobs;
	
	private static final double RAD_R = KNOB_RADIUS*1.9, DIA_R = KNOB_DIAMETER*1.9, S_IN = KNOB_RADIUS*1.6, S_OUT = KNOB_RADIUS*1.7;
	
	public static final double POS_MIN = -10, POS_MAX = 10, SCALE_MIN = 0.01, SCALE_MAX = 0.99;
	
	static {
		/*double[] poi = new double[32];
		int index = 0;
		for (int i = 2; i < 8; i++) {
			poi[index++] = 1./i;
			poi[index++] = Math.sqrt(1./i);
		}
		for (int i = 1; i < 20; i++) {
			poi[index++] = i*0.05;
		}
		poi[index++] = 2./3.;
		Arrays.sort(poi);*/
		double[] poi = 
			{0.1, 0.125, 1.0/7, 1.0/6, 0.2, 0.25, 
				0.3, 1.0/3, Math.sqrt(0.125), 
				Math.sqrt(1.0/7), Math.sqrt(1.0/6),
				Math.sqrt(0.2), 0.5, 0.55,
				Math.sqrt(1.0/3), 0.6, 0.65, 2.0/3, Math.sqrt(0.5),
				0.75, 0.8, 0.85, 0.9};
		
		scalePointsOfInterest = poi;
		double[] seg = new double[poi.length-1];
		for (int i = 0; i < seg.length; i++) {
			seg[i] = (poi[i]+poi[i+1])/2;
		}
		scaleSegments = seg;
	}
	
	public KnobField(double w, double h, Main main) {
		super(w, h);
		app = main;
		init();
	}
	
	/**
	 * Called in the constructor - initializes the KnobField (adds Knobs and stuff)
	 */
	private void init() {
		scaleFactor = this.widthProperty().get()/SCALE*0.5;
		Knob.setRadiusOnField(KNOB_RADIUS / scaleFactor);
		double[] gs = {toOuterX(1.5), toOuterX(1), toOuterX(0.5), toOuterX(-0.5), toOuterX(-1), toOuterX(-1.5), mid};
		gridSections = gs;
		Color[] c = {Color.LAVENDER,Color.LIGHTGRAY,Color.LAVENDER,Color.LAVENDER,Color.LIGHTGRAY,Color.LAVENDER, Color.SKYBLUE};
		gridColors = c;
		knobs.add(new Knob(0, 0, Math.sqrt(0.5), 45));
	}
	
	@Override
	protected void update() {
		needsRedraw = false;
		updateFractal = false;
		updateFractalData = false;

		if (job == null) {
			if (anyMouseDown()) {
				//Init the job
				if (leftMouse) {
					job = leftMouseInstance;
				} else if (rightMouse) {
					job = rightMouseInstance;
				} else {
					job = midMouseInstance;
				}
				job.initialize();
				
				hover = null;
				hoverType = 0;
				needsRedraw = true; //At least because hovering ends we need a redraw
			} else {
				//Do things like hovering, space and deleting
				doBetweenJobs();
			}
		} else {
			if (anyMouseDown()) {
				//Do the job
				job.update();
			} else {
				//Finalize the job and reset to null
				job.finish();
				job = null;
				doBetweenJobs();
			}
		}
		
		doAlways();
		
		if (updateFractal) {
			if (updateFractalData) {
				visibleKnobs.clear();
				for (Knob k : knobs) {
					if (!k.getIgnored()) {
						visibleKnobs.add(k);
					}
				}
				app.updateAndRedrawFractal(visibleKnobs, hover == null ? -1 : visibleKnobs.indexOf(hover));
			} else {
				app.updateAndRedrawFractal(null, hover == null ? -1 : visibleKnobs.indexOf(hover));
			}
		}
		if (needsRedraw) {
			redraw();
		}
	}
	
	/**
	 * Called after jobs / between jobs activities.
	 * Does some things that have be done always.
	 */
	private void doAlways() {
		boolean last = mouseAtMessage;
		mouseAtMessage = mouseY > oobMessageHideValue;
		if (outOfBoundsMessage && last != mouseAtMessage) {
			needsRedraw = true;
		}
		if (lastHover != hover) {
			updateFractal = true;
		}
		lastHover = hover;
	}
	
	/**
	 * Does the things that happen when there is no mouse button pressed.
	 */
	private void doBetweenJobs() {
		//Keys. Tabbing trough the knobs.
		if (tab && !lastTab) {
			if (selected == null) {
				selectKnob(knobs.get(knobs.size()-1));
			} else {
				selectKnob(knobs.get(0)); 
				//The seleceted knob is put at the end of the list. 
				//If we are always putting the first element at the end, we will circle trough all knobs in the list.
			}
			//updateFractal = true;
			//updateFractalData = true;
			needsRedraw = true;
		}
		//Keys: Removing knobs
		if (delete && !lastDelete) {
			if (knobs.size() > 1 && selected != null) {
				knobs.remove(selected);
				if (!selected.getIgnored()) {
					updateFractal = true;
					updateFractalData = true;
				}
				if (isOutOfBounds(selected)) {
					resetOutOfBoundsMessage();
				}
				selectKnob(null);
				updateFractal();
				needsRedraw = true;
			}
		}

		//Hovering
		Knob bestKnob = null;
		byte bestValue = 0;
		for (int i = knobs.size()-1; i >= 0; i--) { //Backwards because the knobs at the top should also be selected first
			Knob k = knobs.get(i);
			byte result = k.touchesMouse(toInnerX(mouseX), toInnerY(mouseY));
			if (result > bestValue) {
				bestValue = result;
				bestKnob = k;
				if (result == 4) {
					break;
				}
			}
		}
		if (hoverType != bestValue) {
			hoverType = bestValue;
			needsRedraw = true;
		}
		if (hover != bestKnob) {
			needsRedraw = true;
			hover = bestKnob;
		}
		
		//Scrolling
		if (scroll != 0 && hover != null) {
			boolean doScaling = hoverType == 1;
			if (toggle) doScaling = !doScaling;
			if (doScaling) {
				double s = hover.getScale();
				s += scroll*0.05;
				if (s < SCALE_MIN) {
					s = SCALE_MIN;
				} else if (s > SCALE_MAX) {
					s = SCALE_MAX;
				}
				hover.setScale(s);
				if (selected == hover) app.setDataFields(Double.NaN, Double.NaN, s, Double.NaN, true);
			} else {
				double r = hover.getRotation();
				r+=scroll*15;
                while(r > 180) {
                	r-=360;
                }
                while(r < -180) {
                	r+=360;
                }
		        hover.setRotation(r);
		        if (selected == hover) app.setDataFields(Double.NaN, Double.NaN, Double.NaN, r, true);
			}
			needsRedraw = true;
			updateFractal = true;
			updateFractalData = true;
		}
		/*
		if (scroll != 0) {
			for (Knob k : knobs) {
				k.setNewPosition(k.getXPosition()+0.02, k.getYPosition());
				needsRedraw = true;
				updateFractal = true;
				updateFractalData = true;
			}
		}
		if (knobs.size() == 2) {
			double r1 = knobs.get(0).getRotation()+0.5;
			if (r1 > 180) {
				r1-=360;
			}
			knobs.get(0).setRotation(r1);
			double r2 = knobs.get(1).getRotation()-0.5;
			if (r2 < -180) {
				r2+=360;
			}
			knobs.get(1).setRotation(r2);
			needsRedraw = true;
			updateFractal = true;
			updateFractalData = true;
		}
		*/
	}
	
	@Override
	protected void doDrawing(GraphicsContext gc) {
		gc.setLineWidth(1);
		for (int i = 0; i < gridSections.length; i++) {
			double s = gridSections[i];
			gc.setStroke(gridColors[i]);
			gc.strokeLine(s, 0, s, this.heightProperty().get());
			gc.strokeLine(0, s, this.widthProperty().get(), s);
		}
		gc.setFill(Color.SKYBLUE);
		gc.setTextAlign(TextAlignment.LEFT);
		gc.setTextBaseline(VPos.BASELINE);
		if (showLettering) {
			gc.fillText("1", gridSections[1]+2, mid+10);
			gc.fillText("-1", gridSections[4]+1, mid+10);
			gc.fillText("-1", mid+2, gridSections[1]+10);
			gc.fillText("1", mid+1, gridSections[4]+10);
			}
		for (Knob k : knobs) {
			gc.setGlobalAlpha(k.getIgnored() ? 0.5 : 1);
			if (k == hover) {
				if (hoverType != 3) {
					gc.setLineWidth(1.3);
					drawKnobTransformTools(k, k == selected?knobColors[1]:knobColors[0], gc);
				}
				gc.setLineWidth(1.5);
			} else {
				gc.setLineWidth(1.0);
			}
			if (k == selected) {
				if (job == leftMouseInstance && ddStarted) {
					drawKnobDuringDD(gc);
				} else if (job == rotateJobInstance) {
					drawKnobDuringRotation(gc);
				} else if (job == scaleJobInstance) {
					drawKnobDuringScaling(gc);
				} else {
					drawKnob(k, knobColors[1], gc);
				}
			} else {
				drawKnob(k, knobColors[0], gc);
			}
		}
		gc.setGlobalAlpha(1);
		
		if (outOfBoundsMessage) {
			if (mouseAtMessage) {
				gc.setFill(oobMessageMouseColor);
			} else {
				gc.setFill(Color.DARKGRAY);
			}
			gc.setTextAlign(TextAlignment.LEFT);
			gc.setTextBaseline(VPos.BOTTOM);
			gc.fillText("One or more knobs are out of range. \nYou can use [space] to go trough all knobs and reselect them.", 5, this.heightProperty().get()-4);
		}
		drawRandomPattern(gc);
		/*
		gc.setFill(Color.DARKRED);
		if (tab) {
			gc.fillText("tab", 20, 20);
		}
		if (delete) {
			gc.fillText("delete", 20, 30);
		}
		if (toggle) {
			gc.fillText("toggle", 20, 40);
		}
		if (escape) {
			gc.fillText("escape", 20, 50);
		}
		*/
	}
	
	/**
	 * Some things that selection with mouse and space have in common
	 */
	private void selectKnob(Knob k) {
		selected = k;
		if (k == null) {
			app.deactivateDataFields(true);
		} else {
			app.activateDataFields();
			app.setDataFields(selected.getXPosition(), selected.getYPosition(), selected.getScale(), selected.getRotation(), true);
			if (knobs.indexOf(selected) != knobs.size()-1) {
				knobs.remove(selected); //Object at mouse should be drawn as top most ... without it looks strange
				knobs.add(selected);
			}
		}
	}
	
	private void drawKnob(Knob k, Color[] colors, GraphicsContext gc) {
		double x = toOuterX(k.getXPosition()), y = toOuterY(k.getYPosition()), sx = x-KNOB_RADIUS, sy = y-KNOB_RADIUS, s = k.getScale(), rot = k.getRotation(), r = KNOB_RADIUS*s, d = r*2;
		gc.setFill(colors[0]);
		gc.fillOval(sx, sy, KNOB_DIAMETER, KNOB_DIAMETER);
		if (rot != 0) {gc.setFill(colors[3]);
		gc.fillArc(sx, sy, KNOB_DIAMETER, KNOB_DIAMETER, 90, -rot, ArcType.ROUND);}
		gc.setStroke(colors[2]);
		//gc.fillOval(x-r, y-r, d, d);
		gc.strokeOval(x-r, y-r, d, d);
		gc.strokeLine(x, y, x, y-KNOB_RADIUS);
		if (rot != 0) {double rad = rot*Math.PI/-180; 
		//gc.strokeLine(x, y, x-Math.sin(rad)*KNOB_RADIUS, y-Math.cos(rad)*KNOB_RADIUS);
		drawLineInCircle(rad, x, y, 0, KNOB_RADIUS, gc);}
		gc.setStroke(colors[1]);
		gc.strokeOval(sx, sy, KNOB_DIAMETER, KNOB_DIAMETER);
	}
	
	private void drawKnobTransformTools(Knob k, Color[] colors, GraphicsContext gc) {
		/*
		double x = toOuterX(k.getXPosition()), y = toOuterY(k.getYPosition());
		if (hoverType == 4) {
			gc.setStroke(colors[1]);
			gc.strokeLine(x, y-12, x, y-22);
			gc.strokeLine(x, y+12, x, y+22);
			gc.strokeLine(x-12, y, x-22, y);
			gc.strokeLine(x+12, y, x+22, y);
		} else {
			gc.setStroke(hoverType == 1 ? colors[1] : colors[2]);
			//gc.strokeOval(x-RAD_S, y-RAD_S, DIA_S, DIA_S);
			double step = Math.PI / 16, max = Math.PI*2;
			for (double ang = 0; ang < max; ang+=step) {
				drawLineInCircle(ang, x, y, S_IN, S_OUT, gc);
			}
			gc.setStroke(hoverType == 2 ? colors[1] : colors[2]);
			gc.strokeOval(x-RAD_R, y-RAD_R, DIA_R, DIA_R);
		}
		*/
		double x = toOuterX(k.getXPosition()), y = toOuterY(k.getYPosition());
		if (hoverType == 4) {
			gc.setStroke(colors[1]);
			gc.strokeLine(x, y-12, x, y-22);
			gc.strokeLine(x, y+12, x, y+22);
			gc.strokeLine(x-12, y, x-22, y);
			gc.strokeLine(x+12, y, x+22, y);
		} else {
			double mid = KNOB_RADIUS*1.8, rad = 5;
			gc.setFill(colors[0]);
			gc.setStroke(colors[1]);
			gc.setLineWidth(1.5);
			gc.beginPath();
			gc.arc(x-mid, y, 8, 8, 90, 180);
			gc.arc(x+mid, y, 8, 8, -90, 180);
			gc.closePath();
			gc.fill();
			gc.stroke();
			gc.setLineWidth(1);
			gc.setStroke(hoverType == 2 ? colors[1] : colors[2]); 
			//gc.setLineWidth(hoverType == 2 ? 1.6 : 1.0);
			gc.strokeArc(x+mid-rad, y-rad, 2*rad, 2*rad, 60, -120, ArcType.OPEN);
			gc.strokeArc(x+mid-rad, y-rad, 2*rad, 2*rad, 120, 120, ArcType.OPEN);
			gc.setStroke(hoverType == 1 ? colors[1] : colors[2]);
			//gc.setLineWidth(hoverType == 1 ? 1.6 : 1.0);
			double ang = 0;//Math.PI/4.0;
			for (int i = 0; i < 6; i++) {
				drawLineInCircle(ang, x-mid, y, 3, 5, gc);
				ang+=Math.PI/3;
			}
			//gc.strokeRect(x-KNOB_RADIUS*2.5, y-8, KNOB_DIAMETER*2.5, 2*8);
		}

	}
	
	/**
	 * We know the knob - it is the "selected" one. 
	 */
	private void drawKnobDuringDD(GraphicsContext gc) {
		double x = selected.getXPosition(), y = selected.getYPosition(),
				ox = toOuterX(x), oy = toOuterY(y);
		Color c = knobColors[1][1];
		gc.setLineWidth(1);
		if (ddPrecise) {
			drawCross(selected, 16, c, gc);
			gc.setFill(c);
			gc.setTextBaseline(VPos.CENTER);
			if (x < -RIM_SIDES) {
				gc.setTextAlign(TextAlignment.LEFT);
				gc.fillText(String.format(Locale.US, "x: %.2f\ny: %.2f", x, y), ox+24, oy);
			} else if (x > RIM_SIDES) {
				gc.setTextAlign(TextAlignment.RIGHT);
				gc.fillText(String.format(Locale.US, "x: %.2f\ny: %.2f", x, y), ox-24, oy);
			} else if (y > RIM_TOP) {
				gc.setTextAlign(TextAlignment.CENTER);
				gc.fillText(String.format(Locale.US, "x: %.2f\ny: %.2f", x, y), ox, oy+32);
			} else {
				gc.setTextAlign(TextAlignment.CENTER);
				gc.fillText(String.format(Locale.US, "x: %.2f\ny: %.2f", x, y), ox, oy-32);
			}
		} else {
			drawCross(selected, 3, c, gc);
			gc.setLineWidth(1.5);
			gc.strokeOval(ox-KNOB_RADIUS, oy-KNOB_RADIUS, KNOB_DIAMETER, KNOB_DIAMETER);
		}
	}
	
	
	private void drawKnobDuringRotation(GraphicsContext gc) {
		double x = toOuterX(selected.getXPosition()), y = toOuterY(selected.getYPosition());
		double rot = selected.getRotation(), height = heightProperty().get();
		gc.setFill(knobColors[1][3]);
		gc.fillArc(x-KNOB_RADIUS, y-KNOB_RADIUS, KNOB_DIAMETER, KNOB_DIAMETER, 90, -rot, ArcType.ROUND);
		gc.beginPath();
		gc.arc(x, y, KNOB_RADIUS, KNOB_RADIUS, 90, -rot);
		gc.arc(x, y, KNOB_RADIUS*0.8, KNOB_RADIUS*0.8, 90-rot, rot);
		gc.closePath();
		gc.fill();
		gc.setStroke(knobColors[1][2]);
		gc.setLineWidth(1.5);
		gc.setStroke(knobColors[1][1]);
		gc.strokeOval(x-KNOB_RADIUS, y-KNOB_RADIUS, KNOB_DIAMETER, KNOB_DIAMETER);
		gc.setLineWidth(1.2);
		double ang = Math.PI;
		boolean mark = true;
		for (int i = -4; i < 4; i++) {
			drawLineInCircle(ang, x, y, KNOB_RADIUS, KNOB_RADIUS*(mark?1.4:1.2), gc);
			mark = !mark;
			ang-=Math.PI / 4;
		}
		double negate = inversedTransformBar ? -1 : 1, barMid = downX+5*negate;
		gc.setTextAlign(inversedTransformBar ? TextAlignment.RIGHT : TextAlignment.LEFT);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFill(knobColors[1][1]);
		gc.fillText(String.format(Locale.US, "%.0f°", rot), barMid+13*negate, downY<10?10:downY>height-10?height-10:downY);
		gc.setLineWidth(1);
		gc.strokeLine(barMid, 0, barMid, height);
		gc.strokeLine(barMid, downY, barMid+negate*6, downY);
		double start = y-(initialY % 90)/ROTATION_FACTOR, bigS = 90/ROTATION_FACTOR, smallS = 45/ROTATION_FACTOR;
		double step = start;
		while (step < height) {
			gc.strokeLine(barMid-7*negate, step, barMid, step);
			gc.strokeLine(barMid-4*negate, step+smallS, barMid, step+smallS);
			step+=bigS;
		}
		step=start;
		while (step > 0) {
			gc.strokeLine(barMid-7*negate, step, barMid, step);
			gc.strokeLine(barMid-4*negate, step+smallS, barMid, step+smallS);
			step-=bigS;
		}
		/*
		private static final double ROT_R_CIRCLE = KNOB_RADIUS*1.4, ROT_D_CIRCLE = KNOB_DIAMETER*1.4;
		double x = toOuterX(selected.getXPosition()), y = toOuterY(selected.getYPosition());
		double init = initialX*Math.PI/180, rot = selected.getRotation();
		gc.setFill(knobColors[1][3]);
		gc.fillArc(x-ROT_R_CIRCLE, y-ROT_R_CIRCLE, ROT_D_CIRCLE, ROT_D_CIRCLE, 90, -rot, ArcType.ROUND);
		gc.beginPath();
		gc.arc(x, y, ROT_R_CIRCLE, ROT_R_CIRCLE, 90, -rot);
		gc.arc(x, y, KNOB_RADIUS*0.9, KNOB_RADIUS*0.9, 90-rot, rot);
		gc.closePath();
		gc.fill();
		gc.setStroke(knobColors[1][2]);
		gc.setLineWidth(1.5);
		gc.setStroke(knobColors[1][1]);
		gc.strokeOval(x-ROT_R_CIRCLE, y-ROT_R_CIRCLE, ROT_D_CIRCLE, ROT_D_CIRCLE);
		gc.setLineWidth(1.2);
		gc.strokeArc(x-KNOB_RADIUS*2.2, y-KNOB_RADIUS*2.2, KNOB_DIAMETER*2.2, KNOB_DIAMETER*2.2, initialX+90, -rot, ArcType.OPEN);
		drawLineInCircle(init-selected.getRotationInRadians(), x, y, KNOB_RADIUS*2.0, KNOB_RADIUS*2.4, gc);
		drawLineInCircle(init, x, y, KNOB_RADIUS*2.0, KNOB_RADIUS*2.4, gc);
		double ang = 0;
		boolean mark = false;
		for (int i = 0; i < 8; i++) {
			ang+=Math.PI / 4;
			drawLineInCircle(init+ang, x, y, ROT_R_CIRCLE, KNOB_RADIUS*(mark?1.9:1.6), gc);
			mark = !mark;
		}
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFill(knobColors[1][1]);
		gc.fillText(String.format(Locale.US, "%.0f°", rot), x, y);
		*/
	}
	
	private void drawKnobDuringScaling(GraphicsContext gc) {
		double x = toOuterX(selected.getXPosition()), y = toOuterY(selected.getYPosition()), height = heightProperty().get(), scl = selected.getScale();
		gc.setStroke(knobColors[1][1]);
		gc.setLineWidth(1.5);
		gc.strokeOval(x-KNOB_RADIUS, y-KNOB_RADIUS, KNOB_DIAMETER, KNOB_DIAMETER);
		gc.setLineWidth(1.0);
		gc.strokeOval(x-KNOB_RADIUS*scl, y-KNOB_RADIUS*scl, KNOB_DIAMETER*scl, KNOB_DIAMETER*scl);
		//Definitely not copied and pasted from rotation
		double negate = inversedTransformBar ? -1 : 1, barMid = downX+5*negate, end = transformBarOffset+1/SCALE_FACTOR;
		gc.setTextAlign(inversedTransformBar ? TextAlignment.RIGHT : TextAlignment.LEFT);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFill(knobColors[1][1]);
		gc.fillText(String.format(Locale.US, "%.3f", scl), barMid+13*negate, downY<10?10:downY>height-10?height-10:downY);
		gc.setLineWidth(1);
		gc.strokeLine(barMid, transformBarOffset, barMid, end);
		gc.strokeLine(barMid, downY, barMid+negate*6, downY);
		//gc.strokeLine(barMid-negate*8, transformBarOffset, barMid, transformBarOffset);
		//gc.strokeLine(barMid-negate*12, end, barMid, end);
		double step = 0.25/SCALE_FACTOR, offset = 0;
		double size = 2;
		for (int i = 0; i < 5; i++) {
			gc.strokeLine(barMid-negate*size, transformBarOffset+offset, barMid, transformBarOffset+offset);
			offset += step;
			size*=1.5; //Okay - our scaling is no exponential one - but this way it simply looks better 
		}
	}
	
	private void drawLineInCircle(double angle, double x, double y, double min, double max, GraphicsContext gc) {
		double fx = Math.sin(angle), fy = Math.cos(angle);
		gc.strokeLine(x-fx*min, y-fy*min, x-fx*max, y-fy*max);
	}
	
	private void drawCross(Knob k, double size, Color c, GraphicsContext gc) {
		double x = toOuterX(k.getXPosition()), y = toOuterY(k.getYPosition());
		gc.setStroke(c);
		gc.strokeLine(x+size, y, x-size, y);
		gc.strokeLine(x, y+size, x, y-size);
	}
	
	public void onValuesChanged(double x, double y, double s, double r) {
		boolean needsRedraw = true;
		//hover = null; Just causes some errors. Does not make sense
		if (selected == null) {
			throw new RuntimeException("New values were inserted in the text fields ... but they might be disabled because there is no knob selected!");
		}
		if (job != null) {
			throw new RuntimeException("Whats goin on here? New values were inserted in the text fields but we are still running a job :o");
		}
		selected.setNewValues(x, y, s, r);
		if (outOfBoundsMessage) {
			if (resetOutOfBoundsMessage()) {
				needsRedraw = true;
			}
		} else {
			outOfBoundsMessage = isOutOfBounds(selected);
		}
		updateFractal();
		app.updateAndRedrawFractal(knobs, -1);
		if (needsRedraw) {
			redraw();
		}
	}
	
	private boolean isOutOfBounds(Knob k) {
		return Math.abs(k.getXPosition()) > SCALE || Math.abs(k.getYPosition()) > SCALE;
	}
	
	/**
	 * True if we set successfully to false
	 */
	private boolean resetOutOfBoundsMessage() {
		for (Knob k : knobs) {
			if (isOutOfBounds(k)) {
				return false;
			}
		}
		outOfBoundsMessage = false;
		return true;
	}
	
	private void updateFractal() {
		//Contacting the FractalField etc
	}
	
	private double rasterizeForCanvasX(double x) {
		return toInnerX(Math.round(toOuterX(x)));
	}
	private double rasterizeForCanvasY(double y) {
		return toInnerY(Math.round(toOuterY(y)));
	}
		
	/**
	 * Converts the coordinates used by drawing and mousePositions 
	 * to the coordinates this position is pointing at.
	 */
	private double toInnerX(double x) {
		return (x-mid)/scaleFactor;
	}
	
	/**
	 * Converts the coordinates used by drawing and mousePositions 
	 * to the coordinates this position is pointing at.
	 */
	private double toInnerY(double y) {
		return (y-mid)/-scaleFactor;
	}
	
	/**
	 * Converts the internal coordinates that are drawn as grid onto this canvas 
	 * to the outer coordinates that are used to draw on this canvas.
	 */
	private double toOuterX(double x) {
		return x*scaleFactor+mid;
	}
	
	/**
	 * Converts the internal coordinates that are drawn as grid onto this canvas 
	 * to the outer coordinates that are used to draw on this canvas.
	 */
	private double toOuterY(double y) {
		return y*-scaleFactor+mid;
	}
	
	/**
	 * The arrayList containing all knobs.
	 * Should be only used in the construction of the Main window.
	 * In all other cases we update everybody who needs to know about them ourself if there is a need for it.
	 */
	public ArrayList<Knob> getKnobs() {
		return knobs;
	}
	
	public void onMouseExit() {
		if (job == null && hover != null) {
			if (!hover.getIgnored()) {
				app.updateAndRedrawFractal(null, -1);
			}
			hover = null;
			lastHover = null;
			redraw();
		}
	}
	
	public void unfocus() {
		if (job != null) {
			throw new RuntimeException("We were called to unfocus ... but there is still any kind of dd process running.");
		}
		hover = null;
		selected = null;
		redraw();
	}
	
	private double limitToBounds(double value, double min, double max) {
		if (value > max) {
			return max;
		} else if (value < min) {
			return min;
		} else {
			return value;
		}
	}
	
	//--------------------------------------------------------------------------------------
	//|||********************************************************************************|||
	//--------------------------------------------------------------------------------------
	
	private MouseJob leftMouseInstance = new LeftMouseJob();
	private MouseJob rightMouseInstance = new RightMouseJob();
	private MouseJob midMouseInstance = new MidMouseJob();
	private MouseJob noJobInstance = new NoJob();
	private MouseJob rotateJobInstance = new RotateJob();
	private MouseJob scaleJobInstance = new ScaleJob();
	
	private class LeftMouseJob extends MouseJob {

		@Override
		public void initialize() {
			parent = null; //Not very necessary ...
			selectKnob(hover);
			if (selected == null) {
				job = noJobInstance;
			} else {
				if (!selected.getIgnored()) {
					updateFractal = true;
					updateFractalData = true;
				}
				if (hoverType == 2) {
					job = rotateJobInstance;
					rotateJobInstance.initialize();
					return;
				} else if (hoverType == 1) {
					job = scaleJobInstance;
					scaleJobInstance.initialize();
					return;
				}
				ddDuplicate = toggle;
				ddRasterize = true;
				initialX = selected.getXPosition();
				initialY = selected.getYPosition();
				ddPrecise = hoverType == 4;
				ddStarted = ddPrecise && !ddDuplicate;
				if (ddStarted) {
					downX = rasterizeForCanvasX(selected.getXPosition()-toInnerX(mouseX));
					downY = rasterizeForCanvasY(selected.getYPosition()-toInnerY(mouseY));
					setCursor(Cursor.NONE);
				} else {
					downX = mouseX;
					downY = mouseY;
				}
			}
		}

		@Override
		public void update() {
			//At this point we will be in a dd process or there are chances that one is started
			if (!leftMouse) {
				this.finish();
				job = noJobInstance;
				return;
			}
			if (!ddStarted) {
				if (rightMouse || escape) {
					job = noJobInstance; //Quite seldom. Dd is exited before we actually start.
				} else if (Math.abs(mouseX-downX)+Math.abs(mouseY-downY)>4) {
					downX = rasterizeForCanvasX(selected.getXPosition()-toInnerX(downX));
					downY = rasterizeForCanvasY(selected.getYPosition()-toInnerY(downY));
					ddStarted = true;
					if (ddDuplicate) {
						ddRasterize = false;
						if (ddPrecise) {
							setCursor(Cursor.NONE);
						}
						Knob k = new Knob(selected);
						knobs.add(k);
						parent = selected;
						selected = k;

						//no selectKnob necessary here
					}
				}
			}
			if (ddStarted) {
				double x, y, oldX = selected.getXPosition(), oldY = selected.getYPosition();
				boolean exit = false;
				if (rightMouse || escape) {
					x = initialX;
					y = initialY;
					exit = true;
					setCursor(Cursor.DEFAULT);
					if (ddDuplicate) {
						knobs.remove(selected);
						selected = parent;
					} else if (oldX == x && oldY == y) {
						oldX=Double.NaN;
					}
				} else {
					x = toInnerX(mouseX)+downX;
					y = toInnerY(mouseY)+downY;
					if (toggle) {
					if (ddRasterize) {
						x = Math.round(x*20)*0.05;
						y = Math.round(y*20)*0.05;
					}} else {ddRasterize = true;}
					x = limitToBounds(x, POS_MIN, POS_MAX);
					y = limitToBounds(y, POS_MIN, POS_MAX);
				}
				if (x != oldX || y != oldY) {
					selected.setNewPosition(x, y);
					app.setDataFields(x, y, Double.NaN, Double.NaN, true);
					if (!selected.getIgnored()) {
						updateFractal = true;
						updateFractalData = true;
					}
					needsRedraw = true;
				}
				
				if (exit) {
					job = noJobInstance;
				}
			}
		}

		@Override
		public void finish() {
			if (ddStarted) { //Started == true implies that selected != null
				ddStarted = false;
				//if (selected != null) {
					if (isOutOfBounds(selected)) {
						outOfBoundsMessage = true;
					} else {
						if (resetOutOfBoundsMessage()) {
							needsRedraw = true;
						}
					}
				//}
			}
			setCursor(Cursor.DEFAULT);
		}
	}
	
	private class RotateJob extends MouseJob {

		@Override
		public void initialize() {
			/*
			initialX = selected.getRotation() - getAngle();
			initialY = selected.getRotation();
			*/
			setCursor(Cursor.NONE);
			initialX = mouseY;
			initialY = selected.getRotation();
			downX = toOuterX(selected.getXPosition())+KNOB_RADIUS*1.8;
			downY = toOuterY(selected.getYPosition());
			
			if (downX+42 > widthProperty().get()) {downX-=KNOB_DIAMETER*1.8; inversedTransformBar=true;} 
			else {inversedTransformBar=false;}
		}

		@Override
		public void update() {
			if (!leftMouse) {
				job = noJobInstance;
				finish();
				return;
			}
			double rot = initialY+(mouseY-initialX)*ROTATION_FACTOR, lastAng = selected.getRotation();
			if (rightMouse || escape) {
				rot = initialY;
				job = noJobInstance;
				finish();
			} else {
				if (toggle) {
					rot = Math.round(rot / 15)*15d;
				}
				
				downY = (rot-initialY)/ROTATION_FACTOR+toOuterY(selected.getYPosition());
				//downY = toOuterY(selected.getYPosition())+(mouseY-initialX);//Just for drawing
				
				rot = fitRotation(rot);
			}
			if (lastAng != rot) {
				selected.setRotation(rot);
				needsRedraw = true;
				if (!selected.getIgnored()) {
					updateFractal = true;
					updateFractalData = true;
				}
				app.setDataFields(Double.NaN, Double.NaN, Double.NaN, rot, true);
			}
		}

		@Override
		public void finish() {
			needsRedraw = true;
			setCursor(Cursor.DEFAULT);
		}
		
		/*private double getAngle() {
			return 180/Math.PI*Math.atan2(toInnerX(mouseX)-selected.getXPosition(), toInnerY(mouseY)-selected.getYPosition());
		}*/
		
		private double fitRotation(double rot) {
			if (rot > 180) {
				while (rot > 180) {rot-=360;}
			} else if (rot <= -180) {
				while (rot <= -180) {rot+=360;}
			}
			return rot;
		}
		
	}
	
	private class ScaleJob extends MouseJob {

		@Override
		public void initialize() {
			initialX = selected.getScale();
			initialY = mouseY;
			setCursor(Cursor.NONE);
			
			downX = toOuterX(selected.getXPosition())-KNOB_RADIUS*1.8;
			transformBarOffset = toOuterY(selected.getYPosition())-(initialX)/SCALE_FACTOR;
			if (transformBarOffset < 20) {
				transformBarOffset = 20;
			} else if (transformBarOffset+1/SCALE_FACTOR > heightProperty().get()-20) {
				transformBarOffset = heightProperty().get()-20-1/SCALE_FACTOR;
			}
			downY = transformBarOffset + initialX/SCALE_FACTOR;
			
			if (downX < 55) {downX+=KNOB_DIAMETER*1.8; inversedTransformBar=false;} 
			else {inversedTransformBar=true;}
		}

		@Override
		public void update() {
			if (!leftMouse) {
				finish();
				job = noJobInstance;
				return;
			}
			double scale = initialX+(mouseY-initialY)*SCALE_FACTOR;
			if (scale > SCALE_MAX) {scale = SCALE_MAX;} else if (scale < SCALE_MIN) scale = SCALE_MIN;
			if (rightMouse || escape) {
				scale = initialX;
				job = noJobInstance;
				finish();
			}
			if (toggle) {
				int i = 0;
				while (scaleSegments[i]<scale && ++i<scaleSegments.length) {}
				scale = scalePointsOfInterest[i];
			}
			downY = scale/SCALE_FACTOR+transformBarOffset;//(scale-initialX)/SCALE_FACTOR+toOuterY(selected.getYPosition());
			if (selected.getScale() != scale) {
				if (!selected.getIgnored()) {
					updateFractal = true;
					updateFractalData = true;
				}
				needsRedraw = true;
			}
			selected.setScale(scale);
			app.setDataFields(Double.NaN, Double.NaN, scale, Double.NaN, true);
		}

		@Override
		public void finish() {
			setCursor(Cursor.DEFAULT);
			needsRedraw = true;
		}
		
	}
	
	private class RightMouseJob extends MouseJob {
		@Override
		public void initialize() {
			if (hoverType == 1 || hoverType == 2) {
				if (hoverType == 1) {
					hover.setScale(0.5);
					app.setDataFields(Double.NaN, Double.NaN, 0.5, Double.NaN, true);
				} else {
					hover.setRotation(0);
					app.setDataFields(Double.NaN, Double.NaN, Double.NaN, 0, true);
				}
				job = noJobInstance;
				needsRedraw = true;
				updateFractal = true;
				updateFractalData = true;
				return;
			}
			//-------------------//
			if (knobs.size() < 2) {
				job = noJobInstance;
				return;
			}
			initialX = mouseX;
			initialY = mouseY;
			ddStarted = false;
			if (hover != null) {
				boolean del;
				if (hoverType != 3) { //Feels strange to remove not the top most knob
					del = false;
					for (int i = knobs.size()-1; i >= 0; i--) {
						if (knobs.get(i).touchesMouse(toInnerX(mouseX), toInnerY(mouseY)) >= 3) {
							hover = knobs.get(i);
							del = true; 
							break;
						}
					}
				} else {
					del = true;
				}
				if (del) {
					knobs.remove(hover);
					if (!hover.getIgnored()) {
						updateFractal = true;
						updateFractalData = true;
					}
				}
				if (knobs.size() < 2) {
					job = noJobInstance;
					return;
				}
			}
		}

		@Override
		public void update() {
			if (!rightMouse) {
				finish();
				job = noJobInstance;
				return;
			}
			if (!ddStarted) {
				if (Math.abs(mouseX-initialX)+Math.abs(mouseX-initialX) > 1) {
					ddStarted = true;
				}
			}
			if (ddStarted) {
				double mx = toInnerX(mouseX), my = toInnerY(mouseY);
				Knob k;
				for (int i = knobs.size()-1; i >= 0; ) {
					k = knobs.get(i);
					if (k.touchesMouse(mx, my) >= 3) {
						knobs.remove(i);
						if (!k.getIgnored()) {
							updateFractal = true;
							updateFractalData = true;
						}
						needsRedraw = true;
						
						if (i == knobs.size()) {i--;}
						if (knobs.size() < 2) {
							job = noJobInstance;
							return;
						}
					} else {
						i--;
					}
				}
			}
			if (knobs.isEmpty()) {
				throw new RuntimeException("HOW COULD IT HAPPEN THAT WE DELETED ALL KNOBS?");
				//knobs.add(new Knob(0, 0, 0.5, 0));
			}
		}

		@Override
		public void finish() {}
	}
	
	private class MidMouseJob extends MouseJob {

		@Override
		public void initialize() {
			initialX = mouseX;
			initialY = mouseY;
			ddStarted = false;
			double x = toInnerX(mouseX), y = toInnerY(mouseY);
			if (hover != null && hoverType != 3) {
				hover = null;
				for (int i = knobs.size()-1; i >= 0; i--) {
					if (knobs.get(i).touchesMouse(x, y) >= 3) {
						hover = knobs.get(i);
						break;
					}
				}
			}
			if (hover != null) {
				hover.toggleIgnored();
				needsRedraw = true;
				updateFractal = true;
				updateFractalData = true;
			}
			//Var recycling
			parent = hover;
		}

		@Override
		public void update() {
			if (!midMouse) {
				finish();
				job = noJobInstance;
				return;
			}
			if (!ddStarted) {
				if (Math.abs(mouseX-initialX)+Math.abs(mouseY-initialY)>1) {
					ddStarted = true;
					ddLastKnobs = new ArrayList<Knob>();
					if (parent != null) {ddLastKnobs.add(parent);}
				}
			}
			if (ddStarted) {
				double x = toInnerX(mouseX), y = toInnerY(mouseY);
				ArrayList<Knob> upToDate = new ArrayList<Knob>();
				for (Knob k : knobs) {
					if (k.touchesMouse(x, y) >= 3) {
						upToDate.add(k);
						if (!ddLastKnobs.contains(k)) {
							k.toggleIgnored();
							needsRedraw = true;
							updateFractal = true;
							updateFractalData = true;
						}
					}
				}
				ddLastKnobs = upToDate;
			}
		}

		@Override
		public void finish() {
			ddLastKnobs = null;
		}
	}
	
	private class NoJob extends MouseJob {
		@Override
		public void initialize() {}
		@Override
		public void update() {}
		@Override
		public void finish() {}
	}

	protected static Color changeColorAlpha(Color c, double alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}
	
	/**
	 * Stores the current data (knobs and their data) in the given FileContent instance.
	 */
	public void saveData(FileContent fc) {
		fc.setKnobs(knobs);
	}
	
	/**
	 * Loads all data from the given FileContent instance. 
	 * After that the PreviewField gets updated with this new knob values.
	 * (So you should have told it already its new values)
	 */
	public void loadData(FileContent fc) {
		if (job != null) {
			throw new RuntimeException("Error during loading process - in the knobField there is still a job running!");
		}
		hover = null;
		selected = null;
		Knob[] kbs = fc.getKnobs();
		knobs.clear();
		visibleKnobs.clear();
		outOfBoundsMessage = false;
		for (Knob k : kbs) {
			knobs.add(k);
			if (!k.getIgnored()) {
				visibleKnobs.add(k);
			}
			if (isOutOfBounds(k)) {
				outOfBoundsMessage = true;
			}
		}
		app.updateAndRedrawFractal(visibleKnobs, -1);
		redraw();
	}

}
