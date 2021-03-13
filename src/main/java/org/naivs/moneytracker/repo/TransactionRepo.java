package org.naivs.moneytracker.repo;

import lombok.RequiredArgsConstructor;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.enums.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class TransactionRepo {

	private final JdbcTemplate jdbcTemplate;

	public Collection<Transaction> findAll() {
		return jdbcTemplate.query(
				"select * from transactions", transactionRowMapper());
	}

	public Collection<Transaction> findAllBetween(LocalDateTime from, LocalDateTime to) {
		String query = "select * from transactions " +
				"where timestamp between ? and ?";
		return jdbcTemplate.query(query, transactionRowMapper(), from, to);
	}

	public Transaction save(Transaction t) {
		t.setHash(t.hashCode());
		int updated = jdbcTemplate.update("insert into transactions values (?,?,?,?,?)",
				t.getHash(), t.getAmount(), t.getType().name(), t.getNote(), t.getTimestamp());

		return updated == 1 ? t : null;
	}

	public Collection<Transaction> saveAll(Collection<Transaction> ts) {
		List<Object[]> args = ts.stream()
				.peek(t -> t.setHash(t.hashCode()))
				.map(t -> new Object[] {
						t.getHash(), t.getAmount(), t.getType().name(), t.getNote(), t.getTimestamp()
				}).collect(Collectors.toList());
		int[] updated = jdbcTemplate.batchUpdate("insert into transactions values (?,?,?,?,?)", args);
		for (int u : updated) {
			if (u != 1)
				throw new RuntimeException("Unable to save transactions as batch");
		}

		return ts;
	}

	private RowMapper<Transaction> transactionRowMapper() {
		return (rs, rowNum) -> {
			Transaction transaction = new Transaction();
			transaction.setHash(rs.getInt("hash"));
			transaction.setAmount(rs.getBigDecimal("amount"));
			transaction.setType(TransactionType.valueOf(rs.getString("type")));
			transaction.setNote(rs.getString("note"));
			transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
			return transaction;
		};
	}
}
