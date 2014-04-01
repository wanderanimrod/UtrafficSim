package com.iKairos.trafficSim.test.network;

import com.iKairos.trafficSim.network.Edge;
import com.iKairos.trafficSim.network.EdgeType;
import com.iKairos.trafficSim.network.Lane;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class EdgeTest {

    @Test
    public void shouldGetNextLaneAs1IfIdOfCurrentLaneIs0OnRuralRoad() {
        Edge edge = new Edge(1, EdgeType.TWO_WAY_RURAL_ROAD, 1.0);
        Lane lane0 = new Lane(0);
        Lane lane1 = new Lane(1);
        edge.addLane(lane0);
        edge.addLane(lane1);

        assertEquals(lane1, edge.getNextLane(lane0));
    }
}
