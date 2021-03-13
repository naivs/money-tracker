package org.naivs.moneytracker.service;

import org.naivs.moneytracker.entity.Transaction;

import java.io.File;
import java.util.Collection;

public abstract class AbstractReportParser implements FinanceReportParser {
	@Override
	public Collection<Transaction> parseXml(File file) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Collection<Transaction> parseExcel(File file) {
		return null;
	}
}
