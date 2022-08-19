# Control Questions

1. What is the number of states in the United States?
2. How many countries do you estimate there are in the world?
3. What is the gas price normally like in your area?
4. In what year was the first digital computer, ENIAC, built?
5. What year is it now?
6. In what year do you think the US will hold its next presidential election?

## Final control questions with the survey

1. What do you think the median home price in the US is in 2021? (10^5)
2. What do you think is the average household income in the US for 2021? (10^4)
3. What is the gas price in your area? (10^0)
4. What is the acceptance rate when applying to Harvard University as an undergraduate? (10^0)
5. What is the number of states in the United States? (10^1)
6. In the US, about how many people do you think use the internet in 2021, in millions? (10^2)
7. What do you think is the average annual tuition of public colleges in the US in 2021? (10^3)
8. In what year will the US will hold its next presidential election? (10^3)

## Correct anaswers & acceptable bounds

1. 453,700 => (0, 784750)  # (25% - 1.5IQR truncated to 0, 75% + 1.5IQR)
2. 67,463 => (0, 120000)  # (25% - 1.5IQR truncated to 0, 75% + 1.5IQR)
3. 3.918 => (3, 8)  # based on actual information
   1. highest: 6.514 in Mono, California (AAA Aug 19 2022)
   2. lowest: 3.127 in Hidalgo, Texas (AAA Aug 19 2022)
4. 4.01% ==> (0,10)
   1. 4.01% = 2318/57786
   2. Harvard Common Data Set 2021-2022 Page 7 Section C
   3. https://oir.harvard.edu/files/huoir/files/harvard_cds_2021-2022.pdf
   4. https://web.archive.org/web/20220819150533/https://oir.harvard.edu/files/huoir/files/harvard_cds_2021-2022.pdf
5. 50 ==> (50,50)
6. 307.2M ==> (0,400)
   1. https://www.statista.com/topics/2237/internet-usage-in-the-united-states/
   2. 333M => US population https://www.census.gov/popclock/
7. 10,740 ==> (5000, 40000)
   1. https://research.collegeboard.org/media/pdf/trends-college-pricing-student-aid-2021.pdf
   2. (From https://research.collegeboard.org/trends/college-pricing)
   3. CollegeBoard, Trends in College Pricing 2021. P10
   4. Public 4year in-state: 10,740
   5. Public 4year out-state: 27,560
   6. Private 4year: 38,070
   7. Carnegie Classification prices are ~1k lower
8. 2024 => 2020 || 2024

