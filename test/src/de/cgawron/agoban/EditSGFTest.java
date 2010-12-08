package de.cgawron.agoban;

import android.test.ActivityInstrumentationTestCase2;

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
public class EditSGFTest extends ActivityInstrumentationTestCase2<EditSGF> {

    public EditSGFTest() {
        super("de.cgawron.agoban", EditSGF.class);
    }

}
