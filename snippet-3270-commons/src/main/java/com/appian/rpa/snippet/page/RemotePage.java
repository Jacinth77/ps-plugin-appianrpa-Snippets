package com.appian.rpa.snippet.page;

import com.appian.rpa.snippet.ConstantsWaits;
import com.appian.rpa.snippet.IBM3270Commons;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.exceptions.JidokaException;
import com.novayre.jidoka.windows.api.IWindows;


/**
 * Abstract class to be extended for each type of page
 */
public abstract class RemotePage extends IBM3270Commons {
	
	
	/**
	 * Unique text on each page for recognition
	 * @return
	 */
	public abstract String getUnivocalRegex();
	
	
	/**
	 * Constructor
	 * @param server
	 * @param windows
	 * @param robot
	 */
	public RemotePage(IJidokaServer<?> server, IWindows windows, IRobot robot) {
		super(server, windows, robot);
	}

	/**
	 * Check that the robot is on the correct page (current page)
	 * 
	 * @throws JidokaGenericException
	 */
	public RemotePage assertIsThisPage() throws JidokaException {

		try {
			locateText(ConstantsWaits.HIGH_NUMBER_OF_RETRIES_LOCATING_TEXT, getUnivocalRegex());
		} catch (Exception e) {
			server.debug(e.getMessage());
			throw new JidokaException(getPageName());
		}
		
		return this;
	}

	/**
	 * Indicates the name of the current page (class)
	 * @return
	 */
	public String getPageName() {
		return this.getClass().getSimpleName();
	}

}
