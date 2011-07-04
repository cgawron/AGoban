package de.cgawron.agoban.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import de.cgawron.agoban.SGFApplication;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.GameInfo;

public abstract class PropertyView extends LinearLayout
{
	private final static String TAG = "PropertyView";
	
	protected Node node;
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
		createView(context);
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
		createView(context, attrs);
	}

	public void createView(Context context)
	{
	}

	public void createView(Context context, AttributeSet attrs)
	{
	}
	
	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		if (isInEditMode())
			return;

		Context context = getContext();
		SGFApplication application = (SGFApplication) context.getApplicationContext();
		if (application.getGameTree() != null) {
			setNode(application.getGameTree().getRoot());
		}
	}

	/**
	 * Sets the text to display in this label
	 * 
	 * @param text
	 *            The text to display. This will be drawn as one line.
	 */
	public void setNode(Node node)
	{
		this.node = node;
		if (this.key != null) {
			Property.Key key = new Property.Key(this.key);
			property = (GameInfo) node.get(key);
			Log.d(TAG, "property: " + property);
	
			if (property == null) {
				Property prop = Property.createProperty(key);
				Log.d(TAG,
						"new property for key " + this.key + ": "
								+ prop.getClass());
				property = (GameInfo) prop;
				node.add(property);
			}
		}
	
		Log.d(TAG, "setPropertyList: " + node);
		initView();
	}

	protected abstract void initView();

	public void setValue(String value)
	{
		valueText = value;

		// ToDo: Delegate to parent class
		if (property != null) {
			Log.d(TAG, "setting property " + key + " to " + value);
			property.setValue(value);
			node.setProperty(property);
		}
	}
}
