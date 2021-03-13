package org.naivs.moneytracker.service;

import lombok.RequiredArgsConstructor;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.enums.TransactionType;
import org.naivs.moneytracker.repo.TransactionRepo;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BudgetService {

	private final TransactionRepo transactionRepo;
	private final Collection<FinanceReportParser> parsers;

	/**
	 * Get available budget in current month.
	 * @return available amount
	 */
	public BigDecimal availableBudget() {
		Pair<LocalDateTime, LocalDateTime> period = currentMonthPeriod();
		Map<Boolean, Optional<BigDecimal>> summarized = transactionRepo
				.findAllBetween(period.getFirst(), period.getSecond()).stream()
				.collect(Collectors.partitioningBy(t -> TransactionType.IN.equals(t.getType()),
						Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal::add))));
		return summarized.get(true).orElse(BigDecimal.ZERO).subtract(summarized.get(false).orElse(BigDecimal.ZERO));
	}

	/**
	 * Get original budget amount at current month.
	 * @return original amount
	 */
	public BigDecimal originalAmount() {
		Pair<LocalDateTime, LocalDateTime> period = currentMonthPeriod();
		return transactionRepo.findAllBetween(period.getFirst(), period.getSecond())
				.stream().filter(t -> TransactionType.IN.equals(t.getType()))
				.map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Get wasted budget amount at current month.
	 * @return wasted amount
	 */
	public BigDecimal wastedAmount() {
		Pair<LocalDateTime, LocalDateTime> period = currentMonthPeriod();
		return transactionRepo.findAllBetween(period.getFirst(), period.getSecond())
				.stream().filter(t -> TransactionType.OUT.equals(t.getType()))
				.map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Parse and save transactions from finance report.
	 * Supports Vtb and Sber .csv reports.
	 * Automatically determines parser.
	 * @param report report file in .csv
	 * @return status
	 */
	public boolean saveFinanceReport(File report) {
		return parsers.stream()
				.dropWhile(p -> !p.supports(report))
				.findFirst()
				.map(p -> {
					Collection<Transaction> transactions = p.parseCSV(report);
					transactionRepo.saveAll(transactions);
					return true;
				}).orElse(false);
	}

	/**
	 * Save transaction
	 * @param t transaction
	 * @return saved transaction
	 */
	public Transaction saveTransaction(Transaction t) {
		return transactionRepo.save(t);
	}

	private Pair<LocalDateTime, LocalDateTime> currentMonthPeriod() {
		LocalDateTime from = LocalDateTime.now(ZoneId.of("+3")).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
		LocalDateTime to = from.plusMonths(1L).minusSeconds(1L);
		return Pair.of(from, to);
	}
}
