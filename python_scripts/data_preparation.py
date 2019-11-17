import json
import numpy as np
import pickle


map_profile = {
    "SOMETHING": 0,
    "POC": 1,
    "IT_WORKS": 2,
    "STARTUP": 3,
    "UNICORN": 4
}

dataset = []
for seed in range(1, 501):
    for profile in ["SOMETHING", "POC", "IT_WORKS", "STARTUP", "UNICORN"]:
        PATH = "/home/goodwine/Documents/Laushack2019/jsons_reduced/{}/SQEvolution_SteakHashés_{}_{}.json".format(profile,
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

        print("Status : {}\n"
              "Seed : {}\n"
              "Profile : {}\n"
              "Perf : {}".format(status, seed, profile, perf))

        currencies = ["CHF", "USD", "GBP", "EUR", "JPY"]
        map_trades = {}
        for c1 in currencies:
            map_trades[c1] = {}
            for c2 in currencies:
                if c2 != c1:
                    map_trades[c1][c2] = []

        for block in trades:
            for t in block:
                map_trades[t["base"]][t["term"]].append(t["quantity"])

        feature_vector = [
            np.std(map_trades["EUR"]["CHF"]),
            np.std(map_trades["CHF"]["EUR"]),
            np.std(map_trades["USD"]["CHF"]),
            np.std(map_trades["CHF"]["USD"]),
            np.std(map_trades["JPY"]["CHF"]),
            np.std(map_trades["CHF"]["JPY"]),
            np.std(map_trades["GBP"]["CHF"]),
            np.std(map_trades["CHF"]["GBP"])
        ]
        dataset.append((feature_vector, map_profile[profile]))

with open("/home/goodwine/Documents/Laushack2019/dataset.pickle", 'wb') as file:
    pickle.dump(dataset, file)

