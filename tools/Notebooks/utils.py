import numpy as np

import numbers
import random

import scipy.stats

from typing import List
import lap

# return distance only, not result
def earth_mover_distance_1(samples1: np.ndarray, samples2: np.ndarray, radixes: List[int], question_types: List[str]):
    ## Sanity check
    if (samples1.shape != samples2.shape):
        print("samples1 and samples2 shape not the same!")
        return

    # N: number of samples in both samples1 and samples2
    numberOfSamples = samples1.shape[0]
    # Q: number of questions in the survey
    numberOfQuestions = samples1.shape[1]

    if (numberOfQuestions != len(radixes)):
        print("radixes length not match!")
        return

    if (numberOfQuestions != len(question_types)):
        print("question_types length not match!")
        return

    ## Calculating distance matrix
    # distance matrix (shape N*N). Element distance[x][y] denotes distance between x-th element in samples1 and y-th element in samples2
    distance = np.zeros((numberOfSamples, numberOfSamples))
    # Memoizes and eliminates unnecessary computations. (z, valueInX) => np.ndarray of cost
    memoMap = {}

    for z in range(0, numberOfQuestions):
        # radio and checkbox question: distance = 1 if different else 0
        if question_types[z] == "radio" or question_types[z] == "checkbox":
            # TODO: probably the following loop can be parallelized?
            for x in range(0, samples1.shape[0]):
                value = samples1[x][z]  # value of samples1 in this col
                
                # Note that we can memoize the operation here: for the same value, the cost contribution of *this question* will always be the same when comparing across entire samples2
                if (z, value) in memoMap.keys():
                    distance[x] += memoMap[(z, value)]
                else:
                    # Note that this operation is vectorized: numpy has builtin function for comparing & setting values
                    memo = np.where(samples2[:, z] == value, 0, 1)
                    memoMap[(z, value)] = memo
                    distance[x] += memo

        # estimate question: distance = absolute difference / radix
        else:
            radix = radixes[z]
            for x in range(0, samples1.shape[0]):
                value = samples1[x][z]
                if (z, value) in memoMap.keys():
                    distance[x] += memoMap[(z, value)]
                else:
                    # Note that this operation is vectorized: numpy has builtin function for subtracting and division across entire row
                    memo = np.abs((samples2[:, z] - value)/radix)
                    memoMap[(z, value)] = memo
                    distance[x] += memo

    ## Find minimum distance matching (i.e. EMD) with Jonker–Volgenant algorithm
    return distance

# return EMD
def earth_mover_distance(samples1: np.ndarray, samples2: np.ndarray, radixes: List[int], question_types: List[str]):
    distance = earth_mover_distance_1(samples1, samples2, radixes, question_types)
    return lap.lapjv(distance)[0]

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

    return np.array(samples)


# outliers are punished as 1. It is the one we will use
def denoise(sample_ans, radixes, question_types):
    SAMPLE_SIZE = len(sample_ans)
    prob_random = np.zeros((SAMPLE_SIZE,))

    for index_q in range(0,len(radixes)):
        ans_q = sample_ans[:,index_q]
        bincount_q = np.bincount(ans_q)
        rad = radixes[index_q]

        if question_types[index_q] == "estimate":
            prob_q = np.select(
                    [ans_q==i for i in np.arange(0,rad+1)], # estimates have to +1 ==> bins are 1...20
                    # note that if there is no upper outlier, this may fail because of different sizes, need to find a way to pad following array
                    1 - (bincount_q[:rad+1]/SAMPLE_SIZE)*rad,
                    1 # handle higher outlier
                )
            prob_q[ans_q == 0] = 1 # handle lower outlier 
        else:
            prob_q = np.select(
                [ans_q==i for i in np.arange(0,rad)],
                1 - (bincount_q[:rad]/SAMPLE_SIZE)*rad,
                1 # handle higher outlier
            )

        # axs[row][col].hist(prob_q)
        prob_random += prob_q
    
    return prob_random


# outliers are punished as 2e
def denoise_2e(sample_ans, radixes, question_types):
    SAMPLE_SIZE = len(sample_ans)
    prob_random = np.zeros((SAMPLE_SIZE,))

    for index_q in range(0,len(radixes)):
        ans_q = sample_ans[:,index_q]
        bincount_q = np.bincount(ans_q)
        rad = radixes[index_q]

        if question_types[index_q] == "estimate":
            prob_q = np.select(
                    [ans_q==i for i in np.arange(0,rad+1)], # estimates have to +1 ==> bins are 1...20
                    # note that if there is no upper outlier, this may fail because of different sizes, need to find a way to pad following array
                    np.power(np.e, (1/rad - bincount_q[:rad+1]/SAMPLE_SIZE)*rad),
                    2 * np.e # handle higher outlier
                )
            prob_q[ans_q == 0] = 2 * np.e # handle lower outlier 
        else:
            prob_q = np.select(
                [ans_q==i for i in np.arange(0,rad)],
                np.power(np.e, (1/rad - bincount_q[:rad]/SAMPLE_SIZE)*rad),
                np.e # handle higher outlier
            )

        # axs[row][col].hist(prob_q)
        prob_random += prob_q
    
    return prob_random

def cohen_d(x,y):
    nx = len(x)
    ny = len(y)
    dof = nx + ny - 2
    return (np.mean(x) - np.mean(y)) / np.sqrt(((nx-1)*np.std(x, ddof=1) ** 2 + (ny-1)*np.std(y, ddof=1) ** 2) / dof)

# create random samples; repetitively (Monte Carlo like) calculate the EMD of random vs random and random vs test,
# then returns the Cohen's d between two distributions
# this function is specific to the pew survey
def empirical_effect_size(df_input, question_types, radix):  
    # sample_input = np.array(df_input.drop(['index', 'Worker ID', 'cost', 'updated', 'round'], axis=1).applymap(
    sample_input = np.array(df_input.drop(['id'], axis=1).applymap(
    # convert answers to integers (within radix)
        lambda x: {
            # dictionary for radio question
            'Easier': 0,
            'Same': 1,
            'Harder': 2,
            # dictionary for boolean radio
            'Yes': 1,
            'No': 0,
        }.get(x, int(x) if isinstance(x, numbers.Number) else x) # if an estimate, make it an int number
    ))

    ITERATION_NUMBER = 100
    SAMPLE_SIZE = len(df_input)

    distances_random = []
    distances_test = []

    for i in range(0, ITERATION_NUMBER):
        sample_random = createSamples(SAMPLE_SIZE,radix)
        sample_random2 = createSamples(SAMPLE_SIZE,radix)

        distance_random = earth_mover_distance(sample_random, sample_random2, radix, question_types)
        distance_test = earth_mover_distance(sample_random, sample_input, radix, question_types)

        distances_random.append(distance_random)
        distances_test.append(distance_test)

    d = cohen_d(distances_random, distances_test)
    return np.abs(d)