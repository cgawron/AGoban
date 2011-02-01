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

package de.cgawron.agoban.view.tool;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.cgawron.agoban.R;
import de.cgawron.agoban.EditSGF;
import de.cgawron.agoban.GobanEvent;
import de.cgawron.agoban.view.GobanView;
import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Value;

/**
 * Provides an sgf editor.
 */
public class MoveTool extends Drawable implements GobanView.Tool 
{
    private static final String TAG = "MoveTool";
    private EditSGF editor;
    private Drawable whiteCursor;
    private Drawable blackCursor;
    
    public MoveTool(EditSGF editor) 
    {
	this.editor = editor;

	Shape oval = new OvalShape();
	oval.resize(1.0f, 1.0f);
	blackCursor = new ShapeDrawable(oval);
	whiteCursor = new ShapeDrawable(oval);
	((ShapeDrawable) whiteCursor).getPaint().setARGB(255, 255, 255, 255);
	
	//blackCursor = resources.getDrawable(R.drawable.black_stone_cursor);
	//whiteCursor = resources.getDrawable(R.drawable.white_stone_cursor);
    }

    @Override
    public Drawable getCursor()
    {
	return this;
    }
    
    @Override
    public void draw(Canvas canvas)
    {
	Node currentNode = editor.getCurrentNode();
	Drawable drawable;
	if (currentNode != null) {
	    BoardType color = currentNode.getColor();

	    if (color == BoardType.BLACK) {
		drawable = whiteCursor;
	    }
	    else drawable = blackCursor;
	}
	else drawable = blackCursor;
	
	Paint paint = new Paint();
	paint.setAntiAlias(true);
	paint.setStrokeWidth(0.05f);
	paint.setARGB(180, 0, 0, 255);
	paint.setStyle(Paint.Style.STROKE);
	canvas.drawLine(-1.5f, 0, 1.5f, 0, paint);
	canvas.drawLine(0, -1.5f, 0, 1.5f, paint);

	canvas.save();
	canvas.scale(0.25f, 0.25f);
	drawable.setBounds(getBounds());
	//drawable.getPaint().setStrokeWidth(0.01f);
	drawable.draw(canvas);
	canvas.restore();
    }

    @Override
    public void onGobanEvent(GobanEvent gobanEvent) 
    {
	Node currentNode = editor.getCurrentNode();
	Map<Point, Node> variations = editor.getVariations();

	if (currentNode != null) {
	    Point point = gobanEvent.getPoint();
	    if (point == null) return;
	    Log.d(TAG, "onGobanEvent: variations: " + variations.keySet());
	    // click on a variation - select it
	    if (variations.containsKey(point)) {
		editor.setCurrentNode(variations.get(point));
	    }
	    // click on an existing stone - go to node
	    else if (currentNode.getGoban().getStone(point) != BoardType.EMPTY) {
		Node node = currentNode;
		while (node.getParent() != null && !point.equals(node.getGoban().getLastMove())) {
		    node = node.getParent();
		}
		editor.setCurrentNode(node);
	    }
	    // click on an empty intersection - move
	    else if (editor.checkNotReadOnly()) {
		if (currentNode.getChildCount() == 1 && currentNode.getDepth() <= 1) {
		    askReplaceMove(currentNode, point);
		}
		else 
		    move(currentNode, point, false);
	    }
	}
    }

    private void move(Node parent, Point point, boolean replaceChild)
    {
	if (replaceChild) {
	    Log.i(TAG, "Removing old child node");
	    parent.getChildren().remove(0);
	}
	
	Node newNode;
	newNode = new Node(editor.getGameTree());
	Log.d(TAG, "Adding new node " + newNode);
	try {
	    newNode.setGoban(parent.getGoban().clone());
	}
	catch (CloneNotSupportedException ex) {
	    Log.e(TAG, "onGobanEvent", ex);
	}
	parent.add(newNode);

	newNode.move(point);	
	Log.d(TAG, "addMove: " + newNode + ", " + parent);
	editor.setCurrentNode(newNode);
    }


    boolean replace;
    public boolean askReplaceMove(final Node currentNode, final Point point)
    {
	Context context = editor;
	Dialog dialog;
	replace  = true;
	OnClickListener listener = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    if (which == DialogInterface.BUTTON_POSITIVE) 
			move(currentNode, point, false);
		    else
			move(currentNode, point, true);
		    
		    dialog.dismiss();
		}
	    };
	AlertDialog.Builder builder = new AlertDialog.Builder(context);
	builder.setTitle("Add Variation?");
	builder.setMessage("Do you want to replace the last move or to add a variaton?");
	builder.setPositiveButton("Add", listener);
	builder.setNegativeButton("Replace", listener);
	dialog = builder.show();
	
	return replace;
    }

    @Override
    public int getOpacity()
    {
	return 255;
    }

    @Override
    public void setAlpha(int alpha)
    {
    }

    @Override
	public void setColorFilter(ColorFilter cf)
    {
    }
}
