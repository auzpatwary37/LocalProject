package populationGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;


/**
 * 
 * @author ashraf
 *
 */
public class SubPopulationTry {
	
	private static final boolean HkiSeperation=true;
	private static final double weightFactorgvtcs=.1;
	private static final double weightFactorTCS=.07;
	private static Double tripPerson=0.;
	private static Double personPerson=0.;
	public static void main(String[] args) throws IOException {
		Double tripPerson=0.;
		Double personPerson=0.;
		Config config=ConfigUtils.createConfig();
		Scenario scenario=ScenarioUtils.createScenario(config);
		Population population=scenario.getPopulation();
		Vehicles vehicles=scenario.getVehicles();
		
		/**
		 * TCS Database
		 */
		Database tcsDatabase=DatabaseBuilder.open(new File("data/TCSDatabase/TCS2011 database.accdb"));
		Table tpusbCoord=tcsDatabase.getTable("TPUSB_coordinate_Mod");
		Table tpusb11=tcsDatabase.getTable("11TPUSB");
		HashMap<Id<TPUSB>,TPUSB> tpusbs=gvtcsConverter.tpusbCreator(tpusbCoord,tpusb11);
		HashMap<Double,String> activityDetailsTCS=new HashMap<>();
		HashMap<Double,TCSMode> modesDetails=new HashMap<>();
		String activityFileLoc="data/TCSDatabase/ActivityManual1.csv";
		String modeFileLoc="data/TCSDatabase/ModeManualPaper.csv";
		
		TCSExtractor.readModeAndActivityTypeManual(activityFileLoc, modeFileLoc, activityDetailsTCS, modesDetails);
		
		Table HH=tcsDatabase.getTable("HH");
		Table HM=tcsDatabase.getTable("HM");
		Table TP=tcsDatabase.getTable("TP");
		
		HashMap<Id<HouseHold>,HouseHold> houseHolds=TCSExtractor.createHouseHolds(HH,tpusbs,weightFactorTCS);
		HashMap<Id<HouseHoldMember>,HouseHoldMember> members=TCSExtractor.createMember(HM,TP,tpusbs,houseHolds,weightFactorTCS);
		
		if(HkiSeperation) {
			TCSExtractor.HKITripExtractor(members);
		}
		
		for(HouseHoldMember hm:members.values()) {

			hm.loadClonedVehicleAndPersons(scenario, activityDetailsTCS, modesDetails, "person", "trip",tripPerson,personPerson,false);

		}

		
		
		
		/**
		 * GVTCS Database
		 */
		
		Database gvtcsDatabase=DatabaseBuilder.open(new File("data/GVTCS DATABASE/GVTCS.accdb"));
		Table govOwner=gvtcsDatabase.getTable("GOV-OWNER");
		Table govTrip=gvtcsDatabase.getTable("GOV-TRIP");
		Table govVehicle=gvtcsDatabase.getTable("GOV-VEH");
		Table ngovOwner=gvtcsDatabase.getTable("NGOV-OWNER");
		Table ngovTrip=gvtcsDatabase.getTable("NGOV-TRIP");
		Table ngovVehicle=gvtcsDatabase.getTable("NGOV-VEH");
		Table sectors=gvtcsDatabase.getTable("Sectors");
		Table sgisTrip=gvtcsDatabase.getTable("SGIS-TRIP");
		
		BufferedReader bf=new BufferedReader(new FileReader(new File("data/GVTCS DATABASE/LandUseCode.csv")));
		bf.readLine();
		String line;
		HashMap<Double,String>activityDetailsgvtcs=new HashMap<>();
		while((line=bf.readLine())!=null) {
			String[] part=line.split(",");
			activityDetailsgvtcs.put(Double.parseDouble(part[0].trim()), part[1].trim());
			//gvtcsConverter.addActivityPlanParameter(config.planCalcScore(),part[1].trim(),30*60);
 		}
		
		//HashMap<Id<Vehicle>,GoodsVehicle> goodsVehicles=gvtcsConverter.createGovVehicles(govTrip,govVehicle,tpusbs,weightFactorgvtcs,!HkiSeperation);
		
		//goodsVehicles.putAll(gvtcsConverter.createNonGovVehicles(ngovTrip,ngovVehicle,tpusbs,weightFactorgvtcs,!HkiSeperation));
		
		
//		for(GoodsVehicle gv:goodsVehicles.values()) {
//			gv.loadClonedVehicleAndPersons(scenario, activityDetailsgvtcs, "person", "trip",tripPerson,personPerson);
//		}
		boolean isConsistant = activityConsistancyTester(population);
		ActivityAnalyzer ac=new ActivityAnalyzer();
		HashMap<String,Double>activityDuration= ac.getAverageActivityDuration(population);
		HashMap<String,Double>activityStartTime=ac.getAverageStartingTime(population);
		HashMap<String,Double>activityEndTime=ac.getAverageClosingTime(population);
		
		Set<String>activityTypes=null;
		activityTypes=ac.getActivityTypes(population);
		Set<String>startAndEndActivities=ac.getStartOrEndActivityTypes(population);
		PlanCalcScoreConfigGroup cp=config.planCalcScore();
		//ac.ActivitySplitter(population, config, "Home", 12*3600.);
		ac.analyzeActivities(population, "toyScenarioLarge/activityDetails1.csv","toyScenarioLarge/activityDistributions.csv");
		
//		ActivityAnalyzer.addActivityPlanParameter(cp, activityTypes, activityDuration, activityStartTime,activityEndTime,startAndEndActivities, 
//				15,15, 8*60*60, 15*60, 8*3600,20*3600, true);
		ac.readActivityTimings("data/toyScenarioLargeData/ActivityTimings.csv", config);
		
		//config.addModule(cp);
//		for(String s:activityDetailsTCS.values()) {
//			if(activityDuration.containsKey(s)) {
//				if(activityDuration.get(s)==0) {
//					activityDuration.put(s, 1.0);
//				}
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,activityDuration.get(s).intValue());
//			}else {
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,8*60*60);
//			}
//		}
//		for(String s:activityDetailsgvtcs.values()) {
//			if(activityDuration.containsKey(s)) {
//				if(activityDuration.get(s)==0) {
//					activityDuration.put(s, 1.0);
//				}
//			
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,activityDuration.get(s).intValue());
//			}else {
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,8*60*60);
//			}
//		}
		
		
		//ActivityAnalyzer.ActivitySplitter(population, config, "Usual place of work", 60*30.);
		
		
		ConfigWriter configWriter=new ConfigWriter(config);
		PopulationWriter popWriter=new PopulationWriter(population);
		VehicleWriterV1 vehWriter=new VehicleWriterV1(vehicles);
		
		
