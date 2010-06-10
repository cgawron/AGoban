/*
 *
 * $Id: Point.java 289 2005-08-15 09:44:31Z cgawron $
 *
 * (c) 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * 
 */
package de.cgawron.go;

import de.cgawron.go.Point;
import java.beans.PropertyChangeListener;

/**
 * Represents the state (i.e. the position of all the stones on the board) of a goban.
 * @author Christian Gawron
 * @version $Id: GobanModel.java 369 2006-04-14 17:04:02Z cgawron $
 * @see Goban
 */
public interface Goban extends Cloneable
{
    public enum BoardType { 
	EMPTY, BLACK, WHITE;

	BoardType opposite() {
	    switch (this) {
	    case BLACK: return WHITE;
	    case WHITE: return BLACK;
	    default: 
		return EMPTY;
	    }
	}
    }

    /** 
     * Create an empty goban of the same size. 
     */
    Goban newInstance();

    /**
     * Constants used for Zobrist hashing.
     * @see #zobristHash
     */
    public int[] zobrist = 
    {
	0x01d1072b, 0x70e87214, 0x2a8d0e50, 0x31c52658, 0x4a544bdc, 0x77308958, 0x05a09446, 
	0x1e5b5112, 0x1401c094, 0x32bc8122, 0x3f000c12, 0x299162b4, 0x5556948e, 0x2fb20b00, 
	0x5e4e7508, 0x759b2840, 0x197872f0, 0x25b94d2a, 0x168fb5de, 0x70f572e4, 0x71ad81c8, 
	0x5d7fe980, 0x7fad6bba, 0x43dc6ef2, 0x76e1010c, 0x2633a48a, 0x5ff55922, 0x4dd49ae6, 
	0x5b23899a, 0x5b09ac76, 0x0599750a, 0x33225e64, 0x0b27ec36, 0x78bb801c, 0x302d99ca, 
	0x7a51413a, 0x5fc5b090, 0x43057600, 0x0a2cd428, 0x39afc258, 0x5f76343e, 0x7a26e344,
	0x32735248, 0x54ceceda, 0x0fdc69ba, 0x2063e2ba, 0x55c2f2be, 0x4a06d906, 0x5738376e,
	0x4f86bbdc, 0x58407386, 0x0ff08f48, 0x6df376a6, 0x1e59e9c6, 0x0ef54100, 0x02ea3332, 
	0x60199296, 0x162ec242, 0x0950e4f4, 0x62989080, 0x1ed7c760, 0x3858bcc0, 0x6aa11596, 
	0x5f5f9416, 0x22020106, 0x13b15956, 0x539e6480, 0x37eb8300, 0x19915a78, 0x6a14a7c4, 
	0x5be08db2, 0x66a8d172, 0x2f203c8a, 0x65e26e74, 0x1b656698, 0x5f4a99c8, 0x576e1cfa, 
	0x5f7ff174, 0x440e6d02, 0x5c47b384, 0x33dddd6e, 0x32d4597c, 0x4b20f628, 0x3c596a52, 
	0x03f6ca76, 0x24cdbaea, 0x22c5fdb0, 0x6fffd508, 0x26e53a84, 0x58dbbcca, 0x038a94a2, 
	0x76fbcca0, 0x2cf7ccf8, 0x5671dfc8, 0x45e1f06e, 0x6dd4833e, 0x79214918, 0x1e89653c, 
	0x4b9f19f0, 0x2b53bb2e, 0x727b1d10, 0x511dd628, 0x797c3c68, 0x6e079de4, 0x45be7cf8, 
	0x62b02a32, 0x40989482, 0x19c432fa, 0x36cc21cc, 0x61c58eaa, 0x37f2ff4e, 0x051e7736, 
	0x0226a872, 0x4c65cd8a, 0x45f12946, 0x56f01384, 0x4e7690b8, 0x759c546e, 0x59de5d4c, 
	0x73dab3f4, 0x2e05dc54, 0x7afadba0, 0x3565f3aa, 0x7bbcfec6, 0x30434de4, 0x4a14ad0e, 
	0x461a6110, 0x573e2b5c, 0x034547ec, 0x36038b52, 0x0ef0e59a, 0x55493942, 0x25404bdc, 
	0x1ce138aa, 0x21c1dbca, 0x588dc72c, 0x26111908, 0x1d519e00, 0x28cba788, 0x14fc89f4, 
	0x78b66bb0, 0x5bf691e0, 0x002f4526, 0x5d4b63a0, 0x54d45bf8, 0x58f83f86, 0x5f72785e, 
	0x00761a5a, 0x28315cee, 0x55852dce, 0x12fe9b52, 0x373baf78, 0x1839db86, 0x494ce644, 
	0x45a00d8c, 0x219b5674, 0x137c7da2, 0x68023c14, 0x5a0a42fc, 0x545e0bf2, 0x732c0104, 
	0x36df3316, 0x54517300, 0x768eccbc, 0x2ca1ba5a, 0x321f86cc, 0x536ee754, 0x6ef96cca,
	0x12d7b43c, 0x0284d15c, 0x1abeeb26, 0x21b20e34, 0x27ed6e26, 0x42d05296, 0x2b845a08, 
	0x3d8f3840, 0x7ea63b80, 0x554e56f2, 0x6e63cef0, 0x19aa8e58, 0x2be94294, 0x49483e8e, 
	0x6ce77342, 0x22e22712, 0x48421b9c, 0x50902436, 0x1afc6dc8, 0x59598326, 0x01610fb2, 
	0x44fa2456, 0x3073f1b4, 0x5df5247c, 0x3b98f7dc, 0x3ddd4408, 0x1282ed9e, 0x0fe1c51e, 
	0x435fbc0c, 0x3598f80a, 0x4d435a8a, 0x3a0c2e3e, 0x0ed27212, 0x3f094bc6, 0x1d322ae0, 
	0x50a7af3a, 0x0f301cb0, 0x4b15a848, 0x08b1d974, 0x3ea93496, 0x125b0a8a, 0x7e263b4a, 
	0x0985987c, 0x434cfefc, 0x345dd52e, 0x15a6fe2e, 0x4658209e, 0x5f8ffa42, 0x60df0012, 
	0x5d865cf8, 0x5c381fe0, 0x53b4b460, 0x64b23794, 0x35bc7d58, 0x381448b6, 0x2cc2f47e, 
	0x0f482ff8, 0x27be25aa, 0x004400f8, 0x6203f1e6, 0x6439246e, 0x0c0a43e8, 0x0e00b0ea, 
	0x02842b0a, 0x5cc1f60c, 0x3fa014be, 0x76e5b236, 0x729adc88, 0x393feacc, 0x6985ea7a, 
	0x2676deea, 0x39591824, 0x62b60c98, 0x503f9414, 0x0797c084, 0x5e1d7bc8, 0x5c4d283c, 
	0x3f5d642a, 0x3e125ee2, 0x56038888, 0x44d06c6c, 0x7cefd808, 0x7013af52, 0x016e99a2,
	0x682fa8ca, 0x4f08de9a, 0x1faea382, 0x39857b76, 0x60fc2cd8, 0x1141bf78, 0x1456d88c, 
	0x6db2a294, 0x49f04704, 0x5a37a1e4, 0x5465e5f6, 0x512302ac, 0x7f6563a6, 0x310ab3c4, 
	0x2e994ca4, 0x184b859c, 0x70dacbfa, 0x52def0aa, 0x539405ec, 0x465f2266, 0x02740528, 
	0x085c88ca, 0x7f59b284, 0x0adb9438, 0x7e7acf3a, 0x0b2fe806, 0x32d36992, 0x2efdb166, 
	0x16d6c428, 0x7b8a5ce6, 0x7d50279e, 0x13476268, 0x769996d0, 0x659e5f98, 0x55ffdd98, 
	0x3cff38e6, 0x6375f1f8, 0x572778f6, 0x05781fdc, 0x73b0a6a8, 0x6f42f74c, 0x04dec980, 
	0x1f33752a, 0x6ea8d4a4, 0x397c5e64, 0x0006305a, 0x0b0e45ae, 0x127a19f6, 0x40c233bc, 
	0x7be21258, 0x1ec969f8, 0x3e7c12e2, 0x445737ba, 0x233ac0ee, 0x478b5a02, 0x2a4857d0,
	0x35a2ddac, 0x7ddc8140, 0x435dfc2c, 0x533dd45a, 0x39b0eac8, 0x66163312, 0x6aac1444,
	0x79fdbe36, 0x3aaeb52a, 0x0e3f97d6, 0x09c9527a, 0x2fef2c4a, 0x097202dc, 0x162e39a4, 
	0x4a6b0266, 0x74bc4da4, 0x0ef188c8, 0x30bff238, 0x2d295a0a, 0x63d44074, 0x0dc809c4,
	0x595411fe, 0x130268d2, 0x71e88916, 0x73b6158e, 0x194f2704, 0x56d7a68e, 0x3a6f2912,
	0x3b1e67ce, 0x5d9b6402, 0x76ee2278, 0x5d235610, 0x5c87e710, 0x5c22a758, 0x4e6f5232, 
	0x6b84d0e4, 0x501e43c8, 0x0956220e, 0x5ee980c6, 0x6652dc8a, 0x1f303758, 0x1daa9ed2, 
	0x29cbdda4, 0x4b15d526, 0x05b36302, 0x2b1868fe, 0x19ed2ba8, 0x31966704, 0x23bccef4, 
	0x429bd390, 0x7d071014, 0x79656598, 0x3f080946
    };
    
