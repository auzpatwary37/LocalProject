package largeScaleScenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.SAXException;

import cadytsTry.CadytsContextHKI;
import cadytsTry.CadytsModule;
import dynamicTransitRouter.DynamicRoutingModule;
import dynamicTransitRouter.TransitRouterFareDynamicImpl;
import dynamicTransitRouter.fareCalculators.ZonalFareXMLParserV2;
import running.RunUtils;

public class TCSRun {
	private static void modifyPopulation(Population population){
		for(Person person: population.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			Plan newPlan = PopulationUtils.createPlan(person);
			List<PlanElement> toAdd = new ArrayList<PlanElement>();
			boolean swifted = false;
			for(PlanElement pe: plan.getPlanElements()) {
				if(pe instanceof Leg) {
					Leg leg = (Leg) pe;
					double depTime = leg.getDepartureTime();
					if(swifted|| (depTime <= 2 * 3600 && depTime >= 0)) {
						leg.setDepartureTime(depTime + 24 * 3600);
						//swifted = true;
						toAdd.add(leg);
					}else {
						newPlan.addLeg(leg);
					}
					
				}else if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					double startTime = act.getStartTime();
					double endTime = act.getEndTime();
					if(swifted || (endTime <= 2 * 3600 && endTime >= 0)) {
						act.setEndTime(endTime + 24 * 3600);
						act.setStartTime(startTime + 24 * 3600);
						toAdd.add(act);
					}else {
						newPlan.addActivity(act);
					}
					
				}else {
					throw new RuntimeException("The PlanElement is strange!");
				}
			}
			
			for(PlanElement pe: toAdd) {
				if(pe instanceof Leg) {
					newPlan.addLeg((Leg) pe);
				}else if (pe instanceof Activity) {
					newPlan.addActivity((Activity) pe);
				}
			}
			person.removePlan(plan);
			person.addPlan(newPlan);
		}
	}
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "data/config_clean.xml");
		
		String PersonChangeWithCar_NAME = "person_TCSwithCar";
		String PersonChangeWithoutCar_NAME = "person_TCSwithoutCar";
		
		String PersonFixed_NAME = "trip_TCS";
		String GVChange_NAME = "person_GV";
		String GVFixed_NAME = "trip_GV";
		
		Config configGV = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(configGV, "data/config_Ashraf.xml");
		for (ActivityParams act: configGV.planCalcScore().getActivityParams()) {
			if(act.getActivityType().contains("Usual place of work")) {
				act.setMinimalDuration(3600 * 2);
			}
			if(config.planCalcScore().getScoringParameters(PersonChangeWithCar_NAME).getActivityParams(act.getActivityType())==null) {
				config.planCalcScore().getScoringParameters(PersonChangeWithCar_NAME).addActivityParams(act);
				config.planCalcScore().getScoringParameters(PersonChangeWithoutCar_NAME).addActivityParams(act);
				config.planCalcScore().getScoringParameters(PersonFixed_NAME).addActivityParams(act);
				config.planCalcScore().getScoringParameters(GVChange_NAME).addActivityParams(act);
				config.planCalcScore().getScoringParameters(GVFixed_NAME).addActivityParams(act);
			}
		}
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(400);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.9);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setWriteEventsInterval(25);
		config.controler().setWritePlansInterval(25);
		config.planCalcScore().setWriteExperiencedPlans(false);
		
//        EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
//        config.addModule(ecg);
		config.removeModule("emissions");

		TransitRouterFareDynamicImpl.distanceFactor = 0.034;
		config.controler().setOutputDirectory(
				"outputHKISCap1.3Feb2/");
		//Updated: The new transit router is applied
		//config.plans().setInputFile("outputHKISCap1.3Updated/output_plans.xml.gz");
		config.plans().setInputFile("data/populationHKI.xml");
		config.plans().setInputPersonAttributeFile("data/personAttributesHKI.xml");
		config.plans().setSubpopulationAttributeName("SUBPOP_ATTRIB_NAME"); /* This is the default anyway. */
		config.vehicles().setVehiclesFile("data/VehiclesHKI.xml");
		config.qsim().setNumberOfThreads(21);
		config.qsim().setStorageCapFactor(1.3);
		config.qsim().setFlowCapFactor(1.1);
		
		config.global().setNumberOfThreads(27);
		config.parallelEventHandling().setNumberOfThreads(10);
		config.parallelEventHandling().setEstimatedNumberOfEvents((long) 1000000000);
		
		RunUtils.createStrategies(config, PersonChangeWithCar_NAME, 0.02, 0.02, 0.005, 0);
		RunUtils.createStrategies(config, PersonChangeWithoutCar_NAME, 0.02, 0.02, 0.005, 0);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString(), PersonChangeWithCar_NAME, 
				0.015, 150);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute.toString(), PersonChangeWithCar_NAME, 
				0.015, 150);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString(), PersonChangeWithCar_NAME, 
				0.02, 50);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute.toString(), PersonChangeWithCar_NAME, 
				0.02, 50);
		
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString(), PersonChangeWithoutCar_NAME, 
				0.015, 130);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute.toString(), PersonChangeWithoutCar_NAME, 
				0.015, 130);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString(), PersonChangeWithoutCar_NAME, 
				0.02, 50);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute.toString(), PersonChangeWithoutCar_NAME, 
				0.02, 50);
		
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString(), PersonFixed_NAME, 
				0.03, 170);
		RunUtils.addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString(), GVFixed_NAME, 
				0.03, 170);
		
		RunUtils.createStrategies(config, PersonFixed_NAME, 0.02, 0.01, 0, 250);
		RunUtils.createStrategies(config, GVChange_NAME, 0.015, 0.01, 0, 0);
		RunUtils.createStrategies(config, GVFixed_NAME, 0.02, 0.01, 0, 250);

		//Create CadytsConfigGroup with defaultValue of everything
