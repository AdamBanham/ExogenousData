
from typing import Dict, List, Callable

import pm4py
from pm4py.objects.log.obj import EventLog,Trace,Event

import pandas as pd
import numpy as np

import vispm
from vispm.helpers.colours.colourmaps import HIGH_CONTRAST_COOL,HIGH_CONTRAST_WARM
from vispm.helpers.colours.colourmaps import EARTH,COOL_WINTER
from matplotlib.cm import get_cmap

import controlflow

from os.path import join

ENDO_LOG_OUT = join(".","..","endo_log.xes")
EXO_PANEL_01_OUT = join(".","..","exo_panel_01.xes")
EXO_PANEL_02_OUT = join(".","..","exo_panel_02.xes")
EXO_PANEL_03_OUT = join(".","..","exo_panel_03.xes")

DESCRIBE_PIC = join(".","..","dotted_description.png")

def capacity_left(time:float) -> float:
    """
    Works out the remainding capacity left of the factory, based on the relative number of days since the start date.
    """
    capacity = np.sin(0.15 * time) * 30
    capacity += 500
    capacity -= (0.07 * time) ** 2
    return capacity

def iron_price(time:float) -> float:
    """
    Works out the price of ircon, based on the relative number of days since the start date.
    """
    cost = np.cos(0.02 * time) * 70
    cost += 150
    cost += (0.05 * time) ** 2
    return cost

def copper_price(time:float) -> float:
    """
    Works out the price of copper, based on the relative number of days since the start date.
    """
    cost = np.sin(0.02 * time) * 70
    cost += 200
    cost += 0.5 * time 
    return cost

def create_exo_log(name:str, coster:Callable) -> EventLog:
    log = EventLog(
            **{
                "attributes" : {
                    "concept:name" : f"ExogenousData - Exemplar - exo-panel for {name}",
                    "exogenous:dataset" : {
                        "value" : "TRUE",
                        "children" : {
                            "exogenous:type" : "numerical",
                            "exogenous:link:method" : "match",
                            "exogenous:link:matching" : { "value" : "",
                                "children" : {
                                    "attribute_1" : f"trace:exogenous_{name}" 
                                }
                            }
                        }
                    }
                }
            }
    )

    # create single exo-series
    trace_ins = Trace()
    trace_ins.attributes["concept:name"] = f"xtrace_{1:d}"
    trace_ins.attributes["exogenous:name"] = name
    trace_ins.attributes[f"trace:exogenous_{name}"] = True
    # create many exo-measurements
    start_date = pd.Timestamp(2122,1,1)
    date = start_date
    points = 1
    while date < pd.Timestamp(2123,1,1):
        event_ins = Event()
        event_ins["concept:name"] = f"{points}_datapoint"
        event_ins["time:timestamp"] = date
        amount = coster((date - start_date).days)
        random_shift_sign = 1.0 if np.random.randint(0,1) == 1 else -1.0
        random_shift = random_shift_sign * (np.random.rand() * 0.15) * amount
        event_ins["exogenous:value"] = amount + random_shift
        trace_ins.append(event_ins)

        # progress the state
        hours_in_change = (np.random.rand() * 15) + 30
        date = date + pd.Timedelta(f"{hours_in_change:.2f} minutes")
        points += 1

    log._list = [ trace_ins ]

    return log

def make_endogenous(panels:Dict[str,EventLog]) -> EventLog:
    log = controlflow.generate_log(panels)

    # save endogenous log
    pm4py.write_xes(log, ENDO_LOG_OUT)

    return log 

def make_exo_panels() -> Dict[str,EventLog]:
    panels = {
        "copper" : None,
        "iron" : None,
        "capacity" : None
    }

    for name,key,coster in zip(["copper", "iron", "capacity"],panels.keys(), [copper_price,iron_price,capacity_left]):
        panels[key] = create_exo_log(name,coster)

    # save exo panels
    pm4py.write_xes( panels["copper"], EXO_PANEL_01_OUT)
    pm4py.write_xes( panels["iron"], EXO_PANEL_02_OUT)
    pm4py.write_xes( panels["capacity"], EXO_PANEL_03_OUT)

    return panels


def make_vis(log:EventLog):
    cmap = get_cmap(HIGH_CONTRAST_WARM, 14)
    presentor = vispm.StaticDottedChartPresentor(log,dpi=100,
        event_colour_scheme=vispm.StaticDottedChartPresentor.EventColourScheme.EventLabel,
        colormap=cmap
    )

    ext = vispm.DescriptionHistogramExtension(
    )
    presentor.add_extension(ext)

    ext = vispm.DescriptionHistogramExtension(
        direction=vispm.DescriptionHistogramExtension.Direction.EAST,
        describe=vispm.DescriptionHistogramExtension.Describe.TraceLength,
        density=vispm.DescriptionHistogramExtension.Density.Event
    )
    presentor.add_extension(ext)

    ext = vispm.DescriptionHistogramExtension(
        direction=vispm.DescriptionHistogramExtension.Direction.SOUTH,
        describe=vispm.DescriptionHistogramExtension.Describe.TraceDuration,
        density=vispm.DescriptionHistogramExtension.Density.Trace
    )
    presentor.add_extension(ext)

    ext = vispm.DescriptionHistogramExtension(
        direction=vispm.DescriptionHistogramExtension.Direction.WEST,
        describe=vispm.DescriptionHistogramExtension.Describe.Monthday,
        density=vispm.DescriptionHistogramExtension.Density.Event
    )
    presentor.add_extension(ext)

    presentor.plot()
    presentor.get_figure().savefig(DESCRIBE_PIC, dpi=300, bbox_inches="tight")

def main():
    panels = make_exo_panels()
    log = make_endogenous(panels)
    make_vis(log)


if __name__ == "__main__":
    main()