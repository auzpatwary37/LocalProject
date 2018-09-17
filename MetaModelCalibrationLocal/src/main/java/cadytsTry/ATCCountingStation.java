package cadytsTry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;



public class ATCCountingStation{
	
	/**
	 * This method will basically create a new Measurements from the given measurements based on given timeBean
	 * Volumes will be based on the provided timeBean 
	 * All time Bean ids will be inserted in the volumes and the volumes for each time Bean will be assumed Zero.
	 * The Measurements file will be written in the provided File Location
	 * 
	 * This is basically needed for toyScenario Large Case
	 * 
	 * @param m
	 * @param desiredtimeBean
	 * @param networkFileLoc 
	 */
	public static Measurements createEmptyTimeMeasurement(Measurements mOld, Map<String,Tuple<Double,Double>>desiredtimeBean,String fileLoc, String networkFileLoc) {
		Config config=ConfigUtils.createConfig();
		config.network().setInputFile(networkFileLoc);
		Network network=ScenarioUtils.loadScenario(config).getNetwork();
		Measurements m=Measurements.createMeasurements(desiredtimeBean);
		
		
		for(Measurement mm:mOld.getMeasurements().values()) {
			boolean containsAllLink=true;
			for(Id<Link>LinkId:(ArrayList<Id<Link>>)mm.getAttribute(Measurement.linkListAttributeName)) {
				if(!network.getLinks().containsKey(LinkId)) {
					containsAllLink=false;
					break;
				}
			}
			
			if(containsAllLink==false) {
				continue;
			}
			
			m.createAnadAddMeasurement(mm.getId().toString());
			for(String timeBeanId:desiredtimeBean.keySet()) {
				m.getMeasurements().get(mm.getId()).addVolume(timeBeanId, 0.);
			}
			m.getMeasurements().get(mm.getId()).setAttribute(mm.linkListAttributeName, new ArrayList<Id<Link>>((ArrayList<Id<Link>>)mm.getAttribute(mm.linkListAttributeName)));
		}
		
		new MeasurementsWriter(m).write(fileLoc);
		return m;
	}
	
@SuppressWarnings("unchecked")
public static void main(String[] args) throws IOException {
		
		boolean takeCoreStationsOnly=true;
		boolean HKISeperate=true;
		String fileLoc="data/ATC2016/StationDetailsATC2016.txt";
		String stationDetailsFolderLoc="data/ATC2016/ATC_TRAFFIC_DATA";
		HashMap<Id<ATCCountingStation>,ATCCountingStation>stations=new HashMap<>();
//		Config config=ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config,"data/config_clean.xml");
//		Network network=ScenarioUtils.loadScenario(config).getNetwork();
		BufferedReader bf=new BufferedReader(new FileReader(new File(fileLoc)));
		String line;
		bf.readLine();
		while((line=bf.readLine())!=null) {
			ATCCountingStation acs=new ATCCountingStation(line,stationDetailsFolderLoc);
			if(HKISeperate) {
				if(acs.getRegion().equals("HK")) {
					stations.put(acs.getCountingStaionId(),acs);
				}
			}else {
				stations.put(acs.getCountingStaionId(),acs);
			}
			
		}
		HashMap<Id<ATCCountingStation>,ATCCountingStation> coreStations=new HashMap<>();
		if(takeCoreStationsOnly==true) {
			for(ATCCountingStation station:stations.values()) {
				if(station.isDetailsInfoAvailable==true) {
					//station.matchNetworkAndExtractLinkIDs(network);
					coreStations.put(station.getCountingStaionId(), station);
					
				}
			}
		}
		
		readAndAssignLinkIdToStations("data\\ATC2016\\coreStationsHKIDetails.txt", coreStations);
		
		ArrayList<Measurement> measurements=new ArrayList<>();
		
		for(ATCCountingStation st:coreStations.values()) {
			measurements.addAll(st.getDirectionSpecificMeasurement(null, false, "peak"));
		}
		
		Map<String,Tuple<Double,Double>>timeBean = null;
		Set<String> timeBeanIds = null;
		timeBeanIds=new HashSet<>();
		int i=0;
		int noOfMeasurementVolume=0;
		for(Measurement m:measurements) {
			if (i==0) {
				timeBean=m.getTimeBean();
				i=1;
			}
			
			timeBeanIds.addAll(m.getVolumes().keySet());
			noOfMeasurementVolume+=m.getVolumes().size();
		}
		Map<String,Tuple<Double,Double>> reducedTimeBean=new HashMap<>(timeBean);
		for(String s:timeBean.keySet()) {
			if(!timeBeanIds.contains(s)) {
				reducedTimeBean.remove(s);
			}
		}
		
		Measurements m=Measurements.createMeasurements(reducedTimeBean);
		
		for(Measurement mm:measurements) {
			m.createAnadAddMeasurement(mm.getId().toString());
			for(String timeBeanId:mm.getVolumes().keySet()) {
				m.getMeasurements().get(mm.getId()).addVolume(timeBeanId, mm.getVolumes().get(timeBeanId));
			}
			m.getMeasurements().get(mm.getId()).setAttribute(mm.linkListAttributeName, new ArrayList<Id<Link>>((ArrayList<Id<Link>>)mm.getAttribute(mm.linkListAttributeName)));
		}
		
		new MeasurementsWriter(m).write("data/ATCMeasurementsPeak.xml");
		
		if(timeBeanIds.size()!=1) {
		Counts<Measurement> counts=new Counts<Measurement>();
		for(Measurement mm:measurements) {
			counts.createAndAddCount(Id.create(mm.getId().toString(), Measurement.class), mm.getId().toString());
			for(String s:mm.getVolumes().keySet()) {
				counts.getCount(mm.getId()).createVolume(Integer.parseInt(s), mm.getVolumes().get(s));
			}
			
		}
		counts.setYear(2016);
		new CountsWriter(counts).write("data/ATCCountsPeakHour.xml");
		
		Config config=ConfigUtils.createConfig();
		config.network().setInputFile("data/network.xml");
		Scenario scenario=ScenarioUtils.loadScenario(config);
		Network network=scenario.getNetwork();
		
		Counts<Link> countsLink=new Counts<Link>();
		for(Measurement mm:measurements) {
			if(((ArrayList<Id<Link>>)mm.getAttribute(mm.linkListAttributeName)).size()>1) {
				continue;
			}
			Id<Link> linkId=((ArrayList<Id<Link>>)mm.getAttribute(mm.linkListAttributeName)).get(0);
			if(countsLink.getCounts().keySet().contains(linkId)){
				System.out.println("Warning!!! duplicate link found for two different stations. Check link id; "+linkId.toString() 
						+" and station id: "+mm.getId().toString()+ " and "+countsLink.getCounts().get(linkId).getCsLabel());
				continue;
			}
			if(!network.getLinks().keySet().contains(linkId)) {
				System.out.println("Netowrk does not contain the link "+ linkId+". Ignoring the station "+mm.getId().toString());
				continue;
			}
			
			countsLink.createAndAddCount(linkId, mm.getId().toString());
			for(String s:mm.getVolumes().keySet()) {
				countsLink.getCount(linkId).createVolume(Integer.parseInt(s), mm.getVolumes().get(s));
			}
			
		}
		countsLink.setYear(2016);
		new CountsWriter(countsLink).write("data/ATCCountsPeakHourLink.xml");
		System.out.println("Total single Links stations = "+countsLink.getCounts().size()*2);
		}
		
		//For Large Scale Toy Scenario
		Config config=ConfigUtils.createConfig();
		config.network().setInputFile("data/toyScenarioLargeData/network.xml");
		Network networkToyLarge=ScenarioUtils.loadScenario(config).getNetwork();
		Measurements toyLargeMeasurement=Measurements.createMeasurements(m.getTimeBean());
		for(Measurement mm:m.getMeasurements().values()) {
			boolean allLinksAvailable=true;
			for(Id<Link> lId:(ArrayList<Id<Link>>)mm.getAttribute(Measurement.linkListAttributeName)) {
				if(!networkToyLarge.getLinks().containsKey(lId)) {
					allLinksAvailable=false;
					break;
				}
			}
			if(allLinksAvailable==true) {
				toyLargeMeasurement.createAnadAddMeasurement(mm.getId().toString());
				
			}
		}
		
		createEmptyTimeMeasurement(m,ParamReader.getDefaultTimeBean(),"data/toyScenarioLargeData/toyScenarioLargeEmptyATCMeasurements.xml","data/toyScenarioLargeData/network.xml");
		
		System.out.println("Total Measurement Volume = "+noOfMeasurementVolume);
		
	}
	

