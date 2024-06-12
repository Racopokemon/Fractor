package rk.ramin.fractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

public class PreviewField extends SensorCanvas {

	public static final double MIN_POS = -50, MAX_POS = 50, MIN_ZOOM = 0.1, MAX_ZOOM = 20;
	
	private FractalDrawer drawer;
	private Main app;
	private int maxIterations, base = Integer.MAX_VALUE, steps = 1, atomCount;
	private double smallest, biggest;

	private int highlightValue = -1;

	private double xPos = 0.0, yPos = 0.0, scale = 1.0, rotation = 0.0;
	private boolean focus = false;

	private static final double SIZE = 1.6;
	private double mid = this.widthProperty().get()*0.5, mx, my, scaleFactor;
	private static final double TRANSFORM_TOOLS_RAD = 15, TRANSFORM_TOOLS_DIA = TRANSFORM_TOOLS_RAD*2, TRANSFORM_TOOLS_SPACE = 10.25, TRANSFORM_TOOLS_WHOLE = TRANSFORM_TOOLS_DIA+TRANSFORM_TOOLS_SPACE*2;
	private double transformToolsWhole;
	
	private Color alphaGrey = new Color(Color.GHOSTWHITE.getRed(), Color.GHOSTWHITE.getGreen(), Color.GHOSTWHITE.getBlue(), 0.5),
			alphaBlue = new Color(Color.AZURE.getRed(), Color.AZURE.getGreen(), Color.AZURE.getBlue(), 0.5);

	private boolean redraw;

	private MouseJob job = null;
	private double ddStartX, ddStartY, ddMouseX, ddMouseY;
	private boolean ddToolHover, ddToolScale;

	public PreviewField(double x, double y, int maxIt, Main main, ArrayList<Knob> knobs) {
		super(x, y);
		maxIterations = maxIt;
		app = main;
		scaleFactor = this.getWidth()/(SIZE*2);
		transformToolsWhole = SIZE-TRANSFORM_TOOLS_WHOLE/scaleFactor;
		drawer = new FractalDrawer(1.2/scaleFactor);
		setData(knobs);
	}

	@Override
	protected void update() {
		mx = (mouseX-mid)/scaleFactor;
		my = (mouseY-mid)/scaleFactor;
		redraw = false;
		
		if (job == null) {
			if (!focus && anyMouseDown()) {
				focus = true;
				if (base >= 2) {					
					app.activateDataFields();
					app.setDataFields(xPos, yPos, scale, rotation, false);
				} else {
					app.deactivateDataFields(false);
				}
			}
			if (base >= 2) {
				if (anyMouseDown()) {
					if (leftMouse) {
						job = leftMouseInstance;
					} else if (rightMouse) {
						job = rightMouseInstance;
					} else {
						job = noJobInstance;
					}
					job.initialize();
				} else {
					doBetweenJobs();
				}
			}
		} else {
			if (anyMouseDown()) {
				job.update();
			} else {
				job.finish();
				job = null;
				doBetweenJobs();
			}
		}
		doAlways();
		
		if (redraw) {
			redraw();
		}
	}

