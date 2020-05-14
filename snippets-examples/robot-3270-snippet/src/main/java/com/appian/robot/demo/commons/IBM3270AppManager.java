package com.appian.robot.demo.commons;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.appian.robot.demo.pages.WelcomePage;
import com.appian.rpa.snippet.IBM3270Commons;
import com.appian.rpa.snippet.page.IBM3270Page;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IWaitFor;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.exceptions.JidokaException;
import com.novayre.jidoka.client.api.exceptions.JidokaFatalException;
import com.novayre.jidoka.client.api.exceptions.JidokaUnsatisfiedConditionException;
import com.novayre.jidoka.windows.api.IWindows;
import com.novayre.jidoka.windows.api.Process;
import com.novayre.jidoka.windows.api.WindowInfo;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * Class for opening and closing the 3270 Terminal
 */
public class IBM3270AppManager {

	/**
	 * Maximum number of attempts to open the APP
	 */
	public static final int APP_MAX_OPEN_RETRIES = 3;

	/**
	 * Pause in seconds
	 */
	public static final int LONG_WAIT_SECONDS = 60;

	/**
	 * Default pause in milliseconds
	 */
	public static final long WAIT_MENU_MILLISECONDS = 500;

	/**
	 * Server instance
	 */
	private IJidokaServer<Serializable> server;

	/**
	 * Windows module instance
	 */
	private IWindows windows;

	/**
	 * Waitfor module instance
	 */
	private IWaitFor waitFor;

	/** IBM3270Commons snippet instance */
	private IBM3270Commons commons;

	/**
	 * Default Constructor
	 * 
	 * @param robot
	 * @param emulator
	 */
	@SuppressWarnings("unchecked")
	public IBM3270AppManager(IBM3270Commons commons) {

		this.server = (IJidokaServer<Serializable>) JidokaFactory.getServer();
		this.windows = IWindows.getInstance(commons.getRobot());
		this.waitFor = windows.getWaitFor(commons.getRobot());
		this.commons = commons;

	}

	/**
	 * Open the 3270 terminal, a symbolic link is used to open it
	 * 
	 * @param processName
	 * @param titleExpected
	 * @throws JidokaException
	 */
	public IBM3270Page openIBM3270() throws JidokaException {

		String app = String.format("%s\\config\\%s.lnk", server.getCurrentDir(), getLinkName());

		for (int i = 1; i <= APP_MAX_OPEN_RETRIES; i++) {

			server.debug(String.format("Trying open [%s] with title [%s]. %s de %s", app, commons.getWindowTitleRegex(),
					i, APP_MAX_OPEN_RETRIES));

			try {
				Runtime.getRuntime().exec(String.format("rundll32 SHELL32.DLL,ShellExec_RunDLL %s", app));
			} catch (IOException e1) {
				server.debug(e1);
				continue;
			}

			AtomicReference<HWND> ieAtomic = new AtomicReference<>();

			try {
				waitFor.wait(LONG_WAIT_SECONDS, "Waiting for the application to open", false, () -> {
					WindowInfo window = windows.getWindow(commons.getWindowTitleRegex());
					if (window != null) {
						ieAtomic.set(window.gethWnd());
						return true;
					}

					return false;
				});
			} catch (JidokaUnsatisfiedConditionException e) {
				;
			}

			if (ieAtomic.get() != null) {
				WelcomePage welcomePage = new WelcomePage(commons);

				try {
					welcomePage.assertIsThisPage();
				} catch (JidokaFatalException e) {
					server.debug(e);
					continue;
				}

				return welcomePage;
			}
		}

		throw new JidokaFatalException(String.format("The App %s could not be opened ", app));
	}

	/**
	 * Close 3270 terminal.
	 */
	public void closeIBM3270(String windowTitle) {

		try {

			List<Process> processes = windows.getProcesses(commons.getProcessName(), true);

			if (processes.size() == 0) {
				return;
			}

		} catch (IOException e1) {
		}

		quit(windowTitle);

		windows.pause(1000);

		// Checks that all instances of the application were closed
		try {
			windows.killAllProcesses(commons.getProcessName(), 1);
		} catch (IOException e) {
		}
	}

	/**
	 * Quit terminal
	 * 
	 * @param windowTitle
	 */
	public void quit(String windowTitle) {

		windows.activateWindow(windowTitle);

		windows.pause(1000);

		windows.getKeyboard().alt("n").pause(WAIT_MENU_MILLISECONDS).end().pause(WAIT_MENU_MILLISECONDS).enter();
	}

	/**
	 * Extract the name of the link from the name of the process
	 * 
	 * @return
	 */
	private String getLinkName() {
		return commons.getProcessName().replace(".exe", "").toUpperCase();
	}
}
