import json
import matplotlib.pyplot as plt
import numpy as np


limit = 1000
for seed in range(1, 11):
    std_bucket_jpy = []
    std_bucket_eur = []
    std_bucket_usd = []
    std_bucket_chf = []
    std_bucket_gbp = []
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
        trades = obj["trades"][:limit]
        bbook_trades = obj["bBookTrades"][:limit]
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
        std_bucket_jpy.append(np.std(bucket_norm_jpy))
        std_bucket_eur.append(np.std(bucket_norm_eur))
        std_bucket_usd.append(np.std(bucket_norm_usd))
        std_bucket_chf.append(np.std(bucket_norm_chf))
        std_bucket_gbp.append(np.std(bucket_norm_gbp))

    std_moy = [(a + b + c + d + e) / 5 for (a, b, c, d, e) in zip(std_bucket_chf, std_bucket_eur, std_bucket_gbp,
                                                              std_bucket_jpy, std_bucket_usd)]
    plt.semilogy(np.arange(len(std_moy)), std_moy)
plt.show()

# SOMETHING & POC : small trades ( < 10**8 )
# UNICORN : horn shape between EUR and USD
# IT_WORKS & START_UP : high trades ( > 10**8 )

