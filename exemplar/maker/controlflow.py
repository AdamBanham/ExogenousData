

from typing import Dict, List, Tuple, Union
from pm4py.objects.log.obj import EventLog,Trace,Event

import pandas as pd
import numpy as np
from tqdm import tqdm

from utils import find_best_fitting_line_slope

RESOURCE_ONE = "JOE"
RESOURCE_TWO = "SARAH"
RESOURCE_THREE = "RILEY"
RESOURCE_FOUR = "ASHLEY"
RESOURCE_FIVE = "RICK"

RESOURCES_A = [RESOURCE_ONE, RESOURCE_THREE, RESOURCE_FIVE]
RESOURCES_C = [RESOURCE_ONE,RESOURCE_TWO]
RESOURCES_DP_1 = [ RESOURCE_ONE, RESOURCE_TWO, RESOURCE_THREE, RESOURCE_FOUR, RESOURCE_FIVE]
RESOURCES_H = [RESOURCE_FIVE, RESOURCE_ONE]
RESOURCES_I = [RESOURCE_THREE,RESOURCE_TWO, RESOURCE_FOUR]
RESOURCES_J = [RESOURCE_FOUR,RESOURCE_ONE]
RESOURCES_DP_3 = [ RESOURCE_ONE, RESOURCE_TWO, RESOURCE_THREE, RESOURCE_FOUR, RESOURCE_FIVE]
RESOURCES_M = [RESOURCE_ONE, RESOURCE_THREE, RESOURCE_FIVE]

XES_CONCEPT = "concept:name"
XES_TIME = "time:timestamp"
XES_LIFE = "lifecycle:transition"
XES_RES = "org:resource"

EXO_VALUE = "exogenous:value"

EXTRA_ATTR_ITEMS = "items"
EXTRA_ATTR_PROFIT = "profit"
EXTRA_ATTR_COST = "cost"

SUOD_FIN_KEY = "FINISHED"

GENERATE_LOG_NAME = "ExogenousData - Exemplar - Endogenous Factory Process"

# costers

def profit_per_time(time:pd.Timestamp) -> float:
    """
    Works out how much profit comes from selling one product, based on the relative number of days since the start date.
    """
    start_date = pd.Timestamp(2122,1,1)
    relative_days = time - start_date
    relative_days = relative_days.days 

    profit = np.sin(0.02 * relative_days) * 100
    profit += 750
    profit -= (0.015 * relative_days) ** 3
    return profit

def new_trace_arrivals(time:pd.Timestamp) -> int:
    """
    Works out the number of new traces to create, based on the relative number of days since the start date.
    """
    start_date = pd.Timestamp(2122,1,1)
    relative_days = time - start_date
    relative_days = relative_days.days 

    traces = np.sin(0.03 * relative_days) * 6
    traces += 17
    traces -= (0.008 * relative_days) ** 2
    return int(np.floor(traces)) 


# log functions

def generate_log(panels:Dict[str,EventLog]) -> EventLog:
    # build log 
    log = EventLog(**{
        "attributes" : {
            XES_CONCEPT : GENERATE_LOG_NAME
        }
    })

    start = pd.Timestamp(2121,10,1)
    end = pd.Timestamp(2123,5,1)
    log_dur = (end - start).days
    log_percent = log_dur / 100.0
    log_progress = 0.0
    traces = list()
    trace_id = 1

    with tqdm(total=100,desc="endogenous",ascii=True) as pbar:
        while start <= end :
            arrivals = new_trace_arrivals(start)

            if (arrivals < 0):
                break 

            for arrival in range(arrivals):
                traces.append(make_trace(trace_id,panels,start))
                trace_id += 1

            nstart = push_log_step(start)
            diff = ((nstart - start).days) / log_percent
            log_progress += diff
            pbar.update(diff)
            start = nstart
        pbar.update(100.0 - log_progress)
    
    log._list = traces

    return log 

def push_log_step(time:pd.Timestamp) -> pd.Timestamp:
    push = (np.random.rand() * np.random.randint(0,7)) + 5
    push = pd.Timedelta(f"{push} days")
    return time + push


# trace functions

