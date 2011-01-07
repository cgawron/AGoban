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
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MotionEvent;
import android.view.View;
import de.cgawron.agoban.GobanEvent;
import de.cgawron.agoban.GobanEventListener;
import de.cgawron.agoban.R;
import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.Markup;
import de.cgawron.go.sgf.Value;

import demo.MultiTouchController.PointInfo;
import demo.MultiTouchController.PositionAndScale;

/**
 * A {@link View} to be used for {@link de.cgawron.go.Goban} 
 *
 */
public class GobanView extends View implements demo.MultiTouchController.MultiTouchObjectCanvas<Object> 
{
    private static String TAG = "GobanView";

    private Goban goban;
    private GobanRenderer renderer;
    private final List<GobanEventListener> listeners = new ArrayList<GobanEventListener>();
    private GobanEventHandler gobanEventHandler ;
    private Point selection;
    private List<GobanRenderer.Markup> markupList = new ArrayList<GobanRenderer.Markup>();

    private final float xOff = 0.0f, yOff = 0.0f, relativeScale = 1.0f;

    public class GobanContextMenuInfo implements ContextMenuInfo
    {
	public Point point;
 
	GobanContextMenuInfo(Point point)
	{
	    this.point = point;
	}
	
	@Override
	public String toString() 
	{
	    return String.format("GobanContextMenuInfo(%s)", point);
	}
    }
    
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
    public GobanView(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
        initGobanView();

        TypedArray a = context.obtainStyledAttributes(attrs,
						      R.styleable.GobanView);

        // Note, if you only care about supporting a single color, that you
        // can instead call a.getColor() and pass that to setTextColor().
        //setTextColor(a.getColor(R.styleable.GobanView_textColor, 0xFF000000));

        a.recycle();
    }

    public void addMarkup(Goban goban, Markup property)
    {
	if (property instanceof Property.Label) {
	    Value.ValueList vl = (Value.ValueList) property.getValue();
	    for (Value v : vl) {
		if (v instanceof Value.Label) {
		    Point point = ((Value.Label) v).getPoint();
		    String text = ((Value.Label) v).toString();
		    Log.d(TAG, "adding " + text + " at " + point);
		    addLabel(point, text);
		}
	    }
	}
	else {
	    Value.PointList pointList = property.getPointList();
	    for (Point point : pointList) {
		Goban.BoardType stone = goban.getStone(point); 
		Log.d(TAG, "adding " + property.getKey() + " at " + point);
		markupList.add(renderer.new SGFMarkup(point, stone, property.getType()));
	    }
	}
    }

    public void addLabel(Point p, String text)
    {
	markupList.add(renderer.new Label(p, text));
    }

    public void addVariation(Point p)
    {
	markupList.add(renderer.new VariationMark(p));
    }

    public void markLastMove(Point p)
    {
	markupList.add(renderer.new LastMoveMark(p));
    }

    public void resetMarkup()
    {
	markupList.clear();
    }

    public Iterable<GobanRenderer.Markup> getMarkup()
    {
	return markupList;
    }

    private final void initGobanView() 
    {
	// Create Event handler
	gobanEventHandler = new GobanEventHandler(this, getResources());
	setOnTouchListener(gobanEventHandler);
	//setOnClickListener(gobanEventHandler);
	setOnLongClickListener(gobanEventHandler);

	renderer = new GobanRenderer(this);
    }

    /**
     * Sets the text to display in this label
     * @param text The text to display. This will be drawn as one line.
     */
    public void setGoban(Goban goban) 
    {
	if (goban == null)
	    throw new NullPointerException("setGoban: goban must not be null!");
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
    protected void onDraw(Canvas canvas) 
    {
        super.onDraw(canvas);
	Rect bounds = new Rect();
	boolean clip = canvas.getClipBounds(bounds);
	Log.d("Goban", "onDraw, clip=" + clip);

	if (goban == null) return;
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
    public boolean onTrackballEvent(MotionEvent event) 
    {
	if (!gobanEventHandler.onTrackballEvent(event))
	    return super.onTrackballEvent(event);
	else
	    return true;
    }
    
    public Object getDraggableObjectAtPoint(PointInfo pt) 
    {
	// We do not support dragging on a goban
	return null;
    }

    public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut) 
    {
	// We start at 0.0f each time the drag position is replaced, because we just want the relative drag distance 
	objPosAndScaleOut.set(xOff, yOff, relativeScale);
    }

    public void selectObject(Object obj, PointInfo pt) 
    {
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
	
    public boolean setPositionAndScale(Object obj, PositionAndScale update, PointInfo touchPoint) {
	// Get new offsets and coords
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
    
    public ContextMenuInfo getContextMenuInfo()
    {
	return new GobanContextMenuInfo(selection);
    }

}