	private void doBetweenJobs() {
		//Only called if base < 2
		
        //"hovering"
        boolean lastH = ddToolHover, lastS = ddToolScale;
        if (mx > transformToolsWhole) {
        	ddToolHover = true;
        	ddToolScale = false;
        } else if (mx < -transformToolsWhole) {
        	ddToolHover = true;
        	ddToolScale = true;
        } else {
        	ddToolHover = false;
        }
        if (lastH != ddToolHover || lastS != ddToolScale) {
        	redraw = true;
        }
		
		//scrolling
        if (scroll != 0) {
        	double mouseX, mouseY;
        	if (ddToolHover) {
        		mouseX = 0;
        		mouseY = 0;
        	} else {
        		mouseX = mx;
        		mouseY = my;
        	}
        	if (toggle ^ (ddToolHover && !ddToolScale)) {
        		double change = scroll*15;
        		rotation += change;
	        	if (rotation > 180) rotation -=360; 
	        	else if (rotation < -180) rotation += 360; 
	        	double deltaX = xPos - mouseX, deltaY = yPos - mouseY;
	        	double rot = change*Math.PI/180,
	        			newX = Math.cos(rot)*deltaX-Math.sin(rot)*deltaY,
	        			newY = Math.cos(rot)*deltaY+Math.sin(rot)*deltaX;
	        	xPos = limitToBounds(mouseX+newX, MIN_POS, MAX_POS);
	        	yPos = limitToBounds(mouseY+newY, MIN_POS, MAX_POS);
        	} else {
            	double factor = Math.pow(1.15, scroll), 
            			newScale = limitToBounds(scale*factor, MIN_ZOOM, MAX_ZOOM);
            	factor = newScale/scale;
            	scale = newScale;
            	double deltaX = (xPos-mouseX)*factor, deltaY = (yPos-mouseY)*factor;
            	xPos = limitToBounds(mouseX+deltaX, MIN_POS, MAX_POS);
            	yPos = limitToBounds(mouseY+deltaY, MIN_POS, MAX_POS);
        	}
        	
        	app.setDataFields(xPos, yPos, scale, rotation, false);
        	redraw = true;
        }
  	}
	
