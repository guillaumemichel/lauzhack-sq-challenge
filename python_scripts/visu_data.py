import json
from scipy.stats import moment
import matplotlib.pyplot as plt
import numpy as np
import math

for seed in range(1, 21):
    moments = []
    for profile in ["SOMETHING", "POC", "IT_WORKS", "STARTUP", "UNICORN"]:
        PATH = "/home/goodwine/Documents/Laushack2019/jsons/{}/SQEvolution_SteackHashés_{}_{}.json".format(profile,
                                                                                                           seed,
                                                                                                           profile)

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
        chf_gain = obj["chfGain"]
        bucket_jpy = obj["buckets"]["JPY"]
        bucket_eur = obj["buckets"]["EUR"]
        bucket_usd = obj["buckets"]["USD"]
        bucket_chf = obj["buckets"]["CHF"]
        bucket_gbp = obj["buckets"]["GBP"]
        price_jpy = obj["prices"]["JPY"]
        price_eur = obj["prices"]["EUR"]
        price_usd = obj["prices"]["USD"]
        price_chf = obj["prices"]["CHF"]
        price_gbp = obj["prices"]["GBP"]
        bucket_norm_jpy = [a * r for (a, r) in zip(bucket_jpy, price_jpy)]
        bucket_norm_eur = [a * r for (a, r) in zip(bucket_eur, price_eur)]
        bucket_norm_usd = [a * r for (a, r) in zip(bucket_usd, price_usd)]
        bucket_norm_chf = [a * r for (a, r) in zip(bucket_chf, price_chf)]
        bucket_norm_gbp = [a * r for (a, r) in zip(bucket_gbp, price_gbp)]

        third_moment_jpy = moment(bucket_norm_jpy, moment=3)
        third_moment_eur = moment(bucket_norm_eur, moment=3)
        third_moment_usd = moment(bucket_norm_usd, moment=3)
        third_moment_chf = moment(bucket_norm_chf, moment=3)
        third_moment_gbp = moment(bucket_norm_gbp, moment=3)
        moments.append((third_moment_chf + third_moment_eur + third_moment_gbp + third_moment_jpy + third_moment_usd) / 5)
    plt.semilogy(np.arange(len(moments)), moments)
print(moments)
plt.show()

