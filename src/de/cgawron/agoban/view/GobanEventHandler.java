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
import android.view.MotionEvent;
import android.view.View;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

import de.cgawron.agoban.R;
import de.cgawron.agoban.GobanEvent;

import demo.multitouch.controller.MultiTouchController;
import demo.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import demo.multitouch.controller.MultiTouchController.PointInfo;
import demo.multitouch.controller.MultiTouchController.PositionAndScale;

public class GobanEventHandler extends MultiTouchController<Object>
{
    private static int MIN_STILL_TIME = 100;

    private GobanView gobanView;
    private long time;

    public GobanEventHandler(GobanView gobanView, Resources res) {
	super(gobanView, res, false);
	this.gobanView = gobanView;
    }


    public boolean onTouchEvent(MotionEvent event) 
    {
 
	if (!super.onTouchEvent(event)) {
	    Log.d("GobanEventHandler", String.format("onTouchEvent: %s %d", event, event.getEventTime() - time));
	    GobanEvent gobanEvent = new GobanEvent(gobanView, event);
	    Log.d("GobanEventHandler", "gobanEvent: " + gobanEvent);

	    switch (event.getAction()) {
	    case MotionEvent.ACTION_UP: 
		if (event.getEventTime() - time > MIN_STILL_TIME) {
		    gobanView.fireGobanEvent(gobanEvent);
		}
		break;

	    default:
		time = event.getEventTime();
		gobanView.setSelection(gobanEvent.getPoint());
		break;
	    }
	    return true;
	}
	else return true;
    }

    public boolean onTrackballEvent(MotionEvent event) 
    {
	Log.d("GobanEventHandler", "onTrackballEvent: " + event);
	Point p = gobanView.getSelection();
	if (p == null) p = new Point(9, 9);
	
	switch (event.getAction()) {
	case MotionEvent.ACTION_UP: 
	    if (event.getEventTime() - time > MIN_STILL_TIME) {
		GobanEvent gobanEvent = new GobanEvent(gobanView, p);
		Log.d("GobanEventHandler", "gobanEvent: " + gobanEvent);
		gobanView.fireGobanEvent(gobanEvent);
	    }
	    break;
	    
	case MotionEvent.ACTION_MOVE:
	    float  dx = event.getX();
	    float  dy = event.getY();
	    int x = (int) (p.getX() + Math.signum(dx));
	    int y = (int) (p.getY() + Math.signum(dy));
	    if (x < 0) 
		x=0;
	    else if (x >= gobanView.getBoardSize())
		x = gobanView.getBoardSize() -1;
	    if (y < 0) 
		y=0;
	    else if (y >= gobanView.getBoardSize())
		y = gobanView.getBoardSize() -1;
	    p = new Point(x, y);
	    Log.d("GobanEventHandler", String.format("onTrackballEvent: %s (%f, %f)", p, dx, dy));
	    /* fall through */
	default:
	    time = event.getEventTime();
	    GobanEvent gobanEvent = new GobanEvent(gobanView, p);
	    Log.d("GobanEventHandler", "gobanEvent: " + gobanEvent);
	    gobanView.setSelection(gobanEvent.getPoint());
	    break;
	}

	return true;
    }
}
