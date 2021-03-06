package largeToyScenarioGeneration;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import dynamicTransitRouter.DynamicRoutingModule;
import dynamicTransitRouter.fareCalculators.ZonalFareXMLParser;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.AnaModelCalibrationModule;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;

public class SimRunImplToyLarge implements SimRun{

	private final int lastIteration;
	
	public SimRunImplToyLarge() {
		this.lastIteration=200;
	}
	
	public SimRunImplToyLarge(int lastIter) {
		this.lastIteration=lastIter;
	}
	
	@Override
	public void run(AnalyticalModel sue, Config config, LinkedHashMap<String, Double> params, boolean generateOd,
			String threadNo, MeasurementsStorage storage) {

		config.controler().setLastIteration(this.lastIteration);
		config.controler().setOutputDirectory("toyScenarioLarge/output"+threadNo);
		config.transit().setUseTransit(true);
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		
		config.parallelEventHandling().setNumberOfThreads(10);
		config.controler().setWritePlansInterval(50);
		config.qsim().setStartTime(0.0);
		config.qsim().setEndTime(28*3600);
	
		config.controler().setWriteEventsInterval(20);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());	
		Controler controler = new Controler(scenario);
		ZonalFareXMLParser busFareGetter = new ZonalFareXMLParser(scenario.getTransitSchedule());
		SAXParser saxParser;
		
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse("data/toyScenarioLargeData/busFare.xml", busFareGetter);
			controler.addOverridingModule(new DynamicRoutingModule(busFareGetter.get(),"data/toyScenarioLargeData/mtr_lines_fares.csv","data/toyScenarioLargeData/GMB.csv"));
			
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		controler.addOverridingModule(new SignalsModule());
		controler.addOverridingModule(new AnaModelCalibrationModule(storage, sue,"toyScenarioLarge/Calibration/",params,true));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}
}


