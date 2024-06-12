package rk.ramin.fractor;

import java.util.Collection;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

/**
 * Extends canvas that provides some variables like mouseX, mouseY and leftMouse
 * Furthermore on every mouse event the update() method is called. 
 * The redraw() method does the primary redrawing and should be used to start a redrawing, 
 * The empty method doDrawing() is called by redraw() and should be used for specific things to be rendered. 
 * 
 * First it was planned to do "tabs" with the tab key or the arrow keys. 
 * Then I found out that FX already used that keys to jump between different nodes ... 
 * and to change this behavior was a bit too extensive for being used hardly. 
 * (http://stackoverflow.com/questions/19625218/javafx-scrolling-vs-focus-traversal-with-arrow-keys)
 */
public class SensorCanvas extends Canvas {

	protected double mouseX = -5, mouseY = -5, scroll = 0;
	protected boolean leftMouse, rightMouse, escape, toggle, delete, tab, lastLeft, lastRight, lastEscape, lastToggle, lastDelete, lastTab, midMouse, lastMid;
	
	private static KeyCode[] relevantKeys = {KeyCode.ESCAPE, KeyCode.DELETE, KeyCode.BACK_SPACE, KeyCode.SPACE};
	private boolean[] pressedKeys = new boolean[relevantKeys.length];
	
	public SensorCanvas() {
		super();
		init();
	}

	public SensorCanvas(double x, double y) {
		super(x, y);
		init();
	}
	
	/**
	 * Called after every event such as mouse moves and clicks.
	 * Is most times followed by a redraw() call on this and / or on the fractal canvas.
	 */
	protected void update() {
		
	}
	
	/**
	 * Is called in the constructor and initializes the canvas.
	 * Adds listeners and stuff.
	 */
	private void init() {
		EventHandler<MouseEvent> h = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				updateMouseInput(event);
			}
		};
		this.setOnMouseMoved(h);
		this.setOnMouseDragged(h);
		this.setOnMouseReleased(h);
		this.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				requestFocus(); //This first because the text fields should be finished before we update the canvas
				updateMouseInput(event); 
			}
		});
		this.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent ke) {
				updateLastInput();
				toggle = ke.isAltDown() || ke.isControlDown() || ke.isShiftDown();
				
				KeyCode kc = ke.getCode();
				for (int i = 0; i < relevantKeys.length; i++) {
					if (relevantKeys[i] == kc) {
						pressedKeys[i] = true;
						translateKeys();
						update();
						return;
					}
				}
				if (toggle != lastToggle) update();
			}
		});
		this.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent ke) {
				updateLastInput();
				toggle = ke.isAltDown() || ke.isControlDown() || ke.isShiftDown();
				
				KeyCode kc = ke.getCode();
				for (int i = 0; i < relevantKeys.length; i++) {
					if (relevantKeys[i] == kc) {
						pressedKeys[i] = false;
						translateKeys();
						update();
						return;
					}
				}
				if (toggle != lastToggle) update();
			}
		});
		this.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				updateLastInput();
				toggle = event.isAltDown() || event.isControlDown() || event.isShiftDown();
				scroll = event.getDeltaY() / event.getMultiplierY();
				//Interestingly scrolling while pressing shift causes scrolling on the x-axis 
				//and the values for y-scrolling are 0. In my case 
				//mouse wheel scrolling in general should be detected, 
				//no matter whether shift is pressed or not.
				//So I choosed this solution:
				if (scroll == 0) scroll = event.getDeltaX() / event.getMultiplierX();
				update();
			}
		});
		this.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				onMouseExit();
			}
		});
	}
	
	private void translateKeys() {
		escape = pressedKeys[0];
		delete = pressedKeys[1] || pressedKeys[2];
		tab = pressedKeys[3];
	}
	
	private void updateLastInput() {
		scroll = 0;
		lastEscape = escape;
		lastTab = tab;
		lastToggle = toggle;
		lastDelete = delete;
		lastLeft = leftMouse;
		lastRight = rightMouse;
		lastMid = midMouse;
	}
	
	private void updateMouseInput(MouseEvent me) {
		updateLastInput();
		mouseX = me.getX();
		mouseY = me.getY();
		leftMouse = me.isPrimaryButtonDown();
		rightMouse = me.isSecondaryButtonDown();
		midMouse = me.isMiddleButtonDown();
		toggle = me.isAltDown() || me.isControlDown() || me.isShiftDown(); 
		update();
	}
	
	/**
	 * Redraws the canvas.
	 * Does no further updates or new calculations - call update() for this.
	 * (This also might include a redraw if necessary)
	 * 
	 * Do not override this method for specific things to render.
	 * Use doDrawing() instead.
	 */
	public void redraw() {
		GraphicsContext gc = this.getGraphicsContext2D();
		gc.clearRect(0, 0, this.widthProperty().getValue(), this.heightProperty().getValue());
		//gc.setFill(Color.GHOSTWHITE);
		//gc.fillRect(0, 0, this.widthProperty().get(), this.heightProperty().get());
		doDrawing(gc);
		gc.setStroke(Color.DIMGRAY);
		gc.setLineWidth(2);
		gc.strokeRect(1, 1, this.widthProperty().getValue()-2, this.heightProperty().getValue()-2);
		gc.restore();
	}

	/**
	 * Override this to draw specific things on the canvas.
	 * Without overriding the cursor position and the mouse button states are shown on canvas.
	 */
	protected void doDrawing(GraphicsContext gc) {
		if (leftMouse) {
			gc.setStroke(Color.CYAN);
			gc.strokeOval(mouseX-6, mouseY-6, 12, 12);
		} else { 
			gc.setStroke(Color.CORNFLOWERBLUE);
			gc.strokeOval(mouseX-8, mouseY-8, 16, 16);
		}
		if (rightMouse) {
			gc.setFill(Color.NAVY);
			gc.fillOval(mouseX+8, mouseY-13, 5, 5);
		}
	}
	
	protected void drawRandomPattern(GraphicsContext gc) {
		gc.setFill(Color.DIMGRAY);
		gc.setStroke(Color.DIMGRAY);
		gc.setLineWidth(1);
		for (int i = 10; i < 34; i+=6) {
			for (int j = 10; j < 34; j+=6) {
				if (Math.random()>0.666) {
					gc.fillOval(i, j, 5, 5);
				} else if (Math.random()>0.5) {
					gc.strokeOval(i, j, 5, 5);
				}
			}
		}
	}
	
	public void onMouseExit() {}
	
	protected boolean anyMouseDown() {
		return leftMouse || rightMouse || midMouse;
	}
	
	public void unfocus() {}; 
}