//		popWriter.write("data/LargeScaleScenario/populationHKI.xml");
//		vehWriter.writeFile("data/LargeScaleScenario/VehiclesHKI.xml");
//		configWriter.write("data/LargeScaleScenario/config_Ashraf.xml");
//		new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("data/LargeScaleScenario/personAttributesHKI.xml");
		
		popWriter.write("data/toyScenarioLargeData/populationHKIPaper.xml");
		vehWriter.writeFile("data/toyScenarioLargeData/VehiclesHKIPaper.xml");
		configWriter.write("data/toyScenarioLargeData/configPaperactivityParam.xml");
		//new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("data/LargeScaleScenario/personAttributesHKI.xml");
		
		System.out.println("total Population = "+population.getPersons().size());
		System.out.println("total Vehicles = "+vehicles.getVehicles().size());
  		
		
		
		//System.out.println("total tripPerson = "+tripPerson);
		//System.out.println("total personPerson = "+personPerson);
		
		//System.out.println("ratio of the background population with real population = "+tripPerson/(tripPerson+personPerson));
		
		System.out.println("TestLine");

	}
	
	public static boolean activityConsistancyTester(Population population) {
		boolean isConsistant=true;
		for(Person person:population.getPersons().values()) {
			for(Plan plan:person.getPlans()) {
				for(PlanElement pe:plan.getPlanElements()) {
					if(pe instanceof Activity) {
						Activity a=(Activity)pe;
						if(a.getStartTime()!=Double.NEGATIVE_INFINITY && a.getEndTime()!=Double.NEGATIVE_INFINITY && a.getStartTime()>a.getEndTime()) {
							isConsistant=false;
							return isConsistant;
						}
					}else {
						continue;
					}
				}
			}
		}
		return isConsistant;
	}
	
	
}


