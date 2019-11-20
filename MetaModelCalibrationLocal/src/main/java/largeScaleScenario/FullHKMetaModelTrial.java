package largeScaleScenario;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.SAXException;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;

import createPTGTFS.FareCalculatorPTGTFS;
import de.xypron.jcobyla.Calcfc;
import de.xypron.jcobyla.Cobyla;
import de.xypron.jcobyla.CobylaExitStatus;
import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.LRFareCalculator;
import dynamicTransitRouter.fareCalculators.MTRFareCalculator;
import dynamicTransitRouter.fareCalculators.UniformFareCalculator;
import dynamicTransitRouter.fareCalculators.ZonalFareXMLParserV2;
import populationGeneration.TPUSB;
import populationGeneration.gvtcsConverter;
import ust.hk.praisehk.metamodelcalibration.Utils.MatlabObj;
import ust.hk.praisehk.metamodelcalibration.Utils.MatlabOptimizer;
import ust.hk.praisehk.metamodelcalibration.Utils.MatlabResult;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelODpair;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModelSubPop;
import ust.hk.praisehk.metamodelcalibration.calibrator.AnalyticalModelOptimizer;
import ust.hk.praisehk.metamodelcalibration.calibrator.AnalyticalModelOptimizerImpl;
import ust.hk.praisehk.metamodelcalibration.calibrator.Calibrator;
import ust.hk.praisehk.metamodelcalibration.calibrator.CalibratorImpl;
import ust.hk.praisehk.metamodelcalibration.calibrator.ObjectiveCalculator;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.matamodels.MetaModel;
import ust.hk.praisehk.metamodelcalibration.matamodels.WrappedMetaModel;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class FullHKMetaModelTrial {
	public static void main(String[] args) {
		Measurements fullHKMeasurements=new MeasurementsReader().readMeasurements("fullHk\\ATCMeasurementsPeak.xml");
		int variableSize=0;
		Database tcsDatabase;
		Map<Id<TPUSB>,TPUSB> tpusbs=null;
		try {
			tcsDatabase = DatabaseBuilder.open(new File("data/TCSDatabase/TCS2011 database.accdb"));
			Table tpusbCoord=tcsDatabase.getTable("TPUSB_coordinate_Mod");
			Table tpusb11=tcsDatabase.getTable("11TPUSB");
			
			tpusbs=gvtcsConverter.tpusbCreator(tpusbCoord,tpusb11);
			System.out.println("Total tpusbs = "+tpusbs.size() );
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		Map<Id<AnalyticalModelODpair>,Map<String,String>> odMultiplierId=new HashMap<>();
		
		Map<String,Measurements> timeSplitMeasurements=timeSplitMeasurements(fullHKMeasurements);
		
		Config config=ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config,"fullHK/config.xml");
		config.plans().setInputFile("fullHK/population_reduced.xml");
		config.network().setInputFile("fullHk/output_network.xml.gz");
		config.plans().setInputPersonAttributeFile("fullHK/output_PersonAttributes.xml.gz");
		ParamReader pReader=new ParamReader("data/subPopParamAndLimit.csv");
		pReader.setAllowUnkownParamaeterWhileScalingUp(true);
		config.transit().setTransitScheduleFile("fullHK/output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile("fullHK/output_transitVehicles.xml.gz");
		config.vehicles().setVehiclesFile("fullHK/output_vehicles.xml.gz");
		Scenario scenario=ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		for(VehicleType vt: scenario.getVehicles().getVehicleTypes().values()) {
			if(vt.getPcuEquivalents()==1) {
				vt.setPcuEquivalents(0.7);
			}
		}		
		Population population = scenario.getPopulation();
				Controler controler = new Controler(scenario);
           		ZonalFareXMLParserV2 busFareGetter = new ZonalFareXMLParserV2(scenario.getTransitSchedule());
		SAXParser saxParser;
		//scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());	

		
		
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse("fullHk/busFare.xml", busFareGetter);
			
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
		Signals.configure(controler);
		
		Map<String,FareCalculator>fareCalculators = new HashMap<>();
		
		try {
			fareCalculators.put("train", new MTRFareCalculator("fullHK/fare/mtr_lines_fares.csv",scenario.getTransitSchedule()));
			fareCalculators.put("bus", FareCalculatorPTGTFS.loadFareCalculatorPTGTFS("fullHK/fare/busFareGTFS.json"));
			fareCalculators.put("minibus", busFareGetter.get());
			fareCalculators.put("LR", new LRFareCalculator("fullHK/fare/light_rail_fares.csv"));
			fareCalculators.put("ferry",FareCalculatorPTGTFS.loadFareCalculatorPTGTFS("fullHK/fare/ferryFareGTFS.json"));
			fareCalculators.put("tram", new UniformFareCalculator(2.6));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Entry<String, Measurements> m:timeSplitMeasurements.entrySet()) {
			AnalyticalModel sue= new CNLSUEModelSubPop(m.getValue().getTimeBean(), pReader);
			sue.generateRoutesAndOD(scenario.getPopulation(), scenario.getNetwork(), scenario.getTransitSchedule(), scenario, fareCalculators);
			sue.setDefaultParameters(pReader.ScaleUp(pReader.getDefaultParam()));
			//Calibrator calibrator=new CalibratorImpl(fullHKMeasurements,"fullHk/Calibration/",false,pReader,20,4);

//			Map<Id<Measurement>,Map<String,MetaModel>> metaModels=new HashMap<>();
//			for(Measurement mId:fullHKMeasurements.getMeasurements().values()) {
//				metaModels.put(mId.getId(), new HashMap<>());
//				for(String timeBeanId:mId.getVolumes().keySet()) {
//					metaModels.get(mId.getId()).put(timeBeanId, new WrappedMetaModel(mId.getId(), timeBeanId));
//				}
//			}

			LinkedHashMap<String,Double> odMultiplier=new LinkedHashMap<>();
			
			for(Id<AnalyticalModelODpair> odId:((CNLSUEModel)sue).getOdPairs().getODpairset().keySet()) {
				if(!odMultiplierId.containsKey(odId)) {
					odMultiplierId.put(odId, new HashMap<>());
				}
				String mId=getODtoODMultiplierIdWithSubPop(odId.toString(),m.getKey(),tpusbs);
				odMultiplierId.get(odId).put(m.getKey(), getODtoODMultiplierIdWithOutSubPop(odId.toString(),m.getKey(),tpusbs));
				if(!odMultiplier.containsKey(mId)) {
					odMultiplier.put(mId, 2.);
				}
			}
			((CNLSUEModel)sue).setOdMultiplierId(odMultiplierId);
			//		
			//		
			//		AnalyticalModelOptimizer anaOptimizer=new AnalyticalModelOptimizerImpl(sue, fullHKMeasurements,metaModels, pReader.getInitialParam(), 
			//				Double.POSITIVE_INFINITY, pReader.getInitialParamLimit(),ObjectiveCalculator.TypeMeasurementAndTimeSpecific ,"WrappedMetaModel",pReader,1,"fullHk/Calibration/");
			//		
			//		LinkedHashMap<String,Double>endParam = anaOptimizer.performOptimization();
			//		Measurements finalMeasurements=sue.perFormSUE(endParam, fullHKMeasurements);
			//		new MeasurementsWriter(finalMeasurements).write("fullHK/Calibration/finalMeasurmenets.xml");
			//BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2*odMultiplier.size()+1 , 20, 0.0005);

			int i=0;
			double[] initial=new double[odMultiplier.size()];
			double[] lb=new double[odMultiplier.size()];
			double[] ub=new double[odMultiplier.size()];

			for(String key:odMultiplier.keySet()) {
				initial[i]=odMultiplier.get(key);
				lb[i]=0.1;
				ub[i]=100;
				i++;
			}

//			final PointValuePair optimum
//			= optimizer.optimize(
//					new MaxEval(500),
//					new ObjectiveFunction(new BobyqaObjective(odMultiplier, pReader, sue, m.getValue(), "fullHk/Calibration/",m.getKey())),
//					GoalType.MINIMIZE,
//					new InitialGuess(initial),
//					//new SimpleBounds(boundaries[0], boundaries[1]));
//					new SimpleBounds(lb,ub));
			
			BobyqaObjective objective=new BobyqaObjective(odMultiplier, pReader, sue, m.getValue(), "fullHk/Calibration/",m.getKey());
			objective.setLowerBound(lb);
			objective.setUpperBound(ub);
			
//			CobylaExitStatus result1= Cobyla.findMinimum(objective,initial.length, initial.length*2,
//					initial,25,.01 ,3, 3000);
//			
			MatlabOptimizer optimizer=new MatlabOptimizer(objective, initial, lb, ub);
//			MatlabResult result=new MatlabResult(initial,0);
			
			MatlabResult result= optimizer.performOptimization();
			writeMatlabResultInCSV(result,"fullHk/Calibration/",optimizer.getObjective(),m.getKey());
			
			//		
		}

	}
	
	
	private static Map<String,Measurements> timeSplitMeasurements(Measurements m){
		Map<String,Measurements> mOuts=new HashMap<>();
		for(String timeId:m.getTimeBean().keySet()) {
			Map<String,Tuple<Double,Double>> singleBeanTimeBean=new HashMap<>();
			singleBeanTimeBean.put(timeId, m.getTimeBean().get(timeId));
			Measurements mt=Measurements.createMeasurements(singleBeanTimeBean);
			mOuts.put(timeId, mt);
			for(Entry<Id<Measurement>, Measurement> d:m.getMeasurements().entrySet()) {
				if(d.getValue().getVolumes().containsKey(timeId)) {
					mt.createAnadAddMeasurement(d.getKey().toString(), d.getValue().getMeasurementType());
					for(Entry<String, Object> attribue:d.getValue().getAttributes().entrySet()) {
						mt.getMeasurements().get(d.getKey()).setAttribute(attribue.getKey(), attribue.getValue());
					}
					mt.getMeasurements().get(d.getKey()).addVolume(timeId, d.getValue().getVolumes().get(timeId));
				}
			}
		}
		
		return mOuts;
	}
	
	
	public static String getODtoODMultiplierIdWithSubPop(String odId,String timeKey,Map<Id<TPUSB>,TPUSB> tpusbs) {
		String oTPU=odId.split("_")[0];
		String OdistrictId =  Double.toString(tpusbs.get(Id.create(oTPU, TPUSB.class)).getDistrict26Id());
		String dTPU=odId.split("_")[1];
		String DdistrictId = Double.toString(tpusbs.get(Id.create(dTPU, TPUSB.class)).getDistrict26Id());
		
		return "All "+OdistrictId+"_"+DdistrictId+"_"+timeKey+"_"+"ODMultiplier";
	}
	
	public static String getODtoODMultiplierIdWithOutSubPop(String odId,String timeKey,Map<Id<TPUSB>,TPUSB> tpusbs) {
		String oTPU=odId.split("_")[0];
		String OdistrictId = Double.toString(tpusbs.get(Id.create(oTPU, TPUSB.class)).getDistrict26Id());
		String dTPU=odId.split("_")[1];
		String DdistrictId = Double.toString(tpusbs.get(Id.create(dTPU, TPUSB.class)).getDistrict26Id());
		
		return OdistrictId+"_"+DdistrictId+"_"+timeKey+"_"+"ODMultiplier";
	}
	
	/**
	 * Provide directory 
	 * file Contains 
	 * 
	 * Feval value 
	 * 
	 * O_district D_distrct multiplier
	 * 
	 * 
	 * @param fileLoc
	 */
	private static void writeMatlabResultInCSV(MatlabResult result,String fileLoc,MatlabObj objective,String timeId) {
		try {
			FileWriter fw=new FileWriter(new File(fileLoc+"/optimizationResult_"+timeId+".csv"));
			fw.append("Feval,"+result.getFval()+"\n");
			fw.append("O_district,D_district,timeId,multiplier\n");
			LinkedHashMap<String,Double> resultingParam=objective.ScaleUp(result.getX());
			
			for(Entry<String, Double> entry:resultingParam.entrySet()) {
				String[] part=entry.getKey().split("_");
				fw.append(part[1]+","+part[2]+","+part[3]+","+entry.getValue()+"\n");
			}
			fw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}



class BobyqaObjective implements MultivariateFunction, MatlabObj,Calcfc{
	
	private final LinkedHashMap<String,Double> initialParam;
	private final ParamReader pReader;
	private AnalyticalModel sue;
	private final Measurements measurements;
	private final String fileLoc;
	private int iterCounter=0;
	private final String timeId;
	private double[] lowerBound=null;
	private double[] upperBound=null;
	
	
	
	
	public double[] getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double[] lowerBound) {
		this.lowerBound = lowerBound;
	}

	public double[] getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double[] upperBound) {
		this.upperBound = upperBound;
	}

	public BobyqaObjective(LinkedHashMap<String,Double> param,ParamReader pReader,AnalyticalModel sue,Measurements measurements,String fileLoc,String timeId) {
		this.initialParam=param;
		this.pReader=pReader;
		this.sue=sue;
		this.measurements=measurements;
		this.fileLoc=fileLoc;
		this.timeId=timeId;
	}
	
	@Override
	public double value(double[] x) {
		LinkedHashMap<String, Double>params=ScaleUp(x);
		this.sue.clearLinkCarandTransitVolume();
		Measurements anaMeasurements=this.measurements.clone();
		anaMeasurements=this.sue.perFormSUE(new LinkedHashMap<>(params),anaMeasurements);
		new MeasurementsWriter(anaMeasurements).write(fileLoc+"/measurements"+iterCounter+".xml");
		double Objective=ObjectiveCalculator.calcObjective(this.measurements, anaMeasurements, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
		this.logOoptimizationDetails(this.iterCounter, this.fileLoc, params, Objective);
		iterCounter++;
		return Objective;
	}
	
	@Override
	public LinkedHashMap<String,Double> ScaleUp(double[] x) {
		LinkedHashMap<String,Double> params=new LinkedHashMap<>();
		int j=0;
		for(String s:this.initialParam.keySet()) {
			//params.put(s, (1+x[j]/100)*this.initialParam.get(s));
			params.put(s, x[j]);
			j++;
		}

		return params;
	}
	private void logOoptimizationDetails(int optimIterNo,String fileLoc,LinkedHashMap<String,Double>params,double objective) {
		System.out.println("Objective for timeBean "+this.timeId+"and iteration " + optimIterNo +" = "+objective);
		try {
			File file=new File(fileLoc+timeId+"_OoptimizationDetails.csv");
			FileWriter fw=new FileWriter(file,true);
			if(optimIterNo==0) {
				fw.append("optimIterNo,Objective");
				for(String s:params.keySet()) {
					fw.append(","+s);
				}
				fw.append("\n");
			}
			fw.append(optimIterNo+","+objective);
			for(double d:params.values()) {
				fw.append(","+d);
			}
			fw.append("\n");

			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public double evaluateFunction(double[] x) {
		LinkedHashMap<String, Double>params=ScaleUp(x);
		this.sue.clearLinkCarandTransitVolume();
		Measurements anaMeasurements=this.measurements.clone();
		anaMeasurements=this.sue.perFormSUE(new LinkedHashMap<>(params),anaMeasurements);
		new MeasurementsWriter(anaMeasurements).write(fileLoc+"/measurements"+iterCounter+".xml");
		double Objective=ObjectiveCalculator.calcObjective(this.measurements, anaMeasurements, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
		this.logOoptimizationDetails(this.iterCounter, this.fileLoc, params, Objective);
		iterCounter++;
		return Objective;
	}

	@Override
	public double evaluateConstrain(double[] x) {
		// TODO Auto-generated method stub
		return 5;
	}

	@Override
	public double compute(int n, int m, double[] x, double[] con) {
		LinkedHashMap<String, Double>params=ScaleUp(x);
		this.sue.clearLinkCarandTransitVolume();
		Measurements anaMeasurements=this.measurements.clone();
		anaMeasurements=this.sue.perFormSUE(new LinkedHashMap<>(params),anaMeasurements);
		new MeasurementsWriter(anaMeasurements).write(fileLoc+"/measurements"+iterCounter+".xml");
		double Objective=ObjectiveCalculator.calcObjective(this.measurements, anaMeasurements, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
		this.logOoptimizationDetails(this.iterCounter, this.fileLoc, params, Objective);
		iterCounter++;
		
		int k=0;
		for(int j=0;j<n;j++) {
			con[k]=x[j]-this.lowerBound[j];
			con[k+1]=this.upperBound[j]-x[j];
			k=k+2;
			j++;
		}
		return Objective;
	}
}


