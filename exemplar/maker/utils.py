
from typing import Tuple,List

import pandas as pd
import numpy as np

def find_best_fitting_line_slope(series:List[Tuple[pd.Timestamp,float]]) -> float :

    start_time = series[0][0]

    series = [ ((x - start_time).days, y) for x,y in series]


    mean_x = sum([ x for x,y in series]) / len(series)
    mean_y = sum([ y for x,y in series]) / len(series)

    top_div = sum([ (x - mean_x) * (y - mean_y) for x,y in series])
    bot_div = sum([ (x - mean_x) ** 2 for x,y in series])

    return top_div / bot_div
    