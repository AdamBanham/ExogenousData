
# Exemplar Usage

In this directory we provide exemplar inputs for our Plugin, a description of the process for which the exemplars represent and how to generate alternative instances using the sub directory called "maker".

Exemplar Inputs:
 - An endogenous event log, 
   - endo_log.xes
 - Several exo-panels to instanitanate xPM using our Plugin, and
   - exo_panel_01.xes
   - exo_panel_02.xes
   - exo_panel_03.xes
 - A Petri net with Data control flow model
   - model.pnml

## Process 

The control flow the process, from which we build upon for this exemplar can be seen as a Petri net below:
<br>
![Exemplar control flow](maker/process.svg)
<br>

The control flow of this process, describes a linear process with a rework loop. 
In this process, there are three decision points, a deicision between D,E,F at p4, a decision to rework at p6, and a choice of K or L at p8. 
The process itself is not based on any real-world environment or use case but can be used to showcase the tools provided in our plugin. 
The process is about making products for a customer, adjusting the costs of products to maximunise profit by forecasting future costs of materials for a just in time supply chain.
In order to offset future costs, during a process more products may be upsold to the customer to offset price increases but the company only has a fixed amount of production capacity so decisions are made upon the future capacity of the company.

## Endogenous Data

For each event in the endogenous event log, we have the following mandatory attributes
 - concept:name = activity label associated with this event
 - lifecycle:transition = state of execution
 - time:timestamp = when an event occured

The following optional attributes could also be attached to events
 - profit = the amount of profit from invoice
 - cost = the amount of cost associated with the event
 - org:resource = the actor responsible for the event
 - items = the total number of products related to the invoice after the event occured

## Exogenous Data



## Python make process