	private void doAlways() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void doDrawing(GraphicsContext gc) {
		if (base < 2) {
			gc.setTextAlign(TextAlignment.CENTER);
			gc.setTextBaseline(VPos.CENTER);
			gc.setFill(Color.DIMGRAY);
			gc.fillText("Nothing to do here.", heightProperty().get()*0.5, widthProperty().get()*0.5);
		} else {
			Affine t = gc.getTransform(); //Should have done this already in the knob field -.-
			gc.translate(getWidth()*0.5, getHeight()*0.5);
			gc.scale(scaleFactor, scaleFactor);
			drawer.startCalculation(xPos, yPos, scale, rotation/180*Math.PI, steps, highlightValue, gc);
			//Origin
			gc.setLineWidth(1.0/scaleFactor);
			gc.setStroke(Color.DIMGRAY);
			double step = Math.PI/2, start = rotation/-180*Math.PI, inner = 0.03*scale, outer = 0.013*scale;
//					arcX = xPos-0.02*scale, arcY = yPos-0.02*scale, arcSize = 0.04*scale, arcStart = -rotation+30;
			for (int r = 0; r < 4; r++) {
				double x = Math.sin(start), y = Math.cos(start);
				gc.strokeLine(xPos+x*inner, yPos+y*inner, xPos+x*outer, yPos+y*outer);
				start+=step;
				//gc.strokeArc(arcX, arcY, arcSize, arcSize, arcStart+=90, 30, ArcType.OPEN);
			}

			gc.setTransform(t);
		}
		
		gc.setLineWidth(1.5);
		gc.setGlobalAlpha(base < 2 ? 0.5 : 1);
		
		if (job != leftMouseInstance) {
			if (job == rotateJobInstance || job == scaleJobInstance) {
				gc.setFill(alphaBlue);
				//gc.setStroke(Color.SKYBLUE);
				gc.setStroke(Color.DODGERBLUE);
			} else {
				gc.setFill(alphaGrey);
				gc.setStroke(Color.GHOSTWHITE);
			}
			if (job == rotateJobInstance || ddToolHover && !ddToolScale) {
				//Rotation highlight box at the right
				gc.fillRect(this.getWidth()-TRANSFORM_TOOLS_WHOLE, 0, TRANSFORM_TOOLS_WHOLE, this.getHeight());
				gc.strokeLine(getWidth()-TRANSFORM_TOOLS_WHOLE, 0, getWidth()-TRANSFORM_TOOLS_WHOLE, getHeight());
				if (job == rotateJobInstance) {
					gc.setFill(Color.DODGERBLUE);
					gc.setTextAlign(TextAlignment.CENTER);
					gc.setTextBaseline(VPos.CENTER);
					gc.fillText(String.format(Locale.US, "%.0f°", rotation), getWidth()-TRANSFORM_TOOLS_WHOLE*0.5, limitToBounds(mouseY, 10, getHeight()-10));
				}
			} else if (job == scaleJobInstance || ddToolHover && ddToolScale) {
				//Scaling highlight box at the left
				gc.fillRect(0, 0, TRANSFORM_TOOLS_WHOLE, this.getHeight());
				gc.strokeLine(TRANSFORM_TOOLS_WHOLE, 0, TRANSFORM_TOOLS_WHOLE, getHeight());
				if (job == scaleJobInstance) {
					gc.setFill(Color.DODGERBLUE);
					gc.setTextAlign(TextAlignment.CENTER);
					gc.setTextBaseline(VPos.CENTER);
					gc.fillText(String.format(Locale.US, "%.3f", scale), TRANSFORM_TOOLS_WHOLE*0.5, limitToBounds(mouseY, 10, getHeight()-10));
				}
			}
			
			if (job != rotateJobInstance && job != scaleJobInstance) {
				//Scale knob at the left
				gc.setLineWidth((ddToolHover && ddToolScale) ? 1.5 : 1);
				gc.setFill(Color.GHOSTWHITE);
				gc.setStroke(Color.DIMGRAY);
				gc.fillOval(TRANSFORM_TOOLS_SPACE, mid-TRANSFORM_TOOLS_RAD, TRANSFORM_TOOLS_DIA, TRANSFORM_TOOLS_DIA);
				gc.strokeOval(TRANSFORM_TOOLS_SPACE, mid-TRANSFORM_TOOLS_RAD, TRANSFORM_TOOLS_DIA, TRANSFORM_TOOLS_DIA);
				double ang = 0, step = Math.PI/3, xMid = TRANSFORM_TOOLS_SPACE+TRANSFORM_TOOLS_RAD;
				for (int i = 0; i < 6; i++) {
					double x = Math.sin(ang), y = Math.cos(ang), min, max, md = TRANSFORM_TOOLS_RAD*0.55;
					min = md*0.75;
					max = md*1.25;
					gc.strokeLine(xMid+x*min, mid+y*min, xMid+x*max, mid+y*max);
					ang+=step;
				}
				
				//Rotation knob at the right
				gc.setLineWidth((ddToolHover && !ddToolScale) ? 1.5 : 1);
				gc.setFill(Color.GHOSTWHITE);
				gc.setStroke(Color.DIMGRAY);
				gc.fillOval(this.widthProperty().get()-TRANSFORM_TOOLS_SPACE-TRANSFORM_TOOLS_DIA, mid-TRANSFORM_TOOLS_RAD, TRANSFORM_TOOLS_DIA, TRANSFORM_TOOLS_DIA);
				double fct = 0.6, px = this.widthProperty().get()-TRANSFORM_TOOLS_RAD-TRANSFORM_TOOLS_RAD*fct-TRANSFORM_TOOLS_SPACE, 
						py = mid-TRANSFORM_TOOLS_RAD*fct, 
						w = TRANSFORM_TOOLS_DIA*fct, h = TRANSFORM_TOOLS_DIA*fct;
				gc.strokeOval(this.widthProperty().get()-TRANSFORM_TOOLS_SPACE-TRANSFORM_TOOLS_DIA, mid-TRANSFORM_TOOLS_RAD, TRANSFORM_TOOLS_DIA, TRANSFORM_TOOLS_DIA);
				gc.strokeArc(px, py, w, h, 60, -120, ArcType.OPEN);
				gc.strokeArc(px, py, w, h, 120, 120, ArcType.OPEN);
				gc.setGlobalAlpha(1);
			}
		}

		/*if (focus) {
			gc.setStroke(Color.SKYBLUE);
			gc.setLineWidth(3);
			gc.strokeRect(4, 4, this.widthProperty().get()-8, this.heightProperty().get()-8);
		}*/
		drawRandomPattern(gc);
	}
	
	/**
	 * Here go all calls only for fractal redrawing in.
	 */
	public void redrawFractal(int highlight) {
		highlightValue = highlight;
		redraw();
	}
	
