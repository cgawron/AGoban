package de.cgawron.agoban.view;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import de.cgawron.go.sgf.Value;

public class NumberPropertyView extends PropertyView
{
	private Button button;
	
	public NumberPropertyView(Context context)
	{
		super(context);
	}

	public NumberPropertyView(Context context, AttributeSet attrs)
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
	protected void initView()
	{
		if (property != null) {
			Value value = property.getValue();
			if (value != null)
				valueText = value.toString();
		}

		button.setText(valueText);
	}

}
