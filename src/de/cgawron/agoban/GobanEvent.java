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

package de.cgawron.agoban;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

import de.cgawron.agoban.R;
import de.cgawron.agoban.view.GobanView;

public class GobanEvent
{
	private GobanView gobanView;
	private PointF pointF;
	private Point point;
	private MotionEvent motionEvent;

	public GobanEvent(GobanView gobanView, MotionEvent motionEvent)
	{
		this.gobanView = gobanView;
		this.motionEvent = motionEvent;
		initialize(motionEvent);
	}

	public GobanEvent(GobanView gobanView, Point point)
	{
		this.gobanView = gobanView;
		initialize(point);
	}

	public void initialize(MotionEvent motionEvent)
	{
		int width = gobanView.getWidth();
		int height = gobanView.getHeight();
		float size = gobanView.getBoardSize();

		float bx = size * motionEvent.getX() / width;
		float by = size * motionEvent.getY() / height;
		if (bx >= size || bx < 0 || by >= size || by < 0) {
			point = null;
			pointF = null;
		} else {
			point = new Point((int) bx, (int) by);
			pointF = new PointF(bx, by);
		}
		Log.d("GobanEvent", String.format("initialize: (%f, %f)", bx, by));
	}

	public void initialize(Point point)
	{
		this.point = point;
		Log.d("GobanEvent", String.format("initialize: (%s)", point));
	}

	@Override
	public String toString()
	{
		return "GobanEvent p=" + point;
	}

	public MotionEvent getBaseEvent()
	{
		return motionEvent;
	}

	public Point getPoint()
	{
		return point;
	}

	public PointF getPointF()
	{
		return pointF;
	}
}