	public void setData(ArrayList<Knob> data) {
		boolean sometingToDo = data.size() >= 2;
		boolean recalc = (sometingToDo ? data.size() : 0) != base;
		double oldS = smallest, oldB = biggest;
		
		if (focus && ((base>=2)!=sometingToDo)) {
			if (sometingToDo) {
				app.activateDataFields();
				app.setDataFields(xPos, yPos, scale, rotation, false);
			} else {
				app.deactivateDataFields(false);
			}
		}
		
		if (sometingToDo) {
			drawer.updatePoints(data);
			base = data.size();
			smallest = 1;
			biggest = 0;
			for (int i = 0; i < base; i++) {
				double s = data.get(i).getScale();
				if (s > biggest) {biggest = s;}
				if (s < smallest) {smallest = s;}
			}
		} else {
			base = 0;
			smallest = 0;
			biggest = 0;
			
			ddToolHover = false;
		}
		if (recalc) {
			recalculateSteps();
		}
		if (recalc || oldS != smallest || oldB != biggest) {
			updateLabels();
		}
	}

	public void setMaxIterations(int iterations) {
		maxIterations = iterations;
		int lastSteps = steps;
		recalculateSteps();
		if (lastSteps != steps) {
			updateLabels();
			this.redraw();
		}
	}
	
	public int getBase() {
		return base;
	}

	/**
	 * Calculates a step count that is as near as possible to the iteration limit. 
	 * Does NOT call updateLabels() itself.
	 */
	private void recalculateSteps() {
		if (base < 2) {
			steps = 0;
		} else {
			double pot = Math.log(maxIterations)/Math.log(base);
			steps = (int) pot; //The very first time that I found a real use for that whole logarithm-topic in math (And only because some developers were too lazy to implement a log(a, b) method)  
		}
		atomCount = (int)Math.round(Math.pow(base,steps));
	}
	
	private void updateLabels() {
		app.updateLabels(atomCount, steps, base, Math.pow(smallest,steps), Math.pow(biggest,steps));
	}
	
	public void unfocus() {
		focus = false;
		redraw();
	}
	
	public void onMouseExit() {
		if (job == null && ddToolHover) {
			ddToolHover = false;
			redraw();
		}
	}
	
	public void onValuesChanged(double x, double y, double s, double r) {
		xPos = x;
		yPos = y;
		scale = s;
		rotation = r;
		redraw();
		//More to come here
	}
	
	
	private LeftMouseJob leftMouseInstance = new LeftMouseJob();
	private RightMouseJob rightMouseInstance = new RightMouseJob();
	private ScaleJob scaleJobInstance = new ScaleJob();
	private RotateJob rotateJobInstance = new RotateJob();
	private NoMouseJob noJobInstance = new NoMouseJob();
	
	private class LeftMouseJob extends MouseJob {

		@Override
		public void initialize() {
			ddMouseX = mx;
			ddMouseY = my;
			ddStartX = xPos;
			ddStartY = yPos;
			if (ddToolHover) {
				if (ddToolScale) {
					job = scaleJobInstance;
				} else {
					job = rotateJobInstance;
				}
				job.initialize();
			}
			redraw = true;
		}

		@Override
		public void update() {
			boolean exit = escape || rightMouse;
			if (!leftMouse) {
				finish();
				job = noJobInstance;
				return;
			}
			double x = ddStartX, y = ddStartY;
			if (exit) {
				job = noJobInstance;
			} else {
				x += mx-ddMouseX;
				y += my-ddMouseY;
				if (!toggle) {
					if (Math.abs(x) < 0.05) x = 0;
					if (Math.abs(y) < 0.05) y = 0;
				}
			}
			x = limitToBounds(x, MIN_POS, MAX_POS);
			y = limitToBounds(y, MIN_POS, MAX_POS);
			xPos = x;
			yPos = y;
			app.setDataFields(x, y, Double.NaN, Double.NaN, false);
			redraw = true;
			if (exit) {
				finish();
			}
		}