    /**
     * Adds a {@link GobanListener} for this model.
     * @param l the {@link GobanListener} to be added.
     */
    void addGobanListener(GobanListener l);

    /**
     * Get the number of stones captured by black.
     * Stones may be captured when a {@link #move} is executed.
     * @return the number of captured black stones.
     */
    int getBlackCaptured();

    /**
     * Get the number of stones captured by white.
     * Stones may be captured when a {@link #move} is executed.
     * @return the number of captured white stones.
     */
    int getWhiteCaptured();

    /**
     * Get the stone at <code>Point</code> p.
     * @param p - point at which to get the stone.
     * @return the stone at p.
     */
    BoardType getStone(Point p);

    /**
     * Place a move by <code>color</code> at <code>p</code>
     * @param p the <code>Point</code> where to move
     * @param color of the move
     */
    void move(Point p, BoardType color);

    /**
     * Place a move by <code>color</code> at <code>p</code>
     * @param p the <code>Point</code> where to move
     * @param color of the move
     * @param moveNo move number (@see MarkupModel.move())
     */
    void move(Point p, BoardType color, int moveNo);


    /**
     * Remove a {@link GobanListener} for the model.
     * @param l a <code>GobanListener</code> value
     */
    void removeGobanListener(GobanListener l);

    /**
     * Get the size of the board.
     * @return the size of the board.
     */
    int getBoardSize();

