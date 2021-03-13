package org.naivs.moneytracker.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class SberReportParser extends AbstractReportParser {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@Override
	public Collection<Transaction> parseCSV(File file) {
		Set<Transaction> transactions = new HashSet<>();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			CSVReader csvReader = new CSVReaderBuilder(br)
					.withSkipLines(1)
					.withCSVParser(
							new CSVParserBuilder()
									.withSeparator(';')
									.build()
					)
					.build();
			for (String[] line : csvReader) {
				if (line.length >= 11) {
					Transaction t = new Transaction();
					BigDecimal amount = new BigDecimal(
							line[11].replace(',', '.'))
							.setScale(2, RoundingMode.HALF_UP);
					t.setType(amount.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.IN : TransactionType.OUT);
					t.setAmount(amount.abs());
					t.setNote(line[8]);
					t.setTimestamp(LocalDate.parse(line[2], DATE_FORMATTER).atTime(0, 0));
					t.setHash(t.hashCode());
					transactions.add(t);
				}
			}
			return transactions;
		} catch (IOException e) {
			return Set.of();
		}
	}

	@Override
	public boolean supports(File file) {
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file))) {
			long skipped = isr.skip(1);
			char[] bytes = new char[22];
			int read = isr.read(bytes);
			if (read + skipped == 23 && "Тип карты;Номер карты;".equals(String.valueOf(bytes)))
				return true;
		} catch (IOException e) {
			log.warn(e.getLocalizedMessage());
		}
		log.info("Parser is not supported");
		return false;
	}
}
