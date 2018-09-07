package toyScenarioGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.moeaframework.util.sequence.LatinHypercube;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;


import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.calibrator.ObjectiveCalculator;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;

public class SimObjectiveRecorder {
/**
 * THis class will record the sim objective with the real objective
 */
	
	private Config config;
	private MeasurementsStorage countData;
	private ParamReader pReader;
	
	private Map<String,Tuple<Double,Double>>timeBean=new HashMap<String,Tuple<Double,Double>>();
	public ArrayList<AnalyticalModel> sueModels=new ArrayList<>();
	public ArrayList<LinkedHashMap<String,Double>> params=new ArrayList<>();
	public ArrayList<Boolean> hasRun=new ArrayList<>();
	
	public SimObjectiveRecorder(MeasurementsStorage countdata,Config config,HashMap<String,Tuple<Double,Double>>timeBean, ParamReader pReader) {
		this.countData=countdata;
		this.config=config;
		this.timeBean=timeBean;
		this.pReader=pReader;
		
	}

	/**
	 * For now let us assume sim replication=1
	 * @param paramLimit
	 * @param totalPoints
	 * @param noOfSimReplication
	 * @param samplingMethod
	 */
	public void recordData(SimRun simRun,LinkedHashMap<String,Tuple<Double,Double>>paramLimit,int totalPoints, int noOfSimReplication,String samplingMethod,String fileLoc,boolean loadPrevious) {
		if(loadPrevious!=true) {
			if(samplingMethod.equals("LHS")) {
				this.createLHSParam(paramLimit,totalPoints);
			}else if(samplingMethod.equals("Grid")){
				this.createUniformParams(paramLimit, totalPoints);
			}
		}else {
			this.readParamDetails(fileLoc);
		}
		int simCounter=0;
		for(LinkedHashMap<String,Double> param:this.params) {
			if(hasRun.get(simCounter)==true) {
				simCounter++;
				continue;
			}
			
			Config config=this.pReader.SetParamToConfig(this.config, pReader.ScaleDown(param));
			AnalyticalModel sueAssignment=new CNLSUEModel(timeBean);
			this.hasRun.add(simCounter, true);
			this.writePramDetails(fileLoc, simCounter);
			simRun.run(sueAssignment, config, param, true, Integer.toString(simCounter),countData);
			this.writeSimCountData(countData, fileLoc, simCounter,param);
			simCounter++;
		}
		
		
		
	}
	