//		
//		CadytsConfigGroup cadytsConfig=new CadytsConfigGroup();
//		cadytsConfig.setEndTime((int)config.qsim().getEndTime());
//		cadytsConfig.setFreezeIteration(Integer.MAX_VALUE);
//		cadytsConfig.setMinFlowStddev_vehPerHour(25);
//		cadytsConfig.setPreparatoryIterations(10);
//		cadytsConfig.setRegressionInertia(.95);
//		cadytsConfig.setStartTime(0);
//		cadytsConfig.setTimeBinSize(3600);
//		cadytsConfig.setUseBruteForce(false);
//		cadytsConfig.setWriteAnalysisFile(true);
//		cadytsConfig.setVarianceScale(1.0);
		
		//add the cadyts config 
		
		//config.addModule(cadytsConfig);
		
		//general Run Configuration
		config.counts().setInputFile("data/ATCCountsPeakHourLink.xml");
		
		new ConfigWriter(config).writeFileV2("data/largeScenario_Config_TCSRun.xml");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		Network ctsNet = NetworkUtils.createNetwork();
//		new MatsimNetworkReader(ctsNet).readFile("input/network_TD.xml");
//		AssignLinkToPlanActivity.defineMatchingTablePath("matching/");
//		AssignLinkToPlanActivity.run(scenario, ctsNet, 28, true);
//		PopulationWriter popWriter=new PopulationWriter(scenario.getPopulation());
//		popWriter.write("output/FinalHKITCSandGVTCS/populationHKI.xml");
		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
//		RunUtils.scaleDownPopulation(scenario.getPopulation(), 0.15);
//		RunUtils.scaleDownPt(scenario.getTransitVehicles(), 0.15);
		for(VehicleType vt: scenario.getVehicles().getVehicleTypes().values()) {
			if(vt.getPcuEquivalents()==1) {
				vt.setPcuEquivalents(0.7);
			}
		}
		
		

		Controler controler = new Controler(scenario);

		ZonalFareXMLParserV2 busFareGetter = new ZonalFareXMLParserV2(scenario.getTransitSchedule());
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

		saxParser.parse("data/busFare.xml", busFareGetter);

		// Add the signal module to the controller
		Signals.configure(controler);
//		final WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
//		controler.getEvents().addHandler(waitTimeCalculator);
//		final StopStopTimeCalculatorImpl stopStopTimeCalculator = new StopStopTimeCalculatorImpl(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
//		controler.getEvents().addHandler(stopStopTimeCalculator);
//		final VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(controler.getScenario().getTransitSchedule(), ((MutableScenario)controler.getScenario()).getTransitVehicles(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
//		controler.getEvents().addHandler(vehicleOccupancyCalculator);
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bind(WaitTime.class).toInstance(waitTimeCalculator.get());
//				bind(StopStopTime.class).toInstance(stopStopTimeCalculator.get());
//				bind(VehicleOccupancy.class).toInstance(vehicleOccupancyCalculator.getVehicleOccupancy());
//				bind(TransitRouter.class).toProvider(TransitRouterEventsWSVFactory.class);
//			}
//		});
		controler.addOverridingModule(new DynamicRoutingModule(busFareGetter.get(), "fare/mtr_lines_fares.csv", 
				"fare/GMB.csv", "fare/light_rail_fares.csv"));
		controler.addOverridingModule(new CadytsModule());
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bind(PRAISEEmissionModule.class).asEagerSingleton();
//			}
//		});
		
		RoadPricingScheme scheme = RunUtils.createDefaultRoadPricingScheme();
		controler.addOverridingModule(new RoadPricingModule(scheme));
		//add Cadyts Scoring
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CadytsContextHKI cadytsContextHKI;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContextHKI);
				scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
		controler.run();
	}

}