def build_trace_attrs(id:int) -> Trace :
    trace_ins = Trace()

    trace_ins.attributes[XES_CONCEPT] = f"trace-{id:06d}"
    exo_links = np.random.choice([1,2])
    for name,_ in zip(["copper", "iron"], range(exo_links)):
        trace_ins.attributes[f"trace:exogenous_{name}"] = True
    trace_ins.attributes[f"trace:exogenous_capacity"] = True

    return trace_ins

def make_trace(id:int, panels:Dict[str,EventLog], start:pd.Timestamp) -> Trace:
    """
    Makes a control-flow trace that follows the outlined Petri net
    """
    # setup trace and datastate
    trace_ins = build_trace_attrs(id)
    datastate = {
        EXTRA_ATTR_COST : 0.0,
        EXTRA_ATTR_ITEMS : 0.0,
        EXTRA_ATTR_PROFIT : 0.0
    }

    for key,time in panels.items():
        datastate[key] = time

    # build control-flow
    # always A
    trace_ins.append(make_A(start, datastate))
    start = push_time(start)
    # followed by B
    trace_ins.append(make_B(start, datastate))
    start = push_time(start)
    # followed by C
    trace_ins.append(make_C(start, datastate))
    start = push_time(start)
    # resolve decision point one
    trace_ins.append(resolve_decision_point_one(start,datastate, panels, trace_ins))
    start = push_time(start)
    # followed by G 
    trace_ins.append(make_G(start, datastate))
    start = push_time(start)
    # begin rework loop
    # trigger first check
    trace_ins.append(resolve_decision_point_two(start, datastate, panels, trace_ins))
    start = push_time(start)
    while SUOD_FIN_KEY not in datastate.keys():
        ### rework section of work
        ## start from I 
        # followed by J 
        trace_ins.append(make_J(start,datastate))
        start = push_time(start)
        # resolve dp 3 
        trace_ins.append(resolve_decision_point_three(start, datastate, panels, trace_ins))
        start = push_time(start)
        # followed by M
        trace_ins.append(make_M(start, datastate))
        start = push_time(start)
        ### normal flow of work
        # followed by B
        trace_ins.append(make_B(start, datastate))
        start = push_time(start)
        # followed by C
        trace_ins.append(make_C(start, datastate))
        start = push_time(start)
        # resolve decision point one
        trace_ins.append(resolve_decision_point_one(start,datastate, panels, trace_ins))
        start = push_time(start)
        # followed by G 
        trace_ins.append(make_G(start, datastate))
        start = push_time(start)
        # trigger rework check
        trace_ins.append(resolve_decision_point_two(start, datastate, panels, trace_ins))
        start = push_time(start)


    return trace_ins
    
def push_time(time:pd.Timestamp) -> pd.Timestamp:
    hours = (np.random.rand() * 48) + 48
    return time + pd.Timedelta(f"{hours} hours")

#  event functions 

def make_event(time:pd.Timestamp, name:str) -> Event :
    event_ins = Event()
    event_ins[XES_CONCEPT] = name
    event_ins[XES_TIME] = time 
    event_ins[XES_LIFE] = "completed"

    return event_ins

def make_A(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info
    event_ins = make_event(time, "A")

    # Update datastate
    datastate[EXTRA_ATTR_ITEMS] = np.random.randint(1,10) * 1.0
    datastate[EXTRA_ATTR_PROFIT] = profit_per_time(time) * datastate[EXTRA_ATTR_ITEMS]

    # build extra info
    event_ins[EXTRA_ATTR_ITEMS] = float(f"{datastate[EXTRA_ATTR_ITEMS]:.2f}")
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_A)

    return event_ins

def make_B(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "B")

    # update datastate 
    datastate[EXTRA_ATTR_COST] += datastate[EXTRA_ATTR_ITEMS] * 5.25 

    # build extra info
    event_ins[EXTRA_ATTR_COST] = float(f"{datastate[EXTRA_ATTR_COST]:.2f}")

    return event_ins

