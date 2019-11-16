package com.swissquote.lauzhack.evolution.sq.team;

import com.swissquote.lauzhack.evolution.api.*;
import com.swissquote.lauzhack.evolution.api.Currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a very simple example of implementation.
 * This class can be completely discarded.
 */
public class HedgeBBook implements BBook {

	// Save a reference to the bank in order to pass orders
	private Bank bank;

	private Map<Currency, BigDecimal> money = new HashMap<>();
	private Map<Currency, List<BigDecimal>> currencyDemand = new HashMap<>();
	private Map<Currency, BigDecimal> currencyPredictions = new HashMap<>();
	private Map<Currency, List<BigDecimal>> currencyPrice = new HashMap<>();
	private Map<Currency, BigDecimal> changeOnPrice = new HashMap<>();
	private int t = 0;

	private Thread thread;

	HedgeBBook() {
		thread = new Thread(() -> {
			ReentrantLock l = new ReentrantLock();
			while(true) {
				if(t > 4) { improviseAdaptOvercome(); }

				l.lock();
				try { t += 1; } finally { l.unlock(); }
				try { Thread.sleep(1000); } catch (Exception e) { }
			}
		});
		thread.start();
	}

	@Override
	public void onInit() {

		for(Currency currency: Currency.values()) {
			currencyDemand.put(currency, initArray());
			//currencyDemand.put(currency, new ArrayList<>());

			if(currency != Currency.CHF) {
				currencyPrice.put(currency, initArray());
				//currencyPrice.put(currency, new ArrayList<>());
			}
		}

		money.put(Currency.CHF, new BigDecimal(9000000));
		money.put(Currency.JPY, new BigDecimal(1000000));
		money.put(Currency.USD, new BigDecimal(4000000));
		money.put(Currency.EUR, new BigDecimal(4000000));
		money.put(Currency.GBP, new BigDecimal(2000000));

		money.forEach((currency, amount) -> {
			if(currency != Currency.CHF) {
				bank.buy(new Trade(currency, Currency.CHF, amount));
			}
		});
	}

	private List<BigDecimal> initArray() {
		ArrayList<BigDecimal> arr = new ArrayList<>();
		for(int i = 0; i < 1000; i++) {
			arr.add(new BigDecimal(0));
		}
		return arr;
	}

	@Override
	public void onTrade(Trade trade) {
		//System.out.println("trade t= "+t);
		if(t > 2){
			// Add what user wants to buy
			currencyDemand.computeIfPresent(trade.base, (k, v) -> {
				List<BigDecimal> demands = v;
				demands.set(t, demands.get(t).add(trade.quantity));
				return demands;
			});
			//BigDecimal rate = trade.base != Currency.CHF ? currencyPrice.get(trade.term).get(t-1) : new BigDecimal(1).divide(currencyPrice.get(trade.term).get(t-2),MathContext.DECIMAL128);
			BigDecimal rate = trade.base != Currency.CHF ? currencyPrice.get(trade.base).get(t-1) : new BigDecimal(1).divide(currencyPrice.get(trade.term).get(t-1),MathContext.DECIMAL128);
			// Remove what they give us in exchange
			currencyDemand.computeIfPresent(trade.term, (k, v) -> {
				List<BigDecimal> demands = v;
				//demands.add(t, demands.size() >= t ? trade.quantity.multiply(rate).multiply(new BigDecimal(-1)) : demands.get(t).subtract(trade.quantity.multiply(rate)));
				demands.set(t, demands.get(t).subtract(trade.quantity.multiply(rate)));
				return demands;
			});

			if(t > 2) { predictCurrencies(); }

			//improviseAdaptOvercome();
		}
		//System.out.println("currency demand for " + trade.base.toString() + " "+ currencyDemand.get(trade.base));
		//System.out.println("currency demand for " + trade.term.toString() + " "+ currencyDemand.get(trade.term));
		//System.out.println("prediction of demand for " + trade.term.toString() + " "+ currencyPredictions.get(trade.term));
	}

	@Override
	public void onPrice(Price price) {
		//System.out.println("on price t= "+t);
		Currency currency = price.base == Currency.CHF ? price.term : price.base;
		BigDecimal rate = price.base == Currency.CHF ? new BigDecimal(1).divide(price.rate): price.rate;

		//currencyPrice.putIfAbsent(currency, new ArrayList<>(Arrays.asList(rate)));

		currencyPrice.computeIfPresent(currency, (k,v) -> {
			List<BigDecimal> changes = v;
			//changes.add(t, rate);
			changes.set(t,rate);
			return changes;
		});

		if(t > 2) {
			BigDecimal change = currencyPrice.get(currency).get(t-1).divide(currencyPrice.get(currency).get(t-2), MathContext.DECIMAL128);
			changeOnPrice.put(currency, change);
		}
		//System.out.println("currency price for " + price.base.toString() + "to "+ price.term + " " +  currencyPrice.get(currency));
	}