	public void writeSimCountData(MeasurementsStorage countData,String fileLoc,int currentSimIter,LinkedHashMap<String,Double>currentParam) {
		File file=new File(fileLoc+"_"+currentSimIter+".csv");
		
		try {
			FileWriter fw=new FileWriter(new File(fileLoc+"SimObjLogger.csv"),true);
			fw.append("Counter,SimObjective");
			FileWriter fileWriter=new FileWriter(file);
			fileWriter.append("MeasurementId,timeBeanId,RealCount,SimCount\n");
			for(Measurement m:this.countData.getCalibrationMeasurements().getMeasurements().values()) {
				for(String timeBeanId:m.getVolumes().keySet()) {
					fileWriter.append(m.getId()+","+timeBeanId+","+m.getVolumes().get(timeBeanId)
					+","+countData.getSimMeasurement(currentParam).getMeasurements().get(m.getId()).getVolumes().get(timeBeanId)+"\n");
				}
			}
			fileWriter.append("SimObjective = "+","+ObjectiveCalculator.calcObjective(countData.getCalibrationMeasurements(), countData.getSimMeasurement(currentParam),ObjectiveCalculator.TypeMeasurementAndTimeSpecific)+"\n");
			fw.append(currentSimIter+","+ObjectiveCalculator.calcObjective(countData.getCalibrationMeasurements(), countData.getSimMeasurement(currentParam), ObjectiveCalculator.TypeMeasurementAndTimeSpecific)+"\n");
			fw.flush();
			fw.close();
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void writePramDetails(String fileLocation,int simCounter) {
		try {
			FileWriter fw=new FileWriter(new File(fileLocation+"paramDetails.csv"),false);
			int i=0;
			fw.append("counter,");
			for(String s:this.params.get(0).keySet()) {
				fw.append(s+",");
			}
			
			fw.append("hasRun\n");
			for(LinkedHashMap<String,Double> p:this.params) {
				fw.append(Integer.toString(i)+",");
				for(Double d:p.values()) {
					fw.append(Double.toString(d)+",");
				}
				fw.append(Boolean.toString(hasRun.get(i))+"\n");
				i++;
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void readParamDetails(String fileLoc) {
		this.params.clear();
		this.hasRun.clear();
		try {
			BufferedReader bf=new BufferedReader(new FileReader(new File(fileLoc+"paramDetails.csv")));
			String header=bf.readLine();//get rid of the header
			String[] headerPart=header.split(",");
			String line;
			while((line=bf.readLine())!=null) {
				String[]part=line.split(",");
				LinkedHashMap<String,Double>param=new LinkedHashMap<>();
				for(int i=1;i<headerPart.length-1;i++) {
					param.put(headerPart[i],Double.parseDouble(part[i]));
				}
				this.params.add(param);
				this.hasRun.add(Boolean.parseBoolean(part[headerPart.length-1]));
			}
			bf.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
//	private LinkedHashMap<String,Double> getOriginalParam(){
//		LinkedHashMap<String,Double> originalParam=new LinkedHashMap<>();
//		originalParam.put("MarginalUtilityofTravelCar",-25.);
//		originalParam.put("MarginalUtilityofTravelpt", -20.);
//		return originalParam;
//	}
	
	
	private void createLHSParam(LinkedHashMap<String,Tuple<Double,Double>>paramLimit,int totalPoints) {
		LatinHypercube lhs=new LatinHypercube();
		double[][] lhsProb=lhs.generate(totalPoints, paramLimit.size());
		for(int i=0;i<totalPoints;i++) {
			LinkedHashMap<String,Double> param=new LinkedHashMap<>();
			int j=0;
			for(String s:paramLimit.keySet()) {
				double p=paramLimit.get(s).getFirst()+(paramLimit.get(s).getSecond()-paramLimit.get(s).getFirst())*lhsProb[i][j];
				param.put(s, p);
				j++;
			}
			this.params.add(param);
			this.hasRun.add(false);
		}
	}
	
	private void createUniformParams(LinkedHashMap<String,Tuple<Double,Double>>paramLimit,int pointsInEachAxis) {
		ArrayList<Collection<Double>> paramS=new ArrayList<>();
		for(String s:paramLimit.keySet()) {
			ArrayList<Double> a=new ArrayList<>();
			for(double i=0;i<1;i=i+1./pointsInEachAxis) {
				a.add(paramLimit.get(s).getFirst()+(paramLimit.get(s).getSecond()-paramLimit.get(s).getFirst())*i);
			}
			paramS.add(a);
		} 
		
		Collection<List<Double>> parameters=this.permutations(paramS);
		for(List<Double> l:parameters) {
			LinkedHashMap<String,Double> parames=new LinkedHashMap<>();
			int i=0;
			for(String s:paramLimit.keySet()) {
				parames.put(s, l.get(i));
				i++;
			}
			this.params.add(parames);
			this.hasRun.add(false);
		}
	}
	
	public static <T> Collection<List<T>> permutations(List<Collection<T>> collections) {
		if (collections == null || collections.isEmpty()) {
			return Collections.emptyList();
		} else {
			Collection<List<T>> res = Lists.newLinkedList();
			permutationsImpl(collections, res, 0, new LinkedList<T>());
			return res;
		}
	}

	/** Recursive implementation for {@link #permutations(List, Collection)} */
	private static <T> void permutationsImpl(List<Collection<T>> ori, Collection<List<T>> res, int d, List<T> current) {
		// if depth equals number of original collections, final reached, add and return
		if (d == ori.size()) {
			res.add(current);
			return;
		}

		// iterate from current collection and copy 'current' element N times, one for each element
		Collection<T> currentCollection = ori.get(d);
		for (T element : currentCollection) {
			List<T> copy = Lists.newLinkedList(current);
			copy.add(element);
			permutationsImpl(ori, res, d + 1, copy);
		}
	}
	public static void main(String[] args) {
		Measurements calibrationMeasurements=new MeasurementsReader().readMeasurements("src/main/resources/toyScenarioData/toyMeasurements.xml");
		Config config=ConfigGenerator.generateToyConfig();
		ParamReader pReader=new ParamReader("src/main/resources/toyScenarioData/paramReaderToy.csv");
		SimRun simRun=new SimRunImplToy();
		MeasurementsStorage ms=new MeasurementsStorage(calibrationMeasurements);
		SimObjectiveRecorder simRecorder=new SimObjectiveRecorder(ms, config, getDefaultTimeBean(), pReader);
		simRecorder.recordData(simRun,getParamLimitSimplified(), 10, 1, "Grid","toyScenario/GreedySearch/CountDataLog",false);
	}
	
//	public static LinkedHashMap<String,Double> getInitialGuessSimplified() {
//		LinkedHashMap<String,Double> initialGuess=new LinkedHashMap<>();
//		initialGuess.put("MarginalUtilityofTravelCar",-30.);
//		initialGuess.put("MarginalUtilityofTravelpt", -30.);
//		return initialGuess;
//	}
	public static HashMap<String, Tuple<Double,Double>> getDefaultTimeBean() {
		HashMap<String, Tuple<Double,Double>> timeBean=new HashMap<>();
		timeBean.put("BeforeMorningPeak", new Tuple<Double,Double>(0.0,25200.));
		timeBean.put("MorningPeak", new Tuple<Double,Double>(25200.,36000.));
		timeBean.put("AfterMorningPeak", new Tuple<Double,Double>(36000.,57600.));
		timeBean.put("AfternoonPeak", new Tuple<Double,Double>(57600.,72000.));
		timeBean.put("AfterAfternoonPeak", new Tuple<Double,Double>(72000.,86400.));
		return timeBean;
	}
	public static LinkedHashMap<String, Tuple<Double, Double>> getParamLimitSimplified() {
		LinkedHashMap<String, Tuple<Double, Double>> paramLimit=new LinkedHashMap<>();
		paramLimit.put("MarginalUtilityofTravelCar",new Tuple<Double, Double>(-60.,-10.));
		paramLimit.put("MarginalUtilityofTravelpt", new Tuple<Double, Double>(-60.,-10.));
		return paramLimit;
	}
}

