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
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * A {@link View} to be used for grade properties (i.e. BR[] and WR[])
 * 
 */
public class GradeView extends TextPropertyView
{
	private static String TAG = "GradeView";

	private final DanKyuFilter[] filters = { new DanKyuFilter() };

	private class DanKyuFilter implements InputFilter
	{
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend)
		{
			StringBuilder builder = new StringBuilder();
			if (dest != null) {
				builder.append(dest, 0, dstart);
				builder.append(source.subSequence(start, end));
				if (dend < dest.length())
					builder.append(dest, dend, dest.length());
			}
			Log.d(TAG,
					String.format("filter: %s->%s, %d, %d: %s",
							source.subSequence(start, end), dest, dstart, dend,
							builder));

			int grade = 0;
			int i;
			for (i = 0; i < builder.length()
					&& Character.isDigit(builder.charAt(i)); i++) {
				grade = 10 * grade + Character.digit(builder.charAt(i), 10);
			}
			if (grade == 0)
				return "";

			switch (builder.length() - i) {
			case 0:
				return null;
			case 1:
				if (isGrade(builder.charAt(builder.length() - 1)))
					return null;
				else
					return "";
			default:
				return "";
			}
		}

		public boolean isGrade(char c)
		{
			switch (c) {
			case 'k':
			case 'd':
			case 'p':
				return true;

			default:
				return false;
			}
		}
	}

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public GradeView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		textView.getText().setFilters(filters);
	}

}
