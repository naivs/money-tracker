package org.naivs.moneytracker.service;

import org.naivs.moneytracker.entity.Transaction;

import java.io.File;
import java.util.Collection;

public interface FinanceReportParser {

	Collection<Transaction> parseCSV(File file);
	Collection<Transaction> parseExcel(File file);
	Collection<Transaction> parseXml(File file);

	boolean supports(File file);
}