	private boolean isRandomLinkAssignment=false;
	private boolean isOneWay=false;
	private boolean isDetailsInfoAvailable=false;
	private ArrayList<String> dataReplication=DirectionalInfoHolder.getDefaultDataReplication();
	
	//from station details. These data should be available for all the stations
	private final double preAADT;
	private final double postAADT;
	
	//Data from core station statistics. These data is available for only 175 stations. 
	
	DirectionalInfoHolder holder1=new DirectionalInfoHolder(null);
	DirectionalInfoHolder holder2=new DirectionalInfoHolder(null);
	//daily variations
	HashMap<String,Double[]>hourlyVariation=new HashMap<String,Double[]>();
	
	//weekly variations
	
	private HashMap<String,Double> allDayWeeklyChange=new HashMap<>();
	
	//monthly variations
	private HashMap<String,Double[]> monthlyChange=new HashMap<>();
		
	//yearlyVariations
	private HashMap<Double,Double> yearlyVariation=new HashMap<>();
	//counting station info
	
	private final Id<ATCCountingStation> countingStaionId;
	private final String fromRoad;
	private final String toRoad;
	private final String roadName;
	private final String stationType;
	private final String roadType;
	private final String region;
	private final String roadNetworkType;
	private final Coord stationCoord;
	private final String deliminator="	";
	private HashMap<String,Double> realLinkCount=new HashMap<>();
	public ATCCountingStation(String line,String stationDetailsFolderLoc) {
		
		//assuming the station details are in a txt file and the rest are in excel files and each line is in the format specified by the ATC gbd file
		String[] part=line.split(deliminator);
		this.preAADT=Double.parseDouble(part[10]);
		this.postAADT=Double.parseDouble(part[11]);
		this.roadName=part[5];
		this.countingStaionId=Id.create(part[0], ATCCountingStation.class);
		this.stationType=part[1];
		this.region=part[2];
		this.roadType=part[3];
		this.roadNetworkType=part[4];
		this.fromRoad=part[6];
		this.toRoad=part[7];
		this.stationCoord=new Coord(Double.parseDouble(part[8]),Double.parseDouble(part[9]));
		this.stationDetailsParser(stationDetailsFolderLoc);
	}
	
