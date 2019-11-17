import sys
import pickle

if __name__ == "__main__":
    with open("tree.pickle", 'rb') as file:
        rf = pickle.load(file)

    args = sys.argv
    if len(args) < 2:
        raise AttributeError("No data to evaluate found")

    data = args[1].split(",")
    prediction = rf.predict([data])
    # Output to standard output to be read by Java program
    print(prediction)
