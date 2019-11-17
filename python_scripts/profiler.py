import json
import matplotlib.pyplot as plt
import numpy as np


limit = 100
for seed in range(1, 11):
    mins_bucket_jpy = []
    maxs_bucket_jpy = []
    mins_bucket_eur = []
    maxs_bucket_eur = []
    mins_bucket_usd = []
    maxs_bucket_usd = []
    mins_bucket_chf = []
    maxs_bucket_chf = []
    mins_bucket_gbp = []
    maxs_bucket_gbp = []
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
        mins_bucket_jpy.append(abs(min(bucket_norm_jpy)))
        maxs_bucket_jpy.append(abs(max(bucket_norm_jpy)))
        mins_bucket_eur.append(abs(min(bucket_norm_eur)))
        maxs_bucket_eur.append(abs(max(bucket_norm_eur)))
        mins_bucket_usd.append(abs(min(bucket_norm_usd)))
        maxs_bucket_usd.append(abs(max(bucket_norm_usd)))
        mins_bucket_chf.append(abs(min(bucket_norm_chf)))
        maxs_bucket_chf.append(abs(max(bucket_norm_chf)))
        mins_bucket_gbp.append(abs(min(bucket_norm_gbp)))
        maxs_bucket_gbp.append(abs(max(bucket_norm_gbp)))

    gen_min = [max(a, b, c, d, e) for (a, b, c, d, e) in zip(mins_bucket_jpy, mins_bucket_eur, mins_bucket_usd,
                                                             mins_bucket_chf, mins_bucket_gbp)]
    gen_max = [max(a, b, c, d, e) for (a, b, c, d, e) in zip(maxs_bucket_jpy, maxs_bucket_eur, maxs_bucket_usd,
                                                             maxs_bucket_chf, maxs_bucket_gbp)]
    gen_moy = [max([a, b]) for (a, b) in zip(gen_min, gen_max)]
    plt.semilogy(np.arange(len(gen_moy)), gen_moy)
plt.show()

# SOMETHING & POC : small trades ( < 10**8 )
# UNICORN : horn shape between EUR and USD
# IT_WORKS & START_UP : high trades ( > 10**8 )

