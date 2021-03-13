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

class VtbReportParserTest {

	private final VtbReportParser parser = new VtbReportParser();

	@Test
	void parseCSV() {
		File report = new File(
				ClassLoader.getSystemResource("vtb-test-report.csv").getFile());
		List<Transaction> transactions = parser.parseCSV(report).stream()
				.sorted(Comparator.comparing(Transaction::getTimestamp))
				.collect(Collectors.toList());

		assertEquals(68-12, transactions.size());

		// first line in file
		Transaction actual = transactions.get(transactions.size() - 1);
		Transaction expected = getTransaction(
				LocalDateTime.of(2021, 2, 28, 18, 53, 51),
				new BigDecimal("1500").setScale(2, RoundingMode.HALF_UP), "Джейн Доу Н*", TransactionType.OUT
		);
		System.out.println(expected.toString() + "\nvs\n" + actual.toString());
		assertEquals(expected.hashCode(), actual.hashCode());

		// last line in file
		expected = getTransaction(
				LocalDateTime.of(2021, 1, 31, 11, 45, 8),
				new BigDecimal("314.9").setScale(2, RoundingMode.HALF_UP), "Карта *3496 FARMANI", TransactionType.OUT
		);
		actual = transactions.get(0);
		System.out.println(expected.toString() + "\nvs\n" + actual.toString());
		assertEquals(expected.hashCode(), actual.hashCode());
	}

	@Test
	void supports() {
		File report = new File(
				ClassLoader.getSystemResource("vtb-test-report.csv").getFile());
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
