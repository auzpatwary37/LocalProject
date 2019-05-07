package largeScaleScenario;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;

import cadytsTry.CadytsModule;
import dynamicTransitRouter.DynamicRoutingModule;
import dynamicTransitRouter.fareCalculators.ZonalFareXMLParserV2;
import running.RunUtils;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.AnaModelCalibrationModule;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;

public class SimRunHKI implements SimRun{
	private int lastIteration=150;

	public SimRunHKI(int i) {
		// TODO Auto-generated constructor stub
		this.lastIteration=i;
	}

	@Override
	public void run(AnalyticalModel sue, Config config, LinkedHashMap<String, Double> params, boolean generateOd,
			String threadNo, MeasurementsStorage storage) {
		
		config.controler().setLastIteration(this.lastIteration);
		config.controler().setOutputDirectory("LargeScaleOutput/output"+threadNo);
		//config.removeModule("roadPricing");
		config.removeModule("emission");
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());	
		
		for(VehicleType vt: scenario.getVehicles().getVehicleTypes().values()) {
			if(vt.getPcuEquivalents()==1) {
				vt.setPcuEquivalents(0.7);
			}
		}
		
		Controler controler = new Controler(scenario);
		ZonalFareXMLParserV2 busFareGetter = new ZonalFareXMLParserV2(scenario.getTransitSchedule());
		SAXParser saxParser;
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse("data/busFare.xml", busFareGetter);
		} catch (ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		

		// Add the signal module to the controller
		Signals.configure(controler);
		//controler.addOverridingModule(new SignalsModule());
		controler.addOverridingModule(new DynamicRoutingModule(busFareGetter.get(), "fare/mtr_lines_fares.csv", 
				"fare/GMB.csv", "fare/light_rail_fares.csv"));
		RoadPricingScheme scheme = RunUtils.createDefaultRoadPricingScheme();
		controler.addOverridingModule(new RoadPricingModule(scheme));
		
		
		//controler.addOverridingModule(new RoadPricingModule(RunUtils.createDefaultRoadPricingScheme()));
		controler.addOverridingModule(new AnaModelCalibrationModule(storage, sue,"LargeScaleOutput/Calibration/",params,true));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		
		
	}
}
