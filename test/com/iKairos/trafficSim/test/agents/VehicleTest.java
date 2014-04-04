package com.iKairos.trafficSim.test.agents;

import com.iKairos.trafficSim.agents.Vehicle;
import com.iKairos.trafficSim.models.IDM;
import com.iKairos.trafficSim.models.LaneChangeModel;
import com.iKairos.trafficSim.models.SharedConstants;
import com.iKairos.trafficSim.network.Edge;
import com.iKairos.trafficSim.network.EdgeType;
import com.iKairos.trafficSim.network.Lane;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

public class VehicleTest {

    Vehicle car, car1, car2;
    Lane lane0, lane1;
    Edge edge;
    IDM idmMock;
    LaneChangeModel laneChangeMock;

    @Before
    public void setUp() {
        edge = new Edge(EdgeType.TWO_WAY_RURAL_ROAD);
        lane0 = new Lane(0, edge);
        lane1 = new Lane(1, edge);

        car = new Vehicle(0, lane0);
        car1 = new Vehicle(1, lane0);
        car2 = new Vehicle(2, lane0);

        idmMock = mock(IDM.class);
        laneChangeMock = mock(LaneChangeModel.class);
        SharedConstants.idm = idmMock;
        SharedConstants.laneChangeModel = laneChangeMock;
    }

    @Test
    public void shouldAddItselfToParentLaneUponInstantiation() {
        Lane lane0Mock = mock(Lane.class);
        car = new Vehicle(1, lane0Mock);
        verify(lane0Mock).addVehicle(car);
    }

    @Test
    public void shouldSortVehiclesInDescendingOrderOfPosition() {
        car1.setPosition(100.0);
        car2.setPosition(700.0);
        List<Vehicle> cars = Arrays.asList(car1, car2);
        List<Vehicle> sortedCars = Arrays.asList(car2, car1);
        Collections.sort(cars);
        assertThat(cars, equalTo(sortedCars));
    }

    @Test
    public void shouldEquateVehiclesOfTheSameID() {
        Vehicle car3 = new Vehicle(car1.getId(), lane0);
        assertThat(car3, equalTo(car1));
    }

    @Test
    public void shouldBeAtPositionZeroOnCreation() {
        Vehicle car = new Vehicle(1, lane0);
        assertThat(car.getPosition(), is(0.0));
    }

    @Test
    public void shouldMaintainInsertionOrderOfCarsIfTheyAreAtTheSamePosition() {
        List<Vehicle> cars = Arrays.asList(car1, car2);
        assertThat(car1.getPosition(), is(car2.getPosition()));
        Collections.sort(cars);
        assertThat(cars, equalTo(Arrays.asList(car1, car2)));
    }

    @Test
    public void shouldLeaveCurrentLaneAfterLaneChange() {
        car = new Vehicle(1, lane0);
        car.changeLane(lane1);
        assertThat(car.getCurrentLane(), is(lane1));
    }

    @Test
    public void shouldRetainItsPositionAfterLaneChange() {
        car.setPosition(125.0d);
        car.changeLane(lane1);
        assertThat(car.getPosition(), is(125.0d));
    }

    @Test
    public void shouldUpdateVelocityAfterTranslateUsingFirstEquationOfMotion() {
        fixIdmAcceleration(0.5);
        car.setVelocity(12.0);
        car.translate(100);
        assertThat(car.getVelocity(), is(62d)); // v = u + at where u = 0
    }

    @Test
    public void shouldUpdatePositionAfterTranslateUsingSecondEquationOfMotion() {
        fixIdmAcceleration(0.5);
        car.setVelocity(12.0);
        car.setPosition(10);
        car.translate(10);
        assertThat(car.getPosition(), is(155.0)); //pos = currentPos + s | s = ut + 0.5(at^2)
    }

    @Test
    public void shouldAttemptLaneChangeIfCurrentVelocityIsLessThanDesiredVelocity() {
        car.setVelocity(car.getDesiredVelocity() - 1);
        car.translate(10);
        verify(laneChangeMock).changeLaneIfNecessary((Vehicle)anyObject());
    }

    @Test
    public void shouldNotAttemptLaneChangeIfCurrentVelocityIsEqualToDesiredVelocity() {
        car.setVelocity(car.getDesiredVelocity());
        car.translate(10);
        verify(laneChangeMock, never()).changeLaneIfNecessary((Vehicle)anyObject());
    }

    private void fixIdmAcceleration(double acceleration) {
        when(idmMock.calculateAcceleration((Vehicle)anyObject(), (Vehicle)anyObject())).thenReturn(acceleration);
    }
}