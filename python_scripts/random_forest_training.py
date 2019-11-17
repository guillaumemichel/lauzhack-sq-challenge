import pickle
from sklearn.ensemble import RandomForestRegressor
import random
import pickle

with open("/home/goodwine/Documents/Laushack2019/dataset.pickle", 'rb') as file:
    dataset = pickle.load(file)

random.shuffle(dataset)

print("Dataset size: {}".format(len(dataset)))

dataset_train = dataset

features_train = []
labels_train = []
for data in dataset_train:
    features_train.append(data[0])
    labels_train.append(data[1])

print("Training forest...")
rf = RandomForestRegressor(n_estimators = 1, random_state = 42)
rf.fit(features_train, labels_train)

with open("/home/goodwine/Documents/Laushack2019/tree.pickle", 'wb') as file:
    pickle.dump(rf, file)

importances = list(rf.feature_importances_)
feature_importances = [(feature, round(importance, 2)) for feature, importance
                       in zip(["1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                               "11", "12"], importances)]
feature_importances = sorted(feature_importances, key = lambda x: x[1], reverse = True)
[print('Variable: {:20} Importance: {}'.format(*pair)) for pair in feature_importances]
