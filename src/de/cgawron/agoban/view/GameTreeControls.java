
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
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.cgawron.agoban.R;
import de.cgawron.go.sgf.GameTree;

/**
 * {@code GameTreeControls} allow to navigate through a game tree.
 *
 */
public class GameTreeControls extends LinearLayout
{
    public interface GameTreeNavigationListener 
    {
    }

    private GameTree gameTree;
    private GameTreeNavigationListener listener;
    
    private final Button buttonNext;
    private final Button buttonPrev;
    private final TextView moveNoView;

    public GameTreeControls(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
	buttonNext = new Button(context);
	buttonPrev = new Button(context);
	moveNoView = new TextView(context);
	buttonNext.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
	buttonPrev.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
	moveNoView.setText("-");

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GobanView);

        a.recycle();
	initView(context);
    }

    private void initView(Context context)
    {
	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
	params.gravity  = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	addView(buttonNext, params);
	params.gravity = Gravity.CENTER;
	addView(moveNoView, params);
	params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
	addView(buttonPrev, params);
    }
}
