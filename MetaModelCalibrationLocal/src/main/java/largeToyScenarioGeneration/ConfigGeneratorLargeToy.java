package largeToyScenarioGeneration;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;



public class ConfigGeneratorLargeToy {
	public static void main(String[] args) {
		Config config =ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config,"data/toyScenarioLargeData/config_clean_withoutsub.xml");
		config.plans().setInputFile("data/toyScenarioLargeData/populationHKIPaper.xml");
		Config configPlanParam = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(configPlanParam, "data/toyScenarioLargeData/configPaperactivityParam.xml");
		
		
		
		config.vehicles().setVehiclesFile("data/toyScenarioLargeData/VehiclesHKIPaper.xml");
		config.transit().setUseTransit(true);
		
		
		Scenario scenario=ScenarioUtils.loadScenario(config);
		
		reducePTCapacity(scenario.getTransitVehicles(),.1);
		reduceLinkCapacity(scenario.getNetwork(),.1);
		for(LanesToLinkAssignment l2l:scenario.getLanes().getLanesToLinkAssignments().values()) {
			for(Lane l: l2l.getLanes().values()) {
				
				//Why this is done? 
				//Why .1 specifically??
				l.setCapacityVehiclesPerHour(1800*.1);
			}
		}
		
		
		new NetworkWriter(scenario.getNetwork()).writeV1("data/toyScenarioLargeData/modNetwork.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile("data/toyScenarioLargeData/transitVehiclesMod.xml");
		new LanesWriter(scenario.getLanes()).write("data/toyScenarioLargeData/LanesMod.xml");
		
		
		Config configMod=ConfigUtils.createConfig();
		for (ActivityParams act: configPlanParam.planCalcScore().getActivityParams()) {
			if(configMod.planCalcScore().getActivityParams(act.getActivityType())==null) {
				configMod.planCalcScore().addActivityParams(act);
			}
		}
		
		SignalSystemsConfigGroup cs=new SignalSystemsConfigGroup();
		cs.setSignalControlFile("data/toyScenarioLargeData/signal_control.xml");
		cs.setSignalGroupsFile("data/toyScenarioLargeData/signal_groups.xml");
		cs.setSignalSystemFile("data/toyScenarioLargeData/signal_systems.xml");
		cs.setUseSignalSystems(true);
		
		configMod.addModule(cs);
		
		configMod.plans().setInputFile("data/toyScenarioLargeData/populationHKIPaper.xml");
		configMod.network().setInputFile("data/toyScenarioLargeData/modNetwork.xml");
		configMod.transit().setVehiclesFile("data/toyScenarioLargeData/transitVehiclesMod.xml");
		configMod.network().setLaneDefinitionsFile("data/toyScenarioLargeData/LanesMod.xml");
		configMod.transit().setTransitScheduleFile("data/toyScenarioLargeData/transitSchedule.xml");
		configMod.vehicles().setVehiclesFile("data/toyScenarioLargeData/VehiclesHKIPaper.xml");
		
		configMod.strategy().addParam("ModuleProbability_1", "0.8");
		configMod.strategy().addParam("Module_1", "ChangeExpBeta");
		configMod.strategy().addParam("ModuleProbability_2", "0.05");
		configMod.strategy().addParam("Module_2", "ReRoute");
		configMod.strategy().addParam("ModuleProbability_3", "0.1");
		configMod.strategy().addParam("Module_3", "TimeAllocationMutator");
		configMod.strategy().addParam("ModuleProbability_4", "0.05");
		configMod.strategy().addParam("Module_4", "ChangeTripMode");
		
		configMod.plansCalcRoute().setInsertingAccessEgressWalk(false);
		configMod.qsim().setUsePersonIdForMissingVehicleId(true);
		configMod.global().setCoordinateSystem("arbitrary");
		configMod.parallelEventHandling().setNumberOfThreads(12);
		configMod.controler().setWritePlansInterval(50);
		configMod.global().setNumberOfThreads(12);
		configMod.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		configMod.controler().setWriteEventsInterval(50);
		configMod.qsim().setUsingFastCapacityUpdate(false);
		configMod.qsim().setUseLanes(true);
		configMod.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		configMod.qsim().setStartTime(0.0);
		configMod.qsim().setEndTime(28*3600);
		configMod.qsim().setStuckTime(1000);
		configMod.qsim().setNodeOffset(20.);
		HashSet<String> modes=new HashSet<>();
		modes.add("pt");
		modes.add("train");
		modes.add("tram");
		configMod.transit().setTransitModes(modes);
		configMod.controler().setLinkToLinkRoutingEnabled(true);
		configMod.transit().setUseTransit(true);
		configMod.transitRouter().setAdditionalTransferTime(2.0);
		configMod.transitRouter().setDirectWalkFactor(100.);
		configMod.transitRouter().setMaxBeelineWalkConnectionDistance(200.);
		configMod.transitRouter().setExtensionRadius(200);
		configMod.transitRouter().setSearchRadius(500);
		configMod.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		
		new ConfigWriter(configMod).writeFileV2("data/toyScenarioLargeData/configToyLargeMod.xml");
		
	}
	
	private static void reducePTCapacity(Vehicles transitVehicles, Double factor) {
		for(VehicleType vt:transitVehicles.getVehicleTypes().values()) {
			vt.setPcuEquivalents(vt.getPcuEquivalents()*factor);
			VehicleCapacity vc=vt.getCapacity();
			if(vc.getSeats()!=null) {
				int seats=(int)Math.ceil(vc.getSeats()*factor);
				if(seats>0) {
					vc.setSeats(seats);
				}else {
					vc.setSeats(1);
				}
				
			}
			if(vc.getStandingRoom()!=null) {
				vc.setStandingRoom((int)Math.ceil(vc.getStandingRoom()*factor));
			}
		}
	}
	
	private static void reduceLinkCapacity(Network network,double capacityFactor) {
		for(Link l:network.getLinks().values()) {
			l.setCapacity(l.getCapacity()*capacityFactor);
		}
		
	}
}
