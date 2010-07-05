/*
 * Copyright (C) 2010 Christian Gawron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cgawron.agoban.view;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

import de.cgawron.agoban.R;
import de.cgawron.agoban.GobanEvent;
import de.cgawron.agoban.GobanEventListener;

import java.util.List;
import java.util.ArrayList;

import demo.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import demo.multitouch.controller.MultiTouchController.PointInfo;
import demo.multitouch.controller.MultiTouchController.PositionAndScale;

/**
 * A {@link View} to be used for {@link de.cgawron.go.Goban} 
 *
 */
public class GobanView extends View implements MultiTouchObjectCanvas<Object> 
{
    private Goban goban;
    private GobanRenderer renderer;
    private List<GobanEventListener> listeners = new ArrayList<GobanEventListener>();
    private GobanEventHandler gobanEventHandler ;
    private Point selection;

    private float xOff = 0.0f, yOff = 0.0f, relativeScale = 1.0f;

    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public GobanView(Context context) {
        super(context);
	//mOrientationListener = new MyOrientationEventListener(context);
        initGobanView();
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public GobanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGobanView();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.GobanView);

        // Note, if you only care about supporting a single color, that you
        // can instead call a.getColor() and pass that to setTextColor().
        //setTextColor(a.getColor(R.styleable.GobanView_textColor, 0xFF000000));

        a.recycle();
    }

    private final void initGobanView() {
	// Create Event handler
	gobanEventHandler = new GobanEventHandler(this, getResources());

	renderer = new GobanRenderer(this);
    }

    /**
     * Sets the text to display in this label
     * @param text The text to display. This will be drawn as one line.
     */
    public void setGoban(Goban goban) {
        this.goban = goban;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
	int width = MeasureSpec.getSize(widthMeasureSpec);
	int height = MeasureSpec.getSize(heightMeasureSpec);
	int min = width < height ? width : height;
	
	Log.d("Goban", "onMeasure: " + width + ", " + height);
	setMeasuredDimension(min, min);
	invalidate();
    }

    /**
     * Render the Goban
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
	Rect bounds = new Rect();
	boolean clip = canvas.getClipBounds(bounds);
	Log.d("Goban", "onDraw, clip=" + clip);

	int size = goban.getBoardSize();
	int width = getWidth();
	int height = getHeight();
	float min = width < height ? width : height;
	Log.d("Goban", "onDraw " + min);

	canvas.scale(min / size, min / size);
	canvas.translate(-0.5f, -0.5f);
	renderer.render(goban, canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	if (!gobanEventHandler.onTouchEvent(event))
	    return super.onTouchEvent(event);
	else
	    return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
	if (!gobanEventHandler.onTrackballEvent(event))
	    return super.onTrackballEvent(event);
	else
	    return true;
    }
    
    
    @Override
    public Object getDraggableObjectAtPoint(PointInfo pt) {
	// We do not support dragging on a goban
	return null;
    }

    @Override
    public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut) {
	// We start at 0.0f each time the drag position is replaced, because we just want the relative drag distance 
	objPosAndScaleOut.set(xOff, yOff, relativeScale);
    }

    @Override
    public void selectObject(Object obj, PointInfo pt) {
	int width = getWidth();
	int height = getHeight();
	int size = goban.getBoardSize();
	Log.d("Goban", String.format("selectObject: %f(%d), %f(%d) obj=%s", pt.getX(), width, pt.getY(), height, (obj == null ? "null" : obj.toString())));
	
	int bx = (int) (size*pt.getX()/width);
	int by = (int) (size*pt.getY()/height);
	Log.d("Goban", String.format("selectObject: (%d, %d)", bx, by));
	
	setSelection(new Point(bx, by));
	//invalidate(bx, by, bx+1, by+1);
	invalidate();
    }
	
    @Override
    public boolean setPositionAndScale(Object obj, PositionAndScale update, PointInfo touchPoint) {
	// Get new offsets and coords
	float newXOff = update.getXOff();
	float newYOff = update.getYOff();
	float newRelativeScale = update.getScale();
	Log.d("Goban", "multitouch: " + newRelativeScale);

	return true;
    }

    public int getBoardSize()
    {
	return goban.getBoardSize();
    }

    public void addGobanEventListener(GobanEventListener listener) 
    {
	listeners.add(listener);
    }

    public void fireGobanEvent(GobanEvent gobanEvent) 
    {
	for (GobanEventListener listener : listeners) {
	    listener.onGobanEvent(gobanEvent);
	}
    }

    public boolean isSelected(int i, int j)
    {
	if (selection == null)
	    return false;
	else
	    return selection.equals(i, j);
    }

    public Point getSelection()
    {
	return selection;
    }

    public void setSelection(Point point)
    {
	Log.d("Goban", "setSelection: " + point);
	selection = point;
	invalidate();
   }
}