	private void predictCurrencies() {
		currencyDemand.entrySet().stream().forEach((entry) -> {
			BigDecimal prediction = entry.getValue().get(t - 1).multiply(new BigDecimal(2)).subtract(entry.getValue().get(t - 2));
			currencyPredictions.put(entry.getKey(), prediction);
		});
	}

	private void improviseAdaptOvercome(){
		for(Currency currency: Currency.values()) {
			BigDecimal whatWeNeed = currencyPredictions.get(currency);
			BigDecimal mulFactor = changeOnPrice.get(currency);
			int powFactor = 100;
			if(whatWeNeed != null && mulFactor!=null){
				mulFactor = mulFactor.pow(powFactor);
				System.out.println("mulFactor " + mulFactor);
				// If positive
				if(whatWeNeed.abs().equals(whatWeNeed) && currency != Currency.CHF) {
					BigDecimal rate = currencyPrice.get(currency).get(t-1);
					//bank.buy(new Trade(currency, Currency.CHF, whatWeNeed.multiply(mulFactor)));
					//money.compute(currency,(k,x)->whatWeNeed.multiply(mulFactor).add(x));
					buy(currency,Currency.CHF,whatWeNeed.multiply(mulFactor),rate);
					//System.out.println("buy " + currency.toString() + "to sell "+ Currency.CHF.toString() + "  amount " +  whatWeNeed.multiply(mulFactor));
				}
				// If negative
				if(!whatWeNeed.abs().equals(whatWeNeed) && currency != Currency.CHF) {
					BigDecimal rate = currencyPrice.get(currency).get(t-1);
					//bank.buy(new Trade(Currency.CHF, currency, whatWeNeed.multiply(new BigDecimal(-1).multiply(mulFactor)).multiply(new BigDecimal(-1).multiply(currencyPrice.get(currency).get(t-1)))));
					//money.compute()
					sell(Currency.CHF,currency,whatWeNeed.multiply(mulFactor),rate);
					//System.out.println("to buy " + Currency.CHF.toString() + "sell "+ currency.toString() + "  amount " +  whatWeNeed.multiply(mulFactor));
				}

				if(currency == Currency.CHF) {
					BigDecimal rate = currencyPrice.get(currency).get(t-1).multiply(new BigDecimal(-1));
					BigDecimal max = new BigDecimal(0);
					Currency curr = Currency.CHF;
					for(Map.Entry<Currency, BigDecimal> entry: changeOnPrice.entrySet()) {
						if(entry.getValue().subtract(max).compareTo(new BigDecimal(0)) > 0) { max = entry.getValue(); curr = entry.getKey(); }
					}
					max = max.pow(powFactor);
					if(whatWeNeed.abs().equals(whatWeNeed)){
						//bank.buy(new Trade(Currency.CHF,curr,whatWeNeed.multiply(max)));
						buy(Currency.CHF,curr,whatWeNeed.multiply(max),rate);
					}else{
						sell(curr,Currency.CHF,whatWeNeed.multiply(max).multiply(new BigDecimal(-1)),rate);
						//bank.buy(new Trade(curr,Currency.CHF,whatWeNeed.multiply(max).multiply(new BigDecimal(-1))));
					}
				}
			}
		}
	}

	@Override
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	private void buy(Currency buy, Currency sell, BigDecimal amountBuy, BigDecimal rate){
		if(amountBuy.compareTo(new BigDecimal(0)) <= 0){return;}
		bank.buy(new Trade(buy, sell , amountBuy));
		money.compute(buy,(k,x)-> x.add(amountBuy));
		if(buy == Currency.CHF){
			money.compute(sell,(k,x)->x.subtract(amountBuy.multiply(rate)));
		}else{
			money.compute(sell,(k,x)->x.subtract(amountBuy.multiply(rate).multiply(new BigDecimal(-1))));
		}

	}
	private void sell(Currency buy, Currency sell, BigDecimal amountSell, BigDecimal rate){
		if(amountSell.compareTo(new BigDecimal(0)) <= 0){return;}
		money.compute(sell,(k,x)-> x.subtract(amountSell));
		if(buy == Currency.CHF){
			bank.buy(new Trade(buy, sell , amountSell.multiply(rate)));
			money.compute(buy,(k,x)->x.add(amountSell.multiply(rate).multiply(new BigDecimal(-1))));
		}else{
			bank.buy(new Trade(buy, sell , amountSell.multiply(rate).multiply(new BigDecimal(-1))));
			money.compute(buy,(k,x)->x.subtract(amountSell.multiply(rate)));
		}
	}

	public void stop() {
		thread.stop();
		System.out.println("stoooooooop");
	}
}