    /**
     * Get the stone at the specified coordinates.
     * @param x a <code>int</code> value
     * @param y a <code>int</code> value
     * @return a <code>BoardType</code> value
     */
    BoardType getStone(int x, int y);

    /**
     *  Place a move by <code>color</code> at the specified coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param color of the move
     */
    void move(int x, int y, BoardType color);

    /**
     * Put a stone of <code>color</code> at the specified coordinates.
     * This method does not capture any stones but just adds stone. Use the {@link move} method to play a move.
     * @param x a <code>int</code> value
     * @param y a <code>int</code> value
     * @param color a <code>BoardType</code> value
     * @see #move
     */
    void putStone(int x, int y, BoardType color);

    /**
     * Put a stone of <code>color</code> at the specified point. This method does not capture any stones but just adds stone.
     * Use the {@link move} method to play a move.
     * @param p the <code>Point</code> where to place the stone
     * @param color of the move
     * @see #move
     */
    void putStone(Point p, BoardType color);

    /**
     * Set the size of the board. This method will usually clear the board.
     * @param size the new size of the board
     */
    void setBoardSize(int size);

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is registered for all properties.
     * @param listener The PropertyChangeListener to be added
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered for all properties.
     * @param listener The PropertyChangeListener to be removed
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Adds a PropertyChangeListener for a specific property. The listener will be invoked only when a call on firePropertyChange
     * names that specific property.
     * @param propertyName The name of the property to listen on
     * @param listener The PropertyChangeListener to be added
     */
    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener for a specific property.
     * @param propertyName The name of the property that was listened on
     * @param listener The PropertyChangeListener to be removed
     */
    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

    /**
     * Calculate the Zobrist hash of the Goban.
     * The Zobrist hash is a hash function which is invariant under symmetry operations (rotation, color invertion etc.).
     * @see Symmetry
     * @return the Zobrist hash.
     */
    int zobristHash();

    /** 
     * check if this model is equal to s.transform(o) 
     * @param o The object to compare with
     * @param s the symmetry to apply to o when comparing 
     */
    boolean equals(Object o, Symmetry s);

    /** 
     * check if this model is equal to o 
     * @param Object o The object to compare with
     */
    boolean equals(Object o);

    /** 
     * transform the model with a symmetry operation
     * @param Symmetry s the symmetry to apply 
     */
    Goban transform(Symmetry s);

    /** 
     * Clear the board 
     */
    void clear();

    /**
     * Copy a different Goban into this one.
     * @param m - the Goban to copy.
     */
    void copy(Goban m);

    /**
     * clone the Goban
     */
    Goban clone() throws CloneNotSupportedException;
}
