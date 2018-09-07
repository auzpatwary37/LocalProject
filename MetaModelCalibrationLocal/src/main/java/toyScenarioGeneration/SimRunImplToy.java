package toyScenarioGeneration;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import dynamicTransitRouter.DynamicRoutingModule;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.AnaModelCalibrationModule;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;

public class SimRunImplToy implements SimRun{

	private final int lastIteration=150;

	@Override
	public void run(AnalyticalModel sue, Config config, LinkedHashMap<String, Double> params, boolean generateOd,
			String threadNo, MeasurementsStorage storage) {
		
		config.controler().setLastIteration(this.lastIteration);
		config.controler().setOutputDirectory("toyScenario/output"+threadNo);
		config.transit().setUseTransit(true);
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.global().setCoordinateSystem("arbitrary");
		config.parallelEventHandling().setNumberOfThreads(3);
		config.controler().setWritePlansInterval(50);
		config.qsim().setStartTime(0.0);
		config.qsim().setEndTime(93600.0);
		config.global().setNumberOfThreads(4);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.controler().setWriteEventsInterval(50);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		try {
			controler.addOverridingModule(new DynamicRoutingModule(transitGenerator.createBusFareCalculator(scenario.getTransitSchedule()),"src/main/resources/toyScenarioData/Mtr_fare.csv","src/main/resources/toyScenarioData/GMB.csv"));
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		controler.addOverridingModule(new AnaModelCalibrationModule(storage, sue,"src/main/resources/toyScenarioData/Calibration/",params,true));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}
}
