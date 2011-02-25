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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.MarkupModel.Type;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Value;

/**
 * A {@code GobanRenderer} can render a {@link Goban} on a {@link Canvas}.
 * 
 * @author Christian Gawron
 */
public class GobanRenderer {
	private static String TAG = "GobanRenderer";

	private static float STONE_RADIUS = 0.47f;
	private static float HOSHI_RADIUS = 0.15f;
	private static float HIGHLIGHT_RADIUS = 0.2f;
	private static float HIGHLIGHT_STROKEWIDTH = 0.06f;
	private static float MARKUP_STROKEWIDTH = 0.05f;
	private static float MARKUP_RADIUS = 0.41f;
	private static float STONE_STROKEWIDTH = 0.03f;
	private static float BOARD_STROKEWIDTH = 0.0f;
	private static float SELECTION_STROKEWIDTH = 0.05f;
	private static float COS30 = (float) Math.sqrt(0.75);
	private static float SIN30 = 0.5f;
	private static float SIN45 = (float) Math.sqrt(0.5);
	private static int SELECTION_COLOR = Color.RED;
	private static int VARIATION_COLOR = Color.argb(128, 128, 128, 128);

	private final GobanView view;

	public abstract class Markup {
		public abstract void draw(Canvas canvas, Goban goban);
	}

	public class VariationMark extends Markup {
		private Point point;

		public VariationMark(Point point) {
			this.point = point;
		}

		public void draw(Canvas canvas, Goban goban) {
			Log.d(TAG, "VariationMark: draw@" + point);
			short x = point.getX();
			short y = point.getY();
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStrokeWidth(SELECTION_STROKEWIDTH);
			paint.setColor(VARIATION_COLOR);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawCircle(x, y, STONE_RADIUS, paint);
		}
	}

	public class LastMoveMark extends Markup {
		private Point point;

		public LastMoveMark(Point point) {
			this.point = point;
		}

		public void draw(Canvas canvas, Goban goban) {
			BoardType stone = goban.getStone(point);
			Log.d(TAG, "LastMoveMark: draw@" + point);
			short x = point.getX();
			short y = point.getY();

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStrokeWidth(HIGHLIGHT_STROKEWIDTH);
			paint.setStyle(Paint.Style.STROKE);

			switch (stone) {
			case BLACK:
				paint.setARGB(255, 255, 255, 255);
				canvas.drawCircle(x, y, HIGHLIGHT_RADIUS, paint);
				break;
			case WHITE:
				paint.setARGB(255, 0, 0, 0);
				canvas.drawCircle(x, y, HIGHLIGHT_RADIUS, paint);
				break;
			default:
				break;
			}
		}
	}

	public class SGFMarkup extends Markup {
		private Point point;
		private BoardType stone;
		private MarkupModel.Type type;

		public SGFMarkup(Point point, BoardType stone, MarkupModel.Type type) {
			this.point = point;
			this.stone = stone;
			this.type = type;
		}

		public void draw(Canvas canvas, Goban goban) {
			Path path = null;
			short x = point.getX();
			short y = point.getY();
			Paint paint = new Paint();

			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setStrokeWidth(MARKUP_STROKEWIDTH);
			switch (stone) {
			case BLACK:
				paint.setARGB(255, 255, 255, 255);
				break;
			case WHITE:
			default:
				paint.setARGB(255, 0, 0, 0);
				break;
			}

			Log.d(TAG, String.format("SGFMarkup: draw %s@%s", type, point));
			float d = MARKUP_RADIUS * SIN45;
			switch (type) {
			case TRIANGLE:
				Log.d(TAG, String.format("SGFMarkup: draw %s@%s", type, point));
				path = new Path();
				path.moveTo(x, y - MARKUP_RADIUS);
				path.lineTo(x + MARKUP_RADIUS * COS30, y + MARKUP_RADIUS
						* SIN30);
				path.lineTo(x - MARKUP_RADIUS * COS30, y + MARKUP_RADIUS
						* SIN30);
				path.close();
				break;
			case SQUARE:
				Log.d(TAG, String.format("SGFMarkup: draw %s@%s", type, point));
				path = new Path();
				path.moveTo(x + d, y + d);
				path.lineTo(x + d, y - d);
				path.lineTo(x - d, y - d);
				path.lineTo(x - d, y + d);
				path.close();
				break;
			case MARK:
				Log.d(TAG, String.format("SGFMarkup: draw %s@%s", type, point));
				path = new Path();
				path.moveTo(x + d, y + d);
				path.lineTo(x - d, y - d);
				path.moveTo(x + d, y - d);
				path.lineTo(x - d, y + d);
				path.close();
				break;
			case CIRCLE:
				Log.d(TAG, String.format("SGFMarkup: draw %s@%s", type, point));
				path = new Path();
				path.addCircle(x, y, 0.25f, Path.Direction.CW);
				break;
			case TERRITORY_BLACK:
				paint.setARGB(210, 0, 0, 0);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				path = new Path();
				path.moveTo(x + d, y + d);
				path.lineTo(x + d, y - d);
				path.lineTo(x - d, y - d);
				path.lineTo(x - d, y + d);
				path.close();
				break;
			case TERRITORY_WHITE:
				paint.setARGB(210, 255, 255, 255);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				path = new Path();
				path.moveTo(x + d, y + d);
				path.lineTo(x + d, y - d);
				path.lineTo(x - d, y - d);
				path.lineTo(x - d, y + d);
				path.close();
				break;
			default:
				Log.e(TAG, String.format(
						"SGFMarkup: draw %s@%s (not implemented)", type, point));
				break;
			}
			if (path != null)
				canvas.drawPath(path, paint);
		}
	}

