package toyScenarioGeneration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;



import dynamicTransitRouter.DynamicRoutingModule;
import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.MTRFareCalculator;

public class RunFile {
	public static void main(String[] args) throws IOException {
		Config config=ConfigUtils.createConfig();
		//ConfigUtils.loadConfig(config, "src/main/resources/toyScenarioData/config.xml");
		Scenario scenario=ScenarioUtils.createScenario(config);
		Network network=NetworkGenerator.generateNetwork(scenario.getNetwork());
		transitGenerator.createTransit(scenario,network);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile("src/main/resources/toyScenarioData/transitVehicles.xml");
		PopulationGenerator.LoadPopulation(scenario,8000);
		MTRFareCalculator MTRFare=new MTRFareCalculator("src/main/resources/toyScenarioData/Mtr_Fare.csv",scenario.getTransitSchedule());
		FareCalculator BusFare=transitGenerator.createBusFareCalculator(scenario.getTransitSchedule());
		new PopulationWriter(scenario.getPopulation()).write("src/main/resources/toyScenarioData/population.xml");
		new VehicleWriterV1(scenario.getVehicles()).writeFile("src/main/resources/toyScenarioData/vehicles.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("src/main/resources/toyScenarioData/transitSchedule.xml");
		new NetworkWriter(scenario.getNetwork()).write("src/main/resources/toyScenarioData/network.xml");
		config.network().setInputFile("src/main/resources/toyScenarioData/network.xml");
		config.plans().setInputFile("src/main/resources/toyScenarioData/population.xml");
		config.transit().setTransitScheduleFile("src/main/resources/toyScenarioData/transitSchedule.xml");
		config.vehicles().setVehiclesFile("src/main/resources/toyScenarioData/vehicles.xml");
		config.controler().setOutputDirectory("src/main/resources/toyScenarioData/output");
		config.transit().setVehiclesFile("src/main/resources/toyScenarioData/transitVehicles.xml");
		config.transit().setUseTransit(true);
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		
		PopulationGenerator.addPlanParameter(config.planCalcScore(), "home", 16*60*60);
		PopulationGenerator.addPlanParameter(config.planCalcScore(), "work", 8*60*60);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		//config.global().setNumberOfThreads(7);
		config.controler().setLastIteration(10);
		config.global().setCoordinateSystem("arbitrary");
		config.parallelEventHandling().setNumberOfThreads(5);
		config.controler().setWritePlansInterval(1);
		
		config.controler().setLastIteration(150);
		
		createCountData(network);
	
		//config.strategy().createParameterSet("strategysettings");
		
		
		
	
		
		
		
		
		new ConfigWriter(config).write("src/main/resources/toyScenarioData/config.xml");
		Map<String,FareCalculator> fareCalc=new HashMap<>();
		fareCalc.put("bus",BusFare);
		fareCalc.put("train", MTRFare);
		
		
		
		
		
		scenario=ScenarioUtils.loadScenario(config);
		Controler controler=new Controler(scenario);
		
		
		controler.addOverridingModule(new DynamicRoutingModule(transitGenerator.createBusFareCalculator(scenario.getTransitSchedule()),"src/main/resources/toyScenarioData/Mtr_fare.csv"));
		//this.controler.addOverridingModule(new AnaModelCalibrationModule(this.countData, this.sueAssignment));
		//this.controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		
		System.out.println("Done O Done!!! ");
		//controler.run();
	}
	
	public static void createCountData(Network network) throws IOException {
		FileWriter fw=new FileWriter(new File("src/main/resources/toyScenarioData/countData.csv"));
		fw.append("StationId"+","+"Sat-X"+","+"Sat-Y"+","+"Type"+","+"Road name"+","+"From"+","+"To"+","+"2010"+","+"2011"+","+"FromX"+","+"FromY"+","+"ToX"+","+"ToY"+"\n");
		for(Link l:network.getLinks().values()) {
			if(!(l.getId().toString().contains("stop"))&&!(l.getId().toString().contains("MTR"))) {
				fw.append(l.getId().toString()+","+0.5*(l.getFromNode().getCoord().getX()+l.getToNode().getCoord().getX())+","+
			0.5*(l.getFromNode().getCoord().getY()+l.getToNode().getCoord().getY())+","+""+","+l.getId().toString()+","+l.getFromNode().getId().toString()+
			","+l.getToNode().getId().toString()+","+0+","+0+","+l.getFromNode().getCoord().getX()+","+l.getFromNode().getCoord().getY()+","+
			l.getToNode().getCoord().getX()+","+l.getToNode().getCoord().getY()+"\n");
			}
		}
		fw.flush();
		fw.close();
	}
}
