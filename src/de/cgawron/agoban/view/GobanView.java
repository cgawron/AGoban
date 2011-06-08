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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MotionEvent;
import android.view.View;
import de.cgawron.agoban.GobanEvent;
import de.cgawron.agoban.GobanEventListener;
import de.cgawron.agoban.R;
import de.cgawron.agoban.SGFApplication;
import de.cgawron.go.Goban;
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
public class GobanView extends View //implements
		//demo.MultiTouchController.MultiTouchObjectCanvas<Object>
{
	private static String TAG = "GobanView";

	private Goban goban;
	private GobanRenderer renderer;
	private final List<GobanEventListener> listeners = new ArrayList<GobanEventListener>();
	private final SharedPreferences settings;
	private GobanEventHandler gobanEventHandler;
	private PointF cursorPosition;
	private Tool tool;
	private final List<GobanRenderer.Markup> markupList = new ArrayList<GobanRenderer.Markup>();

	private final float xOff = 0.0f, yOff = 0.0f, relativeScale = 1.0f;
	private RectF blowup;
	private float blowupScale;
	private float blowupWidth;

	/**
	 * This interface represents an abstract "tool" for a Goban which can
	 * implement functions like moving, adding a stone, etc.
	 */
	public interface Tool
	{
		/**
		 * Get a {@code Drawable} for the cursor.
		 * 
		 */
		Drawable getCursor();

		void onGobanEvent(GobanEvent event);
	}

	public class GobanContextMenuInfo implements ContextMenuInfo
	{
		public PointF pointF;

		GobanContextMenuInfo(PointF pointF)
		{
			this.pointF = pointF;
		}

		@Override
		public String toString()
		{
			return String.format("GobanContextMenuInfo(%s)", pointF);
		}
	}

	/**
	 * Constructor. This version is only needed if you will be instantiating the
	 * object manually (not from a layout XML file).
	 * 
	 * @param context
	 */
	public GobanView(Context context)
	{
		super(context);
		settings = context.getSharedPreferences(SGFApplication.PREF, 0);
		// mOrientationListener = new MyOrientationEventListener(context);
		initGobanView();
	}

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public GobanView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		settings = context.getSharedPreferences(SGFApplication.PREF, 0);
		initGobanView();

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.GobanView);

		// Note, if you only care about supporting a single color, that you
		// can instead call a.getColor() and pass that to setTextColor().
		// setTextColor(a.getColor(R.styleable.GobanView_textColor,
		// 0xFF000000));

		a.recycle();
	}

	private final void initGobanView()
	{
		blowupScale = settings.getFloat("blowupScale", 4f);
		blowupWidth = settings.getFloat("blowupWidth", 8.5f);

		// Create Event handler
		gobanEventHandler = new GobanEventHandler(this, getResources());
		setOnTouchListener(gobanEventHandler);
		// setOnClickListener(gobanEventHandler);
		setOnLongClickListener(gobanEventHandler);

		renderer = new GobanRenderer(this);
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
		} else {
			Value.PointList pointList = property.getPointList();
			for (Point point : pointList) {
				Goban.BoardType stone = goban.getStone(point);
				Log.d(TAG, "adding " + property.getKey() + " at " + point);
				markupList.add(renderer.new SGFMarkup(point, stone, property
						.getType()));
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

	/**
	 * Sets the text to display in this label
	 * 
	 * @param text
	 *            The text to display. This will be drawn as one line.
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

		if (goban == null)
			return;
		float size = goban.getBoardSize();
		int width = getWidth();
		int height = getHeight();
		float min = width < height ? width : height;
		Log.d("Goban", "onDraw " + min);

		canvas.scale(min / size, min / size);
		canvas.translate(0.5f, 0.5f);
		renderer.render(goban, canvas);

		Drawable cursor = null;
		if (tool != null) {
			cursor = tool.getCursor();
			cursor.setBounds(-2, -2, 2, 2);
		}

		// draw blowup
		if (blowup != null) {
			float xm = blowup.left + blowupWidth;
			float ym = blowup.top + blowupWidth;
			canvas.saveLayerAlpha(blowup, 240, Canvas.ALL_SAVE_FLAG);
			canvas.translate(-(blowupScale - 1) * xm, -(blowupScale - 1) * ym);
			canvas.scale(blowupScale, blowupScale);
			renderer.render(goban, canvas);
			if (cursorPosition != null && cursor != null) {
				canvas.save();
				canvas.translate(cursorPosition.x - 0.5f,
						cursorPosition.y - 0.5f);
				cursor.draw(canvas);
				canvas.restore();
			}
			canvas.restore();

			Paint paint = new Paint();
			paint.setARGB(200, 255, 0, 0);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(blowup, paint);
		} else if (cursorPosition != null && cursor != null) {
			canvas.save();
			canvas.translate(cursorPosition.x - 1f, cursorPosition.y - 1f);
			cursor.draw(canvas);
			canvas.restore();
		}
	}

	public void setBlowup(GobanEvent event)
	{
		Log.d(TAG, "setBlowup: " + event);
		MotionEvent me = event.getBaseEvent();
		switch (me.getAction()) {
		case MotionEvent.ACTION_UP:
			blowup = null;
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_DOWN:
			float width = getWidth();
			float height = getHeight();
			float size = getBoardSize();

			float bx = size * me.getX() / width - 0.5f;
			float by = size * me.getY() / height - 0.5f;
			if (bx > size - 0.5f)
				bx = size - 0.5f;
			else if (bx < -0.5f)
				bx = -0.5f;
			if (by > size - 0.5f)
				by = size - 0.5f;
			else if (by < -0.5)
				by = -0.5f;

			// float xm = bx * (size - 10) / size;
			// float ym = by * (size - 10) / size;
			blowup = new RectF(bx - blowupWidth, by - blowupWidth, bx
					+ blowupWidth, by + blowupWidth);
			invalidate();
		}
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

	public void getPositionAndScale(Object obj,
			PositionAndScale objPosAndScaleOut)
	{
		// We start at 0.0f each time the drag position is replaced, because we
		// just want the relative drag distance
		objPosAndScaleOut.set(xOff, yOff, relativeScale);
	}

	public void selectObject(Object obj, PointInfo pt)
	{
		int width = getWidth();
		int height = getHeight();
		int size = goban.getBoardSize();
		Log.d("Goban", String.format("selectObject: %f(%d), %f(%d) obj=%s", pt
				.getX(), width, pt.getY(), height,
				(obj == null ? "null" : obj.toString())));

		float bx = size * pt.getX() / width;
		float by = size * pt.getY() / height;
		Log.d("Goban", String.format("selectObject: (%f, %f)", bx, by));

		setCursorPosition(new PointF(bx, by));
		// invalidate(bx, by, bx+1, by+1);
		invalidate();
	}

	public boolean setPositionAndScale(Object obj, PositionAndScale update,
			PointInfo touchPoint)
	{
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
		if (tool != null)
			tool.onGobanEvent(gobanEvent);
		for (GobanEventListener listener : listeners) {
			listener.onGobanEvent(gobanEvent);
		}
	}

	public void setCursorPosition(PointF point)
	{
		cursorPosition = point;
	}

	public PointF getCursorPosition()
	{
		return cursorPosition;
	}

	@Override
	public ContextMenuInfo getContextMenuInfo()
	{
		return new GobanContextMenuInfo(cursorPosition);
	}

	public void setTool(Tool tool)
	{
		this.tool = tool;
	}
}
