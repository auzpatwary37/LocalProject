package populationGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.utils.collections.Tuple;
/**
 * 
 * @author Ashraf
 *
 */
public class ActivityAnalyzer {
	private HashMap<String,Tuple<Double,Integer>> averageStartingTimeCalculator=new HashMap<>();
	private HashMap<String,Tuple<Double,Integer>> averageEndTimeCalculator=new HashMap<>();
	private HashMap<String,Double>activityDuration=new HashMap<>();
	/**
	 * This function finds the average activity duration for each activity inside a popualtion file
	 * @param population
	 * @return
	 */
	public HashMap<String,Double> getAverageActivityDuration(Population population) {
		HashMap<String,Double>actDurations=new HashMap<>();
		HashMap<String,Tuple<Double,Integer>> activities=new HashMap<>();
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					
					if(a.getStartTime()!=Double.NEGATIVE_INFINITY && a.getEndTime()!=Double.NEGATIVE_INFINITY) {
						if(a.getStartTime()>a.getEndTime()) {
							a.setEndTime(24*3600);
						}
						double duration=a.getEndTime()-a.getStartTime();
						if(duration<0) {
							throw new IllegalArgumentException("duration can not be negative");
						}
						if(activities.containsKey(a.getType())) {
							Tuple<Double,Integer> oldActDetails=activities.get(a.getType());
							Tuple<Double,Integer> newActDetails=new Tuple<>((oldActDetails.getFirst()*oldActDetails.getSecond()+duration)/(oldActDetails.getSecond()+1)
									,oldActDetails.getSecond()+1);
							activities.put(a.getType(), newActDetails);
						}else {
							Tuple<Double,Integer> newActDetails=new Tuple<>(duration,1);
							activities.put(a.getType(), newActDetails);
						}
					}
				}
			}
		}
		for(String s:activities.keySet()) {
			actDurations.put(s,activities.get(s).getFirst());
		}
		this.activityDuration=actDurations;
		return actDurations;
	}

	public HashMap<String, Double> getAverageStartingTime(Population population) {
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getStartTime()!=Double.NEGATIVE_INFINITY) {
					if(averageStartingTimeCalculator.containsKey(a.getType())) {
						Tuple<Double,Integer>oldTuple=this.averageStartingTimeCalculator.get(a.getType());
						Tuple<Double,Integer>newTuple=new Tuple<>(oldTuple.getFirst()+a.getStartTime(),oldTuple.getSecond()+1);
						this.averageStartingTimeCalculator.put(a.getType(),newTuple);
					}else {
						Tuple<Double,Integer>newTuple=new Tuple<>(a.getStartTime(),1);
						this.averageStartingTimeCalculator.put(a.getType(),newTuple);
					}
					}
				}
			}
		}
		HashMap<String, Double> averageStartingTime=new HashMap<>();
		for(String s:this.averageStartingTimeCalculator.keySet()) {
			averageStartingTime.put(s, this.averageStartingTimeCalculator.get(s).getFirst()/this.averageStartingTimeCalculator.get(s).getSecond());
		}
		return averageStartingTime;
	}
	
	public HashMap<String, Double> getAverageClosingTime(Population population) {
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getEndTime()!=Double.NEGATIVE_INFINITY) {
					if(averageEndTimeCalculator.containsKey(a.getType())) {
						Tuple<Double,Integer>oldTuple=this.averageEndTimeCalculator.get(a.getType());
						Tuple<Double,Integer>newTuple=new Tuple<>(oldTuple.getFirst()+a.getEndTime(),oldTuple.getSecond()+1);
						this.averageEndTimeCalculator.put(a.getType(),newTuple);
					}else {
						Tuple<Double,Integer>newTuple=new Tuple<>(a.getEndTime(),1);
						this.averageEndTimeCalculator.put(a.getType(),newTuple);
					}
					}
				}
			}
		}
		HashMap<String, Double> averageStartingTime=new HashMap<>();
		for(String s:this.averageStartingTimeCalculator.keySet()) {
			averageStartingTime.put(s, this.averageStartingTimeCalculator.get(s).getFirst()/this.averageStartingTimeCalculator.get(s).getSecond());
		}
		return averageStartingTime;
	}
	
	public Set<String> getActivityTypes(Population population){
		Set<String> ActivityTypes=new HashSet<String>();
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					ActivityTypes.add(a.getType());
				}
			}
		}
		return ActivityTypes;
	}

	/**
	 * This function splits an activity into multiple activity and writes the activityparams on the config file
	 * @param population
	 * @param config
	 * @param activityType
	 * @param timeGapInSecond
	 */
	public static void ActivitySplitter(Population population,Config config, String activityType,Double timeGapInSecond) {
		HashMap<String,Tuple<Double,Double>> activities=new HashMap<>();
		HashMap<String,Integer> activityCounter=new HashMap<>();
		HashMap<String,Integer> activityDurationSum=new HashMap<>();
		double startTime=0;
		double endTime=24*3600;
		for(double d=startTime;d<endTime;d=d+timeGapInSecond) {
			activities.put(activityType+"_"+d, new Tuple<>(d,d+timeGapInSecond));
			activityCounter.put(activityType+"_"+d, 0);
			activityDurationSum.put(activityType+"_"+d, 0);
		}

		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getType().equals(activityType)) {
						for(Tuple<Double,Double>t:activities.values()) {
							if(a.getStartTime()>=t.getFirst()&&a.getStartTime()<t.getSecond()&&a.getStartTime()!=Double.NEGATIVE_INFINITY) {
								a.setType(activityType+"_"+t.getFirst());
								activityCounter.put(activityType+"_"+t.getFirst(),activityCounter.get(activityType+"_"+t.getFirst())+1);
								activityDurationSum.put(activityType+"_"+t.getFirst(),activityDurationSum.get(activityType+"_"+t.getFirst())+1);
								break;
							}
						}
					}
				}
			}
		}
		ActivityParams aParams=config.planCalcScore().getActivityParams(activityType);
		for(String s:activityCounter.keySet()) {
			if(activityCounter.get(s)!=0) {
				ActivityParams ap=new ActivityParams(s);
				ap.setTypicalDuration(activityDurationSum.get(s)/activityCounter.get(s));
				ap.setClosingTime(aParams.getClosingTime());
				ap.setLatestStartTime(activities.get(s).getSecond());
				ap.setOpeningTime(activities.get(s).getFirst());
				config.planCalcScore().addActivityParams(ap);
			}
		}
	}
	
	public static void addActivityPlanParameter(PlanCalcScoreConfigGroup config,ArrayList<String>activityTypes,HashMap<String,Double>typicalDurations,
			HashMap<String,Double>typicalStartingTime,int addedlatestStartTime,int earliestStartTime,
			int defaultTypicalDuration,int defaultTypicalStartingTime,int defaultOpenningTime){
		if(activityTypes==null) {
			
			activityTypes=new ArrayList<String>();
			for(String s:typicalStartingTime.keySet()) {
				if(!activityTypes.contains(s)) {
					activityTypes.add(s);
				}
			}
			for(String s:typicalDurations.keySet()) {
				if(!activityTypes.contains(s)) {
					activityTypes.add(s);
				}
			}
			
		}
		for(String s:activityTypes) {
			ActivityParams act = new ActivityParams(s);
			if(typicalDurations.get(s)!=null && typicalDurations.get(s)!=0) {
				act.setTypicalDuration(typicalDurations.get(s));
			}else {
				act.setTypicalDuration(defaultTypicalDuration);
			}
			if(typicalStartingTime.get(s)!=null) {
				act.setLatestStartTime(typicalStartingTime.get(s)+15*60);
				act.setOpeningTime(typicalStartingTime.get(s)-3600);
			}else {
				act.setLatestStartTime(0+defaultTypicalStartingTime);
				act.setOpeningTime(defaultOpenningTime);
			}
			act.setClosingTime(26*3600);
			config.addActivityParams(act);
		}
	}
	
	public static void addActivityPlanParameter(PlanCalcScoreConfigGroup config,Set<String>activityTypes,HashMap<String,Double>typicalDurations,
			HashMap<String,Double>typicalStartingTime,HashMap<String,Double>typicalEndTime,int addedlatestStartTimeMin,int earliestEndTimeMin,
			int defaultTypicalDuration,int defaultTypicalStartingTime,int defaultOpenningTime, int defaultEndTime){
		
		for(String s:activityTypes) {
			ActivityParams act = new ActivityParams(s);
			if(typicalDurations.get(s)!=null && typicalDurations.get(s)!=0) {
				act.setTypicalDuration(typicalDurations.get(s));
			}else {
				act.setTypicalDuration(defaultTypicalDuration);
			}
			if(typicalStartingTime.get(s)!=null) {
				act.setLatestStartTime(typicalStartingTime.get(s)+addedlatestStartTimeMin*60);
				if(typicalStartingTime.get(s)-1800<0) {
					act.setOpeningTime(typicalStartingTime.get(s));
				}else {
					act.setOpeningTime(typicalStartingTime.get(s)-1800);
				}
			}else {
				act.setLatestStartTime(defaultTypicalStartingTime);
				act.setOpeningTime(defaultOpenningTime);
			}
			if(typicalEndTime.get(s)!=null) {
				act.setEarliestEndTime(typicalEndTime.get(s)-earliestEndTimeMin*60);
				if(typicalEndTime.get(s)+1800>24*3600) {
					act.setClosingTime(typicalEndTime.get(s));
				}else {
					act.setClosingTime(typicalEndTime.get(s)+1800);
				}
			}else {
				act.setEarliestEndTime(defaultEndTime-earliestEndTimeMin);
				act.setClosingTime(defaultEndTime);
			}
			config.addActivityParams(act);
		}
	}

}

