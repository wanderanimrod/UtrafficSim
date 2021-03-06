package com.iKairos.trafficSim.test.agents;

import com.iKairos.trafficSim.agents.Vehicle;
import com.iKairos.trafficSim.models.IDM;
import com.iKairos.trafficSim.models.LaneChangeModel;
import com.iKairos.trafficSim.models.SharedConstants;
import com.iKairos.trafficSim.network.Lane;
import com.iKairos.trafficSim.network.TwoLaneOneWayEdge;
import com.iKairos.trafficSim.test.helpers.VehicleHelpers;
import com.iKairos.utils.*;
import com.iKairos.utils.IllegalArgumentException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

public class VehicleTest {

    Vehicle car, car1, car2;
    Lane lane0, lane1;
    TwoLaneOneWayEdge edge;
    IDM idmMock;
    LaneChangeModel laneChangeMock;
    VehicleHelpers helpers;

    @Before
    public void setUp() throws IllegalMethodCallException {
        edge = new TwoLaneOneWayEdge();
        lane0 = new Lane(0, edge);
        lane1 = new Lane(1, edge);

        car = new Vehicle(0, lane0);
        car1 = new Vehicle(1, lane0);
        car2 = new Vehicle(2, lane0);

        idmMock = mock(IDM.class);
        laneChangeMock = mock(LaneChangeModel.class);

        SharedConstants.idm = idmMock;
        SharedConstants.laneChangeModel = laneChangeMock;

        helpers = new VehicleHelpers(idmMock);
    }

    @After
    public void tearDown() {
        SharedConstants.idm = new IDM();
        SharedConstants.laneChangeModel = new LaneChangeModel();
    }

    @Test
    public void shouldAddItselfToParentLaneUponInstantiation() {
        Lane lane0Mock = mock(Lane.class);
        car = new Vehicle(1, lane0Mock);
        verify(lane0Mock).addVehicle(car);
    }

    @Test
    public void shouldMakeVehicleWithPoliteness() {
        Vehicle vehicle = Vehicle.makeVehicleWithPoliteness(1, mock(Lane.class), 0.8);
        assertThat(vehicle.getPoliteness(), is(0.8));
    }

    @Test
    public void shouldSortVehiclesInDescendingOrderOfPosition() {
        helpers.moveVehicleToPosition(car1, 100);
        helpers.moveVehicleToPosition(car2, 700);
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
    public void shouldMakeDummyLeaderWithoutALaneWithPosition100000() {
        Vehicle dummyLeader = Vehicle.makeDummyLeader();
        assertThat(dummyLeader.getPosition(), is(100000.0));
        assertThat(dummyLeader.getCurrentLane(), equalTo(null));
    }

    @Test
    public void shouldMakeDummyFollowerWithoutLaneWithDesiredVelocityZero() {
        Vehicle dummyFollower = Vehicle.makeDummyFollower();
        assertThat(dummyFollower.getVelocity(), is(0.0));
        assertThat(dummyFollower.getDesiredVelocity(), is(0.0));
        assertThat(dummyFollower.getPosition(), is(0.0));
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
        helpers.moveVehicleToPosition(car, 125);
        car.changeLane(lane1);
        assertThat(car.getPosition(), is(125.0d));
    }

    @Test
    public void shouldRemoveItselfFromLaneBeforeChangingLanes() {
        Lane laneMock = mock(Lane.class);
        car = new Vehicle(1, laneMock);
        car.changeLane(lane0);
        verify(laneMock).removeVehicle(car);
    }

    @Test
    public void shouldUpdateVelocityAfterTranslateUsingFirstEquationOfMotion() {
        helpers.accelerateVehicleToVelocity(car, 12);
        helpers.fixIdmAcceleration(0.5);
        car.translate(100);
        assertThat(car.getVelocity(), is(62d)); // v = u + at where u = 0
    }

    @Test
    public void shouldUpdatePositionAfterTranslateUsingSecondEquationOfMotion() {
        helpers.fixIdmAcceleration(0.5);
        helpers.moveVehicleToPosition(car, 10);
        double initialVelocity = car.getVelocity();
        double expectedPosition = calculateExpectedPosition(10, 0.5, initialVelocity, 10);
        car.translate(10);
        assertThat(car.getPosition(), is(expectedPosition));
    }

    private double calculateExpectedPosition(double initialPosition, double fixedAcceleration, double initialVelocity, double changeInTime) {
        //pos = currentPos + s | s = ut + 0.5(at^2)
        return initialPosition + initialVelocity*changeInTime + 0.5*(fixedAcceleration*Math.pow(changeInTime, 2));
    }

    @Test
    public void shouldAttemptLaneChangeIfCurrentAccelerationIsSignificantlyLessThanMaxAcceleration() throws IllegalArgumentException {
        helpers.accelerateVehicleToVelocity(car, car.getMaxAcceleration() - 0.5);
        reset(laneChangeMock);
        car.translate(10);
        verify(laneChangeMock).getLaneChangeStatus((Vehicle) anyObject());
    }

    @Test
    public void shouldNotAttemptLaneChangeIfCurrentAccelerationIsAlmostEqualToMaximumAcceleration() throws IllegalArgumentException {
        helpers.accelerateVehicleTo(car, car.getMaxAcceleration());
        reset(laneChangeMock);
        car.translate(10);
        verify(laneChangeMock, never()).getLaneChangeStatus((Vehicle) anyObject());
    }
}