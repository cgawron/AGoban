package de.cgawron.agoban.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.GameInfo;
import de.cgawron.go.sgf.PropertyList;

public abstract class PropertyView extends LinearLayout
{
	private final static String TAG = "PropertyView";
	
	protected PropertyList properties;
	protected GameInfo property;

	protected String key;
	protected String valueText = "";

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file. These attributes are defined in
	 * SDK/assets/res/any/classes.xml.
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet)
	 */
	public PropertyView(Context context)
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
	public PropertyView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		key = attrs.getAttributeValue("http://cgawron", "property");
	}

	/**
	 * Sets the text to display in this label
	 * 
	 * @param text
	 *            The text to display. This will be drawn as one line.
	 */
	public void setPropertyList(PropertyList properties)
	{
		this.properties = properties;
		if (this.key != null) {
			Property.Key key = new Property.Key(this.key);
			property = (GameInfo) properties.get(key);
			Log.d(TAG, "property: " + property);
	
			if (property == null) {
				Property prop = Property.createProperty(key);
				Log.d(TAG,
						"new property for key " + this.key + ": "
								+ prop.getClass());
				property = (GameInfo) prop;
				properties.add(property);
			}
		}
	
		Log.d(TAG, "setPropertyList: " + properties);
		initView();
	}

	protected abstract void initView();
}
