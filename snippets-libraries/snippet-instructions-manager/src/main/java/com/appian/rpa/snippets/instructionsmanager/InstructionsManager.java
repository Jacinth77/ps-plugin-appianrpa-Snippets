package com.appian.rpa.snippets.instructionsmanager;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.exceptions.JidokaFatalException;

/**
 * The InstructionsManager Class provides multiple methods to manage
 * Instructions from your Worflow.
 * 
 * Therefore, each single instance would represent a different instruction from
 * your Appian RPA Robotic Process.
 */
public class InstructionsManager {

	/**
	 * JidokaServer instance.
	 */
	private IJidokaServer<?> server;

	/**
	 * Instruction name
	 */
	private String name;

	/**
	 * <code>true</code> if the instruction is required (mandatory)
	 */
	private Boolean required;

	/**
	 * This variable stores the instruction value in the console
	 */
	private String parameter;

	/**
	 * Constructor for a non required instruction
	 * 
	 * @param name of the instruction
	 */
	public InstructionsManager(String name) {

		this(name, false);
	}

	/**
	 * Constructor for a single instruction
	 * 
	 * @param name     of the instruction
	 * @param required <code>true</code> if the instruction is required (mandatory)
	 *                 for the robot
	 */
	public InstructionsManager(String name, Boolean required) {

		this.server = JidokaFactory.getServer();
		this.name = name;
		this.required = required;

		this.parameter = server.getParameters().get(name);
	}

	/**
	 * Returns the instruction parameter value as a Boolean
	 * 
	 * @return
	 */
	public Boolean getAsBoolean() {

		checkRequired();

		return Boolean.valueOf(parameter);
	}

	/**
	 * Return the instruction parameter value as an Integer
	 * 
	 * @return
	 */
	public Integer getAsInteger() {

		checkRequired();

		if (StringUtils.isBlank(parameter)) {

			return null;
		}

		if (!NumberUtils.isNumber(parameter)) {
			throw new JidokaFatalException(
					String.format("The param %s must be a number. The received value is %s", name, parameter));
		}

		return Integer.parseInt(parameter);
	}

	/**
	 * Return the instruction parameter value as a Long
	 * 
	 * @return
	 */
	public Long getAsLong() {

		checkRequired();

		if (StringUtils.isBlank(parameter)) {

			return null;
		}

		if (!NumberUtils.isNumber(parameter)) {
			throw new JidokaFatalException(
					String.format("The param %s must be a number. The received value is %s", name, parameter));
		}

		return Long.parseLong(parameter);
	}

	/**
	 * Returns the instruction parameter value as a String
	 * 
	 * @return
	 */
	public String getAsString() {

		checkRequired();

		return parameter;
	}

	/**
	 * Returns the instruction parameter value as a File
	 * 
	 * @return
	 */
	public File getAsFile() {

		checkRequired();

		return Paths.get(server.getCurrentDir(), parameter).toFile();
	}

	private void checkRequired() {

		if (StringUtils.isBlank(parameter) && required) {
			throw new JidokaFatalException(String.format("The param %s is mandatory", name));
		}
	}

}