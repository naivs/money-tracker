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
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class VtbReportParser extends AbstractReportParser {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public Collection<Transaction> parseCSV(File file) {
		Set<Transaction> transactions = new HashSet<>();

		try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("cp1251")))) {
			CSVReader csvReader = new CSVReaderBuilder(br)
					.withSkipLines(12)
					.withCSVParser(
							new CSVParserBuilder()
									.withSeparator(';')
									.build()
					)
					.build();
			for (String[] line : csvReader) {
				if (line.length >= 7) {
					Transaction t = new Transaction();
					BigDecimal amount = new BigDecimal(
							line[3].replace(',', '.').replaceAll("\\s", ""))
							.setScale(2, RoundingMode.HALF_UP);
					t.setType(amount.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.IN : TransactionType.OUT);
					t.setAmount(amount.abs());
					t.setNote(line[7]);
					t.setTimestamp(LocalDateTime.parse(line[1], DATE_TIME_FORMATTER));
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
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), Charset.forName("cp1251"))) {
			char[] bytes = new char[14];
			int read = isr.read(bytes);
			if (read == 14 && "Начало периода".equals(String.valueOf(bytes)))
				return true;
		} catch (IOException e) {
			log.warn(e.getLocalizedMessage());
		}
		log.info("Parser is not supported");
		return false;
	}
}
