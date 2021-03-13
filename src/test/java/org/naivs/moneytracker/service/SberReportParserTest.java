package org.naivs.moneytracker.service;

import org.junit.jupiter.api.Test;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.enums.TransactionType;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SberReportParserTest {

	private final SberReportParser parser = new SberReportParser();

	@Test
	void parseCSV() {
		File report = new File(
				ClassLoader.getSystemResource("sber-test-report.csv").getFile());
		List<Transaction> transactions = parser.parseCSV(report).stream()
				.sorted(Comparator.comparing(Transaction::getTimestamp))
				.collect(Collectors.toList());

		assertEquals(33-1, transactions.size());

		// first line in file
		Transaction expected = getTransaction(
				LocalDateTime.of(2021, 2, 26, 0, 0),
				new BigDecimal("1500.00").setScale(2, RoundingMode.HALF_UP), "SBOL перевод 8201****1540 С. МАРИНА ЕВГЕНЬЕВНА", TransactionType.OUT
		);
		Transaction actual = transactions.get(transactions.size() - 1);
		System.out.println(expected.toString() + "\nvs\n" + actual.toString());
		assertEquals(expected.hashCode(), actual.hashCode());

		// last line in file
		expected = getTransaction(
				LocalDateTime.of(2021, 2, 1, 0, 0),
				new BigDecimal("1940.00").setScale(2, RoundingMode.HALF_UP), "BANK131 перевод 1111****4605 ", TransactionType.IN
		);
		actual = transactions.get(0);
		System.out.println(expected.toString() + "\nvs\n" + actual.toString());
		assertEquals(expected.hashCode(), actual.hashCode());
	}

	@Test
	void supports() {
		File report = new File(
				ClassLoader.getSystemResource("sber-test-report.csv").getFile());
		assertTrue(parser.supports(report));
	}

	private Transaction getTransaction(LocalDateTime timestamp, BigDecimal amount, String note, TransactionType type) {
		Transaction t = new Transaction();
		t.setTimestamp(timestamp);
		t.setNote(note);
		t.setAmount(amount);
		t.setType(type);
		return t;
	}
}