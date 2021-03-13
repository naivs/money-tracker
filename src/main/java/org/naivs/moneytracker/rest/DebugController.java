package org.naivs.moneytracker.rest;

import lombok.RequiredArgsConstructor;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.repo.TransactionRepo;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/debug")
public class DebugController {

	private final TransactionRepo transactionRepo;

	@PutMapping("transaction")
	public Transaction createTransaction(@RequestBody Transaction transaction) {
		return transactionRepo.save(transaction);
	}

	@GetMapping("transaction/all")
	public Collection<Transaction> getAll() {
		return transactionRepo.findAll();
	}
}
