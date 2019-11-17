package com.swissquote.lauzhack.evolution.sq.team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private BigDecimal rate;
	private List<BigDecimal> rates;
	private BigDecimal balance;
	private BigDecimal markup;
	private Queue<Trade> last2Trades;
	
	public MyCurrency(boolean isCHF, Currency currency, BigDecimal rate, BigDecimal balance, 
			BigDecimal markup) {
		this.isCHF = isCHF;
		this.currency = currency;
		this.rate = rate;
		this.rates = new ArrayList<>();
		rates.add(rate);
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
		rates.add(newRate);
	}
	
	public int nbRates() {
		return rates.size();
	}
	
	public BigDecimal movingAverage() {
		BigDecimal sum = BigDecimal.ZERO;
		for(BigDecimal bd : rates) {
			sum = sum.add(bd);
		}
		return sum.divide(new BigDecimal(rates.size()), MathContext.DECIMAL128);
	}
	
	
	public BigDecimal movingStd() {
		BigDecimal avg = movingAverage();
		BigDecimal var = BigDecimal.ZERO;
		for(BigDecimal rate : rates) {
			BigDecimal diff = rate.subtract(avg);
			var = var.add(diff.multiply(diff));
		}
		return var.divide(new BigDecimal(rates.size()), MathContext.DECIMAL128)
				.sqrt(MathContext.DECIMAL128);
	}
	
	public BigDecimal risk() {
		return (rate.subtract(movingAverage())).divide(movingAverage());
	}
}

/**
 * This is a very simple example of implementation.
 * This class can be completely discarded.
 */
public class OurBBook implements BBook {

	// Save a reference to the bank in order to pass orders
	private Bank bank;
	
	private MyCurrency usd = new MyCurrency(false, Currency.USD, new BigDecimal(0.99), new BigDecimal(0), 
			new BigDecimal(0.001));
	private MyCurrency eur = new MyCurrency(false, Currency.EUR, new BigDecimal(1.09), new BigDecimal(0), 
			new BigDecimal(0.001));
	private MyCurrency chf = new MyCurrency(true, Currency.CHF, new BigDecimal(1), new BigDecimal(20000000), 
			new BigDecimal(0.000));
	private MyCurrency jpy = new MyCurrency(false, Currency.JPY, new BigDecimal(0.0091), new BigDecimal(0), 
			new BigDecimal(0.003));
	private MyCurrency gbp = new MyCurrency(false, Currency.GBP, new BigDecimal(1.27), new BigDecimal(0), 
			new BigDecimal(0.0005));
  private MarketProfile profile = null;
	
	Currency currList[] = {Currency.USD, Currency.EUR, Currency.CHF, Currency.JPY, Currency.GBP};
	private Map<Currency, Map<Currency, List<BigDecimal>>> mapimap = new HashMap<>();
	
	Map<Currency,MyCurrency> mapos = new HashMap<Currency,MyCurrency>();

	public BigDecimal movingAverage(List<BigDecimal> rates) {
		BigDecimal sum = BigDecimal.ZERO;
		for(BigDecimal bd : rates) {
			sum = sum.add(bd);
		}
		return sum.divide(new BigDecimal(rates.size()), MathContext.DECIMAL128);
	}
	
	public BigDecimal movingStd(List<BigDecimal> rates) {
		BigDecimal avg = movingAverage(rates);
		BigDecimal var = BigDecimal.ZERO;
		for(BigDecimal rate : rates) {
			BigDecimal diff = rate.subtract(avg);
			var = var.add(diff.multiply(diff));
		}
		return var.divide(new BigDecimal(rates.size()), MathContext.DECIMAL128)
				.sqrt(MathContext.DECIMAL128);
	}
	
