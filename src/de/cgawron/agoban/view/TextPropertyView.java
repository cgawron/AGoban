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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.cgawron.go.sgf.Value;

/**
 * A {@link View} to be used for SGF properties
 * 
 */
public class TextPropertyView extends PropertyView implements TextWatcher
{
	static String TAG = "TextPropertyView";
	protected EditText textView;

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public TextPropertyView(Context context)
	{
		super(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
																		 LinearLayout.LayoutParams.MATCH_PARENT);
		addView(textView = new EditText(context), params);
	}

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public TextPropertyView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
																		 LinearLayout.LayoutParams.MATCH_PARENT);
		addView(textView = new EditText(context, attrs), params);
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
	}

	@Override
	protected void initView()
	{
		if (property != null) {
			Value value = property.getValue();
			if (value != null)
				valueText = value.toString();
		}

		textView.setText(valueText);
		textView.addTextChangedListener(this);
	}

	public void setValue(ContentValues values)
	{
		valueText = values.get(key).toString();
		if (valueText == null)
			valueText = "";
		textView.setText(valueText);
	}

	public void setValue(Cursor cursor, int position)
	{
		Log.d(TAG, String.format("setValue(%s, %d)", cursor, position));
		int oldPosition = cursor.getPosition();
		cursor.moveToPosition(position);
		valueText = cursor.getString(cursor.getColumnIndex(key));
		cursor.moveToPosition(oldPosition);

		if (valueText == null)
			valueText = "";
		textView.setText(valueText);
	}

	public void afterTextChanged(Editable s)
	{	
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{	
	}

	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		Log.d(TAG, "onTextChanged: " + s);
		setValue(s.toString());
	}
}
