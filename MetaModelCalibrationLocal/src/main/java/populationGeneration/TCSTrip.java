package populationGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;




public class TCSTrip {
	
	private double qNo;
	private double memNo;
	private double tripNo;
	private Id<TCSTrip> tripId;
	
	private double originActivity;
	private double destinationActivity;
	
	private TPUSB otpusb;
	private TPUSB dtpusb;
	
	private double departureTime;
	private double arrivalTime;
	
	private double tripExpansionFactor;
	private double tripPurpose;
	private double modeHier;
	private double timePeriod;
	
	private HashMap<Integer,TripLeg> tripLegs=new HashMap<>();
	private HashMap<Integer,WalkTripLeg> walkLegs=new HashMap<>();
	private HashMap<Integer,TaxiTripLeg> taxiTripLegs=new HashMap<>();
	private HashMap<Integer,CarTripLeg> carTripLegs=new HashMap<>();
	

	private static double TCStimeConverter(Double time) {
		double outtime=(3600*(time.intValue()/100)+60*(time%100));
		if(outtime<3*3600) {
			outtime=outtime+24*3600;
		}
		return outtime;
		
	}
	
	@SuppressWarnings("static-access")
	public TCSTrip(double qNo,double memNo,double tripNo,double originActivity,double destinationActivity,
			TPUSB otpusb,TPUSB dtpusb,double departureTime,double arrivalTime,double tripPurpose,double modeHier,double timePeriod,
			double expansionFactor) {
		//System.out.println();
		this.qNo=qNo;
		this.memNo=memNo;
		this.tripNo=tripNo;
		this.tripId=Id.create(Double.toString(qNo)+"_"+Double.toString(memNo)+"_"+Double.toString(tripNo), TCSTrip.class);
		this.originActivity=originActivity;
		this.destinationActivity=destinationActivity;
		this.otpusb=otpusb;
		this.dtpusb=dtpusb;
		this.departureTime=this.TCStimeConverter(departureTime);
		this.arrivalTime=this.TCStimeConverter(arrivalTime);
		this.tripPurpose=tripPurpose;
		this.modeHier=modeHier;
		this.timePeriod=timePeriod;
		this.tripExpansionFactor=expansionFactor;
		if(this.arrivalTime<this.departureTime) {
			this.arrivalTime=this.arrivalTime+24*3600;
			//System.out.println("Trip duration cannot be negative!!!");
		}
	}
	public TCSMode getMainMode(HashMap<Double,TCSMode> modesDetails) {
		double mode=0;
		double distance=0;
		for(TripLeg tl:tripLegs.values()) {
			if(!(tl instanceof WalkTripLeg)) {
				if(mode==0) {
					mode=tl.getMode();
					distance=tl.getDistanceCrossed();
				}else {
					if(tl.getDistanceCrossed()>distance) {
						mode=tl.getMode();
						distance=tl.getDistanceCrossed();
					}
				}
			}
		}
		TCSMode mainMode=modesDetails.get(mode);
		return mainMode;
	}
	public Double getTripNo() {
		return tripNo;
	}
	public Id<HouseHold> getHouseHoldId(){
		return Id.create(Double.toString(this.qNo),HouseHold.class);
	}
	
	public Id<HouseHoldMember> getMemberId(){
		return Id.create(Double.toString(this.qNo)+"_"+Double.toString(this.memNo),HouseHoldMember.class);
	}
	public void addTripLeg(TripLeg tripLeg) {
		this.tripLegs.put(tripLeg.getTriplegOrder(),tripLeg);
	}
	public void addWalkTripLeg(WalkTripLeg tripLeg) {
		this.tripLegs.put(tripLeg.getTriplegOrder(),tripLeg);
		this.walkLegs.put(tripLeg.getTriplegOrder(),tripLeg);
	}
	
	public void addCarTripLeg(CarTripLeg carLeg) {
		this.carTripLegs.put(carLeg.getTriplegOrder(),carLeg);
	}
	
	public void addTaxiTripleg(TaxiTripLeg taxiLeg) {
		this.taxiTripLegs.put(taxiLeg.getTriplegOrder(),taxiLeg);
	}

	public double getqNo() {
		return qNo;
	}

	public double getMemNo() {
		return memNo;
	}

	public Id<TCSTrip> getTripId() {
		return tripId;
	}

	public double getOriginActivity() {
		return originActivity;
	}

	public double getDestinationActivity() {
		return destinationActivity;
	}

	public TPUSB getOtpusb() {
		return otpusb;
	}

