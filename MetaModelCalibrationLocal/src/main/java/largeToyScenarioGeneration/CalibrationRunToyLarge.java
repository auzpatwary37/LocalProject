package largeToyScenarioGeneration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.apache.log4j.PropertyConfigurator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;


import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.calibrator.Calibrator;
import ust.hk.praisehk.metamodelcalibration.calibrator.CalibratorImpl;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.matamodels.MetaModel;
import ust.hk.praisehk.metamodelcalibration.matamodels.SimAndAnalyticalGradientCalculator;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class CalibrationRunToyLarge {

	public static void main(String[] args) {
		
		PropertyConfigurator.configure("src/main/resources/log4j.properties");
		
		final boolean internalCalibration=false;
		
		
		Measurements calibrationMeasurements=new MeasurementsReader().readMeasurements("toyScenarioLarge/Calibration/simMeasurement0_230210Laptop.xml");
		Config initialConfig=ConfigUtils.createConfig();
		ConfigUtils.loadConfig(initialConfig, "data/toyScenarioLargeData/configToyLargeMod.xml");
		ParamReader pReader=new ParamReader("src/main/resources/toyScenarioData/paramReaderToyLarge.csv");
		MeasurementsStorage storage=new MeasurementsStorage(calibrationMeasurements);
		LinkedHashMap<String,Double>initialParams=loadInitialParam(pReader,new double[] {-200,-200});
		LinkedHashMap<String,Double>params=initialParams;
		pReader.setInitialParam(initialParams);
		
		Calibrator calibrator=new CalibratorImpl(calibrationMeasurements,"toyScenarioLarge/Calibration/", internalCalibration, pReader,10, 4);
		
		calibrator.setMaxTrRadius(25.0);
	
		
		SimRun simRun=new SimRunImplToyLarge(100);
		
		writeRunParam(calibrator, "toyScenarioLarge/Calibration/", params, pReader);
		AnalyticalModel sue=new CNLSUEModel(calibrationMeasurements.getTimeBean());
		
		for(int i=0;i<30;i++) {
			Config config=pReader.SetParamToConfig(initialConfig, params);
			//config.plans().setInputFile("data/toyScenarioLargeData/output_plans.xml.gz");
			config.global().setNumberOfThreads(7);
			config.qsim().setNumberOfThreads(7);
//			if(i!=0) {
//				int currentParamNo=calibrator.getCurrentParamNo();
//				config.plans().setInputFile("toyScenarioLarge/output"+Integer.toString(currentParamNo)+"/output_plans.xml.gz");
//			}
			sue.setDefaultParameters(pReader.ScaleUp(pReader.getDefaultParam()));
			sue.setFileLoc("toyScenarioLarge/");
			simRun.run(sue, config, params, true, Integer.toString(i), storage);
					
			//new MeasurementsWriter(storage.getSimMeasurement(params)).write("toyScenarioLarge/Calibration/Measurement_"+i+".xml");
			SimAndAnalyticalGradientCalculator gradientFactory=new SimAndAnalyticalGradientCalculator(config, storage, simRun, calibrator.getTrRadius()/2/100, "FD", i, false, pReader);
			params=calibrator.generateNewParam(sue, storage.getSimMeasurement(params), gradientFactory, MetaModel.AnalyticalLinearMetaModelName);
			
		}
		
		
		
	}
	
	public static LinkedHashMap<String,Double> loadInitialParam(ParamReader pReader,double[] paramList) {
		LinkedHashMap<String,Double> initialParam=new LinkedHashMap<>(pReader.getInitialParam());
		if(initialParam.size()!=paramList.length) {
			throw new IllegalArgumentException("Dimension MissMatch! Could not load the param.");
		}
		int i=0;
		for(String s:initialParam.keySet()) {
			initialParam.put(s, paramList[i]);
			i++;
		}
		
		return initialParam;
	}
	
	public static void writeRunParam(Calibrator calibrator,String fileWriteLoc,LinkedHashMap<String,Double>params,ParamReader pReader) {
		try {
			FileWriter fw=new FileWriter(new File(fileWriteLoc+"RunParam.csv"),true);
			fw.append("ParameterName,initialParam ,upperLimit,lowerLimit\n");
			for(String s:params.keySet()) {
				fw.append(s+","+params.get(s)+","+pReader.getParamLimit().get(s).getFirst()+","+pReader.getParamLimit().get(s).getSecond()+"\n");
			}
			fw.append("initialTrustRegionRadius,"+calibrator.getTrRadius()+"\n");
			fw.append("TrInc,"+calibrator.getTrusRegionIncreamentRatio()+"\n");
			fw.append("TrDecreasing,"+calibrator.getTrustRegionDecreamentRatio()+"\n");
			fw.append("MaxTr,"+calibrator.getMaxTrRadius()+"\n");
			fw.append("MinTr,"+calibrator.getMinTrRadius()+"\n");
			fw.append("rou,"+calibrator.getThresholdErrorRatio()+"\n");
			fw.append("AnalyticalModelInternalParamterCalibration,"+calibrator.isShouldPerformInternalParamCalibration()+"\n");
			fw.append("MaxSuccesiveRejection,"+calibrator.getMaxSuccesiveRejection()+"\n");
			fw.append("MinMetaChangeReq,"+calibrator.getMinMetaParamChange()+"\n");
			fw.append("ObjectiveType,"+calibrator.getObjectiveType()+"\n");
			
			fw.append("StrtingTime,"+LocalDateTime.now().toString()+"\n");
			
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}