	public boolean isRandomLinkAssignment() {
		return isRandomLinkAssignment;
	}

	public void stationDetailsParser(String fileLoc) {
		File file=new File(fileLoc+"/S"+this.countingStaionId.toString()+".xls");
		if(!file.exists()) {
			return;
		}
		try {
			this.isDetailsInfoAvailable=true;
			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
			HSSFWorkbook wb;
			wb = new HSSFWorkbook(fs);	
			HSSFSheet sheet = wb.getSheetAt(0);

			//dataExtraction for eastOrNorthBoundAADT
			HashMap<String,Integer> colindexes=new HashMap<>();
			colindexes.put(this.dataReplication.get(0),30);
			colindexes.put(this.dataReplication.get(1),39);
			colindexes.put(this.dataReplication.get(2),48);
			colindexes.put(this.dataReplication.get(3),57);
			//forNorthOrEastBound
			if(!sheet.getRow(101).getCell(1).getStringCellValue().equals("")){
				
				holder1.setDirection(sheet.getRow(101).getCell(1).getStringCellValue());
				HashMap<String,Double>AADTVolume=new HashMap<>();
				HashMap<String,Integer> AMPeak=new HashMap<>();
				HashMap<String,Integer> PMPeak=new HashMap<>();
				HashMap<String,Double> AMPeakVolume=new HashMap<>();
				HashMap<String,Double> PMPeakVolume=new HashMap<>();
				for(String s:this.dataReplication) {
					AADTVolume.put(s, sheet.getRow(102).getCell(colindexes.get(s)).getNumericCellValue());
					String part[]=sheet.getRow(105).getCell(colindexes.get(s)).getStringCellValue().split("-");
					AMPeak.put(s, Integer.parseInt(part[1])/100);
					part=sheet.getRow(108).getCell(colindexes.get(s)).getStringCellValue().split("-");
					PMPeak.put(s, Integer.parseInt(part[1])/100);
					AMPeakVolume.put(s,sheet.getRow(106).getCell(colindexes.get(s)).getNumericCellValue());
					PMPeakVolume.put(s, sheet.getRow(109).getCell(colindexes.get(s)).getNumericCellValue());
				}
				this.holder1.setAADTVolume(AADTVolume);
				this.holder1.setAMPeak(AMPeak);
				this.holder1.setPMPeak(PMPeak);
				this.holder1.setAMPeakVolume(AMPeakVolume);
				this.holder1.setPMPeakVolume(PMPeakVolume);
			}

			//dataExtraction for westOrSounthBoundAADT
			if(!sheet.getRow(113).getCell(1).getStringCellValue().equals("")){
				holder2.setDirection(sheet.getRow(113).getCell(1).getStringCellValue());
				HashMap<String,Double>AADTVolume=new HashMap<>();
				HashMap<String,Integer> AMPeak=new HashMap<>();
				HashMap<String,Integer> PMPeak=new HashMap<>();
				HashMap<String,Double> AMPeakVolume=new HashMap<>();
				HashMap<String,Double> PMPeakVolume=new HashMap<>();
				for(String s:this.dataReplication) {
					AADTVolume.put(s, sheet.getRow(114).getCell(colindexes.get(s)).getNumericCellValue());
					String part[]=sheet.getRow(117).getCell(colindexes.get(s)).getStringCellValue().split("-");
					AMPeak.put(s, Integer.parseInt(part[1])/100);
					part=sheet.getRow(120).getCell(colindexes.get(s)).getStringCellValue().split("-");
					PMPeak.put(s, Integer.parseInt(part[1])/100);
					AMPeakVolume.put(s,sheet.getRow(118).getCell(colindexes.get(s)).getNumericCellValue());
					PMPeakVolume.put(s, sheet.getRow(121).getCell(colindexes.get(s)).getNumericCellValue());
				}
				this.holder2.setAADTVolume(AADTVolume);
				this.holder2.setAMPeak(AMPeak);
				this.holder2.setPMPeak(PMPeak);
				this.holder2.setAMPeakVolume(AMPeakVolume);
				this.holder2.setPMPeakVolume(PMPeakVolume);
			}else {
				this.isOneWay=true;
			}
			
			//data extraction for hourly variation
			colindexes.put(this.dataReplication.get(0),10);
			colindexes.put(this.dataReplication.get(1),11);
			colindexes.put(this.dataReplication.get(2),12);
			colindexes.put(this.dataReplication.get(3),13);
			for(String s:colindexes.keySet()) {
				Double[] a=new Double[24];
				for(int i=7;i<=30;i++) {
					a[i-7]=sheet.getRow(i).getCell(colindexes.get(s)).getNumericCellValue();
				}
				this.hourlyVariation.put(s, a);
			}
			
			//data extraction for monthly variation
			
			colindexes.put(this.dataReplication.get(0),2);
			colindexes.put(this.dataReplication.get(1),3);
			colindexes.put(this.dataReplication.get(2),4);
			colindexes.put(this.dataReplication.get(3),5);
			for(String s:colindexes.keySet()) {
				Double[] a=new Double[12];
				for(int i=8;i<=19;i++) {
					a[i-8]=sheet.getRow(i).getCell(colindexes.get(s)).getNumericCellValue();
				}
				this.monthlyChange.put(s, a);
			}
			
			//data extraction for weekly variation
			for(int i=7;i<=13;i++) {
				if(this.allDayWeeklyChange.containsKey(sheet.getRow(i).getCell(6).getStringCellValue())&&sheet.getRow(i).getCell(6).getStringCellValue().equals("S") ) {
					this.allDayWeeklyChange.put("Sat", sheet.getRow(i).getCell(7).getNumericCellValue());
				}else if (this.allDayWeeklyChange.containsKey(sheet.getRow(i).getCell(6).getStringCellValue())&&sheet.getRow(i).getCell(6).getStringCellValue().equals("T")) {
					this.allDayWeeklyChange.put("Th", sheet.getRow(i).getCell(7).getNumericCellValue());
				}
				this.allDayWeeklyChange.put(sheet.getRow(i).getCell(6).getStringCellValue(), sheet.getRow(i).getCell(7).getNumericCellValue());
			}
			
			//data extraction for yearly vriation
			for(int i=7;i<=26;i++) {
				if(sheet.getRow(i).getCell(15)==null ||sheet.getRow(i).getCell(15).getCellType()==CellType.BLANK) {
					break;	
				}
				this.yearlyVariation.put(sheet.getRow(i).getCell(15).getNumericCellValue(), sheet.getRow(i).getCell(16).getNumericCellValue());
				
			}
			
			//TODO: Understand what these data means and properly handle the data.

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
	

	public void  matchNetworkAndExtractLinkIDs(Network network) {
		//TODO:Fix link matching criteria
		
		Link nearestLink=NetworkUtils.getNearestLink(network, this.stationCoord);
		Link opositeToNearestLink=null;
		for(Link l:nearestLink.getFromNode().getInLinks().values()) {
			if(l.getFromNode().getId().equals(nearestLink.getToNode().getId())) {
				opositeToNearestLink=l;
			}
		}
		this.holder1.setMatsimLinkId(nearestLink.getId());
		if(holder1.getDirection()==null && holder2.getDirection()==null) {
			if(opositeToNearestLink==null) {
				this.isOneWay=true;
			}else {
				this.holder2.setMatsimLinkId(opositeToNearestLink.getId());
			}
			this.isRandomLinkAssignment=true;
			return;
		}
		
		if(holder1.getDirection().equals("EAST BOUND")) {
			if(nearestLink.getFromNode().getCoord().getX()<nearestLink.getToNode().getCoord().getX()) {
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}
			}else if(opositeToNearestLink!=null && opositeToNearestLink.getFromNode().getCoord().getX()<opositeToNearestLink.getToNode().getCoord().getX()) {
				holder1.setMatsimLinkId(opositeToNearestLink.getId());
				if(holder2!=null) {
					holder2.setMatsimLinkId(nearestLink.getId());
					}
			}else {
				System.out.println("Error!!! could not match link. Randomly assigning links to counting stations");
				this.isRandomLinkAssignment=true;
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}else if(holder2!=null && opositeToNearestLink==null) {
					holder2.setMatsimLinkId(nearestLink.getId());
				}
			}
		}else if(holder1.getDirection().equals("SOUTH BOUND")) {
			if(nearestLink.getFromNode().getCoord().getY()>nearestLink.getToNode().getCoord().getY()) {
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}
			}else if(opositeToNearestLink!=null && opositeToNearestLink.getFromNode().getCoord().getY()>opositeToNearestLink.getToNode().getCoord().getY()) {
				holder1.setMatsimLinkId(opositeToNearestLink.getId());
				if(holder2!=null) {
					holder2.setMatsimLinkId(nearestLink.getId());
					}
			}else {
				System.out.println("Error!!! could not match link. Randomly assigning links to counting stations");
				this.isRandomLinkAssignment=true;
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}else if(holder2!=null && opositeToNearestLink==null) {
					holder2.setMatsimLinkId(nearestLink.getId());
				}
			}
		}else if(holder1.getDirection().equals("NORTH BOUND")) {
			if(nearestLink.getFromNode().getCoord().getY()<nearestLink.getToNode().getCoord().getY()) {
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}
			}else if(opositeToNearestLink!=null && opositeToNearestLink.getFromNode().getCoord().getY()<opositeToNearestLink.getToNode().getCoord().getY()) {
				holder1.setMatsimLinkId(opositeToNearestLink.getId());
				if(holder2!=null) {
					holder2.setMatsimLinkId(nearestLink.getId());
					}
			}else {
				System.out.println("Error!!! could not match link. Randomly assigning links to counting stations");
				this.isRandomLinkAssignment=true;
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}else if(holder2!=null && opositeToNearestLink==null) {
					holder2.setMatsimLinkId(nearestLink.getId());
				}
			}
		}else if(holder1.getDirection().equals("WEST BOUND")) {
			if(nearestLink.getFromNode().getCoord().getX()>nearestLink.getToNode().getCoord().getX()) {
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}
			}else if(opositeToNearestLink!=null && opositeToNearestLink.getFromNode().getCoord().getX()>opositeToNearestLink.getToNode().getCoord().getX()) {
				holder1.setMatsimLinkId(opositeToNearestLink.getId());
				if(holder2!=null) {
					holder2.setMatsimLinkId(nearestLink.getId());
				}
			}else {
				System.out.println("Error!!! could not match link. Randomly assigning links to counting stations");
				this.isRandomLinkAssignment=true;
				holder1.setMatsimLinkId(nearestLink.getId());
				if(holder2!=null && opositeToNearestLink!=null) {
					holder2.setMatsimLinkId(opositeToNearestLink.getId());
				}else if(holder2!=null && opositeToNearestLink==null) {
					holder2.setMatsimLinkId(nearestLink.getId());
				}
			}
		}
		if((nearestLink==null || opositeToNearestLink==null)&&this.isOneWay==false) {
			System.out.println();
		}
	}
	
	public ArrayList<Id<Link>> getLinkIds(){
		ArrayList<Id<Link>>linkIds=new ArrayList<>();
		if(this.isOneWay==true) {
			linkIds.add(holder1.getMatsimLinkId());
		}else {
			linkIds.add(holder1.getMatsimLinkId());
			linkIds.add(holder2.getMatsimLinkId());
		}
		return linkIds;
	}
	
	public void generateLinkCount(HashMap<String,Tuple<Double,Double>>timeBean,String dayOfWeek) {
		if(dayOfWeek==null) {
			dayOfWeek="MonToFri";
		}
		HashMap<String,Double> realLinkCount=new HashMap<>();
		if(this.isDetailsInfoAvailable==true){
			for(String timeId:timeBean.keySet()) {
				double linkCount=0;
				for(int i=(int)Math.ceil(timeBean.get(timeId).getFirst()/3600);i<(int)Math.ceil(timeBean.get(timeId).getSecond()/3600);i++) {
					linkCount+=this.postAADT*this.hourlyVariation.get(dayOfWeek)[i]/100;
				}
				realLinkCount.put(timeId, linkCount);
			}
		}else {
			for(String timeId:timeBean.keySet()) {
				double time=timeBean.get(timeId).getSecond()-timeBean.get(timeId).getFirst();
				realLinkCount.put(timeId,this.postAADT*time/(24*3600));
			}
		}
		this.realLinkCount=realLinkCount;
	}
	
	
	
	public boolean isOneWay() {
		return isOneWay;
	}

	public boolean isDetailsInfoAvailable() {
		return isDetailsInfoAvailable;
	}

	public DirectionalInfoHolder getHolder1() {
		return holder1;
	}

	public DirectionalInfoHolder getHolder2() {
		return holder2;
	}

	public final String AADTTypeName="AADT";
	public final String peakHourTypeName="peak";
	
	public Measurement getAADTMeasurement(boolean matchLinks) {
		Map<String,Tuple<Double,Double>> timeBean=new HashMap<>();
		timeBean.put("AADT", new Tuple<Double,Double>(0.,3600*24.));
		Measurements m=Measurements.createMeasurements(timeBean);
		Id<Measurement> mId=Id.create(this.countingStaionId.toString(), Measurement.class);
		m.createAnadAddMeasurement(mId.toString());
		Measurement measurement=m.getMeasurements().get(mId);
		measurement.addVolume(AADTTypeName, postAADT);
		ArrayList<Id<Link>>linkIds=new ArrayList<>(holder1.getMatsimLinkIds());
		if(this.isOneWay()==false) {
			linkIds.addAll(holder2.getMatsimLinkIds());
		}
		measurement.setAttribute(measurement.linkListAttributeName, linkIds);
		return measurement;
	}
	
	public ArrayList<Measurement> getDirectionSpecificMeasurement(Network network, boolean matchLinks,String type) {
		
		if(this.isDetailsInfoAvailable==false || this.holder1.getMatsimLinkIds().size()==0) {
			return new ArrayList<Measurement>();
		}
		
		Map<String,Tuple<Double,Double>> timeBean=new HashMap<>();
		
		if(type.equals(this.AADTTypeName)) {
			timeBean.put("AADT", new Tuple<Double,Double>(0.,3600*24.));
		}else {
			for(int i=1;i<=24;i++) {
				timeBean.put(Integer.toString(i), new Tuple<Double,Double>((i-1)*3600.,i*3600.));
			}
		}
		
		ArrayList<Measurement> countingStations=new ArrayList<>();
		if(matchLinks==true) {
			this.matchNetworkAndExtractLinkIDs(network);
		}
		
		Measurements m=Measurements.createMeasurements(timeBean);
		if(this.isOneWay==true) {
			
			m.createAnadAddMeasurement(this.countingStaionId.toString());
			Measurement cs=m.getMeasurements().get(Id.create(this.countingStaionId.toString(), Measurement.class));
			
			if(timeBean.size()==1) {
				cs.addVolume(this.AADTTypeName,holder1.getAADTVolume().get("MonToFri"));
			}else {
				cs.addVolume(Integer.toString(holder1.getAMPeak().get("MonToFri")),holder1.getAMPeakVolume().get("MonToFri"));
				cs.addVolume(Integer.toString(holder1.getPMPeak().get("MonToFri")), holder1.getPMPeakVolume().get("MonToFri"));
			}
			
			cs.setAttribute(cs.linkListAttributeName, new ArrayList<Id<Link>>(holder1.getMatsimLinkIds()));
			
			countingStations.add(cs);
		}else {
			m.createAnadAddMeasurement(this.countingStaionId.toString()+"_"+holder1.getDirection());
			Measurement cs=m.getMeasurements().get(Id.create(this.countingStaionId.toString()+"_"+holder1.getDirection(), Measurement.class));
			
			if(timeBean.size()==1) {
				cs.addVolume(this.AADTTypeName,holder1.getAADTVolume().get("MonToFri"));
			}else {
				cs.addVolume(Integer.toString(holder1.getAMPeak().get("MonToFri")),holder1.getAMPeakVolume().get("MonToFri"));
				cs.addVolume(Integer.toString(holder1.getPMPeak().get("MonToFri")),holder1.getPMPeakVolume().get("MonToFri"));
			}
			cs.setAttribute(cs.linkListAttributeName, new ArrayList<Id<Link>>(holder1.getMatsimLinkIds()));
			countingStations.add(cs);
			
			m.createAnadAddMeasurement(this.countingStaionId.toString()+"_"+holder2.getDirection());
			Measurement cs1=m.getMeasurements().get(Id.create(this.countingStaionId.toString()+"_"+holder2.getDirection(), Measurement.class));
			
			if(timeBean.size()==1) {
				cs1.addVolume(this.AADTTypeName,holder2.getAADTVolume().get("MonToFri"));
			}else {
				cs1.addVolume(Integer.toString(holder2.getAMPeak().get("MonToFri")),holder2.getAMPeakVolume().get("MonToFri"));
				cs1.addVolume(Integer.toString(holder2.getPMPeak().get("MonToFri")),holder2.getPMPeakVolume().get("MonToFri"));
			}
			cs1.setAttribute(cs1.linkListAttributeName, new ArrayList<Id<Link>>(holder2.getMatsimLinkIds()));
			countingStations.add(cs1);
		}
		return countingStations;
	}

	
	
	
	public double getPreAADT() {
		return preAADT;
	}

	public double getPostAADT() {
		return postAADT;
	}

	public HashMap<Double, Double> getYearlyVariation() {
		return yearlyVariation;
	}

	public Id<ATCCountingStation> getCountingStaionId() {
		return countingStaionId;
	}

	public String getFromRoad() {
		return fromRoad;
	}

	public String getToRoad() {
		return toRoad;
	}

	public String getRoadName() {
		return roadName;
	}

	public String getStationType() {
		return stationType;
	}

	public String getRoadType() {
		return roadType;
	}

	public String getRegion() {
		return region;
	}

	public String getRoadNetworkType() {
		return roadNetworkType;
	}

	public Coord getStationCoord() {
		return stationCoord;
	}
	
	public static void readAndAssignLinkIdToStations(String fileLoc,HashMap<Id<ATCCountingStation>,ATCCountingStation> stations) throws IOException {
		BufferedReader buff=new BufferedReader(new FileReader(new File(fileLoc)));
		buff.readLine();//get rid of the header.
		String line;
		while((line=buff.readLine())!=null){
			String[] part=line.split("	");
			Id<ATCCountingStation> stationId=Id.create(part[0], ATCCountingStation.class);
			String[] linkIds=part[3].split(",");
			for(String s:linkIds) {
				stations.get(stationId).holder1.addMatsimLink(Id.createLinkId(s));
			}
			
			Boolean isOneWay=Boolean.parseBoolean(part[5]);
			if(isOneWay==false) {
				linkIds=part[4].split(",");
				for(String s:linkIds) {
					stations.get(stationId).holder2.setMatsimLinkId(Id.createLinkId(s));
				}
			}
		}
		buff.close();
	}
	
}

