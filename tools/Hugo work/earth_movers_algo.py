import csv
import math
import sys
import statistics
import random
from operator import itemgetter
import matplotlib.pyplot as plt



def readCSV(fileName, index):

    # for the pew-2022-04-13 data, max age is 72 and min is 18

    data = []

    with open(fileName) as file:
        reader = csv.reader(file)
        for row in reader:
            r = row[int(index)].strip('][').split(', ')

            newRow = [int(float(x)) for x in r]

            # for the pew-2022-04-13 data, reduce all the estimates by 18, since that's the lowest number
            # newRow[len(newRow) - 1] -= 18

            data.append(newRow)

    return data



# n is number of samples to create
# radixes describes what a sample should look like: possibilities for each sample
def createSamples(n, radixes):

    samples = []
    length = len(radixes)

    for x in range(n):

        sample = []

        for y in range(length):

            # for each item, randomly choose using probabilities
            radix = radixes[y]

            newNumber = random.randint(0, radix-1)
            sample.append(newNumber)

        samples.append(sample)


    return samples



def earth_movers(samples1, samples2, radixes, question_types):

    if (len(samples1) != len(samples2)):
        print("Error! Lists should be the same length!")
        return

    if (len(samples1[0]) != len(samples2[0])):
        print("Error! Each item in each list should be the same length!")
        return

    numberOfSamples = len(samples1)
    complexity = len(radixes)

    # data is stored in this format:
    # [x1y1, x1y2, x1y3 ... x2y1, x2y2...] where x1y2 is the distance between element 1 of list 1 and element 1 of list 2

    data = []

    # exhaustively compute the distance between every pair
    for x in range(numberOfSamples):

        for y in range(numberOfSamples):

            s1 = samples1[x]
            s2 = samples2[y]

            total = 0


            # Calculate euclidean distance
            for z in range(complexity):

                # check the question type
                if question_types[z] == "radio" or question_types[z] == "checkbox":

                    # for radio and checkbox questions, different is 1 and same is 0
                    if s1[z] != s2[z]:
                        total += 1

                else:
                    # Right now the only other case is estimate. For these, normalize between 0 and 1
                    radix = radixes[z]
                    percent1 = s1[z] / (radix - 1)
                    percent2 = s2[z] / (radix - 1)
                    diff = percent1 - percent2
                    total += (diff * diff)

            # Append the distance, plus the x and y
            totalRoot = math.sqrt(total)
            data.append([x,y,totalRoot])

    distance = 0

    # sort
    sortedData = sorted(data, key=itemgetter(2))

    # rows and columns already used
    rowsUsed = []
    columnsUsed = []

    number_chosen = 0
    distance = 0.0
    index = 0

    # go through and find the smallest value until every item has been "moved"
    while (number_chosen < numberOfSamples):

        current = sortedData[index]
        row = current[0]
        column = current[1]
        dist = current[2]

        if row not in rowsUsed and column not in columnsUsed:

            distance += dist
            number_chosen += 1
            rowsUsed.append(row)
            columnsUsed.append(column)

        index += 1

    return distance




def random_v_random(n, radixes, iterations, question_types):

    # hold all the distances
    distances = []

    for x in range(iterations):

        samples1 = createSamples(n, radixes)
        samples2 = createSamples(n, radixes)

        # calculate earth movers distance
        dist = earth_movers(samples1, samples2, radixes, question_types)
        distances.append(dist)

    return distances


def test_v_random(n, radixes, iterations, question_types, test_samples):

    # hold all the distances
    distances = []

    for x in range(iterations):

        samples1 = createSamples(n, radixes)

        # calculate earth movers distance
        dist = earth_movers(samples1, test_samples, radixes, question_types)
        distances.append(dist)

    return distances



def algo(question_types, test_samples, radixes, iterations, sample_size):

    randomDistances = random_v_random(sample_size, radixes, iterations, question_types)
    testDistances = test_v_random(sample_size, radixes, iterations, question_types, test_samples)

    randomSum = sum(randomDistances)
    testSum = sum(testDistances)

    randomMean = randomSum / len(randomDistances)
    testMean = testSum / len(testDistances)

    std = statistics.pstdev(randomDistances)

    threshold = 3

    stdAway = (testMean - randomMean) / std

    return stdAway



# Starting at min_sample_size to max_sample_size skipping step, randomly sample from the test_samples of that sample size, and run the algorithm iterations_graph times. Plot by sample size what percent have a standard deviation of 3 or more
def plotProbabilityCorrect(min_sample_size, max_sample_size, step, question_types, test_samples, radixes, iterations_graph, iterations_algo):

    # Data for the graph
    x_values = list(range(min_sample_size, max_sample_size + step, step))
    y_values = []

    for sample_size in x_values:

        print("Sample size of {}".format(sample_size))

        numberSignificant = 0

        for i in range(iterations_graph):

            # Randomly sample from the data WITHOUT replacement
            samples = random.sample(test_samples, sample_size)

            # Run the earth_movers algorithm and get the stdAway back
            stdAway = algo(question_types, samples, radixes, iterations_algo, sample_size)

            print(stdAway)

            if stdAway > 3:
                numberSignificant += 1

        percentSignificant = numberSignificant / iterations_graph * 100

        y_values.append(percentSignificant)

    print(y_values)

    plt.plot(x_values,y_values)
    plt.title('Algorithm significance vs sample size')
    plt.xlabel('Sample size')
    plt.ylabel('Percent of significant results')
    plt.savefig("significance_random.png")



if __name__ == "__main__":

    # Command line test: python earth_movers_algo.py short_survey.csv 12


    # read from csv first
    data = readCSV(sys.argv[1], sys.argv[2])

    # # full
    d = algo(["radio","radio","radio","radio","radio","radio","radio","radio","estimate"], data, [3,3,3,3,3,3,3,2,55], 100, 10)


    print("std")
    print(d)


    # Probability graph
    # plotProbabilityCorrect(10, 65, 5, ["radio","radio","radio","radio","radio","radio","radio","radio","estimate"], data, [3,3,3,3,3,3,3,2,55], 100, 100)










#
