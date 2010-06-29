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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
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
 * A {@code GobanRenderer} can render a {@link Goban} on a {@link Canvas}.
 *
 * @author Christian Gawron
 */
public class GobanRenderer 
{
    private float STONE_RADIUS = 0.47f;
    private float HOSHI_RADIUS = 0.15f;
    private float HIGHLIGHT_RADIUS = 0.2f;
    private float HIGHLIGHT_STROKEWIDTH = 0.06f;
    private float STONE_STROKEWIDTH = 0.03f;
    private float SELECTION_STROKEWIDTH = 0.03f;
    private int   SELECTION_COLOR = Color.RED;

    public GobanRenderer()
    {
    }

    public void render(Goban goban, Canvas canvas)
    {
	int size = goban.getBoardSize();
	Point lastMove = goban.getLastMove();
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
			if (lastMove != null && lastMove.equals(i, j))
			    drawStoneHighlighted(i, j, stone, canvas);
			else
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
	paint.setAntiAlias(true);
	paint.setStrokeWidth(STONE_STROKEWIDTH);

	switch (stone) {
	case BLACK:
	    paint.setARGB(255, 0, 0, 0);
	    paint.setStyle(Paint.Style.FILL_AND_STROKE);
	    canvas.drawCircle(i+1f, j+1f, STONE_RADIUS, paint);
	    break;
	case WHITE:
	    paint.setARGB(255, 255, 255, 255);
	    paint.setStyle(Paint.Style.FILL);
	    canvas.drawCircle(i+1f, j+1f, STONE_RADIUS, paint);
	    paint.setARGB(255, 0, 0, 0);
	    paint.setStyle(Paint.Style.STROKE);
	    canvas.drawCircle(i+1f, j+1f, STONE_RADIUS, paint);
	    break;
	default:
	    break;
	}
    }

    void drawStoneHighlighted(int i, int j, BoardType stone, Canvas canvas) 
    {
	drawStone(i, j, stone, canvas);

	Paint paint = new Paint();
	paint.setAntiAlias(true);
	paint.setStrokeWidth(HIGHLIGHT_STROKEWIDTH);
	paint.setStyle(Paint.Style.STROKE);

	switch (stone) {
	case BLACK:
	    paint.setARGB(255, 255, 255, 255);
	    canvas.drawCircle(i+1f, j+1f, HIGHLIGHT_RADIUS, paint);
	    break;
	case WHITE:
	    paint.setARGB(255, 0, 0, 0);
	    canvas.drawCircle(i+1f, j+1f, HIGHLIGHT_RADIUS, paint);
	    break;
	default:
	    break;
	}
    }

    void drawHoshi(int boardSize, Canvas canvas, Paint paint)
    {
	int m = boardSize >> 1;
	int h = boardSize > 9 ? 3 : 2;

	paint.setAntiAlias(true);
	paint.setARGB(230, 0, 0, 0);
	paint.setStyle(Paint.Style.FILL_AND_STROKE);

	if (boardSize % 2 != 0)
	{
	    canvas.drawCircle(m+1, m+1, HOSHI_RADIUS, paint);
	    
	    if (boardSize > 9)
	    {
		canvas.drawCircle(h+1, m+1, HOSHI_RADIUS, paint);
		canvas.drawCircle(boardSize-h, m+1, HOSHI_RADIUS, paint);
		canvas.drawCircle(m+1, h+1, HOSHI_RADIUS, paint);
		canvas.drawCircle(m+1, boardSize-h, HOSHI_RADIUS, paint);
	    }
	}

        if (boardSize > 7)
        {
	    canvas.drawCircle(h+1, h+1, HOSHI_RADIUS, paint);
	    canvas.drawCircle(h+1, boardSize-h, HOSHI_RADIUS, paint);
	    canvas.drawCircle(boardSize-h, h+1, HOSHI_RADIUS, paint);
	    canvas.drawCircle(boardSize-h, boardSize-h, HOSHI_RADIUS, paint);
        }
    }

    void drawSelection(int i, int j, Canvas canvas)
    {
	Paint paint = new Paint();
	paint.setAntiAlias(true);
	paint.setStrokeWidth(SELECTION_STROKEWIDTH);
	paint.setColor(SELECTION_COLOR);
	paint.setStyle(Paint.Style.STROKE);
	canvas.drawRect(i, j, i+1.0f, j+1.0f, paint);
    }

}