class DirectionalInfoHolder{
	private Id<Link> matsimLinkId;
	private Set<Id<Link>> matsimLinkIds=new HashSet<>();
	
	public void addMatsimLink(Id<Link>linkId) {
		this.matsimLinkIds.add(linkId);
	}
	
	public Set<Id<Link>> getMatsimLinkIds(){
		return this.matsimLinkIds;
	}
	
	public Id<Link> getMatsimLinkId() {
		return matsimLinkId;
	}

	public void setMatsimLinkId(Id<Link> matsimLinkId) {
		this.matsimLinkId = matsimLinkId;
		this.matsimLinkIds.add(matsimLinkId);
	}

	private String direction=null;
	//this is normally for ATC2016 -allDay, MondayToFriDay, Saturday and Sunday 
	private ArrayList<String> dataReplication;
	private HashMap<String, Double> AADTVolume=new HashMap<>();
	
	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public HashMap<String, Double> getAADTVolume() {
		return AADTVolume;
	}

	public void setAADTVolume(HashMap<String, Double> aADTVolume) {
		AADTVolume = aADTVolume;
	}

	public HashMap<String, Integer> getAMPeak() {
		return AMPeak;
	}

	public void setAMPeak(HashMap<String, Integer> aMPeak) {
		AMPeak = aMPeak;
	}

