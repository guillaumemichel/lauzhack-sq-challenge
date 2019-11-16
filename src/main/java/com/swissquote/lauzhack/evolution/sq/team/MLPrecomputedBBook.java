package com.swissquote.lauzhack.evolution.sq.team;

import com.swissquote.lauzhack.evolution.api.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.stream.Collectors;

public class MLPrecomputedBBook implements BBook {

	// Save a reference to the bank in order to pass orders
	private Bank bank;

	@Override
	public void onInit() {
		// Start by buying some cash.
		try {
			String result = new BufferedReader(new InputStreamReader(Runtime.getRuntime()
					.exec("python init.py").getInputStream()))
					.lines().collect(Collectors.joining("\n"));
			System.out.println(result);
		} catch(IOException e) {}
	}

	@Override
	public void onTrade(Trade trade) {
		// It would certainly be wise to store the available amount per currency..

		// We cover market 5% of times, for twice the value. Cause why not ??
		if (Math.random() < 0.05) {
			Trade coverTrade = new Trade(trade.base, trade.term, trade.quantity.multiply(new BigDecimal(2)));
			bank.buy(coverTrade);
		}
	}

	@Override
	public void onPrice(Price price) {
		// It would certainly be wise to store the prices somewhere to take educated decision..
	}

	@Override
	public void setBank(Bank bank) {
		this.bank = bank;
	}
}
