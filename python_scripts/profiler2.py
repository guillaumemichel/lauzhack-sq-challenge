import json
import matplotlib.pyplot as plt
import numpy as np

limit = 1000
for seed in range(1, 11):
    stn_jpy = []
    stn_usd = []
    stn_eur = []
    stn_gbp = []
    stn_chf = []
    for profile in ["SOMETHING", "POC", "IT_WORKS", "STARTUP", "UNICORN"]:
        PATH = "/home/goodwine/Documents/Laushack2019/jsons/{}/SQEvolution_SteackHashés_{}_{}.json".format(profile,
                                                                                                           seed, profile)

        with open(PATH, 'r') as file:
            data = file.read()

        obj = json.loads(data)

        keys = obj.keys()
        status = obj["status"]
        team = obj["team"]
        seed = obj["seed"]
        profile = obj["profile"]
        perf = obj["perf"]
        trades = obj["trades"]
        bbook_trades = obj["bBookTrades"]
        chf_gain = obj["chfGain"][:limit]
        bucket_jpy = obj["buckets"]["JPY"][:limit]
        bucket_eur = obj["buckets"]["EUR"][:limit]
        bucket_usd = obj["buckets"]["USD"][:limit]
        bucket_chf = obj["buckets"]["CHF"][:limit]
        bucket_gbp = obj["buckets"]["GBP"][:limit]
        price_jpy = obj["prices"]["JPY"][:limit]
        price_eur = obj["prices"]["EUR"][:limit]
        price_usd = obj["prices"]["USD"][:limit]
        price_chf = obj["prices"]["CHF"][:limit]
        price_gbp = obj["prices"]["GBP"][:limit]
        bucket_norm_jpy = [a * r for (a, r) in zip(bucket_jpy, price_jpy)]
        bucket_norm_eur = [a * r for (a, r) in zip(bucket_eur, price_eur)]
        bucket_norm_usd = [a * r for (a, r) in zip(bucket_usd, price_usd)]
        bucket_norm_chf = [a * r for (a, r) in zip(bucket_chf, price_chf)]
        bucket_norm_gbp = [a * r for (a, r) in zip(bucket_gbp, price_gbp)]
        print("Status : {}\n"
              "Seed : {}\n"
              "Profile : {}\n"
              "Perf : {}".format(status, seed, profile, perf))

        jpy_chf = [a / b for (a, b) in zip(price_jpy, price_chf)]

        stn_jpy.append(np.mean(bucket_norm_jpy) / np.var(bucket_norm_jpy))
        stn_chf.append(np.mean(bucket_norm_chf) / np.var(bucket_norm_chf))
        stn_eur.append(np.mean(bucket_norm_eur) / np.var(bucket_norm_eur))
        stn_gbp.append(np.mean(bucket_norm_gbp) / np.var(bucket_norm_gbp))
        stn_usd.append(np.mean(bucket_norm_usd) / np.var(bucket_norm_usd))

    avg_stn = [(a + b + c + d + e) / 5 for (a, b, c, d, e) in zip(stn_jpy, stn_eur, stn_gbp, stn_chf, stn_usd)]
    plt.plot(np.arange(len(avg_stn)), avg_stn)
plt.show()
# SOMETHING : highest SNR ( > 50)
# SOMETHING & POC : small trades ( < 10**8 )
# UNICORN : horn shape between EUR and USD
# IT_WORKS & START_UP : high trades ( > 10**8 )

