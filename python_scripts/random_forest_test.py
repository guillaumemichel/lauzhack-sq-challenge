import pickle
from sklearn.ensemble import RandomForestRegressor
import matplotlib.pyplot as plt
import numpy as np
import random

split = 0.8

with open("/home/goodwine/Documents/Laushack2019/dataset.pickle", 'rb') as file:
    dataset = pickle.load(file)

random.shuffle(dataset)

print("Dataset size: {}".format(len(dataset)))
split_index = round(len(dataset) * split)

dataset_train = dataset[:split_index]
dataset_test = dataset[split_index:]

features_train = []
labels_train = []
for data in dataset_train:
    features_train.append(data[0])
    labels_train.append(data[1])

features_test = []
labels_test = []
for data in dataset_test:
    features_test.append(data[0])
    labels_test.append(data[1])

print("Train set size: {}\n"
      "Test set size: {}".format(len(dataset_train), len(dataset_test)))

accuracies = []
for n in range(1, 10):
    print("Training forest...")
    rf = RandomForestRegressor(n_estimators = n, random_state = 42)
    rf.fit(features_train, labels_train)

    print("Done!\n"
          "Evaluating...")
    predictions = rf.predict(features_test)
    accuracy = 0
    for i in range(len(predictions)):
        if predictions[i] == labels_test[i]:
            accuracy += 1
    accuracy = accuracy * 100 / len(predictions)

    print("Accuracy for {} estimators: {}".format(n, accuracy))
    accuracies.append(accuracy)

plt.plot(np.arange(len(accuracies)), accuracies)
plt.show()


