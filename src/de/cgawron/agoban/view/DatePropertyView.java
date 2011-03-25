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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import de.cgawron.agoban.SGFApplication;
import de.cgawron.go.sgf.Value;

/**
 * A {@link View} to be used for the SGF DaTe[] property.
 * Unlike the SGF standard, this view only supports a single date as the value.
 */
public class DatePropertyView extends PropertyView 
{
	static String TAG = "DatePropertyView";
	protected Button button;

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public DatePropertyView(Context context)
	{
		super(context);
		addView(button = new Button(context));
	}

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public DatePropertyView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		addView(button = new Button(context, attrs));
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		Context context = getContext();
		if (isInEditMode())
			return;

		SGFApplication application = (SGFApplication) context
				.getApplicationContext();

		// TODO: Rethink initialization
		if (application.getGameTree() != null) {
			setPropertyList(application.getGameTree().getRoot());
		}
	}

	@Override
	protected void initView()
	{
		if (property != null) {
			Value value = property.getValue();
			if (value != null)
				valueText = value.toString();
		}

		button.setText(valueText);
	}

	public void setValue(String value)
	{
		valueText = value;
		button.setText(valueText);
	}

	public void setValue(ContentValues values)
	{
		valueText = values.get(key).toString();
		if (valueText == null)
			valueText = "";
		button.setText(valueText);
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
		button.setText(valueText);
	}

}
