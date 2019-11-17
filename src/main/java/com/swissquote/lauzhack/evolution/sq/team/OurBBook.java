package com.swissquote.lauzhack.evolution.sq.team;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

import com.swissquote.lauzhack.evolution.api.*;
import com.swissquote.lauzhack.evolution.api.Currency;


class CurrencyComparator implements Comparator<MyCurrency>{
	@Override
	public int compare(MyCurrency mc1, MyCurrency mc2) {
		return mc1.getBalance().compareTo(mc2.getBalance());
	}
}


class MyCurrency{
	private boolean isCHF;
	private Currency currency;
	private BigDecimal movingAvg;
	private BigDecimal nbSamples;
	private BigDecimal rate;
	private BigDecimal balance;
	private BigDecimal markup;
	private Queue<Trade> last2Trades;
	
	public MyCurrency(boolean isCHF, Currency currency, BigDecimal rate, BigDecimal balance, 
			BigDecimal markup) {
		this.isCHF = isCHF;
		this.currency = currency;
		this.rate = rate;
		this.movingAvg = rate;
		this.nbSamples = BigDecimal.ONE;
		this.balance = balance;
		this.markup = markup;
		this.last2Trades = new ArrayDeque<>();
	}
	
	public BigDecimal getBalance() { // CHF
		return balance.multiply(rate);
	}
	
	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getRate(){
		return rate;
	}

	public BigDecimal getMarkup(){
		return markup;
	}

	public BigDecimal demandPrediction() {
		if(last2Trades.size() > 2) {
			Queue<Trade> qCopy = new ArrayDeque<>(last2Trades);
			Trade c_2 = qCopy.remove();
			Trade c_1 = qCopy.remove();
			return c_1.quantity.multiply(new BigDecimal(2)).subtract(c_2.quantity);
		}
		return null;
	}

	public void addToQueue(Trade trade) {
		if(last2Trades.size() > 2) { last2Trades.remove(); }
		last2Trades.offer(trade);
	}
	
	public void giveToClient(BigDecimal amount) {
		if(isCHF) {
			balance = balance.subtract(amount).add(new BigDecimal(10));
		}else {
			balance = balance.subtract(amount);
		}
	}
	
	public void receiveFromClient(BigDecimal amount, MyCurrency c) {
		if(isCHF) {
			balance = balance.add(
					amount.multiply(c.getRate()).multiply(c.getMarkup().add(BigDecimal.ONE)))
					.add(new BigDecimal(10));
		}else {
			balance = balance.add(amount.divide(rate.multiply(BigDecimal.ONE.subtract(markup)),MathContext.DECIMAL128));
		}
	}
	
	public void giveToMarket(BigDecimal amount, MyCurrency c) {
		if(isCHF) {
			//BigDecimal balance = balance.subtract(amount.multiply(c.getRate()).multiply(new BigDecimal(1).add(c.getMarkup()))).subtract(new BigDecimal(100));
			
			balance = balance.subtract(amount.multiply(c.getRate()).multiply(new BigDecimal(1).add(c.getMarkup())))
					.subtract(new BigDecimal(100));
			
		}else {
			balance = balance.subtract(amount.divide(rate.multiply(new BigDecimal(1).subtract(markup)), 
					MathContext.DECIMAL128));
		}
	}
	
	public void receiveFromMarket(BigDecimal amount) {
		if(isCHF) {
			balance = balance.add(amount).subtract(new BigDecimal(100));
		}else {
			balance = balance.add(amount);
		}
	}
	
	public void rateChange(BigDecimal newRate) {
		rate = newRate;
		movingAvg = (movingAvg.multiply(nbSamples).add(newRate)).divide(nbSamples.add(BigDecimal.ONE), 
				MathContext.DECIMAL128);
		nbSamples = nbSamples.add(BigDecimal.ONE);
	}
	
	public BigDecimal risk() {
		return (rate.subtract(movingAvg)).divide(movingAvg);
	}
}

/**
 * This is a very simple example of implementation.
 * This class can be completely discarded.
 */
public class OurBBook implements BBook {

	// Save a reference to the bank in order to pass orders
	private Bank bank;
	private MyCurrency usd;
	private MyCurrency eur;
	private MyCurrency chf;
	private MyCurrency jpy;
	private MyCurrency gbp;
	boolean printed = false;

	Map<Currency,MyCurrency> mapos = new HashMap<Currency,MyCurrency>();

