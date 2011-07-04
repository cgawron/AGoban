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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.R;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import de.cgawron.go.sgf.Value;

/**
 * A {@link View} to be used for the SGF DaTe[] property.
 * Unlike the SGF standard, this view only supports a single date as the value.
 */
public class DatePropertyView extends PropertyView implements View.OnClickListener, DatePickerDialog.OnDateSetListener
{
	private static final String TAG = "DatePropertyView";

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
	}

	@Override
	public void createView(Context context)
	{
		addView(button = new Button(context));
	}

	@Override
	public void createView(Context context, AttributeSet attrs)
	{
		addView(button = new Button(context, attrs, R.attr.buttonStyle));
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		button.setRawInputType(InputType.TYPE_CLASS_DATETIME);
		button.setOnClickListener(this);
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

	@Override
	public void setValue(String value)
	{
		super.setValue(value);
		button.setText(value);
	}

	public void setValue(int year, int monthOfYear, int dayOfMonth)
	{
		String date = String.format("%04d-%02d-%02d", year, monthOfYear+1, dayOfMonth); 
		Log.d(TAG, "Date set to " + date);
		setValue(date);
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

	public void onClick(View v) 
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = format.parse(valueText);
		}
		catch (ParseException ex) {
			Log.e(TAG, "Error parsing " + valueText, ex);
		}
	
		Log.d(TAG, String.format("onclick: date=%04d-%02d-%02d", 1900 + date.getYear(), date.getMonth(), date.getDate()));
		DatePickerDialog dialog = new DatePickerDialog(getContext(), this, 1900 + date.getYear(), date.getMonth(), date.getDate());
		// DatePicker datePicker = dialog.getDatePicker();
		// ToDo: Only available in API-Level 11 
		//datePicker.setCalendarViewShown(true);
		dialog.show();
	}

	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) 
	{
		setValue(year, monthOfYear, dayOfMonth);
	}
	
}