		@Override
		public void finish() {
			redraw = true;
		}
	}
	
	private class RightMouseJob extends MouseJob {
		@Override
		public void initialize() {
			if (ddToolHover) {
				if (ddToolScale) {
					scale = 1;
				} else {
					rotation = 0;
				}
			} else {
				xPos = 0;
				yPos = 0;
				if (!toggle) {
					scale = 1;
					rotation = 0;
				}
			}
			app.setDataFields(xPos, yPos, scale, rotation, false);
			job = noJobInstance;
			redraw = true;
		}

		@Override
		public void update() {}

		@Override
		public void finish() {}
	}
	
	private class ScaleJob extends MouseJob {
		@Override
		public void initialize() {
			ddMouseX = scale;
			redraw = true;
			setCursor(Cursor.NONE);
		}

		@Override
		public void update() {
			if (!leftMouse) {
				job = noJobInstance;
				finish();
				return;
			}
			boolean exit = rightMouse || escape;
			double oldS = scale, fct, s;
			if (exit) {
				s = ddMouseX;
				fct = 1;
				job = noJobInstance;
			} else {
				fct = Math.pow(2, ddMouseY-my);
				s = limitToBounds(ddMouseX*fct, MIN_ZOOM, MAX_ZOOM);
			}
			if (s != oldS) {
				scale = s;
				xPos = ddStartX*fct;
				yPos = ddStartY*fct;
				app.setDataFields(xPos, yPos, s, Double.NaN, false);
				redraw = true;
			}
			if (exit) {
				finish();
			}
		}

		@Override
		public void finish() {
			setCursor(Cursor.DEFAULT);
			redraw = true;
		}
	}
	
	private class RotateJob extends MouseJob {
		@Override
		public void initialize() {
			ddMouseX = rotation;
			redraw = true;
			setCursor(Cursor.NONE);
		}

		@Override
		public void update() {
			if (!leftMouse) {
				job = noJobInstance;
				finish();
				return;
			}
			boolean exit = rightMouse || escape;
			double oldR = rotation, newR, delta;
			if (exit) {
				newR = ddMouseX;
				delta = 0;
				job = noJobInstance;
			} else {
				delta = (ddMouseY-my)*scaleFactor; //*scaleFactor for that we can turn the fractal exactly to a half, a quarter etc.
				newR = ddMouseX+delta;
				while(newR > 180) {
					newR-=360;
				}
				while(newR < -180) {
					newR+=360;
				}
			}
			if (oldR != newR) {
				double rad = delta*Math.PI/180, sin = Math.sin(rad), cos = Math.cos(rad);
				xPos = limitToBounds(-sin*ddStartY + cos*ddStartX, MIN_POS, MAX_POS);
				yPos = limitToBounds(sin*ddStartX + cos*ddStartY, MIN_POS, MAX_POS);
				rotation = newR;
				app.setDataFields(xPos, yPos, Double.NaN, newR, false);
				redraw = true;
			}
			if (exit) {
				finish();
			}
		}

		@Override
		public void finish() {
			setCursor(Cursor.DEFAULT);
			redraw = true;
		}
	}
	
	private class NoMouseJob extends MouseJob {
		@Override
		public void initialize() {}
		@Override
		public void update() {}
		@Override
		public void finish() {}
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
	
	/**
	 * Part of the loading process from a FileContent-object.
	 * We do not redraw here - this is done later on in this process, 
	 * when also the KnobField has got its knobs. 
	 */
	public void loadData(FileContent fc) {
		if (job != null) {
			throw new RuntimeException("Error during loading process - in the previewField there is still a job running!");
		}
		double[] pov = fc.getPointOfView();
		xPos = pov[0];
		yPos = pov[1];
		scale = pov[2];
		rotation = pov[3];
	}
	
	/**
	 * Stores the current settings (x,y,s,r) in the given FileContent object
	 */
	public void saveData(FileContent fc) {
		fc.setPointOfView(xPos, yPos, scale, rotation);
	}
}
