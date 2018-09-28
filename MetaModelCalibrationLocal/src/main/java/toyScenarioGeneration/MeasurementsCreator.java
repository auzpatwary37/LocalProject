package toyScenarioGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.collections.Tuple;

import largeToyScenarioGeneration.SimRunImplToyLarge;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class MeasurementsCreator {
	public static Measurements CreateToyScenarioMeasurements(Map<String,Tuple<Double,Double>>timeBeans,String stationDetailsfileLoc,String measurementWriteFileLoc) {
		
		Measurements m=Measurements.createMeasurements(timeBeans);
		try {
			BufferedReader bf=new BufferedReader(new FileReader(new File(stationDetailsfileLoc)));
			bf.readLine();
			String line=null;
			
			while((line=bf.readLine())!=null) {
				String[] part=line.split(",");
				Id<Measurement> mId=Id.create(part[0].trim(), Measurement.class);
				ArrayList<Id<Link>> linkIds=new ArrayList<>();
				linkIds.add(Id.createLinkId(mId.toString()));
				m.createAnadAddMeasurement(mId.toString());
				m.getMeasurements().get(mId).setAttribute(Measurement.linkListAttributeName, linkIds);
				for(String s:timeBeans.keySet()) {
					m.getMeasurements().get(mId).addVolume(s, 0.);
				}
				
			}
			bf.close();
			new MeasurementsWriter(m).write(measurementWriteFileLoc);
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}
	
	/**
	 * The original param should be in number id format instead of paramName,paramValue format
	 * @param emptyMeasurements
	 * @param config
	 * @param preader
	 * @param simrun
	 * @param originalParam
	 * @param writeOrReadLocation
	 * @return
	 */
	public static Measurements generateSyntheticMeasurements(Measurements emptyMeasurements,Config config, ParamReader preader,SimRun simrun,LinkedHashMap<String,Double>originalParam, String writeOrReadLocation, boolean reWrite) {
		Measurements m=null;
		File file =new File(writeOrReadLocation);
		if(file.exists()&& reWrite==false) {
			m=new MeasurementsReader().readMeasurements(writeOrReadLocation);
			return m;
		}
		config=preader.SetParamToConfig(config, originalParam);
		
		new ConfigWriter(config).write("data/toyScenarioLargeData/originalParamConfig.xml");
		
		MeasurementsStorage ms=new MeasurementsStorage(emptyMeasurements);
		
		simrun.run(new CNLSUEModel(emptyMeasurements.getTimeBean()), config, originalParam, false,"fabricated",ms);
		
		m=ms.getSimMeasurement(originalParam);
		
		new MeasurementsWriter(m).write(writeOrReadLocation);
		
		return m;
	}
	
	public static Measurements loadWrittenMeasurements(Measurements emptyMeasurements,String fabricatedCountFileLoc) {
		Measurements m=emptyMeasurements.clone();
		try {
			BufferedReader buff=new BufferedReader(new FileReader(new File(fabricatedCountFileLoc)));
			//buff.readLine();
			String Line;
			String timeId=null;
			while((Line=buff.readLine())!=null) {
				String[] part=Line.split(",");
				Id<Measurement>mId=Id.create(part[1].trim(),Measurement.class);
				timeId=part[2].trim();
				double count=Double.parseDouble(part[3].trim());
				m.getMeasurements().get(mId).addVolume(timeId, count);
				
			}
			buff.close();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}
	
	public static HashMap<String, Tuple<Double,Double>> getDefaultTimeBean() {
		HashMap<String, Tuple<Double,Double>> timeBean=new HashMap<>();
		timeBean.put("BeforeMorningPeak", new Tuple<Double,Double>(0.0,25200.));
		timeBean.put("MorningPeak", new Tuple<Double,Double>(25200.,36000.));
		timeBean.put("AfterMorningPeak", new Tuple<Double,Double>(36000.,57600.));
		timeBean.put("AfternoonPeak", new Tuple<Double,Double>(57600.,72000.));
		timeBean.put("AfterAfternoonPeak", new Tuple<Double,Double>(72000.,86400.));
		return timeBean;
	}
	
	public static LinkedHashMap<String,Double> getOriginalParamSimplified() {
		LinkedHashMap<String,Double> originalParam=new LinkedHashMap<>();
		originalParam.put("MarginalUtilityofTravelCar",-25.);
		originalParam.put("MarginalUtilityofTravelpt",-20.);
		
		return originalParam;
	}
	
	public static void main(String[] args) {
		//Measurements emptyMeasurements=new MeasurementsReader().readMeasurements("src/main/resources/toyScenarioData/emptyMeasurements.xml");
		//Measurements toyScenarioMeasurements=loadWrittenMeasurements(emptyMeasurements,"src/main/resources/toyScenarioData/fabricatedCountData.csv");
		//new MeasurementsWriter(toyScenarioMeasurements).write("src/main/resources/toyScenarioData/toyScenarioMeasurements.xml");
		//Config config=ConfigGenerator.generateToyConfig();
		//ParamReader pReader=new ParamReader("src/main/resources/toyScenarioData/paramReaderToy.csv");
		//pReader.ScaleDown(getOriginalParamSimplified());
		//Measurements newMEasurements=generateSyntheticMeasurements(emptyMeasurements, config, pReader, new SimRunImplToy(), pReader.ScaleDown(getOriginalParamSimplified()), "src/main/resources/toyScenarioData/toyScenarioMeasurementsTrial1.xml",true);
		
		
		Measurements emptyMeasurements=new MeasurementsReader().readMeasurements("data/toyScenarioLargeData/toyScenarioLargeEmptyATCMeasurements.xml");
		Config config=ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "data/toyScenarioLargeData/configToyLargeMod.xml");
		//config.plans().setInputFile("data/toyScenarioLargeData/output_plans_Original.xml.gz");
		config.global().setNumberOfThreads(15);
		ParamReader pReader=new ParamReader("src/main/resources/toyScenarioData/paramReaderToy.csv");
		Measurements newMEasurements=generateSyntheticMeasurements(emptyMeasurements, config, pReader, new SimRunImplToyLarge(), pReader.ScaleDown(getOriginalParamSimplified()), "toyScenarioLarge/fabricatedCount.xml",true);
		System.out.println("Done till toyMeasurements");
	}
	
}
