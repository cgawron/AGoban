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
    private GobanView gobanView;

    public GobanEventHandler(GobanView gobanView, Resources res) {
	super(gobanView, res, false);
	this.gobanView = gobanView;
    }


    public boolean onTouchEvent(MotionEvent event) 
    {
	if (!super.onTouchEvent(event)) {
	    Log.d("GobanEventHandler", "onTouchEvent: " + event);
	    GobanEvent gobanEvent = new GobanEvent(gobanView, event);
	    Log.d("GobanEventHandler", "gobanEvent: " + gobanEvent);

	    if (event.getAction() == event.ACTION_UP) {
		gobanView.fireGobanEvent(gobanEvent);
	    }
	    else
		gobanView.setSelection(gobanEvent.getPoint());

	    return true;
	}
	else return true;
    }

    public boolean onTrackballEvent(MotionEvent event) 
    {
	Log.d("GobanEventHandler", "onTrackballEvent: " + event);
	return false;
    }
}