	@Override
	public void onInit() {
		for(Currency curr : currList) {
			Map<Currency, List<BigDecimal>> map = new HashMap<>();
			for(Currency curr2 : currList) {
				if(curr2 != curr) {
					map.put(curr2, new ArrayList<>());
				}
			}
			mapimap.put(curr, map);
		}
		
		mapos.put(Currency.USD, usd);
		mapos.put(Currency.EUR, eur);
		mapos.put(Currency.CHF, chf);
		mapos.put(Currency.JPY, jpy);
		mapos.put(Currency.GBP, gbp);
		
		BigDecimal eurChange = new BigDecimal(4000000);
		BigDecimal usdChange = new BigDecimal(4000000);
		BigDecimal jpyChange = new BigDecimal(100000000);
		BigDecimal gbpChange = new BigDecimal(2000000);
		
		
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
		
		mapimap.get(trade.base).get(trade.term).add(trade.quantity);
		
		BigDecimal threshold = new BigDecimal(1000000);
		
		mapos.get(trade.term).receiveFromClient(trade.quantity, mapos.get(trade.base));
		switch(trade.base) {
		case CHF:
			chf.giveToClient(trade.quantity);
			if(chf.getBalance().compareTo(threshold) <= 0) {
				//System.out.println("CHF critic "+chf.getBalance()+", "+trade.quantity);
				List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
				MyCurrency best = Collections.max(list, new CurrencyComparator());
				best.giveToMarket(threshold,chf);
				chf.receiveFromMarket(threshold);
				bank.buy(new Trade(Currency.CHF, best.getCurrency(), threshold
						));
			}
			chf.giveToClient(trade.quantity);
			chf.addToQueue(trade);
			break;
		case EUR:
			eur.giveToClient(trade.quantity);
			if(eur.getBalance().compareTo(threshold) <= 0) {
				//System.out.println("EUR critic");

				chf.giveToMarket(threshold,eur);
				eur.receiveFromMarket(threshold);
				bank.buy(new Trade(Currency.EUR, Currency.CHF, threshold));
				if(chf.getBalance().compareTo(threshold) <= 0) {
					//System.out.println("CHF critic "+chf.getBalance()+", "+trade.quantity);
					List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
					MyCurrency best = Collections.max(list, new CurrencyComparator());
					best.giveToMarket(threshold,chf);
					chf.receiveFromMarket(threshold);
					bank.buy(new Trade(Currency.CHF, best.getCurrency(), threshold
							));
				}
			}
			eur.giveToClient(trade.quantity);
			eur.addToQueue(trade);

			break;
		case JPY:
			jpy.giveToClient(trade.quantity);
			if(jpy.getBalance().compareTo(threshold) <= 0) {
				//System.out.println("JPY critic");
				chf.giveToMarket(threshold,jpy);
				jpy.receiveFromMarket(threshold);
				bank.buy(new Trade(Currency.JPY, Currency.CHF, threshold));
				if(chf.getBalance().compareTo(threshold) <= 0) {
					//System.out.println("CHF critic "+chf.getBalance()+", "+trade.quantity);
					List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
					MyCurrency best = Collections.max(list, new CurrencyComparator());
					best.giveToMarket(threshold,chf);
					chf.receiveFromMarket(threshold);
					bank.buy(new Trade(Currency.CHF, best.getCurrency(), threshold
							));
				}
			}
			jpy.giveToClient(trade.quantity);
			jpy.addToQueue(trade);

			break;
		case USD:
			usd.giveToClient(trade.quantity);
			if(usd.getBalance().compareTo(threshold) <= 0) {
				//System.out.println("USD critic");

				chf.giveToMarket(threshold,usd);
				usd.receiveFromMarket(threshold);
				bank.buy(new Trade(Currency.USD, Currency.CHF, threshold));
				if(chf.getBalance().compareTo(threshold) <= 0) {
					//System.out.println("CHF critic "+chf.getBalance()+", "+trade.quantity);
					List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
					MyCurrency best = Collections.max(list, new CurrencyComparator());
					best.giveToMarket(threshold,chf);
					chf.receiveFromMarket(threshold);
					bank.buy(new Trade(Currency.CHF, best.getCurrency(), threshold
							));
				}
			}
			usd.giveToClient(trade.quantity);
			usd.addToQueue(trade);

			break;
		case GBP:
			gbp.giveToClient(trade.quantity);
			////System.out.println(gbp.getBalance());
			if(gbp.getBalance().compareTo(threshold) <= 0) {
				//System.out.println("GBP critic "+gbp.getBalance()+", "+trade.quantity);
				chf.giveToMarket(threshold,gbp);
				gbp.receiveFromMarket(threshold);
				bank.buy(new Trade(Currency.GBP, Currency.CHF, threshold));
				if(chf.getBalance().compareTo(threshold) <= 0) {
					//System.out.println("CHF critic "+chf.getBalance()+", "+trade.quantity);
					List<MyCurrency> list = Arrays.asList(eur, usd, jpy, gbp);
					MyCurrency best = Collections.max(list, new CurrencyComparator());
					best.giveToMarket(threshold,chf);
					chf.receiveFromMarket(threshold);
					bank.buy(new Trade(Currency.CHF, best.getCurrency(), threshold
							));
				}
				//System.out.println(gbp.getBalance());
			}
			gbp.giveToClient(trade.quantity);
			gbp.addToQueue(trade);
		default:
		}
    if(profile != null){
      decide(profile);
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

    
		if(mapimap.size() > 0 && mapimap.get(Currency.USD).get(Currency.CHF).size() > 100) {
			List<BigDecimal> usd_chf = mapimap.get(Currency.USD).get(Currency.CHF);
			List<BigDecimal> chf_usd = mapimap.get(Currency.CHF).get(Currency.USD);
			List<BigDecimal> eur_chf = mapimap.get(Currency.EUR).get(Currency.CHF);
			List<BigDecimal> chf_eur = mapimap.get(Currency.CHF).get(Currency.EUR);
			List<BigDecimal> gbp_chf = mapimap.get(Currency.GBP).get(Currency.CHF);
			List<BigDecimal> chf_gbp = mapimap.get(Currency.CHF).get(Currency.GBP);
			List<BigDecimal> jpy_chf = mapimap.get(Currency.JPY).get(Currency.CHF);
			List<BigDecimal> chf_jpy = mapimap.get(Currency.CHF).get(Currency.JPY);
			BigDecimal std_usd_chf = movingStd(usd_chf); 
			BigDecimal std_chf_usd = movingStd(chf_usd);
			BigDecimal std_eur_chf = movingStd(eur_chf); 
			BigDecimal std_chf_eur = movingStd(chf_eur); 
			BigDecimal std_gbp_chf = movingStd(gbp_chf);
			BigDecimal std_chf_gbp = movingStd(chf_gbp); 
			BigDecimal std_jpy_chf = movingStd(jpy_chf);
			BigDecimal std_chf_jpy = movingStd(chf_jpy);
			try {
				Process p = Runtime.getRuntime().exec("python3 "
						+ "random_forest_evaluation.py "+ std_eur_chf +","+ std_chf_eur +","+ std_usd_chf
						+","+ std_chf_usd +","+ std_jpy_chf +","+ std_chf_jpy +","+ std_gbp_chf +","+ std_chf_gbp);
				BufferedReader stdInput = new BufferedReader(new 
		                 InputStreamReader(p.getInputStream()));
				BufferedReader stdError = new BufferedReader(new 
		                 InputStreamReader(p.getErrorStream()));
				String prediction = stdInput.readLine();
				String s = null;
				while((s = stdError.readLine()) != null) {
          if(s == "0"){
            profile = MarketProfile.SOMETHING;
          }else if(s == "1"){
            profile = MarketProfile.POC;
          }else if(s == "2"){
            profile = MarketProfile.IT_WORKS;
          }else if(s == "3"){
            profile = MarketProfile.STARTUP;
          }else{
            profile = MarketProfile.UNICORN;
          }
        }
				System.out.println(prediction);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
      }

		
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
