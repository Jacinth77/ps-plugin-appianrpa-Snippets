package com.appian.rpa.snippet.clients;

import com.appian.rpa.snippet.IBM3270Commons;
import com.novayre.jidoka.client.api.IRobot;

/**
 * IBM3270Commons extension for wc3270
 */
public class WC3270EmulatorManager extends IBM3270Commons {

	/**
	 * The Constant WINDOW_TITLE_REGEX.
	 */
	private static final String WINDOW_TITLE_REGEX = ".*3270";

	/**
	 * Process Name wc3270
	 */
	public static final String PROCESS_NAME = "wc3270.exe";

	/**
	 * Default X-coordinate
	 */
	private static final int MAX_COORD_X = 80;

	/**
	 * Default Y-coordinate
	 */
	private static final int MAX_COORD_Y = 24;

	/**
	 * Default constructor
	 * 
	 * @param server
	 * @param client
	 * @param robot
	 */
	public WC3270EmulatorManager(IRobot robot) {

		super(robot);

		setMaxCoordX(MAX_COORD_X);
		setMaxCoordY(MAX_COORD_Y);
	}

	/**
	 * Select all text in the screen
	 */
	@Override
	public void selectAllText() {

		moveToBottonRightCorner();
		keyboard.control("a").pause();
	}

	/**
	 * Activate a window by title
	 */
	@Override
	public void activateWindow() {

		client.activateWindow(WINDOW_TITLE_REGEX);

		client.pause();
	}

	/**
	 * Move the cursor to the bottom right corner of the screen
	 */
	@Override
	public void moveToBottonRightCorner() {

		keyboard.down().pause();
		keyboard.control("g").pause().control("g").pause();
	}

	/**
	 * Split the lines of text on the screen
	 */
	@Override
	public String[] splitScreenLines(String screen) {
		return screen.split("(?<=\\G.{80})");
	}

	/**
	 * Returns the window title regex
	 * 
	 * @return
	 */
	@Override
	public String getWindowTitleRegex() {
		return WINDOW_TITLE_REGEX;
	}

	/**
	 * Returns the process name
	 * 
	 * @return
	 */
	@Override
	public String getProcessName() {
		return PROCESS_NAME;
	}
}
