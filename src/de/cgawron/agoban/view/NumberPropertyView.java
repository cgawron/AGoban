package de.cgawron.agoban.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class NumberPropertyView extends TextPropertyView
{
	private TextView text;
	
	public NumberPropertyView(Context context)
	{
		super(context);
	}

	public NumberPropertyView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	/*
	@Override
	public void createView(Context context)
	{
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				                                                         LinearLayout.LayoutParams.MATCH_PARENT);
		addView(text = new Button(context), params);
	}

	@Override
	public void createView(Context context, AttributeSet attrs)
	{
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				                                                         LinearLayout.LayoutParams.MATCH_PARENT);
		addView(text = new Button(context, attrs), params);
	}
	
	@Override
	protected void initView()
	{
		if (property != null) {
			Value value = property.getValue();
			if (value != null)
				valueText = value.toString();
		}

		text.setText(valueText);
	}
    */
}
