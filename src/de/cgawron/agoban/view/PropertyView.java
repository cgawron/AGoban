
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
import android.content.ContentValues;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import de.cgawron.agoban.SGFApplication;
import de.cgawron.agoban.R;

import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Property.GameInfo;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.PropertyList;
import de.cgawron.go.sgf.Value;

/**
 * A {@link View} to be used for {@link de.cgawron.go.Goban} 
 *
 */
public class PropertyView extends TableRow implements TextWatcher
{
    private GameInfo property;

    private TextView label;
    private TextView text;
    private String key;
    private String valueText = "";

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public PropertyView(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
	key = attrs.getAttributeValue("http://cgawron", "property");

	SGFApplication application = (SGFApplication) context.getApplicationContext();
	init(context);

	//TODO: Rethink initialization
	if (application.getGameTree() != null) {
	    setPropertyList(application.getGameTree().getRoot());
	}

	String labelText = "";
	int labelId = attrs.getAttributeResourceValue("http://cgawron", "label", 0);
	if (labelId > 0)
	    labelText = context.getResources().getString(labelId);
	label.setText(labelText);
    }

    protected void init(Context context)
    {
	TableRow.LayoutParams params = new TableRow.LayoutParams(1);
	label = new TextView(context);
	text = new EditText(context);
	label.setPadding(0, 0, 5, 0);
	addView(label);
	addView(text, params);
	text.addTextChangedListener(this);
    }

    protected void initText() 
    {
	if (property != null) {
	    Value value = property.getValue();
	    if (value != null)
		valueText = value.toString();
	}
	    
	text.setText(valueText);
    }

    /**
     * Sets the text to display in this label
     * @param text The text to display. This will be drawn as one line.
     */
    public void setPropertyList(PropertyList properties) {
	if (this.key != null) {
	    Property.Key key = new Property.Key(this.key);
	    property = (GameInfo) properties.get(key); 
	    Log.d("PropertyView", "property: " + property); 
	    
	    if (property == null) {
		Property prop = Property.createProperty(key);
		Log.d("PropertyView", "new property class for key " + this.key + ": " + prop.getClass()); 
		property = (GameInfo) prop;
		properties.add(property);
	    }
	}
	
	Log.d("PropertyView", "setPropertyList: " + properties); 
	initText();
    }

    public void setValue(String value)
    {
	valueText = value;
	text.setText(valueText);
    }

    public void setValue(ContentValues values)
    {
	valueText = values.get(key).toString();
	if (valueText == null)
	    valueText= "";
	text.setText(valueText);
    }

    public void setValue(Cursor cursor, int position)
    {
	Log.d("PropertyView", String.format("setValue(%s, %d)", cursor, position));
	int oldPosition = cursor.getPosition();
	cursor.moveToPosition(position);
	valueText = cursor.getString(cursor.getColumnIndex(key));
	cursor.moveToPosition(oldPosition);
	
	if (valueText == null)
	    valueText= "";
	text.setText(valueText);
    }

    public void afterTextChanged(android.text.Editable s)
    {
	Log.d("PropertyView", "afterTextChanged: " + s);
	if (property != null) {
	    Log.d("PropertyView", "setting property " + key + " to " + s);
	    property.setValue(s.toString());
	}
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
    }

    public void onTextChanged(CharSequence s, int start, int count, int after)
    {
    }
}


