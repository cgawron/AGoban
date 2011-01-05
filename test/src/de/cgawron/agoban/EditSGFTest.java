package de.cgawron.agoban;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import de.cgawron.agoban.SGFApplication;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.view.GameTreeControls;
import de.cgawron.agoban.view.GobanView;
import de.cgawron.go.sgf.GameTree;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class de.cgawron.agoban.EditSGFTest \
 * de.cgawron.agoban.tests/android.test.InstrumentationTestRunner
 */
public class EditSGFTest extends ActivityInstrumentationTestCase2<EditSGF> 
{
    private final String TAG = "EditSGF";

    public EditSGFTest() {
        super("de.cgawron.agoban", EditSGF.class);
    }

    Activity activity;
    SGFApplication application;
    GobanView gobanView;
    GameTreeControls gameTreeControls;
    GameTree gameTree;

    @Override
    protected void setUp() throws Exception 
    {
        super.setUp();

        // setActivityInitialTouchMode(false);

	Uri data = SGFProvider.CONTENT_URI.buildUpon().appendPath("gross4.sgf").build();
	Intent intent = new Intent(Intent.ACTION_VIEW, data);
	setActivityIntent(intent);
	
        activity = getActivity();

	application = (SGFApplication) activity.getApplication();
        gobanView = (GobanView) activity.findViewById(de.cgawron.agoban.R.id.goban);
	gameTreeControls = (GameTreeControls) activity.findViewById(R.id.controls);

	while ((gameTree = application.getGameTree()) == null) {
	    Log.d(TAG, "Waiting for SGF to be loaded ...");
	    Thread.sleep(1000);
	}
	//Debug.dumpHprofData("/sdcard/sgf/dump.hprof");
    }

    public void testPreconditions() 
    {
	// Assert that the GobanView exists
        assertNotNull("checking gobanView", gobanView);
        assertNotNull("checking application", application);
        assertNotNull("checking gameTree", gameTree);
        assertNotNull("checking gameTreeControls", gameTreeControls);
    }

}
