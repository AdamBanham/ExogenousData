
from typing import Union
from uuid import uuid4

class Place():

    def __init__(self, id:int, label:str, initial:bool, final:bool,) -> None:
        self._id = f"p{id}"
        self._label = label 
        self._isinitiallymarked = initial
        self._isfinalmarking = final 
        self._uuid = str(uuid4())

    def is_initial(self) -> bool:
        return self._isinitiallymarked

    def is_final(self) -> bool:
        return self._isfinalmarking

    def __str__(self) -> str:
        xml = f"""\t\t\t\t<place id="{self._id}">\n
        \t\t\t\t\t<name>\n
        \t\t\t\t\t\t<text>{self._label}</text>\n
        \t\t\t\t\t</name>\n
        \t\t\t\t\t<toolspecific localNodeID="{self._uuid}" tool="ProM" version="6.4"/>\n
        """

        if self._isinitiallymarked:
            xml = xml + """\t\t\t\t\t<initialMarking>\n
            \t\t\t\t\t\t<text>1</text>\n
            \t\t\t\t\t</initialMarking>\n
            """
        
        if self._isfinalmarking:
            xml = xml + """\t\t\t\t\t<finalMarking>\n
            \t\t\t\t\t\t<text>1</text>\n
            \t\t\t\t\t</finalMarking>\n
            """

        xml = xml + """\t\t\t\t</place>\n"""

        return xml

    def __repr__(self) -> str:
        return self.__str__()

class Transition():

    def __init__(self, id:int, invisible:bool, label:str) -> None:
        self._id = f"t{id}"
        self._invis = invisible
        self._label = label 
        self._uuid = str(uuid4())

    def __str__(self) -> str:
        xml = f"""\t\t\t\t<transition id="{self._id}" invisible="{'true' if self._invis else 'false'}">\n
        \t\t\t\t\t<name>\n
        \t\t\t\t\t\t<text>{self._label}</text>\n
        \t\t\t\t\t</name>\n
        \t\t\t\t\t<toolspecific localNodeID="{self._uuid}" activity="{self._label}" tool="ProM" version="6.4"/>\n
        """
        xml = xml + """\t\t\t\t</transition>\n"""
        return xml

    def __repr__(self) -> str:
        return self.__str__()


class Arc():

    def __init__(self, id:int, source:Union[Place,Transition], target:Union[Place,Transition]) -> None:
        self._id = f"arc{id}"
        self._source = source._id 
        self._target = target._id
        self._uuid = str(uuid4())

    def __str__(self) -> str:
        xml = f"""\t\t\t\t<arc id="{self._id}" source="{self._source}" target="{self._target}">\n
        \t\t\t\t\t<name>\n
        \t\t\t\t\t\t<text>1</text>\n
        \t\t\t\t\t</name>\n
        \t\t\t\t\t<toolspecific localNodeID="{self._uuid}" tool="ProM" version="6.4"/>\n
        \t\t\t\t\t<arctype>\n
        \t\t\t\t\t\t<text>normal</text>\n
        \t\t\t\t\t</arctype>\n
        \t\t\t\t</arc>\n
        """
        return xml

    def __repr__(self) -> str:
        pass


class PetriNetWithData():

    def __init__(self, name:str) -> None:
        self._name = name 
        self._places = list()
        self._transitions = list()
        self._arcs = list()

    def add_place(self,place:Place):
        self._places.append(place)

    def add_transition(self, transition:Transition):
        self._transitions.append(transition)

    def add_arcs(self, arc:Arc):
        self._arcs.append(arc)

    def __str__(self) -> str:
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>\n
        <pnml>\n
        \t<net id="net1" type="http://www.pnml.org/version-2009/grammar/pnmlcoremodel">\n
        \t\t<name>\n
        \t\t\t<text>{self._name}</text>\n
        \t\t</name>\n
        \t\t<page id="page1">\n
        \t\t\t<name>\n
        \t\t\t\t<text/>\n
        \t\t\t</name>\n
        """

        for place in self._places:
            xml = xml + str(place)

        for trans in self._transitions:
            xml = xml + str(trans)

        for arc in self._arcs:
            xml = xml + str(arc)

        xml = xml + """\t\t</page>"""

        xml = xml + """
        \t\t<finalmarkings>\n
        \t\t\t<marking>\n
        """
        for place in self._places:
                xml = xml + f"""
                \t\t\t\t<place idref="{place._id}">\n
                \t\t\t\t\t<text>0</text>\n
                \t\t\t\t</place>\n
                """

        xml = xml + """\t\t\t</marking>\n
        \t\t</finalmarkings>\n
        """

        xml = xml + """
        \t\t<variables/>\n
        \t</net>\n
        </pnml>\n
        """

        return xml

    def __repr__(self) -> str:
        return self.__str__()