	@Override
	public void onInit() {
		usd = new MyCurrency(false, Currency.USD, new BigDecimal(0.99), new BigDecimal(0), 
				new BigDecimal(0.001));
		eur = new MyCurrency(false, Currency.EUR, new BigDecimal(1.09), new BigDecimal(0), 
				new BigDecimal(0.001));
		chf = new MyCurrency(true, Currency.CHF, new BigDecimal(1), new BigDecimal(20000000), 
				new BigDecimal(0.000));
		jpy = new MyCurrency(false, Currency.JPY, new BigDecimal(0.0091), new BigDecimal(0), 
				new BigDecimal(0.003));
		gbp = new MyCurrency(false, Currency.GBP, new BigDecimal(1.27), new BigDecimal(0), 
				new BigDecimal(0.0005));

		mapos.put(Currency.USD, usd);
		mapos.put(Currency.EUR, eur);
		mapos.put(Currency.CHF, chf);
		mapos.put(Currency.JPY, jpy);
		mapos.put(Currency.GBP, gbp);
		
		BigDecimal eurChange = new BigDecimal(3500000);
		BigDecimal usdChange = new BigDecimal(4000000);
		BigDecimal jpyChange = new BigDecimal(100000000);
		BigDecimal gbpChange = new BigDecimal(1700000);
		
		
		usd.receiveFromMarket(usdChange);
		eur.receiveFromMarket(eurChange);
		jpy.receiveFromMarket(jpyChange);
		gbp.receiveFromMarket(gbpChange);
		chf.giveToMarket(gbpChange, gbp);
		chf.giveToMarket(jpyChange, jpy);
		chf.giveToMarket(usdChange, usd);
		chf.giveToMarket(eurChange, eur);
		
		// Start by buying some cash. Don't search for more logic here: numbers are just random..
		bank.buy(new Trade(Currency.EUR, Currency.CHF, eurChange));
		bank.buy(new Trade(Currency.JPY, Currency.CHF, jpyChange));
		bank.buy(new Trade(Currency.USD, Currency.CHF, usdChange));
		bank.buy(new Trade(Currency.GBP, Currency.CHF, gbpChange));

		/*System.out.println("USD balance:"+usd.getBalance().divide(usd.getRate()));
		System.out.println("EUR balance:"+eur.getBalance().divide(eur.getRate()));
		System.out.println("JPY balance:"+jpy.getBalance().divide(jpy.getRate()));
		System.out.println("GBP balance:"+gbp.getBalance().divide(gbp.getRate()));
		System.out.println("CHF balance:"+chf.getBalance());*/

	}

	@Override
	public void onTrade(Trade trade) {
		// Raise on low risk
		
		//System.out.println(usd.getBalance());
		//System.out.println(chf.getBalance());
		
		BigDecimal times_chf = new BigDecimal(5);
		BigDecimal times_other = new BigDecimal(2);
		BigDecimal rate = new BigDecimal(1);
		
		// It would certainly be wise to store the available amount per currency..
		/*
		switch(trade.term) {
		case CHF:
			chf.receiveFromClient(trade.quantity, mapos.get(trade.base));
			break;
		case EUR:
			eur.receiveFromClient(trade.quantity, mapos.get(trade.base));
			rate = eur.getRate();
			break;
		case GBP:
			gbp.receiveFromClient(trade.quantity, mapos.get(trade.base));
			rate = gbp.getRate();
			break;
		case JPY:
			jpy.receiveFromClient(trade.quantity, mapos.get(trade.base));
			rate = jpy.getRate();
			break;
		case USD:
			usd.receiveFromClient(trade.quantity, mapos.get(trade.base));
			rate = usd.getRate();
		default:
		}
		*/
		mapos.get(trade.term).receiveFromClient(trade.quantity, mapos.get(trade.base));
		rate = mapos.get(trade.term).getRate();
		switch(trade.base) {
		case CHF:
			if(chf.getBalance().compareTo(trade.quantity.multiply(rate)) < 0) {
				List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
				MyCurrency best = Collections.max(list, new CurrencyComparator());
				best.giveToMarket(trade.quantity.multiply(times_chf),chf);
				chf.receiveFromMarket(trade.quantity.multiply(times_chf));
				bank.buy(new Trade(Currency.CHF, best.getCurrency(), trade.quantity.multiply(times_chf)));
			}
			chf.giveToClient(trade.quantity);
			chf.addToQueue(trade);
			break;
		case EUR:
			if(eur.getBalance().compareTo(trade.quantity) < 0) {
				chf.giveToMarket(trade.quantity.multiply(times_other),chf);
				eur.receiveFromMarket(trade.quantity.multiply(times_other));
				bank.buy(new Trade(Currency.EUR, Currency.CHF, trade.quantity.multiply(times_other)));
			}
			eur.giveToClient(trade.quantity);
			eur.addToQueue(trade);
			break;
		case JPY:
			if(jpy.getBalance().compareTo(trade.quantity) < 0) {
				chf.giveToMarket(trade.quantity.multiply(times_other),chf);
				jpy.receiveFromMarket(trade.quantity.multiply(times_other));
				bank.buy(new Trade(Currency.JPY, Currency.CHF, trade.quantity.multiply(times_other)));
			}
			jpy.giveToClient(trade.quantity);
			jpy.addToQueue(trade);
			break;
		case USD:
			if(usd.getBalance().compareTo(trade.quantity) < 0) {
				chf.giveToMarket(trade.quantity.multiply(times_other),chf);
				usd.receiveFromMarket(trade.quantity.multiply(times_other));
				bank.buy(new Trade(Currency.USD, Currency.CHF, trade.quantity.multiply(times_other)));
			}
			usd.giveToClient(trade.quantity);
			usd.addToQueue(trade);
			break;
		case GBP:
			if(gbp.getBalance().compareTo(trade.quantity) < 0) {
				chf.giveToMarket(trade.quantity.multiply(times_other),chf);
				gbp.receiveFromMarket(trade.quantity.multiply(times_other));
				bank.buy(new Trade(Currency.GBP, Currency.CHF, trade.quantity.multiply(times_other)));
			}
			gbp.giveToClient(trade.quantity);
			gbp.addToQueue(trade);
		default:
		}
	}

