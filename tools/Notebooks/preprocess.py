import pandas as pd
import numpy as np

def preprocess_1k():
    df_temp = pd.read_csv("../../../PEW_1k.csv")
    
    ## rename columns
    df_ans = df_temp.rename(columns={
        'Worker ID': 'id',
        'Saving for the future': 'saving',
        'Paying for college': 'tuition',
        'Buying a home': 'home',
        'Finding a spouse or partner': 'spouse',
        'Finding a job': 'job',
        'Getting into college': 'college',
        'Staying in touch with family and friends': 'comm',
        'Do you currently live in the United States?': 'us',
        'What is your age?': 'age',
        'What do you think the median home price in the US is in 2021?': 'e_home',
        'What do you think is the average household income in the US for 2021?': 'e_income',
        'What is the gas price in your area?': 'e_gas',
        'What is the acceptance percentage when applying to Harvard University as an undergraduate?': 'e_harvard',
        'What is the number of states in the United States?': 'e_state',
        'In the US, about how many people do you think use the internet in 2021, in millions?': 'e_internet',
        'What do you think is the average annual tuition of public colleges in the US in 2021?': 'e_tuition',
        'In what year will the US will hold its next presidential election?': 'e_election'
    }, errors="raise").drop(columns=['cost','updated','round'])

    ## predicate filtering
    p_home = (df_ans["e_home"] > 0)  & (df_ans["e_home"] < 784750)
    p_income = (df_ans["e_income"] > 0) & (df_ans["e_income"] < 120000)
    p_gas = (df_ans["e_gas"] > 3) & (df_ans["e_gas"] < 8)
    p_harvard = (df_ans["e_harvard"] > 0)  & (df_ans["e_harvard"] < 10)
    p_state = (df_ans["e_state"] == 50)
    p_internet = (df_ans["e_internet"] > 0) & (df_ans["e_internet"] < 400)
    p_tuition = (df_ans["e_tuition"] > 5000)  & (df_ans["e_tuition"] < 40000)
    p_election = (df_ans["e_election"] == 2024) | (df_ans["e_election"] == 2020)
    
    p_total = np.array([p_home,p_income,p_gas,p_harvard,p_state,p_internet,p_tuition,p_election]).sum(axis=0)
    idx_truth = p_total >= 6 ## at most two wrong

    ## question types and radixes
    question_types = ["radio", "radio", "radio", "radio", "radio", "radio", "radio", "radio", "estimate", 
                  "estimate", "estimate", "estimate", "estimate", "estimate", "estimate", "estimate", "estimate"]
    radixes = [3,3,3,3,3,3,3,2] + [20]*9

    ## binned numerical sample_ans as input to denoise
    sample_ans = np.zeros((1000,17), dtype=int)
    for index, col in enumerate(df_ans.columns):
        if col == "id":
            continue  # drop ID column

        if question_types[index-1] == "radio":
            sample_ans[:,index-1] = df_ans[col].map({
                # dictionary for radio question
                'Easier': 0,
                'Same': 1,
                'Harder': 2,
                # dictionary for boolean radio
                'Yes': 1,
                'No': 0,
            }).astype(int)
        elif question_types[index-1] == "estimate":
            # they are put into 10 bins here

            q3, q1 = np.percentile(df_ans[col], [75, 25])
            IQR = q3 - q1  # Inter-Quartile Range

            upper_bound = q3 + 1.5 * IQR
            lower_bound = q1 - 1.5 * IQR

            # note that with numpy we do not need to handle the case of upper_bound == lower_bound

            arr_tmp = np.digitize(df_ans[col], np.linspace(lower_bound, upper_bound, 21))
            arr_tmp[df_ans[col] < lower_bound] = 0  # lower outliers
            arr_tmp[df_ans[col] == upper_bound] = 20 # upper bound should be 20 instead of 21
            arr_tmp[df_ans[col] > upper_bound] = 21 # upper outliers

            sample_ans[:,index-1] = arr_tmp.astype(int)
        else:
            print("Not handled:", index-1, col, question_types[index-1])

    return (df_ans, question_types, radixes, sample_ans, idx_truth)