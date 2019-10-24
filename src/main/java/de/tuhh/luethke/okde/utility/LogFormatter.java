package de.tuhh.luethke.okde.utility;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(record.getSourceMethodName());
		sb.append(" - ");
		sb.append(record.getMessage());
		sb.append(System.lineSeparator());
		
		return sb.toString();
	}
}