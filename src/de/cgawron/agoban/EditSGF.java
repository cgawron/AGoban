package de.cgawron.agoban;

import de.cgawron.go.Goban;
import static de.cgawron.go.Goban.BoardType.WHITE;
import static de.cgawron.go.Goban.BoardType.BLACK;
import de.cgawron.go.SimpleGoban;

import android.app.Activity;
import android.os.Bundle;

public class EditSGF extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	GobanView gobanView = (GobanView) findViewById(R.id.goban);
	Goban goban = new SimpleGoban();

	goban.move(3, 3, WHITE); 
	goban.move(3, 10, BLACK); 
	gobanView.setGoban(goban);
    }
}
