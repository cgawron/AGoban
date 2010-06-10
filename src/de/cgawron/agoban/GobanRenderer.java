/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.OrientationEventListener;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.agoban.R;


/**
 * Example of how to write a custom subclass of View. LabelView
 * is used to draw simple text views. Note that it does not handle
 * styled text or right-to-left writing systems.
 *
 */
public class GobanRenderer 
{
    public GobanRenderer()
    {
    }

    public void render(Goban goban, Canvas canvas)
    {
	int size = goban.getBoardSize();
	Paint paint = new Paint();
	paint.setARGB(255, 255, 255, 0);
        canvas.drawRect(0.5f, 0.5f, size+0.5f, size+0.5f, paint);
	paint.setARGB(255, 0, 0, 0);
	paint.setStyle(Paint.Style.STROKE);
	for (int i=1; i<=size; i++) {
	    canvas.drawLine(1, i, size, i, paint);
	    canvas.drawLine(i, 1, i, size, paint);
	}

	drawHoshi(size, canvas, paint);

	if (goban != null)
	{
	    for (short i=0; i<size; i++) {
		for (short j=0; j<size; j++) {
		    BoardType stone = goban.getStone(i, j);
		    switch (stone) {
		    case BLACK:
		    case WHITE:
			drawStone(i, j, stone, canvas);
			break;
		    case EMPTY:
			break;
		    }
		}
	    }
	}
    }

    void drawStone(int i, int j, BoardType stone, Canvas canvas)
    {
	Paint paint = new Paint();
	switch (stone) {
	case BLACK:
	    paint.setARGB(255, 0, 0, 0);
	    paint.setStyle(Paint.Style.FILL_AND_STROKE);
	    canvas.drawCircle(i+1f, j+1f, 0.49f, paint);
	    break;
	case WHITE:
	    paint.setARGB(255, 255, 255, 255);
	    paint.setStyle(Paint.Style.FILL);
	    canvas.drawCircle(i+1f, j+1f, 0.49f, paint);
	    paint.setARGB(255, 0, 0, 0);
	    paint.setStyle(Paint.Style.STROKE);
	    canvas.drawCircle(i+1f, j+1f, 0.49f, paint);
	    break;
	default:
	    break;
	}
    }

    void drawHoshi(int boardSize, Canvas canvas, Paint paint)
    {
	int m = boardSize >> 1;
	int h = boardSize > 9 ? 3 : 2;
	float radius = 0.2f;

	paint.setAntiAlias(true);
	paint.setARGB(230, 0, 0, 0);
	paint.setStyle(Paint.Style.FILL_AND_STROKE);

	if (boardSize % 2 != 0)
	{
	    canvas.drawCircle(m+1, m+1, radius, paint);
	    
	    if (boardSize > 9)
	    {
		canvas.drawCircle(h+1, m+1, radius, paint);
		canvas.drawCircle(boardSize-h, m+1, radius, paint);
		canvas.drawCircle(m+1, h+1, radius, paint);
		canvas.drawCircle(m+1, boardSize-h, radius, paint);
	    }
	}

        if (boardSize > 7)
        {
	    canvas.drawCircle(h+1, h+1, radius, paint);
	    canvas.drawCircle(h+1, boardSize-h, radius, paint);
	    canvas.drawCircle(boardSize-h, h+1, radius, paint);
	    canvas.drawCircle(boardSize-h, boardSize-h, radius, paint);
        }

    }

}