	public class Label extends Markup {
		private Point point;
		private String text;

		public Label(Point point, String text) {
			this.point = point;
			this.text = text;
		}

		public void draw(Canvas canvas, Goban goban) {
			Log.d(TAG, "VariationMark: draw@" + point);
			short x = point.getX();
			short y = point.getY();
			BoardType stone = goban.getStone(point);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setTextAlign(Paint.Align.CENTER);

			float size = paint.getTextSize();
			Paint.FontMetrics fm = paint.getFontMetrics();
			Rect bounds = new Rect();
			paint.getTextBounds(text, 0, text.length(), bounds);
			int h = bounds.height();
			int w = bounds.width();
			size /= (fm.top > 0) ? fm.top : -fm.top;
			size *= 0.8;
			paint.setTextSize(size);

			Log.d(TAG, String.format("drawText: h=%d, w=%d, a=%f, t=%f", h, w,
					fm.ascent, fm.top));

			paint.setStyle(Paint.Style.FILL);

			switch (stone) {
			case BLACK:
				paint.setARGB(255, 255, 255, 255);
				break;

			case EMPTY:
				paint.setARGB(255, 255, 255, 10);
				canvas.drawCircle(x, y, 0.5f, paint);
				// fall through
			case WHITE:
				paint.setARGB(255, 0, 0, 0);
				break;
			}
			canvas.drawText(text, x, y + 0.25f, paint);
		}
	}

	public GobanRenderer(GobanView view) {
		this.view = view;
	}

	public void render(Goban goban, Canvas canvas) {
		Paint paint = new Paint();
		int size = goban.getBoardSize();

		paint.setARGB(255, 255, 255, 10);
		canvas.drawRect(-0.5f, -0.5f, size - 0.5f, size - 0.5f, paint);
		paint.setARGB(255, 0, 0, 0);
		paint.setStrokeWidth(BOARD_STROKEWIDTH);
		paint.setStyle(Paint.Style.STROKE);
		for (int i = 0; i < size; i++) {
			canvas.drawLine(0, i, size - 1, i, paint);
			canvas.drawLine(i, 0, i, size - 1, paint);
		}

		drawHoshi(size, canvas, paint);

		if (goban != null) {
			for (short i = 0; i < size; i++) {
				for (short j = 0; j < size; j++) {
					BoardType stone = goban.getStone(i, j);
					MarkupModel.Markup markup = null;
					if (goban instanceof MarkupModel) {
						markup = ((MarkupModel) goban).getMarkup(i, j);
					}
					switch (stone) {
					case BLACK:
					case WHITE:
						drawStone(i, j, stone, canvas);
						break;
					case EMPTY:
						break;
					}
				}
			}
			for (Markup markup : view.getMarkup()) {
				markup.draw(canvas, goban);
			}
		}
	}

	Paint paint = new Paint();

	void drawStone(int i, int j, BoardType stone, Canvas canvas) {
		paint.setAntiAlias(true);
		paint.setStrokeWidth(STONE_STROKEWIDTH);

		switch (stone) {
		case BLACK:
			paint.setARGB(255, 0, 0, 0);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			canvas.drawCircle(i, j, STONE_RADIUS, paint);
			break;
		case WHITE:
			paint.setARGB(255, 255, 255, 255);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawCircle(i, j, STONE_RADIUS, paint);
			paint.setARGB(255, 0, 0, 0);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawCircle(i, j, STONE_RADIUS, paint);
			break;
		default:
			break;
		}
	}

	void drawHoshi(int boardSize, Canvas canvas, Paint paint) {
		int m = boardSize >> 1;
		int h = boardSize > 9 ? 3 : 2;

		paint.setAntiAlias(true);
		paint.setARGB(230, 0, 0, 0);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);

		if (boardSize % 2 != 0) {
			canvas.drawCircle(m, m, HOSHI_RADIUS, paint);

			if (boardSize > 9) {
				canvas.drawCircle(h, m, HOSHI_RADIUS, paint);
				canvas.drawCircle(boardSize - h - 1, m, HOSHI_RADIUS, paint);
				canvas.drawCircle(m, h, HOSHI_RADIUS, paint);
				canvas.drawCircle(m, boardSize - h - 1, HOSHI_RADIUS, paint);
			}
		}

		if (boardSize > 7) {
			canvas.drawCircle(h, h, HOSHI_RADIUS, paint);
			canvas.drawCircle(h, boardSize - h - 1, HOSHI_RADIUS, paint);
			canvas.drawCircle(boardSize - h - 1, h, HOSHI_RADIUS, paint);
			canvas.drawCircle(boardSize - h - 1, boardSize - h - 1,
					HOSHI_RADIUS, paint);
		}
	}

	/*
	 * void drawSelection(int x, int y, Canvas canvas) { Log.d("GobanRenderer",
	 * String.format("drawSelection(%d, %d)", x, y));
	 * 
	 * Paint paint = new Paint(); paint.setAntiAlias(true);
	 * paint.setStrokeWidth(SELECTION_STROKEWIDTH);
	 * paint.setColor(SELECTION_COLOR); paint.setStyle(Paint.Style.STROKE);
	 * canvas.drawRect(x-0.5f, y-0.5f, x+0.5f, y+0.5f, paint); }
	 */
}