	@Override
	public void onPrice(Price price) {
		// It would certainly be wise to store the prices somewhere to take educated decision..
		if(price.base == Currency.CHF) {
			switch(price.term) {
			case EUR:
				eur.rateChange(price.rate);
				break;
			case USD:
				usd.rateChange(price.rate);
				break;
			case JPY:
				jpy.rateChange(price.rate);
				break;
			case GBP:
				gbp.rateChange(price.rate);
			default:
			}
		}else {
			switch(price.term) {
			case EUR:
				eur.rateChange(new BigDecimal(1).divide(price.rate));
				break;
			case USD:
				usd.rateChange(new BigDecimal(1).divide(price.rate));
				break;
			case JPY:
				jpy.rateChange(new BigDecimal(1).divide(price.rate));
				break;
			case GBP:
				gbp.rateChange(new BigDecimal(1).divide(price.rate));
			default:
			}
		}

		decide(MarketProfile.SOMETHING);
	}

	private void decide(MarketProfile profile) {
		if(usd != null){
			//System.out.println("decide");
			List<MyCurrency> currencies =  Arrays.asList(usd, eur, chf, jpy, gbp);
			switch(profile) {
				case SOMETHING:
				case UNICORN:
					MyCurrency mostRisky = currencies.get(0);
					//System.out.println(mostRisky);
					BigDecimal mostRiskyRisk = currencies.get(0).risk();
					MyCurrency lessRisky = currencies.get(0);
					BigDecimal lessRiskyRisk = currencies.get(0).risk();

					for(MyCurrency c: currencies) {
						if(c.risk().compareTo(mostRiskyRisk) > 0) { mostRisky = c; }
						if(c.risk().compareTo(lessRiskyRisk) < 0) { lessRisky = c; }
					}
					//sell
					if(mostRiskyRisk.compareTo(BigDecimal.ONE) > 0 && mostRisky.getBalance().doubleValue() > 2*mostRisky.demandPrediction().doubleValue()) {
						BigDecimal pred = mostRisky.demandPrediction();
						if(pred != null && pred.compareTo(BigDecimal.ZERO) > 0) { mostRisky.giveToMarket(pred, lessRisky); };
					}
					//buy
					if(lessRiskyRisk.compareTo(BigDecimal.ONE.negate()) < 0 && lessRisky.getBalance().doubleValue() < 2*lessRisky.demandPrediction().doubleValue()) {
						BigDecimal pred = lessRisky.demandPrediction();
						if (pred != null && pred.compareTo(BigDecimal.ZERO) < 0) {
							lessRisky.receiveFromMarket(pred);
						}
					}
					break;
				//case IT_WORKS:
					//break;
				case STARTUP:
					break;
				default:

			}
		}
	}

	@Override
	public void setBank(Bank bank) {
		this.bank = bank;
	}
}