	public TPUSB getDtpusb() {
		return dtpusb;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public HashMap<Integer, TripLeg> getTripLegs() {
		return tripLegs;
	}

	public HashMap<Integer, TaxiTripLeg> getTaxiTripLegs() {
		return taxiTripLegs;
	}

	public HashMap<Integer, CarTripLeg> getCarTripLegs() {
		return carTripLegs;
	}

	public double getTripExpansionFactor() {
		return tripExpansionFactor;
	}

	public double getTripPurpose() {
		return tripPurpose;
	}

	public double getModeHier() {
		return modeHier;
	}

	public double getTimePeriod() {
		return timePeriod;
	}

	public HashMap<Integer, WalkTripLeg> getWalkLegs() {
		return walkLegs;
	}
	
	public void calcTunnelUsed() {
		for(TripLeg tl:this.tripLegs.values()) {
			if(!(tl instanceof WalkTripLeg)) {
				if(this.taxiTripLegs.get(tl.getTriplegOrder())!=null) {
					tl.setTunnelUsed(this.taxiTripLegs.get(tl.getTriplegOrder()).getTunnelUsed());
				}else if(this.carTripLegs.get(tl.getTriplegOrder())!=null) {
					tl.setTunnelUsed(this.carTripLegs.get(tl.getTriplegOrder()).getTunnelUsed());
				}
				
			}
		}
	}

	public void setOtpusb(TPUSB otpusb) {
		this.otpusb = otpusb;
	}

	public void setDtpusb(TPUSB dtpusb) {
		this.dtpusb = dtpusb;
	}
}

abstract class TripLeg{
	private int triplegOrder;
	private double mode;
	private TPUSB originTPUSB;
	private TPUSB destinationTPUSB;
	private Double boardingMTRStation;
	private Double bordingAirportMTRStation;
	private Double alightingMTRStation;
	private Double alightingAirportMTRStation;
	private double distanceCrossed;
	private Double TunnelUsed=null;
	
	public TripLeg(int tripLegOrder,double Mode,TPUSB otpusb,TPUSB dtpusb,Double boardingMTRStation,Double boardingAirportMTRStation,
			Double allightingMTRStation,Double allightingAirportMTRStation) {
		this.triplegOrder=tripLegOrder;
		this.mode=Mode;
		this.originTPUSB=otpusb;
		this.destinationTPUSB=dtpusb;
		this.boardingMTRStation=boardingMTRStation;
		this.bordingAirportMTRStation=boardingAirportMTRStation;
		this.alightingMTRStation=allightingMTRStation;
		this.alightingAirportMTRStation=allightingAirportMTRStation;
		this.distanceCrossed=Math.sqrt(Math.pow(otpusb.getSatCoord().getX()-dtpusb.getSatCoord().getX(), 2)+Math.pow(otpusb.getSatCoord().getY()-dtpusb.getSatCoord().getY(), 2));
	}

	public int getTriplegOrder() {
		return triplegOrder;
	}

	public double getMode() {
		return mode;
	}

	public TPUSB getOriginTPUSB() {
		return originTPUSB;
	}

	public TPUSB getDestinationTPUSB() {
		return destinationTPUSB;
	}

	public Double getBoardingMTRStation() {
		return boardingMTRStation;
	}

	public Double getBordingAirportMTRStation() {
		return bordingAirportMTRStation;
	}

	public Double getAlightingMTRStation() {
		return alightingMTRStation;
	}

	public Double getAlightingAirportMTRStation() {
		return alightingAirportMTRStation;
	}

	public double getDistanceCrossed() {
		return distanceCrossed;
	}

	public Double getTunnelUsed() {
		return TunnelUsed;
	}

	public void setTunnelUsed(Double tunnelUsed) {
		TunnelUsed = tunnelUsed;
	}

	public void setOriginTPUSB(TPUSB originTPUSB) {
		this.originTPUSB = originTPUSB;
	}

	public void setDestinationTPUSB(TPUSB destinationTPUSB) {
		this.destinationTPUSB = destinationTPUSB;
	}
	
	
}

class TaxiTripLeg extends TripLeg{

	private List<Double> tollRoadUsed=new ArrayList<>();
	private Double fare;
	private Double waitingTime;
	private Double noOfPassengers;
	private Double haveUsedcallService;
	
	
	public TaxiTripLeg(int tripLegOrder,double Mode,TPUSB otpusb,TPUSB dtpusb,Double boardingMTRStation,Double boardingAirportMTRStation,
			Double allightingMTRStation,Double allightingAirportMTRStation,Double fare,Double taxiWaitingTime,Double noOfPassengers,
			Double haveUsedCallService) {
		super(tripLegOrder,Mode,otpusb,dtpusb,boardingMTRStation,boardingAirportMTRStation,allightingMTRStation,allightingAirportMTRStation);
		this.fare=fare;
		this.waitingTime=taxiWaitingTime;
		this.noOfPassengers=noOfPassengers;
		this.haveUsedcallService=haveUsedCallService;
		
	}
	