	public HashMap<String, Integer> getPMPeak() {
		return PMPeak;
	}

	public void setPMPeak(HashMap<String, Integer> pMPeak) {
		PMPeak = pMPeak;
	}

	public HashMap<String, Double> getAMPeakVolume() {
		return AMPeakVolume;
	}

	public void setAMPeakVolume(HashMap<String, Double> aMPeakVolume) {
		AMPeakVolume = aMPeakVolume;
	}

	public HashMap<String, Double> getPMPeakVolume() {
		return PMPeakVolume;
	}

	public void setPMPeakVolume(HashMap<String, Double> pMPeakVolume) {
		PMPeakVolume = pMPeakVolume;
	}

	public ArrayList<String> getDataReplication() {
		return dataReplication;
	}

	//hour will start from 1 and end at 24
	private HashMap<String,Integer>AMPeak=new HashMap<>();
	private HashMap<String,Integer>PMPeak=new HashMap<>();
	private HashMap<String,Double>AMPeakVolume=new HashMap<>();
	private HashMap<String,Double>PMPeakVolume=new HashMap<>();
	public DirectionalInfoHolder(ArrayList<String> dataReplication) {
		if(dataReplication!=null) {
			this.dataReplication=dataReplication;
		}else {
			this.dataReplication=getDefaultDataReplication();
		}
		
		
	}
	
	public static ArrayList<String> getDefaultDataReplication(){
		ArrayList<String>dataReplication=new ArrayList<>();
		dataReplication.add("allDay");
		dataReplication.add("MonToFri");
		dataReplication.add("Sat");
		dataReplication.add("Sun");
		return dataReplication;
	}
	
	
	}