def make_C(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "C")

    # update datastate 
    new_items = np.random.randint(0,10)
    datastate[EXTRA_ATTR_ITEMS] += new_items * 1.0
    datastate[EXTRA_ATTR_PROFIT] += profit_per_time(time) * datastate[EXTRA_ATTR_ITEMS]

    # build extra info
    event_ins[EXTRA_ATTR_ITEMS] = float(f"{datastate[EXTRA_ATTR_ITEMS]:.2f}")
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_C)

    return event_ins

def resolve_decision_point_one(time:pd.Timestamp, datastate:Dict[str,float], panels:Dict[str,EventLog], trace:Trace) -> Event:
    """
    Works out based on the exogenous state if F needs to occur, F increases cost dramatically but only if capacity will drop in the future.\n
    D only occurs if cost is less 500.\n
    E only occurs if the histroical material prices are increasing.\n
    Logic order, check F, check D, check E, else coin-flip between D or E.\n
    """

    capacity_series = extract_exo_series(panels["capacity"], time + pd.Timedelta("5 days"), 300)
    material_series = list()
    if ("trace:exogenous_iron" in trace.attributes.keys()):
        material_series.append(extract_exo_series(panels["iron"], time, 150))
    if ("trace:exogenous_copper" in trace.attributes.keys()):
        material_series.append(extract_exo_series(panels["copper"], time, 150))

    event_ins = None
    if (len(capacity_series) > 0 and capacity_series[0] < capacity_series[-1]):
        event_ins = make_F(time, datastate)
    elif (datastate[EXTRA_ATTR_COST] < 500):
        event_ins = make_D(time, datastate)
    elif (check_for_increasing_trend(material_series)):
        event_ins = make_E(time, datastate)
    else:
        if (np.random.rand() >= 0.5):
            event_ins = make_D(time, datastate)
        else:
            event_ins = make_E(time, datastate)

    return event_ins

def make_F(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "F")

    # update datastate 
    datastate[EXTRA_ATTR_COST] += np.random.randint(50,100) * datastate[EXTRA_ATTR_ITEMS]

    # build extra info
    event_ins[EXTRA_ATTR_COST] = float(f"{datastate[EXTRA_ATTR_COST]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_DP_1)

    return event_ins 

def make_D(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "D")

    # update datastate 
    datastate[EXTRA_ATTR_COST] += np.random.randint(3,15) * datastate[EXTRA_ATTR_ITEMS]

    # build extra info
    event_ins[EXTRA_ATTR_COST] = float(f"{datastate[EXTRA_ATTR_COST]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_DP_1)

    return event_ins

def make_E(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "E")

    # update datastate 
    new_items = np.random.randint(0,3)
    datastate[EXTRA_ATTR_ITEMS] += new_items * 1.0
    datastate[EXTRA_ATTR_PROFIT] += profit_per_time(time) * datastate[EXTRA_ATTR_ITEMS]

    # build extra info
    event_ins[EXTRA_ATTR_ITEMS] = float(f"{datastate[EXTRA_ATTR_ITEMS]:.2f}")
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_DP_1)

    return event_ins

def make_G(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "G")

    # update datastate 

    # build extra info

    return event_ins

def resolve_decision_point_two(time:pd.Timestamp, datastate:Dict[str,float], panels:Dict[str,EventLog], trace:Trace) -> Union[Event, None]:
    """
    Resolves if a trace should end the rework loop or continue.\n
    If H is returned then the trace ends, pesudo atttribtue is added to datastate to break event-loop.\n
    Naturally, 60% of traces will rework but conditions must be meet for the rework to occur.\n
    Cost must be below 5000 and capacity must be greater than 0.
    """

    event_ins = None

    if (np.random.rand() <= 0.6):
        if (datastate[EXTRA_ATTR_COST] <= 5000):
            capacity = extract_exo_series(panels["capacity"], time, 50)
            if (len(capacity) < 1):
                return make_I(time, datastate)
            elif (capacity[-1][1] > 0):
                return make_I(time, datastate) 
    
    event_ins = make_H(time, datastate)
    
    return event_ins

def make_H(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "H")

    # update datastate 
    datastate[SUOD_FIN_KEY] = True

    # build extra info
    event_ins[XES_RES] = np.random.choice(RESOURCES_H)

    return event_ins

def make_I(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "I")

    # update datastate 
    datastate[EXTRA_ATTR_COST] += 15

    # build extra info
    event_ins[XES_RES] = np.random.choice(RESOURCES_I)

    return event_ins

def make_J(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "J")

    # update datastate 
    datastate[EXTRA_ATTR_PROFIT] -= datastate[EXTRA_ATTR_PROFIT] * 0.03

    # build extra info
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_J)

    return event_ins