	public void addTollRoadUsed(double tollRoad) {
		this.tollRoadUsed.add(tollRoad);
	}
	
	
	public List<Double> getTollRoadUsed() {
		return tollRoadUsed;
	}

	public double getFare() {
		return fare;
	}

	public double getWaitingTime() {
		return waitingTime;
	}

	public double getNoOfPassengers() {
		return noOfPassengers;
	}

	public double getHaveUsedcallService() {
		return haveUsedcallService;
	}

	public Double getTunnelUsed() {
		if(this.tollRoadUsed.contains(1.0)) {
			return 1.0;
		}else if(this.tollRoadUsed.contains(2.0)) {
			return 2.0;
		}else if(this.tollRoadUsed.contains(3.0)) {
			return 3.0;
		}
		return null;
	}
	
}

class CarTripLeg extends TripLeg{
	private Double noOfPassengers;
	private Double parkingFeeAtDestination;
	private List<Double> tollRoadUsed=new ArrayList<>();
	private Double parkingSpaceType;
	
	public CarTripLeg(int tripLegOrder,double Mode,TPUSB otpusb,TPUSB dtpusb,Double boardingMTRStation,Double boardingAirportMTRStation,
			Double allightingMTRStation,Double allightingAirportMTRStation,Double parkingFee,Double parkingType,Double noOfPassengers) {
		super(tripLegOrder,Mode,otpusb,dtpusb,boardingMTRStation,boardingAirportMTRStation,allightingMTRStation,allightingAirportMTRStation);
		this.parkingFeeAtDestination=parkingFee;
		this.parkingSpaceType=parkingType;
		this.noOfPassengers=noOfPassengers;
	}
	
	public void addTollRoadUsed(double tollRoad) {
		this.tollRoadUsed.add(tollRoad);
	}

	public double getNoOfPassengers() {
		return noOfPassengers;
	}

	public double getParkingFeeAtDestination() {
		return parkingFeeAtDestination;
	}

	public List<Double> getTollRoadUsed() {
		return tollRoadUsed;
	}

	public double getParkingSpaceType() {
		return parkingSpaceType;
	}

	public Double getTunnelUsed() {
		if(this.tollRoadUsed.contains(1.0)) {
			return 1.0;
		}else if(this.tollRoadUsed.contains(2.0)) {
			return 2.0;
		}else if(this.tollRoadUsed.contains(3.0)) {
			return 3.0;
		}
		return null;
	}
}

class WalkTripLeg extends TripLeg{
	
	private boolean haveInterchangedBetweenMTR=false;
	private List<Double> MtrInterchanges=new ArrayList<>();
	
	public WalkTripLeg(int tripLegOrder,double Mode,TPUSB otpusb,TPUSB dtpusb,Double boardingMTRStation,Double boardingAirportMTRStation,
			Double allightingMTRStation,Double allightingAirportMTRStation) {
		super(tripLegOrder,Mode,otpusb,dtpusb,boardingMTRStation,boardingAirportMTRStation,allightingMTRStation,allightingAirportMTRStation);
		
	}
	
	public void addMTRInterchange(double MTRInterchange) {
		this.MtrInterchanges.add(MTRInterchange);
		if(!this.haveInterchangedBetweenMTR) {
			this.haveInterchangedBetweenMTR=true;
		}
	}

	public boolean isHaveInterchangedBetweenMTR() {
		return haveInterchangedBetweenMTR;
	}

	public List<Double> getMtrInterchanges() {
		return MtrInterchanges;
	}	
////  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

class MechanaisedTripLeg extends TripLeg{
	
	private boolean haveInterchangedBetweenMTR=false;
	private List<Double> MtrInterchanges=new ArrayList<>();
	private double tunnelUsed;
	
	public MechanaisedTripLeg(int tripLegOrder,double Mode,TPUSB otpusb,TPUSB dtpusb,Double boardingMTRStation,Double boardingAirportMTRStation,
			Double allightingMTRStation,Double allightingAirportMTRStation) {
		super(tripLegOrder,Mode,otpusb,dtpusb,boardingMTRStation,boardingAirportMTRStation,allightingMTRStation,allightingAirportMTRStation);
		
	}
	public void addMTRInterchange(double MTRInterchange) {
		this.MtrInterchanges.add(MTRInterchange);
		if(!this.haveInterchangedBetweenMTR) {
			this.haveInterchangedBetweenMTR=true;
		}
	}

	public boolean isHaveInterchangedBetweenMTR() {
		return haveInterchangedBetweenMTR;
	}

	public List<Double> getMtrInterchanges() {
		return MtrInterchanges;
	}
	
	
}








