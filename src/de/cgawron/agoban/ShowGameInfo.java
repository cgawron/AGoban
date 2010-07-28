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

package de.cgawron.agoban;

import de.cgawron.go.Goban;
import static de.cgawron.go.Goban.BoardType.WHITE;
import static de.cgawron.go.Goban.BoardType.BLACK;
import de.cgawron.go.SimpleGoban;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.Node;

import de.cgawron.agoban.view.GobanView;
import de.cgawron.agoban.intent.GameInfo;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageItemInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Shows the game info
 */
public class ShowGameInfo extends Activity
{
    public static Resources resources;

    private GameTree gameTree;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	resources = getResources();
	try {
	    PackageItemInfo info = getPackageManager().getActivityInfo(new ComponentName(this, EditSGF.class), PackageManager.GET_META_DATA);
	}
	catch (Exception e)
	{
	    throw new RuntimeException("git-id", e);
	}
	Log.d("ShowGameInfo", "starting!");

        setContentView(R.layout.game_info);
    }
}
