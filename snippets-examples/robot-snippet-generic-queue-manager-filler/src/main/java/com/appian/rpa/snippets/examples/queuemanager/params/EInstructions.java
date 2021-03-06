package com.appian.rpa.snippets.examples.queuemanager.params;

import com.appian.rpa.snippets.instruction.Instruction;

/**
 * 
 * Enum in which all the instructions of a robot are defined
 *
 */
public enum EInstructions {

	FOLDER("folder", true);

	private Instruction instruction;

	EInstructions(String instructionName, Boolean instructionRequred) {
		instruction = new Instruction(instructionName, instructionRequred);
	}

	public Instruction getInstruction() {

		return instruction;
	}
}