def resolve_decision_point_three(time:pd.Timestamp, datastate:Dict[str,float], panels:Dict[str,EventLog], trace:Trace) -> Event:
    """
    Resolves whether to do either K or L.\n
    K usually occurs if any material has dropped in price\n
    L usually occurs if any material has a price increasing trend greater than 5.0\n
    otherwise we coin-flip between options.
    """

    material_series = list()
    if ("trace:exogenous_iron" in trace.attributes.keys()):
        material_series.append(extract_exo_series(panels["iron"], time, 150))
    if ("trace:exogenous_copper" in trace.attributes.keys()):
        material_series.append(extract_exo_series(panels["copper"], time, 150))

    any_drop = False
    for series in material_series:
        if len(series) > 2:
            check = series[0] > series[-1]
            any_drop = check or any_drop

    increase_trend = False
    for series in material_series:
        if len(series) > 50:
            check = find_best_fitting_line_slope(series) > 5.0
            increase_trend = check or increase_trend

    if (any_drop):
        return make_K(time, datastate)
    elif (increase_trend):
        return make_L(time, datastate)
    else:
        if (np.random.rand() <= 0.5):
            return make_K(time, datastate)
        else:
            return make_L(time, datastate)



def make_K(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "K")

    # update datastate
    new_items = np.random.randint(3,15)
    datastate[EXTRA_ATTR_ITEMS] += new_items * 1.0
    datastate[EXTRA_ATTR_PROFIT] += profit_per_time(time) * datastate[EXTRA_ATTR_ITEMS] 

    # build extra info
    event_ins[EXTRA_ATTR_ITEMS] = float(f"{datastate[EXTRA_ATTR_ITEMS]:.2f}")
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_DP_3)

    return event_ins 

def make_L(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "L")

    # update datastate 
    less_items = np.random.randint(1,3)
    total_items = datastate[EXTRA_ATTR_ITEMS]

    if (total_items > less_items):
        datastate[EXTRA_ATTR_ITEMS] = np.min([0.0, datastate[EXTRA_ATTR_ITEMS] - less_items])
        datastate[EXTRA_ATTR_COST] += 50
        datastate[EXTRA_ATTR_PROFIT] += profit_per_time(time) * datastate[EXTRA_ATTR_ITEMS] 
    else:
        datastate[EXTRA_ATTR_COST] += 15

    # build extra info
    event_ins[EXTRA_ATTR_ITEMS] = float(f"{datastate[EXTRA_ATTR_ITEMS]:.2f}")
    event_ins[EXTRA_ATTR_COST] = float(f"{datastate[EXTRA_ATTR_COST]:.2f}")
    event_ins[EXTRA_ATTR_PROFIT] = float(f"{datastate[EXTRA_ATTR_PROFIT]:.2f}")
    event_ins[XES_RES] = np.random.choice(RESOURCES_DP_3)

    return event_ins 

def make_M(time:pd.Timestamp, datastate:Dict[str,float]) -> Event:
    # build basic event info 
    event_ins = make_event(time, "M")

    # update datastate 
    datastate[EXTRA_ATTR_COST] += 50

    # build extra info
    event_ins[XES_RES] = np.random.choice(RESOURCES_I)

    return event_ins

def check_for_increasing_trend(series:List) -> bool:
    increasing = False 
    for seri in series:
        if (len(seri) > 2):
            check = find_best_fitting_line_slope(seri) > 0
            increasing = increasing or check 
    return increasing


def extract_exo_series(panel:EventLog, time:pd.Timestamp, length:int) -> List[Tuple[pd.Timestamp,float]]:
    series = list()

    for event in panel[0]:
        if (event[XES_TIME] < time):
            if (len(series) > length):
                series.pop(0)    
            series.append( (event[XES_TIME], event[EXO_VALUE]))
        else:
            break



    return series
