package org.naivs.moneytracker.bot;

import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateMachine {

	private Map<Long, BotState> stateMap = new ConcurrentHashMap<>();
	private Map<Long, Transaction> store = new ConcurrentHashMap<>();

	public void setState(Long user, BotState state) {
		stateMap.put(user, state);
	}

	public <T> void setState(Long user, BotState state, T value) {
		setState(user, state);
		store.putIfAbsent(user, new Transaction());

		if (value instanceof Boolean) {
			store.computeIfPresent(user, (k, v) -> {
				v.setType(((Boolean) value) ? TransactionType.IN : TransactionType.OUT);
				return v;
			});
			return;
		}

		if (value instanceof BigDecimal) {
			store.computeIfPresent(user, (k, v) -> {
				v.setAmount((BigDecimal) value);
				return v;
			});
			return;
		}

		if (value instanceof LocalDate) {
			store.computeIfPresent(user, (k, v) -> {
				v.setTimestamp(((LocalDate) value).atTime(0, 0));
				return v;
			});
			return;
		}

		if (value instanceof String) {
			store.computeIfPresent(user, (k, v) -> {
				v.setNote(value.toString());
				return v;
			});
		}
	}

	public Transaction getValue(Long user) {
		return store.get(user);
	}

	public void clear(Long user) {
		stateMap.remove(user);
		store.remove(user);
	}

	public boolean inState(Long user) {
		return stateMap.containsKey(user);
	}

	public BotState getState(Long user) {
		return stateMap.get(user);
	}
}
